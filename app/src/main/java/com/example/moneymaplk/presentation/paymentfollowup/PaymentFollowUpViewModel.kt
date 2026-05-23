package com.example.moneymaplk.presentation.paymentfollowup

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebasePaymentFollowUpRepository
import com.example.moneymaplk.data.repository.FirebaseRecurringPaymentRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.PaymentFollowUpRepository
import com.example.moneymaplk.data.repository.RecurringPaymentRepository
import com.example.moneymaplk.data.repository.TransactionRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.calculation.RecurringScheduleCalculator
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import com.example.moneymaplk.domain.validation.FinanceValidation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
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

class PaymentFollowUpViewModel(
    private val paymentFollowUpRepository: PaymentFollowUpRepository = FirebasePaymentFollowUpRepository(),
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository(),
    private val recurringPaymentRepository: RecurringPaymentRepository = FirebaseRecurringPaymentRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val paymentFollowUpScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(PaymentFollowUpUiState())
    val uiState: StateFlow<PaymentFollowUpUiState> = _uiState.asStateFlow()
    private var paymentFollowUpJob: Job? = null
    private var recurringJob: Job? = null
    private var profileJob: Job? = null

    fun loadFollowUps() {
        val userId = paymentFollowUpRepository.currentUserId
        if (userId == null) {
            _uiState.update {
                it.copy(isLoading = false, shouldReturnToLogin = true, errorMessage = null)
            }
            return
        }

        if (paymentFollowUpJob == null) {
            paymentFollowUpJob = paymentFollowUpRepository.observePaymentFollowUps(userId)
                .onEach { result ->
                    result
                        .onSuccess { paymentFollowUps ->
                            _uiState.update { state ->
                                val followUpItems = paymentFollowUps.map { followUp ->
                                    FollowUpListItem(
                                        followUp = followUp,
                                        effectiveStatus = FinanceCalculator.calculateEffectiveFollowUpStatus(followUp)
                                    )
                                }
                                state.copy(
                                    isLoading = false,
                                    followUps = followUpItems,
                                    waitingAmountLkr = followUpItems
                                        .filter { item -> item.effectiveStatus == PaymentFollowUpStatus.EXPECTED }
                                        .sumOf { item -> item.followUp.convertedAmountLkr },
                                    overdueAmountLkr = followUpItems
                                        .filter { item -> item.effectiveStatus == PaymentFollowUpStatus.OVERDUE }
                                        .sumOf { item -> item.followUp.convertedAmountLkr },
                                    dueSoonCount = followUpItems.count { item -> item.isDueSoon() },
                                    errorMessage = null
                                ).withDerivedLists()
                            }
                        }
                        .onFailure {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    errorMessage = "Could not load payment follow-ups. Please try again."
                                )
                            }
                        }
                }
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Could not load payment follow-ups. Please try again."
                        )
                    }
                }
                .launchIn(paymentFollowUpScope)
        }

        if (profileJob == null) {
            profileJob = userRepository.observeUserProfile(userId)
                .onEach { result ->
                    result.onSuccess { profile ->
                        profile ?: return@onSuccess
                        val baseCurrency = profile.defaultCurrency.ifBlank { "LKR" }.uppercase(Locale.US)
                        _uiState.update {
                            it.copy(
                                baseCurrency = baseCurrency,
                                currencyRatesToBase = profile.currencyRatesToBase + (baseCurrency to 1.0)
                            )
                        }
                    }
                }
                .catch { }
                .launchIn(paymentFollowUpScope)
        }

        if (recurringJob == null) {
            recurringJob = recurringPaymentRepository.observeRecurringPayments(userId)
                .onEach { result ->
                    result
                        .onSuccess { recurringPayments ->
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    recurringPayments = recurringPayments.filter { payment -> payment.isActive },
                                    errorMessage = null
                                ).withDerivedLists()
                            }
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
                .launchIn(paymentFollowUpScope)
        }

        _uiState.update { state -> state.copy(isLoading = state.followUps.isEmpty() && state.recurringPayments.isEmpty()) }
    }

    fun retryFollowUps() {
        paymentFollowUpJob?.cancel()
        recurringJob?.cancel()
        paymentFollowUpJob = null
        recurringJob = null
        loadFollowUps()
    }

    fun resetSessionState() {
        paymentFollowUpJob?.cancel()
        recurringJob?.cancel()
        profileJob?.cancel()
        paymentFollowUpJob = null
        recurringJob = null
        profileJob = null
        _uiState.value = PaymentFollowUpUiState()
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectBoard(board: FollowUpBoard) {
        _uiState.update { it.copy(board = board).withDerivedLists() }
    }

    fun selectActiveTab(tab: ActiveFollowUpTab) {
        _uiState.update { it.copy(activeTab = tab).withDerivedLists() }
    }

    fun selectHistoryTab(tab: HistoryFollowUpTab) {
        _uiState.update { it.copy(historyTab = tab).withDerivedLists() }
    }

    fun showAddFollowUpForm() {
        _uiState.update {
            it.copy(
                isAddFollowUpVisible = true,
                editingFollowUpId = null,
                manualType = TransactionType.INCOME,
                reasonProject = "",
                referenceNumber = "",
                expectedAmount = "",
                currency = "LKR",
                exchangeRateToLkr = "310",
                dueDate = "",
                followUpDate = "",
                followUpNote = "",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun showEditFollowUpForm(followUpId: String) {
        val item = _uiState.value.followUps.firstOrNull { it.followUp.followUpId == followUpId } ?: return
        val followUp = item.followUp
        val note = followUp.followUpNote.substringAfter(" - ", followUp.followUpNote)
        _uiState.update {
            it.copy(
                isAddFollowUpVisible = true,
                editingFollowUpId = followUp.followUpId,
                manualType = followUp.type,
                reasonProject = followUp.title,
                referenceNumber = "",
                expectedAmount = FinanceCalculator.roundMoney(followUp.originalAmount).toString(),
                currency = followUp.originalCurrency,
                exchangeRateToLkr = FinanceCalculator.roundRate(followUp.exchangeRateToLkr).toString(),
                dueDate = formatDateForInput(followUp.expectedDate.toDate()),
                followUpDate = "",
                followUpNote = note,
                reasonProjectError = null,
                expectedAmountError = null,
                exchangeRateError = null,
                dueDateError = null,
                followUpDateError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun hideAddFollowUpForm() {
        _uiState.update {
            it.copy(
                isAddFollowUpVisible = false,
                editingFollowUpId = null,
                reasonProjectError = null,
                expectedAmountError = null,
                exchangeRateError = null,
                dueDateError = null,
                followUpDateError = null,
                errorMessage = null
            )
        }
    }

    fun onReasonProjectChange(value: String) {
        _uiState.update { it.copy(reasonProject = value, reasonProjectError = null, errorMessage = null) }
    }

    fun onManualTypeChange(type: TransactionType) {
        _uiState.update { it.copy(manualType = type, errorMessage = null) }
    }

    fun onReferenceNumberChange(value: String) {
        _uiState.update { it.copy(referenceNumber = value, errorMessage = null) }
    }

    fun onExpectedAmountChange(value: String) {
        _uiState.update { it.copy(expectedAmount = value, expectedAmountError = null, errorMessage = null) }
    }

    fun onCurrencyChange(currency: String) {
        _uiState.update { it.copy(currency = currency, exchangeRateError = null, errorMessage = null) }
    }

    fun onExchangeRateChange(value: String) {
        _uiState.update { it.copy(exchangeRateToLkr = value, exchangeRateError = null, errorMessage = null) }
    }

    fun onDueDateChange(value: String) {
        _uiState.update { it.copy(dueDate = value, dueDateError = null, errorMessage = null) }
    }

    fun onFollowUpDateChange(value: String) {
        _uiState.update { it.copy(followUpDate = value, followUpDateError = null, errorMessage = null) }
    }

    fun onFollowUpNoteChange(value: String) {
        _uiState.update { it.copy(followUpNote = value, errorMessage = null) }
    }

    fun saveFollowUp() {
        val followUp = buildValidatedPaymentFollowUp() ?: return

        paymentFollowUpScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val isEditing = _uiState.value.editingFollowUpId != null
            val result = if (isEditing) {
                paymentFollowUpRepository.updatePaymentFollowUp(followUp)
            } else {
                paymentFollowUpRepository.savePaymentFollowUp(followUp).map { Unit }
            }

            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddFollowUpVisible = false,
                            editingFollowUpId = null,
                            manualType = TransactionType.INCOME,
                            reasonProject = "",
                            referenceNumber = "",
                            expectedAmount = "",
                            currency = "LKR",
                            exchangeRateToLkr = "310",
                            dueDate = "",
                            followUpDate = "",
                            followUpNote = "",
                            reasonProjectError = null,
                            expectedAmountError = null,
                            exchangeRateError = null,
                            dueDateError = null,
                            followUpDateError = null,
                            errorMessage = null,
                            successMessage = if (isEditing) "Payment follow up updated." else "Payment follow up created."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = if (isEditing) {
                                "Could not update this follow-up. Please try again."
                            } else {
                                "Could not save this follow-up. Please try again."
                            }
                        )
                    }
                }
        }
    }

    fun prepareConfirmFollowUp(followUpId: String) {
        val followUp = _uiState.value.followUps
            .firstOrNull { item -> item.followUp.followUpId == followUpId }
            ?.followUp ?: return
        val state = _uiState.value
        if (followUp.originalCurrency == state.baseCurrency) {
            markPaid(followUpId, exchangeRateOverride = null)
            return
        }
        _uiState.update {
            it.copy(
                confirmExchangeFollowUpId = followUpId,
                confirmExchangeRate = (it.currencyRatesToBase[followUp.originalCurrency]
                    ?: followUp.exchangeRateToLkr).toString().removeSuffix(".0"),
                confirmExchangeRateError = null
            )
        }
    }

    fun onConfirmExchangeRateChange(value: String) {
        _uiState.update { it.copy(confirmExchangeRate = value, confirmExchangeRateError = null) }
    }

    fun cancelConfirmExchangeRate() {
        _uiState.update {
            it.copy(confirmExchangeFollowUpId = null, confirmExchangeRate = "", confirmExchangeRateError = null)
        }
    }

    fun confirmWithExchangeRate() {
        val followUpId = _uiState.value.confirmExchangeFollowUpId ?: return
        val rate = _uiState.value.confirmExchangeRate.toDoubleOrNull()
        if (rate == null || rate <= 0.0) {
            _uiState.update { it.copy(confirmExchangeRateError = "Please check the exchange rate.") }
            return
        }
        _uiState.update { it.copy(confirmExchangeFollowUpId = null, confirmExchangeRateError = null) }
        markPaid(followUpId, exchangeRateOverride = rate)
    }

    private fun markPaid(followUpId: String, exchangeRateOverride: Double?) {
        val userId = paymentFollowUpRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }
        val followUp = _uiState.value.followUps
            .firstOrNull { item -> item.followUp.followUpId == followUpId }
            ?.followUp ?: return

        paymentFollowUpScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            val transactionId = transactionRepository.saveTransactionAndReturnId(
                followUp.toTransaction(
                    baseCurrency = _uiState.value.baseCurrency,
                    exchangeRateOverride = exchangeRateOverride
                )
            )
                .getOrElse {
                    _uiState.update { state ->
                        state.copy(errorMessage = "Could not confirm this expected item. Please try again.")
                    }
                    return@launch
                }

            if (followUp.recurringPaymentId != null) {
                recurringPaymentRepository.loadRecurringPayments(userId)
                    .getOrNull()
                    ?.firstOrNull { payment -> payment.paymentId == followUp.recurringPaymentId }
                    ?.let { payment ->
                        val nextDueDate = RecurringScheduleCalculator.nextDateWithinRepeatEnd(
                            currentDueDate = payment.nextDueDate,
                            frequency = payment.frequency,
                            repeatEndDate = payment.repeatEndDate
                        )
                        recurringPaymentRepository.markOccurrenceConfirmed(payment, nextDueDate)
                    }
            }

            paymentFollowUpRepository.markPaymentFollowUpConfirmed(userId, followUpId, transactionId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Confirmed.") }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(errorMessage = "Could not update this follow-up. Please try again.")
                    }
                }
        }
    }

    fun skipFollowUp(followUpId: String) {
        val userId = paymentFollowUpRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }
        val followUp = _uiState.value.followUps
            .firstOrNull { item -> item.followUp.followUpId == followUpId }
            ?.followUp

        paymentFollowUpScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            if (followUp?.recurringPaymentId != null) {
                recurringPaymentRepository.loadRecurringPayments(userId)
                    .getOrNull()
                    ?.firstOrNull { payment -> payment.paymentId == followUp.recurringPaymentId }
                    ?.let { payment ->
                        val nextDueDate = RecurringScheduleCalculator.nextDateWithinRepeatEnd(
                            currentDueDate = payment.nextDueDate,
                            frequency = payment.frequency,
                            repeatEndDate = payment.repeatEndDate
                        )
                        recurringPaymentRepository.markOccurrenceSkipped(payment, nextDueDate)
                    }
            }
            paymentFollowUpRepository.markPaymentFollowUpSkipped(userId, followUpId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Skipped.") }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(errorMessage = "Could not skip this follow-up. Please try again.")
                    }
                }
        }
    }

    fun deleteHistoryFollowUp(followUpId: String) {
        val userId = paymentFollowUpRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        paymentFollowUpScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            paymentFollowUpRepository.deletePaymentFollowUp(userId, followUpId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Deleted from history.") }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Could not delete this follow-up. Please try again.") }
                }
        }
    }

    private fun buildValidatedPaymentFollowUp(): PaymentFollowUp? {
        val state = _uiState.value
        val userId = paymentFollowUpRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return null
        }

        val amountResult = FinanceValidation.validatePositiveAmount(state.expectedAmount)
        val currencyResult = FinanceValidation.validateCurrency(state.currency)
        val exchangeRateResult = FinanceValidation.validateExchangeRate(
            currency = currencyResult.value ?: "LKR",
            value = state.exchangeRateToLkr
        )
        val dueDate = parseDate(state.dueDate.trim())
        val followUpDate: Date? = null

        val reasonProjectError = if (state.reasonProject.isBlank()) "Name is required" else null
        val expectedAmountError = amountResult.errorMessage
        val exchangeRateError = currencyResult.errorMessage ?: exchangeRateResult.errorMessage
        val dueDateError = when {
            state.dueDate.isBlank() -> "Expected payment date is required"
            dueDate == null -> "Use YYYY-MM-DD, for example 2026-05-25"
            else -> null
        }
        val followUpDateError = if (state.followUpDate.isNotBlank() && followUpDate == null) {
            "Use YYYY-MM-DD, for example 2026-05-24"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                reasonProjectError = reasonProjectError,
                expectedAmountError = expectedAmountError,
                exchangeRateError = exchangeRateError,
                dueDateError = dueDateError,
                followUpDateError = followUpDateError,
                errorMessage = null,
                successMessage = null
            )
        }

        return if (
            reasonProjectError == null &&
            expectedAmountError == null &&
            exchangeRateError == null &&
            dueDateError == null &&
            followUpDateError == null &&
            dueDate != null
        ) {
            val safeAmount = FinanceCalculator.roundMoney(amountResult.value ?: 0.0)
            val safeExchangeRate = exchangeRateResult.value ?: 1.0
            val convertedAmountLkr = FinanceCalculator.convertToLkr(
                originalAmount = safeAmount,
                originalCurrency = state.currency,
                exchangeRateToLkr = safeExchangeRate
            )

            PaymentFollowUp(
                followUpId = state.editingFollowUpId.orEmpty(),
                userId = userId,
                type = state.manualType,
                title = state.reasonProject.trim(),
                category = state.reasonProject.trim(),
                incomeSource = if (state.manualType == TransactionType.INCOME) IncomeSource.OTHER else null,
                referenceNumber = "",
                status = PaymentFollowUpStatus.EXPECTED,
                originalAmount = safeAmount,
                originalCurrency = state.currency,
                exchangeRateToLkr = safeExchangeRate,
                convertedAmountLkr = convertedAmountLkr,
                expectedDate = Timestamp(dueDate),
                followUpDate = null,
                paymentMethod = "Other",
                followUpNote = state.followUpNote.trim(),
                isCommitted = false,
                isDiscretionary = state.manualType == TransactionType.EXPENSE
            )
        } else {
            null
        }
    }

    private fun PaymentFollowUpUiState.withDerivedLists(): PaymentFollowUpUiState {
        val activeFollowUps = followUps
            .filter { it.effectiveStatus == PaymentFollowUpStatus.EXPECTED || it.effectiveStatus == PaymentFollowUpStatus.OVERDUE }
        val recurringIdsWithActiveFollowUps = activeFollowUps
            .mapNotNull { item -> item.followUp.recurringPaymentId }
            .toSet()
        val activeFollowUpEntries = activeFollowUps
            .map { FollowUpBoardItem.FollowUpEntry(it) }

        val activeRecurringEntries = recurringPayments
            .filterNot { payment -> recurringIdsWithActiveFollowUps.contains(payment.paymentId) }
            .map { FollowUpBoardItem.RecurringEntry(it) }
        val activeCombined = (activeFollowUpEntries + activeRecurringEntries)

        val activeItems = when (activeTab) {
            ActiveFollowUpTab.UPCOMING -> activeCombined
                .filter { item -> item.isUpcoming() }
                .sortedBy { item -> item.dueMillis() }
            ActiveFollowUpTab.DUE_TODAY -> activeCombined
                .filter { item -> item.isDueToday() }
                .sortedBy { item -> item.dueMillis() }
            ActiveFollowUpTab.OVERDUE -> activeCombined
                .filter { item -> item.isOverdue() }
                .sortedBy { item -> item.dueMillis() }
        }

        val historyItems = when (historyTab) {
            HistoryFollowUpTab.CONFIRMED -> followUps
                .filter { it.effectiveStatus == PaymentFollowUpStatus.CONFIRMED }
                .sortedByDescending { it.processedSortMillis() }
            HistoryFollowUpTab.SKIPPED -> followUps
                .filter { it.effectiveStatus == PaymentFollowUpStatus.SKIPPED }
                .sortedByDescending { it.processedSortMillis() }
        }

        return copy(activeVisibleItems = activeItems, historyVisibleItems = historyItems)
    }

    private fun FollowUpBoardItem.dueDate(): Date {
        return when (this) {
            is FollowUpBoardItem.FollowUpEntry -> item.followUp.expectedDate.toDate()
            is FollowUpBoardItem.RecurringEntry -> payment.nextDueDate.toDate()
        }
    }

    private fun FollowUpBoardItem.dueMillis(): Long = dueDate().time

    private fun FollowUpBoardItem.isDueToday(): Boolean = sameDay(dueDate(), startOfToday())

    private fun FollowUpBoardItem.isOverdue(): Boolean = dueDate().before(startOfToday())

    private fun FollowUpBoardItem.isUpcoming(): Boolean = !isDueToday() && !isOverdue()

    private fun FollowUpListItem.processedSortMillis(): Long {
        return followUp.confirmedDate?.toDate()?.time
            ?: followUp.skippedDate?.toDate()?.time
            ?: followUp.updatedAt?.toDate()?.time
            ?: followUp.expectedDate.toDate().time
    }

    private fun FollowUpListItem.isDueSoon(): Boolean {
        if (effectiveStatus != PaymentFollowUpStatus.EXPECTED) return false

        val today = startOfToday()
        val soon = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, 7)
        }
        val reminderDate = followUp.followUpDate?.toDate() ?: followUp.expectedDate.toDate()
        return !reminderDate.before(today) && !reminderDate.after(soon.time)
    }

    private fun PaymentFollowUp.toTransaction(
        baseCurrency: String,
        exchangeRateOverride: Double?
    ): Transaction {
        val confirmedTransactionDate = expectedDate
        val rate = exchangeRateOverride ?: exchangeRateToLkr
        val convertedAmount = FinanceCalculator.convertToLkr(
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRateToLkr = rate,
            baseCurrency = baseCurrency
        )
        return Transaction(
            userId = userId,
            type = type,
            title = title,
            category = category,
            incomeSource = if (type == TransactionType.INCOME) incomeSource ?: IncomeSource.OTHER else null,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRateToLkr = rate,
            convertedAmountLkr = convertedAmount,
            transactionDate = confirmedTransactionDate,
            monthId = SimpleDateFormat("yyyy-MM", Locale.US).format(confirmedTransactionDate.toDate()),
            paymentMethod = paymentMethod,
            note = followUpNote,
            isCommitted = type == TransactionType.EXPENSE && isCommitted,
            isDiscretionary = type == TransactionType.EXPENSE && isDiscretionary,
            isRecurring = isRecurring,
            recurringPaymentId = recurringPaymentId,
            followUpId = followUpId
        )
    }

    private fun sameDay(first: Date, second: Date): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(first) == formatter.format(second)
    }

    private fun startOfToday(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun parseDate(value: String): Date? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        return runCatching {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatter.isLenient = false
            formatter.parse(value)
        }.getOrNull()
    }

    private fun formatDateForInput(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    override fun onCleared() {
        paymentFollowUpScope.cancel()
        super.onCleared()
    }
}
