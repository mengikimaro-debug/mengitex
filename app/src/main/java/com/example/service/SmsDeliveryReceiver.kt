package com.example.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.example.data.LiveSmsEventBus
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Receives real carrier SENT and DELIVERED status intents from Android SmsManager.
 * - Preserves original message timestamps (never mutates timestamp during status updates).
 * - Pushes 0ms instant tick updates to the UI via [LiveSmsEventBus].
 * - Writes to Android's Telephony.Sms.Sent provider ONLY ONCE after Activity.RESULT_OK
 *   with the exact same timestamp, eliminating duplicate sent bubbles.
 * - Never falsely downgrades a carrier-accepted SENT SMS to FAILED when a carrier doesn't
 *   return a standard GSM delivery-report PDU.
 */
class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        // Tracks messageIds already written to Android's Telephony.Sms.Sent provider (for multipart SMS)
        private val writtenToSystemSent = ConcurrentHashMap<Long, Long>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsDispatchManager.EXTRA_MESSAGE_ID, -1L)
        if (messageId <= 0L) return

        val pendingResult = goAsync()
        val action = intent.action
        val code = resultCode
        val pduBytes = intent.getByteArrayExtra("pdu")
        val pduFormat = intent.getStringExtra("format")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val crypto = VaultCryptoManager(appCtx)
                val logger = HiddenVaultLogger(appCtx, crypto)
                val dao = SiriSmsDatabase.getInstance(appCtx).dao()
                val repo = SiriSmsRepository(dao, crypto, logger)

                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val existingMsg = runCatching { repo.getMessageByIdDecrypted(messageId) }.getOrNull()

                when (action) {
                    SmsDispatchManager.ACTION_SMS_SENT -> {
                        when (code) {
                            Activity.RESULT_OK -> {
                                // Never downgrade an already DELIVERED message back to SENT on late multipart ACK
                                if (existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Imetumwa ($nowTime)"
                                    repo.updateMessageStatus(
                                        id = messageId,
                                        status = MessageStatus.SENT,
                                        note = note
                                    )
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.SENT, note)
                                }
                                // Persist to Android's Telephony.Sms.Sent ONCE with the EXACT Room timestamp
                                if (existingMsg != null &&
                                    !existingMsg.isSecretBurnAfterRead &&
                                    DefaultAppRoleHelper.isDefaultSmsApp(appCtx) &&
                                    writtenToSystemSent.putIfAbsent(messageId, existingMsg.timestamp) == null
                                ) {
                                    LiveSmsEventBus.rememberLocalMessage(
                                        threadKey = existingMsg.canonicalThreadKey,
                                        direction = MessageDirection.OUTGOING,
                                        body = existingMsg.body,
                                        timestamp = existingMsg.timestamp
                                    )
                                    DefaultAppRoleHelper.writeSentSmsToSystemProvider(
                                        context = appCtx,
                                        address = existingMsg.phoneNumber,
                                        body = existingMsg.body,
                                        timestamp = existingMsg.timestamp
                                    )
                                    if (writtenToSystemSent.size > 256) {
                                        writtenToSystemSent.clear()
                                    }
                                }
                            }

                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                if (existingMsg?.status != MessageStatus.SENT && existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Hakuna Mtandao ($nowTime)"
                                    repo.updateMessageStatus(messageId, MessageStatus.FAILED, note)
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.FAILED, note)
                                }
                            }

                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                if (existingMsg?.status != MessageStatus.SENT && existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Hali ya Ndege Imewashwa ($nowTime)"
                                    repo.updateMessageStatus(messageId, MessageStatus.FAILED, note)
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.FAILED, note)
                                }
                            }

                            else -> {
                                // Only mark FAILED if it hasn't already succeeded
                                if (existingMsg?.status != MessageStatus.SENT && existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Imeshindwa kutumwa ($code • $nowTime)"
                                    repo.updateMessageStatus(messageId, MessageStatus.FAILED, note)
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.FAILED, note)
                                }
                            }
                        }
                    }

                    SmsDispatchManager.ACTION_SMS_DELIVERED -> {
                        val hasPdu = pduBytes != null && pduBytes.isNotEmpty()
                        val tpStatus = if (hasPdu) {
                            runCatching {
                                val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pduFormat.isNullOrBlank()) {
                                    SmsMessage.createFromPdu(pduBytes, pduFormat)
                                } else {
                                    @Suppress("DEPRECATION")
                                    SmsMessage.createFromPdu(pduBytes)
                                }
                                sms?.status ?: 0
                            }.getOrDefault(0)
                        } else {
                            0
                        }

                        // GSM 3GPP TS 23.040: 0x00..0x1F = delivered, 0x20..0x3F = still pending, >=0x40 = delivery report unavailable/rejected
                        val isConfirmedDelivered = code == Activity.RESULT_OK && tpStatus in 0x00..0x1F
                        val isTemporaryUnreachable = tpStatus in 0x20..0x3F

                        when {
                            isConfirmedDelivered -> {
                                val note = "Imefika ($nowTime) ✓✓"
                                repo.updateMessageStatus(
                                    id = messageId,
                                    status = MessageStatus.DELIVERED,
                                    note = note
                                )
                                LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.DELIVERED, note)
                                runCatching {
                                    val settings = SecurityPreferences(appCtx).getSnapshot()
                                    val recipientLabel = existingMsg?.contactName?.ifBlank { existingMsg.phoneNumber } ?: "SMS"
                                    MengiTexNotificationManager.postDeliveryReportNotification(
                                        context = appCtx,
                                        recipientName = recipientLabel,
                                        phoneNumber = existingMsg?.phoneNumber.orEmpty(),
                                        statusNote = note,
                                        settings = settings
                                    )
                                }
                            }

                            isTemporaryUnreachable || !hasPdu || code != Activity.RESULT_OK -> {
                                // Never falsely mark an already-SENT SMS as FAILED when carrier omits delivery report
                                if (existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Imetumwa ($nowTime)"
                                    repo.updateMessageStatus(messageId, MessageStatus.SENT, note)
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.SENT, note)
                                }
                            }

                            else -> {
                                // Explicit permanent delivery rejection PDU while SMSC still accepted it
                                if (existingMsg?.status != MessageStatus.DELIVERED) {
                                    val note = "Imetumwa • Haijafika bado ($nowTime)"
                                    repo.updateMessageStatus(messageId, MessageStatus.SENT, note)
                                    LiveSmsEventBus.emitStatusUpdate(messageId, MessageStatus.SENT, note)
                                }
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
