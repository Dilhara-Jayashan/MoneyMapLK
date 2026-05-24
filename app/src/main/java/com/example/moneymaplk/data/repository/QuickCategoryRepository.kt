package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.QuickCategory
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.TransactionType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface QuickCategoryRepository {
    val currentUserId: String?

    fun observeCategories(userId: String): Flow<Result<List<QuickCategory>>>
    suspend fun addCategory(userId: String, category: QuickCategory): Result<QuickCategory>
    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit>
    suspend fun updateCategoryUsage(userId: String, categoryId: String): Result<Unit>
}

class FirebaseQuickCategoryRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : QuickCategoryRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeCategories(userId: String): Flow<Result<List<QuickCategory>>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load quick categories.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { document -> document.toQuickCategory() }

                trySend(Result.success(categories))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun addCategory(
        userId: String,
        category: QuickCategory
    ): Result<QuickCategory> {
        return runCatching {
            val categoryRef = firestore.collection("users")
                .document(userId)
                .collection("categories")
                .document()
            val now = Timestamp.now()

            val categoryData = mutableMapOf<String, Any?>(
                "categoryId" to categoryRef.id,
                "userId" to userId,
                "name" to category.displayName.trim(),
                "type" to category.type.name,
                "defaultCategoryName" to category.defaultCategoryName.trim(),
                "isSystem" to false,
                "createdAt" to now,
                "updatedAt" to now,
                "usageCount" to category.usageCount.coerceAtLeast(0)
            )

            category.defaultExpenseName.trim().takeIf { it.isNotBlank() }?.let {
                categoryData["defaultExpenseName"] = it
            }
            category.defaultPaymentMethod.trim().takeIf { it.isNotBlank() }?.let {
                categoryData["defaultPaymentMethod"] = it
            }
            category.defaultIsCommitted?.let { categoryData["defaultIsCommitted"] = it }
            category.defaultIsDiscretionary?.let { categoryData["defaultIsDiscretionary"] = it }
            category.defaultIsRepeating?.let { categoryData["defaultIsRepeating"] = it }
            category.defaultFrequency?.let { categoryData["defaultFrequency"] = it.name }
            category.defaultRepeatUntil?.let { categoryData["defaultRepeatUntil"] = it }

            categoryRef.set(categoryData).await()
            category.copy(
                categoryId = categoryRef.id,
                userId = userId,
                name = category.displayName.trim(),
                isSystem = false
            )
        }.mapQuickCategoryError()
    }

    private fun <T> Result<T>.mapQuickCategoryError(): Result<T> {
        return recoverCatching { throwable ->
            val message = if (
                throwable is FirebaseFirestoreException &&
                throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                "Permission denied while saving quick category. Publish the latest firestore.rules file in Firebase Console."
            } else {
                throwable.localizedMessage ?: "Unable to save quick category. Please try again."
            }
            throw IllegalStateException(message, throwable)
        }
    }

    override suspend fun deleteCategory(
        userId: String,
        categoryId: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .document(categoryId)
                .delete()
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun updateCategoryUsage(
        userId: String,
        categoryId: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .document(categoryId)
                .update(
                    mapOf(
                        "lastUsedAt" to FieldValue.serverTimestamp(),
                        "usageCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    private fun DocumentSnapshot.toQuickCategory(): QuickCategory? {
        val type = enumValueOrNull<TransactionType>(getString("type")) ?: TransactionType.EXPENSE
        val frequency = enumValueOrNull<RecurringFrequency>(getString("defaultFrequency"))

        return QuickCategory(
            categoryId = getString("categoryId") ?: id,
            userId = getString("userId") ?: reference.parent.parent?.id.orEmpty(),
            name = getString("name").orEmpty(),
            type = type,
            defaultExpenseName = getString("defaultExpenseName").orEmpty(),
            defaultCategoryName = getString("defaultCategoryName").orEmpty(),
            defaultPaymentMethod = getString("defaultPaymentMethod").orEmpty(),
            defaultIsCommitted = getBoolean("defaultIsCommitted"),
            defaultIsDiscretionary = getBoolean("defaultIsDiscretionary"),
            defaultIsRepeating = getBoolean("defaultIsRepeating"),
            defaultFrequency = frequency,
            defaultRepeatUntil = getTimestamp("defaultRepeatUntil"),
            isSystem = getBoolean("isSystem") ?: false,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt"),
            lastUsedAt = getTimestamp("lastUsedAt"),
            usageCount = (getLong("usageCount") ?: 0L).toInt()
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
    return value?.let { rawValue ->
        runCatching { enumValueOf<T>(rawValue) }.getOrNull()
    }
}
