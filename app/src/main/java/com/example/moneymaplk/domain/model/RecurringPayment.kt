package com.example.moneymaplk.domain.model

import com.google.firebase.Timestamp

data class RecurringPayment(
    val paymentId: String = "",
    val userId: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String,
    val category: String,
    val incomeSource: IncomeSource? = null,
    val originalAmount: Double,
    val originalCurrency: String,
    val exchangeRateToLkr: Double,
    val convertedAmountLkr: Double,
    val frequency: RecurringFrequency,
    val nextDueDate: Timestamp,
    val repeatEndDate: Timestamp? = null,
    val lastPaidDate: Timestamp? = null,
    val paymentMethod: String,
    val isActive: Boolean = true,
    val isCommitted: Boolean,
    val isDiscretionary: Boolean,
    val isRecurring: Boolean = true,
    val autoCreateTransaction: Boolean = false,
    val autoConfirm: Boolean = false,
    val lastConfirmedDueDate: Timestamp? = null,
    val lastSkippedDueDate: Timestamp? = null,
    val pausedAt: Timestamp? = null,
    val note: String = ""
)

enum class RecurringFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}
