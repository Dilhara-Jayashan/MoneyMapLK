package com.example.moneymaplk.presentation.auth

import android.content.Context
import com.example.moneymaplk.data.firebase.GoogleSignInClient
import com.example.moneymaplk.data.firebase.GoogleSignInResult
import com.example.moneymaplk.data.repository.AuthenticatedUser
import androidx.lifecycle.ViewModel
import com.example.moneymaplk.data.repository.AuthRepository
import com.example.moneymaplk.data.repository.FirebaseAuthRepository
import com.example.moneymaplk.data.repository.FirebaseUserRepository
import com.example.moneymaplk.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val googleSignInClient: GoogleSignInClient = GoogleSignInClient()
) : ViewModel() {

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun checkCurrentUser() {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    hasCheckedCurrentUser = true,
                    startupDestination = AuthStartupDestination.LOGIN,
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                isAuthenticated = true,
                hasCheckedCurrentUser = false,
                startupDestination = AuthStartupDestination.UNKNOWN,
                errorMessage = null
            )
        }

        authScope.launch {
            runCatching { userRepository.isSetupCompleted(userId) }
                .onSuccess { setupCompleted ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            hasCheckedCurrentUser = true,
                            startupDestination = if (setupCompleted) {
                                AuthStartupDestination.DASHBOARD
                            } else {
                                AuthStartupDestination.FINANCIAL_SETUP
                            },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            hasCheckedCurrentUser = true,
                            startupDestination = AuthStartupDestination.FINANCIAL_SETUP,
                            errorMessage = throwable.message ?: "Could not check setup status."
                        )
                    }
                }
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.update {
            it.copy(fullName = value, fullNameError = null, errorMessage = null)
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = null, errorMessage = null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun onForgotPasswordEmailChange(value: String) {
        _uiState.update {
            it.copy(
                forgotPasswordEmail = value,
                forgotPasswordEmailError = null,
                passwordResetError = null
            )
        }
    }

    fun login() {
        if (!validateLogin()) return

        authScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    authSuccess = false,
                    startupDestination = AuthStartupDestination.UNKNOWN,
                    errorMessage = null
                )
            }
            val state = _uiState.value
            authRepository.login(state.email, state.password)
                .onSuccess { userId ->
                    val setupCompleted = runCatching {
                        userRepository.isSetupCompleted(userId)
                    }.getOrElse { false }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            authSuccess = true,
                            startupDestination = if (setupCompleted) {
                                AuthStartupDestination.DASHBOARD
                            } else {
                                AuthStartupDestination.FINANCIAL_SETUP
                            },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Login failed. Please try again."
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(context: Context) {
        authScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    authSuccess = false,
                    startupDestination = AuthStartupDestination.UNKNOWN,
                    errorMessage = null
                )
            }

            when (val signInResult = withContext(Dispatchers.Main) { googleSignInClient.signIn(context) }) {
                GoogleSignInResult.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Sign-in cancelled."
                        )
                    }
                }

                is GoogleSignInResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Google sign-in failed. Please try again."
                        )
                    }
                }

                is GoogleSignInResult.Success -> {
                    authRepository.loginWithGoogle(signInResult.idToken)
                        .onSuccess { user ->
                            handleGoogleAuthSuccess(user)
                        }
                        .onFailure {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Google sign-in failed. Please try again."
                                )
                            }
                        }
                }
            }
        }
    }

    fun sendPasswordReset() {
        sendPasswordReset(_uiState.value.forgotPasswordEmail)
    }

    fun sendPasswordReset(email: String) {
        val emailError = validatePasswordResetEmail(email)
        _uiState.update {
            it.copy(
                forgotPasswordEmailError = emailError,
                passwordResetError = null,
                passwordResetMessage = null
            )
        }
        if (emailError != null) return

        authScope.launch {
            _uiState.update {
                it.copy(
                    isSendingPasswordReset = true,
                    passwordResetError = null,
                    passwordResetMessage = null
                )
            }
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSendingPasswordReset = false,
                            forgotPasswordEmail = "",
                            forgotPasswordEmailError = null,
                            passwordResetError = null,
                            passwordResetMessage = "Password reset link sent. Check your email."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSendingPasswordReset = false,
                            passwordResetError = "Could not send reset link. Check the email and try again."
                        )
                    }
                }
        }
    }

    fun register() {
        if (!validateRegister()) return

        authScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            authRepository.register(state.fullName, state.email, state.password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            authSuccess = true,
                            startupDestination = AuthStartupDestination.FINANCIAL_SETUP,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Registration failed. Please try again."
                        )
                    }
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState(
            hasCheckedCurrentUser = true,
            startupDestination = AuthStartupDestination.LOGIN
        )
    }

    fun clearAuthSuccess() {
        _uiState.update { it.copy(authSuccess = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearPasswordResetMessage() {
        _uiState.update { it.copy(passwordResetMessage = null) }
    }

    fun clearPasswordResetError() {
        _uiState.update { it.copy(passwordResetError = null) }
    }

    fun clearPasswordResetForm() {
        _uiState.update {
            it.copy(
                forgotPasswordEmail = "",
                forgotPasswordEmailError = null,
                passwordResetError = null
            )
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                fullNameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                forgotPasswordEmailError = null,
                errorMessage = null,
                passwordResetError = null,
                passwordResetMessage = null
            )
        }
    }

    private fun validateLogin(): Boolean {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.isBlank()) {
            "Password is required"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                errorMessage = null
            )
        }
        return emailError == null && passwordError == null
    }

    private suspend fun handleGoogleAuthSuccess(user: AuthenticatedUser) {
        val setupCompleted = runCatching {
            userRepository.isSetupCompleted(user.uid)
        }.getOrElse { false }

        if (!setupCompleted) {
            val profileResult = userRepository.createBasicUserProfileIfMissing(
                userId = user.uid,
                displayName = user.displayName,
                email = user.email
            )
            if (profileResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Google sign-in failed. Please try again."
                    )
                }
                return
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                authSuccess = true,
                startupDestination = if (setupCompleted) {
                    AuthStartupDestination.DASHBOARD
                } else {
                    AuthStartupDestination.FINANCIAL_SETUP
                },
                errorMessage = null
            )
        }
    }

    private fun validateRegister(): Boolean {
        val state = _uiState.value
        val fullNameError = if (state.fullName.isBlank()) {
            "Full name is required"
        } else {
            null
        }
        val emailError = validateEmail(state.email)
        val passwordError = when {
            state.password.isBlank() -> "Password is required"
            state.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        val confirmPasswordError = when {
            state.confirmPassword.isBlank() -> "Confirm your password"
            state.confirmPassword != state.password -> "Passwords do not match"
            else -> null
        }

        _uiState.update {
            it.copy(
                fullNameError = fullNameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
                errorMessage = null
            )
        }
        return fullNameError == null &&
            emailError == null &&
            passwordError == null &&
            confirmPasswordError == null
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            "@" !in email -> "Enter a valid email"
            else -> null
        }
    }

    private fun validatePasswordResetEmail(email: String): String? {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isBlank() -> "Enter a valid email address."
            "@" !in trimmedEmail || "." !in trimmedEmail.substringAfter("@") -> {
                "Enter a valid email address."
            }
            else -> null
        }
    }

    override fun onCleared() {
        authScope.cancel()
        super.onCleared()
    }
}
