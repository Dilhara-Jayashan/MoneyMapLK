package com.example.moneymaplk.domain.model

import com.google.firebase.Timestamp

data class Transaction(
    val transactionId: String = "",
    val userId: String,
    val type: TransactionType,
    val title: String,
    val category: String,
    val incomeSource: IncomeSource?,
    val originalAmount: Double,
    val originalCurrency: String,
    val exchangeRateToLkr: Double,
    val convertedAmountLkr: Double,
    val transactionDate: Timestamp,
    val monthId: String,
    val paymentMethod: String,
    val note: String,
    val isCommitted: Boolean,
    val isDiscretionary: Boolean,
    val isRecurring: Boolean = false,
    val recurringPaymentId: String? = null,
    val followUpId: String? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class IncomeSource {
    SALARY,
    FREELANCE,
    ADSENSE,
    CRYPTO,
    INVESTMENT,
    REFUND,
    OTHER
}

enum class CurrencyCode {
    LKR,
    USD
}
