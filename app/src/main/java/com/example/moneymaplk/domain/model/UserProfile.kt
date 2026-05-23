package com.example.moneymaplk.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val city: String = "",
    val occupation: String = "",
    val defaultCurrency: String,
    val supportedCurrencies: List<String>,
    val currencyRatesToBase: Map<String, Double> = emptyMap(),
    val currentSavingsLkr: Double,
    val monthlySalaryLkr: Double,
    val plannedSavingsAllocationLkr: Double,
    val safeToSpendBufferLkr: Double,
    val selectedGoalId: String? = null,
    val setupCompleted: Boolean = false,
    val financialMonthStartDay: Int = 1
)
