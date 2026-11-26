package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecuritySettings
import com.example.service.GithubDownloadProgress
import com.example.service.GithubReleaseInfo
import com.example.service.GithubSelfInstallProgress
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateCheckResult
import com.example.service.GithubUpdateChecker
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Flagship 10x Hyper-Speed OTA System Updater:
 * - Sculptural Orbital Speedometer Hero with animated 260-degree precision dial & ambient glow
 * - Live 10-Lane Parallel Stream Equalizer visualizing all 10 concurrent 256KB download streams
 * - Sleek 4-Step SHA-256 Verified In-App Self-Installer milestone ribbon
 * - Clean, breathable Quick-Action Dock & Custom Segmented Channel Pill
 */
@OptIn(ExperimentalLayoutApi::class)
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

    // Refined Flagship Accent Palette (Harmonizes cleanly with M3 Dark & Light themes)
    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentSecondary = MaterialTheme.colorScheme.secondary
    val accentAmber = Color(0xFFF59E0B)
    val heroBgTop = Color(0xFF0A1019)
    val heroBgBottom = Color(0xFF0F1B26)

    var installedVersion by remember(context) {
        mutableStateOf(GithubUpdateChecker.getInstalledAppVersionName(context))
    }
    var installedSha256 by remember(context) {
        mutableStateOf(GithubUpdateChecker.getSelfInstalledSha256(context))
    }
    val installedApkSize = remember(context) {
        GithubUpdateChecker.getInstalledApkSize(context)
    }

    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<GithubUpdateCheckResult?>(null) }
    var showEditRepoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }
    var repoDraft by remember(settings.githubUpdateRepo) {
        mutableStateOf(settings.githubUpdateRepo)
    }
    var tokenDraft by remember {
        mutableStateOf(GithubUpdateChecker.getSavedGithubToken(context))
    }

    var isDownloadingApk by remember { mutableStateOf(false) }
    var isSelfInstalling by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<GithubDownloadProgress?>(null) }
    var selfInstallProgress by remember { mutableStateOf<GithubSelfInstallProgress?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var selfInstallCompletedTag by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val file = downloadedApkFile
        if (uri != null && file != null) {
            scope.launch {
                val res = GithubUpdateChecker.saveApkToSafUri(context, file, uri)
                if (res.isSuccess) {
                    val sizeStr = GithubUpdateChecker.formatByteSize(res.getOrDefault(0L))
                    actionFeedback = if (isSw) {
                        "APK (${file.name} • $sizeStr) imehifadhiwa kwenye simu."
                    } else {
                        "APK (${file.name} • $sizeStr) saved to your device."
                    }
                }
            }
        }
    }

    val runLiveCheck: (String, GithubUpdateChannel) -> Unit = { targetRepo, targetChannel ->
        if (!isChecking) {
            isChecking = true
            actionFeedback = null
            scope.launch {
                val res = GithubUpdateChecker.checkLatestRelease(
                    context = context,
                    rawRepoInput = targetRepo,
                    channel = targetChannel
                )
                checkResult = res
                installedVersion = GithubUpdateChecker.getInstalledAppVersionName(context)
                installedSha256 = GithubUpdateChecker.getSelfInstalledSha256(context)
                isChecking = false
                if (res is GithubUpdateCheckResult.Success) {
                    onCacheLatestRelease(res.latestRelease)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (checkResult == null) {
            runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
        }
    }

    val openExternalUrl: (String) -> Unit = { url ->
        val safeUrl = url.trim()
        if (safeUrl.isNotEmpty() && !safeUrl.startsWith("local://")) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    val defaultReadyRelease = remember(installedVersion, installedApkSize, settings.githubUpdateRepo) {
        val cleanVer = installedVersion.substringBefore('+').removePrefix("v").removePrefix("V")
        GithubReleaseInfo(
            tagName = "v$cleanVer",
            releaseName = "MengiTex v$cleanVer • Hyper 10x",
            bodyNotes = "• 10-Stream (10x) Hyper-Speed Downloader (256KB Buffer & Live MB/s)\n" +
                "• 4-Stage Auto Self-Installer (SHA-256 Verified & Zero Data Loss)\n" +
                "• 6G Frame-0 Instant Chat, Dual-SIM & Smart Ad Blocker (07/06/+255 Safe)\n" +
                "• Hyper-Turbo 600K+ XML/SMS Backup & 3-Tier Auto-Recovery Shield",
            htmlUrl = GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo),
            publishedAt = "v$cleanVer",
            isPrerelease = false,
            apkAssetUrl = GithubUpdateChecker.LOCAL_APK_SCHEME,
            apkAssetName = "MengiTex-v$cleanVer.apk",
            apkSizeBytes = installedApkSize,
            isLocalReadyBuild = true
        )
    }

    val activeRelease: GithubReleaseInfo = when (val res = checkResult) {
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
                apkAssetUrl = settings.githubCachedApkUrl.takeIf { it.isNotBlank() }
                    ?: GithubUpdateChecker.LOCAL_APK_SCHEME,
                apkAssetName = settings.githubCachedApkName.takeIf { it.isNotBlank() }
                    ?: defaultReadyRelease.apkAssetName,
                apkSizeBytes = if (settings.githubCachedApkSizeBytes > 0L) {
                    settings.githubCachedApkSizeBytes
                } else {
                    installedApkSize
                }
            )
        } else {
            defaultReadyRelease
        }
    }

    val isNewerAvailable = remember(installedVersion, activeRelease.tagName, selfInstallCompletedTag) {
        if (selfInstallCompletedTag != null &&
            activeRelease.tagName.removePrefix("v").equals(selfInstallCompletedTag!!.removePrefix("v"), ignoreCase = true)
        ) {
            false
        } else {
            GithubUpdateChecker.isRemoteVersionNewer(installedVersion, activeRelease.tagName)
        }
    }

    val executeSelfInstallNow: (GithubReleaseInfo, File) -> Unit = { targetRelease, file ->
        if (!isSelfInstalling) {
            isSelfInstalling = true
            scope.launch {
                val installRes = GithubUpdateChecker.runInAppSelfInstaller(
                    context = context,
                    release = targetRelease,
                    downloadedFile = file
                ) { stageProg ->
                    selfInstallProgress = stageProg
                }
                isSelfInstalling = false
                if (installRes.isSuccess) {
                    val newVer = installRes.getOrThrow()
                    installedVersion = newVer
                    installedSha256 = GithubUpdateChecker.getSelfInstalledSha256(context)
                    selfInstallCompletedTag = newVer
                    val sizeLabel = GithubUpdateChecker.formatByteSize(file.length())
                    actionFeedback = if (isSw) {
                        "MengiTex v$newVer imeji-install kikamilifu ($sizeLabel) • Nakala iko Downloads/MengiTex"
                    } else {
                        "MengiTex v$newVer self-installed ($sizeLabel) • Copy saved in Downloads/MengiTex"
                    }
                } else {
                    val err = installRes.exceptionOrNull()?.localizedMessage ?: "Install error"
                    actionFeedback = if (isSw) "Hitilafu ya kusakinisha: $err" else "Self-install error: $err"
                }
            }
        }
    }

    val startHyper10xDownloadAndInstall: (GithubReleaseInfo) -> Unit = { targetRelease ->
        if (!isDownloadingApk && !isSelfInstalling) {
            isDownloadingApk = true
            selfInstallProgress = null
            actionFeedback = null
            downloadProgress = GithubDownloadProgress(
                bytesDownloaded = 0L,
                totalBytes = targetRelease.apkSizeBytes.coerceAtLeast(1L),
                percent = 3,
                speedBytesPerSec = 0L,
                activeStreams = 10,
                streamProgresses = List(10) { 0.08f }
            )
            scope.launch {
                val res = GithubUpdateChecker.downloadReleaseApkInApp(
                    context = context,
                    release = targetRelease
                ) { prog ->
                    downloadProgress = prog
                }
                isDownloadingApk = false
                if (res.isSuccess) {
                    val file = res.getOrThrow()
                    downloadedApkFile = file
                    executeSelfInstallNow(targetRelease, file)
                } else {
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Download error"
                    actionFeedback = if (isSw) "Imeshindwa kupakua: $err" else "Download failed: $err"
                }
            }
        }
    }

    val lastCheckedLabel = remember(settings.githubLastCheckedMillis, isSw) {
        if (settings.githubLastCheckedMillis <= 0L) {
            if (isSw) "Tayari • 10x Hyper" else "Ready • 10x Hyper"
        } else {
            val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val formatted = fmt.format(Date(settings.githubLastCheckedMillis))
            if (isSw) "Mwisho: $formatted" else "Synced: $formatted"
        }
    }

    val hasMultipleHistory = ((checkResult as? GithubUpdateCheckResult.Success)?.recentReleases?.size ?: 0) > 1
    val activeAccent = if (isNewerAvailable) accentAmber else accentPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =========================================================================
        // 1. SCULPTURAL ORBITAL 10x HYPER HERO CARD
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(heroBgTop, heroBgBottom)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Minimal Top Bar: Live 10x Pill + Configure/History Icon Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveHyperBadge(
                        isActive = isDownloadingApk || isSelfInstalling || isChecking,
                        accent = activeAccent
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasMultipleHistory) {
                            IconButton(
                                onClick = { showHistoryDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = if (isSw) "Historia ya Matoleo" else "Release History",
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                repoDraft = settings.githubUpdateRepo
                                tokenDraft = GithubUpdateChecker.getSavedGithubToken(context)
                                showEditRepoDialog = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("settings_github_edit_repo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = if (isSw) "Mipangilio ya GitHub Repo" else "Configure GitHub Repo",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Centerpiece: Precision 260-Degree Orbital Speedometer Ring
                HyperOrbitalGauge(
                    installedVersion = installedVersion,
                    targetVersion = activeRelease.tagName,
                    isChecking = isChecking,
                    isDownloading = isDownloadingApk,
                    isInstalling = isSelfInstalling,
                    isNewerAvailable = isNewerAvailable,
                    downloadProgress = downloadProgress,
                    selfInstallProgress = selfInstallProgress,
                    primaryAccent = accentPrimary,
                    secondaryAccent = accentSecondary,
                    amberAccent = accentAmber,
                    isSw = isSw
                )

                // Crisp Centered Release Title & Compact Single-Line Meta
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = activeRelease.releaseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    val sizeStr = GithubUpdateChecker.formatByteSize(activeRelease.apkSizeBytes)
                    val repoShort = if (GithubUpdateChecker.isDirectApkUrl(settings.githubUpdateRepo)) {
                        "Direct APK"
                    } else {
                        settings.githubUpdateRepo
                    }
                    val metaLine = buildList {
                        if (sizeStr.isNotBlank()) add(sizeStr)
                        if (installedSha256.isNotBlank()) add("SHA $installedSha256")
                        add(repoShort)
                    }.joinToString("  •  ")
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Live 10-Lane Parallel Stream Visualizer (Smoothly reveals on 10x download)
                AnimatedVisibility(
                    visible = isDownloadingApk || downloadProgress != null,
                    enter = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    downloadProgress?.let { prog ->
                        TenLaneHyperStreamBars(
                            progress = prog,
                            isDownloading = isDownloadingApk,
                            accentPrimary = accentPrimary,
                            accentSecondary = accentSecondary,
                            isSw = isSw
                        )
                    }
                }

                // Sleek 4-Milestone Self-Installer Stepper (Smoothly reveals during/after self-install)
                AnimatedVisibility(
                    visible = isSelfInstalling || selfInstallProgress != null,
                    enter = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    selfInstallProgress?.let { inst ->
                        SelfInstallMilestoneRibbon(
                            progress = inst,
                            accentPrimary = accentPrimary,
                            accentSecondary = accentSecondary,
                            isSw = isSw
                        )
                    }
                }

                // Signature Full-Width Gradient CTA Button
                val ctaBusy = isDownloadingApk || isSelfInstalling
                val ctaLabel = when {
                    isSelfInstalling -> if (isSw) {
                        "Inaji-Install (${selfInstallProgress?.percent ?: 0}%)..."
                    } else {
                        "Self-Installing (${selfInstallProgress?.percent ?: 0}%)..."
                    }
                    isDownloadingApk -> if (isSw) {
                        "10x Hyper Speed (${downloadProgress?.percent ?: 0}%)..."
                    } else {
                        "10x Hyper Speed (${downloadProgress?.percent ?: 0}%)..."
                    }
                    selfInstallCompletedTag != null -> if (isSw) {
                        "Imeshasakinishwa ${activeRelease.tagName} ✓"
                    } else {
                        "Installed ${activeRelease.tagName} ✓"
                    }
                    isNewerAvailable -> if (isSw) {
                        "Pakua 10x & Ji-Install (${activeRelease.tagName})"
                    } else {
                        "10x Download & Install (${activeRelease.tagName})"
                    }
                    else -> if (isSw) {
                        "Pakua 10x & Ji-Install Sasa"
                    } else {
                        "10x Download & Self-Install"
                    }
                }

                val ctaGradient = if (isNewerAvailable) {
                    Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C)))
                } else {
                    Brush.horizontalGradient(listOf(accentPrimary, accentSecondary))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (ctaBusy) {
                                Brush.horizontalGradient(
                                    listOf(
                                        activeAccent.copy(alpha = 0.35f),
                                        accentSecondary.copy(alpha = 0.35f)
                                    )
                                )
                            } else {
                                ctaGradient
                            }
                        )
                        .clickable(enabled = !ctaBusy) {
                            startHyper10xDownloadAndInstall(activeRelease)
                        }
                        .testTag("settings_github_download_latest_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (ctaBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (selfInstallCompletedTag != null) {
                                    Icons.Filled.Verified
                                } else {
                                    Icons.Filled.Bolt
                                },
                                contentDescription = null,
                                tint = if (ctaBusy) Color.White else Color(0xFF041410),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = ctaLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ctaBusy) Color.White else Color(0xFF041410),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Clean Toast-Style Verification Feedback Strip
        AnimatedVisibility(
            visible = !actionFeedback.isNullOrBlank(),
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentPrimary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = null,
                        tint = accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = actionFeedback.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // =========================================================================
        // 2. AIRY 4-TILE QUICK ACTION DOCK (Kagua • Hifadhi • Tuma • GitHub)
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickDockTile(
                icon = Icons.Filled.Refresh,
                label = if (isSw) "Kagua" else "Sync",
                isLoading = isChecking,
                enabled = !isChecking && !isDownloadingApk && !isSelfInstalling,
                accent = accentPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_github_check_now_button"),
                onClick = {
                    runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                }
            )

            QuickDockTile(
                icon = Icons.Filled.SaveAlt,
                label = if (isSw) "Hifadhi" else "Save",
                isLoading = false,
                enabled = !isDownloadingApk && !isSelfInstalling,
                accent = accentSecondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    val existing = downloadedApkFile
                    if (existing != null && existing.exists()) {
                        saveApkLauncher.launch(existing.name)
                    } else if (!isDownloadingApk) {
                        isDownloadingApk = true
                        scope.launch {
                            val res = GithubUpdateChecker.downloadReleaseApkInApp(
                                context = context,
                                release = activeRelease
                            ) { prog -> downloadProgress = prog }
                            isDownloadingApk = false
                            res.getOrNull()?.let { file ->
                                downloadedApkFile = file
                                saveApkLauncher.launch(file.name)
                            }
                        }
                    }
                }
            )

            QuickDockTile(
                icon = Icons.Filled.Share,
                label = if (isSw) "Tuma" else "Share",
                isLoading = false,
                enabled = !isDownloadingApk && !isSelfInstalling,
                accent = accentPrimary,
                modifier = Modifier.weight(1f),
                onClick = {
                    val existing = downloadedApkFile
                    if (existing != null && existing.exists()) {
                        GithubUpdateChecker.shareDownloadedApk(
                            context = context,
                            apkFile = existing,
                            title = if (isSw) "Tuma / Pakia APK" else "Share / Upload APK"
                        )
                    } else if (!isDownloadingApk) {
                        isDownloadingApk = true
                        scope.launch {
                            val res = GithubUpdateChecker.downloadReleaseApkInApp(
                                context = context,
                                release = activeRelease
                            ) { prog -> downloadProgress = prog }
                            isDownloadingApk = false
                            res.getOrNull()?.let { file ->
                                downloadedApkFile = file
                                GithubUpdateChecker.shareDownloadedApk(
                                    context = context,
                                    apkFile = file,
                                    title = if (isSw) "Tuma / Pakia APK" else "Share / Upload APK"
                                )
                            }
                        }
                    }
                }
            )

            QuickDockTile(
                icon = Icons.Filled.OpenInNew,
                label = "GitHub",
                isLoading = false,
                enabled = true,
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_github_open_releases_button"),
                onClick = {
                    val url = activeRelease.htmlUrl.takeIf { it.isNotBlank() && !it.startsWith("local://") }
                        ?: GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                    openExternalUrl(url)
                }
            )
        }

        // =========================================================================
        // 3. COLLAPSIBLE RELEASE NOTES (Clean & Uncluttered)
        // =========================================================================
        if (activeRelease.bodyNotes.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { notesExpanded = !notesExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSw) "Maboresho ya Toleo ${activeRelease.tagName}" else "Release Notes ${activeRelease.tagName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lastCheckedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (notesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isSw) "Onyesha maelezo" else "Toggle notes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(
                        visible = notesExpanded,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val lines = activeRelease.bodyNotes
                                .lines()
                                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                                .filter { it.isNotBlank() }
                            lines.forEach { bullet ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(accentPrimary)
                                    )
                                    Text(
                                        text = bullet,
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

        // Discovered Repositories Quick-Picker (only shown when found)
        if (activeRelease.discoveredUserRepos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isSw) "Repo zako GitHub:" else "Your GitHub repositories:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeRelease.discoveredUserRepos.take(4).forEach { repoName ->
                        AssistChip(
                            onClick = {
                                onUpdateRepoChange(repoName)
                                runLiveCheck(repoName, settings.githubUpdateChannel)
                            },
                            label = {
                                Text(
                                    text = repoName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. MODERN SEGMENTED CHANNEL PILL & CLEAN AUTO-CHECK ROW
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleek Segmented Pill Switcher (Stable vs Beta)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val channels = listOf(
                            GithubUpdateChannel.STABLE to (if (isSw) "Stable (Rasmi)" else "Stable"),
                            GithubUpdateChannel.PRE_RELEASE to (if (isSw) "Beta / Pre-Release" else "Beta")
                        )
                        channels.forEach { (ch, label) ->
                            val selected = settings.githubUpdateChannel == ch
                            val pillBg by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    Color.Transparent
                                },
                                animationSpec = tween(180),
                                label = "channel_pill_bg"
                            )
                            val pillText by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                animationSpec = tween(180),
                                label = "channel_pill_text"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(pillBg)
                                    .clickable {
                                        onUpdateChannelChange(ch)
                                        runLiveCheck(settings.githubUpdateRepo, ch)
                                    }
                                    .testTag("settings_github_channel_${ch.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = pillText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Clean Auto-Check Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Kagua Toleo Kiotomatiki" else "Auto-Check Updates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSw) {
                                "Angalia GitHub Releases unapofungua mipangilio"
                            } else {
                                "Check GitHub Releases automatically on open"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.githubAutoCheckUpdates,
                        onCheckedChange = onAutoCheckChange,
                        modifier = Modifier.testTag("settings_github_auto_check_switch")
                    )
                }
            }
        }
    }

    // Dialog 1: Configure GitHub Repository / Direct APK Link / Private Token
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
                    text = if (isSw) "Chanzo cha 10x GitHub OTA" else "10x GitHub OTA Source",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isSw) {
                            "Weka GitHub Repo (mmiliki/repo), link ya GitHub, au link ya moja kwa moja ya .apk:"
                        } else {
                            "Enter a GitHub repository (owner/repo), GitHub URL, or direct .apk download URL:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = repoDraft,
                        onValueChange = { repoDraft = it },
                        singleLine = true,
                        label = {
                            Text(if (isSw) "Mmiliki/Repo au Link ya .apk" else "Owner/Repo or .apk URL")
                        },
                        placeholder = {
                            Text(GithubUpdateChecker.DEFAULT_GITHUB_REPO)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_repo_input")
                    )

                    OutlinedTextField(
                        value = tokenDraft,
                        onValueChange = { tokenDraft = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = {
                            Text(if (isSw) "GitHub Token (Hiari - Private Repo)" else "GitHub Token (Optional - Private)")
                        },
                        placeholder = { Text("ghp_...") },
                        modifier = Modifier.fillMaxWidth()
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
                        if (tokenDraft.isNotBlank()) {
                            TextButton(onClick = { tokenDraft = "" }) {
                                Text(
                                    text = if (isSw) "Futa Token" else "Clear Token",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = GithubUpdateChecker.normalizeRepoSlug(repoDraft)
                            .ifBlank { GithubUpdateChecker.DEFAULT_GITHUB_REPO }
                        GithubUpdateChecker.saveGithubToken(context, tokenDraft)
                        onUpdateRepoChange(normalized)
                        showEditRepoDialog = false
                        runLiveCheck(normalized, settings.githubUpdateChannel)
                    },
                    modifier = Modifier.testTag("settings_github_repo_save_button")
                ) {
                    Text(if (isSw) "Hifadhi & Kagua" else "Save & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRepoDialog = false }) {
                    Text(if (isSw) "Ghairi" else "Cancel")
                }
            }
        )
    }

    // Dialog 2: Release History
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
                    text = if (isSw) "Historia ya Matoleo" else "Release History",
                    fontWeight = FontWeight.Bold
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
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable {
                                        showHistoryDialog = false
                                        startHyper10xDownloadAndInstall(item)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.releaseName} (${item.tagName})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = accentPrimary,
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

@Composable
private fun LiveHyperBadge(
    isActive: Boolean,
    accent: Color
) {
    val infinite = rememberInfiniteTransition(label = "hyper_badge_pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 550 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot_alpha"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        modifier = Modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.35f),
            shape = RoundedCornerShape(50)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = pulseAlpha))
            )
            Text(
                text = "10x HYPER OTA",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun HyperOrbitalGauge(
    installedVersion: String,
    targetVersion: String,
    isChecking: Boolean,
    isDownloading: Boolean,
    isInstalling: Boolean,
    isNewerAvailable: Boolean,
    downloadProgress: GithubDownloadProgress?,
    selfInstallProgress: GithubSelfInstallProgress?,
    primaryAccent: Color,
    secondaryAccent: Color,
    amberAccent: Color,
    isSw: Boolean
) {
    val infinite = rememberInfiniteTransition(label = "orbital_gauge_anim")
    val spinAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_spin"
    )

    val rawProgress = when {
        isInstalling -> (selfInstallProgress?.percent ?: 0) / 100f
        isDownloading -> (downloadProgress?.percent ?: 0) / 100f
        else -> 1f
    }.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "orbital_progress"
    )

    val ringStartColor = when {
        isInstalling -> secondaryAccent
        isNewerAvailable && !isDownloading -> amberAccent
        else -> primaryAccent
    }
    val ringEndColor = when {
        isInstalling -> primaryAccent
        isNewerAvailable && !isDownloading -> Color(0xFFEA580C)
        else -> secondaryAccent
    }

    Box(
        modifier = Modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 4.dp.toPx()
            val arcRadius = outerRadius - 12.dp.toPx()
            val strokePx = 7.5.dp.toPx()
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            // Ambient radial glow behind gauge
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringStartColor.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // 24 precision outer chronograph ticks along 260-degree arc
            val totalTicks = 24
            val startDeg = 140.0
            val sweepTotalDeg = 260.0
            for (i in 0..totalTicks) {
                val frac = i.toFloat() / totalTicks.toFloat()
                val deg = startDeg + sweepTotalDeg * frac
                val rad = deg * PI / 180.0
                val isLit = isChecking || frac <= animatedProgress
                val tickInner = outerRadius - 5.dp.toPx()
                val tickOuter = outerRadius
                drawLine(
                    color = if (isLit) {
                        ringStartColor.copy(alpha = if (i % 4 == 0) 0.85f else 0.45f)
                    } else {
                        Color.White.copy(alpha = 0.14f)
                    },
                    start = Offset(
                        center.x + (cos(rad) * tickInner).toFloat(),
                        center.y + (sin(rad) * tickInner).toFloat()
                    ),
                    end = Offset(
                        center.x + (cos(rad) * tickOuter).toFloat(),
                        center.y + (sin(rad) * tickOuter).toFloat()
                    ),
                    strokeWidth = if (i % 4 == 0) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Track arc
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active sweep arc
            if (isChecking) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, ringStartColor, ringEndColor)
                    ),
                    startAngle = spinAngle,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(ringStartColor, ringEndColor)
                    ),
                    startAngle = 140f,
                    sweepAngle = (260f * animatedProgress).coerceAtLeast(8f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Typography Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            when {
                isDownloading -> {
                    val pct = downloadProgress?.percent ?: 0
                    val speedStr = GithubUpdateChecker.formatByteSize(downloadProgress?.speedBytesPerSec ?: 0L)
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (speedStr.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = primaryAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$speedStr/s",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "10x SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                }

                isInstalling -> {
                    val pct = selfInstallProgress?.percent ?: 0
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = secondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isSw) "SAKINISHA" else "INSTALLING",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        color = secondaryAccent
                    )
                }

                isChecking -> {
                    Text(
                        text = "SYNC",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isSw) "Inakagua..." else "Checking...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                else -> {
                    Text(
                        text = if (isNewerAvailable) {
                            "v${targetVersion.removePrefix("v")}"
                        } else {
                            "v${installedVersion.removePrefix("v")}"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isNewerAvailable) Icons.Filled.Bolt else Icons.Filled.Verified,
                            contentDescription = null,
                            tint = if (isNewerAvailable) amberAccent else primaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                isNewerAvailable -> if (isSw) "TOLEO JIPYA" else "UPDATE READY"
                                else -> if (isSw) "TOLEO KISASA" else "UP TO DATE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = if (isNewerAvailable) amberAccent else primaryAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TenLaneHyperStreamBars(
    progress: GithubDownloadProgress,
    isDownloading: Boolean,
    accentPrimary: Color,
    accentSecondary: Color,
    isSw: Boolean
) {
    val lanes = remember(progress.streamProgresses, progress.percent) {
        if (progress.streamProgresses.size == 10) {
            progress.streamProgresses
        } else {
            val base = (progress.percent / 100f).coerceIn(0.05f, 1f)
            List(10) { base }
        }
    }
    val speedStr = GithubUpdateChecker.formatByteSize(progress.speedBytesPerSec)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isDownloading) {
                    if (isSw) "Mikondo 10 ya Kasi (256KB)" else "10x Parallel Streams (256KB)"
                } else {
                    if (isSw) "Mikondo 10 Imekamilika ✓" else "10x Streams Complete ✓"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = if (speedStr.isNotBlank()) "$speedStr/s" else "${progress.percent}%",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentPrimary
            )
        }

        // 10-Lane Vertical Glass Equalizers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            lanes.forEachIndexed { idx, value ->
                val animatedFill by animateFloatAsState(
                    targetValue = value.coerceIn(0.08f, 1f),
                    animationSpec = tween(durationMillis = 120),
                    label = "lane_$idx"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.09f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFill)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(accentSecondary, accentPrimary)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfInstallMilestoneRibbon(
    progress: GithubSelfInstallProgress,
    accentPrimary: Color,
    accentSecondary: Color,
    isSw: Boolean
) {
    val steps = remember(isSw) {
        if (isSw) {
            listOf("SHA-256", "Andaa", "Sakinisha", "Hifadhi")
        } else {
            listOf("SHA-256", "Stage", "Apply", "Backup")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, label ->
                val stepNum = idx + 1
                val done = progress.stageIndex > stepNum || (progress.stageIndex == stepNum && progress.percent >= 100)
                val active = progress.stageIndex == stepNum && !done
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> accentPrimary
                                    active -> accentSecondary.copy(alpha = 0.25f)
                                    else -> Color.White.copy(alpha = 0.10f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    done -> accentPrimary
                                    active -> accentSecondary
                                    else -> Color.White.copy(alpha = 0.2f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF041410),
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNum",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (active) accentSecondary else Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done || active) Color.White else Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = if (isSw) progress.stageSw else progress.stageEn,
            style = MaterialTheme.typography.labelSmall,
            color = accentSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickDockTile(
    icon: ImageVector,
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accent
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
