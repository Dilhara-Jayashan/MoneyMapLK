package com.example.moneymaplk.presentation.dashboard

import com.example.moneymaplk.domain.model.Transaction

data class DashboardUiState(
    val isLoading: Boolean = false,
    val activeUserId: String? = null,
    val loadedUserId: String? = null,
    val summary: DashboardSummary? = null,
    val errorMessage: String? = null,
    val isProfileMissing: Boolean = false,
    val shouldReturnToLogin: Boolean = false
)

data class DashboardSummary(
    val displayName: String,
    val baseCurrency: String,
    val currentSavingsLkr: Double,
    val monthlyIncomeLkr: Double,
    val monthlyExpensesLkr: Double,
    val monthlyNetLkr: Double,
    val safeToSpendLkr: Double,
    val rawSafeToSpendLkr: Double,
    val safeToSpendBufferLkr: Double,
    val safeToSpendHelperText: String,
    val upcomingRequiredRecurringPaymentsLkr: Double,
    val expectedIncomeThisMonthLkr: Double,
    val expectedPaymentsThisMonthLkr: Double,
    val overdueFollowUpCount: Int,
    val firstOverdueFollowUpTitle: String?,
    val firstOverdueFollowUpAmountLkr: Double,
    val activeRecurringIncomeCount: Int,
    val activeRecurringPaymentCount: Int,
    val estimatedMonthlyRecurringIncomeLkr: Double,
    val estimatedMonthlyRecurringPaymentLkr: Double,
    val featuredGoal: DashboardGoal?,
    val recentTransactions: List<Transaction>,
    val requiredSpendingLkr: Double,
    val flexibleSpendingLkr: Double,
    val smartInsight: String
)

data class DashboardGoal(
    val name: String,
    val targetAmountLkr: Double,
    val savedAmountLkr: Double,
    val remainingAmountLkr: Double,
    val progressPercentage: Double
)
