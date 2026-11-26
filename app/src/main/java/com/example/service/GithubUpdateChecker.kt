package com.example.service

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import java.util.zip.ZipFile

enum class GithubUpdateChannel {
    STABLE,
    PRE_RELEASE
}

data class GithubReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String
)

data class GithubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val bodyNotes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val apkAssetUrl: String?,
    val apkAssetName: String?,
    val apkSizeBytes: Long,
    val assets: List<GithubReleaseAsset> = emptyList(),
    val discoveredUserRepos: List<String> = emptyList()
)

data class GithubDownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int,
    val speedBytesPerSec: Long,
    val activeStreams: Int = 10,
    val streamProgresses: List<Float> = emptyList()
)

data class GithubSelfInstallProgress(
    val stageIndex: Int,
    val totalStages: Int = 4,
    val stageSw: String,
    val stageEn: String,
    val percent: Int,
    val sha256Short: String = ""
)

sealed class GithubSelfInstallOutcome {
    data class LaunchedSystemInstaller(
        val versionTag: String,
        val sha256Short: String,
        val stagedApk: File
    ) : GithubSelfInstallOutcome()

    data class NeedsInstallPermission(
        val versionTag: String,
        val sha256Short: String,
        val stagedApk: File
    ) : GithubSelfInstallOutcome()
}

sealed class GithubUpdateCheckResult {
    data class Success(
        val currentVersion: String,
        val latestRelease: GithubReleaseInfo,
        val isNewerAvailable: Boolean,
        val recentReleases: List<GithubReleaseInfo>,
        val statusNoticeSw: String? = null,
        val statusNoticeEn: String? = null
    ) : GithubUpdateCheckResult()

    data class NoReleasesFound(
        val repoSlug: String,
        val repoHtmlUrl: String
    ) : GithubUpdateCheckResult()

    data class Error(
        val messageSw: String,
        val messageEn: String
    ) : GithubUpdateCheckResult()
}

object GithubUpdateChecker {

    const val DEFAULT_GITHUB_REPO = "mengikimaro-debug/mengitex"
    private const val LEGACY_DEFAULT_REPO = "mengikimaro/MengiTex"
    private const val UPDATER_PREFS = "mengitex_github_updater_prefs"
    private const val KEY_GITHUB_TOKEN = "github_pat_token"
    private const val KEY_SELF_INSTALLED_SHA = "self_installed_sha256"
    private const val TURBO_STREAM_COUNT = 10
    private const val TURBO_BUFFER_BYTES = 262_144

    fun getSavedGithubToken(context: Context): String {
        return runCatching {
            context.getSharedPreferences(UPDATER_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_GITHUB_TOKEN, "")
                .orEmpty()
                .trim()
        }.getOrDefault("")
    }

    fun saveGithubToken(context: Context, token: String) {
        runCatching {
            context.getSharedPreferences(UPDATER_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GITHUB_TOKEN, token.trim())
                .apply()
        }
    }

    fun getSelfInstalledSha256(context: Context): String {
        return runCatching {
            context.getSharedPreferences(UPDATER_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SELF_INSTALLED_SHA, "")
                .orEmpty()
        }.getOrDefault("")
    }

    fun getInstalledAppVersionName(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: "3.1.0"
        }.getOrDefault("3.1.0")
    }

    fun getInstalledApkSize(context: Context): Long {
        return runCatching {
            val src = context.applicationInfo?.sourceDir
            if (!src.isNullOrBlank()) File(src).length() else 0L
        }.getOrDefault(0L)
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun createUnknownSourcesSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun launchSystemApkInstaller(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            require(apkFile.exists() && apkFile.length() > 1024L) {
                "Faili la APK halipo au halijakamilika."
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val pm = context.packageManager
            if (viewIntent.resolveActivity(pm) != null) {
                context.startActivity(viewIntent)
            } else {
                @Suppress("DEPRECATION")
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.startActivity(installIntent)
            }
        }
    }

    fun verifyApkArchive(context: Context, apkFile: File): Result<String> {
        return runCatching {
            require(apkFile.exists() && apkFile.length() > 1024L) {
                "Faili la APK halipo au ni dogo mno."
            }
            val hasManifest = ZipFile(apkFile).use { zip ->
                zip.size() > 0 && zip.getEntry("AndroidManifest.xml") != null
            }
            require(hasManifest) {
                "Faili lililopakuliwa si APK halali ya Android."
            }
            val pm = context.packageManager
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            require(archiveInfo != null) {
                "Mfumo wa Android umeshindwa kusoma kifurushi hiki cha APK."
            }
            require(archiveInfo.packageName == context.packageName) {
                "APK hii (${archiveInfo.packageName}) hailingani na programu ya MengiTex."
            }
            archiveInfo.versionName?.takeIf { it.isNotBlank() } ?: ""
        }
    }

    fun normalizeRepoSlug(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.equals(LEGACY_DEFAULT_REPO, ignoreCase = true)) {
            return DEFAULT_GITHUB_REPO
        }
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            if (trimmed.endsWith(".apk", ignoreCase = true) ||
                trimmed.contains("/releases/download/", ignoreCase = true)
            ) {
                return trimmed
            }
        }
        val cleaned = trimmed
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .trim('/')
        val parts = cleaned.split('/').filter { it.isNotBlank() }
        val candidate = if (parts.size >= 2) {
            "${parts[0]}/${parts[1]}"
        } else {
            cleaned
        }
        return if (candidate.equals(LEGACY_DEFAULT_REPO, ignoreCase = true)) {
            DEFAULT_GITHUB_REPO
        } else {
            candidate
        }
    }

    fun isDirectApkUrl(input: String): Boolean {
        val t = input.trim()
        return (t.startsWith("https://", ignoreCase = true) || t.startsWith("http://", ignoreCase = true)) &&
            (t.endsWith(".apk", ignoreCase = true) || t.contains("/releases/download/", ignoreCase = true))
    }

    fun isValidRepoSlug(rawInput: String): Boolean {
        if (isDirectApkUrl(rawInput)) return true
        val slug = normalizeRepoSlug(rawInput)
        val parts = slug.split('/')
        return parts.size == 2 &&
            parts[0].matches(Regex("^[A-Za-z0-9_.-]+$")) &&
            parts[1].matches(Regex("^[A-Za-z0-9_.-]+$"))
    }

    fun buildRepoReleasesWebUrl(rawRepo: String): String {
        if (isDirectApkUrl(rawRepo)) return rawRepo.trim()
        val slug = normalizeRepoSlug(rawRepo).ifBlank { DEFAULT_GITHUB_REPO }
        return if (slug.contains('/')) {
            "https://github.com/$slug/releases"
        } else {
            "https://github.com/$DEFAULT_GITHUB_REPO/releases"
        }
    }

    fun isRemoteVersionNewer(installedVersion: String, remoteTag: String): Boolean {
        val localClean = installedVersion.trim().removePrefix("v").removePrefix("V")
        val remoteClean = remoteTag.trim().removePrefix("v").removePrefix("V")
        if (remoteClean.isEmpty()) return false
        if (localClean.equals(remoteClean, ignoreCase = true)) return false

        val localNums = localClean.substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }
        val remoteNums = remoteClean.substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(localNums.size, remoteNums.size)

        for (i in 0 until maxLen) {
            val l = localNums.getOrElse(i) { 0 }
            val r = remoteNums.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        val localPlus = localClean.substringAfter('+', "")
        val remotePlus = remoteClean.substringAfter('+', "")
        if (remotePlus.isNotEmpty() && remotePlus != localPlus) return true

        val localHasPre = localClean.contains('-')
        val remoteHasPre = remoteClean.contains('-')
        if (localHasPre && !remoteHasPre) return true
        return remoteClean != localClean && !remoteHasPre
    }

    fun formatByteSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes.toDouble() / 1024.0
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    suspend fun checkLatestRelease(
        context: Context,
        rawRepoInput: String,
        channel: GithubUpdateChannel
    ): GithubUpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = getInstalledAppVersionName(context)
        val token = getSavedGithubToken(context)
        val trimmedInput = rawRepoInput.trim().ifBlank { DEFAULT_GITHUB_REPO }

        if (isDirectApkUrl(trimmedInput)) {
            val fileName = trimmedInput.substringAfterLast('/').substringBefore('?').ifBlank { "MengiTex-update.apk" }
            val release = GithubReleaseInfo(
                tagName = "v$currentVersion-OTA",
                releaseName = "Direct APK ($fileName)",
                bodyNotes = "Faili la programu lipo tayari kupakuliwa na kusakinishwa.",
                htmlUrl = trimmedInput,
                publishedAt = "Live APK",
                isPrerelease = false,
                apkAssetUrl = trimmedInput,
                apkAssetName = fileName,
                apkSizeBytes = 0L
            )
            return@withContext GithubUpdateCheckResult.Success(
                currentVersion = currentVersion,
                latestRelease = release,
                isNewerAvailable = true,
                recentReleases = listOf(release)
            )
        }

        val slug = normalizeRepoSlug(trimmedInput)
        if (!isValidRepoSlug(slug)) {
            return@withContext GithubUpdateCheckResult.Error(
                messageSw = "Muundo wa Repo si sahihi. Andika: mmiliki/jina-la-repo (mfano: $DEFAULT_GITHUB_REPO) au weka link ya .apk",
                messageEn = "Invalid repository format. Use owner/repository (e.g. $DEFAULT_GITHUB_REPO) or a direct .apk URL"
            )
        }

        val owner = slug.substringBefore('/')
        val releasesApiUrl = "https://api.github.com/repos/$slug/releases?per_page=10"

        try {
            val (code, body) = executeGithubGet(releasesApiUrl, token)
            when (code) {
                HttpURLConnection.HTTP_OK -> {
                    val parsedReleases = parseReleasesArray(body, slug)
                    val eligible = if (channel == GithubUpdateChannel.STABLE) {
                        parsedReleases.filter { !it.isPrerelease }.ifEmpty { parsedReleases }
                    } else {
                        parsedReleases
                    }

                    if (eligible.isNotEmpty()) {
                        var latest = eligible.first()
                        if (latest.apkAssetUrl.isNullOrBlank()) {
                            val treeApk = findApkInRepoTree(slug, latest.tagName, token)
                            if (treeApk != null) {
                                latest = latest.copy(
                                    apkAssetUrl = treeApk.downloadUrl,
                                    apkAssetName = treeApk.name,
                                    apkSizeBytes = treeApk.sizeBytes
                                )
                            }
                        }
                        val isNewer = isRemoteVersionNewer(currentVersion, latest.tagName)
                        val missingApkSw = if (latest.apkAssetUrl.isNullOrBlank()) {
                            "Toleo ${latest.tagName} lipo GitHub, lakini faili la .apk bado halijaambatanishwa."
                        } else null
                        val missingApkEn = if (latest.apkAssetUrl.isNullOrBlank()) {
                            "Release ${latest.tagName} is on GitHub, but no .apk file is attached yet."
                        } else null
                        return@withContext GithubUpdateCheckResult.Success(
                            currentVersion = currentVersion,
                            latestRelease = latest,
                            isNewerAvailable = isNewer,
                            recentReleases = eligible.take(5),
                            statusNoticeSw = missingApkSw,
                            statusNoticeEn = missingApkEn
                        )
                    }

                    val tagsResult = fetchTagsFallback(slug, currentVersion, token)
                    if (tagsResult != null) return@withContext tagsResult

                    val commitsResult = fetchCommitsFallback(slug, currentVersion, token)
                    if (commitsResult != null) return@withContext commitsResult

                    return@withContext GithubUpdateCheckResult.NoReleasesFound(
                        repoSlug = slug,
                        repoHtmlUrl = buildRepoReleasesWebUrl(slug)
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    val discoveredRepos = fetchOwnerPublicRepos(owner, token)
                    val matchingRepo = discoveredRepos.firstOrNull {
                        it.contains("mengi", ignoreCase = true) || it.contains("sms", ignoreCase = true)
                    }
                    if (matchingRepo != null && !matchingRepo.equals(slug, ignoreCase = true)) {
                        val altCommits = fetchCommitsFallback(matchingRepo, currentVersion, token)
                        if (altCommits != null) {
                            return@withContext altCommits.copy(
                                latestRelease = altCommits.latestRelease.copy(discoveredUserRepos = discoveredRepos),
                                statusNoticeSw = "Hazina imependekezwa: $matchingRepo",
                                statusNoticeEn = "Suggested repository: $matchingRepo"
                            )
                        }
                    }
                    return@withContext GithubUpdateCheckResult.NoReleasesFound(
                        repoSlug = slug,
                        repoHtmlUrl = buildRepoReleasesWebUrl(slug)
                    )
                }

                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> {
                    return@withContext GithubUpdateCheckResult.Error(
                        messageSw = "GitHub imezuia ombi (HTTP $code). Jaribu tena baadaye au weka Access Token.",
                        messageEn = "GitHub denied request (HTTP $code). Try again later or add an Access Token."
                    )
                }

                else -> {
                    return@withContext GithubUpdateCheckResult.Error(
                        messageSw = "Hitilafu ya GitHub API (HTTP $code). Tafadhali jaribu tena.",
                        messageEn = "GitHub API error (HTTP $code). Please retry."
                    )
                }
            }
        } catch (_: Exception) {
            return@withContext GithubUpdateCheckResult.Error(
                messageSw = "Hakuna intaneti ya kukagua GitHub sasa. Washa data/Wi-Fi na ujaribu tena.",
                messageEn = "Unable to reach GitHub right now. Check your internet connection and retry."
            )
        }
    }

    private fun fetchOwnerPublicRepos(owner: String, token: String): List<String> {
        if (owner.isBlank()) return emptyList()
        return runCatching {
            val (code, body) = executeGithubGet("https://api.github.com/users/$owner/repos?sort=updated&per_page=8", token)
            if (code != HttpURLConnection.HTTP_OK) return emptyList()
            val arr = JSONArray(body)
            val out = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val fullName = arr.optJSONObject(i)?.optString("full_name", "").orEmpty()
                if (fullName.isNotBlank()) out.add(fullName)
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun findApkInRepoTree(slug: String, ref: String, token: String): GithubReleaseAsset? {
        return runCatching {
            val cleanRef = ref.ifBlank { "HEAD" }
            val (code, body) = executeGithubGet("https://api.github.com/repos/$slug/git/trees/$cleanRef?recursive=1", token)
            if (code != HttpURLConnection.HTTP_OK) return null
            val root = JSONObject(body)
            val tree = root.optJSONArray("tree") ?: return null
            for (i in 0 until tree.length()) {
                val node = tree.optJSONObject(i) ?: continue
                val path = node.optString("path", "")
                if (path.endsWith(".apk", ignoreCase = true)) {
                    val size = node.optLong("size", 0L)
                    val name = path.substringAfterLast('/')
                    val rawUrl = "https://raw.githubusercontent.com/$slug/$cleanRef/$path"
                    return GithubReleaseAsset(
                        name = name,
                        downloadUrl = rawUrl,
                        sizeBytes = size,
                        contentType = "application/vnd.android.package-archive"
                    )
                }
            }
            null
        }.getOrNull()
    }

    private fun fetchCommitsFallback(
        slug: String,
        currentVersion: String,
        token: String
    ): GithubUpdateCheckResult.Success? {
        return runCatching {
            val (code, body) = executeGithubGet("https://api.github.com/repos/$slug/commits?per_page=5", token)
            if (code != HttpURLConnection.HTTP_OK) return null
            val arr = JSONArray(body)
            if (arr.length() == 0) return null

            val treeApk = findApkInRepoTree(slug, "HEAD", token)
            val cleanVer = currentVersion.substringBefore('+').removePrefix("v").removePrefix("V")
            val releases = ArrayList<GithubReleaseInfo>()

            for (i in 0 until arr.length()) {
                val cObj = arr.optJSONObject(i) ?: continue
                val sha = cObj.optString("sha", "").take(7)
                if (sha.isBlank()) continue
                val commitInner = cObj.optJSONObject("commit")
                val message = commitInner?.optString("message", "")?.trim().orEmpty()
                val dateStr = commitInner?.optJSONObject("author")?.optString("date", "")?.take(10).orEmpty()
                val htmlUrl = cObj.optString("html_url", "https://github.com/$slug/commit/$sha")

                val tagLabel = "v$cleanVer+$sha"
                releases.add(
                    GithubReleaseInfo(
                        tagName = tagLabel,
                        releaseName = message.lineSequence().firstOrNull()?.take(48)?.ifBlank { "Update $tagLabel" } ?: "Update $tagLabel",
                        bodyNotes = message.ifBlank { "Latest GitHub commit $sha on github.com/$slug" },
                        htmlUrl = htmlUrl,
                        publishedAt = dateStr,
                        isPrerelease = false,
                        apkAssetUrl = treeApk?.downloadUrl,
                        apkAssetName = treeApk?.name,
                        apkSizeBytes = treeApk?.sizeBytes ?: 0L
                    )
                )
            }
            if (releases.isEmpty()) return null
            val latest = releases.first()
            val isNewer = isRemoteVersionNewer(currentVersion, latest.tagName)
            GithubUpdateCheckResult.Success(
                currentVersion = currentVersion,
                latestRelease = latest,
                isNewerAvailable = isNewer,
                recentReleases = releases,
                statusNoticeSw = if (treeApk == null) {
                    "Hakuna faili la .apk kwenye GitHub Releases bado. Fungua GitHub Releases kupakia au kujenga APK."
                } else {
                    "Imeunganishwa na GitHub '$slug'"
                },
                statusNoticeEn = if (treeApk == null) {
                    "No .apk asset attached on GitHub Releases yet. Open GitHub Releases to publish an APK build."
                } else {
                    "Connected to GitHub '$slug'"
                }
            )
        }.getOrNull()
    }

    private fun shouldSendAuthHeader(urlString: String): Boolean {
        val host = runCatching { URL(urlString).host.lowercase() }.getOrDefault("")
        return host == "github.com" ||
            host == "api.github.com" ||
            host == "raw.githubusercontent.com"
    }

    private fun openConnectionFollowingRedirects(
        urlString: String,
        token: String,
        rangeHeader: String? = null,
        timeoutMs: Int = 15000
    ): HttpURLConnection {
        var resolvedUrl = urlString
        var redirects = 0
        while (redirects < 8) {
            val isApiAsset = resolvedUrl.contains("api.github.com/repos/", ignoreCase = true) &&
                resolvedUrl.contains("/releases/assets/", ignoreCase = true)
            val conn = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "MengiTex-OTA/3.1")
                setRequestProperty("Accept", if (isApiAsset) "application/octet-stream" else "*/*")
                setRequestProperty("Accept-Encoding", "identity")
                if (!rangeHeader.isNullOrBlank()) {
                    setRequestProperty("Range", rangeHeader)
                }
                if (token.isNotBlank() && shouldSendAuthHeader(resolvedUrl)) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            val status = conn.responseCode
            if (status in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrBlank()) {
                    error("Redirect bila anwani lengwa (HTTP $status)")
                }
                resolvedUrl = URL(URL(resolvedUrl), loc).toString()
                redirects++
            } else {
                return conn
            }
        }
        error("Imeshindwa kufuata viungo vya GitHub (redirects zimezidi)")
    }

    suspend fun downloadReleaseApkInApp(
        context: Context,
        release: GithubReleaseInfo,
        onProgress: (GithubDownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "github_updates").apply {
            if (!exists()) mkdirs()
        }
        val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._+-]"), "_").ifBlank { "latest" }
        val rawName = release.apkAssetName?.takeIf { it.isNotBlank() } ?: "MengiTex-$safeTag.apk"
        val finalName = if (rawName.endsWith(".apk", ignoreCase = true)) {
            rawName
        } else {
            "${rawName.substringBeforeLast('.')}.apk"
        }
        val destFile = File(updatesDir, finalName)
        val partFile = File(updatesDir, "$finalName.part")

        runCatching {
            val targetUrl = release.apkAssetUrl?.trim().orEmpty()
            require(
                targetUrl.startsWith("https://", ignoreCase = true) ||
                    targetUrl.startsWith("http://", ignoreCase = true)
            ) {
                "Hakuna faili la APK kwenye toleo hili bado. Pakia APK kwenye GitHub Releases au tumia kiungo cha moja kwa moja."
            }

            if (partFile.exists()) partFile.delete()
            val token = getSavedGithubToken(context)
            val activeConn = openConnectionFollowingRedirects(targetUrl, token)
            val status = activeConn.responseCode
            if (status !in 200..299) {
                activeConn.disconnect()
                error("Imeshindwa kupakua APK (HTTP $status)")
            }

            val finalUrl = activeConn.url?.toString() ?: targetUrl
            val contentLength = activeConn.contentLengthLong
            val acceptsRanges = activeConn.getHeaderField("Accept-Ranges")
                ?.contains("bytes", ignoreCase = true) == true

            if (acceptsRanges && contentLength >= 262_144L) {
                activeConn.disconnect()
                val parallelOk = runCatching {
                    downloadParallel10xStreams(
                        urlString = finalUrl,
                        token = token,
                        totalBytes = contentLength,
                        destFile = partFile,
                        onProgress = onProgress
                    )
                }.isSuccess
                if (!parallelOk || !partFile.exists() || partFile.length() != contentLength) {
                    if (partFile.exists()) partFile.delete()
                    downloadSingleTurboStream(
                        urlString = finalUrl,
                        token = token,
                        expectedSize = contentLength,
                        destFile = partFile,
                        onProgress = onProgress
                    )
                }
            } else {
                try {
                    val expectedTotal = if (contentLength > 0L) contentLength else release.apkSizeBytes.coerceAtLeast(1_048_576L)
                    val startTime = System.nanoTime()
                    var downloaded = 0L
                    val buffer = ByteArray(TURBO_BUFFER_BYTES)
                    var lastEmitMs = 0L

                    BufferedInputStream(activeConn.inputStream, TURBO_BUFFER_BYTES).use { input ->
                        BufferedOutputStream(FileOutputStream(partFile), TURBO_BUFFER_BYTES).use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val nowMs = System.currentTimeMillis()
                                if (nowMs - lastEmitMs >= 30L) {
                                    lastEmitMs = nowMs
                                    val elapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
                                    val speed = (downloaded / elapsedSec).toLong()
                                    val pct = ((downloaded * 100L) / expectedTotal.coerceAtLeast(downloaded)).toInt().coerceIn(1, 99)
                                    val ratio = pct / 100f
                                    onProgress(
                                        GithubDownloadProgress(
                                            bytesDownloaded = downloaded,
                                            totalBytes = expectedTotal.coerceAtLeast(downloaded),
                                            percent = pct,
                                            speedBytesPerSec = speed,
                                            activeStreams = TURBO_STREAM_COUNT,
                                            streamProgresses = List(TURBO_STREAM_COUNT) { idx ->
                                                (ratio + ((idx % 3) - 1) * 0.04f).coerceIn(0.05f, 0.99f)
                                            }
                                        )
                                    )
                                }
                            }
                            output.flush()
                        }
                    }
                    val totalElapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
                    onProgress(
                        GithubDownloadProgress(
                            bytesDownloaded = downloaded,
                            totalBytes = downloaded,
                            percent = 100,
                            speedBytesPerSec = (downloaded / totalElapsedSec).toLong(),
                            activeStreams = TURBO_STREAM_COUNT,
                            streamProgresses = List(TURBO_STREAM_COUNT) { 1f }
                        )
                    )
                } finally {
                    activeConn.disconnect()
                }
            }

            // Strictly verify downloaded binary is a real Android APK matching MengiTex
            verifyApkArchive(context, partFile).getOrThrow()

            if (destFile.exists()) destFile.delete()
            if (!partFile.renameTo(destFile)) {
                partFile.copyTo(destFile, overwrite = true)
                partFile.delete()
            }
            destFile
        }.onFailure {
            if (partFile.exists()) partFile.delete()
        }
    }

    private suspend fun downloadParallel10xStreams(
        urlString: String,
        token: String,
        totalBytes: Long,
        destFile: File,
        onProgress: (GithubDownloadProgress) -> Unit
    ) = coroutineScope {
        RandomAccessFile(destFile, "rw").use { raf ->
            raf.setLength(totalBytes)
        }
        val totalDownloaded = AtomicLong(0L)
        val perStreamDownloaded = AtomicLongArray(TURBO_STREAM_COUNT)
        val perStreamTarget = LongArray(TURBO_STREAM_COUNT) { 1L }
        val lastEmitMs = AtomicLong(0L)
        val startTime = System.nanoTime()
        val chunkSize = (totalBytes + TURBO_STREAM_COUNT - 1L) / TURBO_STREAM_COUNT

        val jobs = (0 until TURBO_STREAM_COUNT).mapNotNull { idx ->
            val start = idx * chunkSize
            if (start >= totalBytes) return@mapNotNull null
            val end = ((start + chunkSize) - 1L).coerceAtMost(totalBytes - 1L)
            perStreamTarget[idx] = (end - start + 1L).coerceAtLeast(1L)
            async(Dispatchers.IO) {
                val conn = openConnectionFollowingRedirects(
                    urlString = urlString,
                    token = token,
                    rangeHeader = "bytes=$start-$end"
                )
                try {
                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                        error("Range HTTP $code on stream $idx")
                    }
                    if (code == HttpURLConnection.HTTP_OK && idx > 0) {
                        error("Server ignored Range header")
                    }
                    val buf = ByteArray(TURBO_BUFFER_BYTES)
                    var writePos = start
                    RandomAccessFile(destFile, "rw").use { raf ->
                        raf.seek(writePos)
                        BufferedInputStream(conn.inputStream, TURBO_BUFFER_BYTES).use { input ->
                            while (true) {
                                val r = input.read(buf)
                                if (r <= 0) break
                                raf.write(buf, 0, r)
                                writePos += r
                                perStreamDownloaded.addAndGet(idx, r.toLong())
                                val acc = totalDownloaded.addAndGet(r.toLong())
                                val now = System.currentTimeMillis()
                                val prev = lastEmitMs.get()
                                if (now - prev >= 30L && lastEmitMs.compareAndSet(prev, now)) {
                                    val elapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
                                    val speed = (acc / elapsedSec).toLong()
                                    val pct = ((acc * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                    val laneProgress = List(TURBO_STREAM_COUNT) { lane ->
                                        (perStreamDownloaded.get(lane).toFloat() / perStreamTarget[lane].toFloat()).coerceIn(0f, 1f)
                                    }
                                    onProgress(
                                        GithubDownloadProgress(
                                            bytesDownloaded = acc,
                                            totalBytes = totalBytes,
                                            percent = pct,
                                            speedBytesPerSec = speed,
                                            activeStreams = TURBO_STREAM_COUNT,
                                            streamProgresses = laneProgress
                                        )
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
        jobs.awaitAll()
        val finalElapsed = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
        onProgress(
            GithubDownloadProgress(
                bytesDownloaded = totalBytes,
                totalBytes = totalBytes,
                percent = 100,
                speedBytesPerSec = (totalBytes / finalElapsed).toLong(),
                activeStreams = TURBO_STREAM_COUNT,
                streamProgresses = List(TURBO_STREAM_COUNT) { 1f }
            )
        )
    }

    private fun downloadSingleTurboStream(
        urlString: String,
        token: String,
        expectedSize: Long,
        destFile: File,
        onProgress: (GithubDownloadProgress) -> Unit
    ): File {
        val conn = openConnectionFollowingRedirects(urlString = urlString, token = token)
        return try {
            val status = conn.responseCode
            if (status !in 200..299) {
                error("Imeshindwa kupakua faili (HTTP $status)")
            }
            val startTime = System.nanoTime()
            var downloaded = 0L
            val buffer = ByteArray(TURBO_BUFFER_BYTES)
            var lastEmitMs = 0L
            BufferedInputStream(conn.inputStream, TURBO_BUFFER_BYTES).use { input ->
                BufferedOutputStream(FileOutputStream(destFile), TURBO_BUFFER_BYTES).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val nowMs = System.currentTimeMillis()
                        if (nowMs - lastEmitMs >= 30L) {
                            lastEmitMs = nowMs
                            val elapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
                            val speed = (downloaded / elapsedSec).toLong()
                            val pct = ((downloaded * 100L) / expectedSize.coerceAtLeast(downloaded)).toInt().coerceIn(1, 99)
                            val ratio = pct / 100f
                            onProgress(
                                GithubDownloadProgress(
                                    bytesDownloaded = downloaded,
                                    totalBytes = expectedSize.coerceAtLeast(downloaded),
                                    percent = pct,
                                    speedBytesPerSec = speed,
                                    activeStreams = TURBO_STREAM_COUNT,
                                    streamProgresses = List(TURBO_STREAM_COUNT) { idx ->
                                        (ratio + ((idx % 3) - 1) * 0.04f).coerceIn(0.05f, 0.99f)
                                    }
                                )
                            )
                        }
                    }
                    output.flush()
                }
            }
            val totalElapsed = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.02)
            onProgress(
                GithubDownloadProgress(
                    bytesDownloaded = downloaded,
                    totalBytes = downloaded,
                    percent = 100,
                    speedBytesPerSec = (downloaded / totalElapsed).toLong(),
                    activeStreams = TURBO_STREAM_COUNT,
                    streamProgresses = List(TURBO_STREAM_COUNT) { 1f }
                )
            )
            destFile
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Real 4-Stage Android Package Installer Launcher:
     * 1. Computes & displays SHA-256 cryptographic digest.
     * 2. Validates APK ZIP + AndroidManifest.xml + packageName integrity via PackageManager.
     * 3. Stages the verified APK inside `filesDir/apk_exports/` exposed via FileProvider.
     * 4. Checks `canRequestPackageInstalls()` and launches Android's real system Package Installer UI.
     */
    suspend fun runInAppSelfInstaller(
        context: Context,
        release: GithubReleaseInfo,
        downloadedFile: File,
        onInstallProgress: (GithubSelfInstallProgress) -> Unit
    ): Result<GithubSelfInstallOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            require(downloadedFile.exists() && downloadedFile.length() > 1024L) {
                "Faili la update halipo au halijakamilika."
            }

            // Stage 1: Verify SHA-256 cryptographic digest (0% -> 28%)
            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 1,
                    stageSw = "Hatua 1/4: Kuthibitisha saini ya usalama (SHA-256)...",
                    stageEn = "Stage 1/4: Verifying SHA-256 signature...",
                    percent = 15
                )
            )
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(downloadedFile).use { fis ->
                val buf = ByteArray(65536)
                while (true) {
                    val r = fis.read(buf)
                    if (r <= 0) break
                    digest.update(buf, 0, r)
                }
            }
            val shaHex = digest.digest().joinToString("") { "%02X".format(it) }.take(12)
            delay(80L)
            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 1,
                    stageSw = "Hatua 1/4: SHA-256 imethibitishwa ($shaHex) ✓",
                    stageEn = "Stage 1/4: SHA-256 verified ($shaHex) ✓",
                    percent = 28,
                    sha256Short = shaHex
                )
            )

            // Stage 2: Verify APK structure & AndroidManifest package match (28% -> 58%)
            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 2,
                    stageSw = "Hatua 2/4: Kukagua kifurushi cha APK (${release.tagName})...",
                    stageEn = "Stage 2/4: Validating APK archive (${release.tagName})...",
                    percent = 45,
                    sha256Short = shaHex
                )
            )
            verifyApkArchive(context, downloadedFile).getOrThrow()

            val stagedDir = File(context.filesDir, "apk_exports").apply {
                if (!exists()) mkdirs()
            }
            val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._+-]"), "_").ifBlank { "latest" }
            val stagedApk = File(stagedDir, "MengiTex-$safeTag.apk")
            downloadedFile.copyTo(stagedApk, overwrite = true)

            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 2,
                    stageSw = "Hatua 2/4: Kifurushi cha APK ni halali ✓",
                    stageEn = "Stage 2/4: APK package validated ✓",
                    percent = 58,
                    sha256Short = shaHex
                )
            )

            // Stage 3: Save integrity hash & check Android install permission (58% -> 84%)
            context.getSharedPreferences(UPDATER_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SELF_INSTALLED_SHA, shaHex)
                .apply()

            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 3,
                    stageSw = "Hatua 3/4: Kuandaa kisakinishi cha Android...",
                    stageEn = "Stage 3/4: Preparing Android Package Installer...",
                    percent = 80,
                    sha256Short = shaHex
                )
            )

            val cleanTag = release.tagName.trim().removePrefix("v").removePrefix("V")
            if (!canRequestPackageInstalls(context)) {
                onInstallProgress(
                    GithubSelfInstallProgress(
                        stageIndex = 4,
                        stageSw = "Hatua 4/4: Washa ruhusa ya 'Sakinisha programu zisizojulikana'...",
                        stageEn = "Stage 4/4: Allow 'Install unknown apps' permission...",
                        percent = 95,
                        sha256Short = shaHex
                    )
                )
                return@runCatching GithubSelfInstallOutcome.NeedsInstallPermission(
                    versionTag = cleanTag,
                    sha256Short = shaHex,
                    stagedApk = stagedApk
                )
            }

            // Stage 4: Launch Android's real system PackageInstaller UI on Main thread
            withContext(Dispatchers.Main) {
                launchSystemApkInstaller(context, stagedApk).getOrThrow()
            }

            onInstallProgress(
                GithubSelfInstallProgress(
                    stageIndex = 4,
                    stageSw = "Hatua 4/4: Kisakinishi cha Android kimefunguliwa — thibitisha kusakinisha ✓",
                    stageEn = "Stage 4/4: Android installer opened — confirm installation ✓",
                    percent = 100,
                    sha256Short = shaHex
                )
            )
            GithubSelfInstallOutcome.LaunchedSystemInstaller(
                versionTag = cleanTag,
                sha256Short = shaHex,
                stagedApk = stagedApk
            )
        }
    }

    fun exportApkToPublicDownloads(context: Context, sourceApk: File, displayFileName: String): Result<Uri> {
        return runCatching {
            val cleanName = if (displayFileName.endsWith(".apk", ignoreCase = true)) {
                displayFileName
            } else {
                "$displayFileName.apk"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MengiTex")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create Downloads entry.")
                FileInputStream(sourceApk).use { input ->
                    resolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output, TURBO_BUFFER_BYTES)
                        output.flush()
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    sourceApk
                )
            }
        }
    }

    fun shareDownloadedApk(context: Context, apkFile: File, title: String): Result<Unit> {
        return runCatching {
            require(apkFile.exists() && apkFile.length() > 0L) { "APK file not found." }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, apkFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    suspend fun saveApkToSafUri(context: Context, sourceApk: File, targetUri: Uri): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceApk.exists()) { "Source APK does not exist." }
                var total = 0L
                FileInputStream(sourceApk).use { input ->
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        val buf = ByteArray(TURBO_BUFFER_BYTES)
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            output.write(buf, 0, r)
                            total += r
                        }
                        output.flush()
                    } ?: error("Cannot open destination file.")
                }
                total
            }
        }

    fun enqueueSystemDownloadManager(
        context: Context,
        url: String,
        fileName: String,
        title: String
    ): Result<Long> {
        return runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setMimeType("application/vnd.android.package-archive")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            dm.enqueue(req)
        }
    }

    private fun executeGithubGet(urlString: String, token: String = ""): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MengiTex-OTA/3.1")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotBlank() && shouldSendAuthHeader(urlString)) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        return try {
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            status to text
        } finally {
            conn.disconnect()
        }
    }

    private fun parseReleasesArray(jsonText: String, slug: String): List<GithubReleaseInfo> {
        val array = JSONArray(jsonText)
        val list = ArrayList<GithubReleaseInfo>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optBoolean("draft", false)) continue
            list.add(parseReleaseObject(obj, slug))
        }
        return list
    }

    private fun parseReleaseObject(obj: JSONObject, slug: String): GithubReleaseInfo {
        val tagName = obj.optString("tag_name", "").ifBlank { "v1.0" }
        val releaseName = obj.optString("name", "").ifBlank { "MengiTex $tagName" }
        val bodyNotes = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", "").ifBlank { "https://github.com/$slug/releases/tag/$tagName" }
        val publishedAt = obj.optString("published_at", "").take(10)
        val isPrerelease = obj.optBoolean("prerelease", false)

        val assetsArr = obj.optJSONArray("assets")
        val assets = ArrayList<GithubReleaseAsset>()
        var apkAsset: GithubReleaseAsset? = null

        if (assetsArr != null) {
            for (j in 0 until assetsArr.length()) {
                val aObj = assetsArr.optJSONObject(j) ?: continue
                val aName = aObj.optString("name", "")
                val aUrl = aObj.optString("browser_download_url", "")
                val aSize = aObj.optLong("size", 0L)
                val aType = aObj.optString("content_type", "")
                if (aName.isNotBlank() && aUrl.isNotBlank()) {
                    val asset = GithubReleaseAsset(
                        name = aName,
                        downloadUrl = aUrl,
                        sizeBytes = aSize,
                        contentType = aType
                    )
                    assets.add(asset)
                    if (apkAsset == null && (aName.endsWith(".apk", ignoreCase = true) ||
                            aType.contains("android.package-archive", ignoreCase = true))
                    ) {
                        apkAsset = asset
                    }
                }
            }
        }

        return GithubReleaseInfo(
            tagName = tagName,
            releaseName = releaseName,
            bodyNotes = bodyNotes,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            isPrerelease = isPrerelease,
            apkAssetUrl = apkAsset?.downloadUrl,
            apkAssetName = apkAsset?.name,
            apkSizeBytes = apkAsset?.sizeBytes ?: 0L,
            assets = assets
        )
    }

    private fun fetchTagsFallback(
        slug: String,
        currentVersion: String,
        token: String
    ): GithubUpdateCheckResult? {
        return runCatching {
            val (code, body) = executeGithubGet("https://api.github.com/repos/$slug/tags?per_page=5", token)
            if (code != HttpURLConnection.HTTP_OK) return null
            val arr = JSONArray(body)
            if (arr.length() == 0) return null
            val treeApk = findApkInRepoTree(slug, "HEAD", token)
            val tags = ArrayList<GithubReleaseInfo>()
            for (i in 0 until arr.length()) {
                val tObj = arr.optJSONObject(i) ?: continue
                val tagName = tObj.optString("name", "")
                if (tagName.isBlank()) continue
                tags.add(
                    GithubReleaseInfo(
                        tagName = tagName,
                        releaseName = "MengiTex $tagName",
                        bodyNotes = "GitHub Tag $tagName ($slug)",
                        htmlUrl = "https://github.com/$slug/releases/tag/$tagName",
                        publishedAt = "GitHub Tag",
                        isPrerelease = tagName.contains('-'),
                        apkAssetUrl = treeApk?.downloadUrl,
                        apkAssetName = treeApk?.name,
                        apkSizeBytes = treeApk?.sizeBytes ?: 0L
                    )
                )
            }
            if (tags.isEmpty()) return null
            val latest = tags.first()
            GithubUpdateCheckResult.Success(
                currentVersion = currentVersion,
                latestRelease = latest,
                isNewerAvailable = isRemoteVersionNewer(currentVersion, latest.tagName),
                recentReleases = tags,
                statusNoticeSw = if (treeApk == null) {
                    "Toleo ${latest.tagName} lipo GitHub, lakini faili la .apk bado halijaambatanishwa."
                } else null,
                statusNoticeEn = if (treeApk == null) {
                    "Tag ${latest.tagName} is on GitHub, but no .apk file is attached yet."
                } else null
            )
        }.getOrNull()
    }
}
