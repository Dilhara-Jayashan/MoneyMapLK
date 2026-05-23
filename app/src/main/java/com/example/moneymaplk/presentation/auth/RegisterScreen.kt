package com.example.moneymaplk.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.clearErrors()
    }

    LaunchedEffect(uiState.authSuccess, uiState.startupDestination) {
        if (uiState.authSuccess) {
            when (uiState.startupDestination) {
                AuthStartupDestination.DASHBOARD -> {
                    authViewModel.clearAuthSuccess()
                    onNavigateToDashboard()
                }

                AuthStartupDestination.FINANCIAL_SETUP,
                AuthStartupDestination.UNKNOWN,
                AuthStartupDestination.LOGIN -> {
                    authViewModel.clearAuthSuccess()
                    onRegisterSuccess()
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            authViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            authViewModel.clearErrorMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        MoneyMapAuthLogo(
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set up your MoneyMap LK profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        MoneyMapTextField(
            value = uiState.fullName,
            onValueChange = authViewModel::onFullNameChange,
            label = "Full Name",
            placeholder = "Your full name",
            errorText = uiState.fullNameError
        )
        Spacer(modifier = Modifier.height(10.dp))
        MoneyMapTextField(
            value = uiState.email,
            onValueChange = authViewModel::onEmailChange,
            label = "Email",
            placeholder = "name@example.com",
            errorText = uiState.emailError,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(10.dp))
        MoneyMapTextField(
            value = uiState.password,
            onValueChange = authViewModel::onPasswordChange,
            label = "Password",
            errorText = uiState.passwordError,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(10.dp))
        MoneyMapTextField(
            value = uiState.confirmPassword,
            onValueChange = authViewModel::onConfirmPasswordChange,
            label = "Confirm Password",
            errorText = uiState.confirmPasswordError,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorState(
                title = if (message == "Sign-in cancelled.") "Sign-in cancelled" else "Registration failed",
                message = message
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        MoneyMapPrimaryButton(
            text = if (uiState.isLoading) "Creating account..." else "Register",
            onClick = authViewModel::register,
            enabled = !uiState.isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        MoneyMapSecondaryButton(
            text = if (uiState.isLoading) "Please wait..." else "Continue with Google",
            onClick = { authViewModel.signInWithGoogle(context) },
            enabled = !uiState.isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onNavigateToLogin,
            enabled = !uiState.isLoading
        ) {
            Text(text = "Already have an account? Login")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
