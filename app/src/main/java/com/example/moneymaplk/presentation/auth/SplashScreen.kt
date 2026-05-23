package com.example.moneymaplk.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkCurrentUser()
    }

    LaunchedEffect(uiState.startupDestination) {
        when (uiState.startupDestination) {
            AuthStartupDestination.LOGIN -> onNavigateToLogin()
            AuthStartupDestination.FINANCIAL_SETUP -> onNavigateToSetup()
            AuthStartupDestination.DASHBOARD -> onNavigateToDashboard()
            AuthStartupDestination.UNKNOWN -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MoneyMapAuthLogo(modifier = Modifier.size(104.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "MoneyMap LK",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Finance with clarity",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
