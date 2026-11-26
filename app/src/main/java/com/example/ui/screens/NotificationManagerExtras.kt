package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedSmsMessage
import com.example.security.NotificationPreset
import com.example.security.NotificationPreviewMode
import com.example.security.NotificationPriorityLevel
import com.example.security.SecuritySettings
import com.example.security.SmsAdBlockerEngine

/**
 * Quick 1-Tap Notification Profile selector inside the Hero Master Card.
 */
@Composable
internal fun NotificationQuickProfilesRow(
    settings: SecuritySettings,
    isSw: Boolean,
    onApplyPreset: (NotificationPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (isSw) "Profaili za Haraka (Bonyeza 1)" else "Quick Profiles (1-Tap)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val profiles = listOf(
                Triple(
                    NotificationPreset.STANDARD,
                    if (isSw) "Kawaida" else "Standard",
                    Icons.Filled.NotificationsActive
                ),
                Triple(
                    NotificationPreset.SILENT,
                    if (isSw) "Kimya" else "Silent",
                    Icons.Filled.NotificationsOff
                ),
                Triple(
                    NotificationPreset.PRIVATE,
                    if (isSw) "Faragha" else "Private",
                    Icons.Filled.Shield
                ),
                Triple(
                    NotificationPreset.NIGHT_DND,
                    if (isSw) "Usiku DND" else "Night DND",
                    Icons.Filled.Bedtime
                )
            )
            profiles.forEach { (preset, label, icon) ->
                val isSelected = when (preset) {
                    NotificationPreset.STANDARD ->
                        settings.notificationsMasterEnabled &&
                            settings.notifSoundEnabled &&
                            !settings.quietHoursEnabled &&
                            settings.notifPreviewMode == NotificationPreviewMode.SHOW_ALL
                    NotificationPreset.SILENT ->
                        settings.notificationsMasterEnabled &&
                            !settings.notifSoundEnabled &&
                            !settings.notifHeadsUpEnabled
                    NotificationPreset.PRIVATE ->
                        settings.notificationsMasterEnabled &&
                            settings.notifPreviewMode == NotificationPreviewMode.HIDE_ALL
                    NotificationPreset.NIGHT_DND ->
                        settings.notificationsMasterEnabled &&
                            settings.quietHoursEnabled
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onApplyPreset(preset) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("notif_preset_${preset.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Calm matte LED indicator color swatch picker.
 */
@Composable
internal fun NotificationLedColorPicker(
    selectedColorArgb: Long,
    enabled: Boolean,
    isSw: Boolean,
    onSelectLedColor: (Long) -> Unit
) {
    val swatches = remember(isSw) {
        listOf(
            0xFF33496EL to (if (isSw) "Bluu Tulivu" else "Slate Navy"),
            0xFF4F5D75L to (if (isSw) "Kijivu Tulivu" else "Slate Grey"),
            0xFF7D5342L to (if (isSw) "Udongo Tulivu" else "Muted Terracotta"),
            0xFF8C3B38L to (if (isSw) "Nyekundu Tulivu" else "Dusty Crimson"),
            0xFFD6D3CBL to (if (isSw) "Off-White" else "Off-White")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isSw) "Rangi ya Mwanga wa LED" else "LED Pulse Color",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatches.forEach { (argb, name) ->
                val swatchColor = Color(argb)
                val isSelected = selectedColorArgb == argb
                val checkTint = if (swatchColor.luminance() > 0.55f) Color(0xFF1F2421) else Color(0xFFFAF8F4)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled) { onSelectLedColor(argb) }
                        .semantics { contentDescription = name }
                        .testTag("notif_led_color_${argb.toString(16)}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (enabled) swatchColor else swatchColor.copy(alpha = 0.4f))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Notification Priority / Importance level chips.
 */
@Composable
internal fun NotificationPrioritySelector(
    selectedLevel: NotificationPriorityLevel,
    enabled: Boolean,
    isSw: Boolean,
    onSelectPriority: (NotificationPriorityLevel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PriorityHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSw) "Kipaumbele cha Arifa (Importance)" else "Notification Priority",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationPriorityLevel.entries.forEach { level ->
                val label = when (level) {
                    NotificationPriorityLevel.HIGH -> if (isSw) "Juu (Haraka)" else "High (Urgent)"
                    NotificationPriorityLevel.DEFAULT -> if (isSw) "Kawaida" else "Default"
                    NotificationPriorityLevel.LOW -> if (isSw) "Chini (Kimya)" else "Low (Quiet)"
                }
                FilterChip(
                    selected = selectedLevel == level,
                    enabled = enabled,
                    onClick = { onSelectPriority(level) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("notif_priority_${level.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Smart Reminders & In-App Pop Timing collapsible card.
 */
@Composable
internal fun NotificationSmartRemindersCard(
    settings: SecuritySettings,
    isSw: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onRepeatSettingsChange: (repeatCount: Int, intervalMinutes: Int) -> Unit,
    onInAppPopDurationChange: (seconds: Int) -> Unit
) {
    val badgeText = if (settings.notifRepeatCount > 0) {
        "${settings.notifRepeatCount}x • ${settings.notifRepeatIntervalMinutes}m"
    } else {
        "${settings.inAppPopDurationSeconds}s Pop"
    }

    NotifCollapsibleCard(
        icon = Icons.Filled.Alarm,
        title = if (isSw) "Vikumbusho & Muda wa Pop-Up" else "Smart Reminders & Pop Timing",
        subtitle = if (isSw) "Rudia arifa ambazo hazijasomwa na muda wa bango la ndani" else "Repeat unread alerts and in-app banner duration",
        accent = MaterialTheme.colorScheme.secondary,
        expanded = expanded,
        onToggleExpand = onToggleExpand,
        statusBadge = badgeText,
        statusBadgeColor = MaterialTheme.colorScheme.secondary,
        headerTestTag = "notif_mgr_reminders_header"
    ) {
        // Repeat unread reminder count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSw) "Rudia Arifa Isiyosomwa (Repeat)" else "Repeat Unread Reminder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 2, 3, 5).forEach { count ->
                val label = if (count == 0) {
                    if (isSw) "Zima" else "Off"
                } else {
                    if (isSw) "Mara $count" else "${count}x"
                }
                FilterChip(
                    selected = settings.notifRepeatCount == count,
                    enabled = settings.notificationsMasterEnabled,
                    onClick = { onRepeatSettingsChange(count, settings.notifRepeatIntervalMinutes) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("notif_repeat_count_$count")
                )
            }
        }

        if (settings.notifRepeatCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isSw) "Muda wa Kurudia (Dakika)" else "Repeat Interval",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 5, 10, 15).forEach { mins ->
                    FilterChip(
                        selected = settings.notifRepeatIntervalMinutes == mins,
                        enabled = settings.notificationsMasterEnabled,
                        onClick = { onRepeatSettingsChange(settings.notifRepeatCount, mins) },
                        label = { Text(if (isSw) "Dak $mins" else "$mins min") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("notif_repeat_interval_$mins")
                    )
                }
            }
        }

        NotifDivider()

        // In-App Pop Banner Duration
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSw) "Muda wa Pop Banner Ndani ya App" else "In-App Pop Banner Duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 8, 12).forEach { secs ->
                FilterChip(
                    selected = settings.inAppPopDurationSeconds == secs,
                    enabled = settings.notificationsMasterEnabled && settings.inAppPopBannerEnabled,
                    onClick = { onInAppPopDurationChange(secs) },
                    label = { Text(if (isSw) "Sek $secs" else "${secs}s") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("notif_pop_duration_$secs")
                )
            }
        }
    }
}

/**
 * VIP Priority Senders Manager — contacts here bypass Quiet Hours (DND) and always alert with high priority.
 */
@Composable
internal fun NotificationVipSendersCard(
    settings: SecuritySettings,
    recentMessages: List<DecryptedSmsMessage>,
    isSw: Boolean,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddVipSender: (String) -> Unit,
    onRemoveVipSender: (String) -> Unit,
    onClearAllVipSenders: () -> Unit
) {
    var vipInput by remember { mutableStateOf("") }

    val candidateSenders = remember(recentMessages, settings.vipNotificationSenders) {
        val seen = LinkedHashSet<String>()
        recentMessages
            .sortedByDescending { it.timestamp }
            .mapNotNull { msg ->
                val raw = msg.phoneNumber.ifBlank { msg.contactName }.trim()
                val key = SmsAdBlockerEngine.canonicalSenderKey(raw)
                if (key.isNotEmpty() && key !in settings.vipNotificationSenders && seen.add(key)) {
                    msg.contactName.ifBlank { raw } to raw
                } else {
                    null
                }
            }
            .take(8)
    }

    NotifCollapsibleCard(
        icon = Icons.Filled.Star,
        title = if (isSw) "Watumaji Muhimu (VIP Priority)" else "VIP Priority Senders",
        subtitle = if (isSw) "Watu muhimu wanaopita Saa za Kimya (DND) na kuita kila wakati" else "Priority contacts that bypass Quiet Hours and always alert",
        accent = accentColor,
        expanded = expanded,
        onToggleExpand = onToggleExpand,
        statusBadge = settings.vipNotificationSenders.size.toString(),
        statusBadgeColor = if (settings.vipNotificationSenders.isNotEmpty()) {
            accentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        headerTestTag = "notif_mgr_vip_header"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = vipInput,
                onValueChange = { vipInput = it },
                placeholder = {
                    Text(
                        text = if (isSw) "Andika namba au jina la VIP..." else "Enter VIP phone or sender...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("notif_vip_input")
            )
            Button(
                onClick = {
                    if (vipInput.isNotBlank()) {
                        onAddVipSender(vipInput.trim())
                        vipInput = ""
                    }
                },
                enabled = vipInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("notif_vip_add_btn")
            ) {
                Text(if (isSw) "Ongeza" else "Add")
            }
        }

        if (candidateSenders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isSw) "Chagua haraka kutoka mazungumzo ya hivi karibuni:" else "Quick add from recent chats:",
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
                candidateSenders.forEach { (display, addr) ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddVipSender(addr) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
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

        if (settings.vipNotificationSenders.isEmpty()) {
            Text(
                text = if (isSw) {
                    "Hakuna mtumaji wa VIP bado. Ongeza familia au namba za dharura."
                } else {
                    "No VIP senders yet. Add family or urgent contacts here."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                settings.vipNotificationSenders.sorted().forEach { senderKey ->
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
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = accentColor,
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
                                onClick = { onRemoveVipSender(senderKey) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (isSw) "Ondoa" else "Remove")
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onClearAllVipSenders,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_vip_clear_all_btn")
                ) {
                    Text(if (isSw) "Ondoa VIP Wote" else "Clear All VIP Senders")
                }
            }
        }
    }
}
