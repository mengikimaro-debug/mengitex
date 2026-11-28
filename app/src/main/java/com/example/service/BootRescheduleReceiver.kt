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
 * Re-arms all pending Scheduled SMS alarms automatically after device reboot or app update,
 * and immediately dispatches any scheduled SMS whose trigger time passed while powered off.
 */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefs = SecurityPreferences(appContext).getSnapshot()
                if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    runCatching {
                        java.io.File(appContext.cacheDir, "github_updates").listFiles()?.forEach { f ->
                            runCatching { f.delete() }
                        }
                        MengiTexNotificationManager.ensureAllChannelsCreated(appContext, prefs)
                    }
                }
                val crypto = VaultCryptoManager(appContext)
                val logger = HiddenVaultLogger(appContext, crypto)
                val dao = SiriSmsDatabase.getInstance(appContext).dao()
                val repo = SiriSmsRepository(dao, crypto, logger)
                val now = System.currentTimeMillis()

                val allMessages = dao.getAllMessagesOnce()
                val scheduled = allMessages.filter { !it.isDeleted && it.status == MessageStatus.SCHEDULED }
                scheduled.forEach { entity ->
                    val targetTime = entity.scheduledFor ?: return@forEach
                    if (targetTime <= now) {
                        val decrypted = repo.getMessageByIdDecrypted(entity.id) ?: return@forEach
                        val sendResult = SmsDispatchManager.sendMultipartSms(
                            context = appContext,
                            messageId = decrypted.id,
                            phoneNumber = decrypted.phoneNumber,
                            body = decrypted.body,
                            simSubId = decrypted.simSubId,
                            requestDeliveryReport = prefs.deliveryReportsEnabled
                        )
                        if (sendResult.isSuccess) {
                            val note = "Imetumwa baada ya simu kuwaka • ${decrypted.simSlotLabel}"
                            repo.updateMessageStatus(
                                id = decrypted.id,
                                status = MessageStatus.SENDING,
                                note = note
                            )
                            LiveSmsEventBus.emitStatusUpdate(decrypted.id, MessageStatus.SENDING, note)
                        } else {
                            val note = sendResult.exceptionOrNull()?.message ?: "Imeshindwa kutuma baada ya kuwaka"
                            repo.updateMessageStatus(
                                id = decrypted.id,
                                status = MessageStatus.FAILED,
                                note = note
                            )
                            LiveSmsEventBus.emitStatusUpdate(decrypted.id, MessageStatus.FAILED, note)
                        }
                    } else {
                        SmsDispatchManager.scheduleAlarmForSms(appContext, entity.id, targetTime)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
