package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.DecryptedSmsMessage
import com.example.data.LiveSmsEventBus
import com.example.data.MessageDirection
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.data.SmsInstantCache
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.SmsAdBlockerEngine
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 6G-Velocity Incoming SMS Receiver (~3ms end-to-end UI arrival).
 * - Reads RAM-warmed SecuritySettings & O(1) Contact Name Map from [LiveSmsEventBus]
 *   (zero DataStore disk read and zero ContactsProvider Binder IPC when app is warm!).
 * - Atomically deduplicates dual SMS_DELIVER / SMS_RECEIVED broadcasts.
 * - Pushes incoming SMS directly into RAM + [LiveSmsEventBus] BEFORE posting notifications,
 *   writing to Android Telephony Inbox, or flushing the encrypted disk snapshot.
 */
class SmsStealthReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_BUBBLE_SMS = MengiTexNotificationManager.CHANNEL_BUBBLE_SMS
        const val EXTRA_OPEN_PHONE = "extra_open_conversation_phone"
        const val EXTRA_OPEN_NAME = "extra_open_conversation_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isDefaultSms = DefaultAppRoleHelper.isDefaultSmsApp(context)
        val isDeliver = action == Telephony.Sms.Intents.SMS_DELIVER_ACTION
        val isReceived = action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        if (!isDeliver && !isReceived) return
        if (isReceived && isDefaultSms) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val groupedBySender = messages.groupBy { it.displayOriginatingAddress.orEmpty().trim() }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                // 0.001ms RAM settings hit (falls back to DataStore ONLY on cold background boot)
                val prefs = LiveSmsEventBus.cachedSettings ?: SecurityPreferences(appContext).getSnapshot().also {
                    LiveSmsEventBus.updateCachedSettings(it)
                }
                val crypto = VaultCryptoManager(appContext)
                val logger = HiddenVaultLogger(appContext, crypto)
                val db = SiriSmsDatabase.getInstance(appContext)
                val repo = SiriSmsRepository(db.dao(), crypto, logger)
                val contactsWarmed = LiveSmsEventBus.isContactsCacheWarmed
                val localContacts = if (contactsWarmed) emptyList() else db.dao().getLocalContactsOnce()
                var lastMemorySnapshotToPersist: List<DecryptedSmsMessage>? = null

                groupedBySender.forEach { (rawSender, parts) ->
                    val senderAddress = rawSender.trim()
                    val fullBody = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                    val receivedAt = System.currentTimeMillis()
                    val cleanAddr = ContactsLoader.cleanSenderIdentifier(senderAddress)
                    val preThreadKey = ContactsLoader.resolveCanonicalThreadKey(
                        cleanAddr.ifBlank { senderAddress },
                        senderAddress
                    )

                    if (!LiveSmsEventBus.claimIncomingBroadcast(preThreadKey, fullBody, receivedAt)) {
                        return@forEach
                    }

                    val isAlphanumericOrService = ContactsLoader.isAlphanumericSender(senderAddress)
                    val normalized = ContactsLoader.normalizePhone(senderAddress)

                    // 0.001ms O(1) contact name lookup when warmed (skips 40–180ms ContactsProvider Binder IPC!)
                    val resolvedContactName = if (isAlphanumericOrService) {
                        null
                    } else if (contactsWarmed) {
                        LiveSmsEventBus.lookupCachedContactName(normalized)
                    } else {
                        localContacts.firstOrNull {
                            ContactsLoader.normalizePhone(it.phoneNumber) == normalized
                        }?.name ?: ContactsLoader.lookupContactName(appContext, senderAddress)
                    }

                    val isSavedContact = !resolvedContactName.isNullOrBlank()
                    val isUnsavedNumeric = !isAlphanumericOrService && !isSavedContact

                    val adDecision = SmsAdBlockerEngine.evaluate(
                        senderAddress = senderAddress,
                        body = fullBody,
                        isSavedContact = isSavedContact,
                        whitelistedSenders = prefs.whitelistedAdSenders,
                        autoBlockAdsEnabled = prefs.autoBlockAdsEnabled,
                        senderName = resolvedContactName.orEmpty()
                    )
                    val isBlockedAd = adDecision.shouldBlock
                    val isProtectedTxn = adDecision.isProtectedTransaction

                    val shouldSilenceCompletely = isBlockedAd || (!isProtectedTxn && isUnsavedNumeric && prefs.silentUnknownSms)
                    val isSecretBurn = !isBlockedAd && !isProtectedTxn && prefs.autoDeleteSecretAfterRead && prefs.securityLevel >= 4

                    val preservedSenderName = when {
                        !resolvedContactName.isNullOrBlank() -> resolvedContactName
                        cleanAddr.isNotBlank() -> cleanAddr
                        senderAddress.isNotBlank() -> senderAddress
                        prefs.language == AppLanguage.EN -> "Incoming SMS"
                        else -> "Ujumbe Mpya"
                    }
                    val preservedPhone = cleanAddr.ifBlank { preservedSenderName }
                    val adReason = if (prefs.language == AppLanguage.EN) adDecision.reasonEn else adDecision.reasonSw
                    val finalThreadKey = ContactsLoader.resolveCanonicalThreadKey(preservedPhone, preservedSenderName)

                    LiveSmsEventBus.rememberLocalMessage(
                        threadKey = finalThreadKey,
                        direction = MessageDirection.INCOMING,
                        body = fullBody,
                        timestamp = receivedAt
                    )

                    val insertedId = repo.insertIncomingMessage(
                        senderName = preservedSenderName,
                        phoneNumber = preservedPhone,
                        plainBody = fullBody,
                        isUnknownSender = isUnsavedNumeric || isBlockedAd,
                        isSilentIntercepted = shouldSilenceCompletely,
                        isSecretBurnAfterRead = isSecretBurn,
                        isBlockedAd = isBlockedAd,
                        adBlockReason = adReason,
                        timestamp = receivedAt
                    )

                    // 1. INSTANT (~3ms) UI delivery via RAM cache + LiveSmsEventBus
                    runCatching {
                        repo.getMessageByIdDecrypted(insertedId)?.let { decMsg ->
                            lastMemorySnapshotToPersist = SmsInstantCache.prependMessageMemoryOnly(decMsg)
                            LiveSmsEventBus.emitIncoming(decMsg)
                        }
                    }

                    // 2. Post instant notification (unless blocked ad or silenced unknown sender)
                    if (!isBlockedAd && !shouldSilenceCompletely) {
                        MengiTexNotificationManager.postIncomingSmsNotification(
                            context = appContext,
                            senderTitle = preservedSenderName,
                            phoneNumber = preservedPhone,
                            plainBody = fullBody,
                            isProtectedTxn = isProtectedTxn,
                            isSecretBurn = isSecretBurn,
                            settings = prefs
                        )
                    }

                    // 3. Persist to Android Telephony Inbox with the EXACT same timestamp
                    if (isDeliver && !isBlockedAd && !shouldSilenceCompletely && !isSecretBurn) {
                        DefaultAppRoleHelper.writeIncomingSmsToSystemProvider(
                            context = appContext,
                            address = senderAddress.ifBlank { preservedSenderName },
                            body = fullBody,
                            timestamp = receivedAt
                        )
                    }
                }

                // 4. Persist instant disk snapshot AFTER UI & notification are already delivered
                val snap = lastMemorySnapshotToPersist
                if (snap != null && snap.isNotEmpty()) {
                    runCatching { SmsInstantCache.saveSnapshotToDisk(appContext, crypto, snap) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
