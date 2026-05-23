package com.example.moneymaplk.presentation.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymaplk.core.theme.MoneyMapAmber
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapRed
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.MoneyMapDropdownField
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.ui.MoneyMapTopBar
import com.example.moneymaplk.core.ui.SectionHeader
import com.example.moneymaplk.domain.model.PaymentMethods
import com.example.moneymaplk.domain.model.QuickCategory
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun AddTransactionScreen(
    transactionViewModel: TransactionViewModel,
    onSaved: () -> Unit,
    onBackClick: () -> Unit,
    onRequireLogin: () -> Unit,
    initialType: TransactionType? = null,
    initialCommitted: Boolean = false
) {
    val uiState by transactionViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        transactionViewModel.loadQuickCategories()
    }

    LaunchedEffect(initialType, initialCommitted) {
        initialType?.let { type ->
            transactionViewModel.startTransactionFlow(
                type = type,
                committed = initialCommitted
            )
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            transactionViewModel.clearSaveSuccess()
            onSaved()
        }
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) {
            onRequireLogin()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            transactionViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            transactionViewModel.clearSaveSuccess()
            transactionViewModel.clearErrorMessage()
        }
    }

    if (uiState.showSaveAsCategoryDialog) {
        AlertDialog(
            onDismissRequest = transactionViewModel::skipSaveQuickCategory,
            title = { Text(text = "Save quick add?") },
            text = { Text(text = "Do you want to save this as a reusable quick add?") },
            confirmButton = {
                TextButton(
                    onClick = transactionViewModel::savePendingQuickCategory,
                    enabled = !uiState.isSavingCategory
                ) {
                    Text(text = if (uiState.isSavingCategory) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = transactionViewModel::skipSaveQuickCategory,
                    enabled = !uiState.isSavingCategory
                ) {
                    Text(text = "Not now")
                }
            }
        )
    }

    if (uiState.showAddQuickCategoryDialog) {
        AddQuickCategoryDialog(
            uiState = uiState,
            onDismiss = transactionViewModel::hideAddQuickCategoryDialog,
            onNameChange = transactionViewModel::onQuickCategoryNameChange,
            onPaymentMethodChange = transactionViewModel::onQuickCategoryPaymentMethodChange,
            onCommittedChange = transactionViewModel::onQuickCategoryCommittedChange,
            onFrequencyChange = transactionViewModel::onQuickCategoryFrequencyChange,
            onRepeatUntilDateChange = transactionViewModel::onQuickCategoryRepeatUntilDateChange,
            onSave = transactionViewModel::saveQuickCategoryFromForm
        )
    }

    TransactionQuickAddContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onTypeChange = transactionViewModel::onTypeChange,
        onCurrencyChange = transactionViewModel::onCurrencyChange,
        onExchangeRateChange = transactionViewModel::onExchangeRateChange,
        onAmountChange = transactionViewModel::onAmountChange,
        onCategoryClick = transactionViewModel::onQuickCategorySelected,
        onAddCategoryClick = transactionViewModel::showAddQuickCategoryDialog,
        onManageClick = transactionViewModel::toggleQuickCategoryManageMode,
        onDeleteCategoryClick = transactionViewModel::deleteQuickCategory,
        onTitleChange = transactionViewModel::onTitleChange,
        onDateChange = transactionViewModel::onTransactionDateChange,
        onPaymentMethodChange = transactionViewModel::onPaymentMethodChange,
        onNoteChange = transactionViewModel::onNoteChange,
        onCommittedChange = transactionViewModel::onSpendingBehaviorChange,
        onFrequencyChange = transactionViewModel::onRepeatFrequencyChange,
        onRepeatUntilDateChange = transactionViewModel::onRepeatUntilDateChange,
        onSave = transactionViewModel::saveTransaction
    )
    return
}

@Composable
private fun TransactionQuickAddContent(
    uiState: TransactionUiState,
    onBackClick: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onExchangeRateChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryClick: (QuickCategory) -> Unit,
    onAddCategoryClick: () -> Unit,
    onManageClick: () -> Unit,
    onDeleteCategoryClick: (QuickCategory) -> Unit,
    onTitleChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCommittedChange: (Boolean) -> Unit,
    onFrequencyChange: (RecurringFrequency) -> Unit,
    onRepeatUntilDateChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TransactionAmountHeader(
            amount = uiState.amount,
            currency = uiState.currency,
            baseCurrency = uiState.baseCurrency,
            supportedCurrencies = uiState.supportedCurrencies,
            type = uiState.type,
            isSaving = uiState.isSaving,
            onBackClick = onBackClick,
            onTypeChange = onTypeChange,
            onCurrencyChange = onCurrencyChange,
            onAmountChange = onAmountChange
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            if (uiState.currency != uiState.baseCurrency) {
                MoneyMapTextField(
                    value = uiState.exchangeRateToLkr,
                    onValueChange = onExchangeRateChange,
                    label = "Exchange rate to ${uiState.baseCurrency}",
                    errorText = uiState.exchangeRateError,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            uiState.amountError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            QuickCategorySection(
                categories = uiState.quickCategories,
                selectedCategoryId = uiState.selectedQuickCategoryId,
                isManaging = uiState.isManagingQuickCategories,
                isSaving = uiState.isSaving,
                onAddClick = onAddCategoryClick,
                onManageClick = onManageClick,
                onCategoryClick = onCategoryClick,
                onDeleteClick = onDeleteCategoryClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            ExpenseTextCard(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = if (uiState.type == TransactionType.EXPENSE) "Expense name" else "Income name",
                placeholder = if (uiState.type == TransactionType.EXPENSE) "Expense name" else "Income name",
                icon = Icons.Default.Edit,
                errorText = uiState.titleError
            )

            Spacer(modifier = Modifier.height(14.dp))
            ExpenseDateCard(
                value = uiState.transactionDate,
                label = "Date",
                placeholder = "YYYY-MM-DD",
                icon = Icons.Default.DateRange,
                errorText = uiState.transactionDateError,
                onDateSelected = onDateChange
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoneyMapDropdownField(
                label = "Payment method",
                selectedValue = uiState.paymentMethod,
                options = PaymentMethods.values,
                onOptionSelected = onPaymentMethodChange,
                placeholder = "Select payment method",
                errorText = uiState.paymentMethodError,
                enabled = !uiState.isSaving
            )

            Spacer(modifier = Modifier.height(14.dp))
            ExpenseTextCard(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = "Note",
                placeholder = "Add a note...",
                icon = Icons.Default.Edit,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(28.dp))
            CommittedCard(
                type = uiState.type,
                checked = uiState.isCommitted,
                enabled = !uiState.isSaving,
                errorText = uiState.spendingBehaviorError,
                onCheckedChange = onCommittedChange
            )

            if (uiState.isCommitted) {
                Spacer(modifier = Modifier.height(18.dp))
                RepeatFrequencyCard(
                    selectedFrequency = uiState.repeatFrequency,
                    errorText = uiState.repeatFrequencyError,
                    onFrequencyChange = onFrequencyChange
                )
                Spacer(modifier = Modifier.height(14.dp))
                ExpenseDateCard(
                    value = uiState.repeatUntilDate,
                    label = "Until when it repeats",
                    placeholder = "YYYY-MM-DD",
                    icon = Icons.Default.DateRange,
                    errorText = uiState.repeatUntilDateError,
                    onDateSelected = onRepeatUntilDateChange
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorState(
                    title = "Transaction failed",
                    message = message
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
            MoneyMapPrimaryButton(
                text = when {
                    uiState.isSaving && uiState.type == TransactionType.EXPENSE -> "Saving expense..."
                    uiState.isSaving -> "Saving income..."
                    uiState.type == TransactionType.EXPENSE -> "Save Expense"
                    else -> "Save Income"
                },
                onClick = onSave,
                enabled = !uiState.isSaving
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TransactionAmountHeader(
    amount: String,
    currency: String,
    baseCurrency: String,
    supportedCurrencies: List<String>,
    type: TransactionType,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 230.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TransactionSegmentedControl(
                    selectedType = type,
                    enabled = !isSaving,
                    onTypeChange = onTypeChange
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(52.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Enter Amount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                enabled = !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$currency ",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Box {
                                if (amount.isBlank()) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 46.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                }
            )
            CompactCurrencyDropdown(
                selectedCurrency = currency,
                options = supportedCurrencies,
                enabled = !isSaving,
                onCurrencyChange = onCurrencyChange
            )
        }
    }
}

@Composable
private fun TransactionSegmentedControl(
    selectedType: TransactionType,
    enabled: Boolean,
    onTypeChange: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentButton(
            label = "Expense",
            selected = selectedType == TransactionType.EXPENSE,
            enabled = enabled,
            onClick = { onTypeChange(TransactionType.EXPENSE) }
        )
        SegmentButton(
            label = "Income",
            selected = selectedType == TransactionType.INCOME,
            enabled = enabled,
            onClick = { onTypeChange(TransactionType.INCOME) }
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactCurrencyDropdown(
    selectedCurrency: String,
    options: List<String>,
    enabled: Boolean,
    onCurrencyChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = enabled) { expanded = true }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedCurrency,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        expanded = false
                        onCurrencyChange(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickCategorySection(
    categories: List<QuickCategory>,
    selectedCategoryId: String?,
    isManaging: Boolean,
    isSaving: Boolean,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit,
    onCategoryClick: (QuickCategory) -> Unit,
    onDeleteClick: (QuickCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Quick add",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap a saved expense, type the amount, then save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onManageClick, enabled = !isSaving) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isManaging) "Done" else "Manage")
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (categories + null).chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    if (category == null) {
                        NewQuickCategoryTile(
                            enabled = !isSaving,
                            onClick = onAddClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        QuickCategoryTile(
                            category = category,
                            selected = selectedCategoryId == category.categoryId,
                            isManaging = isManaging,
                            enabled = !isSaving,
                            onClick = { onCategoryClick(category) },
                            onDeleteClick = { onDeleteClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickCategoryTile(
    category: QuickCategory,
    selected: Boolean,
    isManaging: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCommitted = category.defaultIsCommitted == true || category.defaultIsRepeating == true
    val tileColor = if (selected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clickable(enabled = enabled && !isManaging, onClick = onClick),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = tileColor),
            border = BorderStroke(
                width = 1.dp,
                color = if (selected) MoneyMapRed else Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (selected) MoneyMapRed.copy(alpha = 0.26f) else MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.displayName.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MoneyMapRed else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        if (isCommitted) {
            Box(
                modifier = Modifier
                    .offset(x = 7.dp, y = 7.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(MoneyMapAmber)
            )
        }
        if (isManaging && !category.isSystem) {
            IconButton(
                onClick = onDeleteClick,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MoneyMapRed)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${category.displayName}",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NewQuickCategoryTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "New",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpenseTextCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    trailingText: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (!trailingText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MoneyMapGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        errorText?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDateCard(
    value: String,
    label: String,
    placeholder: String,
    icon: ImageVector,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toDatePickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toInputDate()
                            ?.let(onDateSelected)
                        showPicker = false
                    }
                ) {
                    Text(text = "Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.secondary else MoneyMapGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        errorText?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CommittedCard(
    type: TransactionType,
    checked: Boolean,
    enabled: Boolean,
    errorText: String?,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MoneyMapAmber,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Committed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (type == TransactionType.EXPENSE) {
                        "Recurring fixed expense"
                    } else {
                        "Recurring income"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
    errorText?.let { message ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RepeatFrequencyCard(
    selectedFrequency: RecurringFrequency?,
    errorText: String?,
    onFrequencyChange: (RecurringFrequency) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MoneyMapAmber.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MoneyMapAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "How often it repeats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecurringFrequency.values().forEach { frequency ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (selectedFrequency == frequency) {
                                    MoneyMapAmber
                                } else {
                                    MaterialTheme.colorScheme.background
                                }
                            )
                            .clickable { onFrequencyChange(frequency) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedFrequency == frequency) {
                                MaterialTheme.colorScheme.background
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
    errorText?.let { message ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AddQuickCategoryDialog(
    uiState: TransactionUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onCommittedChange: (Boolean) -> Unit,
    onFrequencyChange: (RecurringFrequency) -> Unit,
    onRepeatUntilDateChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add quick add") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (uiState.type == TransactionType.EXPENSE) {
                        "Save a reusable expense template. Amount is always entered when you add the expense."
                    } else {
                        "Save a reusable income template. Amount is always entered when you add the income."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoneyMapTextField(
                    value = uiState.quickCategoryName,
                    onValueChange = onNameChange,
                    label = if (uiState.type == TransactionType.EXPENSE) "Expense name" else "Income name",
                    placeholder = if (uiState.type == TransactionType.EXPENSE) "e.g., Spotify" else "e.g., Salary",
                    errorText = uiState.quickCategoryNameError
                )
                MoneyMapDropdownField(
                    label = "Payment method",
                    selectedValue = uiState.quickCategoryPaymentMethod,
                    options = PaymentMethods.values,
                    onOptionSelected = onPaymentMethodChange,
                    placeholder = "Select payment method",
                    errorText = uiState.quickCategoryPaymentMethodError,
                    enabled = !uiState.isSavingCategory
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Committed",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = uiState.quickCategoryIsCommitted,
                        onCheckedChange = onCommittedChange,
                        enabled = !uiState.isSavingCategory
                    )
                }
                if (uiState.quickCategoryIsCommitted) {
                    MoneyMapDropdownField(
                        label = "How often it repeats",
                        selectedValue = uiState.quickCategoryFrequency?.name.orEmpty(),
                        options = RecurringFrequency.values().map { it.name },
                        onOptionSelected = { selected ->
                            onFrequencyChange(RecurringFrequency.valueOf(selected))
                        },
                        placeholder = "Select frequency",
                        errorText = uiState.quickCategoryFrequencyError,
                        enabled = !uiState.isSavingCategory
                    )
                    ExpenseDateCard(
                        value = uiState.quickCategoryRepeatUntilDate,
                        label = "Until when it repeats",
                        placeholder = "YYYY-MM-DD",
                        icon = Icons.Default.DateRange,
                        errorText = uiState.quickCategoryRepeatUntilDateError,
                        onDateSelected = onRepeatUntilDateChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !uiState.isSavingCategory
            ) {
                Text(text = if (uiState.isSavingCategory) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isSavingCategory
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun String.toDatePickerMillis(): Long? {
    if (isBlank()) return null
    return runCatching {
        inputDateFormatter().parse(this)?.time
    }.getOrNull()
}

private fun Long.toInputDate(): String {
    return inputDateFormatter().format(Date(this))
}

private fun isFutureInputDate(value: String): Boolean {
    return runCatching {
        val selected = inputDateFormatter().parse(value) ?: return false
        val today = inputDateFormatter().parse(inputDateFormatter().format(Date())) ?: return false
        selected.after(today)
    }.getOrDefault(false)
}

private fun inputDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
