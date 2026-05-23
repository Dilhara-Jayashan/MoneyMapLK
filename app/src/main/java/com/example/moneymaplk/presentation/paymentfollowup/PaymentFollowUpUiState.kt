package com.example.moneymaplk.presentation.paymentfollowup

import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.TransactionType

data class PaymentFollowUpUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAddFollowUpVisible: Boolean = false,
    val editingFollowUpId: String? = null,
    val followUps: List<FollowUpListItem> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val board: FollowUpBoard = FollowUpBoard.ACTIVE,
    val activeTab: ActiveFollowUpTab = ActiveFollowUpTab.UPCOMING,
    val historyTab: HistoryFollowUpTab = HistoryFollowUpTab.CONFIRMED,
    val activeVisibleItems: List<FollowUpBoardItem> = emptyList(),
    val historyVisibleItems: List<FollowUpListItem> = emptyList(),
    val waitingAmountLkr: Double = 0.0,
    val overdueAmountLkr: Double = 0.0,
    val dueSoonCount: Int = 0,
    val baseCurrency: String = "LKR",
    val currencyRatesToBase: Map<String, Double> = mapOf("LKR" to 1.0, "USD" to 310.0),
    val confirmExchangeFollowUpId: String? = null,
    val confirmExchangeRate: String = "",
    val confirmExchangeRateError: String? = null,
    val manualType: TransactionType = TransactionType.INCOME,
    val reasonProject: String = "",
    val referenceNumber: String = "",
    val expectedAmount: String = "",
    val currency: String = "LKR",
    val exchangeRateToLkr: String = "310",
    val dueDate: String = "",
    val followUpDate: String = "",
    val followUpNote: String = "",
    val reasonProjectError: String? = null,
    val expectedAmountError: String? = null,
    val exchangeRateError: String? = null,
    val dueDateError: String? = null,
    val followUpDateError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
)

enum class FollowUpBoard {
    ACTIVE,
    HISTORY
}

enum class ActiveFollowUpTab {
    UPCOMING,
    DUE_TODAY,
    OVERDUE
}

enum class HistoryFollowUpTab {
    CONFIRMED,
    SKIPPED
}

data class FollowUpListItem(
    val followUp: PaymentFollowUp,
    val effectiveStatus: PaymentFollowUpStatus
)

sealed interface FollowUpBoardItem {
    data class FollowUpEntry(val item: FollowUpListItem) : FollowUpBoardItem
    data class RecurringEntry(val payment: RecurringPayment) : FollowUpBoardItem
}
