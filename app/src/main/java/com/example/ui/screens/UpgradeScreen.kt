package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.ui.components.OneUi85SubScreenHeader
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-Screen Android 17 Material 3 Expressive — New Vision Upgrade Hub.
 * 100% solid opaque tonal containers (zero transparency, zero glow, zero gradients, zero borders).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpgradeScreen(
    settings: SecuritySettings,
    onBack: () -> Unit,
    onUpdateRepoChange: (String) -> Unit,
    onUpdateChannelChange: (GithubUpdateChannel) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    onCacheLatestRelease: (GithubReleaseInfo) -> Unit,
    onUpgradeAllSystems: (((String) -> Unit) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalAppStrings.current
    val isSw = settings.language == AppLanguage.SW

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onTertiaryColor = MaterialTheme.colorScheme.onTertiary
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
    var isUpgradingAllSystems by remember { mutableStateOf(false) }
    var allSystemsUpgradedDone by remember { mutableStateOf(false) }

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
                        event.confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                isUpgradingAllSystems = true
                actionFeedback = if (isSw) {
                    "Inaboresha mifumo yote ya ndani na kukagua APK mpya..."
                } else {
                    "Upgrading all internal systems & probing for APK..."
                }
                onUpgradeAllSystems?.invoke { msg ->
                    isUpgradingAllSystems = false
                    allSystemsUpgradedDone = true
                    actionFeedback = msg
                } ?: run {
                    isUpgradingAllSystems = false
                    allSystemsUpgradedDone = true
                }
                runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
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
            fmt.format(Date(settings.githubLastCheckedMillis))
        }
    }

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
        label = "upgrade_hub_progress"
    )

    val refreshRotation = if (isChecking) {
        val transition = rememberInfiniteTransition(label = "upgrade_hub_spin")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "upgrade_hub_spin_angle"
        )
        angle
    } else {
        0f
    }

    val cleanInstalled = installedVersion.substringBefore('+').removePrefix("v").removePrefix("V")
    val cleanRemote = activeRelease.tagName.removePrefix("v").removePrefix("V")
    val sizeStr = GithubUpdateChecker.formatByteSize(activeRelease.apkSizeBytes)
    val recentHistory = (checkResult as? GithubUpdateCheckResult.Success)?.recentReleases.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OneUi85SubScreenHeader(
            title = s.upgradeScreenTitle,
            subtitle = null,
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "upgrade_back_button",
            onBack = onBack,
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                        },
                        enabled = !isChecking && !ctaBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("upgrade_screen_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = if (isSw) "Kagua sasa" else "Check now",
                            tint = primaryColor,
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (isChecking) Modifier.rotate(refreshRotation) else Modifier)
                        )
                    }
                    IconButton(
                        onClick = {
                            repoDraft = settings.githubUpdateRepo
                            tokenDraft = GithubUpdateChecker.getSavedGithubToken(context)
                            showEditRepoDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("upgrade_screen_source_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = if (isSw) "Chanzo cha maboresho" else "Upgrade source",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Android 17 Solid Hero System Upgrade Card
                item {
                    val heroBg = if (isNewerAvailable) tertiaryContainer else primaryContainer
                    val heroFg = if (isNewerAvailable) onTertiaryContainer else onPrimaryContainer
                    val heroBadgeBg = if (isNewerAvailable) tertiaryColor else primaryColor
                    val heroBadgeFg = if (isNewerAvailable) onTertiaryColor else onPrimaryColor

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = heroBg,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upgrade_screen_hero_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                                ) {
                                    Text(
                                        text = when {
                                            selfInstallCompletedTag != null -> if (isSw) "Imeboreshwa" else "Upgraded"
                                            isNewerAvailable -> if (isSw) "Toleo Jipya" else "Upgrade Ready"
                                            else -> if (isSw) "Kisasa" else "Up to Date"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(heroBadgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecking || ctaBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = heroBadgeFg
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isNewerAvailable) Icons.Filled.SystemUpdate else Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = heroBadgeFg,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "MengiTex v$cleanRemote",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = heroFg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val statusSubtitle = when {
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
                                        isChecking -> if (isSw) "Inakagua..." else "Checking..."
                                        isNewerAvailable -> "v$cleanInstalled → v$cleanRemote"
                                        else -> if (isSw) "Toleo la kisasa" else "Up to date"
                                    }
                                    Text(
                                        text = statusSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = heroFg,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val ctaText = when {
                                isSelfInstalling -> if (isSw) {
                                    "Inasakinisha ${selfInstallProgress?.percent ?: 0}%..."
                                } else {
                                    "Installing ${selfInstallProgress?.percent ?: 0}%..."
                                }
                                isDownloadingApk -> if (isSw) {
                                    "Inapakua ${downloadProgress?.percent ?: 0}%..."
                                } else {
                                    "Downloading ${downloadProgress?.percent ?: 0}%..."
                                }
                                isUpgradingAllSystems -> if (isSw) {
                                    "Inaboresha Mfumo Wote..."
                                } else {
                                    "Upgrading All Systems..."
                                }
                                pendingInstallApkFile?.exists() == true -> if (isSw) "Sakinisha APK Sasa" else "Install Staged APK"
                                !hasApkAsset -> if (isSw) "Boresha Mfumo Wote Sasa" else "Upgrade All Systems Now"
                                isNewerAvailable -> if (isSw) "Pakua & Sakinisha" else "Download & Install"
                                else -> if (isSw) "Sakinisha Tena" else "Reinstall Build"
                            }

                            Button(
                                onClick = { startDownloadAndInstall(activeRelease) },
                                enabled = !ctaBusy && !isUpgradingAllSystems,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = heroBadgeBg,
                                    contentColor = heroBadgeFg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("upgrade_screen_primary_cta_button")
                            ) {
                                if (!ctaBusy && !isUpgradingAllSystems) {
                                    Icon(
                                        imageVector = if (selfInstallCompletedTag != null) Icons.Filled.Check else Icons.Filled.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = ctaText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Unified "Upgrade System All / Update System All" 1-Tap Action Button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = !ctaBusy && !isUpgradingAllSystems) {
                                        isUpgradingAllSystems = true
                                        actionFeedback = if (isSw) {
                                            "Inaboresha mifumo yote 5 (OTA, SIM/SMS, Anwani, Kinga ya Matangazo & Vault AES-256)..."
                                        } else {
                                            "Upgrading all 5 systems (OTA, SIM/SMS, Contacts, Ad-Shield & AES-256 Vault)..."
                                        }
                                        onUpgradeAllSystems?.invoke { summaryMsg ->
                                            isUpgradingAllSystems = false
                                            allSystemsUpgradedDone = true
                                            actionFeedback = summaryMsg
                                        } ?: run {
                                            isUpgradingAllSystems = false
                                            allSystemsUpgradedDone = true
                                            actionFeedback = if (isSw) {
                                                "Mfumo wote umeboreshwa kikamilifu!"
                                            } else {
                                                "All systems upgraded & synced!"
                                            }
                                        }
                                        runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                                        if (hasApkAsset && isNewerAvailable) {
                                            startDownloadAndInstall(activeRelease)
                                        }
                                    }
                                    .testTag("upgrade_screen_upgrade_all_systems_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (allSystemsUpgradedDone) Icons.Filled.Verified else Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isUpgradingAllSystems -> if (isSw) "Inaboresha Mfumo Wote..." else "Upgrading All Systems..."
                                            allSystemsUpgradedDone -> if (isSw) "Mfumo Wote Umeboreshwa (5/5)" else "All Systems Upgraded (5/5)"
                                            else -> if (isSw) "Boresha Mfumo Wote (Update System All)" else "Upgrade All Systems (OTA + SIM + Vault)"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Live 4-Stage Pipeline & 10-Stream Engine Card
                item {
                    val activeStage = when {
                        selfInstallCompletedTag != null -> 4
                        isSelfInstalling -> (selfInstallProgress?.stageIndex ?: 1).coerceIn(1, 4)
                        isDownloadingApk -> 1
                        else -> 0
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        selfInstallCompletedTag != null -> if (isSw) "Maboresho Yamekamilika" else "Upgrade Pipeline Complete"
                                        isSelfInstalling -> if (isSw) "Uthibitisho na Usakinishaji" else "SHA-256 Verification & Staging"
                                        isDownloadingApk -> if (isSw) "Injini ya Mikondo 10" else "10-Stream Parallel Engine"
                                        else -> if (isSw) "Njia ya Maboresho ya Hatua 4" else "4-Stage Upgrade Pipeline"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (ctaBusy || selfInstallCompletedTag != null) {
                                        "${(animatedProgress * 100).toInt()}%"
                                    } else {
                                        if (isSw) "Tayari" else "Ready"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                            }

                            val stages = if (isSw) {
                                listOf("Pakua", "SHA-256", "Andaa", "Tayari")
                            } else {
                                listOf("Fetch", "SHA-256", "Stage", "Done")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        label = "upgrade_stage_$idx"
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(barColor)
                                        )
                                        Text(
                                            text = stageTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                                            color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = ctaBusy || selfInstallCompletedTag != null,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape),
                                        color = if (selfInstallCompletedTag != null) tertiaryColor else primaryColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        strokeCap = StrokeCap.Round
                                    )
                                    val streams = downloadProgress?.streamProgresses.orEmpty()
                                    if (isDownloadingApk && streams.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            streams.take(10).forEach { frac ->
                                                val clamped = frac.coerceIn(0.08f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(5.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(clamped)
                                                            .height(5.dp)
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
                    }
                }

                // 3. Feedback, Signature Conflict & Error Banners
                if (!signatureConflictDetail.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = signatureConflictDetail.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
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
                                                style = MaterialTheme.typography.labelMedium,
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
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!actionFeedback.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (selfInstallCompletedTag != null) Icons.Filled.CheckCircle else Icons.Filled.DownloadDone,
                                    contentDescription = null,
                                    tint = onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = actionFeedback.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

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
                if (!checkErrorMsg.isNullOrBlank()) {
                    item {
                        val isErr = checkResult is GithubUpdateCheckResult.Error
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isErr) errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isErr) onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = checkErrorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isErr) onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isErr && !isChecking) {
                                    TextButton(
                                        onClick = {
                                            runLiveCheck(settings.githubUpdateRepo, settings.githubUpdateChannel)
                                        }
                                    ) {
                                        Text(
                                            text = if (isSw) "Jaribu" else "Retry",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. System Telemetry 2x2 Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpgradeTelemetryTile(
                                icon = Icons.Filled.Verified,
                                label = if (isSw) "Toleo Kwenye Simu" else "Installed Build",
                                value = "v$cleanInstalled",
                                accent = primaryColor,
                                modifier = Modifier.weight(1f)
                            )
                            UpgradeTelemetryTile(
                                icon = Icons.Filled.Storage,
                                label = if (isSw) "Ukubwa wa Kifurushi" else "Package Size",
                                value = sizeStr.ifBlank { "APK" },
                                accent = tertiaryColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpgradeTelemetryTile(
                                icon = Icons.Filled.Security,
                                label = if (isSw) "Ulinzi wa Faili" else "File Integrity",
                                value = if (verifiedShaHex.isNotBlank()) verifiedShaHex else "SHA-256",
                                accent = primaryColor,
                                modifier = Modifier.weight(1f)
                            )
                            UpgradeTelemetryTile(
                                icon = Icons.Filled.Schedule,
                                label = if (isSw) "Ukaguzi wa Mwisho" else "Last Checked",
                                value = lastCheckedLabel,
                                accent = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. Channel, Auto-Check & Source Controls
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isSw) "Mkondo na Chanzo cha Maboresho" else "Upgrade Channel & Source",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val channels = listOf(
                                        GithubUpdateChannel.STABLE to (if (isSw) "Rasmi (Imara)" else "Stable"),
                                        GithubUpdateChannel.PRE_RELEASE to (if (isSw) "Majaribio (Beta)" else "Beta")
                                    )
                                    channels.forEach { (ch, label) ->
                                        val selected = settings.githubUpdateChannel == ch
                                        val bg by animateColorAsState(
                                            targetValue = if (selected) primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                            animationSpec = tween(20),
                                            label = "hub_ch_bg"
                                        )
                                        val fg by animateColorAsState(
                                            targetValue = if (selected) onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = tween(20),
                                            label = "hub_ch_fg"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 40.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(bg)
                                                .clickable {
                                                    onUpdateChannelChange(ch)
                                                    runLiveCheck(settings.githubUpdateRepo, ch)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("upgrade_hub_channel_${ch.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = fg,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isSw) "Kagua Kiotomatiki" else "Auto Check",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = settings.githubAutoCheckUpdates,
                                    onCheckedChange = onAutoCheckChange,
                                    modifier = Modifier.testTag("upgrade_hub_auto_check_switch")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable {
                                        repoDraft = settings.githubUpdateRepo
                                        tokenDraft = GithubUpdateChecker.getSavedGithubToken(context)
                                        showEditRepoDialog = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSw) "Chanzo cha Maboresho" else "Update Source",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = settings.githubUpdateRepo,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = if (isSw) "Badili chanzo" else "Edit source",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

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
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Release Notes / What's New
                if (activeRelease.bodyNotes.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isSw) "Yaliyomo Kwenye Toleo • ${activeRelease.releaseName}" else "What's New • ${activeRelease.releaseName}",
                                    style = MaterialTheme.typography.titleSmall,
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
                                                .padding(top = 6.dp)
                                                .size(6.dp)
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
                }

                // 7. Quick Package Actions (Save APK, Share APK, Open GitHub)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UpgradeQuickToolTile(
                            icon = Icons.Filled.SaveAlt,
                            label = if (isSw) "Hifadhi APK" else "Save APK",
                            enabled = !ctaBusy && (downloadedApkFile?.exists() == true || hasApkAsset),
                            modifier = Modifier.weight(1f),
                            onClick = {
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
                        UpgradeQuickToolTile(
                            icon = Icons.Filled.Share,
                            label = if (isSw) "Shiriki APK" else "Share APK",
                            enabled = !ctaBusy && (downloadedApkFile?.exists() == true || hasApkAsset),
                            modifier = Modifier.weight(1f),
                            onClick = {
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
                        UpgradeQuickToolTile(
                            icon = Icons.Filled.OpenInNew,
                            label = "GitHub",
                            enabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val url = activeRelease.htmlUrl.takeIf { it.isNotBlank() && !it.startsWith("local://") }
                                    ?: GithubUpdateChecker.buildRepoReleasesWebUrl(settings.githubUpdateRepo)
                                openExternalUrl(url)
                            }
                        )
                    }
                }

                // 8. Release History Card (if multiple GitHub releases are present)
                if (recentHistory.size > 1) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isSw) "Historia ya Matoleo" else "Release History",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                recentHistory.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 52.dp)
                                                .clickable(enabled = !ctaBusy) {
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
                        }
                    }
                }
            }
        }
    }

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
                        modifier = Modifier.fillMaxWidth()
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
                        TextButton(onClick = { repoDraft = GithubUpdateChecker.DEFAULT_GITHUB_REPO }) {
                            Text(if (isSw) "Rudisha Asili" else "Reset Default")
                        }
                        if (tokenDraft.isNotBlank()) {
                            TextButton(onClick = { tokenDraft = "" }) {
                                Text(if (isSw) "Futa Ufunguo" else "Clear Token")
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
                    }
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
}

@Composable
private fun UpgradeTelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpgradeQuickToolTile(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
