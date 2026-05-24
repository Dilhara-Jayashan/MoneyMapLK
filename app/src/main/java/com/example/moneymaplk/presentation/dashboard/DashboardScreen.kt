package com.example.moneymaplk.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapAmber
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapRed
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.ui.SectionHeader
import com.example.moneymaplk.core.util.formatMoney
import com.example.moneymaplk.core.util.formatWholeNumber
import com.example.moneymaplk.core.util.formatWholePercent
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onCreateGoalClick: () -> Unit,
    onRecurringPaymentsClick: () -> Unit,
    onPaymentFollowUpsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val summary = uiState.summary.takeIf {
        uiState.activeUserId != null && uiState.loadedUserId == uiState.activeUserId
    }
    val errorMessage = uiState.errorMessage

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) onRequireLogin()
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            dashboardViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose { dashboardViewModel.clearErrorMessage() }
    }

    when {
        uiState.isLoading && summary == null -> LoadingState(message = "Loading your money overview...")
        summary != null -> DashboardContent(
            summary = summary,
            errorMessage = uiState.errorMessage,
            onRetryClick = dashboardViewModel::retryDashboard,
            onAddTransactionClick = onAddTransactionClick,
            onViewAllTransactionsClick = onViewAllTransactionsClick,
            onCreateGoalClick = onCreateGoalClick,
            onRecurringPaymentsClick = onRecurringPaymentsClick,
            onPaymentFollowUpsClick = onPaymentFollowUpsClick,
            onReportsClick = onReportsClick,
            onProfileClick = onProfileClick
        )
        errorMessage != null -> ErrorState(
            title = "Could not load dashboard",
            message = errorMessage,
            actionText = "Try Again",
            onActionClick = dashboardViewModel::retryDashboard
        )
        uiState.isProfileMissing -> EmptyState(
            title = "Your dashboard is almost ready",
            message = "Complete your financial setup to see your money overview."
        )
        else -> EmptyState(
            title = "No dashboard data yet",
            message = "Add your first income or expense to start seeing your overview.",
            actionText = "Add Transaction",
            onActionClick = onAddTransactionClick
        )
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onCreateGoalClick: () -> Unit,
    onRecurringPaymentsClick: () -> Unit,
    onPaymentFollowUpsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardHeader(
                displayName = summary.displayName,
                baseCurrency = summary.baseCurrency,
                onProfileClick = onProfileClick
            )
        }

        if (errorMessage != null) {
            item {
                ErrorState(
                    title = "Dashboard needs a refresh",
                    message = errorMessage,
                    actionText = "Try Again",
                    onActionClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (summary.overdueFollowUpCount > 0) {
            item {
                OverdueAlertCard(
                    summary = summary,
                    onClick = onPaymentFollowUpsClick
                )
            }
        }

        item { BalanceCard(summary = summary) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SafeToSpendMiniCard(
                    summary = summary,
                    modifier = Modifier.weight(1f)
                )
                GoalMiniCard(
                    goal = summary.featuredGoal,
                    baseCurrency = summary.baseCurrency,
                    onClick = onCreateGoalClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpectedMiniCard(
                    summary = summary,
                    onClick = onPaymentFollowUpsClick,
                    modifier = Modifier.weight(1f)
                )
                RecurrentMiniCard(
                    summary = summary,
                    onClick = onRecurringPaymentsClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SmartInsightCard(message = summary.smartInsight)
        }

        item {
            SectionHeader(
                title = "Recent Activity",
                actionText = "View all",
                onActionClick = onViewAllTransactionsClick
            )
        }

        if (summary.recentTransactions.isEmpty()) {
            item {
                Text(
                    text = "Confirmed income and expenses will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(summary.recentTransactions) { transaction ->
                RecentTransactionCard(
                    transaction = transaction,
                    baseCurrency = summary.baseCurrency
                )
            }
        }

        item {
            SectionHeader(title = "Quick Access")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureAccessCard(
                    title = "Add Income or Expense",
                    description = "Record confirmed activity or create expected follow-ups.",
                    icon = Icons.Default.Add,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onAddTransactionClick
                )
                FeatureAccessCard(
                    title = "Activity Records",
                    description = "Review confirmed transactions and money history.",
                    icon = Icons.Default.List,
                    accent = MoneyMapGreen,
                    onClick = onViewAllTransactionsClick
                )
                FeatureAccessCard(
                    title = "Goals",
                    description = "Track active saving targets and progress.",
                    icon = Icons.Default.CheckCircle,
                    accent = MoneyMapAmber,
                    onClick = onCreateGoalClick
                )
                FeatureAccessCard(
                    title = "Payment Follow Up",
                    description = "Confirm, skip, or review expected income and payments.",
                    icon = Icons.Default.List,
                    accent = MoneyMapRed,
                    onClick = onPaymentFollowUpsClick
                )
                FeatureAccessCard(
                    title = "Recurrent Income & Payment",
                    description = "Manage committed repeating income and payments.",
                    icon = Icons.Default.Refresh,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onRecurringPaymentsClick
                )
                FeatureAccessCard(
                    title = "Reports",
                    description = "Open weekly, monthly, and yearly analytics.",
                    icon = Icons.Default.List,
                    accent = MoneyMapGreen,
                    onClick = onReportsClick
                )
                FeatureAccessCard(
                    title = "Profile",
                    description = "Manage base currency, exchange rates, and account settings.",
                    icon = Icons.Default.Person,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    displayName: String,
    baseCurrency: String,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingText(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Base currency $baseCurrency",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OverdueAlertCard(
    summary: DashboardSummary,
    onClick: () -> Unit
) {
    DashboardCard(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.error,
                background = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${summary.overdueFollowUpCount} overdue item${if (summary.overdueFollowUpCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = summary.firstOverdueFollowUpTitle?.let { title ->
                        "$title - ${formatMoney(summary.firstOverdueFollowUpAmountLkr, summary.baseCurrency)}"
                    } ?: "Tap to review overdue follow-ups",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = ">",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun BalanceCard(summary: DashboardSummary) {
    DashboardCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = "Current Available Balance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatMoney(summary.safeToSpendLkr + summary.safeToSpendBufferLkr, summary.baseCurrency),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BalanceMetric(
                label = "Income",
                amount = formatMoney(summary.monthlyIncomeLkr, summary.baseCurrency),
                color = MoneyMapGreen,
                modifier = Modifier.weight(1f)
            )
            BalanceMetric(
                label = "Expense",
                amount = formatMoney(summary.monthlyExpensesLkr, summary.baseCurrency),
                color = MoneyMapRed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        BalanceMetric(
            label = "Net this month",
            amount = formatMoney(summary.monthlyNetLkr, summary.baseCurrency),
            color = if (summary.monthlyNetLkr >= 0.0) MoneyMapGreen else MoneyMapAmber,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BalanceMetric(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SafeToSpendMiniCard(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        StatusDot(color = if (summary.rawSafeToSpendLkr > 0.0) MoneyMapGreen else MoneyMapAmber)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Safe to Spend",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatMoney(summary.safeToSpendLkr, summary.baseCurrency),
            style = MaterialTheme.typography.titleLarge,
            color = if (summary.rawSafeToSpendLkr > 0.0) MoneyMapGreen else MoneyMapAmber,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "After plans",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalMiniCard(
    goal: DashboardGoal?,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = "Goal",
            style = MaterialTheme.typography.titleSmall,
            color = MoneyMapAmber,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (goal == null) {
            Text(
                text = "Create a goal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Start tracking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val progress = (goal.progressPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            Text(
                text = goal.name.ifBlank { "Savings Goal" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = MoneyMapAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${formatWholePercent(goal.progressPercentage)} - ${formatMoney(goal.remainingAmountLkr, baseCurrency)} left",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpectedMiniCard(
    summary: DashboardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = "Expected",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        MiniLine("In", formatMoney(summary.expectedIncomeThisMonthLkr, summary.baseCurrency), MoneyMapGreen)
        MiniLine("Out", formatMoney(summary.expectedPaymentsThisMonthLkr, summary.baseCurrency), MoneyMapAmber)
        if (summary.overdueFollowUpCount > 0) {
            Text(
                text = "${summary.overdueFollowUpCount} overdue",
                style = MaterialTheme.typography.labelMedium,
                color = MoneyMapRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecurrentMiniCard(
    summary: DashboardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = "Recurrent",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        MiniLine("Income", "${summary.activeRecurringIncomeCount}", MoneyMapGreen)
        MiniLine("Payment", "${summary.activeRecurringPaymentCount}", MoneyMapAmber)
        Text(
            text = "Monthly ${formatMoney(summary.estimatedMonthlyRecurringIncomeLkr - summary.estimatedMonthlyRecurringPaymentLkr, summary.baseCurrency)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniLine(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmartInsightCard(message: String) {
    DashboardCard {
        Text(
            text = "Smart Insight",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureAccessCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    DashboardCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                icon = icon,
                tint = accent,
                background = accent.copy(alpha = 0.16f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionCard(
    transaction: Transaction,
    baseCurrency: String
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) MoneyMapGreen else MoneyMapRed
    val transactionIcon = if (isIncome) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val originalAmountText = if (transaction.originalCurrency != baseCurrency) {
        "${transaction.originalCurrency} ${formatWholeNumber(transaction.originalAmount)}"
    } else {
        null
    }
    val behaviorLabel = when {
        isIncome -> "Income"
        transaction.isCommitted -> "Committed"
        else -> "Discretionary"
    }

    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                icon = transactionIcon,
                tint = amountColor,
                background = amountColor.copy(alpha = 0.16f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title.ifBlank { "Untitled transaction" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$behaviorLabel - ${formatDate(transaction)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                originalAmountText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "$amountPrefix ${formatMoney(transaction.convertedAmountLkr, baseCurrency)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    Card(
        modifier = clickableModifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun IconBubble(
    icon: ImageVector,
    tint: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(background, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

private fun greetingText(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

private fun formatDate(transaction: Transaction): String {
    return SimpleDateFormat("MMM dd", Locale.US).format(transaction.transactionDate.toDate())
}
