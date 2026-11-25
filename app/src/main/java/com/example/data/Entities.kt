package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object MessageDirection {
    const val INCOMING = "INCOMING"
    const val OUTGOING = "OUTGOING"
}

object MessageStatus {
    const val RECEIVED = "RECEIVED"
    const val SENDING = "SENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val FAILED = "FAILED"
    const val SCHEDULED = "SCHEDULED"
    const val BLOCKED_AD = "BLOCKED_AD"
}

@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val encryptedBody: String,
    val direction: String = MessageDirection.OUTGOING,
    val status: String = MessageStatus.SENDING,
    val statusNote: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val scheduledFor: Long? = null,
    val simSlotLabel: String = "SIM Chaguo-msingi",
    val simSubId: Int = -1,
    val partCount: Int = 1,
    val isUnknownSender: Boolean = false,
    val isSilentIntercepted: Boolean = false,
    val isSecretBurnAfterRead: Boolean = false,
    val isRead: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val encryptedBody: String,
    val usageCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "saved_contacts")
data class SavedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DecryptedSmsMessage(
    val id: Long,
    val contactName: String,
    val phoneNumber: String,
    val body: String,
    val direction: String,
    val status: String,
    val statusNote: String,
    val timestamp: Long,
    val scheduledFor: Long?,
    val simSlotLabel: String,
    val simSubId: Int,
    val partCount: Int,
    val isUnknownSender: Boolean,
    val isSilentIntercepted: Boolean,
    val isSecretBurnAfterRead: Boolean,
    val isRead: Boolean,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val canonicalThreadKey: String = com.example.service.ContactsLoader.resolveCanonicalThreadKey(phoneNumber, contactName)
)

data class DecryptedTemplate(
    val id: Long,
    val title: String,
    val category: String,
    val body: String,
    val usageCount: Int,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val deletedAt: Long?
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isLocalSaved: Boolean = false
)
