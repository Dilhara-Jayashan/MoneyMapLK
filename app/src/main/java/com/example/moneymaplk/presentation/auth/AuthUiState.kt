package com.example.moneymaplk.presentation.auth

enum class AuthStartupDestination {
    UNKNOWN,
    LOGIN,
    FINANCIAL_SETUP,
    DASHBOARD
}

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val forgotPasswordEmail: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val forgotPasswordEmailError: String? = null,
    val errorMessage: String? = null,
    val passwordResetMessage: String? = null,
    val passwordResetError: String? = null,
    val isLoading: Boolean = false,
    val isSendingPasswordReset: Boolean = false,
    val isAuthenticated: Boolean = false,
    val hasCheckedCurrentUser: Boolean = false,
    val authSuccess: Boolean = false,
    val startupDestination: AuthStartupDestination = AuthStartupDestination.UNKNOWN
)
