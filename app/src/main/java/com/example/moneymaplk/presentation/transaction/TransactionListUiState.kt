package com.example.moneymaplk.presentation.transaction

import com.example.moneymaplk.domain.model.Transaction

enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class TransactionListUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val visibleTransactions: List<Transaction> = emptyList(),
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val errorMessage: String? = null,
    val shouldReturnToLogin: Boolean = false
)
