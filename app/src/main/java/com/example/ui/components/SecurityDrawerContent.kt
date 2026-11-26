package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.security.SecuritySettings
import com.example.service.DetectedSimCard
import com.example.service.MengiTexNotificationManager
import com.example.ui.DrawerDestination
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.securityLevelColor
import kotlin.math.roundToInt

/**
 * Clean Matte Slide Bar (Modal Navigation Drawer) with all app tools & vault options.
 */
@Composable
fun SecurityDrawerContent(
    settings: SecuritySettings,
    detectedSims: List<DetectedSimCard>,
    scheduledCount: Int,
    trashCount: Int,
    hiddenFilesCount: Int,
    totalMessagesCount: Int = 0,
    isDefaultSmsApp: Boolean = false,
    onSecurityLevelChange: (Int) -> Unit,
    onNavigateDestination: (DrawerDestination) -> Unit,
    onLockNow: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val targetLevelColor = securityLevelColor(settings.securityLevel)
    val levelAccent by animateColorAsState(
        targetValue = targetLevelColor,
        animationSpec = tween(280),
        label = "drawer_level_accent"
    )

    // Warm Matte Off-White / Soft Carbon Drawer Palette (Zero Glare)
    val isDarkDrawer = settings.resolveIsDarkTheme(androidx.compose.foundation.isSystemInDarkTheme())
    val drawerSolidBg = if (isDarkDrawer) Color(0xFF111214) else Color(0xFFF3F1EC)
    val drawerCardBg = if (isDarkDrawer) Color(0xFF181A1E) else Color(0xFFFAF8F4)
    val drawerCardBorder = if (isDarkDrawer) Color(0xFF363B45) else Color(0xFFD5D0C5)
    val drawerTitleColor = if (isDarkDrawer) Color(0xFFECEAE4) else Color(0xFF1E2022)
    val drawerSubColor = if (isDarkDrawer) Color(0xFFA0A4AB) else Color(0xFF5D6168)
    var securityLevelExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(332.dp),
        drawerContainerColor = drawerSolidBg,
        drawerContentColor = drawerTitleColor,
        drawerTonalElevation = 0.dp,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(drawerSolidBg)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Calm Matte Hero Header Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = drawerCardBg,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(
                        width = 1.dp,
                        color = drawerCardBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_shield_banner_1795203293960),
                        contentDescription = "MengiTex Slide Bar banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDarkDrawer) {
                                    Color(0xEE181A1E)
                                } else {
                                    Color(0xEB263646)
                                }
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MengiTexLogoMark(
                                size = 44.dp,
                                cornerRadius = 15.dp,
                                showGlow = true,
                                modifier = Modifier.testTag("drawer_brand_logo")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "MengiTex",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF10B981).copy(alpha = 0.24f)
                                    ) {
                                        Text(
                                            text = "v${com.example.BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6EE7B7),
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Keystore AES-256 • 6G Turbo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (detectedSims.isEmpty()) {
                                            s.drawerSimAuto
                                        } else {
                                            detectedSims.joinToString(" • ") { it.displayName }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Minimizable Security Level Control Squircle Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(
                        width = 1.2.dp,
                        color = if (securityLevelExpanded) levelAccent.copy(alpha = 0.65f) else drawerCardBorder,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = drawerCardBg,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { securityLevelExpanded = !securityLevelExpanded }
                            .testTag("drawer_security_level_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(levelAccent.copy(alpha = 0.16f))
                                    .border(1.dp, levelAccent.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = levelAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = s.drawerSecurityLevel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = drawerTitleColor
                                )
                                Text(
                                    text = s.levelTitle(settings.securityLevel),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = levelAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = levelAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "${settings.securityLevel}/5",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = levelAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                            Icon(
                                imageVector = if (securityLevelExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = s.settingsMinimizedTapHint,
                                tint = drawerSubColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = securityLevelExpanded,
                        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                for (seg in 1..5) {
                                    val active = seg <= settings.securityLevel
                                    val segColor by animateColorAsState(
                                        targetValue = if (active) levelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        animationSpec = tween(220),
                                        label = "seg_color_$seg"
                                    )
                                    val segHeight by animateDpAsState(
                                        targetValue = if (active) 5.dp else 3.5.dp,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "seg_height_$seg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(segHeight)
                                            .clip(RoundedCornerShape(50))
                                            .background(segColor)
                                    )
                                }
                            }

                            Slider(
                                value = settings.securityLevel.toFloat(),
                                onValueChange = { onSecurityLevelChange(it.roundToInt()) },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = levelAccent,
                                    activeTrackColor = levelAccent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("drawer_security_level_slider")
                            )

                            Text(
                                text = s.levelDesc(settings.securityLevel),
                                style = MaterialTheme.typography.bodySmall,
                                color = drawerSubColor
                            )
                        }
                    }
                }
            }

            DrawerGroupCard(
                title = s.drawerSectionTools,
                cardBg = drawerCardBg,
                borderColor = drawerCardBorder
            ) {
                DrawerActionTile(
                    icon = Icons.Filled.EditNote,
                    title = s.drawerCompose,
                    subtitle = s.drawerComposeSub,
                    badgeText = null,
                    accentColor = Color(0xFF2563EB),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_compose_button",
                    onClick = { onNavigateDestination(DrawerDestination.COMPOSE) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.ScheduleSend,
                    title = s.drawerSchedule,
                    subtitle = s.drawerScheduleSub,
                    badgeText = if (scheduledCount > 0) scheduledCount.toString() else null,
                    accentColor = Color(0xFF7C3AED),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_schedule_button",
                    onClick = { onNavigateDestination(DrawerDestination.SCHEDULE) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.Description,
                    title = s.drawerTemplates,
                    subtitle = s.drawerTemplatesSub,
                    badgeText = null,
                    accentColor = Color(0xFF0EA5E9),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_templates_button",
                    onClick = { onNavigateDestination(DrawerDestination.TEMPLATES) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.BarChart,
                    title = s.drawerUsageDashboard,
                    subtitle = s.drawerUsageDashboardSub,
                    badgeText = if (totalMessagesCount > 0) totalMessagesCount.toString() else null,
                    accentColor = Color(0xFF10B981),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_usage_dashboard_button",
                    onClick = { onNavigateDestination(DrawerDestination.USAGE_DASHBOARD) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                val notifBadgeText = when {
                    !settings.notificationsMasterEnabled -> s.settingsOffShort
                    MengiTexNotificationManager.isCurrentlyInQuietHours(settings) -> "DND"
                    settings.mutedNotificationSenders.isNotEmpty() -> "${settings.mutedNotificationSenders.size} muted"
                    else -> null
                }
                DrawerActionTile(
                    icon = Icons.Filled.NotificationsActive,
                    title = s.drawerNotifManager,
                    subtitle = s.drawerNotifManagerSub,
                    badgeText = notifBadgeText,
                    accentColor = Color(0xFFF59E0B),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_notif_manager_button",
                    onClick = { onNavigateDestination(DrawerDestination.NOTIFICATION_MANAGER) }
                )
            }

            DrawerGroupCard(
                title = "${s.drawerSectionVault} • Keystore AES-256",
                cardBg = drawerCardBg,
                borderColor = drawerCardBorder
            ) {
                DrawerActionTile(
                    icon = Icons.Filled.SimCard,
                    title = s.backupCenterTitle,
                    subtitle = if (isSw) "600k+ Turbo XML/XLM, Faili & Cloud" else "600k+ Turbo XML/XLM, File & Cloud",
                    badgeText = "600k+",
                    accentColor = Color(0xFF0284C7),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_backup_restore_button",
                    onClick = { onNavigateDestination(DrawerDestination.BACKUP_RESTORE) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.Shield,
                    title = s.securityTitle,
                    subtitle = if (isSw) "Passcode, Anti-Intruder & Vault" else "Passcode, Anti-Intruder & Vault",
                    badgeText = "L${settings.securityLevel}",
                    accentColor = levelAccent,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_security_center_button",
                    onClick = { onNavigateDestination(DrawerDestination.SECURITY) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.FolderOff,
                    title = s.drawerHiddenLogs,
                    subtitle = s.drawerHiddenLogsSub,
                    badgeText = if (hiddenFilesCount > 0) hiddenFilesCount.toString() else null,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_hidden_logs_button",
                    onClick = { onNavigateDestination(DrawerDestination.HIDDEN_LOGS) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.DeleteOutline,
                    title = s.drawerTrash,
                    subtitle = s.drawerTrashSub,
                    badgeText = if (trashCount > 0) trashCount.toString() else null,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_trash_button",
                    onClick = { onNavigateDestination(DrawerDestination.TRASH) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.Settings,
                    title = s.drawerSettings,
                    subtitle = s.drawerSettingsSub,
                    badgeText = null,
                    accentColor = MaterialTheme.colorScheme.primary,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_settings_button",
                    onClick = { onNavigateDestination(DrawerDestination.SETTINGS) }
                )
            }

            // 6. Matte Instant Lock Button
            val lockShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(lockShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .premiumBounceClickable(onClick = onLockNow)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
                    .testTag("drawer_lock_now_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (settings.hasPasscode) s.drawerLockNow else s.drawerSetPasscodeFirst,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun DrawerGroupCard(
    title: String,
    cardBg: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        DrawerSectionHeader(title)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = cardBg,
            tonalElevation = 0.dp,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.1.dp, borderColor, RoundedCornerShape(26.dp))
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun DrawerSubtleDivider(borderColor: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 16.dp),
        color = borderColor.copy(alpha = 0.55f)
    )
}

@Composable
private fun DrawerActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String?,
    accentColor: Color,
    titleColor: Color,
    subColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .premiumBounceClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badgeText != null) {
            Badge(
                containerColor = accentColor,
                contentColor = Color.White
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
