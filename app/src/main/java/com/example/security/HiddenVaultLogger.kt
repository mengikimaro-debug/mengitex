package com.example.security

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class VaultLogEntry(
    val id: String,
    val timestamp: Long,
    val category: String,
    val title: String,
    val detail: String,
    val fileName: String
)

data class EncryptedVaultFile(
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isLogFile: Boolean
)

data class VaultFolderStats(
    val hiddenPathDisplay: String,
    val totalEncryptedFiles: Int,
    val totalSizeBytes: Long,
    val isHardwareBacked: Boolean
)

/**
 * Persists encrypted activity logs and user vault files inside a hidden app-private directory
 * (`.siri_vault_hidden/.logs` & `.siri_vault_hidden/.files`) shielded by `.nomedia` and AES-256-GCM.
 */
class HiddenVaultLogger(
    private val context: Context,
    private val crypto: VaultCryptoManager
) {
    companion object {
        private val asyncLogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private val pendingWriteJobs = ConcurrentHashMap.newKeySet<Job>()

    private val rootHiddenDir: File by lazy {
        File(context.filesDir, ".siri_vault_hidden").apply {
            if (!exists()) mkdirs()
            val nomedia = File(this, ".nomedia")
            if (!nomedia.exists()) runCatching { nomedia.createNewFile() }
        }
    }

    private val logsDir: File by lazy {
        File(rootHiddenDir, ".logs").apply {
            if (!exists()) mkdirs()
        }
    }

    private val vaultFilesDir: File by lazy {
        File(rootHiddenDir, ".files").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun resolveSafeChild(baseDir: File, rawFileName: String): File {
        val safeName = rawFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        val candidate = File(baseDir, safeName)
        val baseCanonical = baseDir.canonicalPath
        val childCanonical = candidate.canonicalPath
        require(childCanonical.startsWith(baseCanonical + File.separator)) {
            "Invalid file path outside hidden vault"
        }
        return candidate
    }

    /**
     * Non-blocking 0ms fire-and-forget audit logger so disk encryption never adds latency
     * to SMS sending, incoming reception, or delivery status ticks.
     */
    fun appendLog(category: String, title: String, detail: String) {
        val now = System.currentTimeMillis()
        val job = asyncLogScope.launch {
            try {
                val id = UUID.randomUUID().toString().take(8)
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
                val fileName = "log_${stamp}_$id.enc"
                val payload = "$id|$now|${category.replace("|", "/")}|${title.replace("|", "/")}|${detail.replace("|", "/")}"
                val encrypted = crypto.encryptBytes(payload.toByteArray(Charsets.UTF_8))
                val target = resolveSafeChild(logsDir, fileName)
                target.writeBytes(encrypted)
            } catch (_: Exception) {
                // Avoid crashing background receivers if storage is constrained
            } finally {
                coroutineContext[Job]?.let { pendingWriteJobs.remove(it) }
            }
        }
        if (!job.isCompleted) {
            pendingWriteJobs.add(job)
        }
    }

    fun readAllLogs(maxEntries: Int = 150): List<VaultLogEntry> {
        if (pendingWriteJobs.isNotEmpty()) {
            runBlocking {
                withTimeoutOrNull(3_000L) {
                    pendingWriteJobs.toList().forEach { it.join() }
                }
            }
        }
        val allFiles = logsDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }
            ?.sortedByDescending { it.lastModified() }
            ?: return emptyList()

        // Keep hidden log directory fast by trimming very old log files past 250 entries
        if (allFiles.size > 250) {
            allFiles.drop(250).forEach { old -> runCatching { old.delete() } }
        }

        return allFiles.take(maxEntries).mapNotNull { file ->
            try {
                val plain = String(crypto.decryptBytes(file.readBytes()), Charsets.UTF_8)
                val parts = plain.split("|", limit = 5)
                if (parts.size >= 5) {
                    VaultLogEntry(
                        id = parts[0],
                        timestamp = parts[1].toLongOrNull() ?: file.lastModified(),
                        category = parts[2],
                        title = parts[3],
                        detail = parts[4],
                        fileName = file.name
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun saveEncryptedVaultFile(title: String, plainContent: String): EncryptedVaultFile {
        val safeBase = title.trim().ifEmpty { "faili_siri" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(40)
        val fileName = "${safeBase}_${System.currentTimeMillis() % 10000}.vault.enc"
        val target = resolveSafeChild(vaultFilesDir, fileName)
        val encrypted = crypto.encryptBytes(plainContent.toByteArray(Charsets.UTF_8))
        target.writeBytes(encrypted)
        appendLog(
            category = "ENCRYPTION",
            title = "Faili limesimbwa AES-256",
            detail = "Faili '$fileName' (${encrypted.size} B) limehifadhiwa kwenye folda iliyofichwa."
        )
        return EncryptedVaultFile(
            name = target.name,
            sizeBytes = target.length(),
            lastModified = target.lastModified(),
            isLogFile = false
        )
    }

    fun saveEncryptedBinaryFile(displayName: String, rawBytes: ByteArray): EncryptedVaultFile {
        val safeBase = displayName.trim().ifEmpty { "imported_file" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(44)
        val fileName = "${safeBase}_${System.currentTimeMillis() % 10000}.bin.enc"
        val target = resolveSafeChild(vaultFilesDir, fileName)
        val encrypted = crypto.encryptBytes(rawBytes)
        target.writeBytes(encrypted)
        appendLog(
            category = "STORAGE_VAULT",
            title = "Faili la Storage limesimbwa AES-256",
            detail = "Faili '$fileName' (${encrypted.size} B) limehifadhiwa kwa usalama."
        )
        return EncryptedVaultFile(
            name = target.name,
            sizeBytes = target.length(),
            lastModified = target.lastModified(),
            isLogFile = false
        )
    }

    fun readEncryptedVaultFile(fileName: String): String {
        val target = resolveSafeChild(vaultFilesDir, fileName)
        if (!target.exists()) return ""
        val decrypted = crypto.decryptBytes(target.readBytes())
        if (fileName.endsWith(".bin.enc")) {
            return "Encrypted Binary File (${decrypted.size} bytes)\nSHA/AES-256-GCM Verified • Stored securely in hidden vault."
        }
        return String(decrypted, Charsets.UTF_8)
    }

    fun deleteEncryptedVaultFile(fileName: String): Boolean {
        val target = resolveSafeChild(vaultFilesDir, fileName)
        return target.exists() && target.delete()
    }

    fun listEncryptedVaultFiles(): List<EncryptedVaultFile> {
        val files = vaultFilesDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }
            ?.sortedByDescending { it.lastModified() }
            ?: return emptyList()
        return files.map {
            EncryptedVaultFile(
                name = it.name,
                sizeBytes = it.length(),
                lastModified = it.lastModified(),
                isLogFile = false
            )
        }
    }

    fun clearLogs() {
        logsDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }?.forEach { it.delete() }
        appendLog(
            category = "USALAMA",
            title = "Logi za zamani zimesafishwa",
            detail = "Folda iliyofichwa imesafishwa na kuanza upya kwa usalama."
        )
    }

    fun getFolderStats(): VaultFolderStats {
        val logFiles = logsDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }.orEmpty()
        val customFiles = vaultFilesDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }.orEmpty()
        val totalBytes = logFiles.sumOf { it.length() } + customFiles.sumOf { it.length() }
        return VaultFolderStats(
            hiddenPathDisplay = "/.siri_vault_hidden/.logs/ (Imefichwa + .nomedia)",
            totalEncryptedFiles = logFiles.size + customFiles.size,
            totalSizeBytes = totalBytes,
            isHardwareBacked = crypto.isHardwareKeystoreReady()
        )
    }
}
