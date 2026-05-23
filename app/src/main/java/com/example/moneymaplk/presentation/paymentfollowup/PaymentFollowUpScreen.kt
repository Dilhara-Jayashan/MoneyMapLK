package com.example.moneymaplk.presentation.paymentfollowup

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapAmber
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapRed
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.ui.MoneyMapTopBar
import com.example.moneymaplk.core.ui.SectionHeader
import com.example.moneymaplk.core.util.formatLkr
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFollowUpScreen(
    paymentFollowUpViewModel: PaymentFollowUpViewModel,
    onAddExpenseActivityClick: () -> Unit,
    onRecurringItemClick: () -> Unit,
    onBackClick: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val uiState by paymentFollowUpViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        paymentFollowUpViewModel.loadFollowUps()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) onRequireLogin()
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(3000)
            paymentFollowUpViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            paymentFollowUpViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            paymentFollowUpViewModel.clearSuccessMessage()
            paymentFollowUpViewModel.clearErrorMessage()
        }
    }

    if (uiState.isAddFollowUpVisible) {
        FollowUpFormSheet(
            uiState = uiState,
            onDismiss = paymentFollowUpViewModel::hideAddFollowUpForm,
            onManualTypeChange = paymentFollowUpViewModel::onManualTypeChange,
            onReasonProjectChange = paymentFollowUpViewModel::onReasonProjectChange,
            onExpectedAmountChange = paymentFollowUpViewModel::onExpectedAmountChange,
            onCurrencyChange = paymentFollowUpViewModel::onCurrencyChange,
            onExchangeRateChange = paymentFollowUpViewModel::onExchangeRateChange,
            onDueDateChange = paymentFollowUpViewModel::onDueDateChange,
            onFollowUpNoteChange = paymentFollowUpViewModel::onFollowUpNoteChange,
            onSaveClick = paymentFollowUpViewModel::saveFollowUp
        )
    }

    uiState.confirmExchangeFollowUpId?.let {
        AlertDialog(
            onDismissRequest = paymentFollowUpViewModel::cancelConfirmExchangeRate,
            title = { Text(text = "Update exchange rate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Enter the current exchange rate to ${uiState.baseCurrency} before confirming.")
                    MoneyMapTextField(
                        value = uiState.confirmExchangeRate,
                        onValueChange = paymentFollowUpViewModel::onConfirmExchangeRateChange,
                        label = "Exchange rate to ${uiState.baseCurrency}",
                        errorText = uiState.confirmExchangeRateError,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = paymentFollowUpViewModel::confirmWithExchangeRate) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = paymentFollowUpViewModel::cancelConfirmExchangeRate) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MoneyMapTopBar(
                title = "Payment Follow Up",
                showBackButton = true,
                onBackClick = onBackClick
            )
            FollowUpContent(
                uiState = uiState,
                onAddExpenseActivityClick = onAddExpenseActivityClick,
                onNewFollowUpClick = paymentFollowUpViewModel::showAddFollowUpForm,
                onSelectBoard = paymentFollowUpViewModel::selectBoard,
                onSelectActiveTab = paymentFollowUpViewModel::selectActiveTab,
                onSelectHistoryTab = paymentFollowUpViewModel::selectHistoryTab,
                onMarkPaid = paymentFollowUpViewModel::prepareConfirmFollowUp,
                onSkip = paymentFollowUpViewModel::skipFollowUp,
                onEdit = paymentFollowUpViewModel::showEditFollowUpForm,
                onDeleteHistory = paymentFollowUpViewModel::deleteHistoryFollowUp,
                onRecurringClick = onRecurringItemClick,
                onRetry = paymentFollowUpViewModel::retryFollowUps
            )
        }
    }
}

@Composable
private fun FollowUpContent(
    uiState: PaymentFollowUpUiState,
    onAddExpenseActivityClick: () -> Unit,
    onNewFollowUpClick: () -> Unit,
    onSelectBoard: (FollowUpBoard) -> Unit,
    onSelectActiveTab: (ActiveFollowUpTab) -> Unit,
    onSelectHistoryTab: (HistoryFollowUpTab) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkip: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onRecurringClick: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Track expected income, payments, and recurrent upcoming items.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAddExpenseActivityClick) {
                Text(text = "Add Activity")
            }
            TextButton(onClick = onNewFollowUpClick) {
                Text(text = "New Follow Up")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        FollowUpSummaryRow(uiState = uiState)

        Spacer(modifier = Modifier.height(12.dp))
        BoardSelector(selected = uiState.board, onSelectBoard = onSelectBoard)

        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.board == FollowUpBoard.ACTIVE) {
            ActiveTabRow(selected = uiState.activeTab, onSelect = onSelectActiveTab)
        } else {
            HistoryTabRow(selected = uiState.historyTab, onSelect = onSelectHistoryTab)
        }

        Spacer(modifier = Modifier.height(10.dp))
        when {
            uiState.isLoading && uiState.followUps.isEmpty() && uiState.recurringPayments.isEmpty() -> {
                LoadingState(
                    message = "Loading follow-ups...",
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.errorMessage != null &&
                ((uiState.board == FollowUpBoard.ACTIVE && uiState.activeVisibleItems.isEmpty()) ||
                    (uiState.board == FollowUpBoard.HISTORY && uiState.historyVisibleItems.isEmpty())) -> {
                ErrorState(
                    title = "Could not load data",
                    message = uiState.errorMessage ?: "Unknown error",
                    actionText = "Try Again",
                    onActionClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    uiState.errorMessage?.let { message ->
                        item {
                            ErrorState(
                                title = "Something went wrong",
                                message = message,
                                actionText = "Try Again",
                                onActionClick = onRetry,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    uiState.successMessage?.let { message ->
                        item { StatusCard(message = message) }
                    }

                    if (uiState.board == FollowUpBoard.ACTIVE) {
                        if (uiState.activeVisibleItems.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No active items",
                                    message = "No items for this tab right now.",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(uiState.activeVisibleItems, key = { keyForActiveItem(it) }) { item ->
                                when (item) {
                                    is FollowUpBoardItem.FollowUpEntry -> FollowUpActiveCard(
                                        followUp = item.item.followUp,
                                        effectiveStatus = item.item.effectiveStatus,
                                        onConfirm = { onMarkPaid(item.item.followUp.followUpId) },
                                        onSkip = { onSkip(item.item.followUp.followUpId) },
                                        onEdit = { onEdit(item.item.followUp.followUpId) }
                                    )

                                    is FollowUpBoardItem.RecurringEntry -> RecurringActiveCard(
                                        payment = item.payment,
                                        onClick = onRecurringClick
                                    )
                                }
                            }
                        }
                    } else {
                        if (uiState.historyVisibleItems.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No history items",
                                    message = "Confirmed or skipped items will appear here.",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(uiState.historyVisibleItems, key = { it.followUp.followUpId }) { item ->
                                FollowUpHistoryCard(
                                    followUp = item.followUp,
                                    effectiveStatus = item.effectiveStatus,
                                    onEdit = { onEdit(item.followUp.followUpId) },
                                    onDelete = { onDeleteHistory(item.followUp.followUpId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardSelector(selected: FollowUpBoard, onSelectBoard: (FollowUpBoard) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentPill(
            label = "Active",
            selected = selected == FollowUpBoard.ACTIVE,
            onClick = { onSelectBoard(FollowUpBoard.ACTIVE) }
        )
        SegmentPill(
            label = "History",
            selected = selected == FollowUpBoard.HISTORY,
            onClick = { onSelectBoard(FollowUpBoard.HISTORY) }
        )
    }
}

@Composable
private fun ActiveTabRow(selected: ActiveFollowUpTab, onSelect: (ActiveFollowUpTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentPill("Upcoming", selected == ActiveFollowUpTab.UPCOMING) { onSelect(ActiveFollowUpTab.UPCOMING) }
        SegmentPill("Today", selected == ActiveFollowUpTab.DUE_TODAY) { onSelect(ActiveFollowUpTab.DUE_TODAY) }
        SegmentPill("Overdue", selected == ActiveFollowUpTab.OVERDUE) { onSelect(ActiveFollowUpTab.OVERDUE) }
    }
}

@Composable
private fun HistoryTabRow(selected: HistoryFollowUpTab, onSelect: (HistoryFollowUpTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentPill("Confirmed", selected == HistoryFollowUpTab.CONFIRMED) { onSelect(HistoryFollowUpTab.CONFIRMED) }
        SegmentPill("Skipped", selected == HistoryFollowUpTab.SKIPPED) { onSelect(HistoryFollowUpTab.SKIPPED) }
    }
}

@Composable
private fun SegmentPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun FollowUpSummaryRow(uiState: PaymentFollowUpUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard(
            title = "Waiting",
            value = formatLkr(uiState.waitingAmountLkr),
            accent = MoneyMapAmber,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Overdue",
            value = formatLkr(uiState.overdueAmountLkr),
            accent = MoneyMapRed,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Due Soon",
            value = uiState.dueSoonCount.toString(),
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FollowUpActiveCard(
    followUp: PaymentFollowUp,
    effectiveStatus: PaymentFollowUpStatus,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit
) {
    val statusColor = if (effectiveStatus == PaymentFollowUpStatus.OVERDUE) MoneyMapRed else MoneyMapAmber
    FollowUpBaseCard(
        title = followUp.title.ifBlank { "Expected item" },
        subtitle = if (followUp.type == TransactionType.INCOME) "Income" else "Payment",
        amount = formatLkr(followUp.convertedAmountLkr),
        statusLabel = if (effectiveStatus == PaymentFollowUpStatus.OVERDUE) "Overdue" else "Expected",
        statusColor = statusColor,
        expectedDate = followUp.expectedDate.toDate(),
        note = followUp.followUpNote,
        trailingAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onConfirm) { Text("Confirm") }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    )
}

@Composable
private fun RecurringActiveCard(
    payment: RecurringPayment,
    onClick: () -> Unit
) {
    val typeLabel = if (payment.type == TransactionType.INCOME) "Recurring Income" else "Recurring Expense"
    val typeColor = if (payment.type == TransactionType.INCOME) MoneyMapGreen else MoneyMapAmber
    FollowUpBaseCard(
        title = payment.title.ifBlank { "Recurring item" },
        subtitle = typeLabel,
        amount = formatLkr(payment.convertedAmountLkr),
        statusLabel = "Recurring",
        statusColor = typeColor,
        expectedDate = payment.nextDueDate.toDate(),
        note = payment.note,
        trailingAction = {
            TextButton(onClick = onClick) { Text("Open") }
        }
    )
}

@Composable
private fun FollowUpHistoryCard(
    followUp: PaymentFollowUp,
    effectiveStatus: PaymentFollowUpStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = if (effectiveStatus == PaymentFollowUpStatus.CONFIRMED) MoneyMapGreen else MaterialTheme.colorScheme.onSurfaceVariant
    FollowUpBaseCard(
        title = followUp.title.ifBlank { "History item" },
        subtitle = if (followUp.type == TransactionType.INCOME) "Income" else "Payment",
        amount = formatLkr(followUp.convertedAmountLkr),
        statusLabel = if (effectiveStatus == PaymentFollowUpStatus.CONFIRMED) "Confirmed" else "Skipped",
        statusColor = statusColor,
        expectedDate = followUp.expectedDate.toDate(),
        note = followUp.followUpNote,
        trailingAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    )
}

@Composable
private fun FollowUpBaseCard(
    title: String,
    subtitle: String,
    amount: String,
    statusLabel: String,
    statusColor: Color,
    expectedDate: Date,
    note: String,
    trailingAction: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = amount, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Status", statusLabel, statusColor)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("Date", formatDate(expectedDate))
            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            trailingAction()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowUpFormSheet(
    uiState: PaymentFollowUpUiState,
    onDismiss: () -> Unit,
    onManualTypeChange: (TransactionType) -> Unit,
    onReasonProjectChange: (String) -> Unit,
    onExpectedAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onExchangeRateChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onFollowUpNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = if (uiState.editingFollowUpId == null) "New Payment Follow Up" else "Edit Payment Follow Up",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SectionHeader(title = "Payment Follow Up Details")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentPill("Income", uiState.manualType == TransactionType.INCOME) {
                        onManualTypeChange(TransactionType.INCOME)
                    }
                    SegmentPill("Payment", uiState.manualType == TransactionType.EXPENSE) {
                        onManualTypeChange(TransactionType.EXPENSE)
                    }
                }
            }
            item {
                MoneyMapTextField(
                    value = uiState.reasonProject,
                    onValueChange = onReasonProjectChange,
                    label = "Name",
                    placeholder = if (uiState.manualType == TransactionType.INCOME) "Expected income" else "Expected payment",
                    errorText = uiState.reasonProjectError
                )
            }

            item {
                SectionHeader(title = "Amount")
            }
            item {
                MoneyMapTextField(
                    value = uiState.expectedAmount,
                    onValueChange = onExpectedAmountChange,
                    label = "Amount",
                    placeholder = "25000",
                    keyboardType = KeyboardType.Decimal,
                    errorText = uiState.expectedAmountError
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentPill("LKR", uiState.currency == "LKR") { onCurrencyChange("LKR") }
                    SegmentPill("USD", uiState.currency == "USD") { onCurrencyChange("USD") }
                }
            }
            if (uiState.currency == "USD") {
                item {
                    MoneyMapTextField(
                        value = uiState.exchangeRateToLkr,
                        onValueChange = onExchangeRateChange,
                        label = "Rate to LKR",
                        placeholder = "310",
                        keyboardType = KeyboardType.Decimal,
                        errorText = uiState.exchangeRateError
                    )
                }
            }

            item { SectionHeader(title = "Dates") }
            item {
                DatePickerField(
                    value = uiState.dueDate,
                    label = "Expected Date",
                    errorText = uiState.dueDateError,
                    onDateSelected = onDueDateChange
                )
            }

            item { SectionHeader(title = "Note") }
            item {
                MoneyMapTextField(
                    value = uiState.followUpNote,
                    onValueChange = onFollowUpNoteChange,
                    label = "Note",
                    placeholder = "Optional",
                    singleLine = false
                )
            }

            item {
                MoneyMapPrimaryButton(
                    text = if (uiState.isSaving) {
                        "Saving..."
                    } else if (uiState.editingFollowUpId == null) {
                        "Create"
                    } else {
                        "Update"
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String,
    label: String,
    errorText: String?,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = parseIsoDateToMillis(value)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        MoneyMapTextField(
            value = value,
            onValueChange = onDateSelected,
            label = label,
            placeholder = "YYYY-MM-DD",
            errorText = errorText,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.DateRange, contentDescription = "Open calendar")
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(formatMillisAsIsoDate(millis))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MoneyMapGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun keyForActiveItem(item: FollowUpBoardItem): String {
    return when (item) {
        is FollowUpBoardItem.FollowUpEntry -> "followup_${item.item.followUp.followUpId}"
        is FollowUpBoardItem.RecurringEntry -> "recurring_${item.payment.paymentId}"
    }
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
}

private fun parseIsoDateToMillis(value: String): Long? {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.isLenient = false
        formatter.parse(value)?.time
    }.getOrNull()
}

private fun formatMillisAsIsoDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}
