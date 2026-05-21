package com.example.moneymaplk.domain.validation

import com.example.moneymaplk.domain.model.FinanceCategories
import com.example.moneymaplk.domain.model.TransactionType

object FinanceValidation {
    private val amountPattern = Regex("^-?\\d+(\\.\\d{1,2})?$")

    fun validatePositiveAmount(value: String): ValidationResult<Double> {
        val amount = parseMoneyInput(value) ?: return ValidationResult.error("Enter a valid amount.")
        return when {
            amount < 0.0 -> ValidationResult.error("This value cannot be negative.")
            amount == 0.0 -> ValidationResult.error("Amount must be greater than 0.")
            else -> ValidationResult.success(amount)
        }
    }

    fun validateNonNegativeAmount(value: String): ValidationResult<Double> {
        val amount = parseMoneyInput(value) ?: return ValidationResult.error("Enter a valid amount.")
        return if (amount < 0.0) {
            ValidationResult.error("This value cannot be negative.")
        } else {
            ValidationResult.success(amount)
        }
    }

    fun validateCurrency(currency: String): ValidationResult<String> {
        val normalized = normalizeCurrencyCode(currency)
        return if (normalized != null) {
            ValidationResult.success(normalized)
        } else {
            ValidationResult.error("Use a 3-letter currency code.")
        }
    }

    fun validateCurrencyCode(value: String): ValidationResult<String> {
        return validateCurrency(value)
    }

    fun validateExchangeRate(
        currency: String,
        value: String,
        baseCurrency: String = "LKR"
    ): ValidationResult<Double> {
        if (currency.equals(baseCurrency, ignoreCase = true)) {
            return ValidationResult.success(1.0)
        }

        val exchangeRate = parseDecimalInput(value) ?: return ValidationResult.error("Please check the exchange rate.")
        return if (exchangeRate > 0.0) {
            ValidationResult.success(exchangeRate)
        } else {
            ValidationResult.error("Please check the exchange rate.")
        }
    }

    fun normalizeCurrencyCode(value: String): String? {
        val normalized = value.trim().uppercase()
        return normalized.takeIf { Regex("[A-Z]{3}").matches(it) }
    }

    fun validateExpenseType(
        isCommitted: Boolean,
        isDiscretionary: Boolean
    ): ValidationResult<Unit> {
        return if (isCommitted != isDiscretionary) {
            ValidationResult.success(Unit)
        } else {
            ValidationResult.error("Select an expense type.")
        }
    }

    fun validateTransactionCategory(
        type: TransactionType,
        category: String
    ): ValidationResult<String> {
        val trimmed = category.trim()
        val isValid = when (type) {
            TransactionType.INCOME -> FinanceCategories.isValidIncomeCategory(trimmed)
            TransactionType.EXPENSE -> FinanceCategories.isValidExpenseCategory(trimmed)
        }

        return if (isValid) {
            ValidationResult.success(trimmed)
        } else {
            ValidationResult.error("Select a category.")
        }
    }

    fun validateExpenseCategory(category: String): ValidationResult<String> {
        val trimmed = category.trim()
        return if (FinanceCategories.isValidExpenseCategory(trimmed)) {
            ValidationResult.success(trimmed)
        } else {
            ValidationResult.error("Select a category.")
        }
    }

    private fun parseMoneyInput(value: String): Double? {
        val trimmed = value.trim()
        if (!amountPattern.matches(trimmed)) return null
        return parseDecimalInput(trimmed)
    }

    private fun parseDecimalInput(value: String): Double? {
        val number = value.trim().toDoubleOrNull() ?: return null
        return number.takeIf { parsed -> java.lang.Double.isFinite(parsed) }
    }

}
