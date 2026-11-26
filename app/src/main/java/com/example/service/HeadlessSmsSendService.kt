package com.example.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import com.example.data.LiveSmsEventBus
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.data.SmsInstantCache
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required by Android for Default SMS App (RoleManager.ROLE_SMS) eligibility.
 * Handles quick "Respond via Message" SMS replies sent directly from the Android incoming call screen.
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val extras = intent.extras
        val messageBody = extras?.getCharSequence(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val recipientsRaw = intent.data?.schemeSpecificPart
            ?.substringBefore('?')
            ?.let { Uri.decode(it) }
            .orEmpty()
        val recipients = recipientsRaw
            .split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (messageBody.isBlank() || recipients.isEmpty()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = applicationContext
                val prefs = SecurityPreferences(appContext).getSnapshot()
                val crypto = VaultCryptoManager(appContext)
                val logger = HiddenVaultLogger(appContext, crypto)
                val db = SiriSmsDatabase.getInstance(appContext)
                val repo = SiriSmsRepository(db.dao(), crypto, logger)
                val sims = SimCardManager.detectSimCards(appContext)
                val activeSim = sims.firstOrNull { it.subscriptionId == prefs.selectedSimSubId } ?: sims.firstOrNull()
                val simSubId = activeSim?.subscriptionId ?: -1
                val simLabel = activeSim?.displayName ?: "SIM 1"

                recipients.forEach { phone ->
                    val contactName = ContactsLoader.lookupContactName(appContext, phone) ?: phone
                    val partCount = SmsDispatchManager.calculateSmsParts(appContext, messageBody, simSubId)
                    val id = repo.insertOutgoingOrScheduled(
                        contactName = contactName,
                        phoneNumber = phone,
                        plainBody = messageBody,
                        status = MessageStatus.SENDING,
                        statusNote = "Quick Call Reply • $simLabel",
                        simSlotLabel = simLabel,
                        simSubId = simSubId,
                        partCount = partCount,
                        isSecretBurnAfterRead = false
                    )
                    runCatching {
                        repo.getMessageByIdDecrypted(id)?.let { decMsg ->
                            SmsInstantCache.prependMessageMemoryOnly(decMsg)
                            LiveSmsEventBus.emitIncoming(decMsg)
                        }
                    }
                    val sendResult = SmsDispatchManager.sendMultipartSms(
                        context = appContext,
                        messageId = id,
                        phoneNumber = phone,
                        body = messageBody,
                        simSubId = simSubId,
                        requestDeliveryReport = prefs.deliveryReportsEnabled
                    )
                    if (sendResult.isFailure) {
                        val note = sendResult.exceptionOrNull()?.message ?: "Quick reply failed"
                        repo.updateMessageStatus(
                            id = id,
                            status = MessageStatus.FAILED,
                            note = note
                        )
                        LiveSmsEventBus.emitStatusUpdate(id, MessageStatus.FAILED, note)
                    }
                }
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }
}
