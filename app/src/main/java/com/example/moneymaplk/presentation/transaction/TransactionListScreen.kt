package com.example.moneymaplk.presentation.transaction

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapRed
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TransactionListScreen(
    transactionViewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val uiState by transactionViewModel.listUiState.collectAsState()
    val errorMessage = uiState.errorMessage

    LaunchedEffect(Unit) {
        transactionViewModel.loadTransactions()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) {
            onRequireLogin()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            transactionViewModel.clearListErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            transactionViewModel.clearListErrorMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "See where your money came from and where it went.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        TransactionFilterRow(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = transactionViewModel::onTransactionFilterChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                LoadingState(
                    message = "Loading transactions...",
                    modifier = Modifier.weight(1f)
                )
            }

            errorMessage != null -> {
                ErrorState(
                    title = "Could not load transactions",
                    message = errorMessage,
                    actionText = "Try Again",
                    onActionClick = transactionViewModel::retryLoadTransactions
                )
            }

            uiState.visibleTransactions.isEmpty() -> {
                val emptyContent = uiState.selectedFilter.emptyContent()
                EmptyState(
                    title = emptyContent.title,
                    message = emptyContent.message,
                    modifier = Modifier.weight(1f),
                    actionText = "Add Transaction",
                    onActionClick = onAddTransaction
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.visibleTransactions) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionFilterRow(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransactionFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label()) }
            )
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) MoneyMapGreen else MoneyMapRed
    val amountPrefix = if (isIncome) "+" else "-"
    val transactionIcon = if (isIncome) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val secondaryAmount = if (transaction.originalCurrency == "USD") {
        "Original: USD ${formatAmount(transaction.originalAmount)}"
    } else {
        null
    }
    val behaviorLabel = if (isIncome) {
        "Income"
    } else if (transaction.isCommitted) {
        "Committed"
    } else {
        "Discretionary"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = transactionIcon,
                    contentDescription = if (isIncome) "Income" else "Expense",
                    tint = amountColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title.ifBlank { "Untitled transaction" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = behaviorLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$amountPrefix LKR ${formatAmount(transaction.convertedAmountLkr)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }
            secondaryAmount?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${formatDate(transaction)} • ${transaction.paymentMethod.ifBlank { "Payment method not set" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = behaviorLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun TransactionFilter.label(): String {
    return when (this) {
        TransactionFilter.ALL -> "All"
        TransactionFilter.INCOME -> "Income"
        TransactionFilter.EXPENSE -> "Expense"
    }
}

private fun TransactionFilter.emptyContent(): EmptyTransactionContent {
    return when (this) {
        TransactionFilter.ALL -> EmptyTransactionContent(
            title = "No transactions yet",
            message = "Add your first income or expense to start tracking your money."
        )
        TransactionFilter.INCOME -> EmptyTransactionContent(
            title = "No income found",
            message = "Income you add will appear here."
        )
        TransactionFilter.EXPENSE -> EmptyTransactionContent(
            title = "No expenses found",
            message = "Expenses you add will appear here."
        )
    }
}

private data class EmptyTransactionContent(
    val title: String,
    val message: String
)

private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

private fun formatDate(transaction: Transaction): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
        timeZone = java.util.TimeZone.getDefault()
    }.format(transaction.transactionDate.toDate())
}
