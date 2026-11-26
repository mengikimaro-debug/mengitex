package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.BubbleChatActivity
import com.example.MainActivity
import com.example.R
import com.example.security.NotificationPreviewMode
import com.example.security.NotificationVibratePattern
import com.example.security.SecuritySettings
import com.example.security.SmsAdBlockerEngine
import com.example.ui.i18n.AppLanguage
import java.util.Calendar

/**
 * Complete centralized Android Notification Manager for MengiTex.
 * Manages all 5 Android notification channels, Quiet Hours (Do Not Disturb), per-sender mute rules,
 * Lock-Screen privacy preview modes, custom ringtones, vibration patterns, LED pulse, app icon badges,
 * Android Floating Bubbles, and interactive notification actions (Pop Reply, Mark as Read, Mute Sender).
 */
object MengiTexNotificationManager {

    const val CHANNEL_BUBBLE_SMS = "mengitex_bubble_messages"
    const val CHANNEL_TRANSACTIONS_OTP = "mengitex_financial_otp"
    const val CHANNEL_SECRET_VAULT = "mengitex_secret_vault"
    const val CHANNEL_DELIVERY_REPORTS = "mengitex_delivery_reports"
    const val CHANNEL_SILENT_ALERTS = "mengitex_silent_messages"

    private const val TEST_NOTIFICATION_ID = 987654

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val systemEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)
        return runtimeGranted && systemEnabled
    }

    fun getActiveNotificationCount(context: Context): Int {
        return runCatching {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.activeNotifications?.size ?: 0
        }.getOrDefault(0)
    }

    fun clearAllNotifications(context: Context) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.cancelAll()
        }
    }

    fun isCurrentlyInQuietHours(settings: SecuritySettings): Boolean {
        if (!settings.quietHoursEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = settings.quietHoursStartHour.coerceIn(0, 23)
        val end = settings.quietHoursEndHour.coerceIn(0, 23)
        return if (start == end) {
            true
        } else if (start < end) {
            hour in start until end
        } else {
            // Overnight range e.g. 22:00 -> 07:00
            hour >= start || hour < end
        }
    }

    fun isSenderMuted(senderAddress: String, settings: SecuritySettings): Boolean {
        if (settings.mutedNotificationSenders.isEmpty()) return false
        val key = SmsAdBlockerEngine.canonicalSenderKey(senderAddress)
        return key.isNotEmpty() && key in settings.mutedNotificationSenders
    }

    fun isSenderVip(senderAddress: String, settings: SecuritySettings): Boolean {
        if (settings.vipNotificationSenders.isEmpty()) return false
        val key = SmsAdBlockerEngine.canonicalSenderKey(senderAddress)
        return key.isNotEmpty() && key in settings.vipNotificationSenders
    }

    private val otpExtractRegex = Regex("\\b(\\d{4,8})\\b")

    fun extractOtpCode(body: String): String? {
        val lower = body.lowercase()
        val looksLikeOtp = lower.contains("otp") ||
            lower.contains("code") ||
            lower.contains("kodi") ||
            lower.contains("pin") ||
            lower.contains("verif") ||
            lower.contains("thibit") ||
            lower.contains("namba ya siri")
        if (!looksLikeOtp) return null
        return otpExtractRegex.find(body)?.groupValues?.getOrNull(1)
    }

    fun shouldSuppressInAppPop(
        phoneNumber: String,
        isProtectedTxn: Boolean,
        settings: SecuritySettings
    ): Boolean {
        if (!settings.notificationsMasterEnabled || !settings.inAppPopBannerEnabled) return true
        if (isSenderMuted(phoneNumber, settings)) return true
        val isVip = isSenderVip(phoneNumber, settings)
        if (isCurrentlyInQuietHours(settings)) {
            val allowedByTxn = isProtectedTxn && settings.quietHoursAllowTransactions
            val allowedByVip = isVip && settings.quietHoursAllowVip
            if (!allowedByTxn && !allowedByVip) return true
        }
        return false
    }

    fun ensureAllChannelsCreated(context: Context, settings: SecuritySettings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val isSw = settings.language == AppLanguage.SW

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val defaultRingtoneUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 1. Personal SMS & Floating Bubbles channel (Managed automatically by Android System)
        val personalChannel = NotificationChannel(
            CHANNEL_BUBBLE_SMS,
            if (isSw) "SMS Binafsi & Pop Bubbles" else "Personal SMS & Pop Bubbles",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isSw) {
                "Arifa za ujumbe mpya zinasimamiwa kiotomatiki na mfumo wa Android"
            } else {
                "Incoming SMS alerts managed automatically by Android system"
            }
            enableLights(true)
            lightColor = 0xFF10B981.toInt()
            setShowBadge(true)
            enableVibration(true)
            setSound(defaultRingtoneUri, audioAttrs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(true)
            }
        }

        // 2. Financial Transactions & OTP Priority channel
        val txnChannel = NotificationChannel(
            CHANNEL_TRANSACTIONS_OTP,
            if (isSw) "Miamala ya Pesa & OTP" else "Transactions & OTP Priority",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isSw) {
                "M-Pesa, Mixx/Tigo Pesa, Airtel Money, Benki, LUKU na namba za OTP"
            } else {
                "M-Pesa, Mixx/Tigo Pesa, Airtel Money, Banks, LUKU and OTP verification codes"
            }
            enableLights(true)
            lightColor = 0xFF10B981.toInt()
            setShowBadge(true)
            enableVibration(true)
            setSound(defaultRingtoneUri, audioAttrs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(true)
            }
        }

        // 3. Secret Vault Private Alerts channel
        val secretChannel = NotificationChannel(
            CHANNEL_SECRET_VAULT,
            if (isSw) "Ujumbe wa Siri (Private)" else "Secret Vault Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isSw) {
                "Arifa za siri zinazoficha maudhui kwenye Lock Screen"
            } else {
                "Privacy-shielded alerts hiding content on lock screen"
            }
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            enableLights(true)
            lightColor = 0xFF7C3AED.toInt()
            setShowBadge(true)
            enableVibration(true)
            setSound(defaultRingtoneUri, audioAttrs)
        }

        // 4. Delivery & Scheduled SMS Reports channel
        val deliveryChannel = NotificationChannel(
            CHANNEL_DELIVERY_REPORTS,
            if (isSw) "Ripoti za Kufika kwa SMS" else "SMS Delivery & Scheduled Reports",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = if (isSw) {
                "Uthibitisho wa SMS zilizofika (✓✓) na SMS za ratiba"
            } else {
                "Delivery receipt confirmations (✓✓) and scheduled SMS alerts"
            }
            enableLights(false)
            enableVibration(false)
            setShowBadge(false)
            setSound(null, null)
        }

        // 5. Silent / Tray-Only Channel
        val silentChannel = NotificationChannel(
            CHANNEL_SILENT_ALERTS,
            if (isSw) "Arifa Kimya (Bila Pop-Up)" else "Silent Tray Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = if (isSw) {
                "Arifa za kimya bila mlio wala kutokea juu ya skrini"
            } else {
                "Quiet tray alerts without sound or heads-up pop-up"
            }
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            setShowBadge(true)
        }

        nm.createNotificationChannels(
            listOf(personalChannel, txnChannel, secretChannel, deliveryChannel, silentChannel)
        )
    }

    /**
     * Central dispatch for all incoming SMS notifications, honoring EVERY Notification Manager rule.
     */
    fun postIncomingSmsNotification(
        context: Context,
        senderTitle: String,
        phoneNumber: String,
        plainBody: String,
        isProtectedTxn: Boolean,
        isSecretBurn: Boolean,
        settings: SecuritySettings
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        if (isSenderMuted(phoneNumber, settings)) return false

        val isVip = isSenderVip(phoneNumber, settings)

        ensureAllChannelsCreated(context, settings)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val isSw = settings.language == AppLanguage.SW

        // Android System NotificationChannel handles sound, vibration, DND, bubbles & priority automatically
        val channelId = when {
            isProtectedTxn -> CHANNEL_TRANSACTIONS_OTP
            isSecretBurn -> CHANNEL_SECRET_VAULT
            else -> CHANNEL_BUBBLE_SMS
        }

        // Apply Lock-Screen Privacy Preview Mode & Security Level
        val effectivePreviewMode = when {
            isSecretBurn && settings.notifPreviewMode == NotificationPreviewMode.SHOW_ALL ->
                NotificationPreviewMode.SENDER_ONLY
            settings.securityLevel >= 4 && !isProtectedTxn && settings.notifPreviewMode == NotificationPreviewMode.SHOW_ALL ->
                NotificationPreviewMode.SENDER_ONLY
            else -> settings.notifPreviewMode
        }

        val displayTitle = when (effectivePreviewMode) {
            NotificationPreviewMode.HIDE_ALL -> "MengiTex"
            else -> senderTitle
        }

        val displayBody = when (effectivePreviewMode) {
            NotificationPreviewMode.SHOW_ALL -> plainBody
            NotificationPreviewMode.SENDER_ONLY -> {
                if (isSw) "Ujumbe mpya uliosimbwa — fungua MengiTex kusoma"
                else "New encrypted message — open MengiTex to read"
            }
            NotificationPreviewMode.HIDE_ALL -> {
                if (isSw) "Una ujumbe mpya wa siri"
                else "You have a new private message"
            }
        }

        val lockVisibility = when (effectivePreviewMode) {
            NotificationPreviewMode.SHOW_ALL -> NotificationCompat.VISIBILITY_PUBLIC
            NotificationPreviewMode.SENDER_ONLY -> NotificationCompat.VISIBILITY_PRIVATE
            NotificationPreviewMode.HIDE_ALL -> NotificationCompat.VISIBILITY_SECRET
        }

        val normKey = ContactsLoader.normalizePhone(phoneNumber).ifBlank { senderTitle.hashCode().toString() }
        val shortcutId = "mengitex_chat_$normKey"
        val notificationId = shortcutId.hashCode()

        val senderAvatarBitmap = createSenderPopBitmap(displayTitle, normKey, isProtectedTxn)
        val personIcon = IconCompat.createWithBitmap(senderAvatarBitmap)

        val person = Person.Builder()
            .setName(displayTitle)
            .setKey(shortcutId)
            .setIcon(personIcon)
            .setImportant(true)
            .build()

        // Push long-lived dynamic shortcut required by Android 11+ Bubble API
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE, phoneNumber)
            putExtra(SmsStealthReceiver.EXTRA_OPEN_NAME, senderTitle)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(displayTitle.take(24))
            .setLongLived(true)
            .setPerson(person)
            .setIcon(personIcon)
            .setIntent(shortcutIntent)
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE, phoneNumber)
            putExtra(SmsStealthReceiver.EXTRA_OPEN_NAME, senderTitle)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subTextLabel = when {
            isProtectedTxn -> if (isSw) "Muamala / OTP" else "Transaction / OTP"
            isSecretBurn -> if (isSw) "Ujumbe wa Siri" else "Secret SMS"
            ContactsLoader.isAlphanumericSender(phoneNumber) -> "SMS"
            else -> phoneNumber
        }

        val accentColor = settings.notifLedColorArgb.toInt()
        val compatPriority = when {
            isVip -> NotificationCompat.PRIORITY_HIGH
            !settings.notifHeadsUpEnabled || !settings.notifSoundEnabled -> NotificationCompat.PRIORITY_LOW
            settings.notifPriorityLevel == com.example.security.NotificationPriorityLevel.HIGH -> NotificationCompat.PRIORITY_HIGH
            settings.notifPriorityLevel == com.example.security.NotificationPriorityLevel.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_pop_message)
            .setLargeIcon(senderAvatarBitmap)
            .setColor(accentColor)
            .setContentTitle(displayTitle)
            .setContentText(displayBody)
            .setSubText(if (isVip) "VIP • $subTextLabel" else subTextLabel)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(lockVisibility)
            .setPriority(compatPriority)
            .setOnlyAlertOnce(settings.notifOnlyAlertOnce)
            .setShortcutId(shortcutId)
            .addPerson(person)
            .setStyle(
                NotificationCompat.MessagingStyle(person)
                    .addMessage(displayBody, System.currentTimeMillis(), person)
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (settings.notifGroupBySender) {
            builder.setGroup("mengitex_group_$normKey")
        }

        // Apply custom sound / vibration / LED for legacy & heads-up behavior
        if (!settings.notifSoundEnabled && !isVip) {
            builder.setSilent(true)
        } else {
            val soundUri = settings.notifRingtoneUri.takeIf { it.isNotBlank() }
                ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
            settings.notifVibratePattern.patternMillis()?.let { builder.setVibrate(it) }
            if (settings.notifLedEnabled) {
                builder.setLights(accentColor, 600, 1200)
            }
        }

        // Action 1: Direct Inline Pop Reply
        if (settings.notifActionPopReplyEnabled) {
            val remoteInput = RemoteInput.Builder(SmsPopReplyReceiver.KEY_TEXT_REPLY)
                .setLabel(if (isSw) "Andika jibu la haraka..." else "Type quick reply...")
                .build()
            val replyIntent = Intent(context, SmsPopReplyReceiver::class.java).apply {
                action = SmsPopReplyReceiver.ACTION_POP_REPLY
                putExtra(SmsPopReplyReceiver.EXTRA_SENDER_NAME, senderTitle)
                putExtra(SmsPopReplyReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(SmsPopReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val replyPendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 303,
                replyIntent,
                replyPendingFlags
            )
            val popReplyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_pop_message,
                "Pop Reply",
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .build()
            builder.addAction(popReplyAction)
        }

        // Action 2: Mark as Read
        if (settings.notifActionMarkReadEnabled) {
            val markReadIntent = Intent(context, SmsPopReplyReceiver::class.java).apply {
                action = SmsPopReplyReceiver.ACTION_MARK_READ
                putExtra(SmsPopReplyReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(SmsPopReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 404,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val markReadAction = NotificationCompat.Action.Builder(
                0,
                if (isSw) "Weka Imesomwa" else "Mark Read",
                markReadPendingIntent
            )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build()
            builder.addAction(markReadAction)
        }

        // Action 3: One-Tap Copy OTP (when verification code detected)
        val detectedOtp = if (settings.notifActionCopyOtpEnabled && !isSecretBurn) extractOtpCode(plainBody) else null
        if (detectedOtp != null) {
            val otpIntent = Intent(context, SmsPopReplyReceiver::class.java).apply {
                action = SmsPopReplyReceiver.ACTION_COPY_OTP
                putExtra(SmsPopReplyReceiver.EXTRA_OTP_CODE, detectedOtp)
                putExtra(SmsPopReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val otpPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 555,
                otpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val copyOtpAction = NotificationCompat.Action.Builder(
                0,
                if (isSw) "Nakili $detectedOtp" else "Copy $detectedOtp",
                otpPendingIntent
            )
                .setShowsUserInterface(false)
                .build()
            builder.addAction(copyOtpAction)
        }

        // Action 4: Call Sender (if enabled and sender is a dialable phone number)
        if (settings.notifActionCallEnabled && phoneNumber.any { it.isDigit() } && !ContactsLoader.isAlphanumericSender(phoneNumber)) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val dialPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 606,
                dialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    if (isSw) "Piga" else "Call",
                    dialPendingIntent
                ).build()
            )
        }

        // Action 5: Mute Sender
        if (settings.notifActionMuteEnabled && !isProtectedTxn && !isVip) {
            val muteIntent = Intent(context, SmsPopReplyReceiver::class.java).apply {
                action = SmsPopReplyReceiver.ACTION_MUTE_SENDER
                putExtra(SmsPopReplyReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(SmsPopReplyReceiver.EXTRA_SENDER_NAME, senderTitle)
                putExtra(SmsPopReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val mutePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 505,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val muteAction = NotificationCompat.Action.Builder(
                0,
                if (isSw) "Nyamazisha" else "Mute",
                mutePendingIntent
            )
                .setShowsUserInterface(false)
                .build()
            builder.addAction(muteAction)
        }

        // Action 6: Delete / Dismiss
        if (settings.notifActionDeleteEnabled) {
            val delIntent = Intent(context, SmsPopReplyReceiver::class.java).apply {
                action = SmsPopReplyReceiver.ACTION_DELETE_LATEST
                putExtra(SmsPopReplyReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(SmsPopReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val delPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 666,
                delIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    if (isSw) "Futa" else "Dismiss",
                    delPendingIntent
                )
                    .setShowsUserInterface(false)
                    .build()
            )
        }

        // Android Floating Bubble support
        if (settings.bubbleNotificationsEnabled && channelId != CHANNEL_SILENT_ALERTS) {
            val bubbleIntent = Intent(context, BubbleChatActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(BubbleChatActivity.EXTRA_SENDER_NAME, senderTitle)
                putExtra(BubbleChatActivity.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(BubbleChatActivity.EXTRA_LAST_MESSAGE, displayBody)
            }
            val bubbleFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val bubblePendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 777,
                bubbleIntent,
                bubbleFlag
            )
            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(bubblePendingIntent, personIcon)
                .setDesiredHeight(540)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
            builder.setBubbleMetadata(bubbleMetadata)
        }

        nm.notify(notificationId, builder.build())
        return true
    }

    /**
     * Posts a clean SMS Delivery Report confirmation notification when enabled.
     */
    fun postDeliveryReportNotification(
        context: Context,
        recipientName: String,
        phoneNumber: String,
        statusNote: String,
        settings: SecuritySettings
    ) {
        if (!hasNotificationPermission(context)) return

        ensureAllChannelsCreated(context, settings)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val isSw = settings.language == AppLanguage.SW

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (phoneNumber.isNotBlank()) {
                putExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE, phoneNumber)
                putExtra(SmsStealthReceiver.EXTRA_OPEN_NAME, recipientName)
            }
        }
        val notifId = ("delivery_$phoneNumber").hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSw) "SMS Imefika ✓✓ • $recipientName" else "SMS Delivered ✓✓ • $recipientName"
        val notification = NotificationCompat.Builder(context, CHANNEL_DELIVERY_REPORTS)
            .setSmallIcon(R.drawable.ic_pop_message)
            .setColor(0xFF10B981.toInt())
            .setContentTitle(title)
            .setContentText(statusNote)
            .setAutoCancel(true)
            .setTimeoutAfter(8000L)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(notifId, notification)
    }

    /**
     * Sends a real live Test Notification using all active Notification Manager preferences.
     */
    fun postTestNotification(context: Context, settings: SecuritySettings): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureAllChannelsCreated(context, settings)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val isSw = settings.language == AppLanguage.SW

        val previewTitle = when (settings.notifPreviewMode) {
            NotificationPreviewMode.HIDE_ALL -> "MengiTex"
            else -> if (isSw) "Jaribio la MengiTex" else "MengiTex Test Alert"
        }
        val previewText = when (settings.notifPreviewMode) {
            NotificationPreviewMode.SHOW_ALL -> if (isSw) {
                "Arifa za mfumo wa MengiTex zinafanya kazi kikamilifu (Jibu la haraka, Mlio na Ulinzi viko tayari)."
            } else {
                "Your Notification Manager is working! (Pop Reply, Sound & Privacy active)"
            }
            NotificationPreviewMode.SENDER_ONLY -> if (isSw) {
                "Ujumbe mpya uliosimbwa — fungua MengiTex kusoma"
            } else {
                "New encrypted message — open MengiTex to read"
            }
            NotificationPreviewMode.HIDE_ALL -> if (isSw) {
                "Una ujumbe mpya wa siri"
            } else {
                "You have a new private message"
            }
        }

        val channelId = if (settings.notifHeadsUpEnabled && settings.notifSoundEnabled && settings.notifPriorityLevel != com.example.security.NotificationPriorityLevel.LOW) {
            CHANNEL_BUBBLE_SMS
        } else {
            CHANNEL_SILENT_ALERTS
        }

        val avatar = createSenderPopBitmap("MT", "mengitex_test", isProtectedTxn = false)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val testPriority = when {
            !settings.notifHeadsUpEnabled || !settings.notifSoundEnabled -> NotificationCompat.PRIORITY_LOW
            settings.notifPriorityLevel == com.example.security.NotificationPriorityLevel.HIGH -> NotificationCompat.PRIORITY_HIGH
            settings.notifPriorityLevel == com.example.security.NotificationPriorityLevel.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_pop_message)
            .setLargeIcon(avatar)
            .setColor(settings.notifLedColorArgb.toInt())
            .setContentTitle(previewTitle)
            .setContentText(previewText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(previewText))
            .setPriority(testPriority)
            .setOnlyAlertOnce(settings.notifOnlyAlertOnce)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (!settings.notifSoundEnabled) {
            builder.setSilent(true)
        } else {
            val soundUri = settings.notifRingtoneUri.takeIf { it.isNotBlank() }
                ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
            settings.notifVibratePattern.patternMillis()?.let { pattern ->
                builder.setVibrate(pattern)
                previewVibration(context, settings.notifVibratePattern)
            }
        }

        nm.notify(TEST_NOTIFICATION_ID, builder.build())
        return true
    }

    /**
     * Triggers a subtle local haptic vibration preview when the user selects a vibration pattern.
     */
    fun previewVibration(context: Context, pattern: NotificationVibratePattern) {
        val timings = pattern.patternMillis() ?: return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }

    fun openSystemAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSystemChannelSettings(context: Context, channelId: String, settings: SecuritySettings) {
        ensureAllChannelsCreated(context, settings)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSystemBubbleSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun resolveRingtoneTitle(context: Context, uri: Uri?, isEnglish: Boolean): String {
        if (uri == null) return if (isEnglish) "Default System Tone" else "Mlio wa Kawaida wa Simu"
        return runCatching {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: if (isEnglish) "Custom Tone" else "Mlio Maalum"
    }

    private fun createSenderPopBitmap(senderName: String, key: String, isProtectedTxn: Boolean): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val palettes = if (isProtectedTxn) {
            listOf(intArrayOf(0xFF059669.toInt(), 0xFF0D9488.toInt()))
        } else {
            listOf(
                intArrayOf(0xFF2563EB.toInt(), 0xFF7C3AED.toInt()),
                intArrayOf(0xFF4F46E5.toInt(), 0xFF0EA5E9.toInt()),
                intArrayOf(0xFF7C3AED.toInt(), 0xFFDB2777.toInt()),
                intArrayOf(0xFF0284C7.toInt(), 0xFF2563EB.toInt()),
                intArrayOf(0xFF0D9488.toInt(), 0xFF2563EB.toInt())
            )
        }
        val colors = palettes[(key.hashCode() and 0x7FFFFFFF) % palettes.size]

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                colors[0], colors[1],
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, bgPaint)

        val words = senderName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val initials = when {
            words.isEmpty() -> "M"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, size / 2f, textY, textPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isProtectedTxn) 0xFF34D399.toInt() else 0xFF10B981.toInt()
        }
        canvas.drawCircle(size - 24f, size - 24f, 18f, ringPaint)
        canvas.drawCircle(size - 24f, size - 24f, 13f, dotPaint)

        return bmp
    }
}
