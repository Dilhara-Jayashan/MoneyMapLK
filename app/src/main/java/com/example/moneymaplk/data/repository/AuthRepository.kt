package com.example.moneymaplk.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

data class AuthenticatedUser(
    val uid: String,
    val displayName: String,
    val email: String
)

interface AuthRepository {
    val currentUserId: String?

    suspend fun login(email: String, password: String): Result<String>
    suspend fun register(fullName: String, email: String, password: String): Result<String>
    suspend fun loginWithGoogle(idToken: String): Result<AuthenticatedUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    fun logout()
}

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<String> {
        return runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user
                ?: throw IllegalStateException("Login succeeded but Firebase user is missing")
            user.uid
        }.mapFirebaseAuthError()
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthenticatedUser> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
                ?: throw IllegalStateException("Google sign-in succeeded but Firebase user is missing")
            AuthenticatedUser(
                uid = user.uid,
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty()
            )
        }.mapFirebaseAuthError()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return runCatching {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Unit
        }.mapFirebaseAuthError()
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): Result<String> {
        return runCatching {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()
            val user = authResult.user
                ?: throw IllegalStateException("Registration succeeded but Firebase user is missing")

            if (fullName.isNotBlank()) {
                val profileChangeRequest = userProfileChangeRequest {
                    displayName = fullName.trim()
                }
                user.updateProfile(profileChangeRequest).await()
            }
            user.uid
        }.mapFirebaseAuthError()
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}

private fun <T> Result<T>.mapFirebaseAuthError(): Result<T> {
    return recoverCatching { throwable ->
        throw IllegalStateException(throwable.toReadableAuthMessage(), throwable)
    }
}

private fun Throwable.toReadableAuthMessage(): String {
    return when (this) {
        is FirebaseAuthInvalidCredentialsException -> "Please check your email and password."
        is FirebaseAuthInvalidUserException -> "No account was found for this email."
        is FirebaseAuthUserCollisionException -> "An account already exists for this email."
        is FirebaseNetworkException -> "Network error. Please check your connection and try again."
        else -> localizedMessage ?: "Authentication failed. Please try again."
    }
}
