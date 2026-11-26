package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.example.data.SiriSmsDatabase
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android CallScreeningService that blocks unknown/unsaved numbers from calling,
 * suppresses ringtone, avoids waking the screen, and prevents any call notification on screen.
 */
@RequiresApi(Build.VERSION_CODES.N)
class StealthCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection != Call.Details.DIRECTION_INCOMING
        ) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val incomingNumber = callDetails.handle?.schemeSpecificPart.orEmpty()

        CoroutineScope(Dispatchers.IO).launch {
            val appContext = applicationContext
            val prefs = SecurityPreferences(appContext).getSnapshot()
            val crypto = VaultCryptoManager(appContext)
            val logger = HiddenVaultLogger(appContext, crypto)

            val systemName = ContactsLoader.lookupContactName(appContext, incomingNumber)
            val normalized = ContactsLoader.normalizePhone(incomingNumber)
            val localContacts = SiriSmsDatabase.getInstance(appContext).dao().getLocalContactsOnce()
            val localName = localContacts.firstOrNull {
                ContactsLoader.normalizePhone(it.phoneNumber) == normalized
            }?.name

            val isUnknown = systemName.isNullOrBlank() && localName.isNullOrBlank()
            val shouldBlockSilently = prefs.blockUnknownCalls && isUnknown

            val response = if (shouldBlockSilently) {
                logger.appendLog(
                    category = "ZUIA_SIMU",
                    title = "Simu ya namba geni imezuiwa kimya",
                    detail = "Namba: ${incomingNumber.ifBlank { "Isiyojulikana" }} • Kioo hakikuwaka na hakuna arifa iliyoonekana."
                )
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setSilenceCall(true)
                        }
                    }
                    .build()
            } else {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipNotification(false)
                    .build()
            }

            respondToCall(callDetails, response)
        }
    }
}
