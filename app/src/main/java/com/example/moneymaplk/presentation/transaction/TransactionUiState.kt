package com.example.moneymaplk.presentation.transaction

import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.QuickCategory
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val currency: String = "LKR",
    val baseCurrency: String = "LKR",
    val supportedCurrencies: List<String> = listOf("LKR", "USD"),
    val currencyRatesToBase: Map<String, Double> = mapOf("LKR" to 1.0, "USD" to 310.0),
    val exchangeRateToLkr: String = "310",
    val title: String = "",
    val category: String = "",
    val transactionDate: String = defaultTransactionDate(),
    val incomeSource: IncomeSource? = null,
    val isCommitted: Boolean = false,
    val isDiscretionary: Boolean = true,
    val repeatFrequency: RecurringFrequency? = null,
    val repeatUntilDate: String = "",
    val selectedQuickCategoryId: String? = null,
    val allQuickCategories: List<QuickCategory> = emptyList(),
    val quickCategories: List<QuickCategory> = emptyList(),
    val isManagingQuickCategories: Boolean = false,
    val showAddQuickCategoryDialog: Boolean = false,
    val showSaveAsCategoryDialog: Boolean = false,
    val pendingQuickCategory: QuickCategory? = null,
    val quickCategoryName: String = "",
    val quickCategoryCategory: String = "",
    val quickCategoryPaymentMethod: String = "Cash",
    val quickCategoryIsCommitted: Boolean = false,
    val quickCategoryFrequency: RecurringFrequency? = null,
    val quickCategoryRepeatUntilDate: String = "",
    val quickCategoryNameError: String? = null,
    val quickCategoryCategoryError: String? = null,
    val quickCategoryPaymentMethodError: String? = null,
    val quickCategoryFrequencyError: String? = null,
    val quickCategoryRepeatUntilDateError: String? = null,
    val isSavingCategory: Boolean = false,
    val paymentMethod: String = "Cash",
    val note: String = "",
    val amountError: String? = null,
    val exchangeRateError: String? = null,
    val titleError: String? = null,
    val categoryError: String? = null,
    val transactionDateError: String? = null,
    val incomeSourceError: String? = null,
    val spendingBehaviorError: String? = null,
    val repeatFrequencyError: String? = null,
    val repeatUntilDateError: String? = null,
    val paymentMethodError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val shouldReturnToLogin: Boolean = false
)

private fun defaultTransactionDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
