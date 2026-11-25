package com.example.cloud

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Keep
@IgnoreExtraProperties
data class CloudBackupRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val deviceModel: String = "",
    val encryptedPayload: String = "",
    val messageCount: Long = 0L,
    val templateCount: Long = 0L,
    val contactCount: Long = 0L,
    val passwordProtected: Boolean = true,
    val formatVersion: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Firestore repository for zero-knowledge AES-256 encrypted MengiTex Cloud Backups.
 * Strictly binds to the provisioned named Enterprise Firestore database ID.
 */
class CloudBackupRepository(private val db: FirebaseFirestore) {

    companion object {
        fun ensureFirebaseInitialized(context: Context): Boolean {
            return runCatching {
                val appCtx = context.applicationContext ?: context
                if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                    true
                } else {
                    FirebaseApp.initializeApp(appCtx) != null
                }
            }.getOrDefault(false)
        }

        fun createOrNull(context: Context): CloudBackupRepository? {
            if (!ensureFirebaseInitialized(context)) return null
            return runCatching {
                val dbId = context.getString(R.string.firestore_database_id)
                CloudBackupRepository(FirebaseFirestore.getInstance(dbId))
            }.getOrNull()
        }
    }

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")

    private fun backupsCollection(userId: String) =
        db.collection("users").document(userId).collection("cloud_backups")

    fun observeCloudBackups(userId: String): Flow<List<CloudBackupRecord>> {
        val path = "users/$userId/cloud_backups"
        return backupsCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudBackupRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { cause ->
                val firestoreError = (cause as? FirebaseFirestoreException)
                    ?: (cause.cause as? FirebaseFirestoreException)
                if (cause is CancellationException && firestoreError == null) {
                    throw cause
                }
                val err = firestoreError ?: (cause as? Exception) ?: Exception(cause)
                handleFirestoreError(err, OperationType.LIST, path)
                throw err
            }
    }

    suspend fun uploadCloudBackup(
        label: String,
        encryptedPayload: String,
        messageCount: Int,
        templateCount: Int,
        contactCount: Int,
        passwordProtected: Boolean
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val backupId = "bk_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$uid/cloud_backups/$backupId"

        val safeLabel = label.trim().ifEmpty { "MengiTex Cloud Backup" }.take(100)
        val safeDeviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { "Android Device" }.take(80)

        if (encryptedPayload.isEmpty() || encryptedPayload.length > 850_000) {
            return Result.failure(
                IllegalArgumentException("Encrypted cloud backup exceeds 850KB single-snapshot limit.")
            )
        }

        return try {
            val payloadMap = hashMapOf<String, Any>(
                "userId" to uid,
                "label" to safeLabel,
                "deviceModel" to safeDeviceModel,
                "encryptedPayload" to encryptedPayload,
                "messageCount" to messageCount.coerceIn(0, 600_000),
                "templateCount" to templateCount.coerceIn(0, 10_000),
                "contactCount" to contactCount.coerceIn(0, 50_000),
                "passwordProtected" to passwordProtected,
                "formatVersion" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            backupsCollection(uid).document(backupId).set(payloadMap).await()
            Result.success(backupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun renameCloudBackup(backupId: String, newLabel: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val safeLabel = newLabel.trim().take(100)
        if (safeLabel.isEmpty()) {
            return Result.failure(IllegalArgumentException("Backup label cannot be empty."))
        }
        val path = "users/$uid/cloud_backups/$backupId"
        return try {
            backupsCollection(uid).document(backupId).update(
                mapOf(
                    "label" to safeLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(backupId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/cloud_backups/$backupId"
        return try {
            backupsCollection(uid).document(backupId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
