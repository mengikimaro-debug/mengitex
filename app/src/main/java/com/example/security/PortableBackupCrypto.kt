package com.example.security

import android.os.Build
import android.util.Base64
import com.example.data.DecryptedSmsMessage
import com.example.data.DecryptedTemplate
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.data.SavedContactEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class MengiTexBackupArchive(
    val formatVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80),
    val messages: List<DecryptedSmsMessage> = emptyList(),
    val templates: List<DecryptedTemplate> = emptyList(),
    val contacts: List<SavedContactEntity> = emptyList(),
    val securityLevel: Int? = null,
    val isDarkMode: Boolean? = null,
    val languageCode: String? = null
)

/**
 * Cross-device portable zero-knowledge AES-256-GCM + GZIP backup encoder/decoder.
 * Uses PBKDF2-HMAC-SHA256 (120,000 iterations) so backups remain fully encrypted both on disk,
 * in shared .mengitex files, and in Firebase Firestore Cloud Backups.
 */
object PortableBackupCrypto {

    private const val PREFIX_PASSWORD = "MTX1P:"
    private const val PREFIX_DEFAULT = "MTX1D:"
    private const val DEFAULT_PORTABLE_SEED = "MengiTex#AES256#PortableVaultKey#v1"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun isPasswordProtected(encryptedPayload: String): Boolean {
        return encryptedPayload.trim().startsWith(PREFIX_PASSWORD)
    }

    fun isValidMengiTexPayload(encryptedPayload: String): Boolean {
        val trimmed = encryptedPayload.trim()
        return trimmed.startsWith(PREFIX_PASSWORD) || trimmed.startsWith(PREFIX_DEFAULT)
    }

    fun encryptArchive(archive: MengiTexBackupArchive, passphrase: String?): String {
        val jsonBytes = serializeArchive(archive).toByteArray(StandardCharsets.UTF_8)
        val compressed = gzipCompress(jsonBytes)

        val useUserPass = !passphrase.isNullOrBlank()
        val secretPhrase = if (useUserPass) passphrase!!.trim() else DEFAULT_PORTABLE_SEED
        val prefix = if (useUserPass) PREFIX_PASSWORD else PREFIX_DEFAULT

        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val key = deriveAesKey(secretPhrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(compressed)

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "$prefix$saltB64:$ivB64:$cipherB64"
    }

    fun decryptArchive(encryptedPayload: String, passphrase: String?): Result<MengiTexBackupArchive> {
        return try {
            val trimmed = encryptedPayload.trim()
            val useUserPass: Boolean
            val body: String
            when {
                trimmed.startsWith(PREFIX_PASSWORD) -> {
                    useUserPass = true
                    body = trimmed.removePrefix(PREFIX_PASSWORD)
                }
                trimmed.startsWith(PREFIX_DEFAULT) -> {
                    useUserPass = false
                    body = trimmed.removePrefix(PREFIX_DEFAULT)
                }
                else -> {
                    return Result.failure(IllegalArgumentException("Invalid MengiTex backup format."))
                }
            }

            if (useUserPass && passphrase.isNullOrBlank()) {
                return Result.failure(SecurityException("PASSPHRASE_REQUIRED"))
            }

            val parts = body.split(":")
            if (parts.size != 3) {
                return Result.failure(IllegalArgumentException("Corrupted backup payload."))
            }

            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)

            val secretPhrase = if (useUserPass) passphrase!!.trim() else DEFAULT_PORTABLE_SEED
            val key = deriveAesKey(secretPhrase, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val compressed = cipher.doFinal(cipherBytes)
            val jsonString = String(gzipDecompress(compressed), StandardCharsets.UTF_8)
            Result.success(deserializeArchive(jsonString))
        } catch (e: javax.crypto.AEADBadTagException) {
            Result.failure(SecurityException("WRONG_PASSPHRASE"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun gzipCompress(input: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(input) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(input: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(input)
        return GZIPInputStream(bis).use { it.readBytes() }
    }

    private fun serializeArchive(archive: MengiTexBackupArchive): String {
        val root = JSONObject().apply {
            put("v", archive.formatVersion)
            put("createdAt", archive.createdAt)
            put("deviceModel", archive.deviceModel)
            archive.securityLevel?.let { put("securityLevel", it) }
            archive.isDarkMode?.let { put("isDarkMode", it) }
            archive.languageCode?.let { put("languageCode", it) }
        }

        val msgsArray = JSONArray()
        archive.messages.forEach { m ->
            msgsArray.put(
                JSONObject().apply {
                    put("c", m.contactName)
                    put("p", m.phoneNumber)
                    put("b", m.body)
                    put("d", m.direction)
                    put("s", m.status)
                    put("n", m.statusNote)
                    put("t", m.timestamp)
                    if (m.scheduledFor != null) put("sf", m.scheduledFor)
                    put("sl", m.simSlotLabel)
                    put("ss", m.simSubId)
                    put("pc", m.partCount)
                    put("uk", m.isUnknownSender)
                    put("si", m.isSilentIntercepted)
                    put("sb", m.isSecretBurnAfterRead)
                    put("rd", m.isRead)
                    put("dl", m.isDeleted)
                    if (m.deletedAt != null) put("da", m.deletedAt)
                }
            )
        }
        root.put("messages", msgsArray)

        val tmplsArray = JSONArray()
        archive.templates.forEach { t ->
            tmplsArray.put(
                JSONObject().apply {
                    put("t", t.title)
                    put("c", t.category)
                    put("b", t.body)
                    put("u", t.usageCount)
                    put("ua", t.updatedAt)
                    put("dl", t.isDeleted)
                    if (t.deletedAt != null) put("da", t.deletedAt)
                }
            )
        }
        root.put("templates", tmplsArray)

        val contactsArray = JSONArray()
        archive.contacts.forEach { c ->
            contactsArray.put(
                JSONObject().apply {
                    put("n", c.name)
                    put("p", c.phoneNumber)
                    put("f", c.isFavorite)
                    put("ca", c.createdAt)
                }
            )
        }
        root.put("contacts", contactsArray)
        return root.toString()
    }

    private fun deserializeArchive(jsonString: String): MengiTexBackupArchive {
        val root = JSONObject(jsonString)
        val msgs = mutableListOf<DecryptedSmsMessage>()
        val msgsArray = root.optJSONArray("messages")
        if (msgsArray != null) {
            for (i in 0 until msgsArray.length()) {
                val o = msgsArray.getJSONObject(i)
                msgs.add(
                    DecryptedSmsMessage(
                        id = 0L,
                        contactName = o.optString("c", ""),
                        phoneNumber = o.optString("p", ""),
                        body = o.optString("b", ""),
                        direction = o.optString("d", MessageDirection.OUTGOING),
                        status = o.optString("s", MessageStatus.SENT),
                        statusNote = o.optString("n", ""),
                        timestamp = o.optLong("t", System.currentTimeMillis()),
                        scheduledFor = if (o.has("sf")) o.optLong("sf") else null,
                        simSlotLabel = o.optString("sl", "SIM"),
                        simSubId = o.optInt("ss", -1),
                        partCount = o.optInt("pc", 1),
                        isUnknownSender = o.optBoolean("uk", false),
                        isSilentIntercepted = o.optBoolean("si", false),
                        isSecretBurnAfterRead = o.optBoolean("sb", false),
                        isRead = o.optBoolean("rd", true),
                        isDeleted = o.optBoolean("dl", false),
                        deletedAt = if (o.has("da")) o.optLong("da") else null
                    )
                )
            }
        }

        val tmpls = mutableListOf<DecryptedTemplate>()
        val tmplsArray = root.optJSONArray("templates")
        if (tmplsArray != null) {
            for (i in 0 until tmplsArray.length()) {
                val o = tmplsArray.getJSONObject(i)
                tmpls.add(
                    DecryptedTemplate(
                        id = 0L,
                        title = o.optString("t", ""),
                        category = o.optString("c", "Jumla"),
                        body = o.optString("b", ""),
                        usageCount = o.optInt("u", 0),
                        updatedAt = o.optLong("ua", System.currentTimeMillis()),
                        isDeleted = o.optBoolean("dl", false),
                        deletedAt = if (o.has("da")) o.optLong("da") else null
                    )
                )
            }
        }

        val contacts = mutableListOf<SavedContactEntity>()
        val contactsArray = root.optJSONArray("contacts")
        if (contactsArray != null) {
            for (i in 0 until contactsArray.length()) {
                val o = contactsArray.getJSONObject(i)
                contacts.add(
                    SavedContactEntity(
                        id = 0L,
                        name = o.optString("n", ""),
                        phoneNumber = o.optString("p", ""),
                        isFavorite = o.optBoolean("f", false),
                        createdAt = o.optLong("ca", System.currentTimeMillis())
                    )
                )
            }
        }

        return MengiTexBackupArchive(
            formatVersion = root.optInt("v", 1),
            createdAt = root.optLong("createdAt", System.currentTimeMillis()),
            deviceModel = root.optString("deviceModel", ""),
            messages = msgs,
            templates = tmpls,
            contacts = contacts,
            securityLevel = if (root.has("securityLevel")) root.optInt("securityLevel") else null,
            isDarkMode = if (root.has("isDarkMode")) root.optBoolean("isDarkMode") else null,
            languageCode = if (root.has("languageCode")) root.optString("languageCode") else null
        )
    }
}
