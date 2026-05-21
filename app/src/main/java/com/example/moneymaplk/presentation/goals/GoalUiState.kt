package com.example.moneymaplk.presentation.goals

import com.example.moneymaplk.domain.model.Goal

data class GoalUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val selectedGoalId: String? = null,
    val homeGoalId: String? = null,
    val availableToAllocateLkr: Double = 0.0,
    val contributionAmount: String = "",
    val contributionError: String? = null,
    val isAddGoalVisible: Boolean = false,
    val newGoalName: String = "",
    val newGoalTargetAmount: String = "",
    val newGoalSavedAmount: String = "0",
    val newGoalTargetDate: String = "",
    val newGoalDescription: String = "",
    val newGoalNameError: String? = null,
    val newGoalTargetAmountError: String? = null,
    val newGoalSavedAmountError: String? = null,
    val newGoalTargetDateError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
) {
    val goal: Goal?
        get() = goals.firstOrNull { goal -> goal.goalId == selectedGoalId }
            ?: goals.firstOrNull()
}
