package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppThemeMode
import com.example.security.LauncherIconMode
import com.example.security.SecuritySettings
import com.example.service.DetectedSimCard
import com.example.service.GithubReleaseInfo
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateChecker
import com.example.ui.components.StaggeredEntrance
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import kotlin.math.roundToInt

/**
 * Clean, auto-minimizing Settings & Permissions Hub:
 * - Permissions Hub & Android System Ownership automatically minimize once granted/active,
 *   and individual granted permission rows stay ultra-compact.
 * - Secondary options (Language, Notifications, Stealth Icon, Backup, SIM) start minimized
 *   with live status pills and expand smoothly on tap.
 */
@Composable
fun SettingsScreen(
    settings: SecuritySettings,
    detectedSims: List<DetectedSimCard>,
    hasContactsPermission: Boolean,
    hasStorageFolderAccess: Boolean,
    hasMicPermission: Boolean,
    hasSmsPermission: Boolean,
    isDefaultSmsApp: Boolean = false,
    isCallScreeningRoleHeld: Boolean = false,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onBubbleNotificationsChange: (Boolean) -> Unit,
    onDeliveryReportsChange: (Boolean) -> Unit = {},
    onAutoBlockAdsChange: (Boolean) -> Unit = {},
    onAutoSweepAdsNow: () -> Unit = {},
    onOpenSystemBubbleSettings: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onDimLightChange: (Boolean) -> Unit,
    onDimLightLevelChange: (Float) -> Unit = {},
    onChatSizeScaleChange: (Float) -> Unit = {},
    onOpenIconStealthDialog: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onRefreshSims: () -> Unit,
    onRequestDefaultSmsApp: () -> Unit = {},
    onRequestCallScreeningRole: () -> Unit = {},
    onSyncPhoneSms: () -> Unit = {},
    onRequestContactsPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestAllPermissions: () -> Unit,
    onOpenSecurityCenter: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenNotificationManager: () -> Unit = {},
    onGithubRepoChange: (String) -> Unit = {},
    onGithubChannelChange: (GithubUpdateChannel) -> Unit = {},
    onGithubAutoCheckChange: (Boolean) -> Unit = {},
    onGithubCacheRelease: (GithubReleaseInfo) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current
    val successGreen = Color(0xFF10B981)

    val grantedPermissionsCount = listOf(
        hasContactsPermission,
        hasStorageFolderAccess,
        hasMicPermission,
        hasSmsPermission
    ).count { it }
    val allPermissionsGranted = grantedPermissionsCount == 4
    val corePermissionsGranted = hasSmsPermission && hasContactsPermission

    // Smart expand/minimize state: auto-minimize permissions & ownership once granted
    var ownershipExpanded by remember { mutableStateOf(!isDefaultSmsApp) }
    var permissionsExpanded by remember { mutableStateOf(!corePermissionsGranted && !allPermissionsGranted) }
    var adBlockerExpanded by remember { mutableStateOf(!settings.autoBlockAdsEnabled) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var languageExpanded by remember { mutableStateOf(false) }
    var notificationsExpanded by remember { mutableStateOf(false) }
    var stealthIconExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var simSecurityExpanded by remember { mutableStateOf(false) }
    var githubUpdateExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(isDefaultSmsApp) {
        if (isDefaultSmsApp) ownershipExpanded = false
    }
    LaunchedEffect(allPermissionsGranted, corePermissionsGranted) {
        if (allPermissionsGranted || corePermissionsGranted) {
            permissionsExpanded = false
        }
    }

    val isSwLang = settings.language == AppLanguage.SW
    val dimPercent = (settings.dimLightLevel * 100f).roundToInt()
    val appearanceBadge = buildString {
        append(
            when (settings.themeMode) {
                AppThemeMode.SYSTEM -> if (isSwLang) "Mfumo" else "System"
                AppThemeMode.LIGHT -> "Off-White"
                AppThemeMode.DARK -> s.settingsModeDarkShort
            }
        )
        if (settings.dimLightEnabled) append(" • Dim $dimPercent%")
    }
    val stealthModeBadge = when (settings.launcherIconMode) {
        LauncherIconMode.VISIBLE -> s.iconVisibleLabel
        LauncherIconMode.DISGUISE_CALC -> s.iconDisguiseLabel
        LauncherIconMode.HIDDEN -> s.iconHiddenLabel
    }
    val notifBadge = if (settings.bubbleNotificationsEnabled || settings.deliveryReportsEnabled) {
        s.settingsActiveShort
    } else {
        s.settingsOffShort
    }
    val simBadge = if (detectedSims.isEmpty()) {
        s.drawerSimAuto
    } else {
        detectedSims.first().displayName
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.settingsTitle,
            subtitle = "Keystore AES-256",
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "settings_back_button",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 0. Android System Ownership Hub (Auto-minimizes once MengiTex is Default SMS App)
            item {
                StaggeredEntrance(index = 0) {
                    SettingsSectionCard(
                        icon = Icons.Filled.VerifiedUser,
                        title = s.settingsOwnershipCardTitle,
                        subtitle = s.settingsOwnershipCardSub,
                        accent = if (isDefaultSmsApp) successGreen else MaterialTheme.colorScheme.primary,
                        expanded = ownershipExpanded,
                        onToggleExpand = { ownershipExpanded = !ownershipExpanded },
                        statusBadge = if (isDefaultSmsApp) s.settingsRoleActive else s.settingsRoleActivate,
                        statusBadgeColor = if (isDefaultSmsApp) successGreen else MaterialTheme.colorScheme.tertiary,
                        headerTestTag = "settings_ownership_header"
                    ) {
                        PermissionStatusRow(
                            icon = Icons.Filled.Sms,
                            title = s.settingsDefaultSmsRowTitle,
                            description = s.settingsDefaultSmsRowSub,
                            granted = isDefaultSmsApp,
                            grantedLabel = s.settingsRoleActive,
                            actionLabel = s.settingsRoleActivate,
                            testTag = "settings_default_sms_role_button",
                            onAction = onRequestDefaultSmsApp
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )
                        PermissionStatusRow(
                            icon = Icons.Filled.PhoneLocked,
                            title = s.settingsCallShieldRowTitle,
                            description = s.settingsCallShieldRowSub,
                            granted = isCallScreeningRoleHeld,
                            grantedLabel = s.settingsRoleActive,
                            actionLabel = s.settingsRoleActivate,
                            testTag = "settings_call_screener_role_button",
                            onAction = onRequestCallScreeningRole
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!isDefaultSmsApp) {
                            Button(
                                onClick = onRequestDefaultSmsApp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_take_full_control_button")
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.settingsTakeFullControlBtn, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = onSyncPhoneSms,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sync_phone_sms_button")
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.settingsSyncPhoneSmsBtn)
                        }
                    }
                }
            }

            // 1. Dedicated Permissions Hub (Auto-minimizes after permissions are granted)
            item {
                StaggeredEntrance(index = 1) {
                    SettingsSectionCard(
                        icon = Icons.Filled.Security,
                        title = s.permHubTitle,
                        subtitle = s.permHubSub,
                        accent = if (allPermissionsGranted || corePermissionsGranted) successGreen else MaterialTheme.colorScheme.primary,
                        expanded = permissionsExpanded,
                        onToggleExpand = { permissionsExpanded = !permissionsExpanded },
                        statusBadge = s.settingsAllGrantedSummary(grantedPermissionsCount, 4),
                        statusBadgeColor = if (allPermissionsGranted || corePermissionsGranted) successGreen else MaterialTheme.colorScheme.tertiary,
                        headerTestTag = "settings_permissions_header"
                    ) {
                        PermissionStatusRow(
                            icon = Icons.Filled.Contacts,
                            title = s.permContactsLabel,
                            description = s.permContactsDesc,
                            granted = hasContactsPermission,
                            grantedLabel = s.permStatusGranted,
                            actionLabel = s.permStatusNeeded,
                            testTag = "settings_perm_contacts_button",
                            onAction = onRequestContactsPermission
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )
                        PermissionStatusRow(
                            icon = Icons.Filled.Folder,
                            title = s.permStorageLabel,
                            description = if (hasStorageFolderAccess && settings.storageFolderLabel.isNotBlank()) {
                                s.permStorageGrantedDesc(settings.storageFolderLabel)
                            } else {
                                s.permStorageDesc
                            },
                            granted = hasStorageFolderAccess,
                            grantedLabel = s.permStatusGranted,
                            actionLabel = s.permStatusNeeded,
                            testTag = "settings_perm_storage_button",
                            onAction = onRequestStoragePermission
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )
                        PermissionStatusRow(
                            icon = Icons.Filled.Mic,
                            title = s.permMicLabel,
                            description = s.permMicDesc,
                            granted = hasMicPermission,
                            grantedLabel = s.permStatusGranted,
                            actionLabel = s.permStatusNeeded,
                            testTag = "settings_perm_mic_button",
                            onAction = onRequestMicPermission
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )
                        PermissionStatusRow(
                            icon = Icons.Filled.Sms,
                            title = s.permSmsLabel,
                            description = s.permSmsDesc,
                            granted = hasSmsPermission,
                            grantedLabel = s.permStatusGranted,
                            actionLabel = s.permStatusNeeded,
                            testTag = "settings_perm_sms_button",
                            onAction = onRequestSmsPermission
                        )

                        if (!allPermissionsGranted) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestAllPermissions,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_grant_all_permissions_button")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.permGrantAllBtn)
                            }
                        }
                    }
                }
            }

            // 1B. Auto SMS Ad Blocker (15..., 09..., & Non-Transaction Ads — minimized when active)
            item {
                StaggeredEntrance(index = 2) {
                    SettingsSectionCard(
                        icon = Icons.Filled.Block,
                        title = s.adBlockerCardTitle,
                        subtitle = s.adBlockerCardSub,
                        accent = if (settings.autoBlockAdsEnabled) successGreen else MaterialTheme.colorScheme.error,
                        expanded = adBlockerExpanded,
                        onToggleExpand = { adBlockerExpanded = !adBlockerExpanded },
                        statusBadge = if (settings.autoBlockAdsEnabled) s.settingsActiveShort else s.settingsOffShort,
                        statusBadgeColor = if (settings.autoBlockAdsEnabled) successGreen else MaterialTheme.colorScheme.error,
                        headerTestTag = "settings_ad_blocker_header"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.adBlockerToggleTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    s.adBlockerToggleSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.autoBlockAdsEnabled,
                                onCheckedChange = onAutoBlockAdsChange,
                                modifier = Modifier.testTag("settings_auto_block_ads_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = successGreen.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = successGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = s.adBlockerProtectedChip,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = successGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onAutoSweepAdsNow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sweep_ads_button")
                        ) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.adBlockerSweepBtn)
                        }
                    }
                }
            }

            // 2. Appearance, Dark Mode & Dim Light (Open by default, minimizable)
            item {
                StaggeredEntrance(index = 2) {
                    SettingsSectionCard(
                        icon = Icons.Filled.BrightnessLow,
                        title = s.settingsAppearanceCard,
                        subtitle = s.settingsAppearanceSub,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = appearanceExpanded,
                        onToggleExpand = { appearanceExpanded = !appearanceExpanded },
                        statusBadge = appearanceBadge,
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "settings_appearance_header"
                    ) {
                        // Color Theme Selector: By System • Off-White • Dark
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (settings.themeMode) {
                                        AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                        AppThemeMode.LIGHT -> Icons.Filled.BrightnessHigh
                                        AppThemeMode.DARK -> Icons.Filled.DarkMode
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = if (isSwLang) "Rangi ya Muonekano" else "Color Theme",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = when (settings.themeMode) {
                                            AppThemeMode.SYSTEM -> if (isSwLang) {
                                                "Inategemea mfumo wa simu (By System)"
                                            } else {
                                                "Follows phone system theme"
                                            }
                                            AppThemeMode.LIGHT -> if (isSwLang) {
                                                "Rangi tulivu ya Off-White (bila mng'ao)"
                                            } else {
                                                "Calm matte Off-White theme"
                                            }
                                            AppThemeMode.DARK -> if (isSwLang) {
                                                "Hali ya giza tulivu kwa macho"
                                            } else {
                                                "Matte eye-comfortable dark mode"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themeOptions = listOf(
                                    Triple(
                                        AppThemeMode.SYSTEM,
                                        if (isSwLang) "Mfumo" else "System",
                                        "settings_theme_system"
                                    ),
                                    Triple(
                                        AppThemeMode.LIGHT,
                                        "Off-White",
                                        "settings_theme_light"
                                    ),
                                    Triple(
                                        AppThemeMode.DARK,
                                        if (isSwLang) "Giza" else "Dark",
                                        "settings_theme_dark"
                                    )
                                )
                                themeOptions.forEach { (mode, label, tag) ->
                                    val selected = settings.themeMode == mode
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onThemeModeChange(mode) },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(tag)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.BrightnessLow, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Column {
                                    Text(s.settingsDimLightTitle, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        s.settingsDimLightSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.dimLightEnabled,
                                onCheckedChange = onDimLightChange,
                                modifier = Modifier.testTag("settings_dim_light_switch")
                            )
                        }

                        AnimatedVisibility(
                            visible = settings.dimLightEnabled,
                            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.settingsDimLightLevel(dimPercent),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0.20f to "20%", 0.35f to "35%", 0.55f to "55%").forEach { (preset, label) ->
                                            val selected = kotlin.math.abs(settings.dimLightLevel - preset) < 0.04f
                                            FilterChip(
                                                selected = selected,
                                                onClick = { onDimLightLevelChange(preset) },
                                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.testTag("settings_dim_preset_${label.dropLast(1)}")
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.BrightnessHigh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Slider(
                                        value = settings.dimLightLevel.coerceIn(0.15f, 0.60f),
                                        onValueChange = onDimLightLevelChange,
                                        valueRange = 0.15f..0.60f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_dim_light_slider")
                                    )
                                    Icon(
                                        Icons.Filled.BrightnessLow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Adjust Chat Size ("Ukubwa wa Chat") moved from Conversation Top Bar
                        val isSw = settings.language == AppLanguage.SW
                        val chatScale = settings.chatSizeScale.coerceIn(0.75f, 1.45f)
                        val chatSizePercent = (chatScale * 100f).roundToInt()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_chat_size_section"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FormatSize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isSw) "Ukubwa wa Chat ($chatSizePercent%)" else "Chat Size ($chatSizePercent%)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isSw) {
                                                "Rekebisha ukubwa wa maandishi na ujumbe kwenye chat"
                                            } else {
                                                "Adjust message bubble and text size in conversations"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { onChatSizeScaleChange(1.0f) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.testTag("settings_chat_size_reset_button")
                                ) {
                                    Text(
                                        text = "100%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Live preview bubble container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Incoming sample bubble
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = (18f * chatScale).coerceIn(14f, 24f).dp,
                                        topEnd = (18f * chatScale).coerceIn(14f, 24f).dp,
                                        bottomEnd = (18f * chatScale).coerceIn(14f, 24f).dp,
                                        bottomStart = 5.dp
                                    ),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = if (isSw) "Habari! Ukubwa wa chat utaonekana hivi." else "Hi! Your chat bubbles will look like this.",
                                        fontSize = (14.5f * chatScale).sp,
                                        lineHeight = (20.5f * chatScale).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(
                                            horizontal = (12f * chatScale).coerceIn(10f, 18f).dp,
                                            vertical = (8f * chatScale).coerceIn(6f, 12f).dp
                                        )
                                    )
                                }

                                // Outgoing sample bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = (18f * chatScale).coerceIn(14f, 24f).dp,
                                            topEnd = (18f * chatScale).coerceIn(14f, 24f).dp,
                                            bottomStart = (18f * chatScale).coerceIn(14f, 24f).dp,
                                            bottomEnd = 5.dp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = if (isSw) "Sawa, inaonekana vizuri!" else "Looks great!",
                                            fontSize = (14.5f * chatScale).sp,
                                            lineHeight = (20.5f * chatScale).sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(
                                                horizontal = (12f * chatScale).coerceIn(10f, 18f).dp,
                                                vertical = (8f * chatScale).coerceIn(6f, 12f).dp
                                            )
                                        )
                                    }
                                }

                                // Slider + Minus / Plus controls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            onChatSizeScaleChange((chatScale - 0.05f).coerceIn(0.75f, 1.45f))
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("settings_chat_size_decrease_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Remove,
                                                contentDescription = if (isSw) "Punguza ukubwa wa chat" else "Decrease chat size",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Slider(
                                        value = chatScale,
                                        onValueChange = { onChatSizeScaleChange(it.coerceIn(0.75f, 1.45f)) },
                                        valueRange = 0.75f..1.45f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_chat_size_slider")
                                    )

                                    Surface(
                                        onClick = {
                                            onChatSizeScaleChange((chatScale + 0.05f).coerceIn(0.75f, 1.45f))
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("settings_chat_size_increase_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = if (isSw) "Ongeza ukubwa wa chat" else "Increase chat size",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Language Switcher (Minimized by default with current language badge)
            item {
                StaggeredEntrance(index = 3) {
                    SettingsSectionCard(
                        icon = Icons.Filled.Language,
                        title = s.settingsLanguageCard,
                        subtitle = s.settingsLanguageSub,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = languageExpanded,
                        onToggleExpand = { languageExpanded = !languageExpanded },
                        statusBadge = settings.language.displayName,
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "settings_language_header"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = settings.language == lang
                                FilterChip(
                                    selected = selected,
                                    onClick = { onLanguageChange(lang) },
                                    label = {
                                        Text(
                                            text = if (lang == AppLanguage.SW) "Kiswahili (SW)" else "English (EN)",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_lang_${lang.code}")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bubble Notifications & Delivery Reports (Minimized by default)
            item {
                StaggeredEntrance(index = 4) {
                    SettingsSectionCard(
                        icon = Icons.Filled.ChatBubble,
                        title = s.settingsNotifCard,
                        subtitle = s.settingsNotifSub,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = notificationsExpanded,
                        onToggleExpand = { notificationsExpanded = !notificationsExpanded },
                        statusBadge = notifBadge,
                        statusBadgeColor = if (settings.bubbleNotificationsEnabled || settings.deliveryReportsEnabled) {
                            successGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        headerTestTag = "settings_notif_header"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.settingsBubbleTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    s.settingsBubbleSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.bubbleNotificationsEnabled,
                                onCheckedChange = onBubbleNotificationsChange,
                                modifier = Modifier.testTag("settings_bubble_notif_switch")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.settingsDeliveryReportTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    s.settingsDeliveryReportSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.deliveryReportsEnabled,
                                onCheckedChange = onDeliveryReportsChange,
                                modifier = Modifier.testTag("settings_delivery_report_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenNotificationManager,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_notif_manager_button")
                        ) {
                            Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.settingsOpenNotifManagerBtn)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onOpenSystemBubbleSettings,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_system_bubbles_button")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.settingsOpenSystemBubbles)
                        }
                    }
                }
            }

            // 5. Stealth App Icon (Minimized by default)
            item {
                StaggeredEntrance(index = 5) {
                    SettingsSectionCard(
                        icon = Icons.Filled.VisibilityOff,
                        title = s.settingsStealthIconCard,
                        subtitle = s.settingsStealthIconSub,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = stealthIconExpanded,
                        onToggleExpand = { stealthIconExpanded = !stealthIconExpanded },
                        statusBadge = stealthModeBadge.take(14),
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "settings_stealth_header"
                    ) {
                        OutlinedButton(
                            onClick = onOpenIconStealthDialog,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_icon_stealth_button")
                        ) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stealthModeBadge)
                        }
                    }
                }
            }

            // 6. Backup & Restore Center (Minimized by default)
            item {
                StaggeredEntrance(index = 6) {
                    SettingsSectionCard(
                        icon = Icons.Filled.CloudSync,
                        title = s.settingsBackupCardTitle,
                        subtitle = s.settingsBackupCardSub,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = backupExpanded,
                        onToggleExpand = { backupExpanded = !backupExpanded },
                        statusBadge = "AES-256",
                        statusBadgeColor = MaterialTheme.colorScheme.secondary,
                        headerTestTag = "settings_backup_header"
                    ) {
                        Button(
                            onClick = onOpenBackupRestore,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_backup_restore_button")
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.settingsBackupOpenBtn)
                        }
                    }
                }
            }

            // 7. SIM & Security Shield Link (Minimized by default)
            item {
                StaggeredEntrance(index = 7) {
                    SettingsSectionCard(
                        icon = Icons.Filled.SimCard,
                        title = s.settingsSimPermsCard,
                        subtitle = s.settingsSimPermsSub,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = simSecurityExpanded,
                        onToggleExpand = { simSecurityExpanded = !simSecurityExpanded },
                        statusBadge = simBadge.take(14),
                        statusBadgeColor = MaterialTheme.colorScheme.secondary,
                        headerTestTag = "settings_sim_header"
                    ) {
                        if (detectedSims.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detectedSims.forEach { sim ->
                                    val selected = sim.subscriptionId == settings.selectedSimSubId ||
                                        (settings.selectedSimSubId == -1 && sim == detectedSims.first())
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectSim(sim.subscriptionId) },
                                        label = { Text(sim.displayName) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRefreshSims,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(s.settingsRefreshSim)
                            }
                            Button(
                                onClick = onOpenSecurityCenter,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("settings_open_security_shield_button")
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.securityTitle)
                            }
                        }
                    }
                }
            }

            // 8. Update New Version from GitHub (Kagua & Pakua Toleo Jipya Toka GitHub)
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val installedVer = remember(context) {
                    GithubUpdateChecker.getInstalledAppVersionName(context)
                }
                val hasNewerCached = remember(installedVer, settings.githubCachedLatestTag) {
                    settings.githubCachedLatestTag.isNotBlank() &&
                        GithubUpdateChecker.isRemoteVersionNewer(installedVer, settings.githubCachedLatestTag)
                }
                val githubBadge = when {
                    hasNewerCached -> if (isSwLang) "Mpya ${settings.githubCachedLatestTag}" else "New ${settings.githubCachedLatestTag}"
                    settings.githubCachedLatestTag.isNotBlank() -> "v${settings.githubCachedLatestTag.removePrefix("v")}"
                    else -> "v${installedVer.removePrefix("v")}"
                }
                val githubBadgeColor = if (hasNewerCached) Color(0xFFD97706) else successGreen

                StaggeredEntrance(index = 8) {
                    SettingsSectionCard(
                        icon = Icons.Filled.SystemUpdate,
                        title = if (isSwLang) "Sasisha Toleo Jipya (GitHub Update)" else "Update New Version (GitHub)",
                        subtitle = if (isSwLang) {
                            "Kagua na upakue toleo jipya la app moja kwa moja kutoka GitHub Releases"
                        } else {
                            "Check and download the latest app version directly from GitHub Releases"
                        },
                        accent = if (hasNewerCached) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                        expanded = githubUpdateExpanded,
                        onToggleExpand = { githubUpdateExpanded = !githubUpdateExpanded },
                        statusBadge = githubBadge,
                        statusBadgeColor = githubBadgeColor,
                        headerTestTag = "settings_github_update_header"
                    ) {
                        GithubUpdateSectionContent(
                            settings = settings,
                            onUpdateRepoChange = onGithubRepoChange,
                            onUpdateChannelChange = onGithubChannelChange,
                            onAutoCheckChange = onGithubAutoCheckChange,
                            onCacheLatestRelease = onGithubCacheRelease
                        )
                    }
                }
            }

            item {
                Text(
                    text = s.settingsAboutOffline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    grantedLabel: String,
    actionLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    val badgeColor by animateColorAsState(
        targetValue = if (granted) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary,
        animationSpec = tween(250),
        label = "perm_row_color"
    )

    // Granted permissions automatically render in an ultra-compact minimized single row
    if (granted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.testTag(testTag)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = grantedLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    statusBadge: String? = null,
    statusBadgeColor: Color = accent,
    headerTestTag: String? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (expanded) 2.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (expanded) accent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = cardShape
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 15.dp, vertical = if (expanded) 14.dp else 12.dp)
                    .then(if (headerTestTag != null) Modifier.testTag(headerTestTag) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (expanded) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!expanded && !statusBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBadgeColor.copy(alpha = 0.15f)
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
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, bottom = 15.dp)
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
