package com.example.moneymaplk.data.repository

import com.example.moneymaplk.domain.model.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface GoalRepository {
    val currentUserId: String?

    suspend fun saveGoal(goal: Goal): Result<Unit>
    suspend fun saveDefaultGoal(goal: Goal): Result<Unit>
    suspend fun updateGoalProgress(
        userId: String,
        goalId: String,
        savedAmountLkr: Double,
        remainingAmountLkr: Double,
        progressPercentage: Double
    ): Result<Unit>
    fun observeGoals(userId: String): Flow<Result<List<Goal>>>
}

class FirebaseGoalRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GoalRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeGoals(userId: String): Flow<Result<List<Goal>>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        Result.failure(
                            IllegalStateException(
                                error.localizedMessage ?: "Unable to load your goals.",
                                error
                            )
                        )
                    )
                    return@addSnapshotListener
                }

                val goals = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { document -> document.toGoal() }

                trySend(Result.success(goals))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun saveDefaultGoal(goal: Goal): Result<Unit> {
        return saveGoal(
            goal.copy(
                description = goal.description.ifBlank { "Savings goal for ${goal.name}" },
                priority = "HIGH"
            )
        )
    }

    override suspend fun saveGoal(goal: Goal): Result<Unit> {
        return runCatching {
            val goalData = mutableMapOf<String, Any?>(
                "goalId" to goal.goalId,
                "userId" to goal.userId,
                "name" to goal.name,
                "targetAmountLkr" to goal.targetAmountLkr,
                "savedAmountLkr" to goal.savedAmountLkr,
                "remainingAmountLkr" to goal.remainingAmountLkr,
                "progressPercentage" to goal.progressPercentage,
                "targetDate" to (goal.targetDate ?: parseGoalDeadline(goal.deadline)),
                "isActive" to goal.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (goal.description.isNotBlank()) {
                goalData["description"] = goal.description.trim()
            }
            if (goal.deadline.isNotBlank()) {
                goalData["deadline"] = goal.deadline.trim()
            }
            if (goal.priority.isNotBlank()) {
                goalData["priority"] = goal.priority.trim()
            }
            firestore.collection("users")
                .document(goal.userId)
                .collection("goals")
                .document(goal.goalId)
                .set(
                    goalData + mapOf("createdAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    override suspend fun updateGoalProgress(
        userId: String,
        goalId: String,
        savedAmountLkr: Double,
        remainingAmountLkr: Double,
        progressPercentage: Double
    ): Result<Unit> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("goals")
                .document(goalId)
                .update(
                    mapOf(
                        "savedAmountLkr" to savedAmountLkr,
                        "remainingAmountLkr" to remainingAmountLkr,
                        "progressPercentage" to progressPercentage,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.mapFirestoreError()
    }

    private fun DocumentSnapshot.toGoal(): Goal? {
        val targetAmountLkr = getDouble("targetAmountLkr") ?: return null

        return Goal(
            goalId = getString("goalId") ?: id,
            userId = getString("userId") ?: reference.parent.parent?.id.orEmpty(),
            name = getString("name").orEmpty().ifBlank { "Savings Goal" },
            description = getString("description").orEmpty(),
            targetAmountLkr = targetAmountLkr,
            savedAmountLkr = getDouble("savedAmountLkr") ?: 0.0,
            deadline = getString("deadline").orEmpty(),
            targetDate = getTimestamp("targetDate"),
            priority = getString("priority").orEmpty(),
            isActive = getBoolean("isActive") ?: true
        )
    }
}
