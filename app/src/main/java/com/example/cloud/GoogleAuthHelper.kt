package com.example.cloud

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles Google Sign-In via Jetpack Credential Manager + Firebase Auth.
 * Strictly passes ONE credential option per GetCredentialRequest (API 36 compliant)
 * and resolves Web Client ID from R.string.default_web_client_id.
 */
object GoogleAuthHelper {

    private const val TAG = "Auth"

    suspend fun attemptAutoSignIn(activityContext: Context): FirebaseUser? {
        if (!CloudBackupRepository.ensureFirebaseInitialized(activityContext)) return null
        if (Firebase.auth.currentUser != null) return Firebase.auth.currentUser
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(activityContext.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                Firebase.auth.signInWithCredential(firebaseCred).await().user
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No previously authorized Google account for silent auto sign-in.")
            null
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Auto Google Sign-In canceled by user.", e)
            null
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        if (!CloudBackupRepository.ensureFirebaseInitialized(activityContext)) {
            return Result.failure(IllegalStateException("Cloud backup service is unavailable right now."))
        }
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = activityContext.getString(R.string.default_web_client_id)
            ).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Google Sign-In returned null Firebase user."))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In canceled by user.", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed: ${e.type} - ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed unexpectedly", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(activityContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear Credential Manager state", e)
            }
            if (CloudBackupRepository.ensureFirebaseInitialized(activityContext)) {
                runCatching { Firebase.auth.signOut() }
            }
        }
    }
}
