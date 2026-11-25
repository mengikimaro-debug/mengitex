package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.security.SecuritySettings
import com.example.service.GithubReleaseInfo
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateCheckResult
import com.example.service.GithubUpdateChecker
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GitHub App Version Update Center embedded inside SettingsScreen:
 * - Shows currently installed version vs latest GitHub Release tag
 * - Live one-tap check against GitHub REST API (`/repos/{owner}/{repo}/releases` & `/tags`)
 * - Configurable GitHub repository (`owner/repo` or full GitHub URL)
 * - Release channel selector (`Stable` vs `Pre-Release / Beta`) & Auto-Check toggle
 * - Release Notes changelog viewer, recent versions history dialog, and direct download/open actions
 */
@Composable
fun GithubUpdateSectionContent(
    settings: SecuritySettings,
    onUpdateRepoChange: (String) -> Unit,
    onUpdateChannelChange: (GithubUpdateChannel) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    onCacheLatestRelease: (GithubReleaseInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = settings.language == AppLanguage.SW
    val successGreen = Color(0xFF10B981)
    val updateAmber = Color(0xFFD97706)

    val installedVersion = remember(context) {
        GithubUpdateChecker.getInstalledAppVersionName(context)
    }

    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<GithubUpdateCheckResult?>(null) }
    var showEditRepoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var repoDraft by remember(settings.githubUpdateRepo) {
        mutableStateOf(settings.githubUpdateRepo)
    }

    val runLiveCheck: (String, GithubUpdateChannel) -> Unit = { targetRepo, targetChannel ->
        if (!isChecking) {
            isChecking = true
            scope.launch {
                val res = GithubUpdateChecker.checkLatestRelease(
                    context = context,
                    rawRepoInput = targetRepo,
                    channel = targetChannel
                )
                checkResult = res
                isChecking = false
                if (res is GithubUpdateCheckResult.Success) {
                    onCacheLatestRelease(res.latestRelease)
                }
            }
        }
    }

    // Auto-check on first open if auto-check is enabled and last check was over 15 minutes ago
    LaunchedEffect(Unit) {
        val elapsed = System.currentTimeMillis() - settings.githubLastCheckedMillis
        if (settings.githubAutoCheckUpdates && elapsed > 15 * 60 * 1000L) {
            runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
        }
    }

    val openExternalUrl: (String) -> Unit = { url ->
        val safeUrl = url.trim()
        if (safeUrl.isNotEmpty()) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    val activeRelease: GithubReleaseInfo? = when (val res = checkResult) {
        is GithubUpdateCheckResult.Success -> res.latestRelease
        else -> if (settings.githubCachedLatestTag.isNotBlank()) {
            GithubReleaseInfo(
                tagName = settings.githubCachedLatestTag,
                releaseName = settings.githubCachedReleaseName.ifBlank { "MengiTex ${settings.githubCachedLatestTag}" },
                bodyNotes = settings.githubCachedReleaseNotes,
                htmlUrl = settings.githubCachedReleaseUrl.ifBlank {
                    GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                },
                publishedAt = settings.githubCachedPublishedAt,
                isPrerelease = settings.githubUpdateChannel == GithubUpdateChannel.PRE_RELEASE,
                apkAssetUrl = settings.githubCachedApkUrl.takeIf { it.isNotBlank() },
                apkAssetName = settings.githubCachedApkName.takeIf { it.isNotBlank() },
                apkSizeBytes = settings.githubCachedApkSizeBytes
            )
        } else {
            null
        }
    }

    val isNewerAvailable = remember(installedVersion, activeRelease?.tagName) {
        val tag = activeRelease?.tagName ?: ""
        tag.isNotBlank() && GithubUpdateChecker.isRemoteVersionNewer(installedVersion, tag)
    }

    val lastCheckedLabel = remember(settings.githubLastCheckedMillis, isSw) {
        if (settings.githubLastCheckedMillis <= 0L) {
            if (isSw) "Bado haijakaguliwa" else "Not checked yet"
        } else {
            val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            val formatted = fmt.format(Date(settings.githubLastCheckedMillis))
            if (isSw) "Ilipokaguliwa: $formatted" else "Last checked: $formatted"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Installed vs Latest GitHub Version Comparison Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isNewerAvailable) {
                        updateAmber.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Installed Version
                    Column {
                        Text(
                            text = if (isSw) "Toleo la Sasa (Installed)" else "Installed Version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v${installedVersion.removePrefix("v")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when {
                            isChecking -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isNewerAvailable -> updateAmber.copy(alpha = 0.18f)
                            activeRelease != null -> successGreen.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isNewerAvailable) {
                                        Icons.Filled.NewReleases
                                    } else {
                                        Icons.Filled.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = if (isNewerAvailable) updateAmber else successGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isChecking -> if (isSw) "Inakagua GitHub..." else "Checking GitHub..."
                                    isNewerAvailable -> if (isSw) "Toleo Jipya Lipo!" else "New Update Ready!"
                                    activeRelease != null -> if (isSw) "Uko na Toleo Jipya" else "Up to Date"
                                    else -> "GitHub Releases"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isChecking -> MaterialTheme.colorScheme.primary
                                    isNewerAvailable -> updateAmber
                                    activeRelease != null -> successGreen
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }

                    // Right: Latest GitHub Tag
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isSw) "Toleo la GitHub" else "GitHub Latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = activeRelease?.tagName?.let { "v${it.removePrefix("v")}" } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNewerAvailable) updateAmber else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Repository Row + Edit button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            repoDraft = settings.githubUpdateRepo
                            showEditRepoDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("settings_github_edit_repo_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "GitHub Repository (Chanzo cha Update)" else "GitHub Repository Source",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "github.com/${settings.githubUpdateRepo}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = if (isSw) "Badili Repo" else "Edit Repo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSw) "Badili" else "Change",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = lastCheckedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Release Channel Selector (Stable vs Pre-Release / Beta)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isSw) "Mkondo wa Toleo (Update Channel)" else "Release Update Channel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val channels = listOf(
                    GithubUpdateChannel.STABLE to (if (isSw) "Stable (Rasmi)" else "Stable Release"),
                    GithubUpdateChannel.PRE_RELEASE to (if (isSw) "Beta / Pre-Release" else "Beta / Pre-Release")
                )
                channels.forEach { (ch, label) ->
                    val selected = settings.githubUpdateChannel == ch
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateChannelChange(ch)
                            runLiveCheck(settings.githubUpdateRepo, ch)
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_github_channel_${ch.name.lowercase()}")
                    )
                }
            }
        }

        // 3. Auto-Check Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSw) "Kagua Toleo Jipya Kiotomatiki" else "Auto-Check GitHub for New Version",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (isSw) {
                        "Kagua GitHub Releases moja kwa moja unapofungua mipangilio"
                    } else {
                        "Automatically check GitHub Releases for newer builds"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = settings.githubAutoCheckUpdates,
                onCheckedChange = onAutoCheckChange,
                modifier = Modifier.testTag("settings_github_auto_check_switch")
            )
        }

        // 4. Error or No-Releases Feedback Banner (if applicable)
        when (val res = checkResult) {
            is GithubUpdateCheckResult.Error -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isSw) res.messageSw else res.messageEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            is GithubUpdateCheckResult.NoReleasesFound -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isSw) {
                                "Repo '${res.repoSlug}' imepatikana kwenye GitHub lakini bado haina Release iliyochapishwa."
                            } else {
                                "Repository '${res.repoSlug}' was found on GitHub, but no formal Releases have been published yet."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { openExternalUrl(res.repoHtmlUrl) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSw) "Fungua Ukurasa wa GitHub Releases" else "Open GitHub Releases Page",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            else -> Unit
        }

        // 5. Latest Release Card & Changelog Notes (when a release is cached or fetched)
        AnimatedVisibility(
            visible = activeRelease != null,
            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
        ) {
            val rel = activeRelease ?: return@AnimatedVisibility
            val sizeLabel = GithubUpdateChecker.formatByteSize(rel.apkSizeBytes)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rel.releaseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val metaParts = buildList {
                                add("Tag: ${rel.tagName}")
                                if (rel.publishedAt.isNotBlank()) add(rel.publishedAt)
                                if (sizeLabel.isNotBlank()) add(sizeLabel)
                            }
                            Text(
                                text = metaParts.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if ((checkResult as? GithubUpdateCheckResult.Success)?.recentReleases?.size ?: 0 > 1) {
                            IconButton(
                                onClick = { showHistoryDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = if (isSw) "Matoleo ya Nyuma" else "Release History",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (rel.bodyNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (isSw) "Mabadiliko ya Toleo Jipya (Changelog):" else "What's New (Release Notes):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rel.bodyNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Direct Download APK / Release Asset CTA
                    val downloadTargetUrl = rel.apkAssetUrl ?: rel.htmlUrl
                    Button(
                        onClick = { openExternalUrl(downloadTargetUrl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNewerAvailable) updateAmber else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_download_latest_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnText = if (rel.apkAssetName != null) {
                            if (isSw) "Pakua Toleo Jipya (${rel.apkAssetName})" else "Download Update (${rel.apkAssetName})"
                        } else {
                            if (isSw) "Pakua Toleo Jipya (${rel.tagName}) Toka GitHub" else "Download New Version (${rel.tagName}) from GitHub"
                        }
                        Text(
                            text = btnText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 6. Primary Action Buttons: Check Now + Open GitHub Releases Web Page
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                },
                enabled = !isChecking,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .weight(1.15f)
                    .testTag("settings_github_check_now_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSw) "Kagua Sasa" else "Check Now",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = {
                    val url = activeRelease?.htmlUrl?.takeIf { it.isNotBlank() }
                        ?: GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                    openExternalUrl(url)
                },
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_github_open_releases_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GitHub",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }

    // Dialog 1: Edit GitHub Repository Owner/Repo
    if (showEditRepoDialog) {
        AlertDialog(
            onDismissRequest = { showEditRepoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = if (isSw) "weka GitHub Repository" else "Configure GitHub Repository",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isSw) {
                            "Weka jina la GitHub Repo (mmiliki/repo) au link kamili ya GitHub ambapo matoleo mapya (Releases / APK) yanapakiwa:"
                        } else {
                            "Enter the GitHub repository (owner/repo) or full GitHub URL where new app releases are published:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = repoDraft,
                        onValueChange = { repoDraft = it },
                        singleLine = true,
                        label = {
                            Text(if (isSw) "Mmiliki/Repo au GitHub URL" else "Owner/Repo or GitHub URL")
                        },
                        placeholder = {
                            Text(GithubUpdateChecker.DEFAULT_GITHUB_REPO)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_repo_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { repoDraft = GithubUpdateChecker.DEFAULT_GITHUB_REPO }
                        ) {
                            Text(
                                text = if (isSw) "Rudisha Default" else "Reset Default",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = GithubUpdateChecker.normalizeRepoSlug(repoDraft)
                            .ifBlank { GithubUpdateChecker.DEFAULT_GITHUB_REPO }
                        onUpdateRepoChange(normalized)
                        showEditRepoDialog = false
                        runLiveCheck(normalized, settings.githubUpdateChannel)
                    },
                    modifier = Modifier.testTag("settings_github_repo_save_button")
                ) {
                    Text(if (isSw) "Hifadhi & Kagua" else "Save & Check")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRepoDialog = false }) {
                    Text(if (isSw) "Ghairi" else "Cancel")
                }
            }
        )
    }

    // Dialog 2: Recent GitHub Releases History
    if (showHistoryDialog) {
        val recentList = (checkResult as? GithubUpdateCheckResult.Success)?.recentReleases.orEmpty()
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = if (isSw) "Historia ya Matoleo (GitHub)" else "GitHub Release History",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentList.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showHistoryDialog = false
                                        openExternalUrl(item.apkAssetUrl ?: item.htmlUrl)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.releaseName} (${item.tagName})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.publishedAt.isNotBlank()) {
                                        Text(
                                            text = item.publishedAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text(if (isSw) "Funga" else "Close")
                }
            }
        )
    }
}
