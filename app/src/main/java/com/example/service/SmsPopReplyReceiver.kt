package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.R
import com.example.data.LiveSmsEventBus
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.data.SmsInstantCache
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles interactive notification actions directly from the Android notification shade:
 * 1. Direct Inline "Pop Reply" (`ACTION_POP_REPLY`) — sends offline cellular SMS via SIM & logs encrypted in Room.
 * 2. "Mark as Read" (`ACTION_MARK_READ`) — marks all unread messages from that sender read & dismisses notification.
 * 3. "Mute Sender" (`ACTION_MUTE_SENDER`) — mutes future notifications from that sender & dismisses notification.
 */
class SmsPopReplyReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_POP_REPLY = "com.example.action.POP_REPLY_SMS"
        const val ACTION_MARK_READ = "com.example.action.MARK_READ_SMS"
        const val ACTION_MUTE_SENDER = "com.example.action.MUTE_SENDER_SMS"
        const val ACTION_COPY_OTP = "com.example.action.COPY_OTP_SMS"
        const val ACTION_DELETE_LATEST = "com.example.action.DELETE_LATEST_SMS"
        const val KEY_TEXT_REPLY = "key_pop_reply_text"
        const val EXTRA_SENDER_NAME = "extra_pop_sender_name"
        const val EXTRA_PHONE_NUMBER = "extra_pop_phone_number"
        const val EXTRA_NOTIFICATION_ID = "extra_pop_notification_id"
        const val EXTRA_OTP_CODE = "extra_pop_otp_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty().trim()
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME).orEmpty().ifBlank { phoneNumber }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val otpCode = intent.getStringExtra(EXTRA_OTP_CODE).orEmpty().trim()

        when (action) {
            ACTION_MARK_READ -> {
                if (phoneNumber.isEmpty()) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        VaultCryptoManager.bindContext(appContext)
                        val crypto = VaultCryptoManager(appContext)
                        val logger = HiddenVaultLogger(appContext, crypto)
                        val repo = SiriSmsRepository(
                            SiriSmsDatabase.getInstance(appContext).dao(),
                            crypto,
                            logger
                        )
                        repo.markSenderMessagesRead(phoneNumber)
                        if (notificationId != 0) {
                            appContext.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_MUTE_SENDER -> {
                if (phoneNumber.isEmpty()) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        SecurityPreferences(appContext).muteNotificationSender(phoneNumber)
                        if (notificationId != 0) {
                            appContext.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_COPY_OTP -> {
                if (otpCode.isNotEmpty()) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("OTP", otpCode))
                    android.widget.Toast.makeText(
                        context.applicationContext,
                        "OTP $otpCode",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                if (notificationId != 0) {
                    context.applicationContext.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
                }
            }

            ACTION_DELETE_LATEST -> {
                if (phoneNumber.isEmpty()) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        VaultCryptoManager.bindContext(appContext)
                        val crypto = VaultCryptoManager(appContext)
                        val logger = HiddenVaultLogger(appContext, crypto)
                        val repo = SiriSmsRepository(
                            SiriSmsDatabase.getInstance(appContext).dao(),
                            crypto,
                            logger
                        )
                        repo.markSenderMessagesRead(phoneNumber)
                        if (notificationId != 0) {
                            appContext.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_POP_REPLY -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
                val replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim().orEmpty()
                if (replyText.isEmpty() || (phoneNumber.isEmpty() && senderName.isEmpty())) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        VaultCryptoManager.bindContext(appContext)
                        val prefs = LiveSmsEventBus.cachedSettings ?: SecurityPreferences(appContext).getSnapshot().also {
                            LiveSmsEventBus.updateCachedSettings(it)
                        }
                        val isSw = prefs.language == AppLanguage.SW
                        val crypto = VaultCryptoManager(appContext)
                        val logger = HiddenVaultLogger(appContext, crypto)
                        val repo = SiriSmsRepository(
                            SiriSmsDatabase.getInstance(appContext).dao(),
                            crypto,
                            logger
                        )

                        val resolvedPhone = repo.findBestPhoneForSender(senderName, phoneNumber)
                            .ifBlank { phoneNumber.ifBlank { senderName } }
                        val resolvedSenderName = ContactsLoader.cleanSenderIdentifier(senderName).ifBlank { resolvedPhone }

                        repo.markSenderMessagesRead(resolvedPhone)

                        val activeSim = SimCardManager.resolveEffectiveSim(appContext, prefs.selectedSimSubId)
                        val simSubId = activeSim?.subscriptionId ?: prefs.selectedSimSubId
                        val simLabel = activeSim?.displayName ?: "SIM"
                        val parts = SmsDispatchManager.calculateSmsParts(appContext, replyText, simSubId)
                        val now = System.currentTimeMillis()

                        if (!SmsDispatchManager.hasSendSmsPermission(appContext)) {
                            val permMsg = if (isSw) {
                                "Ruhusa ya SEND_SMS haijatolewa — gusa kufungua MengiTex"
                            } else {
                                "SEND_SMS permission missing — tap to open MengiTex"
                            }
                            val msgId = repo.insertOutgoingOrScheduled(
                                contactName = resolvedSenderName,
                                phoneNumber = resolvedPhone,
                                plainBody = replyText,
                                status = MessageStatus.FAILED,
                                statusNote = permMsg,
                                simSlotLabel = simLabel,
                                simSubId = simSubId,
                                partCount = parts,
                                timestamp = now
                            )
                            runCatching {
                                repo.getMessageByIdDecrypted(msgId)?.let { decMsg ->
                                    SmsInstantCache.prependMessage(appContext, crypto, decMsg)
                                    LiveSmsEventBus.emitIncoming(decMsg)
                                }
                            }
                            MengiTexNotificationManager.postPopReplyProgressNotification(
                                context = appContext,
                                notificationId = notificationId,
                                senderName = resolvedSenderName,
                                phoneNumber = resolvedPhone,
                                replyText = replyText,
                                statusLine = permMsg,
                                isTerminal = true,
                                isFailure = true,
                                settings = prefs
                            )
                            return@launch
                        }

                        val sendingNote = if (isSw) {
                            "Pop Reply inatuma kupitia $simLabel..."
                        } else {
                            "Pop Reply sending via $simLabel..."
                        }
                        val msgId = repo.insertOutgoingOrScheduled(
                            contactName = resolvedSenderName,
                            phoneNumber = resolvedPhone,
                            plainBody = replyText,
                            status = MessageStatus.SENDING,
                            statusNote = sendingNote,
                            simSlotLabel = simLabel,
                            simSubId = simSubId,
                            partCount = parts,
                            timestamp = now
                        )
                        runCatching {
                            repo.getMessageByIdDecrypted(msgId)?.let { decMsg ->
                                SmsInstantCache.prependMessage(appContext, crypto, decMsg)
                                LiveSmsEventBus.emitIncoming(decMsg)
                            }
                        }

                        MengiTexNotificationManager.registerPopReplyNotification(
                            messageId = msgId,
                            notificationId = notificationId,
                            senderName = resolvedSenderName,
                            phoneNumber = resolvedPhone,
                            replyText = replyText,
                            timestampMillis = now
                        )
                        MengiTexNotificationManager.postPopReplyProgressNotification(
                            context = appContext,
                            notificationId = notificationId,
                            senderName = resolvedSenderName,
                            phoneNumber = resolvedPhone,
                            replyText = replyText,
                            statusLine = sendingNote,
                            isTerminal = false,
                            isFailure = false,
                            settings = prefs
                        )

                        val result = SmsDispatchManager.sendMultipartSms(
                            context = appContext,
                            messageId = msgId,
                            phoneNumber = resolvedPhone,
                            body = replyText,
                            simSubId = simSubId,
                            requestDeliveryReport = prefs.deliveryReportsEnabled
                        )
                        if (result.isFailure) {
                            val failNote = result.exceptionOrNull()?.message
                                ?: if (isSw) "Pop Reply imeshindwa kutumwa" else "Pop Reply failed"
                            repo.updateMessageStatus(msgId, MessageStatus.FAILED, failNote)
                            SmsInstantCache.updateMessageStatus(appContext, crypto, msgId, MessageStatus.FAILED, failNote)
                            LiveSmsEventBus.emitStatusUpdate(msgId, MessageStatus.FAILED, failNote)
                            MengiTexNotificationManager.updatePopReplyNotificationStatus(
                                context = appContext,
                                messageId = msgId,
                                status = MessageStatus.FAILED,
                                statusNote = failNote,
                                settings = prefs
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
