package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.security.SecuritySettings
import com.example.service.GithubDownloadProgress
import com.example.service.GithubReleaseInfo
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateCheckResult
import com.example.service.GithubUpdateChecker
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full In-App GitHub Version Updater & APK Download/Install/Export Center:
 * - Multi-stage GitHub check (Releases -> Tags -> Commits -> Repo Tree `.apk` -> Local Verified APK Build)
 * - Supports Public & Private GitHub repos (via optional GitHub PAT Token) + Direct `.apk` URLs
 * - Real-time In-App 0%..100% Streaming APK Downloader with progress bar, MB counter, and speed
 * - Direct APK Installer via FileProvider + Save APK to Phone Storage (SAF) + Share APK to upload to GitHub
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
    val successGreen = Color(0xFF10B981)
    val updateAmber = Color(0xFFD97706)

    val installedVersion = remember(context) {
        GithubUpdateChecker.getInstalledAppVersionName(context)
    }
    val installedApkSize = remember(context) {
        GithubUpdateChecker.getInstalledApkSize(context)
    }

    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<GithubUpdateCheckResult?>(null) }
    var showEditRepoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var repoDraft by remember(settings.githubUpdateRepo) {
        mutableStateOf(settings.githubUpdateRepo)
    }
    var tokenDraft by remember {
        mutableStateOf(GithubUpdateChecker.getSavedGithubToken(context))
    }

    // In-app APK download & install state
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<GithubDownloadProgress?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    // SAF Launcher to save downloaded/exported APK directly to phone storage (Downloads, etc.)
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
                        "Faili la APK (${file.name} • $sizeStr) limehifadhiwa kwenye simu yako kikamilifu!"
                    } else {
                        "APK file (${file.name} • $sizeStr) saved to your device successfully!"
                    }
                } else {
                    actionFeedback = if (isSw) {
                        "Imeshindwa kuhifadhi APK: ${res.exceptionOrNull()?.message}"
                    } else {
                        "Failed to save APK: ${res.exceptionOrNull()?.message}"
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
                isChecking = false
                if (res is GithubUpdateCheckResult.Success) {
                    onCacheLatestRelease(res.latestRelease)
                }
            }
        }
    }

    // Auto-run check when opening Settings so the card is immediately populated and ready
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
            }.onFailure {
                actionFeedback = if (isSw) {
                    "Imeshindwa kufungua kivinjari. Tumia kitufe cha Kupakua Ndani ya App."
                } else {
                    "Could not open external browser. Use the In-App Download button."
                }
            }
        }
    }

    val defaultReadyRelease = remember(installedVersion, installedApkSize, settings.githubUpdateRepo) {
        val cleanVer = installedVersion.removePrefix("v").removePrefix("V")
        GithubReleaseInfo(
            tagName = "v$cleanVer",
            releaseName = "MengiTex v$cleanVer (Verified APK Package)",
            bodyNotes = "• Kasi ya 6G Frame-0 Instant Chat & Dual-SIM\n" +
                "• Kizuia Matangazo Mahiri (07/06/+255 Salama Kikashani)\n" +
                "• Hyper-Turbo 600K+ XML/SMS Backup & 3-Tier Zero-Data-Loss Recovery\n" +
                "• Hardware-Backed AES-256-GCM Security Vault",
            htmlUrl = GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo),
            publishedAt = "Ready Build",
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

    val isNewerAvailable = remember(installedVersion, activeRelease.tagName, checkResult) {
        (checkResult as? GithubUpdateCheckResult.Success)?.isNewerAvailable
            ?: GithubUpdateChecker.isRemoteVersionNewer(installedVersion, activeRelease.tagName)
    }

    val startInAppApkDownload: (GithubReleaseInfo, Boolean) -> Unit = { targetRelease, autoInstallAfter ->
        if (!isDownloadingApk) {
            isDownloadingApk = true
            actionFeedback = null
            downloadProgress = GithubDownloadProgress(
                bytesDownloaded = 0L,
                totalBytes = targetRelease.apkSizeBytes.coerceAtLeast(1L),
                percent = 1,
                speedBytesPerSec = 0L
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
                    val sizeLabel = GithubUpdateChecker.formatByteSize(file.length())
                    actionFeedback = if (isSw) {
                        "Imekamilika (100%)! ${file.name} ($sizeLabel) iko tayari kusakinishwa au kuhifadhiwa."
                    } else {
                        "Download Complete (100%)! ${file.name} ($sizeLabel) is ready to install or save."
                    }
                    if (autoInstallAfter) {
                        GithubUpdateChecker.installOrOpenDownloadedApk(context, file)
                    }
                } else {
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Download error"
                    actionFeedback = if (isSw) {
                        "Imeshindwa kupakua APK: $err"
                    } else {
                        "Failed to download APK: $err"
                    }
                }
            }
        }
    }

    val lastCheckedLabel = remember(settings.githubLastCheckedMillis, isSw) {
        if (settings.githubLastCheckedMillis <= 0L) {
            if (isSw) "Tayari kukagua GitHub" else "Ready to check GitHub"
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
                        updateAmber.copy(alpha = 0.65f)
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
                            text = if (isSw) "Toleo la Sasa (Simu)" else "Installed Version",
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
                            isChecking || isDownloadingApk -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isNewerAvailable -> updateAmber.copy(alpha = 0.18f)
                            else -> successGreen.copy(alpha = 0.16f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isChecking || isDownloadingApk) {
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
                                    isDownloadingApk -> if (isSw) {
                                        "Inapakua ${downloadProgress?.percent ?: 0}%..."
                                    } else {
                                        "Downloading ${downloadProgress?.percent ?: 0}%..."
                                    }
                                    isChecking -> if (isSw) "Inakagua GitHub..." else "Checking GitHub..."
                                    isNewerAvailable -> if (isSw) "Toleo Jipya Lipo!" else "New Update Ready!"
                                    else -> if (isSw) "Toleo Lipo Tayari" else "Build Ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isChecking || isDownloadingApk -> MaterialTheme.colorScheme.primary
                                    isNewerAvailable -> updateAmber
                                    else -> successGreen
                                }
                            )
                        }
                    }

                    // Right: Latest GitHub Tag
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isSw) "Toleo la GitHub" else "GitHub Version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v${activeRelease.tagName.removePrefix("v")}",
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
                            tokenDraft = GithubUpdateChecker.getSavedGithubToken(context)
                            showEditRepoDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("settings_github_edit_repo_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Chanzo cha GitHub Repo / APK Link" else "GitHub Repo / APK Source",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val repoDisplay = if (GithubUpdateChecker.isDirectApkUrl(settings.githubUpdateRepo)) {
                            settings.githubUpdateRepo
                        } else {
                            "github.com/${settings.githubUpdateRepo}"
                        }
                        Text(
                            text = repoDisplay,
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

                // Quick-Switch Discovered Repositories (if user has other repos on their GitHub account)
                if (activeRelease.discoveredUserRepos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isSw) {
                                "Repo zako zilizopatikana GitHub (Gusa kuchagua):"
                            } else {
                                "Your discovered GitHub repositories (Tap to select):"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = lastCheckedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Smart Status Notice or Error Notice
        val successNotice = (checkResult as? GithubUpdateCheckResult.Success)?.let {
            if (isSw) it.statusNoticeSw else it.statusNoticeEn
        }
        if (!successNotice.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = successNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (checkResult is GithubUpdateCheckResult.Error) {
            val err = checkResult as GithubUpdateCheckResult.Error
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
                        text = if (isSw) err.messageSw else err.messageEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Release Card, Changelog, Live 0-100% Progress Bar, & Install/Save/Share Actions
        val sizeLabel = GithubUpdateChecker.formatByteSize(activeRelease.apkSizeBytes)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeRelease.releaseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val metaParts = buildList {
                            add("Tag: ${activeRelease.tagName}")
                            if (activeRelease.publishedAt.isNotBlank()) add(activeRelease.publishedAt)
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

                if (activeRelease.bodyNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 135.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isSw) "Maelezo ya Toleo (Changelog):" else "Release Notes (Changelog):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeRelease.bodyNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Live 0% - 100% Download Progress Bar
                AnimatedVisibility(
                    visible = isDownloadingApk || downloadProgress != null,
                    enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
                ) {
                    val prog = downloadProgress
                    if (prog != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isDownloadingApk) {
                                            if (isSw) "Inapakua APK Ndani ya App..." else "Downloading APK In-App..."
                                        } else {
                                            if (isSw) "Upakuaji wa APK Umekamilika ✓" else "APK Download Complete ✓"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDownloadingApk) MaterialTheme.colorScheme.primary else successGreen
                                    )
                                    Text(
                                        text = "${prog.percent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDownloadingApk) MaterialTheme.colorScheme.primary else successGreen
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (prog.percent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isDownloadingApk) MaterialTheme.colorScheme.primary else successGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                val dlSize = GithubUpdateChecker.formatByteSize(prog.bytesDownloaded)
                                val totSize = GithubUpdateChecker.formatByteSize(prog.totalBytes)
                                val speedStr = GithubUpdateChecker.formatByteSize(prog.speedBytesPerSec)
                                Text(
                                    text = if (speedStr.isNotBlank() && isDownloadingApk) {
                                        "$dlSize / $totSize • $speedStr/s"
                                    } else {
                                        "$dlSize / $totSize"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action Feedback Banner
                if (!actionFeedback.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = successGreen.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = successGreen,
                                modifier = Modifier.size(18.dp)
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

                // Primary In-App Download & Install Button
                Button(
                    onClick = {
                        startInAppApkDownload(activeRelease, true)
                    },
                    enabled = !isDownloadingApk,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNewerAvailable) updateAmber else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_github_download_latest_button")
                ) {
                    if (isDownloadingApk) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val fileNameLabel = activeRelease.apkAssetName ?: "MengiTex-${activeRelease.tagName}.apk"
                    val btnText = when {
                        isDownloadingApk -> if (isSw) {
                            "Inapakua $fileNameLabel (${downloadProgress?.percent ?: 0}%)..."
                        } else {
                            "Downloading $fileNameLabel (${downloadProgress?.percent ?: 0}%)..."
                        }
                        activeRelease.isLocalReadyBuild -> if (isSw) {
                            "Pakua & Sakinisha Kifurushi ($fileNameLabel)"
                        } else {
                            "Download & Install Package ($fileNameLabel)"
                        }
                        else -> if (isSw) {
                            "Pakua & Sakinisha Update ($fileNameLabel)"
                        } else {
                            "Download & Install Update ($fileNameLabel)"
                        }
                    }
                    Text(
                        text = btnText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Post-Download Quick Actions: Install APK | Save APK to Phone | Share APK to GitHub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Save APK to Phone Storage (SAF)
                    OutlinedButton(
                        onClick = {
                            val existing = downloadedApkFile
                            if (existing != null && existing.exists()) {
                                saveApkLauncher.launch(existing.name)
                            } else {
                                // Download first then prompt save
                                if (!isDownloadingApk) {
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
                        },
                        enabled = !isDownloadingApk,
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSw) "Hifadhi APK" else "Save APK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // 2. Share APK (e.g. to upload to GitHub Releases or send to another phone)
                    OutlinedButton(
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
                        },
                        enabled = !isDownloadingApk,
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSw) "Tuma APK" else "Share APK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // 3. If file already downloaded, one-tap Re-launch Installer
                    if (downloadedApkFile?.exists() == true) {
                        Button(
                            onClick = {
                                downloadedApkFile?.let { file ->
                                    GithubUpdateChecker.installOrOpenDownloadedApk(context, file)
                                }
                            },
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = successGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSw) "Sakinisha" else "Install",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. Release Channel Selector (Stable vs Pre-Release / Beta)
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

        // 5. Auto-Check Toggle
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
                    val url = activeRelease.htmlUrl.takeIf { it.isNotBlank() && !it.startsWith("local://") }
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
                    text = "GitHub Web",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }

    // Dialog 1: Edit GitHub Repository Owner/Repo, Direct APK URL, or Private GitHub Token
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
                    text = if (isSw) "Mipangilio ya GitHub Update" else "Configure GitHub Updater",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isSw) {
                            "Weka jina la GitHub Repo (mmiliki/repo), link kamili ya GitHub, au link ya moja kwa moja ya faili la .apk:"
                        } else {
                            "Enter the GitHub repository (owner/repo), full GitHub URL, or a direct .apk download URL:"
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
                            Text(if (isSw) "GitHub Token (Hiari - kwa Private Repo)" else "GitHub Token (Optional - Private Repo)")
                        },
                        placeholder = {
                            Text("ghp_...")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                repoDraft = GithubUpdateChecker.DEFAULT_GITHUB_REPO
                            }
                        ) {
                            Text(
                                text = if (isSw) "Rudisha Default" else "Reset Default",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (tokenDraft.isNotBlank()) {
                            TextButton(
                                onClick = { tokenDraft = "" }
                            ) {
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

    // Dialog 2: Recent GitHub Releases / Commits History
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
                                        startInAppApkDownload(item, true)
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
