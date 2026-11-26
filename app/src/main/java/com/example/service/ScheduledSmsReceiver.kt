package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.LiveSmsEventBus
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Triggered by AlarmManager when a scheduled SMS reaches its target timestamp.
 */
class ScheduledSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsDispatchManager.EXTRA_MESSAGE_ID, -1L)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefs = SecurityPreferences(appContext).getSnapshot()
                val crypto = VaultCryptoManager(appContext)
                val logger = HiddenVaultLogger(appContext, crypto)
                val repo = SiriSmsRepository(
                    SiriSmsDatabase.getInstance(appContext).dao(),
                    crypto,
                    logger
                )

                val targets = if (messageId > 0L) {
                    listOfNotNull(repo.getMessageByIdDecrypted(messageId))
                        .filter { it.status == MessageStatus.SCHEDULED && !it.isDeleted }
                } else {
                    repo.getDueScheduledMessages()
                }

                targets.forEach { msg ->
                    val sendResult = SmsDispatchManager.sendMultipartSms(
                        context = appContext,
                        messageId = msg.id,
                        phoneNumber = msg.phoneNumber,
                        body = msg.body,
                        simSubId = msg.simSubId,
                        requestDeliveryReport = prefs.deliveryReportsEnabled
                    )
                    if (sendResult.isSuccess) {
                        val note = "Ratiba imefika — inatuma kupitia ${msg.simSlotLabel}"
                        repo.updateMessageStatus(
                            id = msg.id,
                            status = MessageStatus.SENDING,
                            note = note
                        )
                        LiveSmsEventBus.emitStatusUpdate(msg.id, MessageStatus.SENDING, note)
                    } else {
                        val note = "Ratiba imeshindwa kutuma: ${sendResult.exceptionOrNull()?.message ?: "Ruhusa ya SMS inahitajika"}"
                        repo.updateMessageStatus(
                            id = msg.id,
                            status = MessageStatus.FAILED,
                            note = note
                        )
                        LiveSmsEventBus.emitStatusUpdate(msg.id, MessageStatus.FAILED, note)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
