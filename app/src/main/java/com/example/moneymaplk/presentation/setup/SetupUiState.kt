package com.example.moneymaplk.presentation.setup

data class SetupUiState(
    val preferredCurrency: String = "LKR",
    val exchangeRateToLkr: String = "",
    val currentSavings: String = "",
    val monthlySalary: String = "",
    val plannedSavingsAllocation: String = "",
    val safeToSpendBuffer: String = "",
    val exchangeRateError: String? = null,
    val currentSavingsError: String? = null,
    val monthlySalaryError: String? = null,
    val plannedSavingsError: String? = null,
    val safeToSpendBufferError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val shouldReturnToLogin: Boolean = false
)
