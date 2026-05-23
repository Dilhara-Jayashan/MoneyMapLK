package com.example.moneymaplk.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapRed
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.ui.MoneyMapDropdownField
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.ui.MoneyMapTopBar
import com.example.moneymaplk.core.util.formatLkr
import com.example.moneymaplk.domain.model.PaymentMethods
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringPaymentsScreen(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val uiState by recurringPaymentViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        recurringPaymentViewModel.loadRecurringPayments()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) onRequireLogin()
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(4000)
            recurringPaymentViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            recurringPaymentViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recurringPaymentViewModel.clearSuccessMessage()
            recurringPaymentViewModel.clearErrorMessage()
        }
    }

    if (uiState.isAddOrEditVisible) {
        RecurrentItemSheet(
            uiState = uiState,
            onDismiss = recurringPaymentViewModel::hideAddPaymentForm,
            onTitleChange = recurringPaymentViewModel::onTitleChange,
            onAmountChange = recurringPaymentViewModel::onAmountChange,
            onCurrencyChange = recurringPaymentViewModel::onCurrencyChange,
            onExchangeRateChange = recurringPaymentViewModel::onExchangeRateChange,
            onFrequencyChange = recurringPaymentViewModel::onFrequencyChange,
            onNextDueDateChange = recurringPaymentViewModel::onNextDueDateChange,
            onRepeatEndDateChange = recurringPaymentViewModel::onRepeatEndDateChange,
            onPaymentMethodChange = recurringPaymentViewModel::onPaymentMethodChange,
            onAutoConfirmChange = recurringPaymentViewModel::onAutoConfirmChange,
            onNoteChange = recurringPaymentViewModel::onNoteChange,
            onSaveClick = recurringPaymentViewModel::saveRecurringPayment
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MoneyMapTopBar(
            title = "Recurrent Income & Payment",
            showBackButton = true,
            onBackClick = onBackClick
        )
        RecurrentContent(
            uiState = uiState,
            onAddExpenseClick = onAddExpenseClick,
            onAddIncomeClick = onAddIncomeClick,
            onTabSelected = recurringPaymentViewModel::selectTab,
            onConfirmClick = recurringPaymentViewModel::confirmPayment,
            onSkipClick = recurringPaymentViewModel::skipPayment,
            onEditClick = recurringPaymentViewModel::showEditPaymentForm,
            onPauseClick = recurringPaymentViewModel::pausePayment,
            onResumeClick = recurringPaymentViewModel::resumePayment,
            onDeleteClick = { payment -> recurringPaymentViewModel.deletePayment(payment.paymentId) },
            onRetryClick = recurringPaymentViewModel::retryRecurringPayments,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecurrentContent(
    uiState: RecurringPaymentUiState,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onTabSelected: (RecurrentTab) -> Unit,
    onConfirmClick: (RecurringPayment) -> Unit,
    onSkipClick: (RecurringPayment) -> Unit,
    onEditClick: (RecurringPayment) -> Unit,
    onPauseClick: (RecurringPayment) -> Unit,
    onResumeClick: (RecurringPayment) -> Unit,
    onDeleteClick: (RecurringPayment) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        HeaderRow(
            onAddExpenseClick = onAddExpenseClick,
            onAddIncomeClick = onAddIncomeClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                LoadingState(message = "Loading recurrent items...", modifier = Modifier.weight(1f))
            }

            uiState.errorMessage != null &&
                uiState.upcomingPayments.isEmpty() &&
                uiState.dueTodayPayments.isEmpty() &&
                uiState.overduePayments.isEmpty() &&
                uiState.pausedPayments.isEmpty() -> {
                ErrorState(
                    title = "Could not load recurrent items",
                    message = uiState.errorMessage,
                    actionText = "Try Again",
                    onActionClick = onRetryClick,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    uiState.errorMessage?.let { message ->
                        item {
                            ErrorState(
                                title = "Something went wrong",
                                message = message,
                                actionText = "Try Again",
                                onActionClick = onRetryClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    uiState.successMessage?.let { message ->
                        item { StatusCard(message = message) }
                    }

                    item { RecurrentSummaryCard(uiState = uiState) }
                    item {
                        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                            Tab(
                                selected = uiState.selectedTab == RecurrentTab.ONGOING,
                                onClick = { onTabSelected(RecurrentTab.ONGOING) },
                                text = { Text("Ongoing") }
                            )
                            Tab(
                                selected = uiState.selectedTab == RecurrentTab.TODAY,
                                onClick = { onTabSelected(RecurrentTab.TODAY) },
                                text = { Text("Today") }
                            )
                            Tab(
                                selected = uiState.selectedTab == RecurrentTab.PAUSED,
                                onClick = { onTabSelected(RecurrentTab.PAUSED) },
                                text = { Text("Paused") }
                            )
                        }
                    }

                    when (uiState.selectedTab) {
                        RecurrentTab.ONGOING -> {
                            recurrentGroup(
                                title = "Overdue",
                                payments = uiState.overduePayments,
                                status = RecurrentStatus.OVERDUE,
                                onConfirmClick = onConfirmClick,
                                onSkipClick = onSkipClick,
                                onEditClick = onEditClick,
                                onPauseClick = onPauseClick
                            )
                            recurrentGroup(
                                title = "Due today",
                                payments = uiState.dueTodayPayments,
                                status = RecurrentStatus.DUE_TODAY,
                                onConfirmClick = onConfirmClick,
                                onSkipClick = onSkipClick,
                                onEditClick = onEditClick,
                                onPauseClick = onPauseClick
                            )
                            recurrentGroup(
                                title = "Upcoming",
                                payments = uiState.upcomingPayments,
                                status = RecurrentStatus.UPCOMING,
                                onConfirmClick = onConfirmClick,
                                onSkipClick = onSkipClick,
                                onEditClick = onEditClick,
                                onPauseClick = onPauseClick
                            )

                            if (
                                uiState.upcomingPayments.isEmpty() &&
                                uiState.dueTodayPayments.isEmpty() &&
                                uiState.overduePayments.isEmpty()
                            ) {
                                item {
                                    EmptyState(
                                        title = "No ongoing recurrent items",
                                        message = "Add income, subscriptions, rent, or any repeated payment to track what is coming.",
                                        actionText = "Add Payment",
                                        onActionClick = onAddExpenseClick,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        RecurrentTab.TODAY -> {
                            if (uiState.dueTodayPayments.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "No recurrence due today",
                                        message = "Items that need confirmation today will appear here.",
                                        actionText = "Add Payment",
                                        onActionClick = onAddExpenseClick,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                recurrentGroup(
                                    title = "Today due recurrence",
                                    payments = uiState.dueTodayPayments,
                                    status = RecurrentStatus.DUE_TODAY,
                                    onConfirmClick = onConfirmClick,
                                    onSkipClick = onSkipClick,
                                    onEditClick = onEditClick,
                                    onPauseClick = onPauseClick
                                )
                            }
                        }

                        RecurrentTab.PAUSED -> {
                            if (uiState.pausedPayments.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "No paused recurrent items",
                                        message = "Paused recurrent income and payments will appear here.",
                                        actionText = "Add Payment",
                                        onActionClick = onAddExpenseClick,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                item { SectionHeader(title = "Paused") }
                                items(
                                    items = uiState.pausedPayments,
                                    key = { payment -> payment.paymentId }
                                ) { payment ->
                                    PausedRecurrentCard(
                                        payment = payment,
                                        onResumeClick = { onResumeClick(payment) },
                                        onDeleteClick = { onDeleteClick(payment) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recurrentGroup(
    title: String,
    payments: List<RecurringPayment>,
    status: RecurrentStatus,
    onConfirmClick: (RecurringPayment) -> Unit,
    onSkipClick: (RecurringPayment) -> Unit,
    onEditClick: (RecurringPayment) -> Unit,
    onPauseClick: (RecurringPayment) -> Unit
) {
    if (payments.isEmpty()) return
    item { SectionHeader(title = title) }
    items(
        items = payments,
        key = { payment -> payment.paymentId }
    ) { payment ->
        RecurrentPaymentCard(
            payment = payment,
            status = status,
            onConfirmClick = { onConfirmClick(payment) },
            onSkipClick = { onSkipClick(payment) },
            onEditClick = { onEditClick(payment) },
            onPauseClick = { onPauseClick(payment) }
        )
    }
}

@Composable
private fun HeaderRow(
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recurrent Income & Payment",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Confirm what happened, skip what did not, and keep future cash flow visible.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddExpenseClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Payment")
            }
            OutlinedButton(onClick = onAddIncomeClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Income")
            }
        }
    }
}

@Composable
private fun RecurrentSummaryCard(uiState: RecurringPaymentUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Monthly payment commitments",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatLkr(uiState.totalMonthlyCommitmentsLkr),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${uiState.activePaymentCount} payments • ${uiState.activeIncomeCount} income • ${uiState.overdueCount} overdue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RecurrentPaymentCard(
    payment: RecurringPayment,
    status: RecurrentStatus,
    onConfirmClick: () -> Unit,
    onSkipClick: () -> Unit,
    onEditClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    val typeLabel = if (payment.type == TransactionType.INCOME) "Income" else "Payment"
    val typeColor = if (payment.type == TransactionType.INCOME) MoneyMapGreen else MoneyMapRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecurrentTitleRow(payment = payment, typeLabel = typeLabel, typeColor = typeColor)
            Spacer(modifier = Modifier.height(12.dp))
            PaymentInfoRow(label = "Frequency", value = payment.frequency.toLabel())
            Spacer(modifier = Modifier.height(6.dp))
            PaymentInfoRow(label = "Next due date", value = formatDate(payment.nextDueDate.toDate()))
            Spacer(modifier = Modifier.height(6.dp))
            PaymentInfoRow(label = "Confirm mode", value = if (payment.autoConfirm) "Automatic" else "Manual")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelLarge,
                color = status.color(),
                fontWeight = FontWeight.SemiBold
            )
            if (payment.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = payment.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onConfirmClick,
                    enabled = status != RecurrentStatus.UPCOMING
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm")
                }
                OutlinedButton(onClick = onSkipClick) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                TextButton(onClick = onPauseClick) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause")
                }
            }
        }
    }
}

@Composable
private fun PausedRecurrentCard(
    payment: RecurringPayment,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecurrentTitleRow(
                payment = payment,
                typeLabel = if (payment.type == TransactionType.INCOME) "Income" else "Payment",
                typeColor = if (payment.type == TransactionType.INCOME) MoneyMapGreen else MoneyMapRed
            )
            Spacer(modifier = Modifier.height(10.dp))
            PaymentInfoRow(label = "Next due date", value = formatDate(payment.nextDueDate.toDate()))
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onResumeClick) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Activate")
                }
                TextButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RecurrentTitleRow(
    payment: RecurringPayment,
    typeLabel: String,
    typeColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payment.title.ifBlank { "Recurrent item" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = typeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = formatLkr(payment.convertedAmountLkr),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrentItemSheet(
    uiState: RecurringPaymentUiState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onExchangeRateChange: (String) -> Unit,
    onFrequencyChange: (RecurringFrequency) -> Unit,
    onNextDueDateChange: (String) -> Unit,
    onRepeatEndDateChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onAutoConfirmChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val isEditing = uiState.editingPaymentId != null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Text(
                    text = if (isEditing) {
                        "Edit ${if (uiState.type == TransactionType.INCOME) "Income" else "Payment"}"
                    } else {
                        "Add Recurrent"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item { LockedTypeCard(type = uiState.type) }
            item {
                MoneyMapTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = "Name",
                    placeholder = if (uiState.type == TransactionType.INCOME) "Salary" else "Netflix",
                    errorText = uiState.titleError
                )
            }
            item {
                MoneyMapTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    label = "Amount",
                    placeholder = "5000",
                    errorText = uiState.amountError,
                    keyboardType = KeyboardType.Decimal
                )
            }
            item {
                OptionSection(title = "Currency") {
                    listOf("LKR", "USD").forEach { currency ->
                        FilterChip(
                            selected = uiState.currency == currency,
                            onClick = { onCurrencyChange(currency) },
                            label = { Text(text = currency) }
                        )
                    }
                }
            }
            if (uiState.currency == "USD") {
                item {
                    MoneyMapTextField(
                        value = uiState.exchangeRateToLkr,
                        onValueChange = onExchangeRateChange,
                        label = "Exchange rate to LKR",
                        placeholder = "310",
                        errorText = uiState.exchangeRateError,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
            item {
                OptionSection(title = "Frequency") {
                    RecurringFrequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = uiState.frequency == frequency,
                            onClick = { onFrequencyChange(frequency) },
                            label = { Text(text = frequency.toLabel()) }
                        )
                    }
                }
                uiState.frequencyError?.let { message -> ErrorText(message) }
            }
            item {
                MoneyMapTextField(
                    value = uiState.nextDueDate,
                    onValueChange = onNextDueDateChange,
                    label = "Next due date",
                    placeholder = "2026-05-25",
                    errorText = uiState.nextDueDateError
                )
            }
            item {
                MoneyMapTextField(
                    value = uiState.repeatEndDate,
                    onValueChange = onRepeatEndDateChange,
                    label = "Until when it repeats",
                    placeholder = "Optional, e.g. 2026-12-31",
                    errorText = uiState.repeatEndDateError
                )
            }
            item {
                MoneyMapDropdownField(
                    label = "Payment method",
                    selectedValue = uiState.paymentMethod,
                    options = PaymentMethods.values,
                    onOptionSelected = onPaymentMethodChange,
                    placeholder = "Select payment method",
                    errorText = uiState.paymentMethodError,
                    enabled = !uiState.isSaving
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.autoConfirm,
                        onCheckedChange = onAutoConfirmChange,
                        enabled = !uiState.isSaving
                    )
                    Column {
                        Text(
                            text = "Automatically confirm on due date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Manual items become overdue until confirmed or skipped.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                MoneyMapTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    label = "Note",
                    placeholder = "Optional",
                    singleLine = false
                )
            }
            item {
                MoneyMapPrimaryButton(
                    text = if (uiState.isSaving) "Saving..." else "Save Recurrent",
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving
                )
            }
            item {
                MoneyMapSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !uiState.isSaving
                )
            }
        }
    }
}

@Composable
private fun LockedTypeCard(type: TransactionType) {
    val label = if (type == TransactionType.INCOME) "Income" else "Payment"
    val description = if (type == TransactionType.INCOME) {
        "Income recurrence type is locked while editing."
    } else {
        "Payment recurrence type is locked while editing."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OptionSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MoneyMapGreen)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MoneyMapGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private enum class RecurrentStatus(val label: String) {
    OVERDUE("Overdue until confirmed"),
    DUE_TODAY("Due today"),
    UPCOMING("Upcoming")
}

@Composable
private fun RecurrentStatus.color(): androidx.compose.ui.graphics.Color {
    return when (this) {
        RecurrentStatus.OVERDUE -> MoneyMapRed
        RecurrentStatus.DUE_TODAY -> MaterialTheme.colorScheme.primary
        RecurrentStatus.UPCOMING -> MoneyMapGreen
    }
}

private fun formatDate(date: java.util.Date): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
}

private fun RecurringFrequency.toLabel(): String {
    return name.lowercase().replaceFirstChar { it.titlecase() }
}
