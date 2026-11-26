package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Handles unlimited-length multipart SMS sending via Android SmsManager (supporting any SIM card),
 * delivery/sent status tracking via PendingIntents, AlarmManager scheduling, and system SMS fallback.
 */
object SmsDispatchManager {

    const val ACTION_SMS_SENT = "com.example.action.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.example.action.SMS_DELIVERED"
    const val ACTION_SEND_SCHEDULED_SMS = "com.example.action.SEND_SCHEDULED_SMS"
    const val EXTRA_MESSAGE_ID = "extra_message_id"
    const val EXTRA_PART_INDEX = "extra_part_index"
    const val EXTRA_TOTAL_PARTS = "extra_total_parts"

    fun hasSendSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun resolveSmsManager(context: Context, subId: Int): SmsManager {
        val validSubId = subId > 0 && subId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
        @Suppress("DEPRECATION")
        val legacyDefault = { SmsManager.getDefault() }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java) ?: legacyDefault()
            if (validSubId) {
                runCatching { base.createForSubscriptionId(subId) }.getOrDefault(base)
            } else {
                base
            }
        } else {
            @Suppress("DEPRECATION")
            if (validSubId) {
                runCatching { SmsManager.getSmsManagerForSubscriptionId(subId) }
                    .getOrDefault(legacyDefault())
            } else {
                legacyDefault()
            }
        }
    }

    /**
     * Zero-IPC pure-Kotlin SMS segment estimator (~0.001ms) for Main Thread typing & instant sends.
     * Avoids calling SmsManager Binder IPC on every keystroke or Send tap.
     */
    fun estimateSmsPartsFast(text: String): Int {
        if (text.isEmpty()) return 0
        var hasNonGsm = false
        for (i in 0 until text.length) {
            val ch = text[i].code
            if (ch > 127) {
                hasNonGsm = true
                break
            }
        }
        return if (hasNonGsm) {
            if (text.length <= 70) 1 else ((text.length - 1) / 67) + 1
        } else {
            if (text.length <= 160) 1 else ((text.length - 1) / 153) + 1
        }
    }

    /**
     * Calculates exact SMS multipart segments with NO character limit on user typing.
     */
    fun calculateSmsParts(context: Context, text: String, subId: Int = -1): Int {
        if (text.isEmpty()) return 0
        val effectiveSubId = if (subId > 0) subId else (SimCardManager.resolveEffectiveSim(context, subId)?.subscriptionId ?: -1)
        return try {
            resolveSmsManager(context, effectiveSubId).divideMessage(text).size.coerceAtLeast(1)
        } catch (_: Exception) {
            estimateSmsPartsFast(text)
        }
    }

    /**
     * Dispatches unlimited-length SMS via SmsManager.sendMultipartTextMessage with optional
     * carrier Sent & Delivered reports controlled by [requestDeliveryReport].
     * System Telephony.Sms.Sent logging is deferred strictly to SmsDeliveryReceiver upon
     * Activity.RESULT_OK so failed sends NEVER create duplicate "Sent" copies.
     */
    fun sendMultipartSms(
        context: Context,
        messageId: Long,
        phoneNumber: String,
        body: String,
        simSubId: Int,
        requestDeliveryReport: Boolean = true
    ): Result<Int> {
        if (!hasSendSmsPermission(context)) {
            return Result.failure(SecurityException("Ruhusa ya SEND_SMS haijatolewa"))
        }
        val cleanDestination = ContactsLoader.cleanSenderIdentifier(phoneNumber)
        if (cleanDestination.isEmpty()) {
            return Result.failure(IllegalArgumentException("Namba ya mpokeaji haipo"))
        }
        return try {
            val candidateSubIds = SimCardManager.getCandidateSubIds(context, simSubId)
            val primarySubId = if (simSubId > 0 && simSubId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                simSubId
            } else {
                candidateSubIds.firstOrNull() ?: -1
            }
            val smsManager = resolveSmsManager(context, primarySubId)
            val allParts = smsManager.divideMessage(body)
            val totalParts = allParts.size.coerceAtLeast(1)
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Carrier-safe batch size (8 parts per radio frame) so 20, 50, or 200+ part SMS never fail
            val batches = allParts.chunked(8)
            var globalPartIndex = 0

            batches.forEach { batch ->
                val sentIntents = ArrayList<PendingIntent>(batch.size)
                val deliveredIntents = if (requestDeliveryReport) ArrayList<PendingIntent>(batch.size) else null

                batch.indices.forEach { _ ->
                    val partIdx = globalPartIndex
                    val sentIntent = Intent(context, SmsDeliveryReceiver::class.java).apply {
                        action = ACTION_SMS_SENT
                        putExtra(EXTRA_MESSAGE_ID, messageId)
                        putExtra(EXTRA_PART_INDEX, partIdx)
                        putExtra(EXTRA_TOTAL_PARTS, totalParts)
                    }
                    val reqBase = ((messageId * 31L) + partIdx).toInt()
                    sentIntents.add(
                        PendingIntent.getBroadcast(context, reqBase, sentIntent, mutableFlag)
                    )
                    if (deliveredIntents != null) {
                        val deliveredIntent = Intent(context, SmsDeliveryReceiver::class.java).apply {
                            action = ACTION_SMS_DELIVERED
                            putExtra(EXTRA_MESSAGE_ID, messageId)
                            putExtra(EXTRA_PART_INDEX, partIdx)
                            putExtra(EXTRA_TOTAL_PARTS, totalParts)
                        }
                        deliveredIntents.add(
                            PendingIntent.getBroadcast(context, reqBase + 100000, deliveredIntent, mutableFlag)
                        )
                    }
                    globalPartIndex++
                }

                var dispatched = false
                var lastErr: Exception? = null
                val attemptSubIds = ArrayList<Int>(candidateSubIds.size + 2).apply {
                    add(primarySubId)
                    candidateSubIds.forEach { if (it != primarySubId) add(it) }
                    if (-1 !in this) add(-1)
                }
                for (attemptSubId in attemptSubIds) {
                    try {
                        val mgr = if (attemptSubId == primarySubId) smsManager else resolveSmsManager(context, attemptSubId)
                        dispatchBatch(mgr, cleanDestination, body, batch, sentIntents, deliveredIntents)
                        dispatched = true
                        break
                    } catch (err: Exception) {
                        lastErr = err
                    }
                }
                if (!dispatched) {
                    throw (lastErr ?: IllegalStateException("Imeshindwa kutuma SMS"))
                }
            }

            Result.success(totalParts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun dispatchBatch(
        smsManager: SmsManager,
        phoneNumber: String,
        originalBody: String,
        batch: List<String>,
        sentIntents: ArrayList<PendingIntent>,
        deliveredIntents: ArrayList<PendingIntent>?
    ) {
        if (batch.size <= 1) {
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                batch.firstOrNull() ?: originalBody,
                sentIntents.firstOrNull(),
                deliveredIntents?.firstOrNull()
            )
        } else {
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                ArrayList(batch),
                sentIntents,
                deliveredIntents
            )
        }
    }

    /**
     * Schedules a future broadcast via AlarmManager to send an SMS automatically at triggerAtMillis.
     */
    fun scheduleAlarmForSms(context: Context, messageId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ScheduledSmsReceiver::class.java).apply {
            action = ACTION_SEND_SCHEDULED_SMS
            putExtra(EXTRA_MESSAGE_ID, messageId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelScheduledAlarm(context: Context, messageId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ScheduledSmsReceiver::class.java).apply {
            action = ACTION_SEND_SCHEDULED_SMS
            putExtra(EXTRA_MESSAGE_ID, messageId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
        }
    }

    /**
     * Opens the native Android SMS composer with pre-filled recipients and unlimited body text
     * as a zero-permission fallback when user hasn't granted direct SEND_SMS.
     */
    fun openSystemSmsFallback(context: Context, phoneNumbers: List<String>, body: String) {
        val cleaned = phoneNumbers.mapNotNull { ContactsLoader.cleanSenderIdentifier(it).takeIf(String::isNotBlank) }
        if (cleaned.isEmpty()) return
        val joined = cleaned.joinToString(";")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(joined)}")
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Dispatches a selected Photo or Document URI via Android's native MMS / Share chooser
     * with temporary read URI grant (100% zero storage permission, Google Play compliant).
     */
    fun sendMediaViaMmsOrShare(
        context: Context,
        phoneNumber: String,
        uri: Uri,
        mimeType: String,
        captionBody: String,
        chooserTitle: String
    ): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("address", phoneNumber)
            if (captionBody.isNotBlank()) {
                putExtra("sms_body", captionBody)
                putExtra(Intent.EXTRA_TEXT, captionBody)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
