package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.security.HiddenVaultLogger
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required by Android for Default SMS App (RoleManager.ROLE_SMS) eligibility.
 * Handles incoming MMS WAP Push notifications securely inside MengiTex.
 */
class MmsWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION &&
            intent.action != Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val crypto = VaultCryptoManager()
                val logger = HiddenVaultLogger(appContext, crypto)
                val mimeType = intent.type.orEmpty().ifBlank { "application/vnd.wap.mms-message" }
                logger.appendLog(
                    category = "MMS_SYSTEM",
                    title = "MMS WAP Push received",
                    detail = "Intercepted system MMS push ($mimeType)"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
