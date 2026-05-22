package com.example.moneymaplk.presentation.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapAmber
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.theme.MoneyMapSurface
import com.example.moneymaplk.core.theme.MoneyMapSurfaceVariant
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.util.formatMoney
import kotlinx.coroutines.delay

@Composable
fun ReportsAnalyticsScreen(
    reportsViewModel: ReportsViewModel,
    onRequireLogin: () -> Unit
) {
    val uiState by reportsViewModel.uiState.collectAsState()
    val report = uiState.report.takeIf {
        uiState.activeUserId != null && uiState.loadedUserId == uiState.activeUserId
    }

    LaunchedEffect(Unit) {
        reportsViewModel.loadReports()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) onRequireLogin()
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            reportsViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose { reportsViewModel.clearErrorMessage() }
    }

    when {
        uiState.isLoading && report == null -> LoadingState(message = "Loading analytics...")
        uiState.errorMessage != null && report == null -> ErrorState(
            title = "Could not load analytics",
            message = uiState.errorMessage ?: "Unknown error",
            actionText = "Try Again",
            onActionClick = reportsViewModel::retryReports
        )
        report != null -> AnalyticsContent(
            uiState = uiState.copy(report = report),
            onFilterSelected = reportsViewModel::onFilterSelected,
            onRetryClick = reportsViewModel::retryReports
        )
        else -> EmptyState(
            title = "No analytics yet",
            message = "Add income and expenses to unlock charts."
        )
    }
}

@Composable
private fun AnalyticsContent(
    uiState: ReportsUiState,
    onFilterSelected: (ReportsTimeFilter) -> Unit,
    onRetryClick: () -> Unit
) {
    val report = uiState.report ?: return
    var mode by rememberSaveable { mutableStateOf(AnalyticsMode.CHARTS) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsHeader(
                periodLabel = report.periodLabel,
                baseCurrency = report.baseCurrency
            )
        }
        item {
            SegmentedModeSelector(
                selectedMode = mode,
                onModeSelected = { mode = it }
            )
        }
        item {
            TimeFilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected
            )
        }
        uiState.errorMessage?.let { message ->
            item {
                ErrorState(
                    title = "Analytics need a refresh",
                    message = message,
                    actionText = "Try Again",
                    onActionClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item { MoneySnapshotCard(report = report) }
        if (mode == AnalyticsMode.CHARTS) {
            item { FlowAnalysisCard(report = report) }
            item { SpendingCompositionCard(report = report) }
            item { ExpectedAndRecurrentCard(report = report) }
        } else {
            item { BreakdownCard(report = report) }
            item { RecurrentReportCard(report = report) }
            item { FollowUpReportCard(report = report) }
        }
        item { SectionTitle(title = "Smart Insights", icon = Icons.Default.Refresh) }
        items(report.insights) { insight ->
            SmartInsightCard(insight = insight)
        }
    }
}

@Composable
private fun AnalyticsHeader(
    periodLabel: String,
    baseCurrency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$periodLabel report in $baseCurrency",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MoneyMapSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MoneyMapGreen
            )
        }
    }
}

@Composable
private fun SegmentedModeSelector(
    selectedMode: AnalyticsMode,
    onModeSelected: (AnalyticsMode) -> Unit
) {
    SegmentedContainer {
        AnalyticsMode.entries.forEach { mode ->
            SegmentButton(
                label = mode.label,
                icon = mode.icon,
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeFilterRow(
    selectedFilter: ReportsTimeFilter,
    onFilterSelected: (ReportsTimeFilter) -> Unit
) {
    SegmentedContainer {
        ReportsTimeFilter.entries.forEach { filter ->
            SegmentButton(
                label = filter.label(),
                icon = null,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentedContainer(content: @Composable RowScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MoneyMapSurface),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = { content() }
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (selected) Color.White else MoneyMapGreen,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = label,
            color = if (selected) Color.White else MoneyMapGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoneySnapshotCard(report: ReportsSummary) {
    AnalyticsCard {
        Text(
            text = "Money Snapshot",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Actual confirmed income and expenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "Income",
                value = formatMoney(report.totalIncomeLkr, report.baseCurrency),
                color = MoneyMapGreen,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Expenses",
                value = formatMoney(report.totalExpensesLkr, report.baseCurrency),
                color = MoneyMapAmber,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "Net",
                value = formatMoney(report.netBalanceLkr, report.baseCurrency),
                color = if (report.netBalanceLkr >= 0.0) MoneyMapGreen else MoneyMapAmber,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Left",
                value = "${report.incomeLeftRate.formatRate()}%",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MoneyMapSurfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlowAnalysisCard(report: ReportsSummary) {
    AnalyticsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Flow Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Income vs. expenses by ${report.periodLabel.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(label = "IN", color = MoneyMapGreen)
                LegendDot(label = "OUT", color = MoneyMapAmber)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FlowBarChart(points = report.flowPoints)
    }
}

@Composable
private fun FlowBarChart(points: List<ReportFlowPoint>) {
    val maxValue = points.maxOfOrNull { maxOf(it.incomeLkr, it.expenseLkr) }?.coerceAtLeast(1.0) ?: 1.0
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color.White.copy(alpha = 0.08f)
                repeat(4) { index ->
                    val y = size.height * index / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val groupWidth = size.width / points.size.coerceAtLeast(1)
                val barWidth = (groupWidth * 0.2f).coerceIn(4.dp.toPx(), 16.dp.toPx())
                points.forEachIndexed { index, point ->
                    val center = groupWidth * index + groupWidth / 2f
                    val incomeHeight = (point.incomeLkr / maxValue * size.height).toFloat()
                    val expenseHeight = (point.expenseLkr / maxValue * size.height).toFloat()
                    drawRoundRect(
                        color = MoneyMapGreen,
                        topLeft = Offset(center - barWidth - 2.dp.toPx(), size.height - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        color = MoneyMapAmber,
                        topLeft = Offset(center + 2.dp.toPx(), size.height - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SpendingCompositionCard(report: ReportsSummary) {
    val total = (report.requiredSpendingLkr + report.flexibleSpendingLkr).coerceAtLeast(0.0)
    val committedSweep = if (total > 0.0) {
        (report.requiredSpendingLkr / total * 360.0).toFloat()
    } else {
        0f
    }

    AnalyticsCard {
        Text(
            text = "Spending Composition",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Committed vs. discretionary payments",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                committedSweep = committedSweep,
                totalText = formatMoney(total, report.baseCurrency)
            )
        }
        ProgressBreakdownRow(
            label = "Committed",
            value = formatMoney(report.requiredSpendingLkr, report.baseCurrency),
            share = report.requiredSpendingShareFraction,
            color = MaterialTheme.colorScheme.primary
        )
        ProgressBreakdownRow(
            label = "Discretionary",
            value = formatMoney(report.flexibleSpendingLkr, report.baseCurrency),
            share = report.flexibleSpendingShareFraction,
            color = MoneyMapGreen
        )
    }
}

@Composable
private fun DonutChart(
    committedSweep: Float,
    totalText: String
) {
    val committedColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
            val arcSize = Size(size.width, size.height)
            drawArc(
                color = MoneyMapGreen,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = arcSize
            )
            if (committedSweep > 0f) {
                drawArc(
                    color = committedColor,
                    startAngle = -90f,
                    sweepAngle = committedSweep,
                    useCenter = false,
                    style = stroke,
                    size = arcSize
                )
            }
        }
        Text(
            text = totalText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ExpectedAndRecurrentCard(report: ReportsSummary) {
    AnalyticsCard {
        Text(
            text = "Expected & Recurrent",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Future follow-ups and active committed flows",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow("Expected income", formatMoney(report.expectedIncomeAmountLkr, report.baseCurrency))
        DetailRow("Expected payments", formatMoney(report.expectedPaymentAmountLkr, report.baseCurrency))
        DetailRow("Overdue follow-ups", "${report.overdueFollowUpCount} items")
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.08f))
        DetailRow("Recurrent income", "${report.activeRecurringIncomeCount} active")
        DetailRow("Recurrent payments", "${report.activeRecurringPaymentCount} active")
    }
}

@Composable
private fun BreakdownCard(report: ReportsSummary) {
    AnalyticsCard {
        Text(
            text = "Spending Drivers",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Where confirmed payments are going",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        val items = report.expensesByCategory.filter { it.amountLkr > 0.0 }.take(5)
        if (items.isEmpty()) {
            EmptyInlineText("No expense data for this period.")
        } else {
            items.forEach { item ->
                ProgressBreakdownRow(
                    label = item.label,
                    value = formatMoney(item.amountLkr, report.baseCurrency),
                    share = item.shareFraction,
                    color = MoneyMapAmber
                )
            }
        }
    }
}

@Composable
private fun RecurrentReportCard(report: ReportsSummary) {
    AnalyticsCard {
        Text(
            text = "Recurrent Health",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Active committed income and payment load",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        DetailRow("Income streams", "${report.activeRecurringIncomeCount}")
        DetailRow("Payment commitments", "${report.activeRecurringPaymentCount}")
        DetailRow("Monthly recurrent income", formatMoney(report.estimatedMonthlyRecurringIncomeLkr, report.baseCurrency))
        DetailRow("Monthly recurrent payments", formatMoney(report.estimatedMonthlyRecurringPaymentLkr, report.baseCurrency))
    }
}

@Composable
private fun FollowUpReportCard(report: ReportsSummary) {
    AnalyticsCard {
        Text(
            text = "Payment Follow Up",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Expected items are not counted until confirmed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        DetailRow("Waiting confirmation", "${report.waitingFollowUpCount} items")
        DetailRow("Expected income", formatMoney(report.expectedIncomeAmountLkr, report.baseCurrency))
        DetailRow("Expected payments", formatMoney(report.expectedPaymentAmountLkr, report.baseCurrency))
        DetailRow("Overdue amount", formatMoney(report.overdueFollowUpAmountLkr, report.baseCurrency))
    }
}

@Composable
private fun ProgressBreakdownRow(
    label: String,
    value: String,
    share: Float,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { share.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MoneyMapSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SmartInsightCard(insight: ReportInsight) {
    val icon = when (insight.tone) {
        InsightTone.GOOD -> Icons.Default.CheckCircle
        InsightTone.WARNING -> Icons.Default.Refresh
        InsightTone.INFO -> Icons.Default.List
    }
    val accent = when (insight.tone) {
        InsightTone.GOOD -> MoneyMapGreen
        InsightTone.WARNING -> MoneyMapAmber
        InsightTone.INFO -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MoneyMapSurface),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(accent.copy(alpha = 0.18f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun AnalyticsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MoneyMapSurface),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MoneyMapAmber
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyInlineText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.SemiBold
    )
}

private enum class AnalyticsMode(
    val label: String,
    val icon: ImageVector
) {
    CHARTS("Charts", Icons.Default.List),
    DETAILS("Details", Icons.Default.CheckCircle)
}

private fun ReportsTimeFilter.label(): String {
    return when (this) {
        ReportsTimeFilter.WEEKLY -> "Weekly"
        ReportsTimeFilter.MONTHLY -> "Monthly"
        ReportsTimeFilter.YEARLY -> "Yearly"
    }
}

private fun Double.formatRate(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format("%.1f", this)
    }
}
