package com.example.moneymaplk.presentation.reports

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebaseGoalRepository
import com.example.moneymaplk.data.repository.FirebasePaymentFollowUpRepository
import com.example.moneymaplk.data.repository.FirebaseRecurringPaymentRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.GoalRepository
import com.example.moneymaplk.data.repository.PaymentFollowUpRepository
import com.example.moneymaplk.data.repository.RecurringPaymentRepository
import com.example.moneymaplk.data.repository.TransactionRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ReportsViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository(),
    private val goalRepository: GoalRepository = FirebaseGoalRepository(),
    private val recurringPaymentRepository: RecurringPaymentRepository = FirebaseRecurringPaymentRepository(),
    private val paymentFollowUpRepository: PaymentFollowUpRepository = FirebasePaymentFollowUpRepository()
) : ViewModel() {

    private val reportsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    private var reportsJob: Job? = null

    fun loadReports() {
        val userId = userRepository.currentUserId
        if (userId == null) {
            clearReportsJob()
            _uiState.value = ReportsUiState(shouldReturnToLogin = true)
            return
        }

        val state = _uiState.value
        if (reportsJob?.isActive == true && state.activeUserId == userId) return

        val selectedFilter = state.selectedFilter
        clearReportsJob()
        _uiState.value = ReportsUiState(
            isLoading = true,
            activeUserId = userId,
            selectedFilter = selectedFilter
        )

        reportsJob = combine(
            userRepository.observeUserProfile(userId),
            transactionRepository.observeTransactions(userId),
            goalRepository.observeGoals(userId),
            recurringPaymentRepository.observeRecurringPayments(userId),
            paymentFollowUpRepository.observePaymentFollowUps(userId)
        ) { profileResult, transactionResult, goalResult, recurringResult, invoiceResult ->
            val profile = profileResult.getOrThrow()
            goalResult.getOrThrow()
            ReportsSourceData(
                baseCurrency = profile?.defaultCurrency?.ifBlank { "LKR" } ?: "LKR",
                transactions = transactionResult.getOrThrow(),
                recurringPayments = recurringResult.getOrThrow(),
                paymentFollowUps = invoiceResult.getOrThrow()
            )
        }
            .onEach { sourceData ->
                if (userRepository.currentUserId != userId) return@onEach

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        activeUserId = userId,
                        loadedUserId = userId,
                        report = buildReportSummary(
                            sourceData = sourceData,
                            filter = state.selectedFilter
                        ),
                        errorMessage = null
                    )
                }
            }
            .catch { throwable ->
                if (userRepository.currentUserId == userId) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeUserId = userId,
                            errorMessage = throwable.message ?: "Could not load reports. Please try again."
                        )
                    }
                }
            }
            .launchIn(reportsScope)
    }

    fun retryReports() {
        clearReportsJob()
        loadReports()
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionState() {
        clearReportsJob()
        _uiState.value = ReportsUiState(selectedFilter = _uiState.value.selectedFilter)
    }

    private fun clearReportsJob() {
        reportsJob?.cancel()
        reportsJob = null
    }

    fun onFilterSelected(filter: ReportsTimeFilter) {
        _uiState.update { state ->
            state.copy(selectedFilter = filter)
        }
        retryReports()
    }

    private fun buildReportSummary(
        sourceData: ReportsSourceData,
        filter: ReportsTimeFilter
    ): ReportsSummary {
        val transactions = sourceData.transactions
            .filter { transaction -> transaction.isInPeriod(filter) }
        val incomeTransactions = transactions.filter { transaction -> transaction.type == TransactionType.INCOME }
        val expenseTransactions = transactions.filter { transaction -> transaction.type == TransactionType.EXPENSE }
        val totalIncomeLkr = FinanceCalculator.roundMoney(
            incomeTransactions.sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val totalExpensesLkr = FinanceCalculator.roundMoney(
            expenseTransactions.sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val requiredSpendingLkr = expenseTransactions
            .filter { transaction -> transaction.isCommitted && !transaction.isDiscretionary }
            .sumOf { transaction -> transaction.convertedAmountLkr }
            .let(FinanceCalculator::roundMoney)
        val flexibleSpendingLkr = expenseTransactions
            .filter { transaction -> transaction.isDiscretionary && !transaction.isCommitted }
            .sumOf { transaction -> transaction.convertedAmountLkr }
            .let(FinanceCalculator::roundMoney)
        val incomeLeftRate = FinanceCalculator.calculateIncomeLeftRate(
            totalIncomeLkr = totalIncomeLkr,
            totalExpensesLkr = totalExpensesLkr
        )

        val incomeBySource = IncomeSource.entries.map { source ->
            val sourceAmountLkr = FinanceCalculator.roundMoney(
                incomeTransactions
                    .filter { transaction -> transaction.incomeSource == source }
                    .sumOf { transaction -> transaction.convertedAmountLkr }
            )
            ReportBreakdownItem(
                label = source.displayLabel(),
                amountLkr = sourceAmountLkr,
                sharePercentage = FinanceCalculator.calculateSharePercentage(
                    amountLkr = sourceAmountLkr,
                    totalLkr = totalIncomeLkr
                ),
                shareFraction = FinanceCalculator.calculateShareFraction(
                    amountLkr = sourceAmountLkr,
                    totalLkr = totalIncomeLkr
                )
            )
        }

        val expensesByCategory = expenseTransactions
            .groupBy { transaction -> transaction.category.ifBlank { "Uncategorized" } }
            .map { (category, categoryTransactions) ->
                val categoryAmountLkr = FinanceCalculator.roundMoney(
                    categoryTransactions.sumOf { transaction -> transaction.convertedAmountLkr }
                )
                ReportBreakdownItem(
                    label = category,
                    amountLkr = categoryAmountLkr,
                    sharePercentage = FinanceCalculator.calculateSharePercentage(
                        amountLkr = categoryAmountLkr,
                        totalLkr = totalExpensesLkr
                    ),
                    shareFraction = FinanceCalculator.calculateShareFraction(
                        amountLkr = categoryAmountLkr,
                        totalLkr = totalExpensesLkr
                    )
                )
            }
            .sortedByDescending { item -> item.amountLkr }

        val activeRecurringIncome = sourceData.recurringPayments.filter { payment ->
            payment.isActive && payment.type == TransactionType.INCOME
        }
        val activeRecurringPayments = sourceData.recurringPayments.filter { payment ->
            payment.isActive && payment.type == TransactionType.EXPENSE
        }
        val estimatedMonthlyRecurringIncomeLkr = activeRecurringIncome
            .sumOf { payment ->
                FinanceCalculator.calculateMonthlyRecurringEstimate(
                    convertedAmountLkr = payment.convertedAmountLkr,
                    frequency = payment.frequency
                )
            }
            .let(FinanceCalculator::roundMoney)
        val estimatedMonthlyRecurringPaymentLkr = activeRecurringPayments
            .sumOf { payment ->
                FinanceCalculator.calculateMonthlyRecurringEstimate(
                    convertedAmountLkr = payment.convertedAmountLkr,
                    frequency = payment.frequency
                )
            }
            .let(FinanceCalculator::roundMoney)

        val unpaidFollowUps = sourceData.paymentFollowUps.filter { invoice ->
            FinanceCalculator.calculateEffectiveFollowUpStatus(invoice) == PaymentFollowUpStatus.EXPECTED
        }
        val overdueFollowUps = sourceData.paymentFollowUps.filter { invoice ->
            FinanceCalculator.calculateEffectiveFollowUpStatus(invoice) == PaymentFollowUpStatus.OVERDUE
        }
        val expectedInPeriod = unpaidFollowUps.filter { followUp ->
            followUp.expectedDate.toDate().isInPeriod(filter)
        }

        return ReportsSummary(
            baseCurrency = sourceData.baseCurrency,
            periodLabel = filter.label(),
            totalIncomeLkr = totalIncomeLkr,
            totalExpensesLkr = totalExpensesLkr,
            netBalanceLkr = FinanceCalculator.roundMoney(totalIncomeLkr - totalExpensesLkr),
            incomeLeftRate = incomeLeftRate,
            incomeBySource = incomeBySource,
            expensesByCategory = expensesByCategory,
            requiredSpendingLkr = requiredSpendingLkr,
            flexibleSpendingLkr = flexibleSpendingLkr,
            requiredSpendingSharePercentage = FinanceCalculator.calculateSharePercentage(
                amountLkr = requiredSpendingLkr,
                totalLkr = totalExpensesLkr
            ),
            requiredSpendingShareFraction = FinanceCalculator.calculateShareFraction(
                amountLkr = requiredSpendingLkr,
                totalLkr = totalExpensesLkr
            ),
            flexibleSpendingSharePercentage = FinanceCalculator.calculateSharePercentage(
                amountLkr = flexibleSpendingLkr,
                totalLkr = totalExpensesLkr
            ),
            flexibleSpendingShareFraction = FinanceCalculator.calculateShareFraction(
                amountLkr = flexibleSpendingLkr,
                totalLkr = totalExpensesLkr
            ),
            flowPoints = buildFlowPoints(sourceData.transactions, filter),
            activeRecurringIncomeCount = activeRecurringIncome.size,
            activeRecurringPaymentCount = activeRecurringPayments.size,
            estimatedMonthlyRecurringIncomeLkr = estimatedMonthlyRecurringIncomeLkr,
            estimatedMonthlyRecurringPaymentLkr = estimatedMonthlyRecurringPaymentLkr,
            expectedIncomeAmountLkr = FinanceCalculator.roundMoney(
                expectedInPeriod
                    .filter { followUp -> followUp.type == TransactionType.INCOME }
                    .sumOf { followUp -> followUp.convertedAmountLkr }
            ),
            expectedPaymentAmountLkr = FinanceCalculator.roundMoney(
                expectedInPeriod
                    .filter { followUp -> followUp.type == TransactionType.EXPENSE }
                    .sumOf { followUp -> followUp.convertedAmountLkr }
            ),
            waitingFollowUpCount = unpaidFollowUps.size,
            overdueFollowUpCount = overdueFollowUps.size,
            overdueFollowUpAmountLkr = FinanceCalculator.roundMoney(
                overdueFollowUps.sumOf { invoice -> invoice.convertedAmountLkr }
            ),
            insights = buildInsights(
                hasTransactions = transactions.isNotEmpty(),
                totalIncomeLkr = totalIncomeLkr,
                totalExpensesLkr = totalExpensesLkr,
                requiredSpendingLkr = requiredSpendingLkr,
                flexibleSpendingLkr = flexibleSpendingLkr,
                incomeLeftRate = incomeLeftRate
            ),
            hasTransactions = transactions.isNotEmpty()
        )
    }

    private fun buildInsights(
        hasTransactions: Boolean,
        totalIncomeLkr: Double,
        totalExpensesLkr: Double,
        requiredSpendingLkr: Double,
        flexibleSpendingLkr: Double,
        incomeLeftRate: Double
    ): List<ReportInsight> {
        if (!hasTransactions) {
            return listOf(
                ReportInsight(
                    title = "Add your first activity",
                    subtitle = "Income and expenses unlock weekly, monthly, and yearly insights.",
                    tone = InsightTone.INFO
                )
            )
        }

        val insights = mutableListOf<ReportInsight>()

        if (totalExpensesLkr > totalIncomeLkr) {
            insights.add(
                ReportInsight(
                    title = "Expenses are higher than income",
                    subtitle = "Review flexible spending to protect your balance.",
                    tone = InsightTone.WARNING
                )
            )
        }

        val flexibleSpendingIncomeShare = FinanceCalculator.calculateSharePercentage(
            amountLkr = flexibleSpendingLkr,
            totalLkr = totalIncomeLkr
        )
        val flexibleSpendingUsesMoreThanHalfIncome = flexibleSpendingIncomeShare > 50.0
        val flexibleSpendingHigherThanRequired = flexibleSpendingLkr > requiredSpendingLkr &&
            flexibleSpendingLkr > 0.0

        if (flexibleSpendingUsesMoreThanHalfIncome) {
            insights.add(
                ReportInsight(
                    title = "Flexible spending is heavy",
                    subtitle = "${FinanceCalculator.roundRate(flexibleSpendingIncomeShare)}% of income went to discretionary expenses.",
                    tone = InsightTone.WARNING
                )
            )
        }

        if (flexibleSpendingHigherThanRequired) {
            insights.add(
                ReportInsight(
                    title = "Discretionary beats committed",
                    subtitle = "Your optional expenses are higher than fixed payments.",
                    tone = InsightTone.INFO
                )
            )
        }

        val hasMajorWarning = totalExpensesLkr > totalIncomeLkr ||
            flexibleSpendingUsesMoreThanHalfIncome ||
            (flexibleSpendingHigherThanRequired && flexibleSpendingIncomeShare >= 30.0)

        if (incomeLeftRate >= 20.0 && !hasMajorWarning) {
            insights.add(
                ReportInsight(
                    title = "Healthy income left",
                    subtitle = "${FinanceCalculator.roundRate(incomeLeftRate)}% remained after expenses.",
                    tone = InsightTone.GOOD
                )
            )
        }

        return insights.ifEmpty {
            listOf(
                ReportInsight(
                    title = "Balanced money flow",
                    subtitle = "Income and spending look stable for this period.",
                    tone = InsightTone.GOOD
                )
            )
        }
    }

    private fun Transaction.isInPeriod(filter: ReportsTimeFilter): Boolean {
        return transactionDate.toDate().isInPeriod(filter)
    }

    private fun java.util.Date.isInPeriod(filter: ReportsTimeFilter): Boolean {
        return !before(periodStart(filter)) && before(periodEnd(filter))
    }

    private fun buildFlowPoints(
        transactions: List<Transaction>,
        filter: ReportsTimeFilter
    ): List<ReportFlowPoint> {
        val points = when (filter) {
            ReportsTimeFilter.WEEKLY -> (0..6).map { offset ->
                val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
                    time = periodStart(filter)
                    add(Calendar.DAY_OF_YEAR, offset)
                }
                FlowBucket(SimpleDateFormat("EEE", Locale.US).format(calendar.time), calendar, Calendar.DAY_OF_YEAR)
            }
            ReportsTimeFilter.MONTHLY -> (0..4).map { week ->
                val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
                    time = periodStart(filter)
                    add(Calendar.DAY_OF_YEAR, week * 7)
                }
                FlowBucket("W${week + 1}", calendar, Calendar.WEEK_OF_YEAR)
            }
            ReportsTimeFilter.YEARLY -> (0..11).map { month ->
                val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
                    time = periodStart(filter)
                    add(Calendar.MONTH, month)
                }
                FlowBucket(SimpleDateFormat("MMM", Locale.US).format(calendar.time), calendar, Calendar.MONTH)
            }
        }

        return points.map { bucket ->
            val bucketTransactions = transactions.filter { transaction ->
                transaction.transactionDate.toDate().isSameBucket(bucket.calendar, bucket.field)
            }
            ReportFlowPoint(
                label = bucket.label,
                incomeLkr = FinanceCalculator.roundMoney(
                    bucketTransactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.convertedAmountLkr }
                ),
                expenseLkr = FinanceCalculator.roundMoney(
                    bucketTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.convertedAmountLkr }
                )
            )
        }
    }

    private fun periodStart(filter: ReportsTimeFilter): java.util.Date {
        val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (filter) {
            ReportsTimeFilter.WEEKLY -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            ReportsTimeFilter.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            ReportsTimeFilter.YEARLY -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.time
    }

    private fun periodEnd(filter: ReportsTimeFilter): java.util.Date {
        val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
            time = periodStart(filter)
        }
        when (filter) {
            ReportsTimeFilter.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            ReportsTimeFilter.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            ReportsTimeFilter.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.time
    }

    private fun java.util.Date.isSameBucket(
        bucketCalendar: Calendar,
        field: Int
    ): Boolean {
        val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply { time = this@isSameBucket }
        return when (field) {
            Calendar.DAY_OF_YEAR -> calendar.get(Calendar.YEAR) == bucketCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == bucketCalendar.get(Calendar.DAY_OF_YEAR)
            Calendar.WEEK_OF_YEAR -> calendar.get(Calendar.YEAR) == bucketCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.WEEK_OF_YEAR) == bucketCalendar.get(Calendar.WEEK_OF_YEAR)
            Calendar.MONTH -> calendar.get(Calendar.YEAR) == bucketCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == bucketCalendar.get(Calendar.MONTH)
            else -> false
        }
    }

    private fun IncomeSource.displayLabel(): String {
        return when (this) {
            IncomeSource.SALARY -> "Salary"
            IncomeSource.FREELANCE -> "Freelance"
            IncomeSource.ADSENSE -> "AdSense"
            IncomeSource.CRYPTO -> "Crypto"
            IncomeSource.INVESTMENT -> "Investment"
            IncomeSource.REFUND -> "Refund"
            IncomeSource.OTHER -> "Other"
        }
    }

    override fun onCleared() {
        reportsScope.cancel()
        super.onCleared()
    }
}

private data class ReportsSourceData(
    val baseCurrency: String,
    val transactions: List<Transaction>,
    val recurringPayments: List<RecurringPayment>,
    val paymentFollowUps: List<PaymentFollowUp>
)

private data class FlowBucket(
    val label: String,
    val calendar: Calendar,
    val field: Int
)

private fun ReportsTimeFilter.label(): String {
    return when (this) {
        ReportsTimeFilter.WEEKLY -> "Weekly"
        ReportsTimeFilter.MONTHLY -> "Monthly"
        ReportsTimeFilter.YEARLY -> "Yearly"
    }
}
