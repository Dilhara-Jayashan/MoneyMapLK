package com.example.moneymaplk.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.ui.MoneyMapTopBar
import com.example.moneymaplk.core.ui.SectionHeader
import kotlinx.coroutines.delay

@Composable
fun FinancialSetupScreen(
    setupViewModel: SetupViewModel,
    onSetupComplete: () -> Unit,
    onRequireLogin: () -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val uiState by setupViewModel.uiState.collectAsState()
    val currencyLabel = uiState.preferredCurrency
    val isUsd = uiState.preferredCurrency == "USD"
    val currentBalancePlaceholder = if (isUsd) "e.g., 150" else "e.g., 50000"
    val monthlyIncomePlaceholder = if (isUsd) "e.g., 1200" else "e.g., 35000"
    val plannedSavingsPlaceholder = if (isUsd) "e.g., 50" else "e.g., 10000"
    val emergencyBufferPlaceholder = if (isUsd) "e.g., 100" else "e.g., 20000"

    LaunchedEffect(Unit) {
        setupViewModel.checkAuthenticatedUser()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) {
            onRequireLogin()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            setupViewModel.clearSaveSuccess()
            onSetupComplete()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            setupViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            setupViewModel.clearSaveSuccess()
            setupViewModel.clearErrorMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (showBackButton) {
            MoneyMapTopBar(
                title = "Financial Setup",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (!showBackButton) {
                Text(
                    text = "Financial Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set your baseline so MoneyMap LK can calculate savings and safe-to-spend clearly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "Preferred Currency")
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CurrencyChip(
                label = "LKR",
                selected = uiState.preferredCurrency == "LKR",
                enabled = !uiState.isSaving,
                onClick = { setupViewModel.onPreferredCurrencyChange("LKR") }
            )
            CurrencyChip(
                label = "USD",
                selected = uiState.preferredCurrency == "USD",
                enabled = !uiState.isSaving,
                onClick = { setupViewModel.onPreferredCurrencyChange("USD") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(title = "Money Baseline")
        Spacer(modifier = Modifier.height(12.dp))
        if (uiState.preferredCurrency == "USD") {
            MoneyMapTextField(
                value = uiState.exchangeRateToLkr,
                onValueChange = setupViewModel::onExchangeRateChange,
                label = "Exchange rate to LKR",
                placeholder = "e.g., 310",
                errorText = uiState.exchangeRateError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        MoneyMapTextField(
            value = uiState.currentSavings,
            onValueChange = setupViewModel::onCurrentSavingsChange,
            label = "Current available balance in $currencyLabel",
            placeholder = currentBalancePlaceholder,
            errorText = uiState.currentSavingsError,
            keyboardType = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(12.dp))
        MoneyMapTextField(
            value = uiState.monthlySalary,
            onValueChange = setupViewModel::onMonthlySalaryChange,
            label = "Monthly income baseline in $currencyLabel",
            placeholder = monthlyIncomePlaceholder,
            errorText = uiState.monthlySalaryError,
            keyboardType = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(12.dp))
        MoneyMapTextField(
            value = uiState.plannedSavingsAllocation,
            onValueChange = setupViewModel::onPlannedSavingsChange,
            label = "Planned monthly savings in $currencyLabel",
            placeholder = plannedSavingsPlaceholder,
            errorText = uiState.plannedSavingsError,
            keyboardType = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(12.dp))
        MoneyMapTextField(
            value = uiState.safeToSpendBuffer,
            onValueChange = setupViewModel::onSafeToSpendBufferChange,
            label = "Emergency buffer / do-not-spend amount in $currencyLabel",
            placeholder = emergencyBufferPlaceholder,
            errorText = uiState.safeToSpendBufferError,
            keyboardType = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(24.dp))
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            ErrorState(
                title = "Setup failed",
                message = message
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        MoneyMapPrimaryButton(
            text = if (uiState.isSaving) "Saving setup..." else "Save Setup",
            onClick = setupViewModel::saveSetup,
            enabled = !uiState.isSaving
        )
        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CurrencyChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text = label) },
        modifier = Modifier.fillMaxWidth(0.45f)
    )
}
