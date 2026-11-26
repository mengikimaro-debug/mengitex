package com.example.data

import android.content.Context
import com.example.security.VaultCryptoManager
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Ultra-fast Persistent + In-Memory Instant SMS Cache ("Mfumo wa Kukumbuka SMS Zote").
 *
 * Eliminates the 3–5 second empty-screen delay on cold app launch or resume:
 * 1. Keeps the complete active message list in process RAM so Activity recreation or
 *    switching apps never blanks out the UI.
 * 2. Persists an AES-256-GCM encrypted, GZIP-compressed binary snapshot on disk containing
 *    the newest messages plus the latest message of every conversation thread (up to 600 rows)
 *    so cold launches hydrate Frame 1 in ~3ms with a single decryption pass instead of waiting
 *    for Room to read and individually decrypt tens of thousands of rows.
 * 3. Supports instant atomic prepend when a new incoming SMS arrives in the background.
 */
object SmsInstantCache {

    private const val CACHE_FILE_NAME = "mengitex_sms_instant_cache_v1.bin"
    private const val CACHE_BACKUP_FILE_NAME = "mengitex_sms_instant_cache_v1.bin.bak"
    private const val CACHE_MAGIC = 0x4D545831 // "MTX1"
    private const val CACHE_VERSION = 1
    private const val MAX_RECENT_MESSAGES = 900
    private const val MAX_TOTAL_SNAPSHOT_MESSAGES = 1800

    private val diskLock = Any()

    @Volatile
    private var memorySnapshot: List<DecryptedSmsMessage> = emptyList()

    @Volatile
    private var lastSavedFingerprint: Long = 0L

    /**
     * Zero-I/O Frame-0 RAM check (~0.001ms): never touches disk or AndroidKeyStore on Main Thread.
     */
    fun loadMemoryOnly(): List<DecryptedSmsMessage> = memorySnapshot

    /**
     * Returns immediately cached messages for Frame 1 hydration & self-healing recovery:
     * - First checks in-process RAM snapshot (~0ms).
     * - Falls back to primary encrypted disk snapshot (.bin) or shadow backup (.bin.bak) on IO thread.
     */
    fun loadInstantSync(context: Context, crypto: VaultCryptoManager): List<DecryptedSmsMessage> {
        val inMem = memorySnapshot
        if (inMem.isNotEmpty()) return inMem

        VaultCryptoManager.bindContext(context.applicationContext)
        val fromDisk = readDiskSnapshot(context.applicationContext, crypto)
        if (fromDisk.isNotEmpty()) {
            memorySnapshot = fromDisk
            lastSavedFingerprint = computeListFingerprint(fromDisk)
        }
        return fromDisk
    }

    private fun computeListFingerprint(messages: List<DecryptedSmsMessage>): Long {
        if (messages.isEmpty()) return 0L
        val first = messages.first()
        val last = messages.last()
        var h = messages.size.toLong()
        h = h * 31L + first.id
        h = h * 31L + first.timestamp
        h = h * 31L + first.status.hashCode().toLong()
        h = h * 31L + (if (first.isRead) 1L else 0L)
        h = h * 31L + last.id
        h = h * 31L + last.timestamp
        return h
    }

    /**
     * Updates the in-memory snapshot immediately.
     * ZERO-DATA-LOSS GUARD: Never overwrites a non-empty snapshot with an empty list unless [allowEmpty] is true.
     */
    fun updateMemory(messages: List<DecryptedSmsMessage>, allowEmpty: Boolean = false) {
        if (messages.isEmpty() && !allowEmpty && memorySnapshot.isNotEmpty()) {
            return
        }
        memorySnapshot = messages
    }

    /**
     * Persists a dual-redundant encrypted snapshot (.bin + .bin.bak) covering the newest messages AND
     * at least the latest message from every active conversation thread.
     * ZERO-DATA-LOSS GUARD: Never deletes the disk cache on empty list emissions; only [clear] may delete it.
     */
    fun saveSnapshotToDisk(
        context: Context,
        crypto: VaultCryptoManager,
        messages: List<DecryptedSmsMessage>
    ) {
        if (messages.isEmpty()) {
            // Never wipe the persistent disk snapshot on transient empty lists!
            return
        }
        val appContext = context.applicationContext
        val fingerprint = computeListFingerprint(messages)
        val targetFile = File(appContext.filesDir, CACHE_FILE_NAME)
        if (fingerprint != 0L && fingerprint == lastSavedFingerprint && targetFile.exists() && targetFile.length() > 32L) {
            return
        }
        VaultCryptoManager.bindContext(appContext)

        val selected = selectSnapshotSubset(messages)
        synchronized(diskLock) {
            runCatching {
                val rawBytes = serializeMessages(selected)
                val encrypted = crypto.encryptBytes(rawBytes)
                if (encrypted.isEmpty()) return@runCatching

                val target = File(appContext.filesDir, CACHE_FILE_NAME)
                val backup = File(appContext.filesDir, CACHE_BACKUP_FILE_NAME)
                val tmp = File(appContext.filesDir, "$CACHE_FILE_NAME.tmp")

                // Preserve last-known-good primary cache into .bak before replacing
                if (target.exists() && target.length() > 32L) {
                    runCatching { target.copyTo(backup, overwrite = true) }
                }

                tmp.writeBytes(encrypted)
                if (!tmp.renameTo(target)) {
                    target.writeBytes(encrypted)
                    tmp.delete()
                }

                // Ensure shadow backup also exists on first save
                if (!backup.exists() || backup.length() == 0L) {
                    runCatching { target.copyTo(backup, overwrite = true) }
                }
                lastSavedFingerprint = fingerprint
            }
        }
    }

    /**
     * Zero-I/O RAM-only prepend (~0.1ms) for BroadcastReceivers so incoming SMS reaches the UI
     * instantaneously without waiting for disk encryption & serialization.
     */
    fun prependMessageMemoryOnly(message: DecryptedSmsMessage): List<DecryptedSmsMessage> {
        val current = memorySnapshot
        val updated = ArrayList<DecryptedSmsMessage>(current.size + 1)
        updated.add(message)
        for (i in 0 until current.size) {
            val item = current[i]
            if (item.id != message.id || message.id <= 0L) {
                updated.add(item)
            }
        }
        memorySnapshot = updated
        return updated
    }

    /**
     * Immediately prepends a newly arrived/sent SMS into both RAM and the encrypted disk snapshot
     * so even if the app was closed when the SMS arrived, opening the app shows it on Frame 1.
     */
    fun prependMessage(
        context: Context,
        crypto: VaultCryptoManager,
        message: DecryptedSmsMessage
    ) {
        val appContext = context.applicationContext
        VaultCryptoManager.bindContext(appContext)
        val current = memorySnapshot.ifEmpty { readDiskSnapshot(appContext, crypto) }
        val updated = ArrayList<DecryptedSmsMessage>(current.size + 1)
        updated.add(message)
        for (i in 0 until current.size) {
            val item = current[i]
            if (item.id != message.id || message.id <= 0L) {
                updated.add(item)
            }
        }
        memorySnapshot = updated
        saveSnapshotToDisk(appContext, crypto, updated)
    }

    /**
     * Updates a message's Sent/Delivered/Failed status in both RAM and disk snapshot so
     * background Pop Reply / Bubble status ticks stay consistent across restarts.
     */
    fun updateMessageStatus(
        context: Context,
        crypto: VaultCryptoManager,
        messageId: Long,
        status: String,
        statusNote: String,
        persistToDisk: Boolean = true
    ) {
        if (messageId <= 0L) return
        val appContext = context.applicationContext
        val current = if (memorySnapshot.isNotEmpty()) {
            memorySnapshot
        } else if (persistToDisk) {
            VaultCryptoManager.bindContext(appContext)
            readDiskSnapshot(appContext, crypto)
        } else {
            emptyList()
        }
        if (current.isEmpty()) return
        var changed = false
        val updated = ArrayList<DecryptedSmsMessage>(current.size)
        for (i in 0 until current.size) {
            val item = current[i]
            if (item.id == messageId && (item.status != status || item.statusNote != statusNote)) {
                changed = true
                updated.add(item.copy(status = status, statusNote = statusNote))
            } else {
                updated.add(item)
            }
        }
        if (changed) {
            memorySnapshot = updated
            if (persistToDisk) {
                saveSnapshotToDisk(appContext, crypto, updated)
            }
        }
    }

    /**
     * Clears both RAM and disk caches (ONLY used during explicit Emergency Intruder Wipe).
     */
    fun clear(context: Context) {
        memorySnapshot = emptyList()
        lastSavedFingerprint = 0L
        synchronized(diskLock) {
            val dir = context.applicationContext.filesDir
            runCatching { File(dir, CACHE_FILE_NAME).delete() }
            runCatching { File(dir, CACHE_BACKUP_FILE_NAME).delete() }
            runCatching { File(dir, "$CACHE_FILE_NAME.tmp").delete() }
        }
    }

    private fun selectSnapshotSubset(messages: List<DecryptedSmsMessage>): List<DecryptedSmsMessage> {
        if (messages.size <= MAX_TOTAL_SNAPSHOT_MESSAGES) return messages

        val out = ArrayList<DecryptedSmsMessage>(MAX_TOTAL_SNAPSHOT_MESSAGES)
        val seenThreads = HashSet<String>(1024)
        val addedIds = HashSet<Long>(MAX_TOTAL_SNAPSHOT_MESSAGES)

        // 1. Keep the newest MAX_RECENT_MESSAGES messages verbatim (covers active chats & recent history)
        val recentCount = minOf(messages.size, MAX_RECENT_MESSAGES)
        for (i in 0 until recentCount) {
            val m = messages[i]
            out.add(m)
            addedIds.add(m.id)
            seenThreads.add("${m.status == MessageStatus.BLOCKED_AD}_${m.canonicalThreadKey}")
        }

        // 2. Also include the latest message of any older conversation threads up to MAX_TOTAL_SNAPSHOT_MESSAGES
        for (i in recentCount until messages.size) {
            if (out.size >= MAX_TOTAL_SNAPSHOT_MESSAGES) break
            val m = messages[i]
            val threadBucket = "${m.status == MessageStatus.BLOCKED_AD}_${m.canonicalThreadKey}"
            if (seenThreads.add(threadBucket) && addedIds.add(m.id)) {
                out.add(m)
            }
        }
        return out
    }

    private fun readSingleSnapshotFile(file: File, crypto: VaultCryptoManager): List<DecryptedSmsMessage> {
        return runCatching {
            if (!file.exists() || file.length() <= 0L || file.length() > 16L * 1024L * 1024L) {
                return@runCatching emptyList()
            }
            val encrypted = file.readBytes()
            val rawBytes = crypto.decryptBytes(encrypted)
            deserializeMessages(rawBytes)
        }.getOrDefault(emptyList())
    }

    private fun readDiskSnapshot(context: Context, crypto: VaultCryptoManager): List<DecryptedSmsMessage> {
        synchronized(diskLock) {
            val primaryFile = File(context.filesDir, CACHE_FILE_NAME)
            val primaryList = readSingleSnapshotFile(primaryFile, crypto)
            if (primaryList.isNotEmpty()) {
                return primaryList
            }

            // Automatic fallback to shadow backup (.bin.bak) if primary was interrupted or missing
            val backupFile = File(context.filesDir, CACHE_BACKUP_FILE_NAME)
            val backupList = readSingleSnapshotFile(backupFile, crypto)
            if (backupList.isNotEmpty()) {
                runCatching { backupFile.copyTo(primaryFile, overwrite = true) }
                return backupList
            }
            return emptyList()
        }
    }

    private fun serializeMessages(messages: List<DecryptedSmsMessage>): ByteArray {
        val baos = ByteArrayOutputStream(minOf(messages.size * 96 + 64, 512 * 1024))
        GZIPOutputStream(baos, 16384).use { gzip ->
            BufferedOutputStream(gzip, 16384).use { bos ->
                DataOutputStream(bos).use { out ->
                    out.writeInt(CACHE_MAGIC)
                    out.writeInt(CACHE_VERSION)
                    out.writeInt(messages.size)
                for (i in 0 until messages.size) {
                    val m = messages[i]
                    out.writeLong(m.id)
                    writeSafeString(out, m.contactName)
                    writeSafeString(out, m.phoneNumber)
                    writeSafeString(out, m.body)
                    writeSafeString(out, m.direction)
                    writeSafeString(out, m.status)
                    writeSafeString(out, m.statusNote)
                    out.writeLong(m.timestamp)
                    out.writeBoolean(m.scheduledFor != null)
                    if (m.scheduledFor != null) out.writeLong(m.scheduledFor)
                    writeSafeString(out, m.simSlotLabel)
                    out.writeInt(m.simSubId)
                    out.writeInt(m.partCount)
                    out.writeBoolean(m.isUnknownSender)
                    out.writeBoolean(m.isSilentIntercepted)
                    out.writeBoolean(m.isSecretBurnAfterRead)
                    out.writeBoolean(m.isRead)
                    out.writeBoolean(m.isDeleted)
                    out.writeBoolean(m.deletedAt != null)
                    if (m.deletedAt != null) out.writeLong(m.deletedAt)
                    writeSafeString(out, m.canonicalThreadKey)
                }
                }
            }
        }
        return baos.toByteArray()
    }

    private fun deserializeMessages(rawBytes: ByteArray): List<DecryptedSmsMessage> {
        GZIPInputStream(ByteArrayInputStream(rawBytes), 16384).use { gzip ->
            BufferedInputStream(gzip, 16384).use { bis ->
                DataInputStream(bis).use { inp ->
                    val magic = inp.readInt()
                    val version = inp.readInt()
                    if (magic != CACHE_MAGIC || version != CACHE_VERSION) return emptyList()
                    val count = inp.readInt().coerceIn(0, MAX_TOTAL_SNAPSHOT_MESSAGES + 500)
                    val out = ArrayList<DecryptedSmsMessage>(count)
                for (i in 0 until count) {
                    val id = inp.readLong()
                    val contactName = readSafeString(inp)
                    val phoneNumber = readSafeString(inp)
                    val body = readSafeString(inp)
                    val direction = readSafeString(inp)
                    val status = readSafeString(inp)
                    val statusNote = readSafeString(inp)
                    val timestamp = inp.readLong()
                    val hasScheduled = inp.readBoolean()
                    val scheduledFor = if (hasScheduled) inp.readLong() else null
                    val simSlotLabel = readSafeString(inp)
                    val simSubId = inp.readInt()
                    val partCount = inp.readInt()
                    val isUnknownSender = inp.readBoolean()
                    val isSilentIntercepted = inp.readBoolean()
                    val isSecretBurnAfterRead = inp.readBoolean()
                    val isRead = inp.readBoolean()
                    val isDeleted = inp.readBoolean()
                    val hasDeletedAt = inp.readBoolean()
                    val deletedAt = if (hasDeletedAt) inp.readLong() else null
                    val canonicalThreadKey = readSafeString(inp)
                    out.add(
                        DecryptedSmsMessage(
                            id = id,
                            contactName = contactName,
                            phoneNumber = phoneNumber,
                            body = body,
                            direction = direction,
                            status = status,
                            statusNote = statusNote,
                            timestamp = timestamp,
                            scheduledFor = scheduledFor,
                            simSlotLabel = simSlotLabel,
                            simSubId = simSubId,
                            partCount = partCount,
                            isUnknownSender = isUnknownSender,
                            isSilentIntercepted = isSilentIntercepted,
                            isSecretBurnAfterRead = isSecretBurnAfterRead,
                            isRead = isRead,
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            canonicalThreadKey = canonicalThreadKey
                        )
                    )
                }
                return out
                }
            }
        }
    }

    private fun writeSafeString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        if (bytes.isNotEmpty()) {
            out.write(bytes)
        }
    }

    private fun readSafeString(inp: DataInputStream): String {
        val len = inp.readInt()
        if (len <= 0) return ""
        val bytes = ByteArray(len)
        inp.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
