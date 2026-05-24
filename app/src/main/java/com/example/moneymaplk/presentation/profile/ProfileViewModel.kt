package com.example.moneymaplk.presentation.profile

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.AuthRepository
import com.example.moneymaplk.data.repository.FirebaseAuthRepository
import com.example.moneymaplk.data.repository.FirebaseGoalRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.GoalRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.model.Goal
import com.example.moneymaplk.domain.model.UserProfile
import com.example.moneymaplk.domain.validation.FinanceValidation
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
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val goalRepository: GoalRepository = FirebaseGoalRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val profileScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null

    fun loadProfile() {
        val userId = userRepository.currentUserId
        if (userId == null) {
            clearProfileJob()
            _uiState.value = ProfileUiState(shouldReturnToLogin = true)
            return
        }

        val state = _uiState.value
        if (profileJob?.isActive == true && state.activeUserId == userId) return

        clearProfileJob()
        _uiState.value = ProfileUiState(
            isLoading = true,
            activeUserId = userId
        )

        profileJob = combine(
            userRepository.observeUserProfile(userId),
            goalRepository.observeGoals(userId)
        ) { profileResult, goalResult ->
            ProfileSourceData(
                profile = profileResult.getOrThrow(),
                goals = goalResult.getOrThrow()
            )
        }
            .onEach { sourceData ->
                if (userRepository.currentUserId != userId) return@onEach

                _uiState.update { state ->
                    val profile = sourceData.profile
                    if (profile == null) {
                        state.copy(
                            isLoading = false,
                            activeUserId = userId,
                            loadedUserId = null,
                            profile = null,
                            homeGoalName = null,
                            errorMessage = "Complete your financial setup to view your profile."
                        )
                    } else {
                        state.copy(
                            isLoading = false,
                            activeUserId = userId,
                            loadedUserId = userId,
                            profile = profile,
                            homeGoalName = sourceData.goals.homeGoalName(profile.selectedGoalId),
                            displayName = if (state.isEditing) state.displayName else profile.displayName,
                            city = if (state.isEditing) state.city else profile.city,
                            occupation = if (state.isEditing) state.occupation else profile.occupation,
                            defaultCurrency = if (state.isEditing) state.defaultCurrency else profile.defaultCurrency,
                            supportedCurrencies = if (state.isEditing) state.supportedCurrencies else profile.supportedCurrencies,
                            currencyRatesToBase = if (state.isEditing) state.currencyRatesToBase else profile.currencyRatesToBase,
                            currentSavingsLkr = if (state.isEditing) {
                                state.currentSavingsLkr
                            } else {
                                profile.currentSavingsLkr.toInputText()
                            },
                            plannedSavingsAllocationLkr = if (state.isEditing) {
                                state.plannedSavingsAllocationLkr
                            } else {
                                profile.plannedSavingsAllocationLkr.toInputText()
                            },
                            safeToSpendBufferLkr = if (state.isEditing) {
                                state.safeToSpendBufferLkr
                            } else {
                                profile.safeToSpendBufferLkr.toInputText()
                            },
                            errorMessage = null
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
                            errorMessage = throwable.message ?: "Could not load your profile. Please try again."
                        )
                    }
                }
            }
            .launchIn(profileScope)
    }

    fun retryProfile() {
        clearProfileJob()
        loadProfile()
    }

    fun startEditing() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                displayName = profile.displayName,
                city = profile.city,
                occupation = profile.occupation,
                defaultCurrency = profile.defaultCurrency,
                supportedCurrencies = profile.supportedCurrencies,
                currencyRatesToBase = profile.currencyRatesToBase,
                newCurrencyCode = "",
                newCurrencyRate = "",
                currentSavingsLkr = profile.currentSavingsLkr.toInputText(),
                plannedSavingsAllocationLkr = profile.plannedSavingsAllocationLkr.toInputText(),
                safeToSpendBufferLkr = profile.safeToSpendBufferLkr.toInputText(),
                displayNameError = null,
                currentSavingsError = null,
                plannedSavingsError = null,
                safeToSpendBufferError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelEditing() {
        val profile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = false,
                displayName = profile?.displayName.orEmpty(),
                city = profile?.city.orEmpty(),
                occupation = profile?.occupation.orEmpty(),
                defaultCurrency = profile?.defaultCurrency ?: "LKR",
                supportedCurrencies = profile?.supportedCurrencies ?: listOf("LKR", "USD"),
                currencyRatesToBase = profile?.currencyRatesToBase ?: mapOf("LKR" to 1.0, "USD" to 310.0),
                newCurrencyCode = "",
                newCurrencyRate = "",
                currentSavingsLkr = profile?.currentSavingsLkr?.toInputText().orEmpty(),
                plannedSavingsAllocationLkr = profile?.plannedSavingsAllocationLkr?.toInputText().orEmpty(),
                safeToSpendBufferLkr = profile?.safeToSpendBufferLkr?.toInputText().orEmpty(),
                displayNameError = null,
                currentSavingsError = null,
                plannedSavingsError = null,
                safeToSpendBufferError = null,
                errorMessage = null
            )
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, displayNameError = null, errorMessage = null) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value, errorMessage = null) }
    }

    fun onOccupationChange(value: String) {
        _uiState.update { it.copy(occupation = value, errorMessage = null) }
    }

    fun onCurrencyChange(value: String) {
        val normalized = FinanceValidation.normalizeCurrencyCode(value) ?: return
        _uiState.update {
            it.copy(
                defaultCurrency = normalized,
                supportedCurrencies = (it.supportedCurrencies + normalized).distinct(),
                currencyRatesToBase = it.currencyRatesToBase + (normalized to 1.0),
                errorMessage = null
            )
        }
    }

    fun onNewCurrencyCodeChange(value: String) {
        _uiState.update { it.copy(newCurrencyCode = value.uppercase(Locale.US), newCurrencyError = null) }
    }

    fun onNewCurrencyRateChange(value: String) {
        _uiState.update { it.copy(newCurrencyRate = value, newCurrencyError = null) }
    }

    fun addCurrency() {
        val state = _uiState.value
        val code = FinanceValidation.normalizeCurrencyCode(state.newCurrencyCode)
        val rate = state.newCurrencyRate.toDoubleOrNull()
        when {
            code == null -> _uiState.update { it.copy(newCurrencyError = "Use a 3-letter currency code.") }
            code != state.defaultCurrency && (rate == null || rate <= 0.0) -> {
                _uiState.update { it.copy(newCurrencyError = "Enter the exchange rate to ${state.defaultCurrency}.") }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        supportedCurrencies = (it.supportedCurrencies + code).distinct(),
                        currencyRatesToBase = it.currencyRatesToBase + (code to if (code == it.defaultCurrency) 1.0 else rate!!),
                        newCurrencyCode = "",
                        newCurrencyRate = "",
                        newCurrencyError = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun removeCurrency(value: String) {
        val normalized = value.uppercase(Locale.US)
        _uiState.update {
            if (normalized == it.defaultCurrency) {
                it.copy(newCurrencyError = "Base currency cannot be removed.")
            } else {
                it.copy(
                    supportedCurrencies = it.supportedCurrencies.filterNot { currency -> currency == normalized },
                    currencyRatesToBase = it.currencyRatesToBase - normalized,
                    newCurrencyError = null
                )
            }
        }
    }

    fun onCurrentSavingsChange(value: String) {
        _uiState.update { it.copy(currentSavingsLkr = value, currentSavingsError = null, errorMessage = null) }
    }

    fun onPlannedSavingsChange(value: String) {
        _uiState.update { it.copy(plannedSavingsAllocationLkr = value, plannedSavingsError = null, errorMessage = null) }
    }

    fun onSafeToSpendBufferChange(value: String) {
        _uiState.update { it.copy(safeToSpendBufferLkr = value, safeToSpendBufferError = null, errorMessage = null) }
    }

    fun saveProfile() {
        val updatedProfile = buildValidatedProfile() ?: return

        profileScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            userRepository.saveUserProfile(updatedProfile)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            profile = updatedProfile,
                            displayName = updatedProfile.displayName,
                            city = updatedProfile.city,
                            occupation = updatedProfile.occupation,
                            defaultCurrency = updatedProfile.defaultCurrency,
                            currentSavingsLkr = updatedProfile.currentSavingsLkr.toInputText(),
                            plannedSavingsAllocationLkr = updatedProfile.plannedSavingsAllocationLkr.toInputText(),
                            safeToSpendBufferLkr = updatedProfile.safeToSpendBufferLkr.toInputText(),
                            displayNameError = null,
                            currentSavingsError = null,
                            plannedSavingsError = null,
                            safeToSpendBufferError = null,
                            errorMessage = null,
                            successMessage = "Profile updated."
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Could not update your profile. Please try again."
                        )
                    }
                }
        }
    }

    fun logout() {
        authRepository.logout()
        clearProfileJob()
        _uiState.update {
            ProfileUiState(shouldReturnToLogin = true)
        }
    }

    fun consumeReturnToLoginEvent(): Boolean {
        val shouldNavigateToLogin = _uiState.value.shouldReturnToLogin &&
            userRepository.currentUserId == null

        _uiState.update { it.copy(shouldReturnToLogin = false) }

        if (!shouldNavigateToLogin && profileJob == null) {
            loadProfile()
        }

        return shouldNavigateToLogin
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionState() {
        clearProfileJob()
        _uiState.value = ProfileUiState()
    }

    private fun clearProfileJob() {
        profileJob?.cancel()
        profileJob = null
    }

    private fun buildValidatedProfile(): UserProfile? {
        val state = _uiState.value
        val profile = state.profile ?: return null
        val currentSavingsResult = FinanceValidation.validateNonNegativeAmount(state.currentSavingsLkr)
        val plannedSavingsResult = FinanceValidation.validateNonNegativeAmount(state.plannedSavingsAllocationLkr)
        val safeToSpendBufferResult = FinanceValidation.validateNonNegativeAmount(state.safeToSpendBufferLkr)
        val currencyResult = FinanceValidation.validateCurrencyCode(state.defaultCurrency)

        val displayNameError = if (state.displayName.isBlank()) {
            "Display name is required"
        } else {
            null
        }
        val currentSavingsError = currentSavingsResult.errorMessage
        val plannedSavingsError = plannedSavingsResult.errorMessage
        val safeToSpendBufferError = safeToSpendBufferResult.errorMessage

        _uiState.update {
            it.copy(
                displayNameError = displayNameError,
                currentSavingsError = currentSavingsError,
                plannedSavingsError = plannedSavingsError,
                safeToSpendBufferError = safeToSpendBufferError,
                errorMessage = currencyResult.errorMessage,
                successMessage = null
            )
        }

        return if (
            displayNameError == null &&
            currentSavingsError == null &&
            plannedSavingsError == null &&
            safeToSpendBufferError == null &&
            currencyResult.errorMessage == null
        ) {
            profile.copy(
                displayName = state.displayName.trim(),
                city = state.city.trim(),
                occupation = state.occupation.trim(),
                defaultCurrency = currencyResult.value ?: "LKR",
                supportedCurrencies = (state.supportedCurrencies + (currencyResult.value ?: "LKR"))
                    .map { it.uppercase(Locale.US) }
                    .distinct(),
                currencyRatesToBase = state.currencyRatesToBase + ((currencyResult.value ?: "LKR") to 1.0),
                currentSavingsLkr = FinanceCalculator.roundMoney(currentSavingsResult.value ?: 0.0),
                plannedSavingsAllocationLkr = FinanceCalculator.roundMoney(plannedSavingsResult.value ?: 0.0),
                safeToSpendBufferLkr = FinanceCalculator.roundMoney(safeToSpendBufferResult.value ?: 0.0)
            )
        } else {
            null
        }
    }

    private fun List<Goal>.homeGoalName(selectedGoalId: String?): String? {
        val activeGoals = filter { goal -> goal.isActive }
            .sortedBy { goal -> goal.name.lowercase(Locale.US) }
        return selectedGoalId
            ?.let { goalId -> activeGoals.firstOrNull { goal -> goal.goalId == goalId } }
            ?.name
            ?: activeGoals.firstOrNull()?.name
    }

    private fun Double.toInputText(): String {
        return if (this % 1.0 == 0.0) {
            toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", this)
        }
    }

    override fun onCleared() {
        profileScope.cancel()
        super.onCleared()
    }
}

private data class ProfileSourceData(
    val profile: UserProfile?,
    val goals: List<Goal>
)
