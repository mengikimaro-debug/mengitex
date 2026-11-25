package com.example.service

import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony

data class SmsLaunchTarget(
    val phoneNumber: String,
    val draftBody: String
)

/**
 * Manages Android System Ownership Roles so MengiTex operates with full Default SMS App &
 * Call Screening authority (surpassing the stock Messages app with AES-256 vault, stealth blocking,
 * scheduled auto-send, broadcast, pop reply, and delivery reports).
 */
object DefaultAppRoleHelper {

    fun isDefaultSmsApp(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true ||
                    Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
            } else {
                Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    fun createDefaultSmsRoleIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                }
            }
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isDefaultCallScreener(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } catch (_: Exception) {
            false
        }
    }

    fun createCallScreeningRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * When MengiTex is the phone's Default SMS App, persists regular incoming SMS to Android's
     * system Telephony Inbox so the entire OS recognizes MengiTex as authoritative.
     * Stealth-silenced and burn-after-read secret messages intentionally stay ONLY in MengiTex's AES-256 vault.
     */
    fun writeIncomingSmsToSystemProvider(
        context: Context,
        address: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!isDefaultSmsApp(context) || address.isBlank()) return
        runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }
    }

    /**
     * When MengiTex is the phone's Default SMS App, records non-secret outgoing SMS into
     * Android's Telephony Sent provider for complete system-level continuity.
     */
    fun writeSentSmsToSystemProvider(
        context: Context,
        address: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!isDefaultSmsApp(context) || address.isBlank()) return
        runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }
    }

    /**
     * Parses external sms:/smsto:/mms:/mmsto: intents (e.g., tapping "Message" from Contacts,
     * Dialer, or Browser) so MengiTex opens the exact recipient directly.
     */
    fun parseSmsLaunchIntent(intent: Intent?): SmsLaunchTarget? {
        if (intent == null) return null
        val action = intent.action
        if (action != Intent.ACTION_SENDTO && action != Intent.ACTION_SEND && action != Intent.ACTION_VIEW) {
            return null
        }
        val data: Uri? = intent.data
        val scheme = data?.scheme?.lowercase().orEmpty()
        if (scheme !in setOf("sms", "smsto", "mms", "mmsto")) return null

        val ssp = data?.schemeSpecificPart.orEmpty()
        val rawRecipient = ssp.substringBefore('?').trim()
        val recipient = Uri.decode(rawRecipient)
            .split(',', ';')
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()

        val queryBody = runCatching {
            if (ssp.contains("body=", ignoreCase = true)) {
                Uri.decode(ssp.substringAfter("body=", "").substringBefore('&'))
            } else {
                ""
            }
        }.getOrDefault("")

        val extraBody = intent.getStringExtra("sms_body")
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: queryBody

        if (recipient.isBlank() && extraBody.isBlank()) return null
        return SmsLaunchTarget(
            phoneNumber = recipient,
            draftBody = extraBody.trim()
        )
    }
}
