package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SystemUpdate
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
    hasUpgradeAvailable: Boolean = false,
    latestUpgradeTag: String = "",
    onSecurityLevelChange: (Int) -> Unit,
    onNavigateDestination: (DrawerDestination) -> Unit,
    onLockNow: () -> Unit
) {
    val s = LocalAppStrings.current
    val targetLevelColor = securityLevelColor(settings.securityLevel)
    val levelAccent by animateColorAsState(
        targetValue = targetLevelColor,
        animationSpec = tween(25),
        label = "drawer_level_accent"
    )

    // Android 17 Solid Tonal Drawer Palette (100% Opaque, Zero Glow, Warm Off-White in Light Mode)
    val drawerSolidBg = MaterialTheme.colorScheme.background
    val drawerCardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val drawerCardBorder = MaterialTheme.colorScheme.outlineVariant
    val drawerTitleColor = MaterialTheme.colorScheme.onSurface
    val drawerSubColor = MaterialTheme.colorScheme.onSurfaceVariant
    val drawerHeroBg = MaterialTheme.colorScheme.primaryContainer
    val drawerHeroFg = MaterialTheme.colorScheme.onPrimaryContainer
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
            // 1. Android 17 Solid Hero Header Card (100% Opaque, borderless)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = drawerHeroBg,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .background(drawerHeroBg)
                ) {
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
                                showGlow = false,
                                modifier = Modifier.testTag("drawer_brand_logo")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MengiTex",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = drawerHeroFg,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (detectedSims.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SimCard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = detectedSims.joinToString(" • ") { it.displayName },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = drawerHeroFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Minimizable Security Level Control M3 Tonal Card (borderless)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = drawerCardBg,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = levelAccent,
                                    modifier = Modifier.size(19.dp)
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
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "${settings.securityLevel}/5",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = levelAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
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
                                        targetValue = if (active) levelAccent else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        animationSpec = tween(25),
                                        label = "seg_color_$seg"
                                    )
                                    val segHeight by animateDpAsState(
                                        targetValue = if (active) 5.dp else 3.5.dp,
                                        animationSpec = tween(25),
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
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                    subtitle = null,
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
                    subtitle = null,
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
                    subtitle = null,
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
                    subtitle = null,
                    badgeText = if (totalMessagesCount > 0) totalMessagesCount.toString() else null,
                    accentColor = Color(0xFF10B981),
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_usage_dashboard_button",
                    onClick = { onNavigateDestination(DrawerDestination.USAGE_DASHBOARD) }
                )
            }

            DrawerGroupCard(
                title = s.drawerSectionVault,
                cardBg = drawerCardBg,
                borderColor = drawerCardBorder
            ) {
                DrawerActionTile(
                    icon = Icons.Filled.DeleteOutline,
                    title = s.drawerTrash,
                    subtitle = null,
                    badgeText = if (trashCount > 0) trashCount.toString() else null,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_trash_button",
                    onClick = { onNavigateDestination(DrawerDestination.TRASH) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.SystemUpdate,
                    title = s.drawerUpgrade,
                    subtitle = if (hasUpgradeAvailable && latestUpgradeTag.isNotBlank()) latestUpgradeTag else null,
                    badgeText = if (hasUpgradeAvailable) s.drawerUpgradeAvailableBadge else null,
                    accentColor = if (hasUpgradeAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    titleColor = drawerTitleColor,
                    subColor = drawerSubColor,
                    testTag = "drawer_upgrade_button",
                    onClick = { onNavigateDestination(DrawerDestination.UPGRADE) }
                )
                DrawerSubtleDivider(borderColor = drawerCardBorder)
                DrawerActionTile(
                    icon = Icons.Filled.Settings,
                    title = s.drawerSettings,
                    subtitle = null,
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
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun DrawerSubtleDivider(borderColor: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun DrawerActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String?,
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
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = subColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
