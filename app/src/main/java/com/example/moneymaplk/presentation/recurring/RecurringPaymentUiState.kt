package com.example.moneymaplk.presentation.recurring

import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.TransactionType

data class RecurringPaymentUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAddOrEditVisible: Boolean = false,
    val editingPaymentId: String? = null,
    val selectedTab: RecurrentTab = RecurrentTab.ONGOING,
    val upcomingPayments: List<RecurringPayment> = emptyList(),
    val dueTodayPayments: List<RecurringPayment> = emptyList(),
    val overduePayments: List<RecurringPayment> = emptyList(),
    val pausedPayments: List<RecurringPayment> = emptyList(),
    val totalMonthlyCommitmentsLkr: Double = 0.0,
    val activeIncomeCount: Int = 0,
    val activePaymentCount: Int = 0,
    val overdueCount: Int = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val category: String = "",
    val amount: String = "",
    val currency: String = "LKR",
    val exchangeRateToLkr: String = "310",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val nextDueDate: String = "",
    val repeatEndDate: String = "",
    val paymentMethod: String = "",
    val isCommitted: Boolean = true,
    val isDiscretionary: Boolean = false,
    val autoConfirm: Boolean = false,
    val note: String = "",
    val titleError: String? = null,
    val categoryError: String? = null,
    val amountError: String? = null,
    val exchangeRateError: String? = null,
    val frequencyError: String? = null,
    val nextDueDateError: String? = null,
    val repeatEndDateError: String? = null,
    val paymentMethodError: String? = null,
    val expenseTypeError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
)

enum class RecurrentTab {
    ONGOING,
    TODAY,
    PAUSED
}
