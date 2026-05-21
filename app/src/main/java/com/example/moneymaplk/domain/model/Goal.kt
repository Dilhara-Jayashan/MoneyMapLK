package com.example.moneymaplk.domain.model

import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.google.firebase.Timestamp

data class Goal(
    val goalId: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val targetAmountLkr: Double,
    val savedAmountLkr: Double,
    val deadline: String = "",
    val targetDate: Timestamp? = null,
    val priority: String = "MEDIUM",
    val isActive: Boolean = true
) {
    val remainingAmountLkr: Double
        get() = FinanceCalculator.calculateRemainingGoalAmount(
            targetAmountLkr = targetAmountLkr,
            savedAmountLkr = savedAmountLkr
        )

    val progressPercentage: Double
        get() = FinanceCalculator.calculateGoalProgress(
            savedAmountLkr = savedAmountLkr,
            targetAmountLkr = targetAmountLkr
        )
}
