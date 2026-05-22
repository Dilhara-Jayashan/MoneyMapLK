package com.example.moneymaplk.domain.model

import com.google.firebase.Timestamp

data class QuickCategory(
    val categoryId: String = "",
    val userId: String = "",
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val defaultExpenseName: String = "",
    val defaultCategoryName: String = "",
    val defaultPaymentMethod: String = "",
    val defaultIsCommitted: Boolean? = null,
    val defaultIsDiscretionary: Boolean? = null,
    val defaultIsRepeating: Boolean? = null,
    val defaultFrequency: RecurringFrequency? = null,
    val defaultRepeatUntil: Timestamp? = null,
    val isSystem: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastUsedAt: Timestamp? = null,
    val usageCount: Int = 0
) {
    val displayName: String
        get() = name.ifBlank { defaultCategoryName }
}

object QuickCategoryDefaults {
    val expenseCategories: List<QuickCategory> = FinanceCategories.expenseCategories.map { categoryName ->
        QuickCategory(
            categoryId = "system_${categoryName.systemId()}",
            name = categoryName,
            type = TransactionType.EXPENSE,
            defaultCategoryName = categoryName,
            isSystem = true
        )
    }

    private fun String.systemId(): String {
        return lowercase()
            .replace("&", "and")
            .replace("/", " ")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}
