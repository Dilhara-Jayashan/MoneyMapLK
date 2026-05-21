package com.example.moneymaplk.data.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.moneymaplk.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val throwable: Throwable) : GoogleSignInResult
}

class GoogleSignInClient {
    suspend fun signIn(context: Context): GoogleSignInResult {
        val credentialManager = CredentialManager.create(context)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            context.getString(R.string.default_web_client_id)
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val credential = credentialManager.getCredential(
                context = context,
                request = request
            ).credential

            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleCredential.idToken)
            } else {
                GoogleSignInResult.Failure(IllegalStateException("Unexpected Google credential."))
            }
        } catch (throwable: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (throwable: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(throwable)
        } catch (throwable: GetCredentialException) {
            GoogleSignInResult.Failure(throwable)
        } catch (throwable: Exception) {
            GoogleSignInResult.Failure(throwable)
        }
    }

    suspend fun clearCredentialState(context: Context) {
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
