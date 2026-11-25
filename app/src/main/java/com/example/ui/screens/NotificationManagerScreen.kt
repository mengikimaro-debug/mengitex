package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedSmsMessage
import com.example.security.NotificationPreset
import com.example.security.NotificationPreviewMode
import com.example.security.NotificationPriorityLevel
import com.example.security.NotificationVibratePattern
import com.example.security.SecuritySettings
import com.example.security.SmsAdBlockerEngine
import com.example.service.MengiTexNotificationManager
import com.example.ui.components.NextGenGlassSurface
import com.example.ui.components.StaggeredEntrance
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

/**
 * Full-featured Notification Manager for MengiTex.
 * Provides complete control over Master Toggle, Permissions, Test & Clear All alerts,
 * Categories/Channels, Custom Ringtone Picker, Vibration Patterns, Heads-Up/LED/Badges,
 * Lock-Screen Privacy Preview with live preview card, Quick Notification Actions,
 * Quiet Hours (Do Not Disturb), Muted Senders Manager, and Android System Channels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagerScreen(
    settings: SecuritySettings,
    hasNotificationPermission: Boolean,
    activeNotificationCount: Int,
    recentMessages: List<DecryptedSmsMessage>,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onMasterToggle: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onClearAllNotifications: () -> Unit,
    onPersonalSmsToggle: (Boolean) -> Unit,
    onTransactionsOtpToggle: (Boolean) -> Unit,
    onBubbleNotificationsToggle: (Boolean) -> Unit,
    onInAppPopBannerToggle: (Boolean) -> Unit,
    onDeliveryReportsNotifToggle: (Boolean) -> Unit,
    onSilentUnknownToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onPickRingtone: () -> Unit,
    onSelectVibratePattern: (NotificationVibratePattern) -> Unit,
    onHeadsUpToggle: (Boolean) -> Unit,
    onLedToggle: (Boolean) -> Unit,
    onBadgeToggle: (Boolean) -> Unit,
    onSelectPreviewMode: (NotificationPreviewMode) -> Unit,
    onActionPopReplyToggle: (Boolean) -> Unit,
    onActionMarkReadToggle: (Boolean) -> Unit,
    onActionMuteToggle: (Boolean) -> Unit,
    onQuietHoursToggle: (Boolean) -> Unit,
    onQuietHoursRangeChange: (Int, Int) -> Unit,
    onQuietHoursAllowTxnToggle: (Boolean) -> Unit,
    onMuteSender: (String) -> Unit,
    onUnmuteSender: (String) -> Unit,
    onUnmuteAllSenders: () -> Unit,
    onOpenSystemAppNotifs: () -> Unit,
    onOpenSystemBubbles: () -> Unit,
    onOpenSystemChannel: (String) -> Unit,
    onApplyPreset: (NotificationPreset) -> Unit = {},
    onSecretVaultToggle: (Boolean) -> Unit = {},
    onScheduledSmsToggle: (Boolean) -> Unit = {},
    onBlockedSpamToggle: (Boolean) -> Unit = {},
    onWakeScreenToggle: (Boolean) -> Unit = {},
    onResetRingtoneDefault: () -> Unit = {},
    onTestVibration: () -> Unit = {},
    onSelectLedColor: (Long) -> Unit = {},
    onSelectPriorityLevel: (NotificationPriorityLevel) -> Unit = {},
    onOnlyAlertOnceToggle: (Boolean) -> Unit = {},
    onGroupBySenderToggle: (Boolean) -> Unit = {},
    onRepeatSettingsChange: (Int, Int) -> Unit = { _, _ -> },
    onInAppPopDurationChange: (Int) -> Unit = {},
    onActionCopyOtpToggle: (Boolean) -> Unit = {},
    onActionCallToggle: (Boolean) -> Unit = {},
    onActionDeleteToggle: (Boolean) -> Unit = {},
    onQuietHoursAllowVipToggle: (Boolean) -> Unit = {},
    onAddVipSender: (String) -> Unit = {},
    onRemoveVipSender: (String) -> Unit = {},
    onClearAllVipSenders: () -> Unit = {},
    onResetAllSettings: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val s = LocalAppStrings.current
    val isSw = settings.language == AppLanguage.SW
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val successGreen = if (isDark) Color(0xFF6B9E85) else Color(0xFF2F6B55)
    val warnAmber = if (isDark) Color(0xFFC99A4E) else Color(0xFFA16207)
    val inQuietNow = remember(
        settings.quietHoursEnabled,
        settings.quietHoursStartHour,
        settings.quietHoursEndHour
    ) { MengiTexNotificationManager.isCurrentlyInQuietHours(settings) }

    var categoriesExpanded by remember { mutableStateOf(true) }
    var soundStyleExpanded by remember { mutableStateOf(false) }
    var remindersExpanded by remember { mutableStateOf(false) }
    var privacyExpanded by remember { mutableStateOf(false) }
    var actionsExpanded by remember { mutableStateOf(false) }
    var quietExpanded by remember { mutableStateOf(settings.quietHoursEnabled) }
    var vipExpanded by remember { mutableStateOf(settings.vipNotificationSenders.isNotEmpty()) }
    var mutedExpanded by remember { mutableStateOf(settings.mutedNotificationSenders.isNotEmpty()) }
    var systemExpanded by remember { mutableStateOf(false) }
    var muteInput by remember { mutableStateOf("") }

    // Distinct recent senders available for 1-tap quick mute
    val recentSenders = remember(recentMessages, settings.mutedNotificationSenders) {
        val seen = LinkedHashSet<String>()
        recentMessages
            .sortedByDescending { it.timestamp }
            .mapNotNull { msg ->
                val raw = msg.phoneNumber.ifBlank { msg.contactName }.trim()
                val key = SmsAdBlockerEngine.canonicalSenderKey(raw)
                if (key.isNotEmpty() && key !in settings.mutedNotificationSenders && seen.add(key)) {
                    msg.contactName.ifBlank { raw } to raw
                } else {
                    null
                }
            }
            .take(8)
    }

    val previewBadge = when (settings.notifPreviewMode) {
        NotificationPreviewMode.SHOW_ALL -> s.notifMgrPreviewAll
        NotificationPreviewMode.SENDER_ONLY -> s.notifMgrPreviewSender
        NotificationPreviewMode.HIDE_ALL -> s.notifMgrPreviewHide
    }
    val quietBadge = if (settings.quietHoursEnabled) {
        "${formatHour(settings.quietHoursStartHour)} – ${formatHour(settings.quietHoursEndHour)}"
    } else {
        s.settingsOffShort
    }
    val soundBadge = if (settings.notifSoundEnabled) {
        settings.notifRingtoneName.ifBlank { s.notifMgrRingtoneDefault }
    } else {
        s.settingsOffShort
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.notifMgrTitle,
            subtitle = s.notifMgrSubtitle,
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "notif_mgr_back_button",
            onBack = onBack,
            trailingContent = {
                val pillColor = when {
                    !settings.notificationsMasterEnabled -> MaterialTheme.colorScheme.error
                    inQuietNow -> warnAmber
                    else -> successGreen
                }
                val pillText = when {
                    !settings.notificationsMasterEnabled -> s.settingsOffShort
                    inQuietNow -> "DND"
                    else -> s.settingsActiveShort
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = pillColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("notif_manager_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Master Control & System Status Card
            item {
                StaggeredEntrance(index = 0) {
                    NextGenGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = true,
                        accentColor = if (settings.notificationsMasterEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NotifIconOrb(
                                    icon = if (settings.notificationsMasterEnabled) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    accent = if (settings.notificationsMasterEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    size = 44.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.notifMgrMasterTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = s.notifMgrMasterSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.notificationsMasterEnabled,
                                    onCheckedChange = onMasterToggle,
                                    modifier = Modifier.testTag("notif_mgr_master_switch")
                                )
                            }

                            // Permission + Active Status Row
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (hasNotificationPermission) {
                                    successGreen.copy(alpha = 0.12f)
                                } else {
                                    warnAmber.copy(alpha = 0.14f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasNotificationPermission) {
                                            Icons.Filled.CheckCircle
                                        } else {
                                            Icons.Filled.Warning
                                        },
                                        contentDescription = null,
                                        tint = if (hasNotificationPermission) successGreen else warnAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (hasNotificationPermission) {
                                                s.notifMgrPermissionOk
                                            } else {
                                                s.notifMgrPermissionMissing
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasNotificationPermission) successGreen else warnAmber
                                        )
                                        Text(
                                            text = s.notifMgrActiveCount(activeNotificationCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!hasNotificationPermission) {
                                        Button(
                                            onClick = onRequestNotificationPermission,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("notif_mgr_grant_perm_btn")
                                        ) {
                                            Text(s.notifMgrGrantPermBtn, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            // Live Test + Clear All Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onSendTestNotification,
                                    enabled = settings.notificationsMasterEnabled,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("notif_mgr_test_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s.notifMgrTestBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onClearAllNotifications,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("notif_mgr_clear_all_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ClearAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s.notifMgrClearAllBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 1-Tap Quick Notification Profiles (Standard, Silent, Private, Night DND)
                            NotificationQuickProfilesRow(
                                settings = settings,
                                isSw = isSw,
                                onApplyPreset = onApplyPreset
                            )
                        }
                    }
                }
            }

            // 2. Notification Categories & Channels
            item {
                StaggeredEntrance(index = 1) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.Notifications,
                        title = s.notifMgrSectionCategories,
                        subtitle = s.notifMgrSectionCategoriesSub,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = categoriesExpanded,
                        onToggleExpand = { categoriesExpanded = !categoriesExpanded },
                        statusBadge = "${
                            listOf(
                                settings.notifyPersonalSms,
                                settings.notifyTransactionsOtp,
                                settings.bubbleNotificationsEnabled,
                                settings.inAppPopBannerEnabled,
                                settings.notifyDeliveryReports,
                                settings.notifySecretVault,
                                settings.notifyScheduledSms,
                                settings.notifyBlockedSpam,
                                settings.notifWakeScreenEnabled
                            ).count { it }
                        }/9",
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "notif_mgr_categories_header"
                    ) {
                        NotifToggleRow(
                            icon = Icons.Filled.ChatBubble,
                            title = s.notifMgrCatPersonal,
                            subtitle = s.notifMgrCatPersonalSub,
                            checked = settings.notifyPersonalSms,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onPersonalSmsToggle,
                            testTag = "notif_cat_personal"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.AccountBalance,
                            title = s.notifMgrCatTxn,
                            subtitle = s.notifMgrCatTxnSub,
                            checked = settings.notifyTransactionsOtp,
                            enabled = settings.notificationsMasterEnabled,
                            accent = successGreen,
                            onCheckedChange = onTransactionsOtpToggle,
                            testTag = "notif_cat_txn"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.NotificationsActive,
                            title = s.notifMgrCatBubbles,
                            subtitle = s.notifMgrCatBubblesSub,
                            checked = settings.bubbleNotificationsEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onBubbleNotificationsToggle,
                            testTag = "notif_cat_bubbles"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Tune,
                            title = s.notifMgrCatInAppPop,
                            subtitle = s.notifMgrCatInAppPopSub,
                            checked = settings.inAppPopBannerEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onInAppPopBannerToggle,
                            testTag = "notif_cat_in_app_pop"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.DoneAll,
                            title = s.notifMgrCatDelivery,
                            subtitle = s.notifMgrCatDeliverySub,
                            checked = settings.notifyDeliveryReports,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onDeliveryReportsNotifToggle,
                            testTag = "notif_cat_delivery"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Lock,
                            title = if (isSw) "Arifa za Ujumbe wa Siri (Vault)" else "Secret Vault Alerts",
                            subtitle = if (isSw) "Onyesha arifa salama ujumbe wa siri ukiingia" else "Show private shielded alerts for secret messages",
                            checked = settings.notifySecretVault,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onSecretVaultToggle,
                            testTag = "notif_cat_secret_vault"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.ScheduleSend,
                            title = if (isSw) "Arifa za SMS za Ratiba" else "Scheduled SMS Alerts",
                            subtitle = if (isSw) "Taarifa wakati SMS iliyopangwa inapotumwa" else "Notify when a scheduled message is sent",
                            checked = settings.notifyScheduledSms,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onScheduledSmsToggle,
                            testTag = "notif_cat_scheduled"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Block,
                            title = if (isSw) "Muhtasari wa Matangazo Yaliyozuiwa" else "Blocked Ads & Spam Summary",
                            subtitle = if (isSw) "Onyesha arifa kimya matangazo yakizuiwa" else "Show a quiet notice when spam ads are blocked",
                            checked = settings.notifyBlockedSpam,
                            enabled = settings.notificationsMasterEnabled,
                            accent = warnAmber,
                            onCheckedChange = onBlockedSpamToggle,
                            testTag = "notif_cat_blocked_spam"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.LightMode,
                            title = if (isSw) "Washa Skrini SMS Mpya Ikiingia" else "Wake Screen on New SMS",
                            subtitle = if (isSw) "Angaza skrini fupi ujumbe mpya ukiwasili" else "Briefly light up the screen when a message arrives",
                            checked = settings.notifWakeScreenEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onWakeScreenToggle,
                            testTag = "notif_cat_wake_screen"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.PersonOff,
                            title = s.notifMgrCatSilentUnknown,
                            subtitle = s.notifMgrCatSilentUnknownSub,
                            checked = settings.silentUnknownSms,
                            enabled = settings.notificationsMasterEnabled,
                            accent = MaterialTheme.colorScheme.tertiary,
                            onCheckedChange = onSilentUnknownToggle,
                            testTag = "notif_cat_silent_unknown"
                        )
                    }
                }
            }

            // 3. Sound, Custom Ringtone, Vibration Patterns, Heads-Up, LED & Badge
            item {
                StaggeredEntrance(index = 2) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.VolumeUp,
                        title = s.notifMgrSectionSoundStyle,
                        subtitle = s.notifMgrSectionSoundStyleSub,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = soundStyleExpanded,
                        onToggleExpand = { soundStyleExpanded = !soundStyleExpanded },
                        statusBadge = soundBadge.take(18),
                        statusBadgeColor = if (settings.notifSoundEnabled) successGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        headerTestTag = "notif_mgr_sound_header"
                    ) {
                        NotifToggleRow(
                            icon = Icons.Filled.VolumeUp,
                            title = s.notifMgrSoundTitle,
                            subtitle = s.notifMgrSoundSub,
                            checked = settings.notifSoundEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onSoundToggle,
                            testTag = "notif_sound_switch"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real Android Ringtone Picker Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.notifMgrRingtoneLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = settings.notifRingtoneName.ifBlank { s.notifMgrRingtoneDefault },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (settings.notifRingtoneUri.isNotBlank()) {
                                    TextButton(
                                        onClick = onResetRingtoneDefault,
                                        enabled = settings.notificationsMasterEnabled && settings.notifSoundEnabled,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("notif_reset_ringtone_btn")
                                    ) {
                                        Text(
                                            text = if (isSw) "Awali" else "Reset",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = onPickRingtone,
                                    enabled = settings.notificationsMasterEnabled && settings.notifSoundEnabled,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("notif_pick_ringtone_btn")
                                ) {
                                    Text(s.notifMgrRingtonePickBtn, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Vibration Pattern Selector (live haptic preview on tap) + Test Vibrate button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Vibration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = s.notifMgrVibrateTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(
                                onClick = onTestVibration,
                                enabled = settings.notificationsMasterEnabled &&
                                    settings.notifVibratePattern != NotificationVibratePattern.OFF,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("notif_test_vibrate_btn")
                            ) {
                                Text(
                                    text = if (isSw) "Jaribu Mtetemo" else "Test Vibrate",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NotificationVibratePattern.entries.forEach { pattern ->
                                val label = when (pattern) {
                                    NotificationVibratePattern.OFF -> s.notifMgrVibOff
                                    NotificationVibratePattern.SHORT -> s.notifMgrVibShort
                                    NotificationVibratePattern.DOUBLE -> s.notifMgrVibDouble
                                    NotificationVibratePattern.HEARTBEAT -> s.notifMgrVibHeartbeat
                                    NotificationVibratePattern.LONG -> s.notifMgrVibLong
                                }
                                FilterChip(
                                    selected = settings.notifVibratePattern == pattern,
                                    enabled = settings.notificationsMasterEnabled,
                                    onClick = { onSelectVibratePattern(pattern) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("notif_vib_${pattern.name.lowercase()}")
                                )
                            }
                        }

                        NotifDivider()
                        NotificationPrioritySelector(
                            selectedLevel = settings.notifPriorityLevel,
                            enabled = settings.notificationsMasterEnabled,
                            isSw = isSw,
                            onSelectPriority = onSelectPriorityLevel
                        )

                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.NotificationsActive,
                            title = s.notifMgrHeadsUpTitle,
                            subtitle = s.notifMgrHeadsUpSub,
                            checked = settings.notifHeadsUpEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onHeadsUpToggle,
                            testTag = "notif_headsup_switch"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.GraphicEq,
                            title = s.notifMgrLedTitle,
                            subtitle = s.notifMgrLedSub,
                            checked = settings.notifLedEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onLedToggle,
                            testTag = "notif_led_switch"
                        )
                        if (settings.notifLedEnabled) {
                            NotificationLedColorPicker(
                                selectedColorArgb = settings.notifLedColorArgb,
                                enabled = settings.notificationsMasterEnabled,
                                isSw = isSw,
                                onSelectLedColor = onSelectLedColor
                            )
                        }
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Notifications,
                            title = s.notifMgrBadgeTitle,
                            subtitle = s.notifMgrBadgeSub,
                            checked = settings.notifBadgeEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onBadgeToggle,
                            testTag = "notif_badge_switch"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.NotificationsOff,
                            title = if (isSw) "Lia Mara Moja kwa Mazungumzo" else "Alert Once per Chat",
                            subtitle = if (isSw) "Usirudie mlio endapo ujumbe mwingi unafuatana" else "Avoid repeated rings when multiple messages arrive back-to-back",
                            checked = settings.notifOnlyAlertOnce,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onOnlyAlertOnceToggle,
                            testTag = "notif_alert_once_switch"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Forum,
                            title = if (isSw) "Kusanya Arifa kwa Mtumaji" else "Group Notifications by Sender",
                            subtitle = if (isSw) "Panga arifa za mtumaji mmoja kwenye kundi moja" else "Bundle multiple notifications cleanly in the shade",
                            checked = settings.notifGroupBySender,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onGroupBySenderToggle,
                            testTag = "notif_group_sender_switch"
                        )
                    }
                }
            }

            // 3b. Smart Reminders & In-App Pop Timing
            item {
                StaggeredEntrance(index = 3) {
                    NotificationSmartRemindersCard(
                        settings = settings,
                        isSw = isSw,
                        expanded = remindersExpanded,
                        onToggleExpand = { remindersExpanded = !remindersExpanded },
                        onRepeatSettingsChange = onRepeatSettingsChange,
                        onInAppPopDurationChange = onInAppPopDurationChange
                    )
                }
            }

            // 4. Lock-Screen Privacy Preview with Live Mockup
            item {
                StaggeredEntrance(index = 3) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.VisibilityOff,
                        title = s.notifMgrSectionPrivacy,
                        subtitle = s.notifMgrSectionPrivacySub,
                        accent = MaterialTheme.colorScheme.tertiary,
                        expanded = privacyExpanded,
                        onToggleExpand = { privacyExpanded = !privacyExpanded },
                        statusBadge = previewBadge.take(18),
                        statusBadgeColor = MaterialTheme.colorScheme.tertiary,
                        headerTestTag = "notif_mgr_privacy_header"
                    ) {
                        // Live Lock-Screen Preview Card
                        Text(
                            text = s.notifMgrLivePreviewLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val isSw = settings.language == AppLanguage.SW
                        val sampleSender = when (settings.notifPreviewMode) {
                            NotificationPreviewMode.HIDE_ALL -> "MengiTex"
                            else -> "Amina Juma"
                        }
                        val sampleBody = when (settings.notifPreviewMode) {
                            NotificationPreviewMode.SHOW_ALL -> if (isSw) {
                                "Habari! Tukutane saa kumi pale Posta."
                            } else {
                                "Hey! Let's meet at 4 PM downtown."
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
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (settings.notifPreviewMode == NotificationPreviewMode.HIDE_ALL) "M" else "AJ",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = sampleSender,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isSw) "Sasa hivi" else "Now",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = sampleBody,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        NotificationPreviewMode.entries.forEach { mode ->
                            val (title, desc) = when (mode) {
                                NotificationPreviewMode.SHOW_ALL -> s.notifMgrPreviewAll to s.notifMgrPreviewAllSub
                                NotificationPreviewMode.SENDER_ONLY -> s.notifMgrPreviewSender to s.notifMgrPreviewSenderSub
                                NotificationPreviewMode.HIDE_ALL -> s.notifMgrPreviewHide to s.notifMgrPreviewHideSub
                            }
                            val selected = settings.notifPreviewMode == mode
                            Surface(
                                onClick = { onSelectPreviewMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .testTag("notif_preview_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { onSelectPreviewMode(mode) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Quick Notification Actions (Pop Reply, Copy OTP, Mark Read, Call, Mute, Delete)
            item {
                StaggeredEntrance(index = 4) {
                    NotifCollapsibleCard(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = s.notifMgrSectionActions,
                        subtitle = s.notifMgrSectionActionsSub,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = actionsExpanded,
                        onToggleExpand = { actionsExpanded = !actionsExpanded },
                        statusBadge = "${
                            listOf(
                                settings.notifActionPopReplyEnabled,
                                settings.notifActionCopyOtpEnabled,
                                settings.notifActionMarkReadEnabled,
                                settings.notifActionCallEnabled,
                                settings.notifActionMuteEnabled,
                                settings.notifActionDeleteEnabled
                            ).count { it }
                        }/6",
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "notif_mgr_actions_header"
                    ) {
                        NotifToggleRow(
                            icon = Icons.AutoMirrored.Filled.Reply,
                            title = s.notifMgrActionPopReply,
                            subtitle = s.notifMgrActionPopReplySub,
                            checked = settings.notifActionPopReplyEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onActionPopReplyToggle,
                            testTag = "notif_action_pop_reply"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.ContentCopy,
                            title = if (isSw) "Nakili OTP (Bonyeza 1)" else "One-Tap Copy OTP",
                            subtitle = if (isSw) "Weka kitufe cha kunakili namba ya OTP moja kwa moja" else "Show a 1-tap Copy Code button on verification SMS",
                            checked = settings.notifActionCopyOtpEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            accent = successGreen,
                            onCheckedChange = onActionCopyOtpToggle,
                            testTag = "notif_action_copy_otp"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.DoneAll,
                            title = s.notifMgrActionMarkRead,
                            subtitle = s.notifMgrActionMarkReadSub,
                            checked = settings.notifActionMarkReadEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onActionMarkReadToggle,
                            testTag = "notif_action_mark_read"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Call,
                            title = if (isSw) "Piga Simu Kutoka Arifa" else "Call Sender Button",
                            subtitle = if (isSw) "Ongeza kitufe cha kupiga simu moja kwa moja" else "Quick button to open dialer for the sender",
                            checked = settings.notifActionCallEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onActionCallToggle,
                            testTag = "notif_action_call"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.NotificationsOff,
                            title = s.notifMgrActionMute,
                            subtitle = s.notifMgrActionMuteSub,
                            checked = settings.notifActionMuteEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            onCheckedChange = onActionMuteToggle,
                            testTag = "notif_action_mute"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Delete,
                            title = if (isSw) "Futa / Ondoa Arifa" else "Dismiss / Delete Action",
                            subtitle = if (isSw) "Kitufe cha kuondoa na kuweka kama umesomwa haraka" else "Quick button to dismiss and clear alert immediately",
                            checked = settings.notifActionDeleteEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            accent = MaterialTheme.colorScheme.error,
                            onCheckedChange = onActionDeleteToggle,
                            testTag = "notif_action_delete"
                        )
                    }
                }
            }

            // 6. Quiet Hours (Do Not Disturb)
            item {
                StaggeredEntrance(index = 5) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.Bedtime,
                        title = s.notifMgrSectionQuiet,
                        subtitle = s.notifMgrSectionQuietSub,
                        accent = warnAmber,
                        expanded = quietExpanded,
                        onToggleExpand = { quietExpanded = !quietExpanded },
                        statusBadge = if (inQuietNow) s.notifMgrQuietActiveNow else quietBadge,
                        statusBadgeColor = if (settings.quietHoursEnabled) warnAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        headerTestTag = "notif_mgr_quiet_header"
                    ) {
                        NotifToggleRow(
                            icon = Icons.Filled.Bedtime,
                            title = s.notifMgrQuietEnable,
                            subtitle = s.notifMgrQuietEnableSub,
                            checked = settings.quietHoursEnabled,
                            enabled = settings.notificationsMasterEnabled,
                            accent = warnAmber,
                            onCheckedChange = onQuietHoursToggle,
                            testTag = "notif_quiet_switch"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HourStepperBox(
                                label = s.notifMgrQuietStart,
                                hour = settings.quietHoursStartHour,
                                enabled = settings.quietHoursEnabled,
                                onDecrease = {
                                    val next = (settings.quietHoursStartHour + 23) % 24
                                    onQuietHoursRangeChange(next, settings.quietHoursEndHour)
                                },
                                onIncrease = {
                                    val next = (settings.quietHoursStartHour + 1) % 24
                                    onQuietHoursRangeChange(next, settings.quietHoursEndHour)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HourStepperBox(
                                label = s.notifMgrQuietEnd,
                                hour = settings.quietHoursEndHour,
                                enabled = settings.quietHoursEnabled,
                                onDecrease = {
                                    val next = (settings.quietHoursEndHour + 23) % 24
                                    onQuietHoursRangeChange(settings.quietHoursStartHour, next)
                                },
                                onIncrease = {
                                    val next = (settings.quietHoursEndHour + 1) % 24
                                    onQuietHoursRangeChange(settings.quietHoursStartHour, next)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.AccountBalance,
                            title = s.notifMgrQuietAllowTxn,
                            subtitle = s.notifMgrQuietAllowTxnSub,
                            checked = settings.quietHoursAllowTransactions,
                            enabled = settings.quietHoursEnabled,
                            accent = successGreen,
                            onCheckedChange = onQuietHoursAllowTxnToggle,
                            testTag = "notif_quiet_allow_txn"
                        )
                        NotifDivider()
                        NotifToggleRow(
                            icon = Icons.Filled.Star,
                            title = if (isSw) "Ruhusu Watumaji wa VIP Wakati wa DND" else "Allow VIP Senders during DND",
                            subtitle = if (isSw) "Namba za VIP zitaendelea kuita hata saa za kimya" else "Priority VIP contacts break through Quiet Hours",
                            checked = settings.quietHoursAllowVip,
                            enabled = settings.quietHoursEnabled,
                            accent = warnAmber,
                            onCheckedChange = onQuietHoursAllowVipToggle,
                            testTag = "notif_quiet_allow_vip"
                        )
                    }
                }
            }

            // 6b. VIP Priority Senders Manager
            item {
                StaggeredEntrance(index = 6) {
                    NotificationVipSendersCard(
                        settings = settings,
                        recentMessages = recentMessages,
                        isSw = isSw,
                        accentColor = warnAmber,
                        expanded = vipExpanded,
                        onToggleExpand = { vipExpanded = !vipExpanded },
                        onAddVipSender = onAddVipSender,
                        onRemoveVipSender = onRemoveVipSender,
                        onClearAllVipSenders = onClearAllVipSenders
                    )
                }
            }

            // 7. Muted Senders Manager
            item {
                StaggeredEntrance(index = 6) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.NotificationsOff,
                        title = s.notifMgrSectionMuted,
                        subtitle = s.notifMgrSectionMutedSub,
                        accent = MaterialTheme.colorScheme.error,
                        expanded = mutedExpanded,
                        onToggleExpand = { mutedExpanded = !mutedExpanded },
                        statusBadge = settings.mutedNotificationSenders.size.toString(),
                        statusBadgeColor = if (settings.mutedNotificationSenders.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        headerTestTag = "notif_mgr_muted_header"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = muteInput,
                                onValueChange = { muteInput = it },
                                placeholder = { Text(s.notifMgrMuteInputHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("notif_mute_input")
                            )
                            Button(
                                onClick = {
                                    if (muteInput.isNotBlank()) {
                                        onMuteSender(muteInput.trim())
                                        muteInput = ""
                                    }
                                },
                                enabled = muteInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("notif_mute_add_btn")
                            ) {
                                Text(s.notifMgrMuteAddBtn)
                            }
                        }

                        if (recentSenders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = s.notifMgrMuteFromChats,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSenders.forEach { (display, addr) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onMuteSender(addr) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.NotificationsOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = display.take(18),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (settings.mutedNotificationSenders.isEmpty()) {
                            Text(
                                text = s.notifMgrMuteEmpty,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                settings.mutedNotificationSenders.sorted().forEach { senderKey ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.NotificationsOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = senderKey,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            TextButton(
                                                onClick = { onUnmuteSender(senderKey) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(s.notifMgrUnmuteBtn)
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = onUnmuteAllSenders,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notif_unmute_all_btn")
                                ) {
                                    Text(s.notifMgrUnmuteAllBtn)
                                }
                            }
                        }
                    }
                }
            }

            // 8. Android Native Channels & System Controls
            item {
                StaggeredEntrance(index = 7) {
                    NotifCollapsibleCard(
                        icon = Icons.Filled.Tune,
                        title = s.notifMgrSectionSystem,
                        subtitle = s.notifMgrSectionSystemSub,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = systemExpanded,
                        onToggleExpand = { systemExpanded = !systemExpanded },
                        statusBadge = "Android",
                        statusBadgeColor = MaterialTheme.colorScheme.secondary,
                        headerTestTag = "notif_mgr_system_header"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemChannelButton(
                                label = s.notifMgrOpenAppNotifs,
                                icon = Icons.Filled.Notifications,
                                onClick = onOpenSystemAppNotifs,
                                testTag = "notif_sys_app_settings"
                            )
                            SystemChannelButton(
                                label = s.notifMgrOpenBubbles,
                                icon = Icons.Filled.ChatBubble,
                                onClick = onOpenSystemBubbles,
                                testTag = "notif_sys_bubbles"
                            )
                            SystemChannelButton(
                                label = s.notifMgrChannelPersonal,
                                icon = Icons.Filled.NotificationsActive,
                                onClick = { onOpenSystemChannel(MengiTexNotificationManager.CHANNEL_BUBBLE_SMS) },
                                testTag = "notif_sys_ch_personal"
                            )
                            SystemChannelButton(
                                label = s.notifMgrChannelTxn,
                                icon = Icons.Filled.AccountBalance,
                                onClick = { onOpenSystemChannel(MengiTexNotificationManager.CHANNEL_TRANSACTIONS_OTP) },
                                testTag = "notif_sys_ch_txn"
                            )
                            SystemChannelButton(
                                label = s.notifMgrChannelSecret,
                                icon = Icons.Filled.Lock,
                                onClick = { onOpenSystemChannel(MengiTexNotificationManager.CHANNEL_SECRET_VAULT) },
                                testTag = "notif_sys_ch_secret"
                            )
                            SystemChannelButton(
                                label = s.notifMgrChannelDelivery,
                                icon = Icons.Filled.DoneAll,
                                onClick = { onOpenSystemChannel(MengiTexNotificationManager.CHANNEL_DELIVERY_REPORTS) },
                                testTag = "notif_sys_ch_delivery"
                            )
                            SystemChannelButton(
                                label = if (isSw) "Kituo cha Arifa Kimya (Silent)" else "Silent Tray Channel",
                                icon = Icons.Filled.NotificationsOff,
                                onClick = { onOpenSystemChannel(MengiTexNotificationManager.CHANNEL_SILENT_ALERTS) },
                                testTag = "notif_sys_ch_silent"
                            )
                            NotifDivider()
                            OutlinedButton(
                                onClick = onResetAllSettings,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notif_reset_all_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSw) "Rudisha Mipangilio Yote ya Arifa (Reset)" else "Reset All Notification Settings",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
internal fun NotifCollapsibleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    statusBadge: String,
    statusBadgeColor: Color,
    headerTestTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    NextGenGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        highlighted = expanded,
        accentColor = accent
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag(headerTestTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotifIconOrb(icon = icon, accent = accent, size = 38.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBadgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusBadgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
internal fun NotifToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    accent: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled && checked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
internal fun NotifDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    )
}

@Composable
private fun HourStepperBox(
    label: String,
    hour: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease hour", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = formatHour(hour),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onIncrease,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase hour", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SystemChannelButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
    }
}

private fun formatHour(hour: Int): String = String.format(Locale.US, "%02d:00", hour.coerceIn(0, 23))

@Composable
private fun NotifIconOrb(
    icon: ImageVector,
    accent: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
