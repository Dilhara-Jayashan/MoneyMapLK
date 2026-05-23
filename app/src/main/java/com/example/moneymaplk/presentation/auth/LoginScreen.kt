package com.example.moneymaplk.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.clearErrors()
    }

    LaunchedEffect(uiState.authSuccess, uiState.startupDestination) {
        if (uiState.authSuccess) {
            when (uiState.startupDestination) {
                AuthStartupDestination.FINANCIAL_SETUP -> {
                    authViewModel.clearAuthSuccess()
                    onNavigateToSetup()
                }
                AuthStartupDestination.DASHBOARD -> {
                    authViewModel.clearAuthSuccess()
                    onNavigateToDashboard()
                }
                AuthStartupDestination.UNKNOWN,
                AuthStartupDestination.LOGIN -> Unit
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            authViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.passwordResetMessage) {
        if (uiState.passwordResetMessage != null) {
            showResetPasswordDialog = false
            delay(4000)
            authViewModel.clearPasswordResetMessage()
        }
    }

    LaunchedEffect(uiState.passwordResetError) {
        if (uiState.passwordResetError != null) {
            delay(5000)
            authViewModel.clearPasswordResetError()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            authViewModel.clearErrorMessage()
            authViewModel.clearPasswordResetMessage()
            authViewModel.clearPasswordResetError()
        }
    }

    if (showResetPasswordDialog) {
        PasswordResetDialog(
            uiState = uiState,
            onEmailChange = authViewModel::onForgotPasswordEmailChange,
            onSendClick = { authViewModel.sendPasswordReset() },
            onDismiss = {
                if (!uiState.isSendingPasswordReset) {
                    showResetPasswordDialog = false
                    authViewModel.clearPasswordResetForm()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        MoneyMapAuthLogo(
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to continue managing your money clearly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))
        MoneyMapTextField(
            value = uiState.email,
            onValueChange = authViewModel::onEmailChange,
            label = "Email",
            placeholder = "name@example.com",
            errorText = uiState.emailError,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(12.dp))
        MoneyMapTextField(
            value = uiState.password,
            onValueChange = authViewModel::onPasswordChange,
            label = "Password",
            errorText = uiState.passwordError,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                authViewModel.onForgotPasswordEmailChange(uiState.email)
                showResetPasswordDialog = true
            },
            enabled = !uiState.isLoading
        ) {
            Text(text = "Forgot password?")
        }
        uiState.passwordResetMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            PasswordResetStatusCard(message = message)
        }
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorState(
                title = if (message == "Sign-in cancelled.") "Sign-in cancelled" else "Login failed",
                message = message
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        MoneyMapPrimaryButton(
            text = if (uiState.isLoading) "Logging in..." else "Login",
            onClick = authViewModel::login,
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
            onClick = onNavigateToRegister,
            enabled = !uiState.isLoading
        ) {
            Text(text = "New to MoneyMap LK? Create an account")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PasswordResetDialog(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!uiState.isSendingPasswordReset) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(text = "Reset password")
        },
        text = {
            Column {
                Text(
                    text = "Enter your email and we’ll send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                MoneyMapTextField(
                    value = uiState.forgotPasswordEmail,
                    onValueChange = onEmailChange,
                    label = "Email",
                    placeholder = "name@example.com",
                    errorText = uiState.forgotPasswordEmailError,
                    keyboardType = KeyboardType.Email
                )
                uiState.passwordResetError?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSendClick,
                enabled = !uiState.isSendingPasswordReset
            ) {
                Text(text = if (uiState.isSendingPasswordReset) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isSendingPasswordReset
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun PasswordResetStatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
