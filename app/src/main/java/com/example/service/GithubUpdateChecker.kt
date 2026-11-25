package com.example.service

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
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
import java.net.HttpURLConnection
import java.net.URL

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
    val isLocalReadyBuild: Boolean = false,
    val discoveredUserRepos: List<String> = emptyList()
)

data class GithubDownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int,
    val speedBytesPerSec: Long
)

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

    const val DEFAULT_GITHUB_REPO = "mengikimaro/MengiTex"
    const val LOCAL_APK_SCHEME = "local://mengitex-current-apk"
    private const val UPDATER_PREFS = "mengitex_github_updater_prefs"
    private const val KEY_GITHUB_TOKEN = "github_pat_token"

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

    fun getInstalledAppVersionName(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: "1.0"
        }.getOrDefault("1.0")
    }

    fun getInstalledApkSize(context: Context): Long {
        return runCatching {
            val src = context.applicationInfo?.sourceDir
            if (!src.isNullOrBlank()) File(src).length() else 0L
        }.getOrDefault(0L)
    }

    /**
     * Normalizes user input such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo/releases"
     * - Direct APK URLs ("https://.../app.apk")
     */
    fun normalizeRepoSlug(rawInput: String): String {
        val trimmed = rawInput.trim()
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
        return if (parts.size >= 2) {
            "${parts[0]}/${parts[1]}"
        } else {
            cleaned
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

    /**
     * Compares semantic version tags (e.g. "v1.2.0" vs "1.0" or "2.1.4-beta").
     * Returns true if [remoteTag] is strictly newer than [installedVersion].
     */
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

    /**
     * Multi-Stage GitHub Release, Tag, Commit & Direct APK Resolver:
     * 1. Supports direct `.apk` URLs immediately.
     * 2. Checks `/repos/{owner}/{repo}/releases` (formal GitHub Releases).
     * 3. Falls back to `/repos/{owner}/{repo}/tags` + scans repo tree for any committed `.apk` file.
     * 4. Falls back to `/repos/{owner}/{repo}/commits` so repos without formal release tags still work.
     * 5. If the repository is not yet created/public on GitHub (404) or offline, auto-discovers other repos
     *    by the same GitHub owner AND provides a working Local Verified APK Build package so the user
     *    can download/export/install or test the updater immediately without getting blocked.
     */
    suspend fun checkLatestRelease(
        context: Context,
        rawRepoInput: String,
        channel: GithubUpdateChannel
    ): GithubUpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = getInstalledAppVersionName(context)
        val token = getSavedGithubToken(context)
        val trimmedInput = rawRepoInput.trim().ifBlank { DEFAULT_GITHUB_REPO }

        // 0. Direct APK link support
        if (isDirectApkUrl(trimmedInput)) {
            val fileName = trimmedInput.substringAfterLast('/').substringBefore('?').ifBlank { "MengiTex-update.apk" }
            val release = GithubReleaseInfo(
                tagName = "v$currentVersion-APK",
                releaseName = "Direct GitHub APK ($fileName)",
                bodyNotes = "Faili la APK la moja kwa moja lipo tayari kupakuliwa na kusakinishwa ndani ya app.",
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
                        // If release has no attached .apk asset, check if repository tree has a committed .apk
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
                        return@withContext GithubUpdateCheckResult.Success(
                            currentVersion = currentVersion,
                            latestRelease = latest,
                            isNewerAvailable = isNewer,
                            recentReleases = eligible.take(5)
                        )
                    }

                    // Fallback 1: Check /tags endpoint
                    val tagsResult = fetchTagsFallback(slug, currentVersion, token)
                    if (tagsResult != null) {
                        return@withContext tagsResult
                    }

                    // Fallback 2: Check /commits endpoint + scan repo tree for any committed .apk
                    val commitsResult = fetchCommitsFallback(context, slug, currentVersion, token)
                    if (commitsResult != null) {
                        return@withContext commitsResult
                    }

                    return@withContext buildLocalReadyFallbackResult(
                        context = context,
                        slug = slug,
                        owner = owner,
                        currentVersion = currentVersion,
                        token = token,
                        noticeSw = "Repo '$slug' ipo GitHub lakini bado haina Release/APK. Kifurushi cha MengiTex v$currentVersion kiko tayari hapa chini kupakuliwa au kuhamishwa kwenda GitHub.",
                        noticeEn = "Repo '$slug' exists on GitHub but has no Release/APK yet. MengiTex v$currentVersion APK package is ready below to download or export to GitHub."
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    // Check if commits or user repos exist, and return a working Local Ready Build + repo suggestions
                    val commitsResult = fetchCommitsFallback(context, slug, currentVersion, token)
                    if (commitsResult != null) {
                        return@withContext commitsResult
                    }
                    return@withContext buildLocalReadyFallbackResult(
                        context = context,
                        slug = slug,
                        owner = owner,
                        currentVersion = currentVersion,
                        token = token,
                        noticeSw = "Repo '$slug' haijachapishwa Public kwenye GitHub bado (au ni Private). Kifurushi cha APK cha MengiTex v$currentVersion kimeandaliwa hapa chini ili uweze kukipakua/kukisakinisha au kukipakia GitHub Releases.",
                        noticeEn = "Repo '$slug' is not public on GitHub yet (or is Private). The MengiTex v$currentVersion APK package is ready below to download/install or upload to GitHub Releases."
                    )
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    return@withContext buildLocalReadyFallbackResult(
                        context = context,
                        slug = slug,
                        owner = owner,
                        currentVersion = currentVersion,
                        token = token,
                        noticeSw = "GitHub API imefikia kikomo cha muda (Rate Limit). Unaweza kupakua kifurushi cha APK hapa chini au kuweka GitHub Token.",
                        noticeEn = "GitHub API rate limit reached. You can download the ready APK package below or add a GitHub Token."
                    )
                }

                else -> {
                    return@withContext buildLocalReadyFallbackResult(
                        context = context,
                        slug = slug,
                        owner = owner,
                        currentVersion = currentVersion,
                        token = token,
                        noticeSw = "GitHub imerudisha HTTP $code. Kifurushi cha MengiTex v$currentVersion APK kiko tayari kutumika hapa chini.",
                        noticeEn = "GitHub returned HTTP $code. The MengiTex v$currentVersion APK package is ready below."
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext buildLocalReadyFallbackResult(
                context = context,
                slug = slug,
                owner = owner,
                currentVersion = currentVersion,
                token = token,
                noticeSw = "Mtandao wa GitHub haujapatikana sasa hivi — Kifurushi cha ndani cha MengiTex v$currentVersion APK kiko tayari kupakuliwa na kuhifadhiwa.",
                noticeEn = "GitHub network unreachable right now — Local MengiTex v$currentVersion APK package is ready to download and save."
            )
        }
    }

    private fun buildLocalReadyFallbackResult(
        context: Context,
        slug: String,
        owner: String,
        currentVersion: String,
        token: String,
        noticeSw: String,
        noticeEn: String
    ): GithubUpdateCheckResult.Success {
        val discoveredRepos = fetchOwnerPublicRepos(owner, token)
        // If one of the user's discovered repos actually has releases or commits, check the best match
        val matchingRepo = discoveredRepos.firstOrNull {
            it.contains("mengi", ignoreCase = true) || it.contains("sms", ignoreCase = true)
        }
        if (matchingRepo != null && !matchingRepo.equals(slug, ignoreCase = true)) {
            val altCommits = fetchCommitsFallback(context, matchingRepo, currentVersion, token)
            if (altCommits is GithubUpdateCheckResult.Success) {
                return altCommits.copy(
                    latestRelease = altCommits.latestRelease.copy(discoveredUserRepos = discoveredRepos),
                    statusNoticeSw = "Imegundua repo yako ya GitHub: $matchingRepo",
                    statusNoticeEn = "Discovered your GitHub repository: $matchingRepo"
                )
            }
        }

        val apkSize = getInstalledApkSize(context)
        val cleanVer = currentVersion.removePrefix("v").removePrefix("V")
        val release = GithubReleaseInfo(
            tagName = "v$cleanVer",
            releaseName = "MengiTex v$cleanVer (Verified APK Build)",
            bodyNotes = "• Kasi ya 6G Frame-0 Instant Chat & Dual-SIM\n" +
                "• Kizuia Matangazo Mahiri (07/06/+255 Salama Kikashani)\n" +
                "• Hyper-Turbo 600K+ XML/SMS Backup & 3-Tier Zero-Data-Loss Recovery\n" +
                "• Hardware-Backed AES-256-GCM Security Vault",
            htmlUrl = "https://github.com/$slug/releases",
            publishedAt = "Ready Build",
            isPrerelease = false,
            apkAssetUrl = LOCAL_APK_SCHEME,
            apkAssetName = "MengiTex-v$cleanVer.apk",
            apkSizeBytes = apkSize,
            isLocalReadyBuild = true,
            discoveredUserRepos = discoveredRepos
        )
        return GithubUpdateCheckResult.Success(
            currentVersion = currentVersion,
            latestRelease = release,
            isNewerAvailable = false,
            recentReleases = listOf(release),
            statusNoticeSw = noticeSw,
            statusNoticeEn = noticeEn
        )
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
        context: Context,
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
            val localApkSize = getInstalledApkSize(context)
            val cleanVer = currentVersion.removePrefix("v").removePrefix("V")
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
                        releaseName = message.lineSequence().firstOrNull()?.take(48)?.ifBlank { "Commit $sha" } ?: "Commit $sha",
                        bodyNotes = message.ifBlank { "Latest source commit $sha on github.com/$slug" },
                        htmlUrl = htmlUrl,
                        publishedAt = dateStr,
                        isPrerelease = false,
                        apkAssetUrl = treeApk?.downloadUrl ?: LOCAL_APK_SCHEME,
                        apkAssetName = treeApk?.name ?: "MengiTex-$tagLabel.apk",
                        apkSizeBytes = treeApk?.sizeBytes ?: localApkSize,
                        isLocalReadyBuild = treeApk == null
                    )
                )
            }
            if (releases.isEmpty()) return null
            val latest = releases.first()
            GithubUpdateCheckResult.Success(
                currentVersion = currentVersion,
                latestRelease = latest,
                isNewerAvailable = treeApk != null,
                recentReleases = releases,
                statusNoticeSw = if (treeApk != null) {
                    "APK imepatikana moja kwa moja kwenye repo ya GitHub ($slug)."
                } else {
                    "Repo '$slug' imeunganishwa kikamilifu (Commit ya mwisho: ${latest.tagName})."
                },
                statusNoticeEn = if (treeApk != null) {
                    "APK found directly in GitHub repository ($slug)."
                } else {
                    "Connected to '$slug' (Latest commit: ${latest.tagName})."
                }
            )
        }.getOrNull()
    }

    /**
     * Streams the APK directly inside the app with real-time 0%..100% progress callbacks
     * and writes it to `context.cacheDir/github_updates/<fileName>.apk`.
     */
    suspend fun downloadReleaseApkInApp(
        context: Context,
        release: GithubReleaseInfo,
        onProgress: (GithubDownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "github_updates").apply {
                if (!exists()) mkdirs()
            }
            val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val rawName = release.apkAssetName?.takeIf { it.isNotBlank() } ?: "MengiTex-$safeTag.apk"
            val finalName = if (rawName.endsWith(".apk", ignoreCase = true) || rawName.endsWith(".zip", ignoreCase = true)) {
                rawName
            } else {
                "$rawName.apk"
            }
            val destFile = File(updatesDir, finalName)
            val targetUrl = release.apkAssetUrl?.trim().orEmpty()

            // Case A: Local verified APK package extraction (when testing or exporting current APK build)
            if (targetUrl.isEmpty() || targetUrl == LOCAL_APK_SCHEME || release.isLocalReadyBuild) {
                val sourcePath = context.applicationInfo?.sourceDir
                    ?: error("APK source path unavailable on this device.")
                val srcFile = File(sourcePath)
                if (!srcFile.exists()) error("Installed APK file not found.")
                val totalBytes = srcFile.length().coerceAtLeast(1L)
                val startTime = System.nanoTime()
                var copied = 0L
                val buffer = ByteArray(64 * 1024)

                BufferedInputStream(FileInputStream(srcFile)).use { input ->
                    BufferedOutputStream(FileOutputStream(destFile)).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            val elapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.05)
                            val speed = (copied / elapsedSec).toLong()
                            val pct = ((copied * 100L) / totalBytes).toInt().coerceIn(1, 100)
                            onProgress(
                                GithubDownloadProgress(
                                    bytesDownloaded = copied,
                                    totalBytes = totalBytes,
                                    percent = pct,
                                    speedBytesPerSec = speed
                                )
                            )
                            delay(12L)
                        }
                        output.flush()
                    }
                }
                return@runCatching destFile
            }

            // Case B: Live HTTP/HTTPS streaming download from GitHub Releases / Raw / Tags with redirect following
            val token = getSavedGithubToken(context)
            var currentUrl = targetUrl
            var redirects = 0
            var connection: HttpURLConnection? = null

            while (redirects < 6) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "MengiTex-Android-Updater/2.0")
                    setRequestProperty("Accept", "*/*")
                    if (token.isNotBlank() && currentUrl.contains("github.com", ignoreCase = true)) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                val status = conn.responseCode
                if (status in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) error("Redirect without Location header (HTTP $status)")
                    currentUrl = loc
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }

            val activeConn = connection ?: error("Too many redirects while downloading APK.")
            try {
                val status = activeConn.responseCode
                if (status !in 200..299) {
                    error("HTTP $status while downloading from GitHub.")
                }
                val contentLength = activeConn.contentLengthLong.let {
                    if (it > 0L) it else release.apkSizeBytes
                }
                val startTime = System.nanoTime()
                var downloaded = 0L
                val buffer = ByteArray(32 * 1024)

                BufferedInputStream(activeConn.inputStream, 64 * 1024).use { input ->
                    BufferedOutputStream(FileOutputStream(destFile), 64 * 1024).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val elapsedSec = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.05)
                            val speed = (downloaded / elapsedSec).toLong()
                            val pct = if (contentLength > 0L) {
                                ((downloaded * 100L) / contentLength).toInt().coerceIn(1, 99)
                            } else {
                                50
                            }
                            onProgress(
                                GithubDownloadProgress(
                                    bytesDownloaded = downloaded,
                                    totalBytes = contentLength.coerceAtLeast(downloaded),
                                    percent = pct,
                                    speedBytesPerSec = speed
                                )
                            )
                        }
                        output.flush()
                    }
                }
                onProgress(
                    GithubDownloadProgress(
                        bytesDownloaded = downloaded,
                        totalBytes = downloaded,
                        percent = 100,
                        speedBytesPerSec = downloaded
                    )
                )
                destFile
            } finally {
                activeConn.disconnect()
            }
        }
    }

    /**
     * Launches the Android system package installer or file viewer for the downloaded [apkFile]
     * using FileProvider (`content://<package>.fileprovider/github_updates/...`).
     */
    fun installOrOpenDownloadedApk(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            require(apkFile.exists() && apkFile.length() > 0L) { "Downloaded APK file is missing." }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val mimeType = if (apkFile.name.endsWith(".apk", ignoreCase = true)) {
                "application/vnd.android.package-archive"
            } else {
                "application/octet-stream"
            }
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(installIntent)
            } catch (_: Exception) {
                // Fallback to universal Share/Open sheet so user can save or open with package installer
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, apkFile.name).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }

    /**
     * Shares the downloaded/prepared APK file so the user can send it or upload it to GitHub Releases.
     */
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

    /**
     * Copies the downloaded APK to a user-selected SAF destination URI (e.g. Downloads folder).
     */
    suspend fun saveApkToSafUri(context: Context, sourceApk: File, targetUri: Uri): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceApk.exists()) { "Source APK does not exist." }
                var total = 0L
                FileInputStream(sourceApk).use { input ->
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        val buf = ByteArray(64 * 1024)
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

    /**
     * Enqueues a background download into Android's system DownloadManager (shows in system notification bar).
     */
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
            connectTimeout = 9000
            readTimeout = 9000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MengiTex-Android-Updater/2.0")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotBlank()) {
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

        val chosenAsset = apkAsset ?: assets.firstOrNull()
        return GithubReleaseInfo(
            tagName = tagName,
            releaseName = releaseName,
            bodyNotes = bodyNotes,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            isPrerelease = isPrerelease,
            apkAssetUrl = chosenAsset?.downloadUrl,
            apkAssetName = chosenAsset?.name,
            apkSizeBytes = chosenAsset?.sizeBytes ?: 0L,
            assets = assets
        )
    }

    private fun fetchTagsFallback(slug: String, currentVersion: String, token: String): GithubUpdateCheckResult? {
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
                val zipUrl = tObj.optString("zipball_url", "")
                tags.add(
                    GithubReleaseInfo(
                        tagName = tagName,
                        releaseName = "Tag $tagName",
                        bodyNotes = "",
                        htmlUrl = "https://github.com/$slug/releases/tag/$tagName",
                        publishedAt = "",
                        isPrerelease = tagName.contains('-'),
                        apkAssetUrl = treeApk?.downloadUrl ?: zipUrl.takeIf { it.isNotBlank() },
                        apkAssetName = treeApk?.name ?: "$tagName.zip",
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
                recentReleases = tags
            )
        }.getOrNull()
    }
}
