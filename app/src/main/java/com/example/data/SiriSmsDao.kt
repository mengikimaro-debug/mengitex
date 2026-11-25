package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SiriSmsDao {

    // Active (non-deleted, non-scheduled) SMS messages for Inbox & Delivery Reports
    // Capped at 5,000 in the single reactive CursorWindow so 600k+ databases NEVER throw SQLiteBlobTooBigException
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 AND status != 'SCHEDULED' ORDER BY timestamp DESC LIMIT 5000")
    fun observeActiveMessages(): Flow<List<SmsMessageEntity>>

    // Paged active messages reader for safe CursorWindow-free batch loading of large archives
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 AND status != 'SCHEDULED' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getActiveMessagesPaged(limit: Int, offset: Int): List<SmsMessageEntity>

    // Paged all-messages reader so 600,000+ SMS never overflow Android's 2MB SQLite CursorWindow
    @Query("SELECT * FROM sms_messages ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMessagesPaged(limit: Int, offset: Int): List<SmsMessageEntity>

    @Query("SELECT COUNT(*) FROM sms_messages")
    suspend fun countAllMessagesOnce(): Int

    // Ultra-fast recent slice for instant startup hydration when disk cache isn't built yet
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 AND status != 'SCHEDULED' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActiveMessagesOnce(limit: Int = 350): List<SmsMessageEntity>

    // Fast incremental slice for delta sync deduplication (avoids scanning 50,000+ historical rows)
    @Query("SELECT * FROM sms_messages WHERE timestamp >= :minTimestampMillis ORDER BY timestamp DESC LIMIT 5000")
    suspend fun getMessagesSinceOnce(minTimestampMillis: Long): List<SmsMessageEntity>

    @Query("SELECT MAX(timestamp) FROM sms_messages WHERE isDeleted = 0 AND status != 'SCHEDULED'")
    suspend fun getLatestActiveTimestampOnce(): Long?

    @Query("SELECT COUNT(*) FROM sms_messages WHERE isDeleted = 0 AND status != 'SCHEDULED'")
    suspend fun countActiveMessagesOnce(): Int

    // Scheduled messages waiting to be dispatched
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 AND status = 'SCHEDULED' ORDER BY scheduledFor ASC LIMIT 2000")
    fun observeScheduledMessages(): Flow<List<SmsMessageEntity>>

    // Due scheduled messages (for background/alarm dispatch)
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 AND status = 'SCHEDULED' AND scheduledFor <= :nowMillis")
    suspend fun getDueScheduledMessages(nowMillis: Long): List<SmsMessageEntity>

    @Query("SELECT * FROM sms_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): SmsMessageEntity?

    // Soft-deleted messages in Trash (capped at 2,500 to prevent CursorWindow overflow)
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 1 ORDER BY deletedAt DESC LIMIT 2500")
    fun observeTrashedMessages(): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SmsMessageEntity>): List<Long>

    @Query("UPDATE sms_messages SET status = :status, statusNote = :note, timestamp = :updatedAt WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String, note: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sms_messages SET status = :status, statusNote = :note WHERE id = :id")
    suspend fun updateMessageStatusPreservingTimestamp(id: Long, status: String, note: String)

    @Query("UPDATE sms_messages SET isRead = 1 WHERE id = :id")
    suspend fun markMessageRead(id: Long)

    @Query("UPDATE sms_messages SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markMessagesReadBatch(ids: List<Long>)

    @Query("UPDATE sms_messages SET isSecretBurnAfterRead = 0 WHERE isSecretBurnAfterRead = 1")
    suspend fun keepAllMessagesPermanently()

    @Query("UPDATE sms_messages SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMessage(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sms_messages SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteMessagesBatch(ids: List<Long>, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sms_messages SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreMessage(id: Long)

    @Query("UPDATE sms_messages SET isDeleted = 0, deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreMessagesBatch(ids: List<Long>)

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun permanentlyDeleteMessage(id: Long)

    @Query("DELETE FROM sms_messages WHERE id IN (:ids)")
    suspend fun permanentlyDeleteMessagesBatch(ids: List<Long>)

    @Query("DELETE FROM sms_messages WHERE isDeleted = 1")
    suspend fun emptyMessageTrash()

    // Templates
    @Query("SELECT * FROM message_templates WHERE isDeleted = 0 ORDER BY usageCount DESC, updatedAt DESC")
    fun observeActiveTemplates(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeTrashedTemplates(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun countAllTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplateEntity): Long

    @Query("UPDATE message_templates SET title = :title, category = :category, encryptedBody = :encryptedBody, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTemplate(id: Long, title: String, category: String, encryptedBody: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE message_templates SET usageCount = usageCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementTemplateUsage(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE message_templates SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTemplate(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE message_templates SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTemplate(id: Long)

    @Query("DELETE FROM message_templates WHERE id = :id")
    suspend fun permanentlyDeleteTemplate(id: Long)

    @Query("DELETE FROM message_templates WHERE isDeleted = 1")
    suspend fun emptyTemplateTrash()

    // Local quick contacts
    @Query("SELECT * FROM saved_contacts ORDER BY name ASC")
    fun observeLocalContacts(): Flow<List<SavedContactEntity>>

    @Query("SELECT * FROM saved_contacts")
    suspend fun getLocalContactsOnce(): List<SavedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalContact(contact: SavedContactEntity): Long

    @Query("DELETE FROM saved_contacts WHERE id = :id")
    suspend fun deleteLocalContact(id: Long)

    // Backup & Restore snapshot queries
    @Query("SELECT * FROM sms_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<SmsMessageEntity>

    @Query("SELECT * FROM message_templates ORDER BY updatedAt ASC")
    suspend fun getAllTemplatesOnce(): List<MessageTemplateEntity>

    @Query("DELETE FROM sms_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM message_templates")
    suspend fun deleteAllTemplates()

    @Query("DELETE FROM saved_contacts")
    suspend fun deleteAllLocalContacts()
}
