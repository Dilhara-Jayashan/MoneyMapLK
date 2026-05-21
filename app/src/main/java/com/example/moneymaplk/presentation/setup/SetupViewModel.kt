package com.example.moneymaplk.presentation.setup

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.validation.FinanceValidation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val setupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun checkAuthenticatedUser() {
        _uiState.update {
            it.copy(shouldReturnToLogin = userRepository.currentUserId == null)
        }
    }

    fun onPreferredCurrencyChange(value: String) {
        if (value == "LKR" || value == "USD") {
            _uiState.update { state ->
                state.copy(
                    preferredCurrency = value,
                    exchangeRateError = null,
                    errorMessage = null
                )
            }
        }
    }

    fun onExchangeRateChange(value: String) {
        _uiState.update {
            it.copy(
                exchangeRateToLkr = value,
                exchangeRateError = null,
                errorMessage = null
            )
        }
    }

    fun onCurrentSavingsChange(value: String) {
        _uiState.update {
            it.copy(currentSavings = value, currentSavingsError = null, errorMessage = null)
        }
    }

    fun onMonthlySalaryChange(value: String) {
        _uiState.update {
            it.copy(monthlySalary = value, monthlySalaryError = null, errorMessage = null)
        }
    }

    fun onPlannedSavingsChange(value: String) {
        _uiState.update {
            it.copy(plannedSavingsAllocation = value, plannedSavingsError = null, errorMessage = null)
        }
    }

    fun onSafeToSpendBufferChange(value: String) {
        _uiState.update {
            it.copy(safeToSpendBuffer = value, safeToSpendBufferError = null, errorMessage = null)
        }
    }

    fun saveSetup() {
        val validatedValues = validateSetup() ?: return
        val profile = userRepository.buildCurrentUserProfile(
            defaultCurrency = _uiState.value.preferredCurrency,
            currentSavingsLkr = validatedValues.currentSavingsLkr,
            monthlySalaryLkr = validatedValues.monthlySalaryLkr,
            plannedSavingsAllocationLkr = validatedValues.plannedSavingsLkr,
            safeToSpendBufferLkr = validatedValues.safeToSpendBufferLkr
        )

        if (profile == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        setupScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            userRepository.saveUserProfile(profile)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Could not save your profile."
                        )
                    }
                }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionState() {
        _uiState.value = SetupUiState()
    }

    private fun validateSetup(): ValidatedSetupValues? {
        val state = _uiState.value
        val currencyResult = FinanceValidation.validateCurrencyCode(state.preferredCurrency)
        val currency = currencyResult.value ?: "LKR"
        val exchangeRateResult = FinanceValidation.validateExchangeRate(
            currency = currency,
            value = state.exchangeRateToLkr
        )
        val currentSavingsResult = FinanceValidation.validateNonNegativeAmount(state.currentSavings)
        val monthlySalaryResult = FinanceValidation.validatePositiveAmount(state.monthlySalary)
        val plannedSavingsResult = FinanceValidation.validateNonNegativeAmount(state.plannedSavingsAllocation)
        val safeToSpendBufferResult = FinanceValidation.validateNonNegativeAmount(state.safeToSpendBuffer)

        val exchangeRateError = currencyResult.errorMessage ?: exchangeRateResult.errorMessage
        val currentSavingsError = currentSavingsResult.errorMessage
        val monthlySalaryError = monthlySalaryResult.errorMessage
        val plannedSavingsError = plannedSavingsResult.errorMessage
        val safeToSpendBufferError = safeToSpendBufferResult.errorMessage

        val safeExchangeRate = exchangeRateResult.value ?: 1.0
        val currentSavingsLkr = FinanceCalculator.convertToLkr(
            originalAmount = currentSavingsResult.value ?: 0.0,
            originalCurrency = currency,
            exchangeRateToLkr = safeExchangeRate
        )
        val monthlySalaryLkr = FinanceCalculator.convertToLkr(
            originalAmount = monthlySalaryResult.value ?: 0.0,
            originalCurrency = currency,
            exchangeRateToLkr = safeExchangeRate
        )
        val plannedSavingsLkr = FinanceCalculator.convertToLkr(
            originalAmount = plannedSavingsResult.value ?: 0.0,
            originalCurrency = currency,
            exchangeRateToLkr = safeExchangeRate
        )
        val safeToSpendBufferLkr = FinanceCalculator.convertToLkr(
            originalAmount = safeToSpendBufferResult.value ?: 0.0,
            originalCurrency = currency,
            exchangeRateToLkr = safeExchangeRate
        )
        _uiState.update {
            it.copy(
                exchangeRateError = exchangeRateError,
                currentSavingsError = currentSavingsError,
                monthlySalaryError = monthlySalaryError,
                plannedSavingsError = plannedSavingsError,
                safeToSpendBufferError = safeToSpendBufferError,
                errorMessage = null
            )
        }

        return if (
            exchangeRateError == null &&
            currentSavingsError == null &&
            monthlySalaryError == null &&
            plannedSavingsError == null &&
            safeToSpendBufferError == null
        ) {
            ValidatedSetupValues(
                currentSavingsLkr = currentSavingsLkr,
                monthlySalaryLkr = monthlySalaryLkr,
                plannedSavingsLkr = plannedSavingsLkr,
                safeToSpendBufferLkr = safeToSpendBufferLkr
            )
        } else {
            null
        }
    }

    override fun onCleared() {
        setupScope.cancel()
        super.onCleared()
    }

}

private data class ValidatedSetupValues(
    val currentSavingsLkr: Double,
    val monthlySalaryLkr: Double,
    val plannedSavingsLkr: Double,
    val safeToSpendBufferLkr: Double
)
