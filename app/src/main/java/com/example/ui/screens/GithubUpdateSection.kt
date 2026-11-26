package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.security.SecuritySettings
import com.example.service.GithubDownloadProgress
import com.example.service.GithubReleaseInfo
import com.example.service.GithubSelfInstallOutcome
import com.example.service.GithubSelfInstallProgress
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateCheckResult
import com.example.service.GithubUpdateChecker
import com.example.service.PackageInstallSessionEvent
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android 17 Material 3 Expressive — New Vision Upgrade System:
 * - 100% solid opaque tonal containers (zero transparency, zero radial glow, zero gradients, zero borders)
 * - Compact, crisp New Vision top header with Expressive squircle icon, status pill & filled CTA
 * - Snapshot telemetry strip (Installed Version • Package Size • SHA-256 Integrity)
 * - Live 4-Stage Upgrade Pipeline Stepper & 10-Stream Parallel Engine Meter during download/install
 * - Segmented channel switcher (Stable / Beta), Auto check toggle & overflow actions
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GithubUpdateSectionContent(
    settings: SecuritySettings,
    onUpdateRepoChange: (String) -> Unit,
    onUpdateChannelChange: (GithubUpdateChannel) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    onCacheLatestRelease: (GithubReleaseInfo) -> Unit,
    onOpenUpgradeCenter: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = settings.language == AppLanguage.SW

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    var installedVersion by remember(context) {
        mutableStateOf(GithubUpdateChecker.getInstalledAppVersionName(context))
    }
    val installedApkSize = remember(context) {
        GithubUpdateChecker.getInstalledApkSize(context)
    }
    var verifiedShaHex by remember(context) {
        mutableStateOf(GithubUpdateChecker.getSelfInstalledSha256(context).take(8).uppercase())
    }

    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<GithubUpdateCheckResult?>(null) }
    var showEditRepoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
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
    var pendingInstallApkFile by remember { mutableStateOf<File?>(null) }
    var awaitingInstallPermission by remember { mutableStateOf(false) }
    var selfInstallCompletedTag by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var signatureConflictApk by remember { mutableStateOf<File?>(null) }
    var signatureConflictDetail by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val launchStagedInstallNow: (File, String?) -> Unit = { stagedFile, versionTag ->
        val cleanTag = versionTag?.removePrefix("v")?.removePrefix("V").orEmpty().ifBlank { "latest" }
        val sessionRes = GithubUpdateChecker.installApkViaPackageInstallerSession(context, stagedFile, cleanTag)
        if (sessionRes.isSuccess) {
            selfInstallCompletedTag = versionTag
            actionFeedback = if (isSw) {
                "Kisakinishi cha moja kwa moja kimeanzishwa (${stagedFile.name}) — thibitisha kuboresha."
            } else {
                "Direct in-app installer started (${stagedFile.name}) — confirm to upgrade."
            }
        } else {
            val launchRes = GithubUpdateChecker.launchSystemApkInstaller(context, stagedFile)
            if (launchRes.isSuccess) {
                selfInstallCompletedTag = versionTag
                actionFeedback = if (isSw) {
                    "Kisakinishi cha Android kimefunguliwa — thibitisha kuboresha (${stagedFile.name})."
                } else {
                    "Android Package Installer opened — tap Install to apply (${stagedFile.name})."
                }
            } else {
                val err = launchRes.exceptionOrNull()?.localizedMessage ?: "Error"
                actionFeedback = if (isSw) "Imeshindwa kufungua kisakinishi: $err" else "Could not open installer: $err"
            }
        }
    }

    LaunchedEffect(Unit) {
        GithubUpdateChecker.installSessionEvents.collect { event ->
            when (event) {
                is PackageInstallSessionEvent.PendingUserAction -> {
                    runCatching {
                        context.startActivity(event.confirmIntent)
                    }.onFailure {
                        pendingInstallApkFile?.takeIf { it.exists() }?.let { fallbackFile ->
                            GithubUpdateChecker.launchSystemApkInstaller(context, fallbackFile)
                        }
                    }
                }
                is PackageInstallSessionEvent.Success -> {
                    selfInstallCompletedTag = event.versionTag.ifBlank { selfInstallCompletedTag }
                    signatureConflictApk = null
                    signatureConflictDetail = null
                    actionFeedback = if (isSw) {
                        "MengiTex imeboreshwa moja kwa moja!"
                    } else {
                        "MengiTex updated directly in-place!"
                    }
                }
                is PackageInstallSessionEvent.FailureConflict -> {
                    signatureConflictApk = pendingInstallApkFile ?: downloadedApkFile
                    signatureConflictDetail = if (isSw) {
                        "Toleo lililopo kwenye simu lina saini tofauti ya awali kuliko faili hili jipya. Hifadhi APK hii kisha ondoa toleo la zamani mara moja tu; matoleo yote yajayo yatasakinishwa moja kwa moja."
                    } else {
                        "The existing app on your device was signed with a different older key. Save this APK and uninstall the old version once; all future updates will install directly over the app."
                    }
                    actionFeedback = null
                }
                is PackageInstallSessionEvent.FailureAborted -> {
                    actionFeedback = if (isSw) {
                        "Usakinishaji ulisitishwa na mtumiaji."
                    } else {
                        "Installation was cancelled."
                    }
                }
                is PackageInstallSessionEvent.FailureError -> {
                    val staged = pendingInstallApkFile
                    if (staged != null && staged.exists()) {
                        GithubUpdateChecker.launchSystemApkInstaller(context, staged)
                    } else {
                        actionFeedback = if (isSw) {
                            "Hitilafu ya kusakinisha: ${event.message.ifBlank { "Jaribu tena" }}"
                        } else {
                            "Install failed: ${event.message.ifBlank { "Please retry" }}"
                        }
                    }
                }
            }
        }
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val staged = pendingInstallApkFile
        if (GithubUpdateChecker.canRequestPackageInstalls(context) && staged != null && staged.exists()) {
            awaitingInstallPermission = false
            launchStagedInstallNow(staged, selfInstallCompletedTag)
        } else if (!GithubUpdateChecker.canRequestPackageInstalls(context)) {
            awaitingInstallPermission = false
            actionFeedback = if (isSw) {
                "Ruhusa ya 'Sakinisha programu zisizojulikana' inahitajika ili kuendelea."
            } else {
                "Allow 'Install unknown apps' permission in Settings to install the update."
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        GithubUpdateChecker.registerInstallCollector()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installedVersion = GithubUpdateChecker.getInstalledAppVersionName(context)
                val staged = pendingInstallApkFile
                if (awaitingInstallPermission &&
                    staged != null &&
                    staged.exists() &&
                    GithubUpdateChecker.canRequestPackageInstalls(context)
                ) {
                    awaitingInstallPermission = false
                    launchStagedInstallNow(staged, selfInstallCompletedTag)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            GithubUpdateChecker.unregisterInstallCollector()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                        "Faili (${file.name} • $sizeStr) limehifadhiwa kwenye simu."
                    } else {
                        "APK (${file.name} • $sizeStr) saved to your device."
                    }
                } else {
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Error"
                    actionFeedback = if (isSw) "Imeshindwa kuhifadhi: $err" else "Save failed: $err"
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
        if (safeUrl.startsWith("https://", ignoreCase = true) || safeUrl.startsWith("http://", ignoreCase = true)) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    val defaultFallbackRelease = remember(installedVersion, installedApkSize, settings.githubUpdateRepo) {
        val cleanVer = installedVersion.substringBefore('+').removePrefix("v").removePrefix("V")
        GithubReleaseInfo(
            tagName = "v$cleanVer",
            releaseName = "MengiTex v$cleanVer",
            bodyNotes = "",
            htmlUrl = GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo),
            publishedAt = "v$cleanVer",
            isPrerelease = false,
            apkAssetUrl = null,
            apkAssetName = "MengiTex-v$cleanVer.apk",
            apkSizeBytes = installedApkSize
        )
    }

    val activeRelease: GithubReleaseInfo = when (val res = checkResult) {
        is GithubUpdateCheckResult.Success -> res.latestRelease
        else -> if (
            settings.githubCachedLatestTag.isNotBlank() &&
            !GithubUpdateChecker.isRemoteVersionNewer(settings.githubCachedLatestTag, installedVersion)
        ) {
            GithubReleaseInfo(
                tagName = settings.githubCachedLatestTag,
                releaseName = settings.githubCachedReleaseName.ifBlank { "MengiTex ${settings.githubCachedLatestTag}" },
                bodyNotes = settings.githubCachedReleaseNotes,
                htmlUrl = settings.githubCachedReleaseUrl.ifBlank {
                    GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                },
                publishedAt = settings.githubCachedPublishedAt,
                isPrerelease = settings.githubUpdateChannel == GithubUpdateChannel.PRE_RELEASE,
                apkAssetUrl = settings.githubCachedApkUrl.takeIf {
                    it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
                },
                apkAssetName = settings.githubCachedApkName.takeIf { it.isNotBlank() }
                    ?: defaultFallbackRelease.apkAssetName,
                apkSizeBytes = if (settings.githubCachedApkSizeBytes > 0L) {
                    settings.githubCachedApkSizeBytes
                } else {
                    0L
                }
            )
        } else {
            defaultFallbackRelease
        }
    }

    val hasApkAsset = !activeRelease.apkAssetUrl.isNullOrBlank() &&
        (activeRelease.apkAssetUrl!!.startsWith("https://", ignoreCase = true) ||
            activeRelease.apkAssetUrl!!.startsWith("http://", ignoreCase = true))

    val isNewerAvailable = remember(installedVersion, activeRelease.tagName) {
        GithubUpdateChecker.isRemoteVersionNewer(installedVersion, activeRelease.tagName)
    }

    val executeSelfInstallNow: (GithubReleaseInfo, File) -> Unit = { targetRelease, file ->
        if (!isSelfInstalling) {
            isSelfInstalling = true
            signatureConflictApk = null
            signatureConflictDetail = null
            scope.launch {
                val installRes = GithubUpdateChecker.runInAppSelfInstaller(
                    context = context,
                    release = targetRelease,
                    downloadedFile = file
                ) { stageProg ->
                    selfInstallProgress = stageProg
                    if (stageProg.sha256Short.isNotBlank()) {
                        verifiedShaHex = stageProg.sha256Short.uppercase()
                    }
                }
                isSelfInstalling = false
                if (installRes.isSuccess) {
                    when (val outcome = installRes.getOrThrow()) {
                        is GithubSelfInstallOutcome.DirectPackageInstallerStarted -> {
                            pendingInstallApkFile = outcome.stagedApk
                            downloadedApkFile = outcome.stagedApk
                            selfInstallCompletedTag = outcome.versionTag
                            verifiedShaHex = outcome.sha256Short.take(8).uppercase()
                            val sizeLabel = GithubUpdateChecker.formatByteSize(outcome.stagedApk.length())
                            actionFeedback = if (isSw) {
                                "Kisakinishi cha moja kwa moja kimeanzishwa (v${outcome.versionTag} • $sizeLabel) — thibitisha kuboresha."
                            } else {
                                "Direct updater started (v${outcome.versionTag} • $sizeLabel) — confirm to update."
                            }
                        }
                        is GithubSelfInstallOutcome.LaunchedSystemInstaller -> {
                            pendingInstallApkFile = outcome.stagedApk
                            downloadedApkFile = outcome.stagedApk
                            selfInstallCompletedTag = outcome.versionTag
                            verifiedShaHex = outcome.sha256Short.take(8).uppercase()
                            val sizeLabel = GithubUpdateChecker.formatByteSize(outcome.stagedApk.length())
                            actionFeedback = if (isSw) {
                                "Kisakinishi cha Android kimefunguliwa (v${outcome.versionTag} • $sizeLabel). Bonyeza 'Install' kuthibitisha."
                            } else {
                                "Android Installer opened (v${outcome.versionTag} • $sizeLabel). Tap Install to confirm."
                            }
                        }
                        is GithubSelfInstallOutcome.NeedsInstallPermission -> {
                            pendingInstallApkFile = outcome.stagedApk
                            downloadedApkFile = outcome.stagedApk
                            selfInstallCompletedTag = outcome.versionTag
                            verifiedShaHex = outcome.sha256Short.take(8).uppercase()
                            awaitingInstallPermission = true
                            actionFeedback = if (isSw) {
                                "Washa ruhusa ya 'Sakinisha programu zisizojulikana' kisha rudi — APK itaendelea kusakinishwa."
                            } else {
                                "Enable 'Allow from this source' in Settings — installation will resume automatically."
                            }
                            runCatching {
                                unknownSourcesLauncher.launch(
                                    GithubUpdateChecker.createUnknownSourcesSettingsIntent(context)
                                )
                            }.onFailure {
                                awaitingInstallPermission = false
                            }
                        }
                        is GithubSelfInstallOutcome.SignatureConflict -> {
                            pendingInstallApkFile = outcome.stagedApk
                            downloadedApkFile = outcome.stagedApk
                            verifiedShaHex = outcome.sha256Short.take(8).uppercase()
                            signatureConflictApk = outcome.stagedApk
                            val keyPairInfo = if (outcome.installedCertShort.isNotBlank() && outcome.archiveCertShort.isNotBlank()) {
                                " (${outcome.installedCertShort} ≠ ${outcome.archiveCertShort})"
                            } else ""
                            signatureConflictDetail = if (isSw) {
                                "Toleo lililopo kwenye simu lina saini ya awali tofauti$keyPairInfo. Hifadhi APK hii kisha ondoa toleo la zamani mara moja tu; matoleo yote yajayo yatasakinishwa moja kwa moja."
                            } else {
                                "The currently installed app has a different older signing key$keyPairInfo. Save this APK and uninstall the old build once; every future version will update directly in-app."
                            }
                            actionFeedback = null
                        }
                    }
                } else {
                    val err = installRes.exceptionOrNull()?.localizedMessage ?: "Error"
                    actionFeedback = if (isSw) "Hitilafu ya kusakinisha: $err" else "Install error: $err"
                }
            }
        }
    }

    val startDownloadAndInstall: (GithubReleaseInfo) -> Unit = { targetRelease ->
        if (!isDownloadingApk && !isSelfInstalling) {
            val targetUrl = targetRelease.apkAssetUrl.orEmpty()
            val staged = pendingInstallApkFile
            if (staged != null && staged.exists() && selfInstallCompletedTag != null &&
                targetRelease.tagName.removePrefix("v").equals(selfInstallCompletedTag!!.removePrefix("v"), ignoreCase = true)
            ) {
                executeSelfInstallNow(targetRelease, staged)
            } else if (!targetUrl.startsWith("https://", ignoreCase = true) &&
                !targetUrl.startsWith("http://", ignoreCase = true)
            ) {
                actionFeedback = if (isSw) {
                    "Toleo ${targetRelease.tagName} halina faili la .apk kwenye GitHub Releases bado."
                } else {
                    "Release ${targetRelease.tagName} has no .apk asset attached on GitHub yet."
                }
            } else {
                isDownloadingApk = true
                selfInstallProgress = null
                selfInstallCompletedTag = null
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
                        val err = res.exceptionOrNull()?.localizedMessage ?: "Error"
                        actionFeedback = if (isSw) "Imeshindwa kupakua: $err" else "Download failed: $err"
                    }
                }
            }
        }
    }

    val lastCheckedLabel = remember(settings.githubLastCheckedMillis, isSw) {
        if (settings.githubLastCheckedMillis <= 0L) {
            if (isSw) "Tayari" else "Ready"
        } else {
            val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val formatted = fmt.format(Date(settings.githubLastCheckedMillis))
            if (isSw) "Imekaguliwa $formatted" else "Checked $formatted"
        }
    }

    val hasMultipleHistory = ((checkResult as? GithubUpdateCheckResult.Success)?.recentReleases?.size ?: 0) > 1
    val ctaBusy = isDownloadingApk || isSelfInstalling

    val rawProgressFrac = when {
        isSelfInstalling -> (selfInstallProgress?.percent ?: 0) / 100f
        isDownloadingApk -> (downloadProgress?.percent ?: 0) / 100f
        selfInstallCompletedTag != null -> 1f
        else -> 0f
    }.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgressFrac,
        animationSpec = tween(20),
        label = "android17_upgrade_progress"
    )

    val refreshRotation = if (isChecking) {
        val transition = rememberInfiniteTransition(label = "check_spin")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )
        angle
    } else {
        0f
    }

    val cardShape = RoundedCornerShape(26.dp)
    val cleanInstalled = installedVersion.substringBefore('+').removePrefix("v").removePrefix("V")
    val cleanRemote = activeRelease.tagName.removePrefix("v").removePrefix("V")
    val sizeStr = GithubUpdateChecker.formatByteSize(activeRelease.apkSizeBytes)
    val repoShort = if (GithubUpdateChecker.isDirectApkUrl(settings.githubUpdateRepo)) {
        if (isSw) "Kiungo cha APK" else "Direct APK"
    } else {
        settings.githubUpdateRepo.substringAfter('/').ifBlank { settings.githubUpdateRepo }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // =====================================================================
        // ANDROID 17 NEW VISION UPGRADE SYSTEM CARD (100% SOLID OPAQUE M3)
        // =====================================================================
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Top Vision Eyebrow & Quick Utility Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSw) "Maboresho" else "Updates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onOpenUpgradeCenter != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onOpenUpgradeCenter)
                                    .testTag("settings_github_open_hub_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isSw) "Fungua" else "Open Hub",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        CompactActionIcon(
                            icon = Icons.Filled.Refresh,
                            contentDescription = if (isSw) "Kagua sasa" else "Check now",
                            enabled = !isChecking && !ctaBusy,
                            tint = primaryColor,
                            modifier = Modifier
                                .then(if (isChecking) Modifier.rotate(refreshRotation) else Modifier)
                                .testTag("settings_github_check_now_button"),
                            onClick = {
                                runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                            }
                        )
                        CompactActionIcon(
                            icon = Icons.Filled.Tune,
                            contentDescription = if (isSw) "Mipangilio ya chanzo" else "Configure source",
                            enabled = true,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings_github_edit_repo_button"),
                            onClick = {
                                repoDraft = settings.githubUpdateRepo
                                tokenDraft = GithubUpdateChecker.getSavedGithubToken(context)
                                showEditRepoDialog = true
                            }
                        )
                        Box {
                            CompactActionIcon(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = if (isSw) "Chaguo zaidi" else "More options",
                                enabled = true,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { showActionsMenu = true }
                            )
                            DropdownMenu(
                                expanded = showActionsMenu,
                                onDismissRequest = { showActionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isSw) "Hifadhi faili la programu" else "Save APK") },
                                    leadingIcon = { Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    enabled = !ctaBusy && (downloadedApkFile?.exists() == true || hasApkAsset),
                                    onClick = {
                                        showActionsMenu = false
                                        val existing = downloadedApkFile
                                        if (existing != null && existing.exists()) {
                                            saveApkLauncher.launch(existing.name)
                                        } else if (!isDownloadingApk && hasApkAsset) {
                                            isDownloadingApk = true
                                            scope.launch {
                                                val res = GithubUpdateChecker.downloadReleaseApkInApp(
                                                    context = context,
                                                    release = activeRelease
                                                ) { prog -> downloadProgress = prog }
                                                isDownloadingApk = false
                                                if (res.isSuccess) {
                                                    val file = res.getOrThrow()
                                                    downloadedApkFile = file
                                                    saveApkLauncher.launch(file.name)
                                                } else {
                                                    val err = res.exceptionOrNull()?.localizedMessage ?: "Error"
                                                    actionFeedback = if (isSw) "Imeshindwa kupakua: $err" else "Download failed: $err"
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isSw) "Shiriki faili la programu" else "Share APK") },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    enabled = !ctaBusy && (downloadedApkFile?.exists() == true || hasApkAsset),
                                    onClick = {
                                        showActionsMenu = false
                                        val existing = downloadedApkFile
                                        if (existing != null && existing.exists()) {
                                            GithubUpdateChecker.shareDownloadedApk(
                                                context = context,
                                                apkFile = existing,
                                                title = if (isSw) "Shiriki faili la programu" else "Share APK"
                                            )
                                        } else if (!isDownloadingApk && hasApkAsset) {
                                            isDownloadingApk = true
                                            scope.launch {
                                                val res = GithubUpdateChecker.downloadReleaseApkInApp(
                                                    context = context,
                                                    release = activeRelease
                                                ) { prog -> downloadProgress = prog }
                                                isDownloadingApk = false
                                                if (res.isSuccess) {
                                                    val file = res.getOrThrow()
                                                    downloadedApkFile = file
                                                    GithubUpdateChecker.shareDownloadedApk(
                                                        context = context,
                                                        apkFile = file,
                                                        title = if (isSw) "Shiriki faili la programu" else "Share APK"
                                                    )
                                                } else {
                                                    val err = res.exceptionOrNull()?.localizedMessage ?: "Error"
                                                    actionFeedback = if (isSw) "Imeshindwa kupakua: $err" else "Download failed: $err"
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isSw) "Fungua GitHub" else "Open GitHub") },
                                    leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("settings_github_open_releases_button"),
                                    onClick = {
                                        showActionsMenu = false
                                        val url = activeRelease.htmlUrl.takeIf { it.isNotBlank() && !it.startsWith("local://") }
                                            ?: GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                                        openExternalUrl(url)
                                    }
                                )
                                if (activeRelease.bodyNotes.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (notesExpanded) {
                                                    if (isSw) "Ficha maelezo" else "Hide release notes"
                                                } else {
                                                    if (isSw) "Maelezo ya toleo" else "Release notes"
                                                }
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (notesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showActionsMenu = false
                                            notesExpanded = !notesExpanded
                                        }
                                    )
                                }
                                if (hasMultipleHistory) {
                                    DropdownMenuItem(
                                        text = { Text(if (isSw) "Historia ya matoleo" else "Release history") },
                                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showActionsMenu = false
                                            showHistoryDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Hero Version & Primary Upgrade Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_github_update_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isNewerAvailable) tertiaryContainer else primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecking || ctaBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.6.dp,
                                color = if (isNewerAvailable) onTertiaryContainer else onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = if (isNewerAvailable) Icons.Filled.SystemUpdate else Icons.Filled.Verified,
                                contentDescription = null,
                                tint = if (isNewerAvailable) onTertiaryContainer else onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "v$cleanRemote",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    selfInstallCompletedTag != null -> primaryContainer
                                    isNewerAvailable -> tertiaryContainer
                                    else -> secondaryContainer
                                }
                            ) {
                                Text(
                                    text = when {
                                        selfInstallCompletedTag != null -> if (isSw) "Imeboreshwa" else "Upgraded"
                                        isNewerAvailable -> if (isSw) "Toleo Jipya" else "Upgrade Ready"
                                        else -> if (isSw) "Kisasa" else "Current"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        selfInstallCompletedTag != null -> onPrimaryContainer
                                        isNewerAvailable -> onTertiaryContainer
                                        else -> onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        val subLine = when {
                            isSelfInstalling -> if (isSw) selfInstallProgress?.stageSw.orEmpty() else selfInstallProgress?.stageEn.orEmpty()
                            isDownloadingApk -> {
                                val sp = GithubUpdateChecker.formatByteSize(downloadProgress?.speedBytesPerSec ?: 0L)
                                val pct = downloadProgress?.percent ?: 0
                                if (isSw) {
                                    if (sp.isNotBlank()) "Inapakua $pct% • $sp/s" else "Inapakua $pct%"
                                } else {
                                    if (sp.isNotBlank()) "Downloading $pct% • $sp/s" else "Downloading $pct%"
                                }
                            }
                            isChecking -> if (isSw) "Inakagua toleo jipya..." else "Scanning for new build..."
                            isNewerAvailable -> if (isSw) "Kutoka v$cleanInstalled kwenda v$cleanRemote" else "v$cleanInstalled → v$cleanRemote ready"
                            else -> "$repoShort • $lastCheckedLabel"
                        }
                        Text(
                            text = subLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val ctaLabel = when {
                        isSelfInstalling -> "${selfInstallProgress?.percent ?: 0}%"
                        isDownloadingApk -> "${downloadProgress?.percent ?: 0}%"
                        pendingInstallApkFile?.exists() == true -> if (isSw) "Sakinisha" else "Install"
                        !hasApkAsset -> "GitHub"
                        isNewerAvailable -> if (isSw) "Boresha" else "Upgrade"
                        else -> if (isSw) "Sakinisha" else "Install"
                    }
                    Button(
                        onClick = {
                            if (pendingInstallApkFile?.exists() != true && !hasApkAsset) {
                                actionFeedback = if (isSw) {
                                    "Toleo ${activeRelease.tagName} halina faili la .apk kwenye GitHub Releases bado."
                                } else {
                                    "Release ${activeRelease.tagName} has no .apk asset attached on GitHub yet."
                                }
                                val url = activeRelease.htmlUrl.ifBlank {
                                    GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                                }
                                openExternalUrl(url)
                            } else {
                                startDownloadAndInstall(activeRelease)
                            }
                        },
                        enabled = !ctaBusy,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNewerAvailable) tertiaryColor else primaryColor,
                            contentColor = if (isNewerAvailable) MaterialTheme.colorScheme.onTertiary else onPrimaryColor
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("settings_github_download_latest_button")
                    ) {
                        if (!ctaBusy) {
                            Icon(
                                imageVector = if (selfInstallCompletedTag != null) Icons.Filled.Check else Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = ctaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // 3. Compact 3-Metric Telemetry Strip (Solid M3 Tonal Strip)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UpgradeMetricPill(
                            label = if (isSw) "Kwenye Simu" else "Installed",
                            value = "v$cleanInstalled",
                            modifier = Modifier.weight(1f)
                        )
                        UpgradeMetricPill(
                            label = if (isSw) "Kifurushi" else "Package",
                            value = sizeStr.ifBlank { "APK" },
                            modifier = Modifier.weight(1f)
                        )
                        UpgradeMetricPill(
                            label = if (isSw) "Ulinzi" else "Integrity",
                            value = if (verifiedShaHex.isNotBlank()) verifiedShaHex else "SHA-256",
                            icon = Icons.Filled.Security,
                            tint = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Live 4-Stage Upgrade Pipeline & 10-Stream Meter (Expands smoothly when active)
                AnimatedVisibility(
                    visible = isDownloadingApk || isSelfInstalling || selfInstallCompletedTag != null,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    val activeStage = when {
                        selfInstallCompletedTag != null -> 4
                        isSelfInstalling -> (selfInstallProgress?.stageIndex ?: 1).coerceIn(1, 4)
                        isDownloadingApk -> 1
                        else -> 0
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        selfInstallCompletedTag != null -> if (isSw) "Maboresho Yamekamilika" else "Upgrade Complete"
                                        isSelfInstalling -> if (isSw) "Uthibitisho na Usakinishaji" else "Verification & Staging"
                                        else -> if (isSw) "Injini ya Mikondo 10" else "10-Stream Parallel Transfer"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                            }

                            // 4-Stage Pipeline Indicator Row
                            val stages = if (isSw) {
                                listOf("Pakua", "SHA-256", "Andaa", "Tayari")
                            } else {
                                listOf("Fetch", "SHA-256", "Stage", "Done")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                stages.forEachIndexed { idx, stageTitle ->
                                    val stepNum = idx + 1
                                    val reached = activeStage >= stepNum
                                    val current = activeStage == stepNum && selfInstallCompletedTag == null
                                    val barColor by animateColorAsState(
                                        targetValue = when {
                                            selfInstallCompletedTag != null -> primaryColor
                                            current -> tertiaryColor
                                            reached -> primaryColor
                                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        animationSpec = tween(20),
                                        label = "stage_bar_$idx"
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(CircleShape)
                                                .background(barColor)
                                        )
                                        Text(
                                            text = stageTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                                            color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (selfInstallCompletedTag != null) tertiaryColor else primaryColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round
                            )

                            // Live 10-Stream Mini Equalizer Bars during active download
                            val streams = downloadProgress?.streamProgresses.orEmpty()
                            if (isDownloadingApk && streams.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    streams.take(10).forEach { frac ->
                                        val clamped = frac.coerceIn(0.08f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(clamped)
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Inline Check Error / No Releases Banner (if network/repo error)
                val checkErrorMsg = when (val res = checkResult) {
                    is GithubUpdateCheckResult.Error -> if (isSw) res.messageSw else res.messageEn
                    is GithubUpdateCheckResult.Success -> if (isSw) res.statusNoticeSw else res.statusNoticeEn
                    is GithubUpdateCheckResult.NoReleasesFound -> if (isSw) {
                        "Hakuna toleo rasmi lililochapishwa bado kwenye ${res.repoSlug}."
                    } else {
                        "No published releases found yet on ${res.repoSlug}."
                    }
                    else -> null
                }
                AnimatedVisibility(
                    visible = !checkErrorMsg.isNullOrBlank(),
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    val isErr = checkResult is GithubUpdateCheckResult.Error
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isErr) errorContainer else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = if (isErr) onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = checkErrorMsg.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isErr) onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isErr && !isChecking) {
                                Text(
                                    text = if (isSw) "Jaribu" else "Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isErr) onErrorContainer else primaryColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                // 6. Bottom Solid Segmented Channel Switcher + Auto Check + Notes Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val channels = listOf(
                                    GithubUpdateChannel.STABLE to (if (isSw) "Rasmi" else "Stable"),
                                    GithubUpdateChannel.PRE_RELEASE to (if (isSw) "Majaribio" else "Beta")
                                )
                                channels.forEach { (ch, label) ->
                                    val selected = settings.githubUpdateChannel == ch
                                    val bg by animateColorAsState(
                                        targetValue = if (selected) primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        animationSpec = tween(20),
                                        label = "ch_bg"
                                    )
                                    val fg by animateColorAsState(
                                        targetValue = if (selected) onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        animationSpec = tween(20),
                                        label = "ch_fg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 30.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(bg)
                                            .clickable {
                                                onUpdateChannelChange(ch)
                                                runLiveCheck(settings.githubUpdateRepo, ch)
                                            }
                                            .padding(horizontal = 11.dp, vertical = 5.dp)
                                            .testTag("settings_github_channel_${ch.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = fg,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        val autoOn = settings.githubAutoCheckUpdates
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (autoOn) secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .heightIn(min = 30.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onAutoCheckChange(!autoOn) }
                                .testTag("settings_github_auto_check_switch")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (autoOn) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = onSecondaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = if (isSw) "Otomatiki" else "Auto",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (autoOn) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (autoOn) onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (activeRelease.bodyNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (notesExpanded) primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .heightIn(min = 30.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { notesExpanded = !notesExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (notesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isSw) "Maelezo ya toleo" else "Release notes",
                                    tint = if (notesExpanded) onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isSw) "Maelezo" else "Notes",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (notesExpanded) onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Signature Conflict Action Card (only when existing build has a legacy mismatched key)
        AnimatedVisibility(
            visible = !signatureConflictDetail.isNullOrBlank(),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = signatureConflictDetail.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val conflictFile = signatureConflictApk
                        if (conflictFile != null && conflictFile.exists()) {
                            Button(
                                onClick = { saveApkLauncher.launch(conflictFile.name) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isSw) "Hifadhi APK" else "Save APK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        GithubUpdateChecker.createAppDetailsSettingsIntent(context)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSw) "Ondoa ya Zamani" else "App Info",
                                color = onErrorContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Solid Feedback Banner (when download/install/save finishes)
        AnimatedVisibility(
            visible = !actionFeedback.isNullOrBlank(),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selfInstallCompletedTag != null) Icons.Filled.CheckCircle else Icons.Filled.DownloadDone,
                        contentDescription = null,
                        tint = onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = actionFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Expandable Release Notes Card
        AnimatedVisibility(
            visible = notesExpanded && activeRelease.bodyNotes.isNotBlank(),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isSw) "Yaliyomo kwenye toleo • ${activeRelease.releaseName}" else "What's new • ${activeRelease.releaseName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    val lines = activeRelease.bodyNotes
                        .lines()
                        .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                        .filter { it.isNotBlank() }
                    lines.forEach { bullet ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
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

        // Discovered Repositories Quick-Picker (only shown when discovered)
        if (activeRelease.discoveredUserRepos.isNotEmpty()) {
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
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }
    }

    // Dialog 1: Configure GitHub Repository / Direct APK Link / Private Token
    if (showEditRepoDialog) {
        AlertDialog(
            onDismissRequest = { showEditRepoDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            icon = {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = if (isSw) "Chanzo cha Maboresho" else "Upgrade Source",
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
                            "Weka hazina ya GitHub (mmiliki/hazina), kiungo cha GitHub, au kiungo cha moja kwa moja cha APK:"
                        } else {
                            "Enter a GitHub repository (owner/repo), GitHub URL, or direct APK download URL:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = repoDraft,
                        onValueChange = { repoDraft = it },
                        singleLine = true,
                        label = {
                            Text(if (isSw) "Mmiliki/Hazina au Kiungo" else "Owner/Repo or APK URL")
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
                            Text(if (isSw) "Ufunguo wa Siri (Hiari)" else "Access Token (Optional)")
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
                                text = if (isSw) "Rudisha Asili" else "Reset Default",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (tokenDraft.isNotBlank()) {
                            TextButton(onClick = { tokenDraft = "" }) {
                                Text(
                                    text = if (isSw) "Futa Ufunguo" else "Clear Token",
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
                    Text(if (isSw) "Hifadhi na Kagua" else "Save & Check")
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable {
                                        showHistoryDialog = false
                                        startDownloadAndInstall(item)
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
                                    tint = primaryColor,
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
private fun UpgradeMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactActionIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}
