package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentPasteOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.security.LauncherIconMode
import com.example.security.SecuritySettings
import com.example.security.VaultFolderStats
import com.example.service.DetectedSimCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.securityLevelColor
import kotlin.math.roundToInt

@Composable
fun SecurityCenterScreen(
    settings: SecuritySettings,
    detectedSims: List<DetectedSimCard>,
    simStateSummary: String,
    vaultStats: VaultFolderStats,
    isCallScreeningRoleHeld: Boolean,
    trashCount: Int,
    onBack: () -> Unit,
    onSecurityLevelChange: (Int) -> Unit,
    onSilentUnknownSmsChange: (Boolean) -> Unit,
    onAutoBlockAdsChange: (Boolean) -> Unit = {},
    onAutoSweepAdsNow: () -> Unit = {},
    onBlockUnknownCallsChange: (Boolean) -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onAutoDeleteSecretChange: (Boolean) -> Unit,
    onAutoLockIdleSecondsChange: (Int) -> Unit,
    onLockOnScreenOffChange: (Boolean) -> Unit,
    onBlockScreenshotsChange: (Boolean) -> Unit,
    onPrivacyCurtainChange: (Boolean) -> Unit,
    onClipboardAutoClearChange: (Boolean) -> Unit,
    onWipeOnIntruderChange: (Boolean) -> Unit,
    onSetPasscode: (String) -> Unit,
    onRemovePasscode: () -> Unit,
    onRefreshSims: () -> Unit,
    onOpenIconStealthDialog: () -> Unit,
    onOpenHiddenLogs: () -> Unit,
    onOpenTrash: () -> Unit
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current
    val successGreen = Color(0xFF10B981)
    var showPasscodeDialog by remember { mutableStateOf(false) }

    var fortressExpanded by remember { mutableStateOf(true) }
    var stealthExpanded by remember { mutableStateOf(!isCallScreeningRoleHeld) }
    var lockExpanded by remember { mutableStateOf(!settings.hasPasscode) }
    var simExpanded by remember { mutableStateOf(false) }
    var vaultExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isCallScreeningRoleHeld) {
        if (isCallScreeningRoleHeld) stealthExpanded = false
    }

    val fortressEnabledCount = listOf(
        settings.blockScreenshotsAndRecents,
        settings.privacyCurtainOnSwitch,
        settings.clipboardAutoClearEnabled,
        settings.wipeOnIntruderEnabled
    ).count { it }

    val levelColor by animateColorAsState(
        targetValue = securityLevelColor(settings.securityLevel),
        animationSpec = tween(260),
        label = "fortress_level_color"
    )

    val fortressScore = remember(settings) {
        var score = 24 // AES-256-GCM hardware vault always active
        if (settings.privacyCurtainOnSwitch) score += 16
        if (settings.silentUnknownSms) score += 14
        if (settings.blockUnknownCalls) score += 14
        if (settings.autoDeleteSecretAfterRead) score += 10
        if (settings.clipboardAutoClearEnabled) score += 8
        if (settings.hasPasscode) score += 9
        if (settings.lockOnScreenOff) score += 5
        if (settings.blockScreenshotsAndRecents) score += 5
        score.coerceIn(0, 100)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = fortressScore / 100f,
        animationSpec = tween(350),
        label = "fortress_score_progress"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.securityTitle,
            subtitle = "Keystore AES-256-GCM",
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "security_back_button",
            onBack = onBack,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PulsingStatusDot(color = levelColor, size = 6.dp)
                        Text(
                            text = "$fortressScore%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Fortress Score & Master Security Level Slider
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = levelColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = s.fortressScoreTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = s.levelTitle(settings.securityLevel),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = levelColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "$fortressScore%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = levelColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = levelColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Text(
                            text = s.levelDesc(settings.securityLevel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Slider(
                            value = settings.securityLevel.toFloat(),
                            onValueChange = { onSecurityLevelChange(it.roundToInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = levelColor,
                                activeTrackColor = levelColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_center_level_slider")
                        )

                        if (settings.securityLevel < 5) {
                            Button(
                                onClick = { onSecurityLevelChange(5) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("activate_max_fortress_button")
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.fortressMaxBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Ultra-Strong Fortress Hardware & Intruder Defenses (Minimizable)
            item {
                SecurityCard(
                    title = s.fortressCardTitle,
                    subtitle = s.fortressCardSub,
                    icon = Icons.Filled.Security,
                    accentColor = MaterialTheme.colorScheme.primary,
                    expanded = fortressExpanded,
                    onToggleExpand = { fortressExpanded = !fortressExpanded },
                    statusBadge = "$fortressEnabledCount/4",
                    statusBadgeColor = if (fortressEnabledCount >= 3) successGreen else MaterialTheme.colorScheme.primary
                ) {
                    SecurityToggleRow(
                        icon = Icons.Filled.Screenshot,
                        title = s.toggleBlockScreenshotsTitle,
                        subtitle = s.toggleBlockScreenshotsSub,
                        checked = settings.blockScreenshotsAndRecents,
                        onCheckedChange = onBlockScreenshotsChange,
                        testTag = "toggle_block_screenshots"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    SecurityToggleRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = s.togglePrivacyCurtainTitle,
                        subtitle = s.togglePrivacyCurtainSub,
                        checked = settings.privacyCurtainOnSwitch,
                        onCheckedChange = onPrivacyCurtainChange,
                        testTag = "toggle_privacy_curtain"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    SecurityToggleRow(
                        icon = Icons.Filled.ContentPasteOff,
                        title = s.toggleClipboardClearTitle,
                        subtitle = s.toggleClipboardClearSub,
                        checked = settings.clipboardAutoClearEnabled,
                        onCheckedChange = onClipboardAutoClearChange,
                        testTag = "toggle_clipboard_auto_clear"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    SecurityToggleRow(
                        icon = Icons.Filled.DeleteForever,
                        title = s.toggleIntruderWipeTitle,
                        subtitle = s.toggleIntruderWipeSub,
                        checked = settings.wipeOnIntruderEnabled,
                        onCheckedChange = onWipeOnIntruderChange,
                        testTag = "toggle_wipe_on_intruder"
                    )
                }
            }

            // 3. Stealth Unknown SMS & Call Blocker (Auto-minimizes when Call Shield active)
            item {
                SecurityCard(
                    title = s.stealthCardTitle,
                    subtitle = s.stealthCardSub,
                    icon = Icons.Filled.NotificationsOff,
                    accentColor = if (isCallScreeningRoleHeld) successGreen else MaterialTheme.colorScheme.primary,
                    expanded = stealthExpanded,
                    onToggleExpand = { stealthExpanded = !stealthExpanded },
                    statusBadge = if (isCallScreeningRoleHeld) s.settingsActiveShort else s.settingsRoleActivate,
                    statusBadgeColor = if (isCallScreeningRoleHeld) successGreen else MaterialTheme.colorScheme.tertiary
                ) {
                    SecurityToggleRow(
                        icon = Icons.Filled.Block,
                        title = s.adBlockerToggleTitle,
                        subtitle = s.adBlockerToggleSub,
                        checked = settings.autoBlockAdsEnabled,
                        onCheckedChange = onAutoBlockAdsChange,
                        testTag = "security_toggle_auto_block_ads"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onAutoSweepAdsNow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_sweep_ads_button")
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s.adBlockerSweepBtn)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    SecurityToggleRow(
                        icon = Icons.Filled.NotificationsOff,
                        title = s.toggleSilentSmsTitle,
                        subtitle = s.toggleSilentSmsSub,
                        checked = settings.silentUnknownSms,
                        onCheckedChange = onSilentUnknownSmsChange,
                        testTag = "toggle_silent_unknown_sms"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    SecurityToggleRow(
                        icon = Icons.Filled.CallEnd,
                        title = s.toggleBlockCallsTitle,
                        subtitle = s.toggleBlockCallsSub,
                        checked = settings.blockUnknownCalls,
                        onCheckedChange = onBlockUnknownCallsChange,
                        testTag = "toggle_block_unknown_calls"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCallScreeningRoleHeld) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = successGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = s.callRoleActiveTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.callRoleInactiveTitle,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = s.callRoleInactiveSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onRequestCallScreeningRole,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("enable_call_screening_role_button")
                                ) {
                                    Text(s.btnEnable)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    SecurityToggleRow(
                        icon = Icons.Filled.LocalFireDepartment,
                        title = s.toggleAutoBurnTitle,
                        subtitle = s.toggleAutoBurnSub,
                        checked = settings.autoDeleteSecretAfterRead,
                        onCheckedChange = onAutoDeleteSecretChange,
                        testTag = "toggle_auto_delete_secret"
                    )
                }
            }

            // 4. Passcode & Idle Auto-Lock (Minimizable, starts minimized once passcode set)
            item {
                SecurityCard(
                    title = s.lockCardTitle,
                    subtitle = if (settings.hasPasscode) s.lockCardPassSet else s.lockCardNoPass,
                    icon = Icons.Filled.LockClock,
                    accentColor = if (settings.hasPasscode) successGreen else MaterialTheme.colorScheme.tertiary,
                    expanded = lockExpanded,
                    onToggleExpand = { lockExpanded = !lockExpanded },
                    statusBadge = if (settings.hasPasscode) s.settingsActiveShort else s.settingsOffShort,
                    statusBadgeColor = if (settings.hasPasscode) successGreen else MaterialTheme.colorScheme.tertiary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPasscodeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("security_set_passcode_button")
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (settings.hasPasscode) s.btnChangePasscode else s.btnSetPasscode)
                        }
                        if (settings.hasPasscode) {
                            OutlinedButton(
                                onClick = onRemovePasscode,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("security_remove_passcode_button")
                            ) {
                                Text(s.btnRemove)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = s.idleLockHeader, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15 to "15s", 30 to "30s", 60 to "1m", 180 to "3m").forEach { (sec, label) ->
                            FilterChip(
                                selected = settings.autoLockIdleSeconds == sec,
                                onClick = { onAutoLockIdleSecondsChange(sec) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SecurityToggleRow(
                        icon = Icons.Filled.LockClock,
                        title = s.toggleLockScreenOffTitle,
                        subtitle = s.toggleLockScreenOffSub,
                        checked = settings.lockOnScreenOff,
                        onCheckedChange = onLockOnScreenOffChange,
                        testTag = "toggle_lock_on_screen_off"
                    )
                }
            }

            // 5. SIM Card Detection (Minimized by default)
            item {
                SecurityCard(
                    title = s.simCardTitle,
                    subtitle = simStateSummary,
                    icon = Icons.Filled.SimCard,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    expanded = simExpanded,
                    onToggleExpand = { simExpanded = !simExpanded },
                    statusBadge = if (detectedSims.isEmpty()) "0 SIM" else "${detectedSims.size} SIM",
                    statusBadgeColor = MaterialTheme.colorScheme.secondary,
                    trailingAction = {
                        if (simExpanded) {
                            IconButton(onClick = onRefreshSims, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                ) {
                    if (detectedSims.isEmpty()) {
                        Text(
                            text = s.simNoneFound,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detectedSims.forEach { sim ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Slot ${sim.slotIndex} • ${sim.carrierName}",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "SubID: ${sim.subscriptionId} ${if (sim.countryIso.isNotBlank()) "• ${sim.countryIso}" else ""} ${if (sim.numberHint.isNotBlank()) "• ${sim.numberHint}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = s.simActiveBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. AES-256 Encryption, Hidden Folder, Trash, Stealth Icon (Minimized by default)
            item {
                SecurityCard(
                    title = s.vaultSummaryTitle,
                    subtitle = s.vaultSummarySub(vaultStats.totalEncryptedFiles),
                    icon = Icons.Filled.EnhancedEncryption,
                    accentColor = MaterialTheme.colorScheme.primary,
                    expanded = vaultExpanded,
                    onToggleExpand = { vaultExpanded = !vaultExpanded },
                    statusBadge = "AES-256 ✓",
                    statusBadgeColor = successGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenHiddenLogs,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("security_open_hidden_logs_button")
                        ) {
                            Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.btnHiddenFolderShort)
                        }
                        OutlinedButton(
                            onClick = onOpenTrash,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("security_open_trash_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.btnTrashCount(trashCount))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenIconStealthDialog,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_icon_stealth_button")
                    ) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (settings.launcherIconMode) {
                                LauncherIconMode.VISIBLE -> s.iconVisibleLabel
                                LauncherIconMode.DISGUISE_CALC -> s.iconDisguiseLabel
                                LauncherIconMode.HIDDEN -> s.iconHiddenLabel
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }

    if (showPasscodeDialog) {
        SetPasscodeDialog(
            onDismiss = { showPasscodeDialog = false },
            onConfirm = { newPass ->
                onSetPasscode(newPass)
                showPasscodeDialog = false
            }
        )
    }
}

@Composable
private fun SecurityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
    statusBadge: String? = null,
    statusBadgeColor: Color = accentColor,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .then(if (onToggleExpand != null) Modifier.clickable(onClick = onToggleExpand) else Modifier)
                    .padding(horizontal = 15.dp, vertical = if (expanded) 14.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(19.dp))
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
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = statusBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                trailingAction?.invoke()
                if (onToggleExpand != null) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(19.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val s = LocalAppStrings.current
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    val mismatch = pass2.isNotEmpty() && pass1 != pass2

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(s.passcodeDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.passcodeDialogDesc, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pass1,
                    onValueChange = { pass1 = it },
                    label = { Text(s.passcodeNewLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_passcode_input_1")
                )
                OutlinedTextField(
                    value = pass2,
                    onValueChange = { pass2 = it },
                    label = { Text(s.passcodeConfirmLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(s.passcodeMismatch) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_passcode_input_2")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pass1) },
                enabled = pass1.length >= 4 && pass1 == pass2,
                modifier = Modifier.testTag("confirm_set_passcode_button")
            ) {
                Text(s.btnSavePasscode)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}
