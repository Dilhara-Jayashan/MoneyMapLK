package com.example.moneymaplk.presentation.transaction

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebasePaymentFollowUpRepository
import com.example.moneymaplk.data.repository.FirebaseQuickCategoryRepository
import com.example.moneymaplk.data.repository.FirebaseRecurringPaymentRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.PaymentFollowUpRepository
import com.example.moneymaplk.data.repository.QuickCategoryRepository
import com.example.moneymaplk.data.repository.RecurringPaymentRepository
import com.example.moneymaplk.data.repository.TransactionRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.calculation.RecurringScheduleCalculator
import com.example.moneymaplk.domain.model.FinanceCategories
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.QuickCategory
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import com.example.moneymaplk.domain.validation.FinanceValidation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.ZoneId
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

class TransactionViewModel(
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository(),
    private val quickCategoryRepository: QuickCategoryRepository = FirebaseQuickCategoryRepository(),
    private val recurringPaymentRepository: RecurringPaymentRepository = FirebaseRecurringPaymentRepository(),
    private val paymentFollowUpRepository: PaymentFollowUpRepository = FirebasePaymentFollowUpRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val transactionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()
    private val _listUiState = MutableStateFlow(TransactionListUiState())
    val listUiState: StateFlow<TransactionListUiState> = _listUiState.asStateFlow()
    private var transactionListenerJob: Job? = null
    private var categoryListenerJob: Job? = null
    private var profileListenerJob: Job? = null

    fun loadTransactions() {
        val userId = transactionRepository.currentUserId
        if (userId == null) {
            _listUiState.update {
                it.copy(
                    isLoading = false,
                    shouldReturnToLogin = true,
                    errorMessage = null
                )
            }
            return
        }

        if (transactionListenerJob != null) return

        _listUiState.update {
            it.copy(isLoading = true, errorMessage = null, shouldReturnToLogin = false)
        }
        transactionListenerJob = transactionRepository.observeTransactions(userId)
            .onEach { result ->
                result
                    .onSuccess { transactions ->
                        _listUiState.update { state ->
                            val sortedTransactions = sortNewestFirst(transactions)
                            state.copy(
                                isLoading = false,
                                transactions = sortedTransactions,
                                visibleTransactions = filterTransactions(
                                    transactions = sortedTransactions,
                                    filter = state.selectedFilter
                                ),
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _listUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = throwable.message ?: "Could not load transactions."
                            )
                        }
                    }
            }
            .catch { throwable ->
                _listUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Could not load transactions."
                    )
                }
            }
            .launchIn(transactionScope)
    }

    fun loadQuickCategories() {
        val userId = quickCategoryRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        if (categoryListenerJob != null) return

        loadCurrencySettings(userId)
        categoryListenerJob = quickCategoryRepository.observeCategories(userId)
            .onEach { result ->
                result
                    .onSuccess { userCategories ->
                        _uiState.update {
                            it.copy(
                                allQuickCategories = userCategories,
                                quickCategories = mergeQuickCategories(userCategories, it.type),
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(errorMessage = "Could not load quick categories.")
                        }
                    }
            }
            .catch {
                _uiState.update { it.copy(errorMessage = "Could not load quick categories.") }
            }
            .launchIn(transactionScope)
    }

    fun retryLoadTransactions() {
        transactionListenerJob?.cancel()
        transactionListenerJob = null
        loadTransactions()
    }

    fun resetSessionState() {
        transactionListenerJob?.cancel()
        categoryListenerJob?.cancel()
        profileListenerJob?.cancel()
        transactionListenerJob = null
        categoryListenerJob = null
        profileListenerJob = null
        _uiState.value = TransactionUiState()
        _listUiState.value = TransactionListUiState()
    }

    fun onTransactionFilterChange(filter: TransactionFilter) {
        _listUiState.update { state ->
            state.copy(
                selectedFilter = filter,
                visibleTransactions = filterTransactions(
                    transactions = state.transactions,
                    filter = filter
                )
            )
        }
    }

    fun startTransactionFlow(
        type: TransactionType,
        committed: Boolean = false
    ) {
        _uiState.update { state ->
            TransactionUiState(
                type = type,
                allQuickCategories = state.allQuickCategories,
                quickCategories = mergeQuickCategories(state.allQuickCategories, type),
                baseCurrency = state.baseCurrency,
                supportedCurrencies = state.supportedCurrencies,
                currencyRatesToBase = state.currencyRatesToBase,
                currency = state.baseCurrency,
                exchangeRateToLkr = exchangeRateTextFor(state.baseCurrency, state.baseCurrency, state.currencyRatesToBase),
                isCommitted = committed,
                isDiscretionary = type == TransactionType.EXPENSE && !committed,
                repeatFrequency = if (committed) {
                    RecurringFrequency.MONTHLY
                } else {
                    null
                }
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            val committed = if (type != it.type) false else it.isCommitted
            it.copy(
                type = type,
                category = "",
                quickCategories = mergeQuickCategories(it.allQuickCategories, type),
                selectedQuickCategoryId = null,
                incomeSource = null,
                repeatFrequency = if (committed) {
                    it.repeatFrequency ?: RecurringFrequency.MONTHLY
                } else {
                    null
                },
                repeatUntilDate = if (committed) {
                    it.repeatUntilDate
                } else {
                    ""
                },
                categoryError = null,
                incomeSourceError = null,
                spendingBehaviorError = null,
                repeatFrequencyError = null,
                repeatUntilDateError = null,
                errorMessage = null,
                isCommitted = committed,
                isDiscretionary = type == TransactionType.EXPENSE && !committed
            )
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value, amountError = null, errorMessage = null) }
    }

    fun onCurrencyChange(currency: String) {
        _uiState.update {
            val normalized = currency.trim().uppercase()
            it.copy(
                currency = normalized,
                exchangeRateToLkr = exchangeRateTextFor(normalized, it.baseCurrency, it.currencyRatesToBase),
                exchangeRateError = null,
                errorMessage = null
            )
        }
    }

    fun onExchangeRateChange(value: String) {
        _uiState.update {
            it.copy(exchangeRateToLkr = value, exchangeRateError = null, errorMessage = null)
        }
    }

    private fun loadCurrencySettings(userId: String) {
        if (profileListenerJob != null) return
        profileListenerJob = userRepository.observeUserProfile(userId)
            .onEach { result ->
                result.onSuccess { profile ->
                    profile ?: return@onSuccess
                    val baseCurrency = profile.defaultCurrency.ifBlank { "LKR" }.uppercase()
                    val supportedCurrencies = (profile.supportedCurrencies + baseCurrency)
                        .map { it.uppercase() }
                        .distinct()
                    val rates = profile.currencyRatesToBase + (baseCurrency to 1.0)
                    _uiState.update {
                        val selectedCurrency = it.currency.takeIf { currency ->
                            supportedCurrencies.contains(currency)
                        } ?: baseCurrency
                        it.copy(
                            baseCurrency = baseCurrency,
                            supportedCurrencies = supportedCurrencies,
                            currencyRatesToBase = rates,
                            currency = selectedCurrency,
                            exchangeRateToLkr = exchangeRateTextFor(selectedCurrency, baseCurrency, rates)
                        )
                    }
                }
            }
            .catch { }
            .launchIn(transactionScope)
    }

    fun onTitleChange(value: String) {
        _uiState.update {
            it.copy(
                title = value,
                titleError = null,
                selectedQuickCategoryId = null,
                category = "",
                incomeSource = if (it.type == TransactionType.INCOME) null else it.incomeSource,
                errorMessage = null
            )
        }
    }

    fun onCategoryChange(value: String) {
        _uiState.update {
            it.copy(
                category = value,
                selectedQuickCategoryId = null,
                incomeSource = if (it.type == TransactionType.INCOME) {
                    FinanceCategories.incomeSourceForCategory(value)
                } else {
                    null
                },
                categoryError = null,
                incomeSourceError = null,
                errorMessage = null
            )
        }
    }

    fun onTransactionDateChange(value: String) {
        _uiState.update {
            it.copy(transactionDate = value, transactionDateError = null, repeatUntilDateError = null)
        }
    }

    fun onSpendingBehaviorChange(committed: Boolean) {
        _uiState.update {
            it.copy(
                isCommitted = committed,
                isDiscretionary = it.type == TransactionType.EXPENSE && !committed,
                repeatFrequency = if (committed) it.repeatFrequency ?: RecurringFrequency.MONTHLY else null,
                repeatUntilDate = if (committed) it.repeatUntilDate else "",
                spendingBehaviorError = null,
                repeatFrequencyError = null,
                repeatUntilDateError = null,
                errorMessage = null
            )
        }
    }

    fun onRepeatingExpenseChange(isRepeating: Boolean) {
        onSpendingBehaviorChange(committed = isRepeating)
    }

    fun onRepeatFrequencyChange(frequency: RecurringFrequency) {
        _uiState.update {
            it.copy(repeatFrequency = frequency, repeatFrequencyError = null, errorMessage = null)
        }
    }

    fun onRepeatUntilDateChange(value: String) {
        _uiState.update {
            it.copy(repeatUntilDate = value, repeatUntilDateError = null, errorMessage = null)
        }
    }

    fun onPaymentMethodChange(value: String) {
        _uiState.update {
            it.copy(paymentMethod = value, paymentMethodError = null, errorMessage = null)
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value, errorMessage = null) }
    }

    fun toggleQuickCategoryManageMode() {
        _uiState.update { it.copy(isManagingQuickCategories = !it.isManagingQuickCategories) }
    }

    fun showAddQuickCategoryDialog() {
        _uiState.update {
            it.copy(
                showAddQuickCategoryDialog = true,
                quickCategoryName = it.title,
                quickCategoryCategory = it.title.ifBlank { it.category },
                quickCategoryPaymentMethod = it.paymentMethod,
                quickCategoryIsCommitted = it.isCommitted,
                quickCategoryFrequency = if (it.isCommitted) {
                    it.repeatFrequency ?: RecurringFrequency.MONTHLY
                } else {
                    null
                },
                quickCategoryRepeatUntilDate = if (it.isCommitted) {
                    it.repeatUntilDate
                } else {
                    ""
                },
                quickCategoryNameError = null,
                quickCategoryCategoryError = null,
                quickCategoryPaymentMethodError = null,
                quickCategoryFrequencyError = null,
                quickCategoryRepeatUntilDateError = null,
                errorMessage = null
            )
        }
    }

    fun hideAddQuickCategoryDialog() {
        _uiState.update {
            it.copy(
                showAddQuickCategoryDialog = false,
                quickCategoryNameError = null,
                quickCategoryCategoryError = null,
                quickCategoryPaymentMethodError = null,
                quickCategoryFrequencyError = null,
                quickCategoryRepeatUntilDateError = null,
                errorMessage = null
            )
        }
    }

    fun onQuickCategoryNameChange(value: String) {
        _uiState.update { it.copy(quickCategoryName = value, quickCategoryNameError = null) }
    }

    fun onQuickCategoryCategoryChange(value: String) {
        _uiState.update { it.copy(quickCategoryCategory = value, quickCategoryCategoryError = null) }
    }

    fun onQuickCategoryPaymentMethodChange(value: String) {
        _uiState.update { it.copy(quickCategoryPaymentMethod = value, quickCategoryPaymentMethodError = null) }
    }

    fun onQuickCategoryCommittedChange(committed: Boolean) {
        _uiState.update {
            it.copy(
                quickCategoryIsCommitted = committed,
                quickCategoryFrequency = if (committed) {
                    it.quickCategoryFrequency ?: RecurringFrequency.MONTHLY
                } else {
                    null
                },
                quickCategoryRepeatUntilDate = if (committed) it.quickCategoryRepeatUntilDate else "",
                quickCategoryFrequencyError = null,
                quickCategoryRepeatUntilDateError = null
            )
        }
    }

    fun onQuickCategoryFrequencyChange(frequency: RecurringFrequency) {
        _uiState.update { it.copy(quickCategoryFrequency = frequency, quickCategoryFrequencyError = null) }
    }

    fun onQuickCategoryRepeatUntilDateChange(value: String) {
        _uiState.update {
            it.copy(quickCategoryRepeatUntilDate = value, quickCategoryRepeatUntilDateError = null)
        }
    }

    fun saveQuickCategoryFromForm() {
        if (_uiState.value.isSavingCategory) return
        val category = buildValidatedQuickCategoryFromForm() ?: return
        val userId = quickCategoryRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        transactionScope.launch {
            _uiState.update { it.copy(isSavingCategory = true, errorMessage = null) }
            quickCategoryRepository.addCategory(userId, category)
                .onSuccess { savedCategory ->
                    _uiState.update {
                        val allCategories = upsertQuickCategory(it.allQuickCategories, savedCategory)
                        it.copy(
                            isSavingCategory = false,
                            showAddQuickCategoryDialog = false,
                            allQuickCategories = allCategories,
                            quickCategories = mergeQuickCategories(allCategories, it.type),
                            quickCategoryName = "",
                            quickCategoryCategory = "",
                            quickCategoryPaymentMethod = "Cash",
                            quickCategoryIsCommitted = false,
                            quickCategoryFrequency = null,
                            quickCategoryRepeatUntilDate = "",
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSavingCategory = false,
                            errorMessage = "Could not save this quick category."
                        )
                    }
                }
        }
    }

    fun onQuickCategorySelected(category: QuickCategory) {
        _uiState.update {
            if (category.type != it.type) return@update it
            val repeatUntil = category.defaultRepeatUntil
                ?.takeIf { timestamp -> !timestamp.toDate().before(startOfToday()) }
                ?.let { timestamp -> formatInputDate(timestamp.toDate()) }
                .orEmpty()
            val committed = category.defaultIsCommitted == true ||
                category.defaultIsRepeating == true
            val internalCategory = category.defaultCategoryName.ifBlank { category.displayName }

            it.copy(
                selectedQuickCategoryId = category.categoryId,
                title = category.displayName,
                category = internalCategory,
                incomeSource = if (it.type == TransactionType.INCOME) {
                    FinanceCategories.incomeSourceForCategory(internalCategory) ?: IncomeSource.OTHER
                } else {
                    null
                },
                paymentMethod = category.defaultPaymentMethod.ifBlank { it.paymentMethod },
                isCommitted = committed,
                isDiscretionary = it.type == TransactionType.EXPENSE && !committed,
                repeatFrequency = if (committed) {
                    category.defaultFrequency ?: it.repeatFrequency ?: RecurringFrequency.MONTHLY
                } else {
                    null
                },
                repeatUntilDate = if (committed) repeatUntil.ifBlank { it.repeatUntilDate } else "",
                titleError = null,
                categoryError = null,
                paymentMethodError = null,
                spendingBehaviorError = null,
                repeatFrequencyError = null,
                repeatUntilDateError = null,
                errorMessage = null
            )
        }

        if (!category.isSystem && category.categoryId.isNotBlank()) {
            val userId = quickCategoryRepository.currentUserId ?: return
            transactionScope.launch {
                quickCategoryRepository.updateCategoryUsage(userId, category.categoryId)
            }
        }
    }

    fun deleteQuickCategory(category: QuickCategory) {
        if (category.isSystem || category.categoryId.isBlank()) return
        val userId = quickCategoryRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        transactionScope.launch {
            quickCategoryRepository.deleteCategory(userId, category.categoryId)
                .onSuccess {
                    _uiState.update { state ->
                        val deletedSelectedCategory = state.selectedQuickCategoryId == category.categoryId
                        val allCategories = state.allQuickCategories.filterNot {
                            it.categoryId == category.categoryId
                        }
                        state.copy(
                            allQuickCategories = allCategories,
                            quickCategories = mergeQuickCategories(allCategories, state.type),
                            selectedQuickCategoryId = state.selectedQuickCategoryId.takeUnless {
                                deletedSelectedCategory
                            },
                            category = state.category.takeUnless { deletedSelectedCategory }.orEmpty(),
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(errorMessage = "Could not delete this quick category.")
                    }
                }
        }
    }

    fun saveTransaction() {
        val preparedTransaction = buildValidatedTransaction() ?: return

        transactionScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val today = todayLocalDate()
            val isFutureTransaction = preparedTransaction.selectedDate.isAfter(today)
            val recurringPaymentId = if (preparedTransaction.transaction.isRecurring) {
                val nextDueDate = if (isFutureTransaction) {
                    preparedTransaction.transaction.transactionDate
                } else {
                    nextDueDateFor(
                        date = preparedTransaction.transaction.transactionDate.toDate(),
                        frequency = preparedTransaction.repeatFrequency ?: RecurringFrequency.MONTHLY
                    )
                }
                recurringPaymentRepository.createRecurringPayment(
                    buildRecurringPayment(
                        preparedTransaction = preparedTransaction,
                        nextDueDate = nextDueDate
                    )
                ).getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Could not save recurrent transaction."
                        )
                    }
                    return@launch
                }
            } else {
                null
            }

            if (isFutureTransaction) {
                val followUpTransaction = preparedTransaction.transaction.copy(
                    recurringPaymentId = recurringPaymentId
                )
                val followUpResult = paymentFollowUpRepository.savePaymentFollowUp(
                    followUpTransaction.toPaymentFollowUp()
                )
                if (followUpResult.isSuccess) {
                    completeSaveWithOptionalQuickAddPrompt(followUpTransaction)
                } else {
                    recurringPaymentId?.let { paymentId ->
                        recurringPaymentRepository.deleteRecurringPayment(
                            userId = preparedTransaction.transaction.userId,
                            paymentId = paymentId
                        )
                    }
                    val throwable = followUpResult.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable?.message ?: "Could not save follow-up."
                        )
                    }
                }
                return@launch
            }

            val transaction = if (recurringPaymentId != null) {
                preparedTransaction.transaction.copy(recurringPaymentId = recurringPaymentId)
            } else {
                preparedTransaction.transaction
            }

            transactionRepository.saveTransactionAndReturnId(transaction)
                .onSuccess { transactionId ->
                    val savedTransaction = transaction.copy(transactionId = transactionId)
                    publishSavedTransaction(savedTransaction)
                    completeSaveWithOptionalQuickAddPrompt(savedTransaction)
                    // Ensure listener is active to receive updates from Firestore
                    if (transactionListenerJob == null) {
                        loadTransactions()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Could not save transaction."
                        )
                    }
                }
        }
    }

    private fun completeSaveWithOptionalQuickAddPrompt(transaction: Transaction) {
        val pendingCategory = pendingQuickCategoryFor(
            transaction = transaction,
            repeatEndDate = null
        )
        if (pendingCategory == null) {
            resetFormAfterSave()
            return
        }

        _uiState.update {
            it.copy(
                isSaving = false,
                showSaveAsCategoryDialog = true,
                pendingQuickCategory = pendingCategory,
                errorMessage = null
            )
        }
    }

    private fun publishSavedTransaction(transaction: Transaction) {
        _listUiState.update { state ->
            val transactions = sortNewestFirst(
                state.transactions
                    .filterNot { existing -> existing.transactionId == transaction.transactionId }
                    .plus(transaction)
            )
            state.copy(
                isLoading = false,
                transactions = transactions,
                visibleTransactions = filterTransactions(transactions, state.selectedFilter),
                errorMessage = null
            )
        }
    }

    fun savePendingQuickCategory() {
        if (_uiState.value.isSavingCategory) return
        val pendingCategory = _uiState.value.pendingQuickCategory ?: return
        val userId = quickCategoryRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        transactionScope.launch {
            _uiState.update { it.copy(isSavingCategory = true, errorMessage = null) }
            quickCategoryRepository.addCategory(userId, pendingCategory)
                .onSuccess { savedCategory ->
                    _uiState.update {
                        val allCategories = upsertQuickCategory(it.allQuickCategories, savedCategory)
                        it.copy(
                            allQuickCategories = allCategories,
                            quickCategories = mergeQuickCategories(allCategories, it.type)
                        )
                    }
                    resetFormAfterSave()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSavingCategory = false,
                            errorMessage = throwable.message ?: "Could not save this quick category."
                        )
                    }
                }
        }
    }

    fun skipSaveQuickCategory() {
        resetFormAfterSave()
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearListErrorMessage() {
        _listUiState.update { it.copy(errorMessage = null) }
    }

    private fun buildValidatedTransaction(): PreparedTransaction? {
        val state = _uiState.value
        val userId = transactionRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return null
        }

        val amountResult = FinanceValidation.validatePositiveAmount(state.amount)
        val currencyResult = FinanceValidation.validateCurrency(state.currency)
        val exchangeRateResult = FinanceValidation.validateExchangeRate(
            currency = currencyResult.value ?: "LKR",
            value = state.exchangeRateToLkr,
            baseCurrency = state.baseCurrency
        )
        val internalCategory = state.category.trim()
            .ifBlank { state.title.trim() }
            .ifBlank { INTERNAL_OTHER_CATEGORY }
        val selectedIncomeSource = if (state.type == TransactionType.INCOME) {
            state.incomeSource
                ?: FinanceCategories.incomeSourceForCategory(state.category)
                ?: IncomeSource.OTHER
        } else {
            null
        }

        val parsedTransactionDate = parseInputDate(state.transactionDate.trim())
        val selectedLocalDate = parseInputLocalDate(state.transactionDate.trim())
        val parsedRepeatEndDate = state.repeatUntilDate.trim()
            .takeIf { it.isNotBlank() }
            ?.let { parseInputDate(it) }

        val amountError = amountResult.errorMessage
        val exchangeRateError = currencyResult.errorMessage ?: exchangeRateResult.errorMessage
        val titleError = when {
            state.title.isNotBlank() -> null
            state.type == TransactionType.EXPENSE -> "Expense name is required."
            else -> "Income name is required."
        }
        val transactionDateError = when {
            state.transactionDate.isBlank() -> "Transaction date is required"
            parsedTransactionDate == null || selectedLocalDate == null -> "Use YYYY-MM-DD, for example 2026-05-25"
            else -> null
        }
        val incomeSourceError = null
        val spendingBehaviorError = if (state.type == TransactionType.EXPENSE) {
            FinanceValidation.validateExpenseType(
                isCommitted = state.isCommitted,
                isDiscretionary = state.isDiscretionary
            ).errorMessage
        } else {
            null
        }
        val repeatFrequencyError = if (state.isCommitted && state.repeatFrequency == null) {
            "Select how often it repeats."
        } else {
            null
        }
        val repeatUntilDateError = when {
            !state.isCommitted -> null
            state.repeatUntilDate.isBlank() -> null
            parsedRepeatEndDate == null -> "Use YYYY-MM-DD, for example 2026-12-31"
            parsedRepeatEndDate.before(startOfToday()) -> "End date cannot be in the past."
            parsedTransactionDate != null &&
                state.repeatFrequency != null &&
                !RecurringScheduleCalculator.canRepeatAtLeastOnce(
                    startDate = parsedTransactionDate,
                    frequency = state.repeatFrequency,
                    repeatEndDate = parsedRepeatEndDate
                ) -> "End date must allow at least one repeat."
            else -> null
        }
        val paymentMethodError = if (state.paymentMethod.isBlank()) {
            "Payment method is required"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                amountError = amountError,
                exchangeRateError = exchangeRateError,
                titleError = titleError,
                categoryError = null,
                transactionDateError = transactionDateError,
                incomeSourceError = incomeSourceError,
                spendingBehaviorError = spendingBehaviorError,
                repeatFrequencyError = repeatFrequencyError,
                repeatUntilDateError = repeatUntilDateError,
                paymentMethodError = paymentMethodError,
                errorMessage = if (
                    listOf(
                        amountError,
                        exchangeRateError,
                        titleError,
                        transactionDateError,
                        spendingBehaviorError,
                        repeatFrequencyError,
                        repeatUntilDateError,
                        paymentMethodError
                    ).any { error -> error != null }
                ) {
                    "Please check the highlighted fields."
                } else {
                    null
                }
            )
        }

        return if (
            amountError == null &&
            exchangeRateError == null &&
            titleError == null &&
            transactionDateError == null &&
            spendingBehaviorError == null &&
            repeatFrequencyError == null &&
            repeatUntilDateError == null &&
            paymentMethodError == null &&
            parsedTransactionDate != null &&
            selectedLocalDate != null
        ) {
            val safeAmount = FinanceCalculator.roundMoney(amountResult.value ?: 0.0)
            val safeExchangeRate = exchangeRateResult.value ?: 1.0
            val convertedAmountLkr = FinanceCalculator.convertToLkr(
                originalAmount = safeAmount,
                originalCurrency = state.currency,
                exchangeRateToLkr = safeExchangeRate,
                baseCurrency = state.baseCurrency
            )
            val transactionDate = createTimestampAtMidnight(parsedTransactionDate)

            PreparedTransaction(
                transaction = Transaction(
                    userId = userId,
                    type = state.type,
                    title = state.title.trim(),
                    category = internalCategory,
                    incomeSource = selectedIncomeSource,
                    originalAmount = safeAmount,
                    originalCurrency = state.currency,
                    exchangeRateToLkr = safeExchangeRate,
                    convertedAmountLkr = convertedAmountLkr,
                    transactionDate = transactionDate,
                    monthId = monthIdFor(transactionDate),
                    paymentMethod = state.paymentMethod.trim(),
                    note = state.note.trim(),
                    isCommitted = state.type == TransactionType.EXPENSE && state.isCommitted,
                    isDiscretionary = state.type == TransactionType.EXPENSE && !state.isCommitted,
                    isRecurring = state.isCommitted
                ),
                repeatFrequency = state.repeatFrequency,
                repeatEndDate = parsedRepeatEndDate?.let { Timestamp(it) },
                selectedDate = selectedLocalDate
            )
        } else {
            null
        }
    }

    private fun buildRecurringPayment(
        preparedTransaction: PreparedTransaction,
        nextDueDate: Timestamp
    ): RecurringPayment {
        val transaction = preparedTransaction.transaction
        return RecurringPayment(
            userId = transaction.userId,
            type = transaction.type,
            title = transaction.title,
            category = transaction.category,
            incomeSource = transaction.incomeSource,
            originalAmount = transaction.originalAmount,
            originalCurrency = transaction.originalCurrency,
            exchangeRateToLkr = transaction.exchangeRateToLkr,
            convertedAmountLkr = transaction.convertedAmountLkr,
            frequency = preparedTransaction.repeatFrequency ?: RecurringFrequency.MONTHLY,
            nextDueDate = nextDueDate,
            repeatEndDate = preparedTransaction.repeatEndDate,
            paymentMethod = transaction.paymentMethod,
            isActive = true,
            isCommitted = transaction.type == TransactionType.EXPENSE,
            isDiscretionary = false,
            isRecurring = true,
            autoCreateTransaction = false,
            autoConfirm = false,
            note = transaction.note
        )
    }

    private fun pendingQuickCategoryFor(
        transaction: Transaction,
        repeatEndDate: Timestamp?
    ): QuickCategory? {
        if (transaction.isRecurring) {
            return null
        }
        val state = _uiState.value
        if (state.selectedQuickCategoryId != null) return null
        if (transaction.title.isBlank() || transaction.category.isBlank()) return null
        val hasMatchingCategory = state.quickCategories.any { category ->
            category.type == transaction.type &&
                (
                    category.name.equals(transaction.title, ignoreCase = true) ||
                        category.defaultExpenseName.equals(transaction.title, ignoreCase = true)
                    )
        }
        if (hasMatchingCategory) return null

        return QuickCategory(
            userId = transaction.userId,
            name = transaction.title,
            type = transaction.type,
            defaultExpenseName = transaction.title,
            defaultCategoryName = transaction.title,
            defaultPaymentMethod = transaction.paymentMethod,
            defaultIsCommitted = false,
            defaultIsDiscretionary = transaction.type == TransactionType.EXPENSE,
            defaultIsRepeating = false,
            defaultFrequency = null,
            defaultRepeatUntil = null,
            isSystem = false
        )
    }

    private fun Transaction.toPaymentFollowUp(): PaymentFollowUp {
        return PaymentFollowUp(
            userId = userId,
            type = type,
            title = title,
            category = category,
            incomeSource = incomeSource,
            status = PaymentFollowUpStatus.EXPECTED,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRateToLkr = exchangeRateToLkr,
            convertedAmountLkr = convertedAmountLkr,
            expectedDate = transactionDate,
            paymentMethod = paymentMethod,
            followUpNote = note,
            isCommitted = type == TransactionType.EXPENSE && isCommitted,
            isDiscretionary = type == TransactionType.EXPENSE && isDiscretionary,
            isRecurring = isRecurring,
            recurringPaymentId = recurringPaymentId
        )
    }

    private fun buildValidatedQuickCategoryFromForm(): QuickCategory? {
        val state = _uiState.value
        val userId = quickCategoryRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return null
        }

        val repeatEndDate = state.quickCategoryRepeatUntilDate.trim()
            .takeIf { it.isNotBlank() }
            ?.let { parseInputDate(it) }
        val nameError = if (state.quickCategoryName.isBlank()) {
            if (state.type == TransactionType.EXPENSE) {
                "Expense name is required."
            } else {
                "Income name is required."
            }
        } else {
            null
        }
        val paymentMethodError = if (state.quickCategoryPaymentMethod.isBlank()) {
            "Payment method is required."
        } else {
            null
        }
        val frequencyError = if (state.quickCategoryIsCommitted && state.quickCategoryFrequency == null) {
            "Select how often it repeats."
        } else {
            null
        }
        val repeatUntilError = when {
            !state.quickCategoryIsCommitted || state.quickCategoryRepeatUntilDate.isBlank() -> null
            repeatEndDate == null -> "Use YYYY-MM-DD, for example 2026-12-31"
            repeatEndDate.before(startOfToday()) -> "End date cannot be in the past."
            else -> null
        }

        _uiState.update {
            it.copy(
                quickCategoryNameError = nameError,
                quickCategoryCategoryError = null,
                quickCategoryPaymentMethodError = paymentMethodError,
                quickCategoryFrequencyError = frequencyError,
                quickCategoryRepeatUntilDateError = repeatUntilError,
                errorMessage = null
            )
        }

        return if (
            nameError == null &&
            paymentMethodError == null &&
            frequencyError == null &&
            repeatUntilError == null
        ) {
            QuickCategory(
                userId = userId,
                name = state.quickCategoryName.trim(),
                type = state.type,
                defaultExpenseName = state.quickCategoryName.trim(),
                defaultCategoryName = state.quickCategoryName.trim(),
                defaultPaymentMethod = state.quickCategoryPaymentMethod.trim(),
                defaultIsCommitted = state.quickCategoryIsCommitted,
                defaultIsDiscretionary = state.type == TransactionType.EXPENSE && !state.quickCategoryIsCommitted,
                defaultIsRepeating = state.quickCategoryIsCommitted,
                defaultFrequency = if (state.quickCategoryIsCommitted) {
                    state.quickCategoryFrequency ?: RecurringFrequency.MONTHLY
                } else {
                    null
                },
                defaultRepeatUntil = if (state.quickCategoryIsCommitted) {
                    repeatEndDate?.let { Timestamp(it) }
                } else {
                    null
                },
                isSystem = false
            )
        } else {
            null
        }
    }

    private fun mergeQuickCategories(
        userCategories: List<QuickCategory>,
        type: TransactionType
    ): List<QuickCategory> {
        val userExpenseCategories = userCategories
            .dedupeQuickCategories()
            .filter { category -> category.type == type && !category.isSystem }
            .sortedWith(
                compareByDescending<QuickCategory> { it.usageCount }
                    .thenBy { it.displayName.lowercase() }
            )
        return userExpenseCategories
    }

    private fun upsertQuickCategory(
        categories: List<QuickCategory>,
        savedCategory: QuickCategory
    ): List<QuickCategory> {
        val existingIndex = categories.indexOfFirst { category ->
            categoriesReferToSameQuickAdd(category, savedCategory)
        }
        return if (existingIndex == -1) {
            (categories + savedCategory).dedupeQuickCategories()
        } else {
            categories.mapIndexed { index, category ->
                if (index == existingIndex) savedCategory else category
            }.dedupeQuickCategories()
        }
    }

    private fun List<QuickCategory>.dedupeQuickCategories(): List<QuickCategory> {
        return fold(emptyList()) { unique, category ->
            if (unique.any { existing -> categoriesReferToSameQuickAdd(existing, category) }) {
                unique
            } else {
                unique + category
            }
        }
    }

    private fun categoriesReferToSameQuickAdd(
        first: QuickCategory,
        second: QuickCategory
    ): Boolean {
        val firstId = first.categoryId.takeIf { it.isNotBlank() }
        val secondId = second.categoryId.takeIf { it.isNotBlank() }
        if (firstId != null && secondId != null) return firstId == secondId

        return first.type == second.type &&
            first.displayName.equals(second.displayName, ignoreCase = true)
    }

    private fun resetFormAfterSave() {
        _uiState.update {
            val type = it.type
            TransactionUiState(
                type = type,
                allQuickCategories = it.allQuickCategories,
                quickCategories = mergeQuickCategories(it.allQuickCategories, type),
                baseCurrency = it.baseCurrency,
                supportedCurrencies = it.supportedCurrencies,
                currencyRatesToBase = it.currencyRatesToBase,
                currency = it.baseCurrency,
                exchangeRateToLkr = exchangeRateTextFor(it.baseCurrency, it.baseCurrency, it.currencyRatesToBase),
                isDiscretionary = type == TransactionType.EXPENSE,
                saveSuccess = true,
                shouldReturnToLogin = it.shouldReturnToLogin
            )
        }
    }

    private fun monthIdFor(timestamp: Timestamp): String {
        return monthIdFormatter().format(timestamp.toDate())
    }

    private fun monthIdFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
    }

    private fun sortNewestFirst(transactions: List<Transaction>): List<Transaction> {
        return transactions.sortedByDescending { transaction ->
            transaction.transactionDate.toDate().time
        }
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        return when (filter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
            TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
        }
    }

    private fun parseInputDate(value: String): Date? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        return runCatching {
            inputDateFormatter().parse(value)
        }.getOrNull()
    }

    private fun formatInputDate(date: Date): String {
        return inputDateFormatter().format(date)
    }

    private fun todayLocalDate(): LocalDate {
        return LocalDate.now(ZoneId.systemDefault())
    }

    private fun createTimestampAtMidnight(date: Date): Timestamp {
        val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(calendar.time)
    }

    private fun parseInputLocalDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun nextDueDateFor(
        date: Date,
        frequency: RecurringFrequency
    ): Timestamp {
        val calendar = Calendar.getInstance(java.util.TimeZone.getDefault()).apply { 
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (frequency) {
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        return Timestamp(calendar.time)
    }

    private fun exchangeRateTextFor(
        currency: String,
        baseCurrency: String,
        rates: Map<String, Double>
    ): String {
        if (currency.equals(baseCurrency, ignoreCase = true)) return "1"
        return (rates[currency] ?: 1.0).toString().removeSuffix(".0")
    }

    private fun startOfToday(): Date {
        return Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    override fun onCleared() {
        transactionScope.cancel()
        super.onCleared()
    }

    private fun inputDateFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = java.util.TimeZone.getDefault()
        }
    }

    private companion object {
        const val INTERNAL_OTHER_CATEGORY = "Other"
    }
}

private data class PreparedTransaction(
    val transaction: Transaction,
    val repeatFrequency: RecurringFrequency?,
    val repeatEndDate: Timestamp?,
    val selectedDate: LocalDate
)
