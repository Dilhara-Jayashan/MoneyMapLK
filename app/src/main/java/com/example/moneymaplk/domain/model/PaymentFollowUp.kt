package com.example.moneymaplk.domain.model

import com.google.firebase.Timestamp

data class PaymentFollowUp(
    val followUpId: String = "",
    val userId: String,
    val type: TransactionType,
    val title: String,
    val category: String,
    val incomeSource: IncomeSource? = null,
    val referenceNumber: String = "",
    val status: PaymentFollowUpStatus = PaymentFollowUpStatus.EXPECTED,
    val originalAmount: Double,
    val originalCurrency: String,
    val exchangeRateToLkr: Double,
    val convertedAmountLkr: Double,
    val expectedDate: Timestamp,
    val confirmedDate: Timestamp? = null,
    val skippedDate: Timestamp? = null,
    val followUpDate: Timestamp? = null,
    val paymentMethod: String,
    val followUpNote: String = "",
    val isCommitted: Boolean,
    val isDiscretionary: Boolean,
    val isRecurring: Boolean = false,
    val recurringPaymentId: String? = null,
    val linkedTransactionId: String? = null,
    val updatedAt: Timestamp? = null
)

enum class PaymentFollowUpStatus {
    EXPECTED,
    OVERDUE,
    CONFIRMED,
    SKIPPED
}
