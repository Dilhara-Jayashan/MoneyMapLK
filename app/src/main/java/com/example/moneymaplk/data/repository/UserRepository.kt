package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserRepository {
    val currentUserId: String?

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun createBasicUserProfileIfMissing(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit>
    suspend fun updateSelectedGoal(userId: String, goalId: String): Result<Unit>
    suspend fun isSetupCompleted(userId: String): Boolean
    fun observeUserProfile(userId: String): Flow<Result<UserProfile?>>
    fun buildCurrentUserProfile(
        defaultCurrency: String,
        currentSavingsLkr: Double,
        monthlySalaryLkr: Double,
        plannedSavingsAllocationLkr: Double,
        safeToSpendBufferLkr: Double
    ): UserProfile?
}

class FirebaseUserRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeUserProfile(userId: String): Flow<Result<UserProfile?>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load your profile.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                trySend(Result.success(snapshot?.toUserProfile()))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return runCatching {
            val userData = mapOf(
                "uid" to profile.uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "city" to profile.city,
                "occupation" to profile.occupation,
                "defaultCurrency" to profile.defaultCurrency,
                "supportedCurrencies" to profile.supportedCurrencies,
                "currencyRatesToBase" to profile.currencyRatesToBase,
                "currentSavingsLkr" to profile.currentSavingsLkr,
                "monthlySalaryLkr" to profile.monthlySalaryLkr,
                "plannedSavingsAllocationLkr" to profile.plannedSavingsAllocationLkr,
                "safeToSpendBufferLkr" to profile.safeToSpendBufferLkr,
                "selectedGoalId" to profile.selectedGoalId,
                "setupCompleted" to profile.setupCompleted,
                "financialMonthStartDay" to profile.financialMonthStartDay,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users")
                .document(profile.uid)
                .set(
                    userData + mapOf("createdAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun createBasicUserProfileIfMissing(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection("users").document(userId)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                userRef.set(
                    mapOf(
                        "uid" to userId,
                        "displayName" to displayName.trim().ifBlank { "MoneyMap LK User" },
                        "email" to email.trim(),
                        "city" to "",
                        "occupation" to "",
                        "setupCompleted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
            Unit
        }.mapFirestoreError()
    }

    override suspend fun updateSelectedGoal(userId: String, goalId: String): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "selectedGoalId" to goalId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun isSetupCompleted(userId: String): Boolean {
        val snapshot = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        if (!snapshot.exists()) return false
        if (snapshot.getBoolean("setupCompleted") == true) return true

        val data = snapshot.data ?: return false
        return legacySetupFields.all { field -> data.containsKey(field) }
    }

    override fun buildCurrentUserProfile(
        defaultCurrency: String,
        currentSavingsLkr: Double,
        monthlySalaryLkr: Double,
        plannedSavingsAllocationLkr: Double,
        safeToSpendBufferLkr: Double
    ): UserProfile? {
        val user = firebaseAuth.currentUser ?: return null
        return UserProfile(
            uid = user.uid,
            displayName = user.displayName.orEmpty().ifBlank { "MoneyMap LK User" },
            email = user.email.orEmpty(),
            city = "",
            occupation = "",
            defaultCurrency = defaultCurrency,
            supportedCurrencies = listOf("LKR", "USD"),
            currencyRatesToBase = mapOf(defaultCurrency to 1.0, "LKR" to 1.0, "USD" to 310.0),
            currentSavingsLkr = currentSavingsLkr,
            monthlySalaryLkr = monthlySalaryLkr,
            plannedSavingsAllocationLkr = plannedSavingsAllocationLkr,
            safeToSpendBufferLkr = safeToSpendBufferLkr,
            selectedGoalId = null,
            setupCompleted = true
        )
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        val supportedCurrencies = (get("supportedCurrencies") as? List<*>)
            ?.mapNotNull { value -> value as? String }
            ?.ifEmpty { listOf("LKR", "USD") }
            ?: listOf("LKR", "USD")
        val currencyRatesToBase = (get("currencyRatesToBase") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val currency = key as? String ?: return@mapNotNull null
                val rate = (value as? Number)?.toDouble() ?: return@mapNotNull null
                currency to rate
            }
            ?.toMap()
            .orEmpty()

        return UserProfile(
            uid = getString("uid") ?: id,
            displayName = getString("displayName").orEmpty().ifBlank { "MoneyMap LK User" },
            email = getString("email").orEmpty(),
            city = getString("city").orEmpty(),
            occupation = getString("occupation").orEmpty(),
            defaultCurrency = getString("defaultCurrency") ?: "LKR",
            supportedCurrencies = supportedCurrencies,
            currencyRatesToBase = currencyRatesToBase.ifEmpty {
                supportedCurrencies.associateWith { currency ->
                    if (currency == (getString("defaultCurrency") ?: "LKR")) 1.0 else if (currency == "USD") 310.0 else 1.0
                }
            },
            currentSavingsLkr = getDouble("currentSavingsLkr") ?: 0.0,
            monthlySalaryLkr = getDouble("monthlySalaryLkr") ?: 0.0,
            plannedSavingsAllocationLkr = getDouble("plannedSavingsAllocationLkr") ?: 0.0,
            safeToSpendBufferLkr = getDouble("safeToSpendBufferLkr") ?: 0.0,
            selectedGoalId = getString("selectedGoalId"),
            setupCompleted = getBoolean("setupCompleted") ?: false,
            financialMonthStartDay = getLong("financialMonthStartDay")?.toInt() ?: 1
        )
    }

    private companion object {
        val legacySetupFields = listOf(
            "defaultCurrency",
            "currentSavingsLkr",
            "monthlySalaryLkr",
            "plannedSavingsAllocationLkr",
            "safeToSpendBufferLkr"
        )
    }
}

internal fun <T> Result<T>.mapFirestoreError(): Result<T> {
    return recoverCatching { throwable ->
        throw IllegalStateException(
            throwable.localizedMessage ?: "Unable to save data. Please try again.",
            throwable
        )
    }
}

internal fun parseGoalDeadline(deadline: String): Timestamp? {
    return runCatching {
        val parts = deadline.trim().split("-")
        if (parts.size == 3) {
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.clear()
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            Timestamp(calendar.time)
        } else {
            null
        }
    }.getOrNull()
}
