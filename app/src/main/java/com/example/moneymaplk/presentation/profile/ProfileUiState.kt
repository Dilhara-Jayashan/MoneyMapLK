package com.example.moneymaplk.presentation.profile

import com.example.moneymaplk.domain.model.UserProfile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val activeUserId: String? = null,
    val loadedUserId: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val profile: UserProfile? = null,
    val homeGoalName: String? = null,
    val displayName: String = "",
    val city: String = "",
    val occupation: String = "",
    val defaultCurrency: String = "LKR",
    val supportedCurrencies: List<String> = listOf("LKR", "USD"),
    val currencyRatesToBase: Map<String, Double> = mapOf("LKR" to 1.0, "USD" to 310.0),
    val newCurrencyCode: String = "",
    val newCurrencyRate: String = "",
    val currentSavingsLkr: String = "",
    val plannedSavingsAllocationLkr: String = "",
    val safeToSpendBufferLkr: String = "",
    val displayNameError: String? = null,
    val currentSavingsError: String? = null,
    val plannedSavingsError: String? = null,
    val safeToSpendBufferError: String? = null,
    val newCurrencyError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
)
