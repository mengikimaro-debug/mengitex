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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
 * Clean Settings Hub:
 * Permissions & Notification management are handled automatically by the Android system.
 */
@Composable
fun SettingsScreen(
    settings: SecuritySettings,
    detectedSims: List<DetectedSimCard>,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onAutoBlockAdsChange: (Boolean) -> Unit = {},
    onAutoSweepAdsNow: () -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onDimLightChange: (Boolean) -> Unit,
    onDimLightLevelChange: (Float) -> Unit = {},
    onChatSizeScaleChange: (Float) -> Unit = {},
    onOpenIconStealthDialog: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onRefreshSims: () -> Unit,
    onOpenSecurityCenter: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenUpgradeCenter: () -> Unit = {},
    onGithubRepoChange: (String) -> Unit = {},
    onGithubChannelChange: (GithubUpdateChannel) -> Unit = {},
    onGithubAutoCheckChange: (Boolean) -> Unit = {},
    onGithubCacheRelease: (GithubReleaseInfo) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current
    val successGreen = Color(0xFF10B981)

    var adBlockerExpanded by remember { mutableStateOf(!settings.autoBlockAdsEnabled) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var languageExpanded by remember { mutableStateOf(false) }
    var stealthIconExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var simSecurityExpanded by remember { mutableStateOf(false) }

    val isSwLang = settings.language == AppLanguage.SW
    val dimPercent = (settings.dimLightLevel * 100f).roundToInt()
    val appearanceBadge = buildString {
        append(
            when (settings.themeMode) {
                AppThemeMode.SYSTEM -> if (isSwLang) "Mfumo" else "System"
                AppThemeMode.LIGHT -> if (isSwLang) "Mwanga" else "Light"
                AppThemeMode.DARK -> s.settingsModeDarkShort
            }
        )
        if (settings.dimLightEnabled) {
            append(if (isSwLang) " • Hafifu $dimPercent%" else " • Dim $dimPercent%")
        }
    }
    val stealthModeBadge = when (settings.launcherIconMode) {
        LauncherIconMode.VISIBLE -> s.iconVisibleLabel
        LauncherIconMode.DISGUISE_CALC -> s.iconDisguiseLabel
        LauncherIconMode.HIDDEN -> s.iconHiddenLabel
    }
    val simBadge = if (detectedSims.isEmpty()) {
        s.drawerSimAuto
    } else {
        detectedSims.first().displayName
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.settingsTitle,
            subtitle = null,
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
            // 1. New Vision System Upgrade Hub (Top Featured Card)
            item {
                StaggeredEntrance(index = 0) {
                    GithubUpdateSectionContent(
                        settings = settings,
                        onUpdateRepoChange = onGithubRepoChange,
                        onUpdateChannelChange = onGithubChannelChange,
                        onAutoCheckChange = onGithubAutoCheckChange,
                        onCacheLatestRelease = onGithubCacheRelease,
                        onOpenUpgradeCenter = onOpenUpgradeCenter
                    )
                }
            }

            // 1B. Auto SMS Ad Blocker
            item {
                StaggeredEntrance(index = 1) {
                    SettingsSectionCard(
                        icon = Icons.Filled.Block,
                        title = s.adBlockerCardTitle,
                        subtitle = null,
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
                            Text(
                                text = s.adBlockerToggleTitle,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.autoBlockAdsEnabled,
                                onCheckedChange = onAutoBlockAdsChange,
                                modifier = Modifier.testTag("settings_auto_block_ads_switch")
                            )
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
                        subtitle = null,
                        accent = MaterialTheme.colorScheme.primary,
                        expanded = appearanceExpanded,
                        onToggleExpand = { appearanceExpanded = !appearanceExpanded },
                        statusBadge = appearanceBadge,
                        statusBadgeColor = MaterialTheme.colorScheme.primary,
                        headerTestTag = "settings_appearance_header"
                    ) {
                        // Color Theme Selector
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
                                Text(
                                    text = if (isSwLang) "Mandhari" else "Theme",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                        if (isSwLang) "Mwanga" else "Light",
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
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
                                Text(s.settingsDimLightTitle, style = MaterialTheme.typography.titleSmall)
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FormatSize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isSw) "Ukubwa wa Maandishi ($chatSizePercent%)" else "Text Size ($chatSizePercent%)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    onClick = { onChatSizeScaleChange(1.0f) },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.testTag("settings_chat_size_reset_button")
                                ) {
                                    Text(
                                        text = "100%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Live preview bubble container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
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
                                        text = if (isSw) "Habari! Ukubwa wa ujumbe utaonekana hivi." else "Hi! Your chat bubbles will look like this.",
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
                        subtitle = null,
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
                                            text = if (lang == AppLanguage.SW) "Kiswahili" else "English",
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

            // 5. Stealth App Icon (Minimized by default)
            item {
                StaggeredEntrance(index = 5) {
                    SettingsSectionCard(
                        icon = Icons.Filled.VisibilityOff,
                        title = s.settingsStealthIconCard,
                        subtitle = null,
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
                        subtitle = null,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = backupExpanded,
                        onToggleExpand = { backupExpanded = !backupExpanded },
                        statusBadge = null,
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
                        subtitle = null,
                        accent = MaterialTheme.colorScheme.secondary,
                        expanded = simSecurityExpanded,
                        onToggleExpand = { simSecurityExpanded = !simSecurityExpanded },
                        statusBadge = if (detectedSims.isNotEmpty()) simBadge.take(14) else null,
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
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    accent: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    statusBadge: String? = null,
    statusBadgeColor: Color = accent,
    headerTestTag: String? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(24.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = if (expanded) 14.dp else 13.dp)
                    .then(if (headerTestTag != null) Modifier.testTag(headerTestTag) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (expanded && !subtitle.isNullOrBlank()) {
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = statusBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
