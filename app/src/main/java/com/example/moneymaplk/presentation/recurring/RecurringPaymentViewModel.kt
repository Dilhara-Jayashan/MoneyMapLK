package com.example.moneymaplk.presentation.recurring

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebaseRecurringPaymentRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.data.repository.RecurringPaymentRepository
import com.example.moneymaplk.data.repository.TransactionRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.calculation.RecurringScheduleCalculator
import com.example.moneymaplk.domain.model.FinanceCategories
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import com.example.moneymaplk.domain.validation.FinanceValidation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecurringPaymentViewModel(
    private val recurringPaymentRepository: RecurringPaymentRepository = FirebaseRecurringPaymentRepository(),
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository()
) : ViewModel() {

    private val recurringScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(RecurringPaymentUiState())
    val uiState: StateFlow<RecurringPaymentUiState> = _uiState.asStateFlow()
    private var recurringPaymentsJob: Job? = null
    private val autoConfirmingIds = mutableSetOf<String>()

    fun loadRecurringPayments() {
        val userId = recurringPaymentRepository.currentUserId
        if (userId == null) {
            _uiState.update {
                it.copy(isLoading = false, shouldReturnToLogin = true, errorMessage = null)
            }
            return
        }

        if (recurringPaymentsJob != null) return

        _uiState.update { state ->
            state.copy(
                isLoading = state.upcomingPayments.isEmpty() &&
                    state.dueTodayPayments.isEmpty() &&
                    state.overduePayments.isEmpty(),
                errorMessage = null,
                shouldReturnToLogin = false
            )
        }

        recurringPaymentsJob = recurringPaymentRepository.observeRecurringPayments(userId)
            .onEach { result ->
                result
                    .onSuccess { payments ->
                        publishPayments(payments)
                        processAutoConfirmPayments(payments)
                    }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = "Could not load recurrent items. Please try again."
                            )
                        }
                    }
            }
            .catch {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "Could not load recurrent items. Please try again."
                    )
                }
            }
            .launchIn(recurringScope)
    }

    fun retryRecurringPayments() {
        recurringPaymentsJob?.cancel()
        recurringPaymentsJob = null
        loadRecurringPayments()
    }

    fun resetSessionState() {
        recurringPaymentsJob?.cancel()
        recurringPaymentsJob = null
        autoConfirmingIds.clear()
        _uiState.value = RecurringPaymentUiState()
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectTab(tab: RecurrentTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showAddPaymentForm() {
        _uiState.update {
            RecurringPaymentUiState(
                upcomingPayments = it.upcomingPayments,
                dueTodayPayments = it.dueTodayPayments,
                overduePayments = it.overduePayments,
                pausedPayments = it.pausedPayments,
                totalMonthlyCommitmentsLkr = it.totalMonthlyCommitmentsLkr,
                activeIncomeCount = it.activeIncomeCount,
                activePaymentCount = it.activePaymentCount,
                overdueCount = it.overdueCount,
                selectedTab = it.selectedTab,
                isAddOrEditVisible = true
            )
        }
    }

    fun showEditPaymentForm(payment: RecurringPayment) {
        _uiState.update {
            it.copy(
                isAddOrEditVisible = true,
                editingPaymentId = payment.paymentId,
                type = payment.type,
                title = payment.title,
                category = payment.category,
                amount = payment.originalAmount.toString(),
                currency = payment.originalCurrency,
                exchangeRateToLkr = payment.exchangeRateToLkr.toString(),
                frequency = payment.frequency,
                nextDueDate = formatInputDate(payment.nextDueDate.toDate()),
                repeatEndDate = payment.repeatEndDate?.toDate()?.let(::formatInputDate).orEmpty(),
                paymentMethod = payment.paymentMethod,
                isCommitted = payment.isCommitted,
                isDiscretionary = payment.isDiscretionary,
                autoConfirm = payment.autoConfirm,
                note = payment.note,
                titleError = null,
                categoryError = null,
                amountError = null,
                exchangeRateError = null,
                frequencyError = null,
                nextDueDateError = null,
                repeatEndDateError = null,
                paymentMethodError = null,
                expenseTypeError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun hideAddPaymentForm() {
        _uiState.update {
            it.copy(
                isAddOrEditVisible = false,
                editingPaymentId = null,
                titleError = null,
                categoryError = null,
                amountError = null,
                exchangeRateError = null,
                frequencyError = null,
                nextDueDateError = null,
                repeatEndDateError = null,
                paymentMethodError = null,
                expenseTypeError = null,
                errorMessage = null
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            if (it.editingPaymentId != null) return@update it
            it.copy(
                type = type,
                category = "",
                isCommitted = type == TransactionType.EXPENSE,
                isDiscretionary = false,
                paymentMethod = if (type == TransactionType.INCOME) "Income" else it.paymentMethod,
                categoryError = null,
                expenseTypeError = null,
                errorMessage = null
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null, errorMessage = null) }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value, categoryError = null, errorMessage = null) }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value, amountError = null, errorMessage = null) }
    }

    fun onCurrencyChange(currency: String) {
        _uiState.update { it.copy(currency = currency, exchangeRateError = null, errorMessage = null) }
    }

    fun onExchangeRateChange(value: String) {
        _uiState.update { it.copy(exchangeRateToLkr = value, exchangeRateError = null, errorMessage = null) }
    }

    fun onFrequencyChange(frequency: RecurringFrequency) {
        _uiState.update { it.copy(frequency = frequency, frequencyError = null, repeatEndDateError = null, errorMessage = null) }
    }

    fun onNextDueDateChange(value: String) {
        _uiState.update { it.copy(nextDueDate = value, nextDueDateError = null, repeatEndDateError = null, errorMessage = null) }
    }

    fun onRepeatEndDateChange(value: String) {
        _uiState.update { it.copy(repeatEndDate = value, repeatEndDateError = null, errorMessage = null) }
    }

    fun onPaymentMethodChange(value: String) {
        _uiState.update { it.copy(paymentMethod = value, paymentMethodError = null, errorMessage = null) }
    }

    fun onExpenseTypeChange(committed: Boolean) {
        _uiState.update {
            it.copy(
                isCommitted = committed,
                isDiscretionary = !committed,
                expenseTypeError = null,
                errorMessage = null
            )
        }
    }

    fun onAutoConfirmChange(value: Boolean) {
        _uiState.update { it.copy(autoConfirm = value, errorMessage = null) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value, errorMessage = null) }
    }

    fun saveRecurringPayment() {
        val payment = buildValidatedRecurringPayment() ?: return

        recurringScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val result = if (payment.paymentId.isBlank()) {
                recurringPaymentRepository.saveRecurringPayment(payment)
            } else {
                recurringPaymentRepository.updateRecurringPayment(payment)
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddOrEditVisible = false,
                            editingPaymentId = null,
                            title = "",
                            category = "",
                            amount = "",
                            currency = "LKR",
                            exchangeRateToLkr = "310",
                            frequency = RecurringFrequency.MONTHLY,
                            nextDueDate = "",
                            repeatEndDate = "",
                            paymentMethod = "",
                            isCommitted = true,
                            isDiscretionary = false,
                            autoConfirm = false,
                            note = "",
                            successMessage = if (payment.paymentId.isBlank()) {
                                "Recurrent item added."
                            } else {
                                "Recurrent item updated."
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Could not save recurrent item. Please try again."
                        )
                    }
                }
        }
    }

    fun confirmPayment(payment: RecurringPayment) {
        if (
            !RecurringScheduleCalculator.isDueToday(payment.nextDueDate) &&
            !RecurringScheduleCalculator.isOverdue(payment.nextDueDate)
        ) {
            _uiState.update {
                it.copy(errorMessage = "You can confirm this recurrence on its due date.")
            }
            return
        }
        recurringScope.launch {
            confirmOccurrence(payment, showMessage = true)
        }
    }

    fun skipPayment(payment: RecurringPayment) {
        val nextDueDate = RecurringScheduleCalculator.nextDateWithinRepeatEnd(
            currentDueDate = payment.nextDueDate,
            frequency = payment.frequency,
            repeatEndDate = payment.repeatEndDate
        )
        recurringScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            recurringPaymentRepository.markOccurrenceSkipped(payment, nextDueDate)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Occurrence skipped.") }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Could not skip this occurrence.") }
                }
        }
    }

    fun pausePayment(payment: RecurringPayment) {
        recurringScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            recurringPaymentRepository.pauseRecurringPayment(payment)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Recurrent item paused.") }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Could not pause this recurrent item.") }
                }
        }
    }

    fun resumePayment(payment: RecurringPayment) {
        recurringScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            recurringPaymentRepository.resumeRecurringPayment(payment)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Recurrent item activated.") }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Could not activate this recurrent item.") }
                }
        }
    }

    fun deletePayment(paymentId: String) {
        val userId = recurringPaymentRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        recurringScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            recurringPaymentRepository.deleteRecurringPayment(userId, paymentId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Recurrent item deleted.") }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Could not delete this recurrent item.") }
                }
        }
    }

    private suspend fun confirmOccurrence(
        payment: RecurringPayment,
        showMessage: Boolean
    ) {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        val nextDueDate = RecurringScheduleCalculator.nextDateWithinRepeatEnd(
            currentDueDate = payment.nextDueDate,
            frequency = payment.frequency,
            repeatEndDate = payment.repeatEndDate
        )
        val transaction = payment.toTransaction()
        transactionRepository.saveTransaction(transaction)
            .onSuccess {
                recurringPaymentRepository.markOccurrenceConfirmed(payment, nextDueDate)
                    .onSuccess {
                        if (showMessage) {
                            _uiState.update { state ->
                                state.copy(successMessage = "${payment.title} confirmed.")
                            }
                        }
                    }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(errorMessage = "Transaction was saved, but the recurrent item was not advanced.")
                        }
                    }
            }
            .onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Could not confirm this recurrent item.")
                }
            }
    }

    private fun processAutoConfirmPayments(payments: List<RecurringPayment>) {
        val dueAutoPayments = payments.filter { payment ->
            payment.isActive &&
                payment.autoConfirm &&
                payment.paymentId !in autoConfirmingIds &&
                (
                    RecurringScheduleCalculator.isDueToday(payment.nextDueDate) ||
                        RecurringScheduleCalculator.isOverdue(payment.nextDueDate)
                    )
        }
        dueAutoPayments.forEach { payment ->
            autoConfirmingIds.add(payment.paymentId)
            recurringScope.launch {
                confirmOccurrence(payment, showMessage = false)
                autoConfirmingIds.remove(payment.paymentId)
            }
        }
    }

    private fun publishPayments(payments: List<RecurringPayment>) {
        val sorted = payments.sortedBy { payment -> payment.nextDueDate.toDate().time }
        val active = sorted.filter { payment -> payment.isActive }
        val paused = sorted.filter { payment -> !payment.isActive }
        val overdue = active.filter { payment -> RecurringScheduleCalculator.isOverdue(payment.nextDueDate) }
        val dueToday = active.filter { payment -> RecurringScheduleCalculator.isDueToday(payment.nextDueDate) }
        val upcoming = active.filterNot { payment ->
            RecurringScheduleCalculator.isOverdue(payment.nextDueDate) ||
                RecurringScheduleCalculator.isDueToday(payment.nextDueDate)
        }
        val paymentCommitments = active
            .filter { payment ->
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

        _uiState.update {
            it.copy(
                isLoading = false,
                upcomingPayments = upcoming,
                dueTodayPayments = dueToday,
                overduePayments = overdue,
                pausedPayments = paused,
                totalMonthlyCommitmentsLkr = FinanceCalculator.roundMoney(paymentCommitments),
                activeIncomeCount = active.count { payment -> payment.type == TransactionType.INCOME },
                activePaymentCount = active.count { payment -> payment.type == TransactionType.EXPENSE },
                overdueCount = overdue.size,
                errorMessage = null
            )
        }
    }

    private fun buildValidatedRecurringPayment(): RecurringPayment? {
        val state = _uiState.value
        val userId = recurringPaymentRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return null
        }

        val amountResult = FinanceValidation.validatePositiveAmount(state.amount)
        val currencyResult = FinanceValidation.validateCurrency(state.currency)
        val exchangeRateResult = FinanceValidation.validateExchangeRate(
            currency = currencyResult.value ?: "LKR",
            value = state.exchangeRateToLkr
        )
        val dueDate = parseDate(state.nextDueDate.trim())
        val repeatEndDate = state.repeatEndDate.trim().takeIf { it.isNotBlank() }?.let(::parseDate)
        val existingPayment = allPayments().firstOrNull { payment -> payment.paymentId == state.editingPaymentId }
        val internalCategory = existingPayment?.category
            ?.takeIf { it.isNotBlank() }
            ?: state.category.takeIf { it.isNotBlank() }
            ?: state.title.takeIf { it.isNotBlank() }
            ?: if (state.type == TransactionType.INCOME) "Other" else "Other"

        val titleError = if (state.title.isBlank()) "Title is required" else null
        val amountError = amountResult.errorMessage
        val exchangeRateError = currencyResult.errorMessage ?: exchangeRateResult.errorMessage
        val nextDueDateError = when {
            state.nextDueDate.isBlank() -> "Next due date is required"
            dueDate == null -> "Use YYYY-MM-DD, for example 2026-05-25"
            dueDate.before(RecurringScheduleCalculator.startOfToday()) -> "Next due date cannot be in the past."
            else -> null
        }
        val repeatEndDateError = when {
            state.repeatEndDate.isBlank() -> null
            repeatEndDate == null -> "Use YYYY-MM-DD, for example 2026-12-31"
            repeatEndDate.before(RecurringScheduleCalculator.startOfToday()) -> "End date cannot be in the past."
            dueDate != null && !RecurringScheduleCalculator.canRepeatAtLeastOnce(
                startDate = dueDate,
                frequency = state.frequency,
                repeatEndDate = repeatEndDate
            ) -> "End date must allow at least one repeat."
            else -> null
        }
        val paymentMethodError = if (state.paymentMethod.isBlank()) "Payment method is required" else null
        val expenseTypeError = if (state.type == TransactionType.EXPENSE) {
            FinanceValidation.validateExpenseType(
                isCommitted = state.isCommitted,
                isDiscretionary = state.isDiscretionary
            ).errorMessage
        } else {
            null
        }

        _uiState.update {
            it.copy(
                titleError = titleError,
                categoryError = null,
                amountError = amountError,
                exchangeRateError = exchangeRateError,
                frequencyError = null,
                nextDueDateError = nextDueDateError,
                repeatEndDateError = repeatEndDateError,
                paymentMethodError = paymentMethodError,
                expenseTypeError = expenseTypeError,
                errorMessage = null,
                successMessage = null
            )
        }

        return if (
            titleError == null &&
            amountError == null &&
            exchangeRateError == null &&
            nextDueDateError == null &&
            repeatEndDateError == null &&
            paymentMethodError == null &&
            expenseTypeError == null &&
            dueDate != null
        ) {
            val safeAmount = FinanceCalculator.roundMoney(amountResult.value ?: 0.0)
            val safeExchangeRate = exchangeRateResult.value ?: 1.0
            val convertedAmountLkr = FinanceCalculator.convertToLkr(
                originalAmount = safeAmount,
                originalCurrency = state.currency,
                exchangeRateToLkr = safeExchangeRate
            )
            RecurringPayment(
                paymentId = state.editingPaymentId.orEmpty(),
                userId = userId,
                type = state.type,
                title = state.title.trim(),
                category = internalCategory.trim(),
                incomeSource = if (state.type == TransactionType.INCOME) {
                    existingPayment?.incomeSource
                        ?: FinanceCategories.incomeSourceForCategory(internalCategory)
                        ?: IncomeSource.OTHER
                } else {
                    null
                },
                originalAmount = safeAmount,
                originalCurrency = state.currency,
                exchangeRateToLkr = safeExchangeRate,
                convertedAmountLkr = convertedAmountLkr,
                frequency = state.frequency,
                nextDueDate = Timestamp(dueDate),
                repeatEndDate = repeatEndDate?.let { Timestamp(it) },
                lastPaidDate = existingPayment?.lastPaidDate,
                paymentMethod = state.paymentMethod.trim(),
                isActive = existingPayment?.isActive ?: true,
                isCommitted = state.type == TransactionType.EXPENSE &&
                    (existingPayment?.isCommitted ?: state.isCommitted),
                isDiscretionary = state.type == TransactionType.EXPENSE &&
                    (existingPayment?.isDiscretionary ?: state.isDiscretionary),
                isRecurring = true,
                autoCreateTransaction = state.autoConfirm,
                autoConfirm = state.autoConfirm,
                lastConfirmedDueDate = existingPayment?.lastConfirmedDueDate,
                lastSkippedDueDate = existingPayment?.lastSkippedDueDate,
                pausedAt = existingPayment?.pausedAt,
                note = state.note.trim()
            )
        } else {
            null
        }
    }

    private fun RecurringPayment.toTransaction(): Transaction {
        return Transaction(
            userId = userId,
            type = type,
            title = title,
            category = category,
            incomeSource = if (type == TransactionType.INCOME) incomeSource ?: IncomeSource.OTHER else null,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRateToLkr = exchangeRateToLkr,
            convertedAmountLkr = convertedAmountLkr,
            transactionDate = nextDueDate,
            monthId = monthIdFor(nextDueDate),
            paymentMethod = paymentMethod,
            note = note,
            isCommitted = type == TransactionType.EXPENSE && isCommitted,
            isDiscretionary = type == TransactionType.EXPENSE && isDiscretionary,
            isRecurring = true,
            recurringPaymentId = paymentId
        )
    }

    private fun allPayments(): List<RecurringPayment> {
        val state = _uiState.value
        return state.upcomingPayments +
            state.dueTodayPayments +
            state.overduePayments +
            state.pausedPayments
    }

    private fun parseDate(value: String): Date? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        return runCatching {
            inputDateFormatter().parse(value)
        }.getOrNull()
    }

    private fun formatInputDate(date: Date): String {
        return inputDateFormatter().format(date)
    }

    private fun monthIdFor(timestamp: Timestamp): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(timestamp.toDate())
    }

    private fun inputDateFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }

    override fun onCleared() {
        recurringScope.cancel()
        super.onCleared()
    }
}
