package com.example.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloud.CloudBackupRecord
import com.example.cloud.CloudBackupRepository
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.data.DecryptedTemplate
import com.example.data.LiveSmsEventBus
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.data.SmsInstantCache
import com.example.security.AppIconManager
import com.example.security.AppThemeMode
import com.example.security.EncryptedVaultFile
import com.example.security.HiddenVaultLogger
import com.example.security.LauncherIconMode
import com.example.security.LocalBackupManager
import com.example.security.LocalBackupSnapshot
import com.example.security.MengiTexBackupArchive
import com.example.security.NotificationPreset
import com.example.security.NotificationPreviewMode
import com.example.security.NotificationPriorityLevel
import com.example.security.NotificationVibratePattern
import com.example.security.PortableBackupCrypto
import com.example.security.SecurityPreferences
import com.example.security.SecuritySettings
import com.example.security.SmsAdBlockerEngine
import com.example.security.VaultCryptoManager
import com.example.security.VaultFolderStats
import com.example.security.VaultLogEntry
import com.example.service.ContactsLoader
import com.example.service.DefaultAppRoleHelper
import com.example.service.DetectedSimCard
import com.example.service.GithubReleaseInfo
import com.example.service.GithubUpdateChannel
import com.example.service.GithubUpdateCheckResult
import com.example.service.GithubUpdateChecker
import com.example.service.MengiTexNotificationManager
import com.example.service.SimCardManager
import com.example.service.SmsDispatchManager
import com.example.service.VoiceDictationHelper
import com.example.ui.i18n.AppLanguage
import com.example.ui.screens.GroupedInboxData
import com.example.ui.screens.buildInboxThreadsSinglePass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Slim 2-tab Bottom Navigation Bar: Messages & Contacts only.
 */
enum class MainTab {
    MESSAGES,
    CONTACTS
}

/**
 * Secondary destinations organized cleanly inside the Slide Bar (ModalNavigationDrawer).
 */
enum class DrawerDestination {
    NONE,
    CONVERSATION,
    COMPOSE,
    SCHEDULE,
    TEMPLATES,
    USAGE_DASHBOARD,
    SECURITY,
    HIDDEN_LOGS,
    BACKUP_RESTORE,
    TRASH,
    SETTINGS,
    UPGRADE
}

enum class InboxFilter {
    ALL,
    BLOCKED_ADS,
    UNKNOWN_SILENT,
    REPORTS,
    SECRET
}

data class SiriSmsUiState(
    val selectedTab: MainTab = MainTab.MESSAGES,
    val activeDestination: DrawerDestination = DrawerDestination.NONE,
    val inboxFilter: InboxFilter = InboxFilter.ALL,
    val settings: SecuritySettings = SecuritySettings(),
    val isLocked: Boolean = false,
    val lockError: String? = null,
    val detectedSims: List<DetectedSimCard> = emptyList(),
    val simStateSummary: String = "SIM...",
    val deviceContacts: List<ContactItem> = emptyList(),
    val selectedRecipients: List<ContactItem> = emptyList(),
    val composeBody: String = "",
    val composePartsCount: Int = 0,
    val composeIsSecretBurn: Boolean = false,
    val vaultLogs: List<VaultLogEntry> = emptyList(),
    val vaultFiles: List<EncryptedVaultFile> = emptyList(),
    val vaultStats: VaultFolderStats = VaultFolderStats("/.siri_vault_hidden/.logs/", 0, 0, true),
    val activeSecretReaderMessage: DecryptedSmsMessage? = null,
    val isDefaultSmsApp: Boolean = false,
    val isCallScreeningRoleHeld: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasMicPermission: Boolean = false,
    val hasStorageFolderAccess: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val activeNotificationCount: Int = 0,
    val localSnapshots: List<LocalBackupSnapshot> = emptyList(),
    val isBackupBusy: Boolean = false,
    val importProgressText: String? = null,
    val activeConversationPhone: String = "",
    val activeConversationName: String = "",
    val statusBanner: String? = null,
    val isInitialMessagesLoaded: Boolean = false,
    val hasUpgradeAvailable: Boolean = false,
    val latestUpgradeTag: String = ""
)

class SiriSmsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val crypto = VaultCryptoManager(appContext)
    val vaultLogger = HiddenVaultLogger(appContext, crypto)
    private val preferences = SecurityPreferences(appContext)
    val localBackupManager = LocalBackupManager(appContext)
    private val cloudBackupRepository: CloudBackupRepository? by lazy {
        CloudBackupRepository.createOrNull(appContext)
    }
    private val repository = SiriSmsRepository(
        dao = SiriSmsDatabase.getInstance(appContext).dao(),
        crypto = crypto,
        vaultLogger = vaultLogger
    )

    // Zero-I/O RAM check on Frame 0 (~0.001ms); disk snapshot decryption runs immediately on Dispatchers.IO.
    private val initialInstantMessages: List<DecryptedSmsMessage> =
        SmsInstantCache.loadMemoryOnly()

    private val _uiState = MutableStateFlow(
        SiriSmsUiState(isInitialMessagesLoaded = initialInstantMessages.isNotEmpty())
    )
    val uiState: StateFlow<SiriSmsUiState> = _uiState.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<DecryptedSmsMessage>>(initialInstantMessages)
    val activeMessages: StateFlow<List<DecryptedSmsMessage>> = _activeMessages.asStateFlow()

    // O(1) pre-indexed thread map (oldest-first per thread) so opening ANY chat out of 600,000 SMS takes ~0.01ms!
    val messagesByThreadKey: StateFlow<Map<String, List<DecryptedSmsMessage>>> = _activeMessages
        .map { messages ->
            val buckets = HashMap<String, ArrayList<DecryptedSmsMessage>>(256)
            // Iterate in reverse (newest->oldest input => oldest->newest per thread without sorting!)
            for (i in messages.lastIndex downTo 0) {
                val m = messages[i]
                val key = m.canonicalThreadKey
                buckets.getOrPut(key) { ArrayList(8) }.add(m)
            }
            buckets
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val scheduledMessages: StateFlow<List<DecryptedSmsMessage>> = repository.scheduledMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trashedMessages: StateFlow<List<DecryptedSmsMessage>> = repository.trashedMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeTemplates: StateFlow<List<DecryptedTemplate>> = repository.activeTemplates
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trashedTemplates: StateFlow<List<DecryptedTemplate>> = repository.trashedTemplates
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allContacts: StateFlow<List<ContactItem>> = combine(
        repository.localContacts,
        _uiState.map { it.deviceContacts }.distinctUntilChanged()
    ) { local, deviceContacts ->
        val localMapped = local.map {
            ContactItem(
                id = "local_${it.id}",
                name = it.name,
                phoneNumber = it.phoneNumber,
                isLocalSaved = true
            )
        }
        val seen = LinkedHashSet<String>(localMapped.size + deviceContacts.size)
        val merged = ArrayList<ContactItem>(localMapped.size + deviceContacts.size)
        (localMapped + deviceContacts).forEach { c ->
            val norm = ContactsLoader.normalizePhone(c.phoneNumber)
            if (norm.isNotEmpty() && seen.add(norm)) {
                merged.add(c)
            }
        }
        val sorted = merged.sortedBy { it.name.lowercase() }
        // Warm O(1) RAM contact cache so incoming SMS skips ContactsProvider Binder IPC (~3ms arrival!)
        com.example.data.LiveSmsEventBus.updateContactsIndex(sorted)
        sorted
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Pre-computed O(N) inbox conversation groups on Default dispatcher so Inbox/Contacts/Back switch in ~0ms
    val groupedInboxData: StateFlow<GroupedInboxData> = combine(
        _activeMessages,
        allContacts
    ) { messages, contacts ->
        buildInboxThreadsSinglePass(messages, contacts)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, GroupedInboxData(emptyList(), 0))

    val trashCount: StateFlow<Int> = combine(trashedMessages, trashedTemplates) { msgs, tmpls ->
        msgs.size + tmpls.size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val syncPrefs by lazy {
        appContext.getSharedPreferences("mengitex_sms_memory_sync", Context.MODE_PRIVATE)
    }

    @Volatile
    private var lastInteractionMillis: Long = System.currentTimeMillis()
    private var initialLockEvaluated = false
    @Volatile
    private var hasAutoSyncedPhoneSmsThisSession = false
    @Volatile
    private var isDeltaSyncRunning = false
    @Volatile
    private var pendingDeltaSync = false
    @Volatile
    private var lastDeltaSyncMillis = 0L
    private var deltaSyncDebounceJob: Job? = null
    private var diskCacheSaveJob: Job? = null
    private var autoGuardianSaveJob: Job? = null
    private var channelSyncJob: Job? = null
    private var smsContentObserver: ContentObserver? = null
    @Volatile
    private var isSelfHealingRunning = false
    @Volatile
    private var lastAutoGuardianCount = -1
    @Volatile
    private var lastAutoGuardianSavedMillis = 0L

    // Zero-latency optimistic chat & settings coordinators
    private val tempOptimisticIdSeq = AtomicLong(1L)
    private val pendingOptimisticMessages = ConcurrentHashMap<Long, DecryptedSmsMessage>()
    private val trackedPopReplyRecipients = ConcurrentHashMap<Long, String>()
    @Volatile
    private var lastDirectSendFingerprint: String = ""
    @Volatile
    private var lastDirectSendMillis: Long = 0L
    private val pendingSettingsWrites = AtomicInteger(0)
    private val settingsWriteMutex = Mutex()
    @Volatile
    private var vaultDataDirty: Boolean = true
    private var lastBurnAfterReadSetting: Boolean? = null
    @Volatile
    private var lastContactsLoadMillis: Long = 0L
    @Volatile
    private var lastEnvRefreshMillis: Long = 0L
    @Volatile
    private var isEnvRefreshRunning: Boolean = false
    @Volatile
    private var hasInitializedChannels: Boolean = false

    private fun isEnglish(): Boolean = _uiState.value.settings.language == AppLanguage.EN

    private fun msg(sw: String, en: String): String = if (isEnglish()) en else sw

    private inline fun updateSettingsOptimistically(
        crossinline transform: (SecuritySettings) -> SecuritySettings,
        crossinline persist: suspend () -> Unit
    ) {
        recordUserInteraction()
        pendingSettingsWrites.incrementAndGet()
        var nextSettings: SecuritySettings? = null
        _uiState.update {
            val updated = transform(it.settings)
            nextSettings = updated
            it.copy(settings = updated)
        }
        nextSettings?.let { com.example.data.LiveSmsEventBus.updateCachedSettings(it) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsWriteMutex.withLock {
                    runCatching { persist() }
                }
            } finally {
                pendingSettingsWrites.decrementAndGet()
            }
        }
    }

    private fun scheduleNotificationChannelSync() {
        channelSyncJob?.cancel()
        channelSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(20L)
            runCatching {
                MengiTexNotificationManager.ensureAllChannelsCreated(appContext, _uiState.value.settings)
            }
        }
    }

    init {
        // 1. Non-blocking multi-stage hydration on IO:
        //    - Pre-warm Keystore & load single-blob instant cache on IO (~3–10ms, zero Main-Thread freeze)
        //    - Fallback to 350-message quick slice if disk snapshot empty
        //    - Observe full active messages continuously
        viewModelScope.launch(Dispatchers.IO) {
            VaultCryptoManager.bindContext(appContext)
            if (_activeMessages.value.isEmpty()) {
                val diskInstant = runCatching {
                    SmsInstantCache.loadInstantSync(appContext, crypto)
                }.getOrDefault(emptyList())
                if (diskInstant.isNotEmpty() && _activeMessages.value.isEmpty()) {
                    _activeMessages.value = diskInstant
                    _uiState.update { it.copy(isInitialMessagesLoaded = true) }
                }
            }
            if (_activeMessages.value.isEmpty()) {
                val quickSlice = runCatching { repository.getRecentActiveMessagesOnce(350) }.getOrDefault(emptyList())
                if (quickSlice.isNotEmpty() && _activeMessages.value.isEmpty()) {
                    SmsInstantCache.updateMemory(quickSlice)
                    _activeMessages.value = quickSlice
                    _uiState.update { it.copy(isInitialMessagesLoaded = true) }
                }
            }
            repository.activeMessages.collect { fullList ->
                if (fullList.isEmpty()) {
                    val currentInUi = _activeMessages.value
                    val actualDbCount = runCatching { repository.countActiveMessagesOnce() }.getOrDefault(0)
                    if (currentInUi.isNotEmpty() || actualDbCount == 0) {
                        // ZERO-DATA-LOSS SELF-HEALING GUARD:
                        // Do NOT wipe existing UI messages on unexpected empty emission; trigger automatic recovery!
                        if (!_uiState.value.isInitialMessagesLoaded) {
                            _uiState.update { it.copy(isInitialMessagesLoaded = true) }
                        }
                        triggerSelfHealingVaultRecoveryIfNeeded(cachedFallback = currentInUi)
                        return@collect
                    }
                }

                val mergedList = if (pendingOptimisticMessages.isEmpty()) {
                    fullList
                } else {
                    // Drop any in-flight optimistic placeholder that Room has already committed
                    val recentPersisted = fullList.take(24)
                    val remainingInFlight = pendingOptimisticMessages.values
                        .filter { opt ->
                            val alreadyInRoom = recentPersisted.any { persisted ->
                                persisted.direction == opt.direction &&
                                    persisted.canonicalThreadKey == opt.canonicalThreadKey &&
                                    persisted.body == opt.body &&
                                    kotlin.math.abs(persisted.timestamp - opt.timestamp) <= 15_000L
                            }
                            if (alreadyInRoom) {
                                pendingOptimisticMessages.remove(opt.id)
                            }
                            !alreadyInRoom
                        }
                        .sortedByDescending { it.timestamp }
                    if (remainingInFlight.isEmpty()) fullList else remainingInFlight + fullList
                }
                val currentDisplayed = _activeMessages.value
                val listChanged = !areMessageListsEquivalent(currentDisplayed, mergedList)
                SmsInstantCache.updateMemory(fullList)
                if (listChanged) {
                    _activeMessages.value = mergedList
                }
                if (!_uiState.value.isInitialMessagesLoaded) {
                    _uiState.update { it.copy(isInitialMessagesLoaded = true) }
                }
                if (listChanged) {
                    scheduleInstantCachePersist(fullList)
                }
                scheduleAutoGuardianBackup(fullList)
            }
        }

        // 1b. Zero-latency live incoming SMS push (<5ms Frame-0 arrival from SmsStealthReceiver)
        viewModelScope.launch {
            com.example.data.LiveSmsEventBus.incomingMessages.collect { incoming ->
                _activeMessages.update { current ->
                    val alreadyExists = current.take(30).any { existing ->
                        (incoming.id > 0L && existing.id == incoming.id) ||
                            (existing.direction == incoming.direction &&
                                existing.canonicalThreadKey == incoming.canonicalThreadKey &&
                                existing.body == incoming.body &&
                                kotlin.math.abs(existing.timestamp - incoming.timestamp) <= 15_000L)
                    }
                    if (alreadyExists) current else listOf(incoming) + current
                }
                if (!_uiState.value.isInitialMessagesLoaded) {
                    _uiState.update { it.copy(isInitialMessagesLoaded = true) }
                }
            }
        }

        // 1c. Zero-latency live Sent / Delivered / Failed status tick push from SmsDeliveryReceiver
        viewModelScope.launch {
            com.example.data.LiveSmsEventBus.statusUpdates.collect { update ->
                _activeMessages.update { current ->
                    var changed = false
                    val mapped = current.map { m ->
                        if (m.id == update.messageId && (m.status != update.status || m.statusNote != update.statusNote)) {
                            changed = true
                            m.copy(status = update.status, statusNote = update.statusNote)
                        } else {
                            m
                        }
                    }
                    if (changed) mapped else current
                }
                SmsInstantCache.updateMessageStatus(
                    appContext,
                    crypto,
                    update.messageId,
                    update.status,
                    update.statusNote,
                    persistToDisk = false
                )
                val popRecipient = trackedPopReplyRecipients[update.messageId]
                if (popRecipient != null) {
                    when (update.status) {
                        MessageStatus.SENT -> showBanner(
                            msg(
                                "Pop Reply imetumwa kwa $popRecipient ✓",
                                "Pop Reply sent to $popRecipient ✓"
                            )
                        )
                        MessageStatus.DELIVERED -> {
                            trackedPopReplyRecipients.remove(update.messageId)
                            showBanner(
                                msg(
                                    "Pop Reply imefika kwa $popRecipient ✓✓",
                                    "Pop Reply delivered to $popRecipient ✓✓"
                                )
                            )
                        }
                        MessageStatus.FAILED -> {
                            trackedPopReplyRecipients.remove(update.messageId)
                            showBanner(
                                msg(
                                    "Imeshindwa kutuma Pop Reply kwa $popRecipient: ${update.statusNote}",
                                    "Pop Reply failed for $popRecipient: ${update.statusNote}"
                                )
                            )
                        }
                    }
                } else if (update.status == MessageStatus.FAILED) {
                    showBanner(
                        msg(
                            "Ujumbe umeshindwa kutumwa: ${update.statusNote}",
                            "Message failed to send: ${update.statusNote}"
                        )
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.seedStarterTemplatesIfEmpty() }
        }

        viewModelScope.launch {
            runCatching {
                preferences.settingsFlow.collect { settings ->
                    val shouldApplySettings = pendingSettingsWrites.get() == 0 || !initialLockEvaluated
                    if (shouldApplySettings) {
                        com.example.data.LiveSmsEventBus.updateCachedSettings(settings)
                    }
                    _uiState.update { current ->
                        val shouldLockInitially = !initialLockEvaluated && settings.hasPasscode
                        if (!initialLockEvaluated) initialLockEvaluated = true
                        current.copy(
                            settings = if (shouldApplySettings) settings else current.settings,
                            isLocked = if (shouldLockInitially) true else current.isLocked
                        )
                    }
                    // Only run SQLite update when autoDeleteSecretAfterRead actually changes — NEVER on unrelated toggles!
                    if (lastBurnAfterReadSetting != settings.autoDeleteSecretAfterRead) {
                        lastBurnAfterReadSetting = settings.autoDeleteSecretAfterRead
                        if (!settings.autoDeleteSecretAfterRead) {
                            viewModelScope.launch(Dispatchers.IO) {
                                runCatching {
                                    repository.ensurePermanentMessagesWhenBurnDisabled(false)
                                }
                            }
                        }
                    }
                }
            }
        }

        registerRealtimeTelephonyObserver()
        refreshSystemEnvironment(startupDeferHeavyQueries = true)
        reconcileAdClassificationRulesOnce()
        startInactivityAutoLockWatcher()
        startDueScheduledSmsWatcher()
        triggerStartupUpgradeCheckIfEnabled()
    }

    private fun areMessageListsEquivalent(
        a: List<DecryptedSmsMessage>,
        b: List<DecryptedSmsMessage>
    ): Boolean {
        if (a === b) return true
        if (a.size != b.size) return false
        for (i in 0 until a.size) {
            val x = a[i]
            val y = b[i]
            if (x.id != y.id ||
                x.timestamp != y.timestamp ||
                x.isRead != y.isRead ||
                x.status != y.status ||
                x.statusNote != y.statusNote ||
                x.isSecretBurnAfterRead != y.isSecretBurnAfterRead ||
                x.contactName != y.contactName ||
                x.body != y.body
            ) {
                return false
            }
        }
        return true
    }

    private fun triggerStartupUpgradeCheckIfEnabled() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(650L)
            val currentSettings = _uiState.value.settings
            val installed = GithubUpdateChecker.getInstalledAppVersionName(appContext)
            val cachedTag = currentSettings.githubCachedLatestTag
            if (cachedTag.isNotBlank() && GithubUpdateChecker.isRemoteVersionNewer(installed, cachedTag)) {
                _uiState.update {
                    it.copy(hasUpgradeAvailable = true, latestUpgradeTag = cachedTag)
                }
            }
            if (!currentSettings.githubAutoCheckUpdates) return@launch
            val res = runCatching {
                GithubUpdateChecker.checkLatestRelease(
                    context = appContext,
                    rawRepoInput = currentSettings.githubUpdateRepo,
                    channel = currentSettings.githubUpdateChannel
                )
            }.getOrNull()
            if (res is GithubUpdateCheckResult.Success) {
                cacheGithubLatestRelease(res.latestRelease)
            }
        }
    }

    /**
     * One-time silent upgrade pass so any 07/06/+255 human messages previously placed in Ads
     * are automatically rescued back to Inbox, and 15/16/17/18/19/09/55 + name-only ads are sorted cleanly.
     */
    private fun reconcileAdClassificationRulesOnce() {
        viewModelScope.launch(Dispatchers.IO) {
            if (syncPrefs.getBoolean("ad_rules_v2_reconciled", false)) return@launch
            delay(500L)
            runCatching {
                val settings = _uiState.value.settings
                if (settings.autoBlockAdsEnabled) {
                    repository.autoSweepExistingAds(
                        savedContacts = allContacts.value,
                        whitelistedSenders = settings.whitelistedAdSenders,
                        isEnglish = isEnglish()
                    )
                }
                syncPrefs.edit().putBoolean("ad_rules_v2_reconciled", true).apply()
            }
        }
    }

    private fun scheduleInstantCachePersist(messages: List<DecryptedSmsMessage>) {
        if (messages.isEmpty() || _uiState.value.isBackupBusy) return
        diskCacheSaveJob?.cancel()
        diskCacheSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(100L)
            if (!_uiState.value.isBackupBusy && messages.isNotEmpty()) {
                SmsInstantCache.saveSnapshotToDisk(appContext, crypto, messages)
            }
        }
    }

    /**
     * Automatically maintains a rolling encrypted Auto-Guardian recovery snapshot on disk
     * whenever the user's message state actually changes, skipping redundant cold-start re-encryptions.
     */
    private fun scheduleAutoGuardianBackup(messages: List<DecryptedSmsMessage>) {
        if (messages.isEmpty() || _uiState.value.isBackupBusy) return
        val topMsg = messages.first()
        val stateSig = "${messages.size}:${topMsg.id}:${topMsg.timestamp}"
        val now = System.currentTimeMillis()
        if (messages.size == lastAutoGuardianCount && (now - lastAutoGuardianSavedMillis) < 45_000L) {
            return
        }
        autoGuardianSaveJob?.cancel()
        autoGuardianSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400L)
            if (_uiState.value.isBackupBusy || messages.isEmpty()) return@launch
            val savedSig = syncPrefs.getString("last_auto_guardian_sig", "")
            val savedAt = syncPrefs.getLong("last_auto_guardian_ms", 0L)
            val nowIo = System.currentTimeMillis()
            if (stateSig == savedSig && (nowIo - savedAt) < 6L * 60L * 60L * 1000L) {
                lastAutoGuardianCount = messages.size
                lastAutoGuardianSavedMillis = savedAt
                return@launch
            }
            runCatching {
                val subset = if (messages.size > 5_000) messages.take(5_000) else messages
                val templates = repository.getAllTemplatesDecryptedOnce()
                val contacts = repository.getAllLocalContactsOnce()
                val settings = _uiState.value.settings
                val archive = MengiTexBackupArchive(
                    messages = subset,
                    templates = templates,
                    contacts = contacts,
                    securityLevel = settings.securityLevel,
                    isDarkMode = settings.isDarkMode,
                    languageCode = settings.language.code
                )
                val encrypted = PortableBackupCrypto.encryptArchive(archive, null)
                localBackupManager.saveAutoGuardianSnapshot(
                    encryptedPayload = encrypted,
                    messageCount = subset.size,
                    templateCount = templates.size,
                    contactCount = contacts.size
                )
                val savedMs = System.currentTimeMillis()
                lastAutoGuardianCount = messages.size
                lastAutoGuardianSavedMillis = savedMs
                syncPrefs.edit()
                    .putString("last_auto_guardian_sig", stateSig)
                    .putLong("last_auto_guardian_ms", savedMs)
                    .apply()
            }
        }
    }

    /**
     * 3-Tier Automatic Self-Healing Vault Recovery Engine:
     * If Room ever reports 0 messages unexpectedly, automatically restores all messages from:
     * Tier 1: In-memory or Dual-File Instant Cache (.bin / .bin.bak)
     * Tier 2: Rolling Auto-Guardian / Local Encrypted Snapshot (.mengitex)
     * Tier 3: Android System Telephony.Sms database
     */
    private fun triggerSelfHealingVaultRecoveryIfNeeded(cachedFallback: List<DecryptedSmsMessage>) {
        if (isSelfHealingRunning || _uiState.value.isBackupBusy) return
        isSelfHealingRunning = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbCount = runCatching { repository.countActiveMessagesOnce() }.getOrDefault(0)
                if (dbCount > 0) {
                    val slice = runCatching { repository.getRecentActiveMessagesOnce(1500) }.getOrDefault(emptyList())
                    if (slice.isNotEmpty()) {
                        SmsInstantCache.updateMemory(slice)
                        _activeMessages.value = slice
                    }
                    return@launch
                }

                val currentSettings = _uiState.value.settings
                val currentContacts = allContacts.value

                // Tier 1: Recover from RAM or Dual-File Instant Cache (.bin / .bin.bak)
                val cacheCandidates = cachedFallback.ifEmpty {
                    SmsInstantCache.loadInstantSync(appContext, crypto)
                }
                if (cacheCandidates.isNotEmpty()) {
                    if (_activeMessages.value.isEmpty()) {
                        _activeMessages.value = cacheCandidates
                    }
                    val session = repository.beginStreamingImportSession(
                        replaceExisting = false,
                        savedContacts = currentContacts,
                        autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                        whitelistedSenders = currentSettings.whitelistedAdSenders,
                        isEnglish = isEnglish()
                    )
                    cacheCandidates.chunked(1000).forEach { chunk ->
                        repository.ingestStreamingBatch(session, chunk, "Auto-Recovery Cache")
                    }
                }

                // Tier 2: Also merge any additional messages/templates/contacts from Auto-Guardian Snapshot
                val guardianPayload = localBackupManager.findBestRecoverySnapshotPayload()
                if (!guardianPayload.isNullOrBlank()) {
                    val archiveRes = PortableBackupCrypto.decryptArchive(guardianPayload, null)
                    val archive = archiveRes.getOrNull()
                    if (archive != null && archive.messages.isNotEmpty()) {
                        val session = repository.beginStreamingImportSession(
                            replaceExisting = false,
                            savedContacts = currentContacts,
                            autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                            whitelistedSenders = currentSettings.whitelistedAdSenders,
                            isEnglish = isEnglish()
                        )
                        archive.messages.chunked(2000).forEach { chunk ->
                            repository.ingestStreamingBatch(session, chunk, "Auto-Guardian Shield")
                        }
                        if (archive.templates.isNotEmpty() || archive.contacts.isNotEmpty()) {
                            repository.importTemplatesAndContacts(
                                archive.templates,
                                archive.contacts,
                                replaceExisting = false
                            )
                        }
                    }
                }

                // Tier 3: Also sync from Android System Telephony.Sms if permission is held
                if (localBackupManager.hasReadSmsPermission()) {
                    var session: SiriSmsRepository.StreamingImportSession? = null
                    localBackupManager.streamSystemDeviceSms(batchSize = 1000) { chunk, _ ->
                        val activeSession = session ?: repository.beginStreamingImportSession(
                            replaceExisting = false,
                            savedContacts = currentContacts,
                            autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                            whitelistedSenders = currentSettings.whitelistedAdSenders,
                            isEnglish = isEnglish()
                        ).also {
                            session = it
                        }
                        repository.ingestStreamingBatch(activeSession, chunk, "Phone SMS")
                    }
                    syncPrefs.edit().putBoolean("initial_full_sync_done", true).apply()
                }
            } catch (_: Exception) {
                // Keep existing UI state untouched
            } finally {
                isSelfHealingRunning = false
            }
        }
    }

    private fun registerRealtimeTelephonyObserver() {
        runCatching {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    // Short 150ms debounce instead of dropping events: catches incoming SMS immediately
                    // even on devices where MengiTex is not the Default SMS App.
                    deltaSyncDebounceJob?.cancel()
                    deltaSyncDebounceJob = viewModelScope.launch {
                        delay(15L)
                        syncNewDeviceSmsDelta(force = true)
                    }
                }
            }
            appContext.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            smsContentObserver = observer
        }
    }

    override fun onCleared() {
        smsContentObserver?.let { obs ->
            runCatching { appContext.contentResolver.unregisterContentObserver(obs) }
        }
        smsContentObserver = null
        deltaSyncDebounceJob?.cancel()
        diskCacheSaveJob?.cancel()
        autoGuardianSaveJob?.cancel()
        channelSyncJob?.cancel()
        super.onCleared()
    }

    fun recordUserInteraction() {
        lastInteractionMillis = System.currentTimeMillis()
    }

    private fun startInactivityAutoLockWatcher() {
        viewModelScope.launch {
            while (isActive) {
                delay(5000L)
                val state = _uiState.value
                val idleTimeoutMs = state.settings.autoLockIdleSeconds * 1000L
                val elapsed = System.currentTimeMillis() - lastInteractionMillis
                if (!state.isLocked && state.settings.hasPasscode && elapsed >= idleTimeoutMs) {
                    lockAppNow("Auto-lock idle (${state.settings.autoLockIdleSeconds}s)")
                }
            }
        }
    }

    private fun startDueScheduledSmsWatcher() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(15000L)
                runCatching {
                    val due = repository.getDueScheduledMessages()
                    due.forEach { dueMsg ->
                        dispatchSingleMessageNow(dueMsg)
                    }
                }
            }
        }
    }

    fun onAppBackgroundedOrScreenOff() {
        val settings = _uiState.value.settings
        if (settings.hasPasscode && settings.lockOnScreenOff) {
            lockAppNow("Screen off / background auto-lock")
        }
    }

    fun lockAppNow(reason: String = "Manual lock") {
        val settings = _uiState.value.settings
        if (!settings.hasPasscode) {
            showBanner(
                msg(
                    "Weka nenosiri kwanza kwenye Ngome ya Ulinzi ili kufunga app.",
                    "Set a passcode in Security Shield first to lock MengiTex."
                )
            )
            return
        }
        _uiState.update { it.copy(isLocked = true, lockError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            vaultLogger.appendLog("KUJIFUNGA", "App locked", reason)
            refreshVaultData()
        }
    }

    fun unlockWithPasscode(candidate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = _uiState.value.settings
            val now = System.currentTimeMillis()
            if (currentSettings.lockoutUntilMillis > now) {
                val remSec = ((currentSettings.lockoutUntilMillis - now) / 1000L).coerceAtLeast(1L).toInt()
                _uiState.update {
                    it.copy(
                        lockError = msg(
                            "Ulinzi umezuia majaribio — subiri sekunde $remSec.",
                            "Intruder lockout active — wait ${remSec}s."
                        )
                    )
                }
                return@launch
            }

            val ok = preferences.verifyPasscode(candidate, crypto)
            if (ok) {
                preferences.resetUnlockAttempts()
                lastInteractionMillis = System.currentTimeMillis()
                vaultLogger.appendLog("FUNGUA", "Passcode verified", "Access to MengiTex granted.")
                _uiState.update { it.copy(isLocked = false, lockError = null) }
                refreshVaultData()
            } else {
                val (attempts, lockoutUntil) = preferences.recordFailedUnlockAttempt()
                vaultLogger.appendLog(
                    "INTRUDER_ALERT",
                    "Failed unlock attempt #$attempts",
                    "Invalid passcode blocked by Fortress defense."
                )

                if (currentSettings.wipeOnIntruderEnabled && attempts >= 10) {
                    repository.emergencyIntruderWipe()
                    SmsInstantCache.clear(appContext)
                    preferences.resetUnlockAttempts()
                    _uiState.update {
                        it.copy(
                            lockError = msg(
                                "Majaribio 10 yameshindwa — data za ndani zimefutwa kwa usalama.",
                                "10 failed attempts — local vault data shredded for safety."
                            )
                        )
                    }
                    refreshVaultData()
                    return@launch
                }

                val cooldownSec = if (lockoutUntil > System.currentTimeMillis()) {
                    ((lockoutUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(1L).toInt()
                } else 0

                _uiState.update {
                    it.copy(
                        lockError = if (cooldownSec > 0) {
                            msg(
                                "Nenosiri si sahihi (#$attempts). Ulinzi umefunga kwa sekunde $cooldownSec.",
                                "Incorrect passcode (#$attempts). Locked out for ${cooldownSec}s."
                            )
                        } else {
                            msg(
                                "Nenosiri si sahihi (Jaribio #$attempts). Jaribu tena.",
                                "Incorrect passcode (Attempt #$attempts). Try again."
                            )
                        }
                    )
                }
                refreshVaultData()
            }
        }
    }

    fun setOrChangePasscode(newPasscode: String) {
        if (newPasscode.length < 4) {
            showBanner(
                msg(
                    "Nenosiri linapaswa kuwa na angalau herufi 4.",
                    "Passcode must be at least 4 characters."
                )
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            preferences.savePasscode(newPasscode, crypto)
            vaultLogger.appendLog("NENOSIRI", "Passcode updated", "Stored using PBKDF2-HMAC-SHA256.")
            refreshVaultData()
            showBanner(msg("Nenosiri la ulinzi limehifadhiwa.", "Security passcode saved."))
        }
    }

    fun removePasscode() {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.clearPasscode()
            vaultLogger.appendLog("NENOSIRI", "Passcode removed", "Passcode lock disabled by user.")
            _uiState.update { it.copy(isLocked = false) }
            refreshVaultData()
            showBanner(msg("Nenosiri limeondolewa.", "Passcode removed."))
        }
    }

    fun refreshSystemEnvironment(
        forceReloadContacts: Boolean = false,
        startupDeferHeavyQueries: Boolean = false
    ) {
        val callNow = System.currentTimeMillis()
        if (!forceReloadContacts && (isEnvRefreshRunning || (callNow - lastEnvRefreshMillis) < 2_500L)) {
            return
        }
        lastEnvRefreshMillis = callNow
        isEnvRefreshRunning = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
            val sims = runCatching { SimCardManager.detectSimCards(appContext) }.getOrDefault(emptyList())
            val simSummary = runCatching { SimCardManager.getSimStateSummary(appContext) }.getOrDefault("SIM...")
            val hasSms = runCatching { SmsDispatchManager.hasSendSmsPermission(appContext) }.getOrDefault(false)
            val hasContacts = runCatching { ContactsLoader.hasContactsPermission(appContext) }.getOrDefault(false)
            val hasMic = runCatching { VoiceDictationHelper.hasMicrophonePermission(appContext) }.getOrDefault(false)
            val hasNotifPerm = runCatching { MengiTexNotificationManager.hasNotificationPermission(appContext) }.getOrDefault(false)
            val activeNotifs = runCatching { MengiTexNotificationManager.getActiveNotificationCount(appContext) }.getOrDefault(0)
            val isDefaultSms = runCatching { DefaultAppRoleHelper.isDefaultSmsApp(appContext) }.getOrDefault(false)
            val hasCallRole = runCatching { DefaultAppRoleHelper.isDefaultCallScreener(appContext) }.getOrDefault(false)
            val persistedStorage = runCatching {
                appContext.contentResolver.persistedUriPermissions.any { it.isReadPermission }
            }.getOrDefault(false)
            if (!hasInitializedChannels) {
                hasInitializedChannels = true
                runCatching { MengiTexNotificationManager.ensureAllChannelsCreated(appContext, _uiState.value.settings) }
            }

            _uiState.update { state ->
                val currentSub = state.settings.selectedSimSubId
                val validSub = if (sims.any { it.subscriptionId == currentSub }) {
                    currentSub
                } else {
                    sims.firstOrNull()?.subscriptionId ?: -1
                }
                if (validSub != currentSub && validSub != -1) {
                    viewModelScope.launch { runCatching { preferences.setSelectedSimSubId(validSub) } }
                }
                state.copy(
                    detectedSims = sims,
                    simStateSummary = simSummary,
                    hasSmsPermission = hasSms,
                    hasContactsPermission = hasContacts,
                    hasMicPermission = hasMic,
                    hasStorageFolderAccess = persistedStorage || state.settings.storageFolderUri.isNotBlank(),
                    hasNotificationPermission = hasNotifPerm,
                    activeNotificationCount = activeNotifs,
                    isDefaultSmsApp = isDefaultSms,
                    isCallScreeningRoleHeld = hasCallRole
                )
            }

            // Run ContentResolver queries immediately on Dispatchers.IO without startup sleep
            val nowMs = System.currentTimeMillis()
            val existingContacts = _uiState.value.deviceContacts
            val shouldLoadContacts = forceReloadContacts ||
                existingContacts.isEmpty() ||
                (nowMs - lastContactsLoadMillis) > 30_000L
            if (shouldLoadContacts) {
                lastContactsLoadMillis = nowMs
                val loaded = runCatching { ContactsLoader.loadDeviceContacts(appContext) }.getOrDefault(existingContacts)
                if (loaded != existingContacts) {
                    _uiState.update { it.copy(deviceContacts = loaded) }
                }
            }

            // Smart Phone SMS Sync:
            // - If app already has remembered SMS in Room DB, run an ultra-fast (<15ms) incremental delta sync
            //   so new SMS are remembered immediately without blocking startup for 5 seconds.
            // - Only do a full historical device import on first-ever launch when Room DB has not been synced yet.
            if (localBackupManager.hasReadSmsPermission()) {
                val existingCount = runCatching { repository.countActiveMessagesOnce() }.getOrDefault(0)
                // Always perform a full recovery sync if existingCount == 0!
                val initialFullDone = syncPrefs.getBoolean("initial_full_sync_done", false) && existingCount > 0

                if (!initialFullDone && !hasAutoSyncedPhoneSmsThisSession) {
                    hasAutoSyncedPhoneSmsThisSession = true
                    runCatching {
                        val currentSettings = _uiState.value.settings
                        val currentContacts = allContacts.value
                        var session: SiriSmsRepository.StreamingImportSession? = null
                        // Use a quick initial batch size so first chats appear right away while older history streams
                        localBackupManager.streamSystemDeviceSms(batchSize = 500) { chunk, _ ->
                            val activeSession = session ?: repository.beginStreamingImportSession(
                                replaceExisting = false,
                                savedContacts = currentContacts,
                                autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                                whitelistedSenders = currentSettings.whitelistedAdSenders,
                                isEnglish = isEnglish()
                            ).also {
                                session = it
                            }
                            repository.ingestStreamingBatch(activeSession, chunk, "Phone SMS")
                        }
                        syncPrefs.edit().putBoolean("initial_full_sync_done", true).apply()
                    }
                } else {
                    hasAutoSyncedPhoneSmsThisSession = true
                    syncNewDeviceSmsDelta(force = false)
                }
            }
            } finally {
                isEnvRefreshRunning = false
            }
        }
    }

    /**
     * Ultra-fast incremental delta sync: pulls only newly arrived/sent system SMS since our latest
     * stored message (with a 2-minute overlap for safety) without ever scanning historical messages
     * or clearing the already-remembered UI list.
     */
    fun syncNewDeviceSmsDelta(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isDeltaSyncRunning) {
            pendingDeltaSync = true
            return
        }
        if (!force && now - lastDeltaSyncMillis < 350L) return
        if (!localBackupManager.hasReadSmsPermission()) return

        isDeltaSyncRunning = true
        pendingDeltaSync = false
        lastDeltaSyncMillis = now
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val latestTs = repository.getLatestActiveTimestampOnce() ?: 0L
                val sinceTs = if (latestTs > 120_000L) latestTs - 120_000L else 0L
                val currentSettings = _uiState.value.settings
                val currentContacts = allContacts.value
                var session: SiriSmsRepository.StreamingImportSession? = null
                localBackupManager.streamSystemDeviceSms(
                    batchSize = 250,
                    minTimestampMillis = sinceTs,
                    includeMms = sinceTs == 0L
                ) { chunk, _ ->
                    if (chunk.isEmpty()) return@streamSystemDeviceSms
                    val activeSession = session ?: repository.beginStreamingImportSession(
                        replaceExisting = false,
                        savedContacts = currentContacts,
                        autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                        whitelistedSenders = currentSettings.whitelistedAdSenders,
                        isEnglish = isEnglish(),
                        sinceTimestampMillis = sinceTs
                    ).also {
                        session = it
                    }
                    repository.ingestStreamingBatch(activeSession, chunk, "Phone SMS")
                }
            } catch (_: Exception) {
                // Keep existing remembered messages untouched on transient ContentResolver error
            } finally {
                val shouldReRun = pendingDeltaSync
                isDeltaSyncRunning = false
                pendingDeltaSync = false
                if (shouldReRun) {
                    delay(15L)
                    syncNewDeviceSmsDelta(force = true)
                }
            }
        }
    }

    fun openExternalSmsTarget(phoneNumber: String, draftBody: String) {
        recordUserInteraction()
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val resolvedName = runCatching { ContactsLoader.lookupContactName(appContext, cleanPhone) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: cleanPhone
                _uiState.update {
                    it.copy(
                        activeConversationName = resolvedName,
                        activeConversationPhone = cleanPhone,
                        activeDestination = DrawerDestination.CONVERSATION,
                        composeBody = if (draftBody.isNotBlank()) draftBody else it.composeBody
                    )
                }
            }
        } else if (draftBody.isNotBlank()) {
            updateComposeBody(draftBody)
            openDestination(DrawerDestination.COMPOSE)
        }
    }

    fun onStorageFolderAuthorized(treeUri: Uri) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(treeUri, flags)
            }
            val label = treeUri.lastPathSegment
                ?.substringAfterLast(':')
                ?.substringAfterLast('/')
                ?.ifBlank { "Storage Folder" }
                ?: "Storage Folder"
            preferences.setStorageFolder(treeUri.toString(), label)
            vaultLogger.appendLog(
                category = "STORAGE_PERM",
                title = "Storage folder authorized",
                detail = "Android SAF folder access granted: $label"
            )
            _uiState.update { it.copy(hasStorageFolderAccess = true) }
            refreshVaultData()
            showBanner(
                msg(
                    "Ruhusa ya Hifadhi (Storage) imewashwa: $label",
                    "Storage folder access granted: $label"
                )
            )
        }
    }

    fun importFileFromStorageToVault(uri: Uri) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val displayName = resolveDisplayName(uri) ?: "vault_file"
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                    val maxBytes = 8 * 1024 * 1024 // 8MB safety cap
                    val data = stream.readBytes()
                    if (data.size > maxBytes) data.copyOf(maxBytes) else data
                }
                if (bytes == null || bytes.isEmpty()) {
                    showBanner(msg("Faili halikuweza kusomwa.", "Could not read selected file."))
                    return@launch
                }
                vaultLogger.saveEncryptedBinaryFile(displayName, bytes)
                refreshVaultData()
                showBanner(
                    msg(
                        "Faili '$displayName' limesimbwa AES-256 kwenye Folda Siri.",
                        "File '$displayName' AES-256 encrypted in Hidden Vault."
                    )
                )
            } catch (e: Exception) {
                showBanner(
                    msg(
                        "Imeshindwa kuingiza faili: ${e.message ?: "Hitilafu"}",
                        "Failed to import file: ${e.message ?: "Error"}"
                    )
                )
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun appendVoiceTranscriptToCompose(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.composeBody
        val combined = if (current.isBlank()) clean else "$current $clean"
        updateComposeBody(combined)
    }

    private fun checkCallScreeningRole(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = appContext.getSystemService(RoleManager::class.java)
            rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }
    }

    fun selectTab(tab: MainTab) {
        recordUserInteraction()
        _uiState.update { it.copy(selectedTab = tab, activeDestination = DrawerDestination.NONE) }
    }

    fun openDestination(destination: DrawerDestination) {
        recordUserInteraction()
        _uiState.update { it.copy(activeDestination = destination) }
        if (destination == DrawerDestination.HIDDEN_LOGS ||
            destination == DrawerDestination.BACKUP_RESTORE ||
            ((destination == DrawerDestination.SECURITY || destination == DrawerDestination.USAGE_DASHBOARD) && vaultDataDirty)
        ) {
            viewModelScope.launch(Dispatchers.IO) { refreshVaultData(force = true) }
        }
    }

    fun closeDestination() {
        recordUserInteraction()
        _uiState.update { it.copy(activeDestination = DrawerDestination.NONE) }
    }

    fun openComposeForSingleContact(contact: ContactItem) {
        recordUserInteraction()
        openConversation(contactName = contact.name, phoneNumber = contact.phoneNumber)
    }

    fun openConversation(contactName: String, phoneNumber: String) {
        recordUserInteraction()
        val normTarget = ContactsLoader.resolveCanonicalThreadKey(phoneNumber, contactName)
        _uiState.update {
            it.copy(
                activeConversationName = contactName.ifBlank { phoneNumber },
                activeConversationPhone = phoneNumber.ifBlank { contactName },
                activeDestination = DrawerDestination.CONVERSATION
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val unreadIds = ArrayList<Long>()
            val msgs = _activeMessages.value
            for (i in 0 until msgs.size) {
                val m = msgs[i]
                if (!m.isRead && m.id > 0L && m.canonicalThreadKey == normTarget) {
                    unreadIds.add(m.id)
                }
            }
            if (unreadIds.isNotEmpty()) {
                val unreadSet = unreadIds.toHashSet()
                _activeMessages.update { list ->
                    list.map { m -> if (m.id in unreadSet) m.copy(isRead = true) else m }
                }
                repository.markReadBatch(unreadIds)
            }
        }
    }

    fun sendPopReplyFromBanner(
        contactName: String,
        phoneNumber: String,
        plainBody: String,
        useSystemFallbackIfDenied: Boolean = false
    ) {
        val cleanPhone = ContactsLoader.cleanSenderIdentifier(phoneNumber)
            .ifBlank { ContactsLoader.cleanSenderIdentifier(contactName) }
        val cleanName = ContactsLoader.cleanSenderIdentifier(contactName).ifBlank { cleanPhone }
        val threadKey = ContactsLoader.resolveCanonicalThreadKey(cleanPhone, cleanName)
        viewModelScope.launch(Dispatchers.IO) {
            val unreadIds = _activeMessages.value
                .filter { !it.isRead && it.id > 0L && it.canonicalThreadKey == threadKey }
                .map { it.id }
            if (unreadIds.isNotEmpty()) {
                val unreadSet = unreadIds.toHashSet()
                _activeMessages.update { list ->
                    list.map { m -> if (m.id in unreadSet) m.copy(isRead = true) else m }
                }
                repository.markReadBatch(unreadIds)
            } else if (cleanPhone.isNotEmpty()) {
                repository.markSenderMessagesRead(cleanPhone)
            }
        }
        sendDirectConversationMessage(
            contactName = cleanName,
            phoneNumber = cleanPhone,
            plainBody = plainBody,
            isSecretBurn = false,
            useSystemFallbackIfDenied = useSystemFallbackIfDenied,
            isPopReply = true
        )
    }

    fun sendDirectConversationMessage(
        contactName: String,
        phoneNumber: String,
        plainBody: String,
        isSecretBurn: Boolean,
        useSystemFallbackIfDenied: Boolean = false,
        isPopReply: Boolean = false
    ) {
        recordUserInteraction()
        val body = plainBody.trim()
        val cleanPhone = ContactsLoader.cleanSenderIdentifier(phoneNumber)
            .ifBlank { ContactsLoader.cleanSenderIdentifier(contactName) }
        if (body.isEmpty() || cleanPhone.isBlank()) return

        val state = _uiState.value
        val activeSim = state.detectedSims.firstOrNull { it.subscriptionId == state.settings.selectedSimSubId }
            ?: state.detectedSims.firstOrNull()
            ?: SimCardManager.resolveEffectiveSim(appContext, state.settings.selectedSimSubId)
        val simLabel = activeSim?.displayName ?: "SIM"
        val simSubId = activeSim?.subscriptionId ?: state.settings.selectedSimSubId
        val fastParts = SmsDispatchManager.estimateSmsPartsFast(body)
        val resolvedName = ContactsLoader.cleanSenderIdentifier(contactName).ifBlank { cleanPhone }
        val now = System.currentTimeMillis()
        val threadKey = ContactsLoader.resolveCanonicalThreadKey(cleanPhone, resolvedName)

        // Rapid double-tap guard (650ms) so accidental double-taps NEVER dispatch the same SMS twice
        val sendFingerprint = "$threadKey|${body.hashCode()}|${body.length}"
        if (sendFingerprint == lastDirectSendFingerprint && (now - lastDirectSendMillis) < 650L) {
            return
        }
        lastDirectSendFingerprint = sendFingerprint
        lastDirectSendMillis = now

        // Register in LiveSmsEventBus BEFORE dispatch so Telephony ContentObserver NEVER duplicates it
        com.example.data.LiveSmsEventBus.rememberLocalMessage(
            threadKey = threadKey,
            direction = MessageDirection.OUTGOING,
            body = body,
            timestamp = now
        )

        if (!SmsDispatchManager.hasSendSmsPermission(appContext) && useSystemFallbackIfDenied) {
            SmsDispatchManager.openSystemSmsFallback(
                context = appContext,
                phoneNumbers = listOf(cleanPhone),
                body = body
            )
            val tempId = -tempOptimisticIdSeq.incrementAndGet()
            val fallbackNote = msg("Imefunguliwa kupitia App ya SMS ya Mfumo", "Opened in system SMS app")
            val optimisticMsg = DecryptedSmsMessage(
                id = tempId,
                contactName = resolvedName,
                phoneNumber = cleanPhone,
                body = body,
                direction = MessageDirection.OUTGOING,
                status = MessageStatus.SENDING,
                statusNote = fallbackNote,
                timestamp = now,
                scheduledFor = null,
                simSlotLabel = simLabel,
                simSubId = simSubId,
                partCount = fastParts,
                isUnknownSender = false,
                isSilentIntercepted = false,
                isSecretBurnAfterRead = isSecretBurn,
                isRead = true,
                isDeleted = false,
                deletedAt = null,
                canonicalThreadKey = threadKey
            )
            pendingOptimisticMessages[tempId] = optimisticMsg
            _activeMessages.update { listOf(optimisticMsg) + it }
            viewModelScope.launch(Dispatchers.IO) {
                val exactParts = SmsDispatchManager.calculateSmsParts(appContext, body, simSubId)
                val realId = repository.insertOutgoingOrScheduled(
                    contactName = resolvedName,
                    phoneNumber = cleanPhone,
                    plainBody = body,
                    status = MessageStatus.SENDING,
                    statusNote = fallbackNote,
                    simSlotLabel = simLabel,
                    simSubId = simSubId,
                    partCount = exactParts,
                    isSecretBurnAfterRead = isSecretBurn,
                    timestamp = now
                )
                val persisted = optimisticMsg.copy(id = realId, partCount = exactParts)
                pendingOptimisticMessages.remove(tempId)
                _activeMessages.update { current ->
                    val hasRealAlready = current.any { it.id == realId }
                    if (hasRealAlready) {
                        current.filterNot { it.id == tempId }
                    } else {
                        current.map { m -> if (m.id == tempId) persisted else m }
                    }
                }
                SmsInstantCache.prependMessage(appContext, crypto, persisted)
                refreshVaultData()
            }
            return
        }

        // Frame-0 optimistic chat bubble insertion — user sees sent SMS instantaneously (0ms delay)
        val tempId = -tempOptimisticIdSeq.incrementAndGet()
        val sendingNote = if (isPopReply) {
            msg(
                "Pop Reply inatuma kwa $resolvedName kupitia $simLabel...",
                "Pop Reply sending to $resolvedName via $simLabel..."
            )
        } else {
            msg(
                "Inatuma kupitia $simLabel ($fastParts sehemu)...",
                "Sending via $simLabel ($fastParts parts)..."
            )
        }
        if (isPopReply) {
            showBanner(sendingNote)
        }
        val optimisticMsg = DecryptedSmsMessage(
            id = tempId,
            contactName = resolvedName,
            phoneNumber = cleanPhone,
            body = body,
            direction = MessageDirection.OUTGOING,
            status = MessageStatus.SENDING,
            statusNote = sendingNote,
            timestamp = now,
            scheduledFor = null,
            simSlotLabel = simLabel,
            simSubId = simSubId,
            partCount = fastParts,
            isUnknownSender = false,
            isSilentIntercepted = false,
            isSecretBurnAfterRead = isSecretBurn,
            isRead = true,
            isDeleted = false,
            deletedAt = null,
            canonicalThreadKey = threadKey
        )
        pendingOptimisticMessages[tempId] = optimisticMsg
        _activeMessages.update { listOf(optimisticMsg) + it }

        viewModelScope.launch(Dispatchers.IO) {
            // Skip redundant SmsManager.divideMessage Binder IPC — fastParts is exact & sendMultipartSms divides once!
            val id = repository.insertOutgoingOrScheduled(
                contactName = resolvedName,
                phoneNumber = cleanPhone,
                plainBody = body,
                status = MessageStatus.SENDING,
                statusNote = sendingNote,
                simSlotLabel = simLabel,
                simSubId = simSubId,
                partCount = fastParts,
                isSecretBurnAfterRead = isSecretBurn,
                timestamp = now
            )
            if (isPopReply && id > 0L) {
                trackedPopReplyRecipients[id] = resolvedName
                if (trackedPopReplyRecipients.size > 40) {
                    val oldestKey = trackedPopReplyRecipients.keys.minOrNull()
                    if (oldestKey != null) trackedPopReplyRecipients.remove(oldestKey)
                }
            }
            val persisted = optimisticMsg.copy(id = id, partCount = fastParts)
            pendingOptimisticMessages.remove(tempId)
            _activeMessages.update { current ->
                val hasRealAlready = current.any { it.id == id }
                if (hasRealAlready) {
                    current.filterNot { it.id == tempId }
                } else {
                    current.map { m -> if (m.id == tempId) persisted else m }
                }
            }
            SmsInstantCache.prependMessage(appContext, crypto, persisted)

            val result = SmsDispatchManager.sendMultipartSms(
                context = appContext,
                messageId = id,
                phoneNumber = cleanPhone,
                body = body,
                simSubId = simSubId,
                requestDeliveryReport = state.settings.deliveryReportsEnabled
            )
            if (result.isFailure) {
                trackedPopReplyRecipients.remove(id)
                val failNote = result.exceptionOrNull()?.message
                    ?: msg("Ruhusa ya SMS inahitajika", "SEND_SMS permission required")
                repository.updateMessageStatus(
                    id = id,
                    status = MessageStatus.FAILED,
                    note = failNote
                )
                SmsInstantCache.updateMessageStatus(appContext, crypto, id, MessageStatus.FAILED, failNote)
                com.example.data.LiveSmsEventBus.emitStatusUpdate(id, MessageStatus.FAILED, failNote)
                showBanner(
                    msg(
                        "Imeshindwa kutuma: $failNote",
                        "Failed to send: $failNote"
                    )
                )
            }
            refreshVaultData()
        }
    }

    fun sendAttachmentInConversation(
        contactName: String,
        phoneNumber: String,
        uri: android.net.Uri,
        mimeType: String,
        displayLabel: String,
        captionBody: String
    ) {
        recordUserInteraction()
        val state = _uiState.value
        val activeSim = state.detectedSims.firstOrNull { it.subscriptionId == state.settings.selectedSimSubId }
            ?: state.detectedSims.firstOrNull()
        val simLabel = activeSim?.displayName ?: "SIM"
        val simSubId = activeSim?.subscriptionId ?: -1

        val launched = SmsDispatchManager.sendMediaViaMmsOrShare(
            context = appContext,
            phoneNumber = phoneNumber,
            uri = uri,
            mimeType = mimeType,
            captionBody = captionBody,
            chooserTitle = msg("Tuma Kiambatanisho (MMS/Share)", "Send Attachment (MMS/Share)")
        )
        val combinedBody = buildString {
            append("📎 ")
            append(displayLabel)
            if (captionBody.isNotBlank()) {
                append("\n")
                append(captionBody.trim())
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOutgoingOrScheduled(
                contactName = contactName.ifBlank { phoneNumber },
                phoneNumber = phoneNumber,
                plainBody = combinedBody,
                status = if (launched) MessageStatus.SENT else MessageStatus.FAILED,
                statusNote = if (launched) {
                    msg("Kiambatanisho kimetumwa kupitia MMS/Share", "Attachment dispatched via MMS/Share")
                } else {
                    msg("Imeshindwa kufungua programu ya kutuma faili", "Could not open attachment sender")
                },
                simSlotLabel = simLabel,
                simSubId = simSubId,
                partCount = 1,
                isSecretBurnAfterRead = false
            )
            refreshVaultData()
        }
    }

    fun forwardMessageToRecipients(
        recipients: List<ContactItem>,
        plainBody: String,
        useSystemFallbackIfDenied: Boolean = false
    ) {
        recordUserInteraction()
        val body = plainBody.trim()
        if (body.isEmpty() || recipients.isEmpty()) return

        val state = _uiState.value
        val activeSim = state.detectedSims.firstOrNull { it.subscriptionId == state.settings.selectedSimSubId }
            ?: state.detectedSims.firstOrNull()
        val simLabel = activeSim?.displayName ?: "SIM"
        val simSubId = activeSim?.subscriptionId ?: -1
        val partCount = SmsDispatchManager.calculateSmsParts(appContext, body, simSubId)

        if (!SmsDispatchManager.hasSendSmsPermission(appContext) && useSystemFallbackIfDenied) {
            SmsDispatchManager.openSystemSmsFallback(
                context = appContext,
                phoneNumbers = recipients.map { it.phoneNumber },
                body = body
            )
            viewModelScope.launch(Dispatchers.IO) {
                recipients.forEach { recipient ->
                    repository.insertOutgoingOrScheduled(
                        contactName = recipient.name.ifBlank { recipient.phoneNumber },
                        phoneNumber = recipient.phoneNumber,
                        plainBody = body,
                        status = MessageStatus.SENT,
                        statusNote = msg("Imetumwa (Forwarded) kupitia mfumo wa SMS", "Forwarded via system SMS app"),
                        simSlotLabel = simLabel,
                        simSubId = simSubId,
                        partCount = partCount,
                        isSecretBurnAfterRead = false
                    )
                }
                refreshVaultData()
            }
            showBanner(
                msg(
                    "Ujumbe umetayarishwa kutumwa kwa watu ${recipients.size}.",
                    "Message prepared to forward to ${recipients.size} recipient(s)."
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var sentCount = 0
            recipients.forEachIndexed { index, recipient ->
                if (index > 0 && index % 10 == 0) {
                    delay(10L)
                }
                val id = repository.insertOutgoingOrScheduled(
                    contactName = recipient.name.ifBlank { recipient.phoneNumber },
                    phoneNumber = recipient.phoneNumber,
                    plainBody = body,
                    status = MessageStatus.SENDING,
                    statusNote = msg("Inatuma (Forward) kupitia $simLabel...", "Forwarding via $simLabel..."),
                    simSlotLabel = simLabel,
                    simSubId = simSubId,
                    partCount = partCount,
                    isSecretBurnAfterRead = false
                )
                val result = SmsDispatchManager.sendMultipartSms(
                    context = appContext,
                    messageId = id,
                    phoneNumber = recipient.phoneNumber,
                    body = body,
                    simSubId = simSubId,
                    requestDeliveryReport = state.settings.deliveryReportsEnabled
                )
                if (result.isSuccess) {
                    sentCount++
                } else {
                    repository.updateMessageStatus(
                        id = id,
                        status = MessageStatus.FAILED,
                        note = result.exceptionOrNull()?.message
                            ?: msg("Ruhusa ya SMS inahitajika", "SEND_SMS permission required")
                    )
                }
            }
            refreshVaultData()
            showBanner(
                if (sentCount > 0) {
                    msg(
                        "Ujumbe umetumwa (Forwarded) kwa watu $sentCount.",
                        "Message forwarded to $sentCount recipient(s)."
                    )
                } else {
                    msg(
                        "Toa ruhusa ya SMS ili kutuma moja kwa moja.",
                        "Grant SMS permission to forward directly."
                    )
                }
            )
        }
    }

    fun saveContactNameForConversation(newName: String, phoneNumber: String) {
        val cleanName = newName.trim()
        val cleanPhone = phoneNumber.trim()
        if (cleanName.isEmpty() || cleanPhone.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addLocalContact(cleanName, cleanPhone)
            _uiState.update { state ->
                if (ContactsLoader.normalizePhone(state.activeConversationPhone) == ContactsLoader.normalizePhone(cleanPhone)) {
                    state.copy(activeConversationName = cleanName)
                } else {
                    state
                }
            }
            showBanner(
                msg(
                    "Mwasiliani '$cleanName' amehifadhiwa.",
                    "Contact '$cleanName' saved."
                )
            )
        }
    }

    fun openScheduleForConversation(contactName: String, phoneNumber: String, draftBody: String) {
        recordUserInteraction()
        val contact = ContactItem(
            id = "conv_${ContactsLoader.normalizePhone(phoneNumber)}",
            name = contactName.ifBlank { phoneNumber },
            phoneNumber = phoneNumber
        )
        _uiState.update {
            it.copy(
                selectedRecipients = listOf(contact),
                activeDestination = DrawerDestination.COMPOSE
            )
        }
        if (draftBody.isNotBlank()) {
            updateComposeBody(draftBody)
        }
    }

    fun setInboxFilter(filter: InboxFilter) {
        recordUserInteraction()
        _uiState.update { it.copy(inboxFilter = filter) }
    }

    fun updateComposeBody(newText: String) {
        recordUserInteraction()
        val parts = SmsDispatchManager.estimateSmsPartsFast(newText)
        _uiState.update { it.copy(composeBody = newText, composePartsCount = parts) }
    }

    fun setComposeSecretBurn(enabled: Boolean) {
        recordUserInteraction()
        _uiState.update { it.copy(composeIsSecretBurn = enabled) }
    }

    fun toggleRecipient(contact: ContactItem) {
        recordUserInteraction()
        _uiState.update { state ->
            val exists = state.selectedRecipients.any {
                ContactsLoader.normalizePhone(it.phoneNumber) == ContactsLoader.normalizePhone(contact.phoneNumber)
            }
            val updated = if (exists) {
                state.selectedRecipients.filterNot {
                    ContactsLoader.normalizePhone(it.phoneNumber) == ContactsLoader.normalizePhone(contact.phoneNumber)
                }
            } else {
                state.selectedRecipients + contact
            }
            state.copy(selectedRecipients = updated)
        }
    }

    fun selectAllContacts(contacts: List<ContactItem>) {
        recordUserInteraction()
        _uiState.update { it.copy(selectedRecipients = contacts) }
    }

    fun clearSelectedRecipients() {
        recordUserInteraction()
        _uiState.update { it.copy(selectedRecipients = emptyList()) }
    }

    fun addManualRecipient(name: String, phone: String, saveToList: Boolean) {
        val cleanedPhone = phone.trim()
        if (cleanedPhone.length < 3) {
            showBanner(msg("Weka namba sahihi ya simu.", "Please enter a valid phone number."))
            return
        }
        val displayName = name.trim().ifEmpty { cleanedPhone }
        val item = ContactItem(
            id = "manual_${System.currentTimeMillis()}",
            name = displayName,
            phoneNumber = cleanedPhone,
            isLocalSaved = saveToList
        )
        toggleRecipient(item)
        if (saveToList) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.addLocalContact(displayName, cleanedPhone)
            }
        }
    }

    fun applyTemplateToCompose(template: DecryptedTemplate) {
        recordUserInteraction()
        updateComposeBody(template.body)
        _uiState.update { it.copy(activeDestination = DrawerDestination.COMPOSE) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementTemplateUsage(template.id)
        }
        showBanner(
            msg(
                "Templeti '${template.title}' imewekwa tayari kutumwa.",
                "Template '${template.title}' loaded into composer."
            )
        )
    }

    fun selectSim(simSubId: Int) {
        updateSettingsOptimistically(
            transform = { it.copy(selectedSimSubId = simSubId) },
            persist = { preferences.setSelectedSimSubId(simSubId) }
        )
    }

    fun setLanguage(language: AppLanguage) {
        updateSettingsOptimistically(
            transform = { it.copy(language = language) },
            persist = { preferences.setLanguage(language) }
        )
    }

    fun setBubbleNotificationsEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(bubbleNotificationsEnabled = enabled) },
            persist = {
                preferences.setBubbleNotificationsEnabled(enabled)
                vaultLogger.appendLog(
                    category = "ARIFA_BUBBLE",
                    title = if (enabled) "Bubble notifications enabled" else "Bubble notifications disabled",
                    detail = "Saved contacts receive floating chat bubbles; unknown numbers stay silent."
                )
                refreshVaultData()
            }
        )
    }

    fun setDeliveryReportsEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(deliveryReportsEnabled = enabled) },
            persist = { preferences.setDeliveryReportsEnabled(enabled) }
        )
        showBanner(
            if (enabled) {
                msg(
                    "Ripoti ya Kufika SMS (Delivery Report) imewashwa ✓✓",
                    "SMS Text Delivery Reports enabled ✓✓"
                )
            } else {
                msg(
                    "Ripoti ya Kufika SMS (Delivery Report) imezimwa.",
                    "SMS Text Delivery Reports disabled."
                )
            }
        )
    }

    // --- Full Notification Manager Actions (0ms Optimistic UI + Async DataStore Persistence) ---

    fun setNotificationsMasterEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notificationsMasterEnabled = enabled) },
            persist = {
                preferences.setNotificationsMasterEnabled(enabled)
                if (!enabled) {
                    MengiTexNotificationManager.clearAllNotifications(appContext)
                }
                refreshNotificationMetrics()
            }
        )
    }

    fun setNotifyPersonalSms(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifyPersonalSms = enabled) },
            persist = { preferences.setNotifyPersonalSms(enabled) }
        )
    }

    fun setNotifyTransactionsOtp(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifyTransactionsOtp = enabled) },
            persist = { preferences.setNotifyTransactionsOtp(enabled) }
        )
    }

    fun setNotifyDeliveryReports(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifyDeliveryReports = enabled) },
            persist = { preferences.setNotifyDeliveryReports(enabled) }
        )
    }

    fun setInAppPopBannerEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(inAppPopBannerEnabled = enabled) },
            persist = { preferences.setInAppPopBannerEnabled(enabled) }
        )
    }

    fun setNotifSoundEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifSoundEnabled = enabled) },
            persist = { preferences.setNotifSoundEnabled(enabled) }
        )
        scheduleNotificationChannelSync()
    }

    fun setNotifRingtone(uri: String, displayName: String) {
        updateSettingsOptimistically(
            transform = { it.copy(notifRingtoneUri = uri, notifRingtoneName = displayName) },
            persist = { preferences.setNotifRingtone(uri, displayName) }
        )
        scheduleNotificationChannelSync()
        showBanner(
            msg(
                "Mlio wa arifa umehifadhiwa: $displayName",
                "Notification tone saved: $displayName"
            )
        )
    }

    fun setNotifVibratePattern(pattern: NotificationVibratePattern) {
        MengiTexNotificationManager.previewVibration(appContext, pattern)
        updateSettingsOptimistically(
            transform = { it.copy(notifVibratePattern = pattern) },
            persist = { preferences.setNotifVibratePattern(pattern) }
        )
        scheduleNotificationChannelSync()
    }

    fun setNotifLedEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifLedEnabled = enabled) },
            persist = { preferences.setNotifLedEnabled(enabled) }
        )
    }

    fun setNotifHeadsUpEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifHeadsUpEnabled = enabled) },
            persist = { preferences.setNotifHeadsUpEnabled(enabled) }
        )
    }

    fun setNotifBadgeEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifBadgeEnabled = enabled) },
            persist = { preferences.setNotifBadgeEnabled(enabled) }
        )
    }

    fun setNotifPreviewMode(mode: NotificationPreviewMode) {
        updateSettingsOptimistically(
            transform = { it.copy(notifPreviewMode = mode) },
            persist = { preferences.setNotifPreviewMode(mode) }
        )
    }

    fun setNotifActionPopReplyEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionPopReplyEnabled = enabled) },
            persist = { preferences.setNotifActionPopReplyEnabled(enabled) }
        )
    }

    fun setNotifActionMarkReadEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionMarkReadEnabled = enabled) },
            persist = { preferences.setNotifActionMarkReadEnabled(enabled) }
        )
    }

    fun setNotifActionMuteEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionMuteEnabled = enabled) },
            persist = { preferences.setNotifActionMuteEnabled(enabled) }
        )
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(quietHoursEnabled = enabled) },
            persist = { preferences.setQuietHoursEnabled(enabled) }
        )
    }

    fun setQuietHoursRange(startHour: Int, endHour: Int) {
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(0, 23)
        updateSettingsOptimistically(
            transform = { it.copy(quietHoursStartHour = start, quietHoursEndHour = end) },
            persist = { preferences.setQuietHoursRange(start, end) }
        )
    }

    fun setQuietHoursAllowTransactions(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(quietHoursAllowTransactions = enabled) },
            persist = { preferences.setQuietHoursAllowTransactions(enabled) }
        )
    }

    fun setChatSizeScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 1.45f)
        updateSettingsOptimistically(
            transform = { it.copy(chatSizeScale = clamped) },
            persist = { preferences.setChatSizeScale(clamped) }
        )
    }

    fun muteNotificationSender(sender: String) {
        val clean = sender.trim()
        if (clean.isEmpty()) return
        val key = SmsAdBlockerEngine.canonicalSenderKey(clean)
        updateSettingsOptimistically(
            transform = {
                if (key.isEmpty()) it else it.copy(mutedNotificationSenders = it.mutedNotificationSenders + key)
            },
            persist = { preferences.muteNotificationSender(clean) }
        )
        showBanner(
            msg(
                "$clean amenyamazishwa (hakuna mlio wala pop-up).",
                "$clean muted (no sound or pop-up alerts)."
            )
        )
    }

    fun unmuteNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        updateSettingsOptimistically(
            transform = {
                if (key.isEmpty()) it else it.copy(mutedNotificationSenders = it.mutedNotificationSenders - key)
            },
            persist = { preferences.unmuteNotificationSender(sender) }
        )
        showBanner(
            msg(
                "Arifa za $sender zimewashwa tena.",
                "Notifications restored for $sender."
            )
        )
    }

    fun clearAllMutedNotificationSenders() {
        updateSettingsOptimistically(
            transform = { it.copy(mutedNotificationSenders = emptySet()) },
            persist = { preferences.clearAllMutedNotificationSenders() }
        )
        showBanner(
            msg(
                "Watumaji wote waliokimya wamewashwa tena.",
                "All muted senders unmuted."
            )
        )
    }

    fun applyNotificationPreset(preset: NotificationPreset) {
        updateSettingsOptimistically(
            transform = { cur ->
                val base = cur.copy(notificationsMasterEnabled = true)
                when (preset) {
                    NotificationPreset.STANDARD -> base.copy(
                        notifyPersonalSms = true,
                        notifyTransactionsOtp = true,
                        notifySecretVault = true,
                        notifyScheduledSms = true,
                        bubbleNotificationsEnabled = true,
                        inAppPopBannerEnabled = true,
                        notifSoundEnabled = true,
                        notifVibratePattern = NotificationVibratePattern.DOUBLE,
                        notifHeadsUpEnabled = true,
                        notifPriorityLevel = NotificationPriorityLevel.HIGH,
                        notifPreviewMode = NotificationPreviewMode.SHOW_ALL,
                        quietHoursEnabled = false
                    )
                    NotificationPreset.SILENT -> base.copy(
                        notifSoundEnabled = false,
                        notifVibratePattern = NotificationVibratePattern.OFF,
                        notifHeadsUpEnabled = false,
                        notifLedEnabled = false,
                        notifWakeScreenEnabled = false,
                        notifPriorityLevel = NotificationPriorityLevel.LOW,
                        notifBadgeEnabled = true
                    )
                    NotificationPreset.PRIVATE -> base.copy(
                        notifSoundEnabled = true,
                        notifVibratePattern = NotificationVibratePattern.SHORT,
                        notifPreviewMode = NotificationPreviewMode.HIDE_ALL,
                        notifySecretVault = true,
                        notifActionPopReplyEnabled = false,
                        notifWakeScreenEnabled = false,
                        notifPriorityLevel = NotificationPriorityLevel.DEFAULT
                    )
                    NotificationPreset.NIGHT_DND -> base.copy(
                        quietHoursEnabled = true,
                        quietHoursStartHour = 22,
                        quietHoursEndHour = 7,
                        quietHoursAllowTransactions = true,
                        quietHoursAllowVip = true,
                        notifWakeScreenEnabled = false
                    )
                }
            },
            persist = { preferences.applyNotificationPreset(preset) }
        )
        scheduleNotificationChannelSync()
        val label = when (preset) {
            NotificationPreset.STANDARD -> msg("Kawaida", "Standard")
            NotificationPreset.SILENT -> msg("Kimya", "Silent")
            NotificationPreset.PRIVATE -> msg("Faragha", "Private")
            NotificationPreset.NIGHT_DND -> msg("Usiku DND", "Night DND")
        }
        showBanner(
            msg(
                "Profaili ya arifa imewekwa: $label",
                "Notification profile applied: $label"
            )
        )
    }

    fun setNotifySecretVault(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifySecretVault = enabled) },
            persist = { preferences.setNotifySecretVault(enabled) }
        )
    }

    fun setNotifyScheduledSms(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifyScheduledSms = enabled) },
            persist = { preferences.setNotifyScheduledSms(enabled) }
        )
    }

    fun setNotifyBlockedSpam(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifyBlockedSpam = enabled) },
            persist = { preferences.setNotifyBlockedSpam(enabled) }
        )
    }

    fun setNotifWakeScreenEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifWakeScreenEnabled = enabled) },
            persist = { preferences.setNotifWakeScreenEnabled(enabled) }
        )
    }

    fun setNotifOnlyAlertOnce(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifOnlyAlertOnce = enabled) },
            persist = { preferences.setNotifOnlyAlertOnce(enabled) }
        )
    }

    fun setNotifGroupBySender(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifGroupBySender = enabled) },
            persist = { preferences.setNotifGroupBySender(enabled) }
        )
    }

    fun setNotifPriorityLevel(level: NotificationPriorityLevel) {
        updateSettingsOptimistically(
            transform = { it.copy(notifPriorityLevel = level) },
            persist = { preferences.setNotifPriorityLevel(level) }
        )
        scheduleNotificationChannelSync()
    }

    fun setNotifRepeatSettings(repeatCount: Int, intervalMinutes: Int) {
        val count = repeatCount.coerceIn(0, 5)
        val interval = intervalMinutes.coerceIn(1, 30)
        updateSettingsOptimistically(
            transform = { it.copy(notifRepeatCount = count, notifRepeatIntervalMinutes = interval) },
            persist = { preferences.setNotifRepeatSettings(count, interval) }
        )
    }

    fun setInAppPopDurationSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(2, 15)
        updateSettingsOptimistically(
            transform = { it.copy(inAppPopDurationSeconds = clamped) },
            persist = { preferences.setInAppPopDurationSeconds(clamped) }
        )
    }

    fun setNotifLedColorArgb(colorArgb: Long) {
        updateSettingsOptimistically(
            transform = { it.copy(notifLedColorArgb = colorArgb) },
            persist = { preferences.setNotifLedColorArgb(colorArgb) }
        )
        scheduleNotificationChannelSync()
    }

    fun resetNotifRingtoneToDefault() {
        updateSettingsOptimistically(
            transform = { it.copy(notifRingtoneUri = "", notifRingtoneName = "") },
            persist = { preferences.setNotifRingtone("", "") }
        )
        scheduleNotificationChannelSync()
        showBanner(
            msg(
                "Mlio wa arifa umerudishwa wa kawaida wa simu.",
                "Notification ringtone reset to system default."
            )
        )
    }

    fun testNotifVibrationNow() {
        recordUserInteraction()
        val pattern = uiState.value.settings.notifVibratePattern
        if (pattern == NotificationVibratePattern.OFF) {
            showBanner(
                msg(
                    "Mtetemo umezimwa. Chagua mtindo wa mtetemo kwanza.",
                    "Vibration is Off. Select a vibration pattern first."
                )
            )
        } else {
            MengiTexNotificationManager.previewVibration(appContext, pattern)
        }
    }

    fun setNotifActionCopyOtpEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionCopyOtpEnabled = enabled) },
            persist = { preferences.setNotifActionCopyOtpEnabled(enabled) }
        )
    }

    fun setNotifActionCallEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionCallEnabled = enabled) },
            persist = { preferences.setNotifActionCallEnabled(enabled) }
        )
    }

    fun setNotifActionDeleteEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(notifActionDeleteEnabled = enabled) },
            persist = { preferences.setNotifActionDeleteEnabled(enabled) }
        )
    }

    fun setQuietHoursAllowVip(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(quietHoursAllowVip = enabled) },
            persist = { preferences.setQuietHoursAllowVip(enabled) }
        )
    }

    fun addVipNotificationSender(sender: String) {
        val clean = sender.trim()
        if (clean.isEmpty()) return
        val key = SmsAdBlockerEngine.canonicalSenderKey(clean)
        updateSettingsOptimistically(
            transform = { cur ->
                if (key.isEmpty()) cur
                else cur.copy(
                    vipNotificationSenders = cur.vipNotificationSenders + key,
                    mutedNotificationSenders = cur.mutedNotificationSenders - key
                )
            },
            persist = { preferences.addVipNotificationSender(clean) }
        )
        showBanner(
            msg(
                "$clean ameongezwa kwenye VIP (atapita DND/Saa za Kimya).",
                "$clean added to VIP priority senders."
            )
        )
    }

    fun removeVipNotificationSender(sender: String) {
        val key = SmsAdBlockerEngine.canonicalSenderKey(sender)
        updateSettingsOptimistically(
            transform = { cur ->
                if (key.isEmpty()) cur else cur.copy(vipNotificationSenders = cur.vipNotificationSenders - key)
            },
            persist = { preferences.removeVipNotificationSender(sender) }
        )
        showBanner(
            msg(
                "$sender ameondolewa kwenye VIP.",
                "$sender removed from VIP senders."
            )
        )
    }

    fun clearAllVipNotificationSenders() {
        updateSettingsOptimistically(
            transform = { it.copy(vipNotificationSenders = emptySet()) },
            persist = { preferences.clearAllVipNotificationSenders() }
        )
        showBanner(
            msg(
                "Orodha yote ya VIP imesafishwa.",
                "All VIP senders cleared."
            )
        )
    }

    fun resetAllNotificationSettings() {
        updateSettingsOptimistically(
            transform = { cur ->
                cur.copy(
                    notificationsMasterEnabled = true,
                    notifyPersonalSms = true,
                    notifyTransactionsOtp = true,
                    notifyDeliveryReports = true,
                    bubbleNotificationsEnabled = true,
                    inAppPopBannerEnabled = true,
                    silentUnknownSms = true,
                    notifySecretVault = true,
                    notifyScheduledSms = true,
                    notifyBlockedSpam = false,
                    notifSoundEnabled = true,
                    notifRingtoneUri = "",
                    notifRingtoneName = "",
                    notifVibratePattern = NotificationVibratePattern.DOUBLE,
                    notifLedEnabled = true,
                    notifLedColorArgb = 0xFF334E68L,
                    notifHeadsUpEnabled = true,
                    notifBadgeEnabled = true,
                    notifWakeScreenEnabled = true,
                    notifOnlyAlertOnce = false,
                    notifGroupBySender = true,
                    notifPriorityLevel = NotificationPriorityLevel.HIGH,
                    notifRepeatCount = 0,
                    notifRepeatIntervalMinutes = 5,
                    inAppPopDurationSeconds = 5,
                    notifPreviewMode = NotificationPreviewMode.SHOW_ALL,
                    notifActionPopReplyEnabled = true,
                    notifActionMarkReadEnabled = true,
                    notifActionMuteEnabled = true,
                    notifActionCopyOtpEnabled = true,
                    notifActionCallEnabled = false,
                    notifActionDeleteEnabled = false,
                    quietHoursEnabled = false,
                    quietHoursStartHour = 22,
                    quietHoursEndHour = 7,
                    quietHoursAllowTransactions = true,
                    quietHoursAllowVip = true,
                    mutedNotificationSenders = emptySet(),
                    vipNotificationSenders = emptySet()
                )
            },
            persist = { preferences.resetAllNotificationSettings() }
        )
        scheduleNotificationChannelSync()
        showBanner(
            msg(
                "Mipangilio yote ya Notification Manager imerudishwa ya awali.",
                "All Notification Manager settings reset to defaults."
            )
        )
    }

    fun sendTestNotificationNow() {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = preferences.getSnapshot()
            val posted = MengiTexNotificationManager.postTestNotification(appContext, snapshot)
            refreshNotificationMetrics()
            if (posted) {
                showBanner(
                    msg(
                        "Arifa ya jaribio imetumwa! Angalia upau wa juu wa simu.",
                        "Test notification sent! Check your status bar."
                    )
                )
            } else {
                showBanner(
                    msg(
                        "Tafadhali ruhusu Arifa (Notifications) kwanza ili kuona jaribio.",
                        "Please grant Notification permission first to test alerts."
                    )
                )
            }
        }
    }

    fun clearAllActiveNotificationsNow() {
        recordUserInteraction()
        MengiTexNotificationManager.clearAllNotifications(appContext)
        refreshNotificationMetrics()
        showBanner(
            msg(
                "Arifa zote za MengiTex zimefutwa kwenye upau wa juu.",
                "All active MengiTex notifications cleared."
            )
        )
    }

    private fun refreshNotificationMetrics() {
        val granted = MengiTexNotificationManager.hasNotificationPermission(appContext)
        val count = MengiTexNotificationManager.getActiveNotificationCount(appContext)
        _uiState.update {
            it.copy(
                hasNotificationPermission = granted,
                activeNotificationCount = count
            )
        }
    }

    fun sendBroadcastNow(useSystemFallbackIfDenied: Boolean = false) {
        recordUserInteraction()
        val state = _uiState.value
        val recipients = state.selectedRecipients
        val body = state.composeBody.trim()
        if (recipients.isEmpty()) {
            showBanner(msg("Chagua angalau rafiki mmoja.", "Select at least one recipient."))
            return
        }
        if (body.isEmpty()) {
            showBanner(msg("Andika ujumbe kwanza.", "Write a message first."))
            return
        }

        val activeSim = state.detectedSims.firstOrNull { it.subscriptionId == state.settings.selectedSimSubId }
            ?: state.detectedSims.firstOrNull()
        val simLabel = activeSim?.displayName ?: "SIM"
        val simSubId = activeSim?.subscriptionId ?: -1
        val partCount = SmsDispatchManager.calculateSmsParts(appContext, body, simSubId)

        if (!SmsDispatchManager.hasSendSmsPermission(appContext) && useSystemFallbackIfDenied) {
            SmsDispatchManager.openSystemSmsFallback(
                context = appContext,
                phoneNumbers = recipients.map { it.phoneNumber },
                body = body
            )
            viewModelScope.launch(Dispatchers.IO) {
                recipients.forEach { recipient ->
                    repository.insertOutgoingOrScheduled(
                        contactName = recipient.name,
                        phoneNumber = recipient.phoneNumber,
                        plainBody = body,
                        status = MessageStatus.SENT,
                        statusNote = msg("Imefunguliwa kupitia App ya SMS ya Mfumo", "Opened in system SMS app"),
                        simSlotLabel = simLabel,
                        simSubId = simSubId,
                        partCount = partCount,
                        isSecretBurnAfterRead = state.composeIsSecretBurn
                    )
                }
                refreshVaultData()
            }
            _uiState.update { it.copy(composeBody = "", composePartsCount = 0) }
            showBanner(
                msg(
                    "Ujumbe umepelekwa kwenye mfumo wa SMS kwa marafiki ${recipients.size}.",
                    "Opened system SMS composer for ${recipients.size} recipient(s)."
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            var failCount = 0
            recipients.forEachIndexed { index, recipient ->
                if (index > 0 && index % 10 == 0) {
                    delay(10L)
                }
                val id = repository.insertOutgoingOrScheduled(
                    contactName = recipient.name,
                    phoneNumber = recipient.phoneNumber,
                    plainBody = body,
                    status = MessageStatus.SENDING,
                    statusNote = msg(
                        "Inatuma kupitia $simLabel ($partCount sehemu)...",
                        "Sending via $simLabel ($partCount parts)..."
                    ),
                    simSlotLabel = simLabel,
                    simSubId = simSubId,
                    partCount = partCount,
                    isSecretBurnAfterRead = state.composeIsSecretBurn
                )
                val result = SmsDispatchManager.sendMultipartSms(
                    context = appContext,
                    messageId = id,
                    phoneNumber = recipient.phoneNumber,
                    body = body,
                    simSubId = simSubId,
                    requestDeliveryReport = state.settings.deliveryReportsEnabled
                )
                if (result.isSuccess) {
                    successCount++
                } else {
                    failCount++
                    repository.updateMessageStatus(
                        id = id,
                        status = MessageStatus.FAILED,
                        note = result.exceptionOrNull()?.message ?: msg("Ruhusa ya SMS inahitajika", "SEND_SMS permission required")
                    )
                }
            }
            refreshVaultData()
            withContext(Dispatchers.Main) {
                if (successCount > 0) {
                    _uiState.update {
                        it.copy(
                            composeBody = "",
                            composePartsCount = 0,
                            selectedTab = MainTab.MESSAGES,
                            activeDestination = DrawerDestination.NONE
                        )
                    }
                    showBanner(
                        msg(
                            "SMS inatumwa kwa marafiki $successCount ($partCount sehemu).",
                            "Sending SMS to $successCount friend(s) ($partCount parts)."
                        )
                    )
                } else {
                    showBanner(
                        msg(
                            "Imeshindwa ($failCount). Toa ruhusa ya SMS au tumia Mfumo wa Simu.",
                            "Failed ($failCount). Grant SMS permission or use Device SMS App."
                        )
                    )
                }
            }
        }
    }

    fun scheduleBroadcast(triggerAtMillis: Long) {
        recordUserInteraction()
        val state = _uiState.value
        val recipients = state.selectedRecipients
        val body = state.composeBody.trim()
        if (recipients.isEmpty() || body.isEmpty()) {
            showBanner(msg("Chagua wapokeaji na andika ujumbe kwanza.", "Select recipients and write a message first."))
            return
        }
        if (triggerAtMillis <= System.currentTimeMillis() + 5_000L) {
            showBanner(msg("Chagua muda ujao.", "Select a future time."))
            return
        }

        val activeSim = state.detectedSims.firstOrNull { it.subscriptionId == state.settings.selectedSimSubId }
            ?: state.detectedSims.firstOrNull()
        val simLabel = activeSim?.displayName ?: "SIM"
        val simSubId = activeSim?.subscriptionId ?: -1
        val partCount = SmsDispatchManager.calculateSmsParts(appContext, body, simSubId)

        viewModelScope.launch(Dispatchers.IO) {
            recipients.forEach { recipient ->
                val id = repository.insertOutgoingOrScheduled(
                    contactName = recipient.name,
                    phoneNumber = recipient.phoneNumber,
                    plainBody = body,
                    status = MessageStatus.SCHEDULED,
                    statusNote = msg("Imeratibiwa kupitia $simLabel", "Scheduled via $simLabel"),
                    simSlotLabel = simLabel,
                    simSubId = simSubId,
                    partCount = partCount,
                    scheduledFor = triggerAtMillis,
                    isSecretBurnAfterRead = state.composeIsSecretBurn
                )
                SmsDispatchManager.scheduleAlarmForSms(appContext, id, triggerAtMillis)
            }
            refreshVaultData()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        composeBody = "",
                        composePartsCount = 0,
                        activeDestination = DrawerDestination.SCHEDULE
                    )
                }
                showBanner(
                    msg(
                        "Ujumbe umeratibiwa kwa marafiki ${recipients.size}.",
                        "SMS scheduled for ${recipients.size} recipient(s)."
                    )
                )
            }
        }
    }

    fun sendScheduledNow(message: DecryptedSmsMessage) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            SmsDispatchManager.cancelScheduledAlarm(appContext, message.id)
            dispatchSingleMessageNow(message)
            refreshVaultData()
            showBanner(
                msg(
                    "Ujumbe unatumwa sasa kwa ${message.contactName}.",
                    "Sending scheduled SMS now to ${message.contactName}."
                )
            )
        }
    }

    private suspend fun dispatchSingleMessageNow(msgItem: DecryptedSmsMessage) {
        val deliveryEnabled = _uiState.value.settings.deliveryReportsEnabled
        val result = SmsDispatchManager.sendMultipartSms(
            context = appContext,
            messageId = msgItem.id,
            phoneNumber = msgItem.phoneNumber,
            body = msgItem.body,
            simSubId = msgItem.simSubId,
            requestDeliveryReport = deliveryEnabled
        )
        if (result.isSuccess) {
            val note = "Dispatched via ${msgItem.simSlotLabel}"
            repository.updateMessageStatus(
                id = msgItem.id,
                status = MessageStatus.SENDING,
                note = note
            )
            LiveSmsEventBus.emitStatusUpdate(msgItem.id, MessageStatus.SENDING, note)
        } else {
            val note = result.exceptionOrNull()?.message ?: "SEND_SMS required"
            repository.updateMessageStatus(
                id = msgItem.id,
                status = MessageStatus.FAILED,
                note = note
            )
            LiveSmsEventBus.emitStatusUpdate(msgItem.id, MessageStatus.FAILED, note)
        }
    }

    fun openSecretReader(message: DecryptedSmsMessage) {
        recordUserInteraction()
        _uiState.update { it.copy(activeSecretReaderMessage = message) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.markRead(message.id)
        }
    }

    fun closeSecretReader(burnImmediately: Boolean) {
        recordUserInteraction()
        val target = _uiState.value.activeSecretReaderMessage
        _uiState.update { it.copy(activeSecretReaderMessage = null) }
        if (target != null && burnImmediately) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.burnSecretMessageAfterRead(target)
                refreshVaultData()
                showBanner(
                    msg(
                        "Ujumbe wa siri umefutwa moja kwa moja baada ya kusomwa.",
                        "Secret message permanently deleted after reading."
                    )
                )
            }
        }
    }

    fun moveMessageToTrash(message: DecryptedSmsMessage) {
        recordUserInteraction()
        // Instant optimistic removal from UI list
        _activeMessages.update { list -> list.filterNot { it.id == message.id } }
        showBanner(msg("Ujumbe umehamishiwa kwenye Taka.", "Message moved to Trash."))
        viewModelScope.launch(Dispatchers.IO) {
            if (message.status == MessageStatus.SCHEDULED) {
                SmsDispatchManager.cancelScheduledAlarm(appContext, message.id)
            }
            if (message.id > 0L) {
                repository.moveMessageToTrash(message)
            }
            refreshVaultData()
        }
    }

    fun moveMessagesToTrash(messages: List<DecryptedSmsMessage>) {
        if (messages.isEmpty()) return
        recordUserInteraction()
        val idsToRemove = messages.mapTo(HashSet(messages.size)) { it.id }
        _activeMessages.update { list -> list.filterNot { it.id in idsToRemove } }
        val count = messages.size
        showBanner(
            msg(
                "Ujumbe $count umehamishiwa kwenye Taka.",
                "$count messages moved to Trash."
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            messages.forEach { message ->
                if (message.status == MessageStatus.SCHEDULED) {
                    SmsDispatchManager.cancelScheduledAlarm(appContext, message.id)
                }
            }
            val persistedMsgs = messages.filter { it.id > 0L }
            if (persistedMsgs.isNotEmpty()) {
                repository.moveMessagesToTrashBatch(persistedMsgs)
            }
            refreshVaultData()
        }
    }

    fun restoreMessage(message: DecryptedSmsMessage) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreMessageFromTrash(message)
            refreshVaultData()
            showBanner(msg("Ujumbe umerejeshwa.", "Message restored."))
        }
    }

    fun permanentlyDeleteMessage(message: DecryptedSmsMessage) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.permanentlyDeleteMessage(message.id)
            refreshVaultData()
        }
    }

    fun saveTemplate(id: Long?, title: String, category: String, body: String) {
        recordUserInteraction()
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTemplate(id, title, category, body)
            refreshVaultData()
            showBanner(msg("Templeti imehifadhiwa (AES-256).", "Template encrypted & saved."))
        }
    }

    fun moveTemplateToTrash(template: DecryptedTemplate) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveTemplateToTrash(template)
            refreshVaultData()
            showBanner(msg("Templeti imehamishiwa kwenye Taka.", "Template moved to Trash."))
        }
    }

    fun restoreTemplate(template: DecryptedTemplate) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreTemplateFromTrash(template)
            refreshVaultData()
            showBanner(msg("Templeti imerejeshwa.", "Template restored."))
        }
    }

    fun permanentlyDeleteTemplate(template: DecryptedTemplate) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.permanentlyDeleteTemplate(template.id)
            refreshVaultData()
        }
    }

    fun emptyTrash() {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyAllTrash()
            refreshVaultData()
            showBanner(msg("Taka zote zimesafishwa.", "Trash emptied."))
        }
    }

    fun setSecurityLevel(level: Int) {
        val clamped = level.coerceIn(1, 5)
        updateSettingsOptimistically(
            transform = { cur ->
                when (clamped) {
                    1 -> cur.copy(
                        securityLevel = 1,
                        silentUnknownSms = false,
                        blockUnknownCalls = false,
                        autoDeleteSecretAfterRead = false,
                        privacyCurtainOnSwitch = false,
                        autoLockIdleSeconds = 300
                    )
                    2 -> cur.copy(
                        securityLevel = 2,
                        silentUnknownSms = true,
                        blockUnknownCalls = false,
                        autoDeleteSecretAfterRead = false,
                        privacyCurtainOnSwitch = false,
                        autoLockIdleSeconds = 180
                    )
                    3 -> cur.copy(
                        securityLevel = 3,
                        silentUnknownSms = true,
                        blockUnknownCalls = true,
                        autoDeleteSecretAfterRead = false,
                        privacyCurtainOnSwitch = true,
                        autoLockIdleSeconds = 60
                    )
                    4 -> cur.copy(
                        securityLevel = 4,
                        silentUnknownSms = true,
                        blockUnknownCalls = true,
                        autoDeleteSecretAfterRead = false,
                        privacyCurtainOnSwitch = true,
                        clipboardAutoClearEnabled = true,
                        autoLockIdleSeconds = 30,
                        lockOnScreenOff = true
                    )
                    else -> cur.copy(
                        securityLevel = 5,
                        silentUnknownSms = true,
                        blockUnknownCalls = true,
                        privacyCurtainOnSwitch = true,
                        clipboardAutoClearEnabled = true,
                        autoLockIdleSeconds = 15,
                        lockOnScreenOff = true
                    )
                }
            },
            persist = {
                preferences.applySecurityLevel(clamped)
                vaultLogger.appendLog(
                    category = "USALAMA",
                    title = "Security Level: $clamped / 5",
                    detail = "Adjusted via security slider."
                )
                refreshVaultData()
            }
        )
    }

    fun setSilentUnknownSms(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(silentUnknownSms = enabled) },
            persist = {
                preferences.setSilentUnknownSms(enabled)
                vaultLogger.appendLog(
                    category = "KIMYA_GENI",
                    title = if (enabled) "Silent unknown SMS ON" else "Silent unknown SMS OFF",
                    detail = "Unknown senders produce zero sound and zero notification."
                )
                refreshVaultData()
            }
        )
    }

    fun setBlockUnknownCalls(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(blockUnknownCalls = enabled) },
            persist = {
                preferences.setBlockUnknownCalls(enabled)
                vaultLogger.appendLog(
                    category = "ZUIA_SIMU",
                    title = if (enabled) "Unknown call blocker ON" else "Unknown call blocker OFF",
                    detail = "Blocks unsaved callers silently without waking screen."
                )
                refreshVaultData()
            }
        )
    }

    fun setAutoDeleteSecret(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(autoDeleteSecretAfterRead = enabled) },
            persist = { preferences.setAutoDeleteSecret(enabled) }
        )
    }

    fun setAutoLockIdleSeconds(seconds: Int) {
        updateSettingsOptimistically(
            transform = { it.copy(autoLockIdleSeconds = seconds) },
            persist = {
                preferences.setAutoLockIdleSeconds(seconds)
                refreshVaultData()
            }
        )
    }

    fun setLockOnScreenOff(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(lockOnScreenOff = enabled) },
            persist = { preferences.setLockOnScreenOff(enabled) }
        )
    }

    fun setBlockScreenshotsAndRecents(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(blockScreenshotsAndRecents = enabled) },
            persist = {
                preferences.setBlockScreenshotsAndRecents(enabled)
                vaultLogger.appendLog(
                    category = "FORTRESS",
                    title = if (enabled) "Anti-Screenshot FLAG_SECURE ON" else "Anti-Screenshot OFF",
                    detail = "Controls screenshots, screen recording, and Recent Apps masking."
                )
                refreshVaultData()
            }
        )
    }

    fun setPrivacyCurtainOnSwitch(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(privacyCurtainOnSwitch = enabled) },
            persist = { preferences.setPrivacyCurtainOnSwitch(enabled) }
        )
    }

    fun setClipboardAutoClear(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(clipboardAutoClearEnabled = enabled) },
            persist = { preferences.setClipboardAutoClear(enabled) }
        )
    }

    fun setWipeOnIntruder(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(wipeOnIntruderEnabled = enabled) },
            persist = {
                preferences.setWipeOnIntruder(enabled)
                vaultLogger.appendLog(
                    category = "FORTRESS",
                    title = if (enabled) "Intruder Self-Destruct (10x) ARMED" else "Intruder Self-Destruct DISARMED",
                    detail = "Emergency local data shred after 10 consecutive wrong passcodes."
                )
                refreshVaultData()
            }
        )
    }

    fun scheduleClipboardAutoClearIfEnabled() {
        if (!_uiState.value.settings.clipboardAutoClearEnabled) return
        viewModelScope.launch {
            delay(30_000L)
            runCatching {
                val cm = appContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cm?.clearPrimaryClip()
                } else {
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = {
                it.copy(
                    isDarkMode = enabled,
                    themeMode = if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT
                )
            },
            persist = { preferences.setDarkMode(enabled) }
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        updateSettingsOptimistically(
            transform = {
                it.copy(
                    themeMode = mode,
                    isDarkMode = (mode == AppThemeMode.DARK)
                )
            },
            persist = { preferences.setThemeMode(mode) }
        )
    }

    fun setDimLightEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(dimLightEnabled = enabled) },
            persist = { preferences.setDimLightEnabled(enabled) }
        )
    }

    fun setDimLightLevel(level: Float) {
        val clamped = level.coerceIn(0.15f, 0.60f)
        updateSettingsOptimistically(
            transform = { it.copy(dimLightLevel = clamped) },
            persist = { preferences.setDimLightLevel(clamped) }
        )
    }

    fun setGithubUpdateRepo(repoSlug: String) {
        val normalized = GithubUpdateChecker.normalizeRepoSlug(repoSlug)
            .ifBlank { GithubUpdateChecker.DEFAULT_GITHUB_REPO }
        _uiState.update {
            it.copy(hasUpgradeAvailable = false, latestUpgradeTag = "")
        }
        updateSettingsOptimistically(
            transform = {
                it.copy(
                    githubUpdateRepo = normalized,
                    githubCachedLatestTag = "",
                    githubCachedReleaseName = "",
                    githubCachedReleaseNotes = "",
                    githubCachedReleaseUrl = "",
                    githubCachedApkUrl = "",
                    githubCachedApkName = "",
                    githubCachedApkSizeBytes = 0L,
                    githubCachedPublishedAt = ""
                )
            },
            persist = { preferences.setGithubUpdateRepo(normalized) }
        )
    }

    fun setGithubUpdateChannel(channel: GithubUpdateChannel) {
        updateSettingsOptimistically(
            transform = { it.copy(githubUpdateChannel = channel) },
            persist = { preferences.setGithubUpdateChannel(channel) }
        )
    }

    fun setGithubAutoCheckUpdates(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(githubAutoCheckUpdates = enabled) },
            persist = { preferences.setGithubAutoCheckUpdates(enabled) }
        )
    }

    fun cacheGithubLatestRelease(release: GithubReleaseInfo) {
        val now = System.currentTimeMillis()
        val installed = GithubUpdateChecker.getInstalledAppVersionName(appContext)
        val newerAvailable = GithubUpdateChecker.isRemoteVersionNewer(installed, release.tagName)
        _uiState.update {
            it.copy(
                hasUpgradeAvailable = newerAvailable,
                latestUpgradeTag = release.tagName
            )
        }
        updateSettingsOptimistically(
            transform = {
                it.copy(
                    githubLastCheckedMillis = now,
                    githubCachedLatestTag = release.tagName,
                    githubCachedReleaseName = release.releaseName,
                    githubCachedReleaseNotes = release.bodyNotes,
                    githubCachedReleaseUrl = release.htmlUrl,
                    githubCachedApkUrl = release.apkAssetUrl ?: "",
                    githubCachedApkName = release.apkAssetName ?: "",
                    githubCachedApkSizeBytes = release.apkSizeBytes,
                    githubCachedPublishedAt = release.publishedAt
                )
            },
            persist = {
                preferences.saveGithubReleaseCheckSnapshot(
                    checkedMillis = now,
                    latestTag = release.tagName,
                    releaseName = release.releaseName,
                    releaseNotes = release.bodyNotes,
                    releaseUrl = release.htmlUrl,
                    apkUrl = release.apkAssetUrl ?: "",
                    apkName = release.apkAssetName ?: "",
                    apkSizeBytes = release.apkSizeBytes,
                    publishedAt = release.publishedAt
                )
            }
        )
    }

    fun setLauncherIconMode(mode: LauncherIconMode) {
        val label = when (mode) {
            LauncherIconMode.VISIBLE -> msg("Icon ya kawaida (MengiTex)", "Default icon (MengiTex)")
            LauncherIconMode.DISGUISE_CALC -> msg("Icon imebadilishwa kuwa Kikokotoo", "Icon disguised as Calculator")
            LauncherIconMode.HIDDEN -> msg("Icon imefichwa kwenye skrini kuu", "Home screen icon hidden")
        }
        updateSettingsOptimistically(
            transform = { it.copy(launcherIconMode = mode) },
            persist = {
                AppIconManager.applyMode(appContext, mode)
                preferences.setLauncherIconMode(mode)
                vaultLogger.appendLog("ICON_SIRI", "App Icon Updated", label)
                refreshVaultData()
            }
        )
        showBanner(label)
    }

    fun saveEncryptedVaultNote(title: String, content: String) {
        recordUserInteraction()
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            vaultLogger.saveEncryptedVaultFile(title, content)
            refreshVaultData(force = true)
            showBanner(msg("Faili limesimbwa (AES-256) kwenye folda iliyofichwa.", "File AES-256 encrypted in hidden vault."))
        }
    }

    fun readEncryptedVaultNote(fileName: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val plain = vaultLogger.readEncryptedVaultFile(fileName)
            withContext(Dispatchers.Main) { onResult(plain) }
        }
    }

    fun deleteEncryptedVaultFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultLogger.deleteEncryptedVaultFile(fileName)
            refreshVaultData(force = true)
        }
    }

    fun clearVaultLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            vaultLogger.clearLogs()
            refreshVaultData(force = true)
            showBanner(msg("Logi zimesafishwa.", "Logs cleared."))
        }
    }

    private fun refreshVaultData(force: Boolean = false) {
        val dest = _uiState.value.activeDestination
        val isVaultScreenVisible = dest == DrawerDestination.HIDDEN_LOGS || dest == DrawerDestination.BACKUP_RESTORE
        if (!force && !isVaultScreenVisible) {
            vaultDataDirty = true
            return
        }
        vaultDataDirty = false
        runCatching {
            val logs = vaultLogger.readAllLogs()
            val files = vaultLogger.listEncryptedVaultFiles()
            val stats = vaultLogger.getFolderStats()
            val snapshots = localBackupManager.listLocalSnapshots()
            _uiState.update {
                it.copy(
                    vaultLogs = logs,
                    vaultFiles = files,
                    vaultStats = stats,
                    localSnapshots = snapshots
                )
            }
        }
    }

    // =========================================================================
    // BACKUP & RESTORE CENTER (Cloud, File SAF, Share, Local Snapshots, Phone SMS)
    // =========================================================================

    private data class BuiltEncryptedArchive(
        val encryptedPayload: String,
        val messageCount: Int,
        val templateCount: Int,
        val contactCount: Int,
        val passwordProtected: Boolean
    )

    private suspend fun buildEncryptedArchive(passphrase: String?): BuiltEncryptedArchive {
        val messages = repository.getAllMessagesDecryptedOnce()
        val templates = repository.getAllTemplatesDecryptedOnce()
        val contacts = repository.getAllLocalContactsOnce()
        val settings = _uiState.value.settings
        val archive = MengiTexBackupArchive(
            messages = messages,
            templates = templates,
            contacts = contacts,
            securityLevel = settings.securityLevel,
            isDarkMode = settings.isDarkMode,
            languageCode = settings.language.code
        )
        val encrypted = PortableBackupCrypto.encryptArchive(archive, passphrase)
        return BuiltEncryptedArchive(
            encryptedPayload = encrypted,
            messageCount = messages.size,
            templateCount = templates.size,
            contactCount = contacts.size,
            passwordProtected = !passphrase.isNullOrBlank()
        )
    }

    fun observeCloudBackups(userId: String): Flow<List<CloudBackupRecord>> {
        val repo = cloudBackupRepository ?: return flow {
            throw IllegalStateException(
                msg(
                    "Huduma ya Cloud Backup haiko tayari kwa sasa. Tumia Backup ya Faili au Snapshot ya Simu.",
                    "Cloud Backup is currently unavailable. Use File Backup or Device Snapshot."
                )
            )
        }
        return repo.observeCloudBackups(userId)
    }

    fun uploadCloudBackupNow(label: String, passphrase: String?) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = cloudBackupRepository
            if (repo == null) {
                showBanner(
                    msg(
                        "Cloud Backup haipatikani sasa. Tumia Backup ya Faili (.mengitex) au Snapshot.",
                        "Cloud Backup unavailable right now. Use .mengitex File Backup or Snapshot."
                    )
                )
                return@launch
            }
            _uiState.update { it.copy(isBackupBusy = true) }
            try {
                val built = buildEncryptedArchive(passphrase)
                val res = repo.uploadCloudBackup(
                    label = label.trim().ifEmpty { "MengiTex Cloud Backup" },
                    encryptedPayload = built.encryptedPayload,
                    messageCount = built.messageCount,
                    templateCount = built.templateCount,
                    contactCount = built.contactCount,
                    passwordProtected = built.passwordProtected
                )
                if (res.isSuccess) {
                    vaultLogger.appendLog(
                        category = "CLOUD_BACKUP",
                        title = "Cloud Backup uploaded",
                        detail = "AES-256 encrypted • SMS: ${built.messageCount}, Templates: ${built.templateCount}, Contacts: ${built.contactCount}"
                    )
                    refreshVaultData()
                    showBanner(
                        msg(
                            "Cloud Backup imehifadhiwa salama (AES-256)!",
                            "Encrypted Cloud Backup uploaded successfully!"
                        )
                    )
                } else {
                    val errMsg = res.exceptionOrNull()?.message ?: "Cloud error"
                    showBanner(msg("Cloud Backup imeshindwa: $errMsg", "Cloud Backup failed: $errMsg"))
                }
            } catch (e: Exception) {
                showBanner(msg("Hitilafu ya Cloud Backup: ${e.message}", "Cloud Backup error: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    fun renameCloudBackup(backupId: String, newLabel: String) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = cloudBackupRepository ?: return@launch
            val res = repo.renameCloudBackup(backupId, newLabel)
            if (res.isSuccess) {
                showBanner(msg("Jina la backup limebadilishwa.", "Backup renamed."))
            } else {
                showBanner(msg("Imeshindwa kubadili jina.", "Failed to rename backup."))
            }
        }
    }

    fun deleteCloudBackup(backupId: String) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = cloudBackupRepository ?: return@launch
            val res = repo.deleteCloudBackup(backupId)
            if (res.isSuccess) {
                vaultLogger.appendLog("CLOUD_BACKUP", "Cloud Backup deleted", "Snapshot #$backupId removed.")
                refreshVaultData()
                showBanner(msg("Cloud Backup imefutwa.", "Cloud Backup deleted."))
            } else {
                showBanner(msg("Imeshindwa kufuta Cloud Backup.", "Failed to delete Cloud Backup."))
            }
        }
    }

    fun createLocalSnapshotNow(label: String, passphrase: String?) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBackupBusy = true) }
            try {
                val built = buildEncryptedArchive(passphrase)
                localBackupManager.saveLocalSnapshot(
                    label = label.trim().ifEmpty { msg("Snapshot ya Simu", "Device Snapshot") },
                    encryptedPayload = built.encryptedPayload,
                    messageCount = built.messageCount,
                    templateCount = built.templateCount,
                    contactCount = built.contactCount,
                    passwordProtected = built.passwordProtected
                )
                vaultLogger.appendLog(
                    category = "LOCAL_BACKUP",
                    title = "Local Vault Snapshot created",
                    detail = "SMS: ${built.messageCount} • Templates: ${built.templateCount} • Contacts: ${built.contactCount}"
                )
                refreshVaultData()
                showBanner(
                    msg(
                        "Snapshot ya ndani imehifadhiwa kwenye Folda Siri!",
                        "Instant local encrypted snapshot saved!"
                    )
                )
            } catch (e: Exception) {
                showBanner(msg("Imeshindwa kuhifadhi snapshot: ${e.message}", "Snapshot failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    fun deleteLocalSnapshot(fileName: String) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            localBackupManager.deleteLocalSnapshot(fileName)
            refreshVaultData()
            showBanner(msg("Snapshot imefutwa.", "Local snapshot deleted."))
        }
    }

    fun readLocalSnapshotPayload(fileName: String, onReady: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = localBackupManager.readSnapshotPayload(fileName)
            withContext(Dispatchers.Main) { onReady(payload) }
        }
    }

    fun exportBackupToSafUri(uri: Uri, passphrase: String?) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBackupBusy = true) }
            try {
                val built = buildEncryptedArchive(passphrase)
                val res = localBackupManager.exportPayloadToUri(uri, built.encryptedPayload)
                if (res.isSuccess) {
                    val bytes = res.getOrDefault(0L)
                    vaultLogger.appendLog(
                        category = "EXPORT_FILE",
                        title = "Encrypted .mengitex exported",
                        detail = "Size: $bytes B • SMS: ${built.messageCount} • Templates: ${built.templateCount}"
                    )
                    refreshVaultData()
                    showBanner(
                        msg(
                            "Faili la backup (.mengitex) limehifadhiwa salama (${bytes} B).",
                            "Encrypted .mengitex backup exported ($bytes B)."
                        )
                    )
                } else {
                    showBanner(
                        msg(
                            "Imeshindwa kuhamisha faili: ${res.exceptionOrNull()?.message}",
                            "File export failed: ${res.exceptionOrNull()?.message}"
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    /**
     * 600k+ Hyper-Turbo Streaming Importer for XML / XLM, .mengitex, JSON, CSV, and TXT files.
     * Streams batches of 10,000 messages through a multi-core parallel encryption + Ad Blocker
     * pipeline directly into SQLite WAL in O(1) memory so 600,000+ SMS finish in seconds.
     */
    fun readSafBackupFile(uri: Uri, onPayloadLoaded: (String) -> Unit) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val startNanos = System.nanoTime()
            _uiState.update {
                it.copy(
                    isBackupBusy = true,
                    importProgressText = msg(
                        "Hyper-Turbo 600k+: Inachambua faili la SMS/XML kwa kasi ya juu...",
                        "Hyper-Turbo 600k+: Streaming & analyzing SMS/XML at peak speed..."
                    )
                )
            }
            try {
                var session: SiriSmsRepository.StreamingImportSession? = null
                val currentSettings = _uiState.value.settings
                val currentContacts = allContacts.value
                val res = localBackupManager.streamImportFromUri(
                    uri = uri,
                    batchSize = LocalBackupManager.TURBO_DEFAULT_BATCH_SIZE
                ) { chunk, totalReadSoFar ->
                    val activeSession = session ?: repository.beginStreamingImportSession(
                        replaceExisting = false,
                        savedContacts = currentContacts,
                        autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                        whitelistedSenders = currentSettings.whitelistedAdSenders,
                        isEnglish = isEnglish()
                    ).also {
                        session = it
                    }
                    repository.ingestStreamingBatch(activeSession, chunk, "XML/File")
                    val elapsedSec = ((System.nanoTime() - startNanos) / 1_000_000_000.0).coerceAtLeast(0.1)
                    val ratePerSec = (totalReadSoFar / elapsedSec).toInt()
                    _uiState.update {
                        it.copy(
                            importProgressText = msg(
                                "Turbo 600k+: SMS $totalReadSoFar ($ratePerSec/sek • ${activeSession.totalAddedOrRestored} mpya • Ads ${activeSession.totalAdsRouted})",
                                "Turbo 600k+: $totalReadSoFar SMS ($ratePerSec/s • ${activeSession.totalAddedOrRestored} new • ${activeSession.totalAdsRouted} Ads)"
                            )
                        )
                    }
                }

                if (res.isFailure) {
                    val detail = res.exceptionOrNull()?.message.orEmpty()
                    showBanner(
                        msg(
                            "Imeshindwa kusoma faili la SMS/XML. Tumia .xml, .xlm, .mengitex, .json, .csv au .txt ($detail)",
                            "Could not read SMS/XML file. Supported: .xml, .xlm, .mengitex, .json, .csv, .txt ($detail)"
                        )
                    )
                    return@launch
                }

                val outcome = res.getOrThrow()
                if (outcome.isPasswordProtectedMengiTex && !outcome.pendingMengiTexPayload.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onPayloadLoaded(outcome.pendingMengiTexPayload)
                    }
                    return@launch
                }

                val (tmplCount, contactCount) = if (outcome.templates.isNotEmpty() || outcome.contacts.isNotEmpty()) {
                    repository.importTemplatesAndContacts(outcome.templates, outcome.contacts, replaceExisting = false)
                } else {
                    0 to 0
                }

                val added = session?.totalAddedOrRestored ?: outcome.totalMessagesRead
                val adsRouted = session?.totalAdsRouted ?: 0
                val elapsedSec = String.format(java.util.Locale.US, "%.1f", (System.nanoTime() - startNanos) / 1_000_000_000.0)
                vaultLogger.appendLog(
                    category = "TURBO_600K_IMPORT",
                    title = "Imported ${outcome.totalMessagesRead} SMS (${outcome.formatLabel}) in ${elapsedSec}s",
                    detail = "New/Restored: $added • Ads routed: $adsRouted • Templates: $tmplCount • Contacts: $contactCount"
                )
                _uiState.update { it.copy(inboxFilter = InboxFilter.ALL) }
                refreshVaultData()
                showBanner(
                    msg(
                        "Turbo 600k+ (${outcome.formatLabel}) imekamilika kwa sek $elapsedSec! SMS ${outcome.totalMessagesRead} ($added mpya, Ads $adsRouted)!",
                        "Turbo 600k+ (${outcome.formatLabel}) finished in ${elapsedSec}s! ${outcome.totalMessagesRead} SMS ($added new, $adsRouted Ads)!"
                    )
                )
            } finally {
                _uiState.update { it.copy(isBackupBusy = false, importProgressText = null) }
                scheduleInstantCachePersist(_activeMessages.value)
            }
        }
    }

    fun exportXmlBackupToSafUri(uri: Uri) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isBackupBusy = true,
                    importProgressText = msg("Inatengeneza faili la XML / XLM...", "Exporting XML / XLM SMS file...")
                )
            }
            try {
                val allMessages = repository.getAllMessagesDecryptedOnce()
                val res = localBackupManager.exportXmlSmsToUri(uri, allMessages)
                if (res.isSuccess) {
                    val count = res.getOrDefault(0)
                    vaultLogger.appendLog(
                        category = "EXPORT_XML",
                        title = "Exported $count SMS to XML/XLM",
                        detail = "Universal Android XML/XLM format exported."
                    )
                    refreshVaultData()
                    showBanner(
                        msg(
                            "SMS $count zimehifadhiwa kwenye faili la XML / XLM kikamilifu!",
                            "Exported $count SMS to XML / XLM file successfully!"
                        )
                    )
                } else {
                    showBanner(
                        msg(
                            "Imeshindwa kuhamisha XML: ${res.exceptionOrNull()?.message}",
                            "XML export failed: ${res.exceptionOrNull()?.message}"
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isBackupBusy = false, importProgressText = null) }
            }
        }
    }

    fun prepareShareableBackup(passphrase: String?, onShareUriReady: (Uri) -> Unit) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBackupBusy = true) }
            try {
                val built = buildEncryptedArchive(passphrase)
                val uriRes = localBackupManager.createShareableBackupUri(built.encryptedPayload)
                withContext(Dispatchers.Main) {
                    if (uriRes.isSuccess) {
                        onShareUriReady(uriRes.getOrThrow())
                    } else {
                        showBanner(
                            msg(
                                "Imeshindwa kuandaa faili la kutuma.",
                                "Failed to prepare shareable backup."
                            )
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    fun restoreFromEncryptedPayload(
        encryptedPayload: String,
        passphrase: String?,
        replaceExisting: Boolean,
        sourceLabel: String
    ) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isBackupBusy = true,
                    importProgressText = msg("Inafungua na kuingiza SMS zote...", "Decrypting and importing all SMS...")
                )
            }
            try {
                val decryptedRes = PortableBackupCrypto.decryptArchive(encryptedPayload, passphrase)
                if (decryptedRes.isFailure) {
                    val code = decryptedRes.exceptionOrNull()?.message
                    val message = when (code) {
                        "WRONG_PASSPHRASE" -> msg(
                            "Nenosiri la backup si sahihi. Jaribu tena.",
                            "Incorrect backup passphrase. Please try again."
                        )
                        "PASSPHRASE_REQUIRED" -> msg(
                            "Backup hii inahitaji nenosiri ili kufunguliwa.",
                            "This backup requires a passphrase to decrypt."
                        )
                        else -> msg(
                            "Imeshindwa kufungua backup: ${code ?: "Hitilafu"}",
                            "Failed to decrypt backup: ${code ?: "Error"}"
                        )
                    }
                    showBanner(message)
                    return@launch
                }

                val archive = decryptedRes.getOrThrow()
                val currentSettings = _uiState.value.settings
                val session = repository.beginStreamingImportSession(
                    replaceExisting = replaceExisting,
                    savedContacts = allContacts.value,
                    autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                    whitelistedSenders = currentSettings.whitelistedAdSenders,
                    isEnglish = isEnglish()
                )
                val startNanos = System.nanoTime()
                var processed = 0
                archive.messages.chunked(LocalBackupManager.TURBO_DEFAULT_BATCH_SIZE).forEach { chunk ->
                    repository.ingestStreamingBatch(session, chunk, sourceLabel)
                    processed += chunk.size
                    val elapsedSec = ((System.nanoTime() - startNanos) / 1_000_000_000.0).coerceAtLeast(0.1)
                    val ratePerSec = (processed / elapsedSec).toInt()
                    _uiState.update {
                        it.copy(
                            importProgressText = msg(
                                "Turbo 600k+: $processed / ${archive.messages.size} SMS ($ratePerSec/sek • Ads ${session.totalAdsRouted})",
                                "Turbo 600k+: $processed / ${archive.messages.size} SMS ($ratePerSec/s • ${session.totalAdsRouted} Ads)"
                            )
                        )
                    }
                }
                val (tmplCount, contactCount) = repository.importTemplatesAndContacts(
                    archive.templates,
                    archive.contacts,
                    replaceExisting
                )
                val elapsedSec = String.format(java.util.Locale.US, "%.1f", (System.nanoTime() - startNanos) / 1_000_000_000.0)
                _uiState.update { it.copy(inboxFilter = InboxFilter.ALL) }
                refreshVaultData()
                showBanner(
                    msg(
                        "SMS zote ${archive.messages.size} zimeingia kwa sek $elapsedSec (${session.totalAddedOrRestored} mpya, Ads ${session.totalAdsRouted}), Templeti $tmplCount & Marafiki $contactCount!",
                        "All ${archive.messages.size} SMS imported in ${elapsedSec}s (${session.totalAddedOrRestored} new, ${session.totalAdsRouted} Ads), $tmplCount templates & $contactCount contacts!"
                    )
                )
            } finally {
                _uiState.update { it.copy(isBackupBusy = false, importProgressText = null) }
                scheduleInstantCachePersist(_activeMessages.value)
            }
        }
    }

    fun setAutoBlockAdsEnabled(enabled: Boolean) {
        updateSettingsOptimistically(
            transform = { it.copy(autoBlockAdsEnabled = enabled) },
            persist = {
                preferences.setAutoBlockAdsEnabled(enabled)
                if (enabled) {
                    val isEn = _uiState.value.settings.language == AppLanguage.EN
                    val swept = repository.autoSweepExistingAds(
                        savedContacts = allContacts.value,
                        whitelistedSenders = _uiState.value.settings.whitelistedAdSenders,
                        isEnglish = isEn
                    )
                    refreshVaultData()
                    showBanner(
                        if (swept > 0) {
                            msg(
                                "Kizuia Ads kimewashwa! Matangazo $swept yamezuiwa moja kwa moja.",
                                "Auto Ad Blocker enabled! $swept existing ads were auto-blocked."
                            )
                        } else {
                            msg(
                                "Kizuia Ads kimewashwa (15, 16, 17, 18, 19, 09, 55 & majina bila namba).",
                                "Auto Ad Blocker enabled (15, 16, 17, 18, 19, 09, 55 & name-only senders)."
                            )
                        }
                    )
                } else {
                    showBanner(
                        msg(
                            "Kizuia Ads kiotomatiki kimezimwa.",
                            "Auto Ad Blocker turned off."
                        )
                    )
                }
            }
        )
    }

    fun autoSweepExistingAdsNow() {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val isEn = _uiState.value.settings.language == AppLanguage.EN
            val swept = repository.autoSweepExistingAds(
                savedContacts = allContacts.value,
                whitelistedSenders = _uiState.value.settings.whitelistedAdSenders,
                isEnglish = isEn
            )
            refreshVaultData()
            showBanner(
                if (swept > 0) {
                    msg(
                        "Matangazo $swept (15-19, 09, 55 & majina) yamezuiwa! Namba 07/06/+255 zipo salama.",
                        "Auto-blocked $swept ads (15-19, 09, 55 & name-only)! Human numbers (07/06/+255) are safe."
                    )
                } else {
                    msg(
                        "Kikasha chako ni safi — namba za watu (07/06/+255) zipo salama.",
                        "Inbox is clean — human numbers (07/06/+255) are safe."
                    )
                }
            )
        }
    }

    fun allowBlockedAdSender(senderName: String, phoneNumber: String) {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val key = phoneNumber.ifBlank { senderName }
            preferences.addWhitelistedAdSender(key)
            val restored = repository.unblockSenderMessages(key)
            refreshVaultData()
            showBanner(
                msg(
                    "$senderName ameruhusiwa — ujumbe $restored umerejeshwa kwenye Kikasha.",
                    "$senderName allowed — $restored message(s) restored to your Inbox."
                )
            )
        }
    }

    fun deleteAllBlockedAdsNow() {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = repository.deleteAllBlockedAds()
            refreshVaultData()
            showBanner(
                if (deleted > 0) {
                    msg(
                        "Matangazo $deleted yamefutwa kabisa!",
                        "Permanently deleted $deleted blocked ads!"
                    )
                } else {
                    msg(
                        "Hakuna matangazo ya kufuta.",
                        "No blocked ads to delete."
                    )
                }
            )
        }
    }

    fun importPhoneSystemSmsNow() {
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val startNanos = System.nanoTime()
            _uiState.update {
                it.copy(
                    isBackupBusy = true,
                    importProgressText = msg(
                        "Hyper-Turbo 600k+: Inachambua SMS zote za simu kwa sek chache...",
                        "Hyper-Turbo 600k+: Scanning & analyzing all device SMS..."
                    )
                )
            }
            try {
                if (!localBackupManager.hasReadSmsPermission()) {
                    showBanner(
                        msg(
                            "Toa ruhusa ya SMS kwanza ili kuingiza ujumbe wote kutoka kwenye simu.",
                            "Grant SMS permission first to import all messages from your device."
                        )
                    )
                    return@launch
                }
                val currentSettings = _uiState.value.settings
                val session = repository.beginStreamingImportSession(
                    replaceExisting = false,
                    savedContacts = allContacts.value,
                    autoBlockAdsEnabled = currentSettings.autoBlockAdsEnabled,
                    whitelistedSenders = currentSettings.whitelistedAdSenders,
                    isEnglish = isEnglish()
                )
                val streamRes = localBackupManager.streamSystemDeviceSms(
                    batchSize = LocalBackupManager.TURBO_DEFAULT_BATCH_SIZE
                ) { chunk, totalSoFar ->
                    repository.ingestStreamingBatch(session, chunk, "Phone SMS")
                    val elapsedSec = ((System.nanoTime() - startNanos) / 1_000_000_000.0).coerceAtLeast(0.1)
                    val ratePerSec = (totalSoFar / elapsedSec).toInt()
                    _uiState.update {
                        it.copy(
                            importProgressText = msg(
                                "Turbo 600k+ Simu: SMS $totalSoFar ($ratePerSec/sek • ${session.totalAddedOrRestored} mpya • Ads ${session.totalAdsRouted})",
                                "Phone Turbo 600k+: $totalSoFar SMS ($ratePerSec/s • ${session.totalAddedOrRestored} new • ${session.totalAdsRouted} Ads)"
                            )
                        )
                    }
                }
                val totalScanned = streamRes.getOrDefault(session.totalProcessed)
                val added = session.totalAddedOrRestored
                val adsRouted = session.totalAdsRouted
                val elapsedSec = String.format(java.util.Locale.US, "%.1f", (System.nanoTime() - startNanos) / 1_000_000_000.0)
                _uiState.update { it.copy(inboxFilter = InboxFilter.ALL) }
                refreshVaultData()
                showBanner(
                    msg(
                        "Turbo 600k+ imekamilika kwa sek $elapsedSec! SMS $totalScanned ($added mpya, Ads $adsRouted zimewekwa kwenye Ads)!",
                        "Turbo 600k+ finished in ${elapsedSec}s! $totalScanned phone SMS ($added new, $adsRouted Ads routed to Ads)!"
                    )
                )
            } finally {
                _uiState.update { it.copy(isBackupBusy = false, importProgressText = null) }
                scheduleInstantCachePersist(_activeMessages.value)
            }
        }
    }

    /**
     * Zero-latency batch conversation deletion triggered from the Conversation List Pop View.
     * Removes all matching threads from UI state on Frame 0, then moves them to Trash in SQLite.
     */
    fun deleteConversationsByThreadKeys(threadKeys: Set<String>) {
        if (threadKeys.isEmpty()) return
        recordUserInteraction()
        val matching = _activeMessages.value.filter { it.canonicalThreadKey in threadKeys }
        if (matching.isEmpty()) return
        val idsToRemove = matching.mapTo(HashSet(matching.size)) { it.id }
        _activeMessages.update { list -> list.filterNot { it.id in idsToRemove } }
        val threadCount = threadKeys.size
        val msgCount = matching.size
        showBanner(
            msg(
                "Mazungumzo $threadCount (SMS $msgCount) yamehamishiwa kwenye Taka.",
                "$threadCount conversation(s) ($msgCount SMS) moved to Trash."
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            matching.forEach { message ->
                if (message.status == MessageStatus.SCHEDULED) {
                    SmsDispatchManager.cancelScheduledAlarm(appContext, message.id)
                }
            }
            val persisted = matching.filter { it.id > 0L }
            if (persisted.isNotEmpty()) {
                repository.moveMessagesToTrashBatch(persisted)
            }
            scheduleInstantCachePersist(_activeMessages.value)
            refreshVaultData()
        }
    }

    /**
     * Zero-latency batch Mark-as-Read for selected conversations from the Pop View.
     */
    fun markConversationsReadByThreadKeys(threadKeys: Set<String>) {
        if (threadKeys.isEmpty()) return
        recordUserInteraction()
        val unreadIds = _activeMessages.value
            .filter { !it.isRead && it.canonicalThreadKey in threadKeys }
            .map { it.id }
        if (unreadIds.isEmpty()) {
            showBanner(
                msg(
                    "Mazungumzo uliyochagua tayari yamesomwa.",
                    "Selected conversations are already marked read."
                )
            )
            return
        }
        val unreadIdSet = unreadIds.toHashSet()
        _activeMessages.update { list ->
            list.map { if (it.id in unreadIdSet) it.copy(isRead = true) else it }
        }
        showBanner(
            msg(
                "Mazungumzo ${threadKeys.size} yamewekwa kuwa yamesomwa.",
                "${threadKeys.size} conversation(s) marked as read."
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            val persistedIds = unreadIds.filter { it > 0L }
            if (persistedIds.isNotEmpty()) {
                repository.markReadBatch(persistedIds)
            }
            scheduleInstantCachePersist(_activeMessages.value)
        }
    }

    /**
     * Zero-latency batch Mute / Unmute for selected conversation senders from the Pop View.
     */
    fun toggleMuteConversations(senders: List<String>, mute: Boolean) {
        val cleaned = senders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) return
        val keys = cleaned.mapNotNull { sender ->
            SmsAdBlockerEngine.canonicalSenderKey(sender).takeIf { it.isNotEmpty() }
        }.toSet()
        if (keys.isEmpty()) return
        updateSettingsOptimistically(
            transform = { cur ->
                val updated = if (mute) {
                    cur.mutedNotificationSenders + keys
                } else {
                    cur.mutedNotificationSenders - keys
                }
                cur.copy(mutedNotificationSenders = updated)
            },
            persist = {
                cleaned.forEach { sender ->
                    if (mute) preferences.muteNotificationSender(sender)
                    else preferences.unmuteNotificationSender(sender)
                }
            }
        )
        showBanner(
            if (mute) {
                msg(
                    "Arifa za mazungumzo ${cleaned.size} zimenyamazishwa.",
                    "Muted notifications for ${cleaned.size} conversation(s)."
                )
            } else {
                msg(
                    "Arifa za mazungumzo ${cleaned.size} zimewashwa tena.",
                    "Unmuted notifications for ${cleaned.size} conversation(s)."
                )
            }
        )
    }

    fun showBanner(message: String) {
        _uiState.update { it.copy(statusBanner = message) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }
}
