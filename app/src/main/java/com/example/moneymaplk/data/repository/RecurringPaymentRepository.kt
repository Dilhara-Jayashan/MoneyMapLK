package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.RecurringFrequency
import com.example.moneymaplk.domain.model.RecurringPayment
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

interface RecurringPaymentRepository {
    val currentUserId: String?

    suspend fun saveRecurringPayment(payment: RecurringPayment): Result<Unit>
    suspend fun createRecurringPayment(payment: RecurringPayment): Result<String>
    suspend fun updateRecurringPayment(payment: RecurringPayment): Result<Unit>
    suspend fun pauseRecurringPayment(payment: RecurringPayment): Result<Unit>
    suspend fun resumeRecurringPayment(payment: RecurringPayment): Result<Unit>
    suspend fun deleteRecurringPayment(userId: String, paymentId: String): Result<Unit>
    suspend fun markOccurrenceConfirmed(payment: RecurringPayment, nextDueDate: Timestamp?): Result<Unit>
    suspend fun markOccurrenceSkipped(payment: RecurringPayment, nextDueDate: Timestamp?): Result<Unit>
    suspend fun loadRecurringPayments(userId: String): Result<List<RecurringPayment>>
    fun observeRecurringPayments(userId: String): Flow<Result<List<RecurringPayment>>>
}

class FirebaseRecurringPaymentRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RecurringPaymentRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeRecurringPayments(userId: String): Flow<Result<List<RecurringPayment>>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("recurringPayments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load recurring payments.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                val payments = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { document -> document.toRecurringPayment() }

                trySend(Result.success(payments))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun saveRecurringPayment(payment: RecurringPayment): Result<Unit> {
        return createRecurringPayment(payment).map { Unit }
    }

    override suspend fun createRecurringPayment(payment: RecurringPayment): Result<String> {
        return runCatching {
            val paymentRef = firestore.collection("users")
                .document(payment.userId)
                .collection("recurringPayments")
                .document()

            val paymentData = payment.toFirestoreData(
                paymentId = paymentRef.id,
                createdAt = FieldValue.serverTimestamp()
            )

            paymentRef.set(paymentData).await()
            paymentRef.id
        }.mapFirestoreError()
    }

    override suspend fun updateRecurringPayment(payment: RecurringPayment): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(payment.userId)
                .collection("recurringPayments")
                .document(payment.paymentId)
                .set(
                    payment.toFirestoreData(
                        paymentId = payment.paymentId,
                        createdAt = FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun pauseRecurringPayment(payment: RecurringPayment): Result<Unit> {
        return runCatching {
            val updates = mutableMapOf<String, Any?>(
                "isActive" to false,
                "pausedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users")
                .document(payment.userId)
                .collection("recurringPayments")
                .document(payment.paymentId)
                .update(updates)
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun resumeRecurringPayment(payment: RecurringPayment): Result<Unit> {
        return runCatching {
            val updates = mutableMapOf<String, Any?>(
                "isActive" to true,
                "pausedAt" to FieldValue.delete(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users")
                .document(payment.userId)
                .collection("recurringPayments")
                .document(payment.paymentId)
                .update(updates)
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun deleteRecurringPayment(userId: String, paymentId: String): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("recurringPayments")
                .document(paymentId)
                .delete()
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun markOccurrenceConfirmed(
        payment: RecurringPayment,
        nextDueDate: Timestamp?
    ): Result<Unit> {
        return updateOccurrence(
            payment = payment,
            nextDueDate = nextDueDate,
            processedField = "lastConfirmedDueDate"
        )
    }

    override suspend fun markOccurrenceSkipped(
        payment: RecurringPayment,
        nextDueDate: Timestamp?
    ): Result<Unit> {
        return updateOccurrence(
            payment = payment,
            nextDueDate = nextDueDate,
            processedField = "lastSkippedDueDate"
        )
    }

    override suspend fun loadRecurringPayments(userId: String): Result<List<RecurringPayment>> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("recurringPayments")
                .get()
                .await()
                .documents
                .mapNotNull { document -> document.toRecurringPayment() }
        }.mapFirestoreError()
    }

    private suspend fun updateOccurrence(
        payment: RecurringPayment,
        nextDueDate: Timestamp?,
        processedField: String
    ): Result<Unit> {
        return runCatching {
            val updates = mutableMapOf<String, Any?>(
                processedField to payment.nextDueDate,
                "lastPaidDate" to payment.nextDueDate,
                "isActive" to (nextDueDate != null),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nextDueDate == null) {
            } else {
                updates["nextDueDate"] = nextDueDate
            }

            firestore.collection("users")
                .document(payment.userId)
                .collection("recurringPayments")
                .document(payment.paymentId)
                .update(updates)
                .await()
            Unit
        }.mapFirestoreError()
    }

    private fun RecurringPayment.toFirestoreData(
        paymentId: String,
        createdAt: Any
    ): Map<String, Any?> {
        val paymentData = mutableMapOf<String, Any?>(
                "paymentId" to paymentId,
                "userId" to userId,
                "type" to type.name,
                "title" to title,
                "category" to category,
                "originalAmount" to originalAmount,
                "originalCurrency" to originalCurrency,
                "exchangeRateToLkr" to exchangeRateToLkr,
                "convertedAmountLkr" to convertedAmountLkr,
                "frequency" to frequency.name,
                "nextDueDate" to nextDueDate,
                "repeatEndDate" to repeatEndDate,
                "lastPaidDate" to lastPaidDate,
                "paymentMethod" to paymentMethod,
                "isActive" to isActive,
                "isCommitted" to isCommitted,
                "isDiscretionary" to isDiscretionary,
                "isRecurring" to isRecurring,
                "autoCreateTransaction" to autoCreateTransaction,
                "note" to note,
                "createdAt" to createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        incomeSource?.let { paymentData["incomeSource"] = it.name }
        paymentData["autoConfirm"] = autoConfirm
        repeatEndDate?.let { paymentData["repeatEndDate"] = it }
        lastPaidDate?.let { paymentData["lastPaidDate"] = it }
        lastConfirmedDueDate?.let { paymentData["lastConfirmedDueDate"] = it }
        lastSkippedDueDate?.let { paymentData["lastSkippedDueDate"] = it }
        pausedAt?.let { paymentData["pausedAt"] = it }
        return paymentData
    }

    private fun DocumentSnapshot.toRecurringPayment(): RecurringPayment? {
        val type = enumValueOrNull<TransactionType>(getString("type")) ?: TransactionType.EXPENSE
        val currency = getString("originalCurrency") ?: "LKR"
        val frequency = enumValueOrNull<RecurringFrequency>(getString("frequency")) ?: return null
        val nextDueDate = getTimestamp("nextDueDate") ?: Timestamp.now()
        val originalAmount = getDouble("originalAmount") ?: 0.0

        return RecurringPayment(
            paymentId = getString("paymentId") ?: id,
            userId = getString("userId") ?: reference.parent.parent?.id.orEmpty(),
            type = type,
            title = getString("title").orEmpty(),
            category = getString("category").orEmpty(),
            incomeSource = enumValueOrNull<IncomeSource>(getString("incomeSource")),
            originalAmount = originalAmount,
            originalCurrency = currency,
            exchangeRateToLkr = getDouble("exchangeRateToLkr") ?: if (currency == "LKR") 1.0 else 0.0,
            convertedAmountLkr = getDouble("convertedAmountLkr") ?: originalAmount,
            frequency = frequency,
            nextDueDate = nextDueDate,
            repeatEndDate = getTimestamp("repeatEndDate"),
            lastPaidDate = getTimestamp("lastPaidDate"),
            paymentMethod = getString("paymentMethod").orEmpty(),
            isActive = getBoolean("isActive") ?: true,
            isCommitted = getBoolean("isCommitted") ?: false,
            isDiscretionary = getBoolean("isDiscretionary") ?: false,
            isRecurring = getBoolean("isRecurring") ?: true,
            autoCreateTransaction = getBoolean("autoCreateTransaction") ?: false,
            autoConfirm = getBoolean("autoConfirm") ?: getBoolean("autoCreateTransaction") ?: false,
            lastConfirmedDueDate = getTimestamp("lastConfirmedDueDate"),
            lastSkippedDueDate = getTimestamp("lastSkippedDueDate"),
            pausedAt = getTimestamp("pausedAt"),
            note = getString("note").orEmpty()
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
    return value?.let { rawValue ->
        runCatching { enumValueOf<T>(rawValue) }.getOrNull()
    }
}
