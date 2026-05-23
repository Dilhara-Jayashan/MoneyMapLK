package com.example.moneymaplk.presentation.reports

data class ReportsUiState(
    val isLoading: Boolean = false,
    val activeUserId: String? = null,
    val loadedUserId: String? = null,
    val selectedFilter: ReportsTimeFilter = ReportsTimeFilter.WEEKLY,
    val report: ReportsSummary? = null,
    val errorMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
)

enum class ReportsTimeFilter {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class ReportsSummary(
    val baseCurrency: String,
    val periodLabel: String,
    val totalIncomeLkr: Double,
    val totalExpensesLkr: Double,
    val netBalanceLkr: Double,
    val incomeLeftRate: Double,
    val incomeBySource: List<ReportBreakdownItem>,
    val expensesByCategory: List<ReportBreakdownItem>,
    val requiredSpendingLkr: Double,
    val flexibleSpendingLkr: Double,
    val requiredSpendingSharePercentage: Double,
    val requiredSpendingShareFraction: Float,
    val flexibleSpendingSharePercentage: Double,
    val flexibleSpendingShareFraction: Float,
    val flowPoints: List<ReportFlowPoint>,
    val activeRecurringIncomeCount: Int,
    val activeRecurringPaymentCount: Int,
    val estimatedMonthlyRecurringIncomeLkr: Double,
    val estimatedMonthlyRecurringPaymentLkr: Double,
    val expectedIncomeAmountLkr: Double,
    val expectedPaymentAmountLkr: Double,
    val waitingFollowUpCount: Int,
    val overdueFollowUpCount: Int,
    val overdueFollowUpAmountLkr: Double,
    val insights: List<ReportInsight>,
    val hasTransactions: Boolean
)

data class ReportBreakdownItem(
    val label: String,
    val amountLkr: Double,
    val sharePercentage: Double,
    val shareFraction: Float
)

data class ReportFlowPoint(
    val label: String,
    val incomeLkr: Double,
    val expenseLkr: Double
)

data class ReportInsight(
    val title: String,
    val subtitle: String,
    val tone: InsightTone
)

enum class InsightTone {
    GOOD,
    WARNING,
    INFO
}
