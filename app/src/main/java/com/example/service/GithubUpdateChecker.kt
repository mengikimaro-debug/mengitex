package com.example.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
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
    val assets: List<GithubReleaseAsset> = emptyList()
)

sealed class GithubUpdateCheckResult {
    data class Success(
        val currentVersion: String,
        val latestRelease: GithubReleaseInfo,
        val isNewerAvailable: Boolean,
        val recentReleases: List<GithubReleaseInfo>
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

    /**
     * Normalizes user input such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo/releases"
     * into a clean "owner/repo" slug.
     */
    fun normalizeRepoSlug(rawInput: String): String {
        var cleaned = rawInput.trim()
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

    fun isValidRepoSlug(rawInput: String): Boolean {
        val slug = normalizeRepoSlug(rawInput)
        val parts = slug.split('/')
        return parts.size == 2 &&
            parts[0].matches(Regex("^[A-Za-z0-9_.-]+$")) &&
            parts[1].matches(Regex("^[A-Za-z0-9_.-]+$"))
    }

    fun buildRepoReleasesWebUrl(rawRepo: String): String {
        val slug = normalizeRepoSlug(rawRepo).ifBlank { DEFAULT_GITHUB_REPO }
        return "https://github.com/$slug/releases"
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

        val localNums = localClean.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val remoteNums = remoteClean.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(localNums.size, remoteNums.size)

        for (i in 0 until maxLen) {
            val l = localNums.getOrElse(i) { 0 }
            val r = remoteNums.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        // If numeric parts are equal, a stable tag is newer than a pre-release tag, or different build tag
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
     * Queries the live GitHub REST API (`https://api.github.com/repos/{owner}/{repo}/releases`)
     * and falls back to `/tags` if the repository publishes git tags without formal GitHub Releases.
     */
    suspend fun checkLatestRelease(
        context: Context,
        rawRepoInput: String,
        channel: GithubUpdateChannel
    ): GithubUpdateCheckResult = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(rawRepoInput)
        if (!isValidRepoSlug(slug)) {
            return@withContext GithubUpdateCheckResult.Error(
                messageSw = "Jina la GitHub Repo si sahihi. Tumia mfumo wa: mmiliki/jina-la-repo (mfano: $DEFAULT_GITHUB_REPO)",
                messageEn = "Invalid GitHub repository format. Use owner/repository (e.g. $DEFAULT_GITHUB_REPO)"
            )
        }

        val currentVersion = getInstalledAppVersionName(context)
        val releasesApiUrl = "https://api.github.com/repos/$slug/releases?per_page=10"

        try {
            val (code, body) = executeGithubGet(releasesApiUrl)
            when (code) {
                HttpURLConnection.HTTP_OK -> {
                    val parsedReleases = parseReleasesArray(body, slug)
                    val eligible = if (channel == GithubUpdateChannel.STABLE) {
                        parsedReleases.filter { !it.isPrerelease }.ifEmpty { parsedReleases }
                    } else {
                        parsedReleases
                    }

                    if (eligible.isNotEmpty()) {
                        val latest = eligible.first()
                        val isNewer = isRemoteVersionNewer(currentVersion, latest.tagName)
                        return@withContext GithubUpdateCheckResult.Success(
                            currentVersion = currentVersion,
                            latestRelease = latest,
                            isNewerAvailable = isNewer,
                            recentReleases = eligible.take(5)
                        )
                    }

                    // Fallback: check /tags endpoint if repository uses tags without formal Releases
                    val tagsResult = fetchTagsFallback(slug, currentVersion)
                    if (tagsResult != null) {
                        return@withContext tagsResult
                    }

                    GithubUpdateCheckResult.NoReleasesFound(
                        repoSlug = slug,
                        repoHtmlUrl = buildRepoReleasesWebUrl(slug)
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    GithubUpdateCheckResult.Error(
                        messageSw = "Repo ya GitHub '$slug' haijapatikana au bado ni Private. Hakikisha jina la owner/repo ni sahihi.",
                        messageEn = "GitHub repository '$slug' was not found or is private. Verify the owner/repo name."
                    )
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    GithubUpdateCheckResult.Error(
                        messageSw = "GitHub API imefikia kikomo cha muda (Rate Limit). Jaribu tena baada ya dakika chache au fungua GitHub moja kwa moja.",
                        messageEn = "GitHub API rate limit reached. Try again shortly or open GitHub Releases directly."
                    )
                }

                else -> {
                    GithubUpdateCheckResult.Error(
                        messageSw = "Imeshindwa kuwasiliana na GitHub (HTTP $code). Angalia mtandao wako wa intaneti.",
                        messageEn = "GitHub request failed (HTTP $code). Please check your internet connection."
                    )
                }
            }
        } catch (e: Exception) {
            GithubUpdateCheckResult.Error(
                messageSw = "Hakuna mawasiliano ya intaneti au GitHub haipatikani (${e.localizedMessage ?: "Network Error"}).",
                messageEn = "Unable to reach GitHub (${e.localizedMessage ?: "Network Error"}). Check your internet connection."
            )
        }
    }

    private fun executeGithubGet(urlString: String): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 9000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MengiTex-Android-Updater/1.0")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
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

    private fun fetchTagsFallback(slug: String, currentVersion: String): GithubUpdateCheckResult? {
        return runCatching {
            val (code, body) = executeGithubGet("https://api.github.com/repos/$slug/tags?per_page=5")
            if (code != HttpURLConnection.HTTP_OK) return null
            val arr = JSONArray(body)
            if (arr.length() == 0) return null
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
                        apkAssetUrl = zipUrl.takeIf { it.isNotBlank() },
                        apkAssetName = "$tagName.zip",
                        apkSizeBytes = 0L
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
