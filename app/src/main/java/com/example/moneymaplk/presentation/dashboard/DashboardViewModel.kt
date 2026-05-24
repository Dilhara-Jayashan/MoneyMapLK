package com.example.moneymaplk.presentation.dashboard

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
import com.example.moneymaplk.domain.model.Goal
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import com.example.moneymaplk.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository(),
    private val goalRepository: GoalRepository = FirebaseGoalRepository(),
    private val recurringPaymentRepository: RecurringPaymentRepository = FirebaseRecurringPaymentRepository(),
    private val paymentFollowUpRepository: PaymentFollowUpRepository = FirebasePaymentFollowUpRepository()
) : ViewModel() {

    private val dashboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var dashboardJob: Job? = null

    fun loadDashboard() {
        val userId = userRepository.currentUserId
        if (userId == null) {
            clearDashboardJob()
            _uiState.value = DashboardUiState(shouldReturnToLogin = true)
            return
        }

        val state = _uiState.value
        if (dashboardJob?.isActive == true && state.activeUserId == userId) return

        clearDashboardJob()
        _uiState.value = DashboardUiState(
            isLoading = true,
            activeUserId = userId
        )

        dashboardJob = combine(
            userRepository.observeUserProfile(userId),
            transactionRepository.observeTransactions(userId),
            goalRepository.observeGoals(userId),
            recurringPaymentRepository.observeRecurringPayments(userId),
            paymentFollowUpRepository.observePaymentFollowUps(userId)
        ) { profileResult, transactionResult, goalResult, recurringResult, followUpResult ->
            DashboardSourceData(
                profile = profileResult.getOrThrow(),
                transactions = transactionResult.getOrThrow(),
                goals = goalResult.getOrThrow(),
                recurringPayments = recurringResult.getOrThrow(),
                paymentFollowUps = followUpResult.getOrThrow()
            )
        }
            .onEach { sourceData ->
                if (userRepository.currentUserId != userId) return@onEach

                val profile = sourceData.profile
                if (profile == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeUserId = userId,
                            loadedUserId = null,
                            summary = null,
                            errorMessage = null,
                            isProfileMissing = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeUserId = userId,
                            loadedUserId = userId,
                            summary = buildDashboardSummary(
                                profile = profile,
                                transactions = sourceData.transactions,
                                goals = sourceData.goals,
                                recurringPayments = sourceData.recurringPayments,
                                paymentFollowUps = sourceData.paymentFollowUps
                            ),
                            errorMessage = null,
                            isProfileMissing = false
                        )
                    }
                }
            }
            .catch { throwable ->
                if (userRepository.currentUserId == userId) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeUserId = userId,
                            errorMessage = throwable.message ?: "Could not load your dashboard."
                        )
                    }
                }
            }
            .launchIn(dashboardScope)
    }

    fun retryDashboard() {
        clearDashboardJob()
        loadDashboard()
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionState() {
        clearDashboardJob()
        _uiState.value = DashboardUiState()
    }

    private fun clearDashboardJob() {
        dashboardJob?.cancel()
        dashboardJob = null
    }

    private fun buildDashboardSummary(
        profile: UserProfile,
        transactions: List<Transaction>,
        goals: List<Goal>,
        recurringPayments: List<RecurringPayment>,
        paymentFollowUps: List<PaymentFollowUp>
    ): DashboardSummary {
        val currentMonthId = currentMonthId()
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.monthId == currentMonthId
        }
        val monthlyIncomeLkr = FinanceCalculator.roundMoney(
            monthlyTransactions
                .filter { transaction -> transaction.type == TransactionType.INCOME }
                .sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val monthlyExpensesLkr = FinanceCalculator.roundMoney(
            monthlyTransactions
                .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                .sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val requiredSpendingLkr = FinanceCalculator.roundMoney(
            monthlyTransactions
                .filter { transaction ->
                    transaction.type == TransactionType.EXPENSE &&
                        transaction.isCommitted &&
                        !transaction.isDiscretionary
                }
                .sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val flexibleSpendingLkr = FinanceCalculator.roundMoney(
            monthlyTransactions
                .filter { transaction ->
                    transaction.type == TransactionType.EXPENSE &&
                        transaction.isDiscretionary &&
                        !transaction.isCommitted
                }
                .sumOf { transaction -> transaction.convertedAmountLkr }
        )
        val upcomingRequiredRecurringPaymentsLkr = recurringPayments
            .filter { payment ->
                payment.isActive &&
                    payment.type == TransactionType.EXPENSE &&
                    payment.isCommitted &&
                    !payment.isDiscretionary
            }
            .sumOf { payment ->
                FinanceCalculator.calculateMonthlyRecurringEstimate(
                    convertedAmountLkr = payment.convertedAmountLkr,
                    frequency = payment.frequency
                )
            }
            .let(FinanceCalculator::roundMoney)
        val activeRecurringIncome = recurringPayments.filter { payment ->
            payment.isActive && payment.type == TransactionType.INCOME
        }
        val activeRecurringPayments = recurringPayments.filter { payment ->
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
        val safeToSpend = FinanceCalculator.calculateSafeToSpend(
            currentSavingsLkr = profile.currentSavingsLkr,
            thisMonthIncomeLkr = monthlyIncomeLkr,
            thisMonthExpensesLkr = monthlyExpensesLkr,
            plannedSavingsAllocationLkr = profile.plannedSavingsAllocationLkr,
            safeToSpendBufferLkr = profile.safeToSpendBufferLkr,
            upcomingRequiredRecurringPaymentsLkr = upcomingRequiredRecurringPaymentsLkr
        )
        val recentTransactions = transactions
            .sortedByDescending { transaction -> transaction.transactionDate.toDate().time }
            .take(3)
        val expectedThisMonth = paymentFollowUps.filter { followUp ->
            currentMonthIdFormatter().format(followUp.expectedDate.toDate()) == currentMonthId &&
                FinanceCalculator.calculateEffectiveFollowUpStatus(followUp) == PaymentFollowUpStatus.EXPECTED
        }
        val expectedIncomeThisMonthLkr = expectedThisMonth
            .filter { followUp -> followUp.type == TransactionType.INCOME }
            .sumOf { followUp -> followUp.convertedAmountLkr }
            .let(FinanceCalculator::roundMoney)
        val expectedPaymentsThisMonthLkr = expectedThisMonth
            .filter { followUp -> followUp.type == TransactionType.EXPENSE }
            .sumOf { followUp -> followUp.convertedAmountLkr }
            .let(FinanceCalculator::roundMoney)
        val overdueFollowUpCount = paymentFollowUps.count { followUp ->
            FinanceCalculator.calculateEffectiveFollowUpStatus(followUp) == PaymentFollowUpStatus.OVERDUE
        }
        val firstOverdueFollowUp = paymentFollowUps
            .filter { followUp ->
                FinanceCalculator.calculateEffectiveFollowUpStatus(followUp) == PaymentFollowUpStatus.OVERDUE
            }
            .minByOrNull { followUp -> followUp.expectedDate.toDate().time }

        return DashboardSummary(
            displayName = profile.displayName.ifBlank { "MoneyMap LK User" },
            baseCurrency = profile.defaultCurrency.ifBlank { "LKR" },
            currentSavingsLkr = profile.currentSavingsLkr,
            monthlyIncomeLkr = monthlyIncomeLkr,
            monthlyExpensesLkr = monthlyExpensesLkr,
            monthlyNetLkr = FinanceCalculator.roundMoney(monthlyIncomeLkr - monthlyExpensesLkr),
            safeToSpendLkr = safeToSpend.displayAmountLkr,
            rawSafeToSpendLkr = safeToSpend.rawAmountLkr,
            safeToSpendHelperText = safeToSpendHelperText(
                isSafeToSpendDepleted = safeToSpend.isZeroOrNegative,
                upcomingRequiredRecurringPaymentsLkr = upcomingRequiredRecurringPaymentsLkr
            ),
            upcomingRequiredRecurringPaymentsLkr = upcomingRequiredRecurringPaymentsLkr,
            expectedIncomeThisMonthLkr = expectedIncomeThisMonthLkr,
            expectedPaymentsThisMonthLkr = expectedPaymentsThisMonthLkr,
            overdueFollowUpCount = overdueFollowUpCount,
            firstOverdueFollowUpTitle = firstOverdueFollowUp?.title,
            firstOverdueFollowUpAmountLkr = firstOverdueFollowUp?.convertedAmountLkr ?: 0.0,
            activeRecurringIncomeCount = activeRecurringIncome.size,
            activeRecurringPaymentCount = activeRecurringPayments.size,
            estimatedMonthlyRecurringIncomeLkr = estimatedMonthlyRecurringIncomeLkr,
            estimatedMonthlyRecurringPaymentLkr = estimatedMonthlyRecurringPaymentLkr,
            featuredGoal = goals.homeGoal(profile.selectedGoalId)?.toDashboardGoal(),
            recentTransactions = recentTransactions,
            requiredSpendingLkr = requiredSpendingLkr,
            flexibleSpendingLkr = flexibleSpendingLkr,
            smartInsight = buildSmartInsight(
                hasTransactions = transactions.isNotEmpty(),
                isSafeToSpendDepleted = safeToSpend.isZeroOrNegative,
                upcomingRequiredRecurringPaymentsLkr = upcomingRequiredRecurringPaymentsLkr,
                requiredSpendingLkr = requiredSpendingLkr,
                flexibleSpendingLkr = flexibleSpendingLkr
            )
        )
    }

    private fun safeToSpendHelperText(
        isSafeToSpendDepleted: Boolean,
        upcomingRequiredRecurringPaymentsLkr: Double
    ): String {
        val hasRequiredRecurringPayments = upcomingRequiredRecurringPaymentsLkr > 0.0
        return when {
            isSafeToSpendDepleted && hasRequiredRecurringPayments -> {
                "Your savings plan, expenses, recurring payments, and emergency buffer use up your available money."
            }
            isSafeToSpendDepleted -> {
                "Your savings plan, expenses, and emergency buffer use up your available money."
            }
            hasRequiredRecurringPayments -> {
                "Available after your savings plan, expenses, recurring payments, and emergency buffer."
            }
            else -> "Available after your savings plan, expenses, and emergency buffer."
        }
    }

    private fun buildSmartInsight(
        hasTransactions: Boolean,
        isSafeToSpendDepleted: Boolean,
        upcomingRequiredRecurringPaymentsLkr: Double,
        requiredSpendingLkr: Double,
        flexibleSpendingLkr: Double
    ): String {
        return when {
            isSafeToSpendDepleted && upcomingRequiredRecurringPaymentsLkr > 0.0 -> {
                "Your savings plan, expenses, recurring payments, and emergency buffer use up your available money."
            }
            isSafeToSpendDepleted -> "Your savings plan, expenses, and emergency buffer use up your available money."
            !hasTransactions -> "Add your first income or expense to unlock insights."
            flexibleSpendingLkr > requiredSpendingLkr -> "Flexible spending is higher than required spending this month."
            else -> "You are keeping your spending under control this month."
        }
    }

    private fun List<Goal>.homeGoal(selectedGoalId: String?): Goal? {
        val activeGoals = filter { goal -> goal.isActive }
            .sortedBy { goal -> goal.name.lowercase(Locale.US) }
        return selectedGoalId
            ?.let { goalId -> activeGoals.firstOrNull { goal -> goal.goalId == goalId } }
            ?: activeGoals.firstOrNull()
    }

    private fun Goal.toDashboardGoal(): DashboardGoal {
        return DashboardGoal(
            name = name,
            targetAmountLkr = targetAmountLkr,
            savedAmountLkr = savedAmountLkr,
            remainingAmountLkr = remainingAmountLkr,
            progressPercentage = progressPercentage
        )
    }

    private fun currentMonthId(): String {
        return currentMonthIdFormatter().format(Date())
    }

    private fun currentMonthIdFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
    }

    override fun onCleared() {
        dashboardScope.cancel()
        super.onCleared()
    }
}

private data class DashboardSourceData(
    val profile: UserProfile?,
    val transactions: List<Transaction>,
    val goals: List<Goal>,
    val recurringPayments: List<RecurringPayment>,
    val paymentFollowUps: List<PaymentFollowUp>
)
