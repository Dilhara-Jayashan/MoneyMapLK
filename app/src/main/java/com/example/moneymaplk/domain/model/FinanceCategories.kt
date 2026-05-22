package com.example.moneymaplk.domain.model

object FinanceCategories {
    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "AdSense",
        "Crypto",
        "Investment",
        "Refund",
        "Other"
    )

    val expenseCategories = listOf(
        "Food & Dining",
        "Transport",
        "Rent",
        "Utilities",
        "Subscriptions",
        "Health",
        "Shopping",
        "Education",
        "Entertainment",
        "Family",
        "Savings / Goal",
        "Business / Work",
        "Other"
    )

    fun isValidIncomeCategory(category: String): Boolean {
        return incomeCategories.contains(category.trim())
    }

    fun isValidExpenseCategory(category: String): Boolean {
        return expenseCategories.contains(category.trim())
    }

    fun incomeSourceForCategory(category: String): IncomeSource? {
        return when (category.trim()) {
            "Salary" -> IncomeSource.SALARY
            "Freelance" -> IncomeSource.FREELANCE
            "AdSense" -> IncomeSource.ADSENSE
            "Crypto" -> IncomeSource.CRYPTO
            "Investment" -> IncomeSource.INVESTMENT
            "Refund" -> IncomeSource.REFUND
            "Other" -> IncomeSource.OTHER
            else -> null
        }
    }
}
