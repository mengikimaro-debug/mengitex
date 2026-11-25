package com.example.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateChecker
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.siriSecurityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sirisms_security_prefs"
)

enum class LauncherIconMode {
    VISIBLE,
    DISGUISE_CALC,
    HIDDEN
}

enum class NotificationPreviewMode {
    SHOW_ALL,
    SENDER_ONLY,
    HIDE_ALL
}

enum class NotificationVibratePattern {
    OFF,
    SHORT,
    DOUBLE,
    HEARTBEAT,
    LONG;

    fun patternMillis(): LongArray? = when (this) {
        OFF -> null
        SHORT -> longArrayOf(0L, 140L)
        DOUBLE -> longArrayOf(0L, 160L, 110L, 160L)
        HEARTBEAT -> longArrayOf(0L, 120L, 90L, 120L, 260L, 180L)
        LONG -> longArrayOf(0L, 420L)
    }
}

enum class NotificationPriorityLevel {
    HIGH,
    DEFAULT,
    LOW
}

enum class NotificationPreset {
    STANDARD,
    SILENT,
    PRIVATE,
    NIGHT_DND
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class SecuritySettings(
    val securityLevel: Int = 4,
    val silentUnknownSms: Boolean = true,
    val blockUnknownCalls: Boolean = true,
    val autoDeleteSecretAfterRead: Boolean = false,
    val autoLockIdleSeconds: Int = 30,
    val lockOnScreenOff: Boolean = true,
    val hasPasscode: Boolean = false,
    val isDarkMode: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dimLightEnabled: Boolean = false,
    val dimLightLevel: Float = 0.35f,
    val launcherIconMode: LauncherIconMode = LauncherIconMode.VISIBLE,
    val selectedSimSubId: Int = -1,
    val language: AppLanguage = AppLanguage.SW,
    val bubbleNotificationsEnabled: Boolean = true,
    val storageFolderUri: String = "",
    val storageFolderLabel: String = "",
    val blockScreenshotsAndRecents: Boolean = false,
    val privacyCurtainOnSwitch: Boolean = true,
    val clipboardAutoClearEnabled: Boolean = true,
    val wipeOnIntruderEnabled: Boolean = false,
    val failedUnlockAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val deliveryReportsEnabled: Boolean = true,
    val autoBlockAdsEnabled: Boolean = true,
    val whitelistedAdSenders: Set<String> = emptySet(),
    // Full Notification Manager settings
    val notificationsMasterEnabled: Boolean = true,
    val notifyPersonalSms: Boolean = true,
    val notifyTransactionsOtp: Boolean = true,
    val notifyDeliveryReports: Boolean = true,
    val inAppPopBannerEnabled: Boolean = true,
    val notifSoundEnabled: Boolean = true,
    val notifRingtoneUri: String = "",
    val notifRingtoneName: String = "",
    val notifVibratePattern: NotificationVibratePattern = NotificationVibratePattern.DOUBLE,
    val notifLedEnabled: Boolean = true,
    val notifHeadsUpEnabled: Boolean = true,
    val notifBadgeEnabled: Boolean = true,
    val notifPreviewMode: NotificationPreviewMode = NotificationPreviewMode.SHOW_ALL,
    val notifActionPopReplyEnabled: Boolean = true,
    val notifActionMarkReadEnabled: Boolean = true,
    val notifActionMuteEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val quietHoursAllowTransactions: Boolean = true,
    val quietHoursAllowVip: Boolean = true,
    val mutedNotificationSenders: Set<String> = emptySet(),
    val vipNotificationSenders: Set<String> = emptySet(),
    val notifySecretVault: Boolean = true,
    val notifyScheduledSms: Boolean = true,
    val notifyBlockedSpam: Boolean = false,
    val notifWakeScreenEnabled: Boolean = true,
    val notifOnlyAlertOnce: Boolean = false,
    val notifGroupBySender: Boolean = true,
    val notifPriorityLevel: NotificationPriorityLevel = NotificationPriorityLevel.HIGH,
    val notifRepeatCount: Int = 0,
    val notifRepeatIntervalMinutes: Int = 5,
    val inAppPopDurationSeconds: Int = 5,
    val notifLedColorArgb: Long = 0xFF334E68L,
    val notifActionCopyOtpEnabled: Boolean = true,
    val notifActionCallEnabled: Boolean = false,
    val notifActionDeleteEnabled: Boolean = false,
    val chatSizeScale: Float = 1.0f,
    // GitHub App Version Updater settings
    val githubUpdateRepo: String = GithubUpdateChecker.DEFAULT_GITHUB_REPO,
    val githubUpdateChannel: GithubUpdateChannel = GithubUpdateChannel.STABLE,
    val githubAutoCheckUpdates: Boolean = true,
    val githubLastCheckedMillis: Long = 0L,
    val githubCachedLatestTag: String = "",
    val githubCachedReleaseName: String = "",
    val githubCachedReleaseNotes: String = "",
    val githubCachedReleaseUrl: String = "",
    val githubCachedApkUrl: String = "",
    val githubCachedApkName: String = "",
    val githubCachedApkSizeBytes: Long = 0L,
    val githubCachedPublishedAt: String = ""
) {
    fun resolveIsDarkTheme(systemIsDark: Boolean): Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> systemIsDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
}

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val SECURITY_LEVEL = intPreferencesKey("security_level")
        val SILENT_UNKNOWN_SMS = booleanPreferencesKey("silent_unknown_sms")
        val BLOCK_UNKNOWN_CALLS = booleanPreferencesKey("block_unknown_calls")
        val AUTO_DELETE_SECRET = booleanPreferencesKey("auto_delete_secret_v2_opt_in")
        val AUTO_LOCK_SECONDS = intPreferencesKey("auto_lock_seconds")
        val LOCK_ON_SCREEN_OFF = booleanPreferencesKey("lock_on_screen_off")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val DARK_MODE = booleanPreferencesKey("dark_mode_theme_v2")
        val THEME_MODE = stringPreferencesKey("app_theme_mode_v1")
        val DIM_LIGHT_ENABLED = booleanPreferencesKey("dim_light_enabled")
        val DIM_LIGHT_LEVEL = floatPreferencesKey("dim_light_level")
        val ICON_MODE = stringPreferencesKey("icon_mode")
        val SELECTED_SIM_SUB_ID = intPreferencesKey("selected_sim_sub_id")
        val LANGUAGE = stringPreferencesKey("app_language")
        val BUBBLE_NOTIFICATIONS = booleanPreferencesKey("bubble_notifications")
        val STORAGE_FOLDER_URI = stringPreferencesKey("storage_folder_uri")
        val STORAGE_FOLDER_LABEL = stringPreferencesKey("storage_folder_label")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("strict_flag_secure_opt_in")
        val PRIVACY_CURTAIN = booleanPreferencesKey("privacy_curtain_on_switch")
        val CLIPBOARD_AUTO_CLEAR = booleanPreferencesKey("clipboard_auto_clear")
        val WIPE_ON_INTRUDER = booleanPreferencesKey("wipe_on_intruder_10")
        val FAILED_UNLOCK_ATTEMPTS = intPreferencesKey("failed_unlock_attempts")
        val LOCKOUT_UNTIL_MILLIS = androidx.datastore.preferences.core.longPreferencesKey("lockout_until_millis")
        val DELIVERY_REPORTS_ENABLED = booleanPreferencesKey("sms_delivery_reports_enabled")
        val AUTO_BLOCK_ADS_ENABLED = booleanPreferencesKey("auto_block_ads_enabled")
        val WHITELISTED_AD_SENDERS = stringSetPreferencesKey("whitelisted_ad_senders")
        // Notification Manager keys
        val NOTIF_MASTER_ENABLED = booleanPreferencesKey("notif_master_enabled")
        val NOTIF_PERSONAL_SMS = booleanPreferencesKey("notif_personal_sms")
        val NOTIF_TRANSACTIONS_OTP = booleanPreferencesKey("notif_transactions_otp")
        val NOTIF_DELIVERY_REPORTS = booleanPreferencesKey("notif_delivery_reports")
        val NOTIF_IN_APP_POP = booleanPreferencesKey("notif_in_app_pop")
        val NOTIF_SOUND_ENABLED = booleanPreferencesKey("notif_sound_enabled")
        val NOTIF_RINGTONE_URI = stringPreferencesKey("notif_ringtone_uri")
        val NOTIF_RINGTONE_NAME = stringPreferencesKey("notif_ringtone_name")
        val NOTIF_VIBRATE_PATTERN = stringPreferencesKey("notif_vibrate_pattern")
        val NOTIF_LED_ENABLED = booleanPreferencesKey("notif_led_enabled")
        val NOTIF_HEADS_UP_ENABLED = booleanPreferencesKey("notif_heads_up_enabled")
        val NOTIF_BADGE_ENABLED = booleanPreferencesKey("notif_badge_enabled")
        val NOTIF_PREVIEW_MODE = stringPreferencesKey("notif_preview_mode")
        val NOTIF_ACTION_POP_REPLY = booleanPreferencesKey("notif_action_pop_reply")
        val NOTIF_ACTION_MARK_READ = booleanPreferencesKey("notif_action_mark_read")
        val NOTIF_ACTION_MUTE = booleanPreferencesKey("notif_action_mute")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
        val QUIET_HOURS_ALLOW_TXN = booleanPreferencesKey("quiet_hours_allow_txn")
        val QUIET_HOURS_ALLOW_VIP = booleanPreferencesKey("quiet_hours_allow_vip")
        val MUTED_NOTIF_SENDERS = stringSetPreferencesKey("muted_notif_senders")
        val VIP_NOTIF_SENDERS = stringSetPreferencesKey("vip_notif_senders")
        val NOTIFY_SECRET_VAULT = booleanPreferencesKey("notify_secret_vault")
        val NOTIFY_SCHEDULED_SMS = booleanPreferencesKey("notify_scheduled_sms")
        val NOTIFY_BLOCKED_SPAM = booleanPreferencesKey("notify_blocked_spam")
        val NOTIF_WAKE_SCREEN = booleanPreferencesKey("notif_wake_screen")
        val NOTIF_ONLY_ALERT_ONCE = booleanPreferencesKey("notif_only_alert_once")
        val NOTIF_GROUP_BY_SENDER = booleanPreferencesKey("notif_group_by_sender")
        val NOTIF_PRIORITY_LEVEL = stringPreferencesKey("notif_priority_level")
        val NOTIF_REPEAT_COUNT = intPreferencesKey("notif_repeat_count")
        val NOTIF_REPEAT_INTERVAL_MIN = intPreferencesKey("notif_repeat_interval_min")
        val IN_APP_POP_DURATION_SEC = intPreferencesKey("in_app_pop_duration_sec")
        val NOTIF_LED_COLOR_ARGB = androidx.datastore.preferences.core.longPreferencesKey("notif_led_color_argb")
        val NOTIF_ACTION_COPY_OTP = booleanPreferencesKey("notif_action_copy_otp")
        val NOTIF_ACTION_CALL = booleanPreferencesKey("notif_action_call")
        val NOTIF_ACTION_DELETE = booleanPreferencesKey("notif_action_delete")
        val CHAT_SIZE_SCALE = floatPreferencesKey("chat_size_scale")
        // GitHub Updater keys
        val GITHUB_UPDATE_REPO = stringPreferencesKey("github_update_repo")
        val GITHUB_UPDATE_CHANNEL = stringPreferencesKey("github_update_channel")
        val GITHUB_AUTO_CHECK = booleanPreferencesKey("github_auto_check_updates")
        val GITHUB_LAST_CHECKED_MS = androidx.datastore.preferences.core.longPreferencesKey("github_last_checked_ms")
        val GITHUB_CACHED_TAG = stringPreferencesKey("github_cached_tag")
        val GITHUB_CACHED_NAME = stringPreferencesKey("github_cached_name")
        val GITHUB_CACHED_NOTES = stringPreferencesKey("github_cached_notes")
        val GITHUB_CACHED_HTML_URL = stringPreferencesKey("github_cached_html_url")
        val GITHUB_CACHED_APK_URL = stringPreferencesKey("github_cached_apk_url")
        val GITHUB_CACHED_APK_NAME = stringPreferencesKey("github_cached_apk_name")
        val GITHUB_CACHED_APK_SIZE = androidx.datastore.preferences.core.longPreferencesKey("github_cached_apk_size")
        val GITHUB_CACHED_PUBLISHED_AT = stringPreferencesKey("github_cached_published_at")
    }

    val settingsFlow: Flow<SecuritySettings> = context.siriSecurityDataStore.data.map { prefs ->
        val iconModeName = prefs[Keys.ICON_MODE] ?: LauncherIconMode.VISIBLE.name
        val iconMode = runCatching { LauncherIconMode.valueOf(iconModeName) }
            .getOrDefault(LauncherIconMode.VISIBLE)

        val langName = prefs[Keys.LANGUAGE] ?: AppLanguage.SW.name
        val language = runCatching { AppLanguage.valueOf(langName) }
            .getOrDefault(AppLanguage.SW)

        val vibrateMode = runCatching {
            NotificationVibratePattern.valueOf(prefs[Keys.NOTIF_VIBRATE_PATTERN] ?: NotificationVibratePattern.DOUBLE.name)
        }.getOrDefault(NotificationVibratePattern.DOUBLE)

        val previewMode = runCatching {
            NotificationPreviewMode.valueOf(prefs[Keys.NOTIF_PREVIEW_MODE] ?: NotificationPreviewMode.SHOW_ALL.name)
        }.getOrDefault(NotificationPreviewMode.SHOW_ALL)

        val savedThemeModeName = prefs[Keys.THEME_MODE]
        val legacyDark = prefs[Keys.DARK_MODE] ?: false
        val themeMode = if (savedThemeModeName != null) {
            runCatching { AppThemeMode.valueOf(savedThemeModeName) }.getOrDefault(AppThemeMode.SYSTEM)
        } else if (legacyDark) {
            AppThemeMode.DARK
        } else {
            AppThemeMode.SYSTEM
        }

        SecuritySettings(
            securityLevel = (prefs[Keys.SECURITY_LEVEL] ?: 4).coerceIn(1, 5),
            silentUnknownSms = prefs[Keys.SILENT_UNKNOWN_SMS] ?: true,
            blockUnknownCalls = prefs[Keys.BLOCK_UNKNOWN_CALLS] ?: true,
            autoDeleteSecretAfterRead = prefs[Keys.AUTO_DELETE_SECRET] ?: false,
            autoLockIdleSeconds = prefs[Keys.AUTO_LOCK_SECONDS] ?: 30,
            lockOnScreenOff = prefs[Keys.LOCK_ON_SCREEN_OFF] ?: true,
            hasPasscode = !prefs[Keys.PIN_HASH].isNullOrEmpty(),
            isDarkMode = themeMode == AppThemeMode.DARK,
            themeMode = themeMode,
            dimLightEnabled = prefs[Keys.DIM_LIGHT_ENABLED] ?: false,
            dimLightLevel = (prefs[Keys.DIM_LIGHT_LEVEL] ?: 0.35f).coerceIn(0.15f, 0.60f),
            launcherIconMode = iconMode,
            selectedSimSubId = prefs[Keys.SELECTED_SIM_SUB_ID] ?: -1,
            language = language,
            bubbleNotificationsEnabled = prefs[Keys.BUBBLE_NOTIFICATIONS] ?: true,
            storageFolderUri = prefs[Keys.STORAGE_FOLDER_URI] ?: "",
            storageFolderLabel = prefs[Keys.STORAGE_FOLDER_LABEL] ?: "",
            blockScreenshotsAndRecents = prefs[Keys.BLOCK_SCREENSHOTS] ?: false,
            privacyCurtainOnSwitch = prefs[Keys.PRIVACY_CURTAIN] ?: true,
            clipboardAutoClearEnabled = prefs[Keys.CLIPBOARD_AUTO_CLEAR] ?: true,
            wipeOnIntruderEnabled = prefs[Keys.WIPE_ON_INTRUDER] ?: false,
            failedUnlockAttempts = prefs[Keys.FAILED_UNLOCK_ATTEMPTS] ?: 0,
            lockoutUntilMillis = prefs[Keys.LOCKOUT_UNTIL_MILLIS] ?: 0L,
            deliveryReportsEnabled = prefs[Keys.DELIVERY_REPORTS_ENABLED] ?: true,
            autoBlockAdsEnabled = prefs[Keys.AUTO_BLOCK_ADS_ENABLED] ?: true,
            whitelistedAdSenders = prefs[Keys.WHITELISTED_AD_SENDERS] ?: emptySet(),
            notificationsMasterEnabled = prefs[Keys.NOTIF_MASTER_ENABLED] ?: true,
            notifyPersonalSms = prefs[Keys.NOTIF_PERSONAL_SMS] ?: true,
            notifyTransactionsOtp = prefs[Keys.NOTIF_TRANSACTIONS_OTP] ?: true,
            notifyDeliveryReports = prefs[Keys.NOTIF_DELIVERY_REPORTS] ?: true,
            inAppPopBannerEnabled = prefs[Keys.NOTIF_IN_APP_POP] ?: true,
            notifSoundEnabled = prefs[Keys.NOTIF_SOUND_ENABLED] ?: true,
            notifRingtoneUri = prefs[Keys.NOTIF_RINGTONE_URI] ?: "",
            notifRingtoneName = prefs[Keys.NOTIF_RINGTONE_NAME] ?: "",
            notifVibratePattern = vibrateMode,
            notifLedEnabled = prefs[Keys.NOTIF_LED_ENABLED] ?: true,
            notifHeadsUpEnabled = prefs[Keys.NOTIF_HEADS_UP_ENABLED] ?: true,
            notifBadgeEnabled = prefs[Keys.NOTIF_BADGE_ENABLED] ?: true,
            notifPreviewMode = previewMode,
            notifActionPopReplyEnabled = prefs[Keys.NOTIF_ACTION_POP_REPLY] ?: true,
            notifActionMarkReadEnabled = prefs[Keys.NOTIF_ACTION_MARK_READ] ?: true,
            notifActionMuteEnabled = prefs[Keys.NOTIF_ACTION_MUTE] ?: true,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartHour = (prefs[Keys.QUIET_HOURS_START_HOUR] ?: 22).coerceIn(0, 23),
            quietHoursEndHour = (prefs[Keys.QUIET_HOURS_END_HOUR] ?: 7).coerceIn(0, 23),
            quietHoursAllowTransactions = prefs[Keys.QUIET_HOURS_ALLOW_TXN] ?: true,
            quietHoursAllowVip = prefs[Keys.QUIET_HOURS_ALLOW_VIP] ?: true,
            mutedNotificationSenders = prefs[Keys.MUTED_NOTIF_SENDERS] ?: emptySet(),
            vipNotificationSenders = prefs[Keys.VIP_NOTIF_SENDERS] ?: emptySet(),
            notifySecretVault = prefs[Keys.NOTIFY_SECRET_VAULT] ?: true,
            notifyScheduledSms = prefs[Keys.NOTIFY_SCHEDULED_SMS] ?: true,
            notifyBlockedSpam = prefs[Keys.NOTIFY_BLOCKED_SPAM] ?: false,
            notifWakeScreenEnabled = prefs[Keys.NOTIF_WAKE_SCREEN] ?: true,
            notifOnlyAlertOnce = prefs[Keys.NOTIF_ONLY_ALERT_ONCE] ?: false,
            notifGroupBySender = prefs[Keys.NOTIF_GROUP_BY_SENDER] ?: true,
            notifPriorityLevel = runCatching {
                NotificationPriorityLevel.valueOf(prefs[Keys.NOTIF_PRIORITY_LEVEL] ?: NotificationPriorityLevel.HIGH.name)
            }.getOrDefault(NotificationPriorityLevel.HIGH),
            notifRepeatCount = (prefs[Keys.NOTIF_REPEAT_COUNT] ?: 0).coerceIn(0, 5),
            notifRepeatIntervalMinutes = (prefs[Keys.NOTIF_REPEAT_INTERVAL_MIN] ?: 5).coerceIn(1, 30),
            inAppPopDurationSeconds = (prefs[Keys.IN_APP_POP_DURATION_SEC] ?: 5).coerceIn(2, 15),
            notifLedColorArgb = prefs[Keys.NOTIF_LED_COLOR_ARGB] ?: 0xFF334E68L,
            notifActionCopyOtpEnabled = prefs[Keys.NOTIF_ACTION_COPY_OTP] ?: true,
            notifActionCallEnabled = prefs[Keys.NOTIF_ACTION_CALL] ?: false,
            notifActionDeleteEnabled = prefs[Keys.NOTIF_ACTION_DELETE] ?: false,
            chatSizeScale = (prefs[Keys.CHAT_SIZE_SCALE] ?: 1.0f).coerceIn(0.75f, 1.45f),
            githubUpdateRepo = (prefs[Keys.GITHUB_UPDATE_REPO] ?: GithubUpdateChecker.DEFAULT_GITHUB_REPO)
                .ifBlank { GithubUpdateChecker.DEFAULT_GITHUB_REPO },
            githubUpdateChannel = runCatching {
                GithubUpdateChannel.valueOf(prefs[Keys.GITHUB_UPDATE_CHANNEL] ?: GithubUpdateChannel.STABLE.name)
            }.getOrDefault(GithubUpdateChannel.STABLE),
            githubAutoCheckUpdates = prefs[Keys.GITHUB_AUTO_CHECK] ?: true,
            githubLastCheckedMillis = prefs[Keys.GITHUB_LAST_CHECKED_MS] ?: 0L,
            githubCachedLatestTag = prefs[Keys.GITHUB_CACHED_TAG] ?: "",
            githubCachedReleaseName = prefs[Keys.GITHUB_CACHED_NAME] ?: "",
            githubCachedReleaseNotes = prefs[Keys.GITHUB_CACHED_NOTES] ?: "",
            githubCachedReleaseUrl = prefs[Keys.GITHUB_CACHED_HTML_URL] ?: "",
            githubCachedApkUrl = prefs[Keys.GITHUB_CACHED_APK_URL] ?: "",
            githubCachedApkName = prefs[Keys.GITHUB_CACHED_APK_NAME] ?: "",
            githubCachedApkSizeBytes = prefs[Keys.GITHUB_CACHED_APK_SIZE] ?: 0L,
            githubCachedPublishedAt = prefs[Keys.GITHUB_CACHED_PUBLISHED_AT] ?: ""
        )
    }

    suspend fun getSnapshot(): SecuritySettings = settingsFlow.first()

    suspend fun applySecurityLevel(level: Int) {
        val clamped = level.coerceIn(1, 5)
        context.siriSecurityDataStore.edit { prefs ->
            prefs[Keys.SECURITY_LEVEL] = clamped
            when (clamped) {
                1 -> {
                    prefs[Keys.SILENT_UNKNOWN_SMS] = false
                    prefs[Keys.BLOCK_UNKNOWN_CALLS] = false
                    prefs[Keys.AUTO_DELETE_SECRET] = false
                    prefs[Keys.PRIVACY_CURTAIN] = false
                    prefs[Keys.AUTO_LOCK_SECONDS] = 300
                }
                2 -> {
                    prefs[Keys.SILENT_UNKNOWN_SMS] = true
                    prefs[Keys.BLOCK_UNKNOWN_CALLS] = false
                    prefs[Keys.AUTO_DELETE_SECRET] = false
                    prefs[Keys.PRIVACY_CURTAIN] = false
                    prefs[Keys.AUTO_LOCK_SECONDS] = 180
                }
                3 -> {
                    prefs[Keys.SILENT_UNKNOWN_SMS] = true
                    prefs[Keys.BLOCK_UNKNOWN_CALLS] = true
                    prefs[Keys.AUTO_DELETE_SECRET] = false
                    prefs[Keys.PRIVACY_CURTAIN] = true
                    prefs[Keys.AUTO_LOCK_SECONDS] = 60
                }
                4 -> {
                    prefs[Keys.SILENT_UNKNOWN_SMS] = true
                    prefs[Keys.BLOCK_UNKNOWN_CALLS] = true
                    prefs[Keys.AUTO_DELETE_SECRET] = false
                    prefs[Keys.PRIVACY_CURTAIN] = true
                    prefs[Keys.CLIPBOARD_AUTO_CLEAR] = true
                    prefs[Keys.AUTO_LOCK_SECONDS] = 30
                    prefs[Keys.LOCK_ON_SCREEN_OFF] = true
                }
                5 -> {
                    prefs[Keys.SILENT_UNKNOWN_SMS] = true
                    prefs[Keys.BLOCK_UNKNOWN_CALLS] = true
                    prefs[Keys.PRIVACY_CURTAIN] = true
                    prefs[Keys.CLIPBOARD_AUTO_CLEAR] = true
                    prefs[Keys.AUTO_LOCK_SECONDS] = 15
                    prefs[Keys.LOCK_ON_SCREEN_OFF] = true
                }
            }
        }
    }

    suspend fun setSilentUnknownSms(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.SILENT_UNKNOWN_SMS] = enabled }
    }

    suspend fun setBlockUnknownCalls(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.BLOCK_UNKNOWN_CALLS] = enabled }
    }

    suspend fun setAutoDeleteSecret(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.AUTO_DELETE_SECRET] = enabled }
    }

    suspend fun setAutoLockIdleSeconds(seconds: Int) {
        context.siriSecurityDataStore.edit { it[Keys.AUTO_LOCK_SECONDS] = seconds }
    }

    suspend fun setLockOnScreenOff(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.LOCK_ON_SCREEN_OFF] = enabled }
    }

    suspend fun setBlockScreenshotsAndRecents(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.BLOCK_SCREENSHOTS] = enabled }
    }

    suspend fun setPrivacyCurtainOnSwitch(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.PRIVACY_CURTAIN] = enabled }
    }

    suspend fun setClipboardAutoClear(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.CLIPBOARD_AUTO_CLEAR] = enabled }
    }

    suspend fun setWipeOnIntruder(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.WIPE_ON_INTRUDER] = enabled }
    }

    suspend fun recordFailedUnlockAttempt(): Pair<Int, Long> {
        var newAttempts = 0
        var lockoutUntil = 0L
        context.siriSecurityDataStore.edit { prefs ->
            newAttempts = (prefs[Keys.FAILED_UNLOCK_ATTEMPTS] ?: 0) + 1
            prefs[Keys.FAILED_UNLOCK_ATTEMPTS] = newAttempts
            val cooldownSeconds = when {
                newAttempts >= 7 -> 120L
                newAttempts >= 5 -> 60L
                newAttempts >= 3 -> 30L
                else -> 0L
            }
            lockoutUntil = if (cooldownSeconds > 0L) {
                System.currentTimeMillis() + (cooldownSeconds * 1000L)
            } else {
                0L
            }
            prefs[Keys.LOCKOUT_UNTIL_MILLIS] = lockoutUntil
        }
        return newAttempts to lockoutUntil
    }

    suspend fun resetUnlockAttempts() {
        context.siriSecurityDataStore.edit {
            it[Keys.FAILED_UNLOCK_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.siriSecurityDataStore.edit {
            it[Keys.DARK_MODE] = enabled
            it[Keys.THEME_MODE] = if (enabled) AppThemeMode.DARK.name else AppThemeMode.LIGHT.name
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.siriSecurityDataStore.edit {
            it[Keys.THEME_MODE] = mode.name
            it[Keys.DARK_MODE] = (mode == AppThemeMode.DARK)
        }
    }

    suspend fun setDimLightEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.DIM_LIGHT_ENABLED] = enabled }
    }

    suspend fun setDimLightLevel(level: Float) {
        val clamped = level.coerceIn(0.15f, 0.60f)
        context.siriSecurityDataStore.edit { it[Keys.DIM_LIGHT_LEVEL] = clamped }
    }

    suspend fun setLauncherIconMode(mode: LauncherIconMode) {
        context.siriSecurityDataStore.edit { it[Keys.ICON_MODE] = mode.name }
    }

    suspend fun setSelectedSimSubId(subId: Int) {
        context.siriSecurityDataStore.edit { it[Keys.SELECTED_SIM_SUB_ID] = subId }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.siriSecurityDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setBubbleNotificationsEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.BUBBLE_NOTIFICATIONS] = enabled }
    }

    suspend fun setDeliveryReportsEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.DELIVERY_REPORTS_ENABLED] = enabled }
    }

    suspend fun setAutoBlockAdsEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.AUTO_BLOCK_ADS_ENABLED] = enabled }
    }

    suspend fun addWhitelistedAdSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.WHITELISTED_AD_SENDERS] ?: emptySet()
            prefs[Keys.WHITELISTED_AD_SENDERS] = current + key
        }
    }

    suspend fun removeWhitelistedAdSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.WHITELISTED_AD_SENDERS] ?: emptySet()
            prefs[Keys.WHITELISTED_AD_SENDERS] = current - key
        }
    }

    suspend fun setStorageFolder(uri: String, label: String) {
        context.siriSecurityDataStore.edit {
            it[Keys.STORAGE_FOLDER_URI] = uri
            it[Keys.STORAGE_FOLDER_LABEL] = label
        }
    }

    suspend fun savePasscode(passcode: String, crypto: VaultCryptoManager) {
        val salt = crypto.generateSalt()
        val hash = crypto.hashPasscode(passcode, salt)
        context.siriSecurityDataStore.edit {
            it[Keys.PIN_SALT] = salt
            it[Keys.PIN_HASH] = hash
            it[Keys.FAILED_UNLOCK_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun clearPasscode() {
        context.siriSecurityDataStore.edit {
            it.remove(Keys.PIN_SALT)
            it.remove(Keys.PIN_HASH)
            it[Keys.FAILED_UNLOCK_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun verifyPasscode(candidate: String, crypto: VaultCryptoManager): Boolean {
        val prefs = context.siriSecurityDataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return true
        val hash = prefs[Keys.PIN_HASH] ?: return true
        return crypto.verifyPasscode(candidate, salt, hash)
    }

    // --- Full Notification Manager setters ---

    suspend fun setNotificationsMasterEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_MASTER_ENABLED] = enabled }
    }

    suspend fun setNotifyPersonalSms(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_PERSONAL_SMS] = enabled }
    }

    suspend fun setNotifyTransactionsOtp(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_TRANSACTIONS_OTP] = enabled }
    }

    suspend fun setNotifyDeliveryReports(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_DELIVERY_REPORTS] = enabled }
    }

    suspend fun setInAppPopBannerEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_IN_APP_POP] = enabled }
    }

    suspend fun setNotifSoundEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_SOUND_ENABLED] = enabled }
    }

    suspend fun setNotifRingtone(uri: String, displayName: String) {
        context.siriSecurityDataStore.edit {
            it[Keys.NOTIF_RINGTONE_URI] = uri
            it[Keys.NOTIF_RINGTONE_NAME] = displayName
        }
    }

    suspend fun setNotifVibratePattern(pattern: NotificationVibratePattern) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_VIBRATE_PATTERN] = pattern.name }
    }

    suspend fun setNotifLedEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_LED_ENABLED] = enabled }
    }

    suspend fun setNotifHeadsUpEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_HEADS_UP_ENABLED] = enabled }
    }

    suspend fun setNotifBadgeEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_BADGE_ENABLED] = enabled }
    }

    suspend fun setNotifPreviewMode(mode: NotificationPreviewMode) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_PREVIEW_MODE] = mode.name }
    }

    suspend fun setNotifActionPopReplyEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_POP_REPLY] = enabled }
    }

    suspend fun setNotifActionMarkReadEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_MARK_READ] = enabled }
    }

    suspend fun setNotifActionMuteEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_MUTE] = enabled }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun setQuietHoursRange(startHour: Int, endHour: Int) {
        context.siriSecurityDataStore.edit {
            it[Keys.QUIET_HOURS_START_HOUR] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_HOURS_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setQuietHoursAllowTransactions(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.QUIET_HOURS_ALLOW_TXN] = enabled }
    }

    suspend fun muteNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.MUTED_NOTIF_SENDERS] ?: emptySet()
            prefs[Keys.MUTED_NOTIF_SENDERS] = current + key
        }
    }

    suspend fun unmuteNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.MUTED_NOTIF_SENDERS] ?: emptySet()
            prefs[Keys.MUTED_NOTIF_SENDERS] = current - key
        }
    }

    suspend fun clearAllMutedNotificationSenders() {
        context.siriSecurityDataStore.edit { it[Keys.MUTED_NOTIF_SENDERS] = emptySet() }
    }

    suspend fun setNotifySecretVault(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIFY_SECRET_VAULT] = enabled }
    }

    suspend fun setNotifyScheduledSms(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIFY_SCHEDULED_SMS] = enabled }
    }

    suspend fun setNotifyBlockedSpam(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIFY_BLOCKED_SPAM] = enabled }
    }

    suspend fun setNotifWakeScreenEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_WAKE_SCREEN] = enabled }
    }

    suspend fun setNotifOnlyAlertOnce(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ONLY_ALERT_ONCE] = enabled }
    }

    suspend fun setNotifGroupBySender(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_GROUP_BY_SENDER] = enabled }
    }

    suspend fun setNotifPriorityLevel(level: NotificationPriorityLevel) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_PRIORITY_LEVEL] = level.name }
    }

    suspend fun setNotifRepeatSettings(repeatCount: Int, intervalMinutes: Int) {
        context.siriSecurityDataStore.edit {
            it[Keys.NOTIF_REPEAT_COUNT] = repeatCount.coerceIn(0, 5)
            it[Keys.NOTIF_REPEAT_INTERVAL_MIN] = intervalMinutes.coerceIn(1, 30)
        }
    }

    suspend fun setInAppPopDurationSeconds(seconds: Int) {
        context.siriSecurityDataStore.edit { it[Keys.IN_APP_POP_DURATION_SEC] = seconds.coerceIn(2, 15) }
    }

    suspend fun setNotifLedColorArgb(colorArgb: Long) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_LED_COLOR_ARGB] = colorArgb }
    }

    suspend fun setNotifActionCopyOtpEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_COPY_OTP] = enabled }
    }

    suspend fun setNotifActionCallEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_CALL] = enabled }
    }

    suspend fun setNotifActionDeleteEnabled(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.NOTIF_ACTION_DELETE] = enabled }
    }

    suspend fun setQuietHoursAllowVip(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.QUIET_HOURS_ALLOW_VIP] = enabled }
    }

    suspend fun addVipNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.VIP_NOTIF_SENDERS] ?: emptySet()
            prefs[Keys.VIP_NOTIF_SENDERS] = current + key
            // Ensure VIP sender is not simultaneously muted
            val muted = prefs[Keys.MUTED_NOTIF_SENDERS] ?: emptySet()
            if (key in muted) prefs[Keys.MUTED_NOTIF_SENDERS] = muted - key
        }
    }

    suspend fun removeVipNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        if (key.isEmpty()) return
        context.siriSecurityDataStore.edit { prefs ->
            val current = prefs[Keys.VIP_NOTIF_SENDERS] ?: emptySet()
            prefs[Keys.VIP_NOTIF_SENDERS] = current - key
        }
    }

    suspend fun clearAllVipNotificationSenders() {
        context.siriSecurityDataStore.edit { it[Keys.VIP_NOTIF_SENDERS] = emptySet() }
    }

    suspend fun applyNotificationPreset(preset: NotificationPreset) {
        context.siriSecurityDataStore.edit { p ->
            p[Keys.NOTIF_MASTER_ENABLED] = true
            when (preset) {
                NotificationPreset.STANDARD -> {
                    p[Keys.NOTIF_PERSONAL_SMS] = true
                    p[Keys.NOTIF_TRANSACTIONS_OTP] = true
                    p[Keys.NOTIFY_SECRET_VAULT] = true
                    p[Keys.NOTIFY_SCHEDULED_SMS] = true
                    p[Keys.BUBBLE_NOTIFICATIONS] = true
                    p[Keys.NOTIF_IN_APP_POP] = true
                    p[Keys.NOTIF_SOUND_ENABLED] = true
                    p[Keys.NOTIF_VIBRATE_PATTERN] = NotificationVibratePattern.DOUBLE.name
                    p[Keys.NOTIF_HEADS_UP_ENABLED] = true
                    p[Keys.NOTIF_PRIORITY_LEVEL] = NotificationPriorityLevel.HIGH.name
                    p[Keys.NOTIF_PREVIEW_MODE] = NotificationPreviewMode.SHOW_ALL.name
                    p[Keys.QUIET_HOURS_ENABLED] = false
                }
                NotificationPreset.SILENT -> {
                    p[Keys.NOTIF_SOUND_ENABLED] = false
                    p[Keys.NOTIF_VIBRATE_PATTERN] = NotificationVibratePattern.OFF.name
                    p[Keys.NOTIF_HEADS_UP_ENABLED] = false
                    p[Keys.NOTIF_LED_ENABLED] = false
                    p[Keys.NOTIF_WAKE_SCREEN] = false
                    p[Keys.NOTIF_PRIORITY_LEVEL] = NotificationPriorityLevel.LOW.name
                    p[Keys.NOTIF_BADGE_ENABLED] = true
                }
                NotificationPreset.PRIVATE -> {
                    p[Keys.NOTIF_SOUND_ENABLED] = true
                    p[Keys.NOTIF_VIBRATE_PATTERN] = NotificationVibratePattern.SHORT.name
                    p[Keys.NOTIF_PREVIEW_MODE] = NotificationPreviewMode.HIDE_ALL.name
                    p[Keys.NOTIFY_SECRET_VAULT] = true
                    p[Keys.NOTIF_ACTION_POP_REPLY] = false
                    p[Keys.NOTIF_WAKE_SCREEN] = false
                    p[Keys.NOTIF_PRIORITY_LEVEL] = NotificationPriorityLevel.DEFAULT.name
                }
                NotificationPreset.NIGHT_DND -> {
                    p[Keys.QUIET_HOURS_ENABLED] = true
                    p[Keys.QUIET_HOURS_START_HOUR] = 22
                    p[Keys.QUIET_HOURS_END_HOUR] = 7
                    p[Keys.QUIET_HOURS_ALLOW_TXN] = true
                    p[Keys.QUIET_HOURS_ALLOW_VIP] = true
                    p[Keys.NOTIF_WAKE_SCREEN] = false
                }
            }
        }
    }

    suspend fun resetAllNotificationSettings() {
        context.siriSecurityDataStore.edit { p ->
            p[Keys.NOTIF_MASTER_ENABLED] = true
            p[Keys.NOTIF_PERSONAL_SMS] = true
            p[Keys.NOTIF_TRANSACTIONS_OTP] = true
            p[Keys.NOTIF_DELIVERY_REPORTS] = true
            p[Keys.BUBBLE_NOTIFICATIONS] = true
            p[Keys.NOTIF_IN_APP_POP] = true
            p[Keys.SILENT_UNKNOWN_SMS] = true
            p[Keys.NOTIFY_SECRET_VAULT] = true
            p[Keys.NOTIFY_SCHEDULED_SMS] = true
            p[Keys.NOTIFY_BLOCKED_SPAM] = false
            p[Keys.NOTIF_SOUND_ENABLED] = true
            p[Keys.NOTIF_RINGTONE_URI] = ""
            p[Keys.NOTIF_RINGTONE_NAME] = ""
            p[Keys.NOTIF_VIBRATE_PATTERN] = NotificationVibratePattern.DOUBLE.name
            p[Keys.NOTIF_LED_ENABLED] = true
            p[Keys.NOTIF_LED_COLOR_ARGB] = 0xFF334E68L
            p[Keys.NOTIF_HEADS_UP_ENABLED] = true
            p[Keys.NOTIF_BADGE_ENABLED] = true
            p[Keys.NOTIF_WAKE_SCREEN] = true
            p[Keys.NOTIF_ONLY_ALERT_ONCE] = false
            p[Keys.NOTIF_GROUP_BY_SENDER] = true
            p[Keys.NOTIF_PRIORITY_LEVEL] = NotificationPriorityLevel.HIGH.name
            p[Keys.NOTIF_REPEAT_COUNT] = 0
            p[Keys.NOTIF_REPEAT_INTERVAL_MIN] = 5
            p[Keys.IN_APP_POP_DURATION_SEC] = 5
            p[Keys.NOTIF_PREVIEW_MODE] = NotificationPreviewMode.SHOW_ALL.name
            p[Keys.NOTIF_ACTION_POP_REPLY] = true
            p[Keys.NOTIF_ACTION_MARK_READ] = true
            p[Keys.NOTIF_ACTION_MUTE] = true
            p[Keys.NOTIF_ACTION_COPY_OTP] = true
            p[Keys.NOTIF_ACTION_CALL] = false
            p[Keys.NOTIF_ACTION_DELETE] = false
            p[Keys.QUIET_HOURS_ENABLED] = false
            p[Keys.QUIET_HOURS_START_HOUR] = 22
            p[Keys.QUIET_HOURS_END_HOUR] = 7
            p[Keys.QUIET_HOURS_ALLOW_TXN] = true
            p[Keys.QUIET_HOURS_ALLOW_VIP] = true
            p[Keys.MUTED_NOTIF_SENDERS] = emptySet()
            p[Keys.VIP_NOTIF_SENDERS] = emptySet()
        }
    }

    suspend fun setChatSizeScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 1.45f)
        context.siriSecurityDataStore.edit { it[Keys.CHAT_SIZE_SCALE] = clamped }
    }

    suspend fun setGithubUpdateRepo(repoSlug: String) {
        val normalized = GithubUpdateChecker.normalizeRepoSlug(repoSlug)
            .ifBlank { GithubUpdateChecker.DEFAULT_GITHUB_REPO }
        context.siriSecurityDataStore.edit { it[Keys.GITHUB_UPDATE_REPO] = normalized }
    }

    suspend fun setGithubUpdateChannel(channel: GithubUpdateChannel) {
        context.siriSecurityDataStore.edit { it[Keys.GITHUB_UPDATE_CHANNEL] = channel.name }
    }

    suspend fun setGithubAutoCheckUpdates(enabled: Boolean) {
        context.siriSecurityDataStore.edit { it[Keys.GITHUB_AUTO_CHECK] = enabled }
    }

    suspend fun saveGithubReleaseCheckSnapshot(
        checkedMillis: Long,
        latestTag: String,
        releaseName: String,
        releaseNotes: String,
        releaseUrl: String,
        apkUrl: String,
        apkName: String,
        apkSizeBytes: Long,
        publishedAt: String
    ) {
        context.siriSecurityDataStore.edit { p ->
            p[Keys.GITHUB_LAST_CHECKED_MS] = checkedMillis
            p[Keys.GITHUB_CACHED_TAG] = latestTag
            p[Keys.GITHUB_CACHED_NAME] = releaseName
            p[Keys.GITHUB_CACHED_NOTES] = releaseNotes
            p[Keys.GITHUB_CACHED_HTML_URL] = releaseUrl
            p[Keys.GITHUB_CACHED_APK_URL] = apkUrl
            p[Keys.GITHUB_CACHED_APK_NAME] = apkName
            p[Keys.GITHUB_CACHED_APK_SIZE] = apkSizeBytes
            p[Keys.GITHUB_CACHED_PUBLISHED_AT] = publishedAt
        }
    }
}
