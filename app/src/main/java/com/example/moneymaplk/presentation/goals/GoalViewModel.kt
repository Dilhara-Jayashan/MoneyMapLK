package com.example.moneymaplk.presentation.goals

import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.FirebaseGoalRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.GoalRepository
import com.example.moneymaplk.data.repository.TransactionRepository
import com.example.moneymaplk.data.repository.UserRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.model.Goal
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import com.example.moneymaplk.domain.model.UserProfile
import com.example.moneymaplk.domain.validation.FinanceValidation
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GoalViewModel(
    private val goalRepository: GoalRepository = FirebaseGoalRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val transactionRepository: TransactionRepository = FirebaseTransactionRepository()
) : ViewModel() {

    private val goalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()
    private var goalListenerJob: Job? = null

    fun loadGoal() {
        val userId = userRepository.currentUserId
        if (userId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    shouldReturnToLogin = true,
                    errorMessage = null
                )
            }
            return
        }

        if (goalListenerJob != null) return

        _uiState.update { state ->
            state.copy(
                isLoading = state.goals.isEmpty(),
                errorMessage = null,
                shouldReturnToLogin = false
            )
        }

        goalListenerJob = combine(
            goalRepository.observeGoals(userId),
            userRepository.observeUserProfile(userId),
            transactionRepository.observeTransactions(userId)
        ) { goalResult, profileResult, transactionResult ->
            val profile = profileResult.getOrThrow()
            GoalSourceData(
                goals = goalResult.getOrThrow(),
                homeGoalId = profile?.selectedGoalId,
                availableToAllocateLkr = availableToAllocateLkr(
                    profile = profile,
                    transactions = transactionResult.getOrThrow()
                )
            )
        }
            .onEach { sourceData ->
                _uiState.update { state ->
                    val activeGoals = sourceData.goals.activeGoals()
                    val homeGoalId = homeGoalId(
                        activeGoals = activeGoals,
                        savedHomeGoalId = sourceData.homeGoalId
                    )
                    state.copy(
                        isLoading = false,
                        goals = activeGoals,
                        selectedGoalId = selectedGoalId(
                            activeGoals = activeGoals,
                            currentSelectedGoalId = state.selectedGoalId,
                            homeGoalId = homeGoalId
                        ),
                        homeGoalId = homeGoalId,
                        availableToAllocateLkr = sourceData.availableToAllocateLkr,
                        errorMessage = null
                    )
                }
            }
            .catch {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "Could not load your goals. Please try again."
                    )
                }
            }
            .launchIn(goalScope)
    }

    fun retryGoal() {
        goalListenerJob?.cancel()
        goalListenerJob = null
        loadGoal()
    }

    fun resetSessionState() {
        goalListenerJob?.cancel()
        goalListenerJob = null
        _uiState.value = GoalUiState()
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onGoalSelected(goalId: String) {
        _uiState.update {
            it.copy(
                selectedGoalId = goalId,
                contributionAmount = "",
                contributionError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onContributionAmountChange(value: String) {
        _uiState.update {
            it.copy(
                contributionAmount = value,
                contributionError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun showAddGoalForm() {
        _uiState.update {
            it.copy(
                isAddGoalVisible = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun hideAddGoalForm() {
        _uiState.update {
            it.copy(
                isAddGoalVisible = false,
                newGoalNameError = null,
                newGoalTargetAmountError = null,
                newGoalSavedAmountError = null,
                newGoalTargetDateError = null,
                errorMessage = null
            )
        }
    }

    fun onNewGoalNameChange(value: String) {
        _uiState.update { it.copy(newGoalName = value, newGoalNameError = null, errorMessage = null) }
    }

    fun onNewGoalTargetAmountChange(value: String) {
        _uiState.update {
            it.copy(newGoalTargetAmount = value, newGoalTargetAmountError = null, errorMessage = null)
        }
    }

    fun onNewGoalSavedAmountChange(value: String) {
        _uiState.update {
            it.copy(newGoalSavedAmount = value, newGoalSavedAmountError = null, errorMessage = null)
        }
    }

    fun onNewGoalTargetDateChange(value: String) {
        _uiState.update {
            it.copy(newGoalTargetDate = value, newGoalTargetDateError = null, errorMessage = null)
        }
    }

    fun onNewGoalDescriptionChange(value: String) {
        _uiState.update { it.copy(newGoalDescription = value, errorMessage = null) }
    }

    fun createGoal() {
        val validatedGoal = buildValidatedGoal() ?: return

        goalScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            goalRepository.saveGoal(validatedGoal)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddGoalVisible = false,
                            selectedGoalId = validatedGoal.goalId,
                            newGoalName = "",
                            newGoalTargetAmount = "",
                            newGoalSavedAmount = "0",
                            newGoalTargetDate = "",
                            newGoalDescription = "",
                            newGoalNameError = null,
                            newGoalTargetAmountError = null,
                            newGoalSavedAmountError = null,
                            newGoalTargetDateError = null,
                            errorMessage = null,
                            successMessage = "Goal created."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Could not create your goal. Please try again."
                        )
                    }
                }
        }
    }

    fun addContribution() {
        val goal = _uiState.value.goal ?: return
        val contributionAmount = validateContribution(goal) ?: return
        val userId = goalRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        val updatedSavedAmount = FinanceCalculator.roundMoney(goal.savedAmountLkr + contributionAmount)
        val updatedRemainingAmount = FinanceCalculator.calculateRemainingGoalAmount(
            targetAmountLkr = goal.targetAmountLkr,
            savedAmountLkr = updatedSavedAmount
        )
        val updatedProgress = FinanceCalculator.calculateGoalProgress(
            savedAmountLkr = updatedSavedAmount,
            targetAmountLkr = goal.targetAmountLkr
        )

        goalScope.launch {
            _uiState.update {
                it.copy(isSaving = true, errorMessage = null, successMessage = null)
            }
            goalRepository.updateGoalProgress(
                userId = userId,
                goalId = goal.goalId,
                savedAmountLkr = updatedSavedAmount,
                remainingAmountLkr = updatedRemainingAmount,
                progressPercentage = updatedProgress
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            contributionAmount = "",
                            contributionError = null,
                            errorMessage = null,
                            successMessage = "Goal updated."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Could not update your goal. Please try again."
                        )
                    }
                }
        }
    }

    fun showOnHome(goalId: String) {
        val userId = userRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        if (_uiState.value.goals.none { goal -> goal.goalId == goalId }) return

        goalScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            userRepository.updateSelectedGoal(userId = userId, goalId = goalId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            homeGoalId = goalId,
                            successMessage = "Goal will show on Home."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Could not update your Home goal. Please try again."
                        )
                    }
                }
        }
    }

    fun createDefaultMacBookGoal() {
        val userId = userRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return
        }

        val defaultGoal = Goal(
            goalId = MACBOOK_GOAL_ID,
            userId = userId,
            name = MACBOOK_GOAL_NAME,
            description = "Savings goal for MacBook Pro M4",
            targetAmountLkr = MACBOOK_TARGET_LKR,
            savedAmountLkr = 0.0,
            deadline = "",
            priority = "HIGH"
        )

        goalScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            goalRepository.saveDefaultGoal(defaultGoal)
                .onSuccess {
                    userRepository.updateSelectedGoal(userId = userId, goalId = defaultGoal.goalId)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    selectedGoalId = defaultGoal.goalId,
                                    homeGoalId = defaultGoal.goalId,
                                    successMessage = "MacBook goal created."
                                )
                            }
                        }
                        .onFailure {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    selectedGoalId = defaultGoal.goalId,
                                    errorMessage = "Goal created, but Home was not updated."
                                )
                            }
                        }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Could not create your goal. Please try again."
                        )
                    }
                }
        }
    }

    private fun buildValidatedGoal(): Goal? {
        val state = _uiState.value
        val userId = goalRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(shouldReturnToLogin = true) }
            return null
        }

        val targetAmountResult = FinanceValidation.validatePositiveAmount(state.newGoalTargetAmount)
        val savedAmountResult = FinanceValidation.validateNonNegativeAmount(state.newGoalSavedAmount)
        val targetAmount = targetAmountResult.value
        val savedAmount = savedAmountResult.value
        val targetDate = state.newGoalTargetDate.trim()

        val nameError = if (state.newGoalName.isBlank()) "Goal name is required" else null
        val targetAmountError = targetAmountResult.errorMessage
        val savedAmountError = when {
            savedAmountResult.errorMessage != null -> savedAmountResult.errorMessage
            targetAmount != null && savedAmount != null && savedAmount > targetAmount -> {
                "Current saved amount cannot exceed target amount"
            }
            else -> null
        }
        val targetDateError = when {
            targetDate.isBlank() -> "Target date is required"
            !isValidDate(targetDate) -> "Use YYYY-MM-DD, for example 2026-12-25"
            else -> null
        }

        _uiState.update {
            it.copy(
                newGoalNameError = nameError,
                newGoalTargetAmountError = targetAmountError,
                newGoalSavedAmountError = savedAmountError,
                newGoalTargetDateError = targetDateError,
                errorMessage = null,
                successMessage = null
            )
        }

        return if (
            nameError == null &&
            targetAmountError == null &&
            savedAmountError == null &&
            targetDateError == null
        ) {
            val safeTargetAmount = FinanceCalculator.roundMoney(targetAmount ?: 0.0)
            val safeSavedAmount = FinanceCalculator.roundMoney(savedAmount ?: 0.0)
            Goal(
                goalId = goalIdFor(state.newGoalName),
                userId = userId,
                name = state.newGoalName.trim(),
                description = state.newGoalDescription.trim(),
                targetAmountLkr = safeTargetAmount,
                savedAmountLkr = safeSavedAmount,
                deadline = targetDate,
                priority = "MEDIUM",
                isActive = true
            )
        } else {
            null
        }
    }

    private fun validateContribution(goal: Goal): Double? {
        val state = _uiState.value
        val contributionResult = FinanceValidation.validatePositiveAmount(state.contributionAmount)
        val contributionAmount = contributionResult.value
        val remainingAmount = goal.remainingAmountLkr
        val error = when {
            contributionResult.errorMessage != null -> contributionResult.errorMessage
            remainingAmount <= 0.0 -> "Goal is already complete"
            contributionAmount != null && contributionAmount > remainingAmount -> {
                "This contribution is higher than the remaining goal amount."
            }
            contributionAmount != null && contributionAmount > state.availableToAllocateLkr -> {
                "This contribution is higher than your available balance."
            }
            else -> null
        }

        _uiState.update {
            it.copy(contributionError = error, errorMessage = null, successMessage = null)
        }

        return if (error == null) contributionAmount else null
    }

    private fun availableToAllocateLkr(
        profile: UserProfile?,
        transactions: List<Transaction>
    ): Double {
        if (profile == null) return 0.0

        val currentMonthId = currentMonthId()
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.monthId == currentMonthId
        }
        val thisMonthIncomeLkr = monthlyTransactions
            .filter { transaction -> transaction.type == TransactionType.INCOME }
            .sumOf { transaction -> transaction.convertedAmountLkr }
        val thisMonthExpensesLkr = monthlyTransactions
            .filter { transaction -> transaction.type == TransactionType.EXPENSE }
            .sumOf { transaction -> transaction.convertedAmountLkr }

        return FinanceCalculator.calculateAvailableToAllocate(
            currentSavingsLkr = profile.currentSavingsLkr,
            thisMonthIncomeLkr = thisMonthIncomeLkr,
            thisMonthExpensesLkr = thisMonthExpensesLkr
        )
    }

    private fun currentMonthId(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(Date())
    }

    private fun List<Goal>.activeGoals(): List<Goal> {
        return filter { goal -> goal.isActive }
            .sortedBy { goal -> goal.name.lowercase(Locale.US) }
    }

    private fun selectedGoalId(
        activeGoals: List<Goal>,
        currentSelectedGoalId: String?,
        homeGoalId: String?
    ): String? {
        return when {
            currentSelectedGoalId != null && activeGoals.any { goal -> goal.goalId == currentSelectedGoalId } -> {
                currentSelectedGoalId
            }
            homeGoalId != null && activeGoals.any { goal -> goal.goalId == homeGoalId } -> {
                homeGoalId
            }
            else -> activeGoals.firstOrNull()?.goalId
        }
    }

    private fun homeGoalId(
        activeGoals: List<Goal>,
        savedHomeGoalId: String?
    ): String? {
        return savedHomeGoalId
            ?.takeIf { goalId -> activeGoals.any { goal -> goal.goalId == goalId } }
            ?: activeGoals.firstOrNull()?.goalId
    }

    private fun goalIdFor(goalName: String): String {
        val slug = goalName.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "goal" }
        return "$slug-${System.currentTimeMillis()}"
    }

    private fun isValidDate(value: String): Boolean {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value.trim())) return false
        return runCatching {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = java.util.TimeZone.getDefault()
            }
            formatter.isLenient = false
            formatter.parse(value.trim())
        }.getOrNull() != null
    }

    override fun onCleared() {
        goalScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val MACBOOK_GOAL_ID = "macbookProM4"
        const val MACBOOK_GOAL_NAME = "MacBook Pro M4"
        const val MACBOOK_TARGET_LKR = 490000.0
    }
}

private data class GoalSourceData(
    val goals: List<Goal>,
    val homeGoalId: String?,
    val availableToAllocateLkr: Double
)
