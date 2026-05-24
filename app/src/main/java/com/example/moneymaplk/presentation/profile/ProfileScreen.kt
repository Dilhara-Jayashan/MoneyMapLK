package com.example.moneymaplk.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.ui.MoneyMapDropdownField
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.ui.SectionHeader
import com.example.moneymaplk.core.util.formatMoney
import com.example.moneymaplk.domain.model.UserProfile
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogoutComplete: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val profile = uiState.profile.takeIf {
        uiState.activeUserId != null && uiState.loadedUserId == uiState.activeUserId
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) {
            if (profileViewModel.consumeReturnToLoginEvent()) {
                onLogoutComplete()
            }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(4000)
            profileViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            profileViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            profileViewModel.clearSuccessMessage()
            profileViewModel.clearErrorMessage()
        }
    }

    when {
        uiState.isLoading && profile == null -> {
            LoadingState(message = "Loading profile...")
        }

        uiState.errorMessage != null && profile == null -> {
            uiState.errorMessage?.let { message ->
                ErrorState(
                    title = "Could not load profile",
                    message = message,
                    actionText = "Try Again",
                    onActionClick = profileViewModel::retryProfile
                )
            }
        }

        profile != null -> {
            ProfileContent(
                uiState = uiState.copy(profile = profile),
                onEditClick = profileViewModel::startEditing,
                onCancelEdit = profileViewModel::cancelEditing,
                onSaveClick = profileViewModel::saveProfile,
                onLogoutClick = profileViewModel::logout,
                onDisplayNameChange = profileViewModel::onDisplayNameChange,
                onCityChange = profileViewModel::onCityChange,
                onOccupationChange = profileViewModel::onOccupationChange,
                onCurrencyChange = profileViewModel::onCurrencyChange,
                onNewCurrencyCodeChange = profileViewModel::onNewCurrencyCodeChange,
                onNewCurrencyRateChange = profileViewModel::onNewCurrencyRateChange,
                onAddCurrency = profileViewModel::addCurrency,
                onRemoveCurrency = profileViewModel::removeCurrency,
                onCurrentSavingsChange = profileViewModel::onCurrentSavingsChange,
                onPlannedSavingsChange = profileViewModel::onPlannedSavingsChange,
                onSafeToSpendBufferChange = profileViewModel::onSafeToSpendBufferChange
            )
        }

        else -> {
            EmptyState(
                title = "Profile not ready",
                message = "Complete your financial setup to view your profile."
            )
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onNewCurrencyCodeChange: (String) -> Unit,
    onNewCurrencyRateChange: (String) -> Unit,
    onAddCurrency: () -> Unit,
    onRemoveCurrency: (String) -> Unit,
    onCurrentSavingsChange: (String) -> Unit,
    onPlannedSavingsChange: (String) -> Unit,
    onSafeToSpendBufferChange: (String) -> Unit
) {
    val profile = uiState.profile ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader()
        }

        uiState.successMessage?.let { message ->
            item {
                StatusCard(message = message)
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                ErrorState(
                    title = "Could not save changes",
                    message = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            ProfileInfoCard(profile = profile)
        }

        item {
            FinancialSummaryCard(profile = profile)
        }

        item {
            SectionHeader(title = "Financial Settings")
        }

        item {
            if (uiState.isEditing) {
                EditFinancialSettingsCard(
                    uiState = uiState,
                    onDisplayNameChange = onDisplayNameChange,
                    onCityChange = onCityChange,
                    onOccupationChange = onOccupationChange,
                    onCurrencyChange = onCurrencyChange,
                    onNewCurrencyCodeChange = onNewCurrencyCodeChange,
                    onNewCurrencyRateChange = onNewCurrencyRateChange,
                    onAddCurrency = onAddCurrency,
                    onRemoveCurrency = onRemoveCurrency,
                    onCurrentSavingsChange = onCurrentSavingsChange,
                    onPlannedSavingsChange = onPlannedSavingsChange,
                    onSafeToSpendBufferChange = onSafeToSpendBufferChange,
                    onSaveClick = onSaveClick,
                    onCancelClick = onCancelEdit
                )
            } else {
                MoneyMapSecondaryButton(
                    text = "Edit Financial Settings",
                    onClick = onEditClick
                )
            }
        }

        item {
            LogoutCard(
                isSaving = uiState.isSaving,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun ProfileHeader() {
    Column {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Manage your account and financial settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileInfoCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            ProfileValueRow(label = "Email", value = profile.email.ifBlank { "Not set" })
            ProfileValueRow(label = "Occupation", value = profile.occupation.ifBlank { "Not set" })
            ProfileValueRow(label = "City", value = profile.city.ifBlank { "Not set" })
        }
    }
}

@Composable
private fun FinancialSummaryCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Money Setup",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            ProfileValueRow(label = "Base currency", value = profile.defaultCurrency)
            ProfileValueRow(label = "Current available balance", value = formatMoney(profile.currentSavingsLkr, profile.defaultCurrency))
            ProfileValueRow(label = "Planned monthly savings", value = formatMoney(profile.plannedSavingsAllocationLkr, profile.defaultCurrency))
            ProfileValueRow(
                label = "Emergency buffer / do-not-spend amount",
                value = formatMoney(profile.safeToSpendBufferLkr, profile.defaultCurrency)
            )
        }
    }
}

@Composable
private fun EditFinancialSettingsCard(
    uiState: ProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onNewCurrencyCodeChange: (String) -> Unit,
    onNewCurrencyRateChange: (String) -> Unit,
    onAddCurrency: () -> Unit,
    onRemoveCurrency: (String) -> Unit,
    onCurrentSavingsChange: (String) -> Unit,
    onPlannedSavingsChange: (String) -> Unit,
    onSafeToSpendBufferChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Financial Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            MoneyMapTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChange,
                label = "Display name",
                errorText = uiState.displayNameError
            )
            MoneyMapTextField(
                value = uiState.city,
                onValueChange = onCityChange,
                label = "City"
            )
            MoneyMapTextField(
                value = uiState.occupation,
                onValueChange = onOccupationChange,
                label = "Occupation"
            )
            CurrencySelector(
                selectedCurrency = uiState.defaultCurrency,
                supportedCurrencies = uiState.supportedCurrencies,
                currencyRatesToBase = uiState.currencyRatesToBase,
                newCurrencyCode = uiState.newCurrencyCode,
                newCurrencyRate = uiState.newCurrencyRate,
                errorText = uiState.newCurrencyError,
                onCurrencyChange = onCurrencyChange,
                onNewCurrencyCodeChange = onNewCurrencyCodeChange,
                onNewCurrencyRateChange = onNewCurrencyRateChange,
                onAddCurrency = onAddCurrency,
                onRemoveCurrency = onRemoveCurrency
            )
            MoneyMapTextField(
                value = uiState.currentSavingsLkr,
                onValueChange = onCurrentSavingsChange,
                label = "Current available balance in ${uiState.defaultCurrency}",
                errorText = uiState.currentSavingsError,
                keyboardType = KeyboardType.Decimal
            )
            MoneyMapTextField(
                value = uiState.plannedSavingsAllocationLkr,
                onValueChange = onPlannedSavingsChange,
                label = "Planned monthly savings in ${uiState.defaultCurrency}",
                errorText = uiState.plannedSavingsError,
                keyboardType = KeyboardType.Decimal
            )
            MoneyMapTextField(
                value = uiState.safeToSpendBufferLkr,
                onValueChange = onSafeToSpendBufferChange,
                label = "Emergency buffer / do-not-spend amount in ${uiState.defaultCurrency}",
                errorText = uiState.safeToSpendBufferError,
                keyboardType = KeyboardType.Decimal
            )
            MoneyMapPrimaryButton(
                text = if (uiState.isSaving) "Saving..." else "Save Changes",
                onClick = onSaveClick,
                enabled = !uiState.isSaving
            )
            MoneyMapSecondaryButton(
                text = "Cancel",
                onClick = onCancelClick,
                enabled = !uiState.isSaving
            )
        }
    }
}

@Composable
private fun CurrencySelector(
    selectedCurrency: String,
    supportedCurrencies: List<String>,
    currencyRatesToBase: Map<String, Double>,
    newCurrencyCode: String,
    newCurrencyRate: String,
    errorText: String?,
    onCurrencyChange: (String) -> Unit,
    onNewCurrencyCodeChange: (String) -> Unit,
    onNewCurrencyRateChange: (String) -> Unit,
    onAddCurrency: () -> Unit,
    onRemoveCurrency: (String) -> Unit
) {
    val removableCurrencies = supportedCurrencies.filter { it != selectedCurrency }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoneyMapDropdownField(
            label = "Base currency",
            selectedValue = selectedCurrency,
            options = supportedCurrencies,
            onOptionSelected = onCurrencyChange,
            placeholder = "Select base currency"
        )
        if (supportedCurrencies.isNotEmpty()) {
            Text(
                text = supportedCurrencies.joinToString("  ") { currency ->
                    val rate = currencyRatesToBase[currency]
                    if (currency == selectedCurrency || rate == null) currency else "$currency x$rate"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MoneyMapTextField(
            value = newCurrencyCode,
            onValueChange = onNewCurrencyCodeChange,
            label = "Add currency code",
            placeholder = "EUR",
            errorText = errorText
        )
        MoneyMapTextField(
            value = newCurrencyRate,
            onValueChange = onNewCurrencyRateChange,
            label = "Exchange rate to $selectedCurrency",
            placeholder = "330",
            keyboardType = KeyboardType.Decimal
        )
        MoneyMapSecondaryButton(
            text = "Add Currency",
            onClick = onAddCurrency
        )
        if (removableCurrencies.isNotEmpty()) {
            MoneyMapDropdownField(
                label = "Remove currency",
                selectedValue = "",
                options = removableCurrencies,
                onOptionSelected = onRemoveCurrency,
                placeholder = "Select currency to remove"
            )
        }
    }
}

@Composable
private fun LogoutCard(
    isSaving: Boolean,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Sign out from this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyMapSecondaryButton(
                text = "Logout",
                onClick = onLogoutClick,
                enabled = !isSaving
            )
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MoneyMapGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
