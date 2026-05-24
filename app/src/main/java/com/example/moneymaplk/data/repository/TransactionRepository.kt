package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface TransactionRepository {
    val currentUserId: String?

    suspend fun saveTransaction(transaction: Transaction): Result<Unit>
    suspend fun saveTransactionAndReturnId(transaction: Transaction): Result<String>
    fun observeTransactions(userId: String): Flow<Result<List<Transaction>>>
}

class FirebaseTransactionRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TransactionRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeTransactions(userId: String): Flow<Result<List<Transaction>>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load transactions.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { document -> document.toTransaction() }

                trySend(Result.success(transactions))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun saveTransaction(transaction: Transaction): Result<Unit> {
        return saveTransactionAndReturnId(transaction).map { Unit }
    }

    override suspend fun saveTransactionAndReturnId(transaction: Transaction): Result<String> {
        return runCatching {
            val transactionRef = firestore.collection("users")
                .document(transaction.userId)
                .collection("transactions")
                .document()

            val transactionData = mutableMapOf<String, Any>(
                "transactionId" to transactionRef.id,
                "userId" to transaction.userId,
                "type" to transaction.type.name,
                "title" to transaction.title,
                "category" to transaction.category,
                "originalAmount" to transaction.originalAmount,
                "originalCurrency" to transaction.originalCurrency,
                "exchangeRateToLkr" to transaction.exchangeRateToLkr,
                "convertedAmountLkr" to transaction.convertedAmountLkr,
                "transactionDate" to transaction.transactionDate,
                "monthId" to transaction.monthId,
                "paymentMethod" to transaction.paymentMethod,
                "note" to transaction.note,
                "isCommitted" to transaction.isCommitted,
                "isDiscretionary" to transaction.isDiscretionary,
                "isRecurring" to transaction.isRecurring,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (transaction.type == TransactionType.INCOME) {
                transaction.incomeSource?.let { transactionData["incomeSource"] = it.name }
            }
            transaction.recurringPaymentId?.let { transactionData["recurringPaymentId"] = it }
            transaction.followUpId?.let { transactionData["followUpId"] = it }

            transactionRef.set(transactionData).await()
            transactionRef.id
        }.mapFirestoreError()
    }

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        val type = enumValueOrNull<TransactionType>(getString("type")) ?: return null
        val currency = getString("originalCurrency") ?: "LKR"
        val transactionDate = getTimestamp("transactionDate") ?: Timestamp.now()
        val originalAmount = getDouble("originalAmount") ?: 0.0

        return Transaction(
            transactionId = getString("transactionId") ?: id,
            userId = getString("userId") ?: reference.parent.parent?.id.orEmpty(),
            type = type,
            title = getString("title").orEmpty(),
            category = getString("category").orEmpty(),
            incomeSource = enumValueOrNull<IncomeSource>(getString("incomeSource")),
            originalAmount = originalAmount,
            originalCurrency = currency,
            exchangeRateToLkr = getDouble("exchangeRateToLkr") ?: if (currency == "LKR") 1.0 else 0.0,
            convertedAmountLkr = getDouble("convertedAmountLkr") ?: originalAmount,
            transactionDate = transactionDate,
            monthId = getString("monthId").orEmpty(),
            paymentMethod = getString("paymentMethod").orEmpty(),
            note = getString("note").orEmpty(),
            isCommitted = getBoolean("isCommitted") ?: false,
            isDiscretionary = getBoolean("isDiscretionary") ?: false,
            isRecurring = getBoolean("isRecurring") ?: false,
            recurringPaymentId = getString("recurringPaymentId"),
            followUpId = getString("followUpId")
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
    return value?.let { rawValue ->
        runCatching { enumValueOf<T>(rawValue) }.getOrNull()
    }
}
