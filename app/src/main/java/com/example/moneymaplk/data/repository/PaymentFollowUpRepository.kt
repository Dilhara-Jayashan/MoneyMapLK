package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
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

interface PaymentFollowUpRepository {
    val currentUserId: String?

    suspend fun savePaymentFollowUp(followUp: PaymentFollowUp): Result<String>
    suspend fun updatePaymentFollowUp(followUp: PaymentFollowUp): Result<Unit>
    suspend fun markPaymentFollowUpConfirmed(
        userId: String,
        followUpId: String,
        linkedTransactionId: String?
    ): Result<Unit>
    suspend fun markPaymentFollowUpSkipped(userId: String, followUpId: String): Result<Unit>
    suspend fun markPaymentFollowUpOverdue(userId: String, followUpId: String): Result<Unit>
    suspend fun deletePaymentFollowUp(userId: String, followUpId: String): Result<Unit>
    suspend fun loadPaymentFollowUps(userId: String): Result<List<PaymentFollowUp>>
    fun observePaymentFollowUps(userId: String): Flow<Result<List<PaymentFollowUp>>>
}

class FirebasePaymentFollowUpRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PaymentFollowUpRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observePaymentFollowUps(userId: String): Flow<Result<List<PaymentFollowUp>>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("paymentFollowUps")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load payment follow-ups.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                val paymentFollowUps = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { document -> document.toPaymentFollowUp() }

                trySend(Result.success(paymentFollowUps))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun savePaymentFollowUp(followUp: PaymentFollowUp): Result<String> {
        return runCatching {
            val followUpRef = firestore.collection("users")
                .document(followUp.userId)
                .collection("paymentFollowUps")
                .document()

            val followUpData = mutableMapOf<String, Any?>(
                "followUpId" to followUpRef.id,
                "userId" to followUp.userId,
                "type" to followUp.type.name,
                "title" to followUp.title,
                "category" to followUp.category,
                "status" to followUp.status.name,
                "originalAmount" to followUp.originalAmount,
                "originalCurrency" to followUp.originalCurrency,
                "exchangeRateToLkr" to followUp.exchangeRateToLkr,
                "convertedAmountLkr" to followUp.convertedAmountLkr,
                "expectedDate" to followUp.expectedDate,
                "paymentMethod" to followUp.paymentMethod,
                "isCommitted" to followUp.isCommitted,
                "isDiscretionary" to followUp.isDiscretionary,
                "isRecurring" to followUp.isRecurring,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            followUp.incomeSource?.let { followUpData["incomeSource"] = it.name }
            if (followUp.referenceNumber.isNotBlank()) {
                followUpData["referenceNumber"] = followUp.referenceNumber
            }
            followUp.confirmedDate?.let { followUpData["confirmedDate"] = it }
            followUp.skippedDate?.let { followUpData["skippedDate"] = it }
            followUp.followUpDate?.let { followUpData["followUpDate"] = it }
            if (followUp.followUpNote.isNotBlank()) {
                followUpData["followUpNote"] = followUp.followUpNote
            }
            followUp.recurringPaymentId?.let { followUpData["recurringPaymentId"] = it }
            followUp.linkedTransactionId?.let { followUpData["linkedTransactionId"] = it }

            followUpRef.set(followUpData).await()
            followUpRef.id
        }.mapFirestoreError()
    }

    override suspend fun updatePaymentFollowUp(followUp: PaymentFollowUp): Result<Unit> {
        return runCatching {
            require(followUp.followUpId.isNotBlank()) { "Follow-up id is required for update." }
            val followUpRef = firestore.collection("users")
                .document(followUp.userId)
                .collection("paymentFollowUps")
                .document(followUp.followUpId)

            val followUpData = mutableMapOf<String, Any?>(
                "type" to followUp.type.name,
                "title" to followUp.title,
                "category" to followUp.category,
                "originalAmount" to followUp.originalAmount,
                "originalCurrency" to followUp.originalCurrency,
                "exchangeRateToLkr" to followUp.exchangeRateToLkr,
                "convertedAmountLkr" to followUp.convertedAmountLkr,
                "expectedDate" to followUp.expectedDate,
                "paymentMethod" to followUp.paymentMethod,
                "followUpDate" to followUp.followUpDate,
                "followUpNote" to followUp.followUpNote,
                "incomeSource" to followUp.incomeSource?.name,
                "isCommitted" to followUp.isCommitted,
                "isDiscretionary" to followUp.isDiscretionary,
                "isRecurring" to followUp.isRecurring,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (followUp.referenceNumber.isNotBlank()) {
                followUpData["referenceNumber"] = followUp.referenceNumber
            } else {
                followUpData["referenceNumber"] = ""
            }
            followUpRef.update(followUpData).await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun markPaymentFollowUpConfirmed(
        userId: String,
        followUpId: String,
        linkedTransactionId: String?
    ): Result<Unit> {
        return runCatching {
            val updates = mutableMapOf<String, Any?>(
                "status" to PaymentFollowUpStatus.CONFIRMED.name,
                "confirmedDate" to Timestamp.now(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            linkedTransactionId?.let { updates["linkedTransactionId"] = it }
            firestore.collection("users")
                .document(userId)
                .collection("paymentFollowUps")
                .document(followUpId)
                .update(updates)
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun markPaymentFollowUpSkipped(userId: String, followUpId: String): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("paymentFollowUps")
                .document(followUpId)
                .update(
                    mapOf(
                        "status" to PaymentFollowUpStatus.SKIPPED.name,
                        "skippedDate" to Timestamp.now(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun markPaymentFollowUpOverdue(userId: String, followUpId: String): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("paymentFollowUps")
                .document(followUpId)
                .update(
                    mapOf(
                        "status" to PaymentFollowUpStatus.OVERDUE.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun deletePaymentFollowUp(userId: String, followUpId: String): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("paymentFollowUps")
                .document(followUpId)
                .delete()
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun loadPaymentFollowUps(userId: String): Result<List<PaymentFollowUp>> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("paymentFollowUps")
                .get()
                .await()
                .documents
                .mapNotNull { document -> document.toPaymentFollowUp() }
        }.mapFirestoreError()
    }

    private fun DocumentSnapshot.toPaymentFollowUp(): PaymentFollowUp? {
        val currency = getString("originalCurrency") ?: "LKR"
        val status = enumValueOrNull<PaymentFollowUpStatus>(getString("status")) ?: PaymentFollowUpStatus.EXPECTED
        val expectedDate = getTimestamp("expectedDate") ?: getTimestamp("dueDate") ?: Timestamp.now()
        val originalAmount = getDouble("originalAmount") ?: 0.0
        val type = enumValueOrNull<TransactionType>(getString("type")) ?: TransactionType.INCOME

        return PaymentFollowUp(
            followUpId = getString("followUpId") ?: id,
            userId = getString("userId") ?: reference.parent.parent?.id.orEmpty(),
            type = type,
            title = getString("title") ?: getString("projectName").orEmpty(),
            category = getString("category") ?: getString("projectName").orEmpty(),
            incomeSource = enumValueOrNull<IncomeSource>(getString("incomeSource")),
            referenceNumber = getString("referenceNumber").orEmpty(),
            status = status,
            originalAmount = originalAmount,
            originalCurrency = currency,
            exchangeRateToLkr = getDouble("exchangeRateToLkr") ?: if (currency == "LKR") 1.0 else 0.0,
            convertedAmountLkr = getDouble("convertedAmountLkr") ?: originalAmount,
            expectedDate = expectedDate,
            confirmedDate = getTimestamp("confirmedDate") ?: getTimestamp("paidDate"),
            skippedDate = getTimestamp("skippedDate"),
            followUpDate = getTimestamp("followUpDate"),
            paymentMethod = getString("paymentMethod").orEmpty(),
            followUpNote = getString("followUpNote").orEmpty(),
            isCommitted = getBoolean("isCommitted") ?: false,
            isDiscretionary = getBoolean("isDiscretionary") ?: false,
            isRecurring = getBoolean("isRecurring") ?: false,
            recurringPaymentId = getString("recurringPaymentId"),
            linkedTransactionId = getString("linkedTransactionId"),
            updatedAt = getTimestamp("updatedAt")
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
    return value?.let { rawValue ->
        runCatching { enumValueOf<T>(rawValue) }.getOrNull()
    }
}
