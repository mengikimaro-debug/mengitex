package com.example

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.security.LauncherIconMode
import com.example.security.SmsAdBlockerEngine
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.DefaultAppRoleHelper
import com.example.service.MengiTexNotificationManager
import com.example.service.SmsDispatchManager
import com.example.service.SmsStealthReceiver
import com.example.ui.DrawerDestination
import com.example.ui.MainTab
import com.example.ui.SiriSmsViewModel
import com.example.ui.components.InAppPopMessageOverlay
import com.example.ui.components.MengiTexLaunchAnimationOverlay
import com.example.ui.components.MengiTexLogoMark
import com.example.ui.components.NextGenAmbientBackdrop
import com.example.ui.components.NextGenIconOrb
import com.example.ui.components.PremiumBottomNavBar
import com.example.ui.components.PrivacyShieldCurtainOverlay
import com.example.ui.components.SecurityDrawerContent
import com.example.ui.components.VaultLockScreen
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.ComposeScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HiddenLogsScreen
import com.example.ui.screens.InboxAndReportsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SecurityCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.UpgradeScreen
import com.example.ui.screens.UsageDashboardScreen
import com.example.ui.theme.SiriSmsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SiriSmsViewModel by viewModels()
    private val isAppInForegroundState = mutableStateOf(true)
    private val isMainAppMountedState = mutableStateOf(true)
    private val showLaunchAnimationState = mutableStateOf(false)
    private val launchAnimationSessionState = mutableIntStateOf(1)
    private val fastBootDarkState = mutableStateOf<Boolean?>(null)
    private val fastBootLanguageState = mutableStateOf(AppLanguage.SW)
    private val fastBootLockedState = mutableStateOf(false)

    private var pendingLaunchIntent: Intent? = null
    private var suppressNextResumeAnimation = false
    private var hasStoppedSinceLaunch = false
    private var isFirstResumeAfterCreate = true

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isMainAppMountedState.value) {
                viewModel.onAppBackgroundedOrScreenOff()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Immediately dismiss Android 12+ static splash screen with 0ms fade delay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    splashScreenView.remove()
                }
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Synchronous <0.2ms boot preference cache so Frame 0 matches exact Dark/Light mode & language
        loadFastBootPrefs()

        runCatching {
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        // Defer intent handling until after Frame 0 paints so ViewModel/Room/Keystore never block launch animation
        if (hasDirectSmsLaunchTarget(intent)) {
            pendingLaunchIntent = intent
        }

        setContent {
            LaunchedEffect(Unit) {
                pendingLaunchIntent?.let { deferred ->
                    handleNotificationLaunchIntent(deferred)
                    pendingLaunchIntent = null
                }
                withFrameNanos { }
                runCatching { reportFullyDrawn() }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                MengiTexAppRoot(
                    viewModel = viewModel,
                    isAppInForeground = isAppInForegroundState.value,
                    fastBootDark = fastBootDarkState.value,
                    fastBootLanguage = fastBootLanguageState.value,
                    fastBootLocked = fastBootLockedState.value,
                    onSyncFastBootPrefs = ::persistFastBootPrefs,
                    onWillLaunchExternalSystemUi = { suppressNextResumeAnimation = true }
                )
            }
        }
    }

    private fun loadFastBootPrefs() {
        runCatching {
            val prefs = getSharedPreferences(FAST_BOOT_PREFS, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_FAST_DARK)) {
                fastBootDarkState.value = prefs.getBoolean(KEY_FAST_DARK, false)
            }
            val langCode = prefs.getString(KEY_FAST_LANG, AppLanguage.SW.code)
            fastBootLanguageState.value = if (langCode == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.SW
            fastBootLockedState.value = prefs.getBoolean(KEY_FAST_LOCKED, false)
        }
    }

    private fun persistFastBootPrefs(isDark: Boolean, language: AppLanguage, hasPasscode: Boolean) {
        fastBootDarkState.value = isDark
        fastBootLanguageState.value = language
        fastBootLockedState.value = hasPasscode
        runCatching {
            getSharedPreferences(FAST_BOOT_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FAST_DARK, isDark)
                .putString(KEY_FAST_LANG, language.code)
                .putBoolean(KEY_FAST_LOCKED, hasPasscode)
                .apply()
        }
    }

    private fun hasDirectSmsLaunchTarget(intent: Intent?): Boolean {
        val phone = intent?.getStringExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE)?.trim().orEmpty()
        if (phone.isNotEmpty()) return true
        return DefaultAppRoleHelper.parseSmsLaunchIntent(intent) != null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (hasDirectSmsLaunchTarget(intent)) {
            if (isMainAppMountedState.value) {
                handleNotificationLaunchIntent(intent)
            } else {
                pendingLaunchIntent = intent
            }
        }
    }

    private fun handleNotificationLaunchIntent(intent: Intent?) {
        val phone = intent?.getStringExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE)?.trim().orEmpty()
        if (phone.isNotEmpty()) {
            val name = intent?.getStringExtra(SmsStealthReceiver.EXTRA_OPEN_NAME)?.trim().orEmpty().ifBlank { phone }
            viewModel.openConversation(name, phone)
            intent?.removeExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE)
            intent?.removeExtra(SmsStealthReceiver.EXTRA_OPEN_NAME)
            return
        }
        val externalTarget = DefaultAppRoleHelper.parseSmsLaunchIntent(intent)
        if (externalTarget != null) {
            viewModel.openExternalSmsTarget(externalTarget.phoneNumber, externalTarget.draftBody)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (isMainAppMountedState.value) {
            viewModel.recordUserInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onRestart() {
        super.onRestart()
        // 0ms instant resume — never block returning users with a splash curtain replay
        hasStoppedSinceLaunch = false
        suppressNextResumeAnimation = false
    }

    override fun onResume() {
        super.onResume()
        isAppInForegroundState.value = true
        suppressNextResumeAnimation = false
        if (isFirstResumeAfterCreate) {
            isFirstResumeAfterCreate = false
            return
        }
        if (isMainAppMountedState.value) {
            viewModel.refreshSystemEnvironment()
            viewModel.syncNewDeviceSmsDelta(force = false)
        }
    }

    override fun onPause() {
        super.onPause()
        isAppInForegroundState.value = false
    }

    override fun onStop() {
        super.onStop()
        isAppInForegroundState.value = false
        hasStoppedSinceLaunch = true
        if (isMainAppMountedState.value) {
            viewModel.onAppBackgroundedOrScreenOff()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    companion object {
        private const val FAST_BOOT_PREFS = "mengitex_fast_boot"
        private const val KEY_FAST_DARK = "is_dark"
        private const val KEY_FAST_LANG = "lang"
        private const val KEY_FAST_LOCKED = "is_locked"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MengiTexAppRoot(
    viewModel: SiriSmsViewModel,
    isAppInForeground: Boolean,
    fastBootDark: Boolean?,
    fastBootLanguage: AppLanguage,
    fastBootLocked: Boolean,
    onSyncFastBootPrefs: (Boolean, AppLanguage, Boolean) -> Unit,
    onWillLaunchExternalSystemUi: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val groupedInboxData by viewModel.groupedInboxData.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showIconStealthDialog by remember { mutableStateOf(false) }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var pendingImportedBackupPayload by remember { mutableStateOf<String?>(null) }

    // Live In-App Pop Message tracker (pops incoming SMS card with Pop Icon + Pop Reply)
    val sessionStartedAt = remember { System.currentTimeMillis() }
    var lastPoppedMessageId by remember { mutableStateOf(-1L) }
    var activePopMessage by remember { mutableStateOf<DecryptedSmsMessage?>(null) }
    var isUserTypingPopReply by remember { mutableStateOf(false) }
    var pendingPopReplyAfterPermission by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    LaunchedEffect(activeMessages, uiState.isLocked) {
        if (uiState.isLocked || activeMessages.isEmpty()) return@LaunchedEffect
        var newestIncoming: DecryptedSmsMessage? = null
        // activeMessages is ordered newest-first: break immediately once below sessionStartedAt (~1 iteration!)
        for (i in 0 until activeMessages.size) {
            val m = activeMessages[i]
            if (m.timestamp < sessionStartedAt) break
            if (m.direction == MessageDirection.INCOMING &&
                !m.isSilentIntercepted &&
                m.status != MessageStatus.BLOCKED_AD &&
                m.id > lastPoppedMessageId
            ) {
                if (newestIncoming == null || m.timestamp > newestIncoming.timestamp) {
                    newestIncoming = m
                }
            }
        }
        val targetIncoming = newestIncoming ?: return@LaunchedEffect

        lastPoppedMessageId = targetIncoming.id
        val isTxn = SmsAdBlockerEngine.evaluate(
            senderAddress = targetIncoming.phoneNumber,
            body = targetIncoming.body,
            isSavedContact = false,
            whitelistedSenders = emptySet(),
            autoBlockAdsEnabled = true
        ).isProtectedTransaction
        if (MengiTexNotificationManager.shouldSuppressInAppPop(targetIncoming.phoneNumber, isTxn, uiState.settings)) {
            return@LaunchedEffect
        }
        val viewingSameThread = uiState.activeDestination == DrawerDestination.CONVERSATION &&
            ContactsLoader.normalizePhone(uiState.activeConversationPhone) ==
            ContactsLoader.normalizePhone(targetIncoming.phoneNumber)
        if (!viewingSameThread && !(isUserTypingPopReply && activePopMessage != null)) {
            activePopMessage = targetIncoming
        }
    }

    val effectiveLanguage = if (uiState.hasLoadedSettings) uiState.settings.language else fastBootLanguage
    val appStrings = remember(effectiveLanguage) { stringsFor(effectiveLanguage) }
    val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()
    val resolvedDarkTheme = if (uiState.hasLoadedSettings) {
        uiState.settings.resolveIsDarkTheme(systemIsDark)
    } else {
        fastBootDark ?: false
    }
    val effectiveLocked = if (uiState.hasLoadedSettings) uiState.isLocked else fastBootLocked

    LaunchedEffect(
        uiState.hasLoadedSettings,
        resolvedDarkTheme,
        effectiveLanguage,
        uiState.settings.hasPasscode
    ) {
        if (uiState.hasLoadedSettings) {
            onSyncFastBootPrefs(resolvedDarkTheme, effectiveLanguage, uiState.settings.hasPasscode)
        }
    }

    // 1. All core runtime permissions launcher (SMS, Contacts, Mic, SIM, Notifications)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemEnvironment()
        val queuedPop = pendingPopReplyAfterPermission
        if (queuedPop != null) {
            pendingPopReplyAfterPermission = null
            if (SmsDispatchManager.hasSendSmsPermission(context)) {
                viewModel.sendPopReplyFromBanner(
                    contactName = queuedPop.first,
                    phoneNumber = queuedPop.second,
                    plainBody = queuedPop.third
                )
            } else {
                viewModel.showBanner(
                    if (uiState.settings.language == AppLanguage.EN) {
                        "SMS permission is required to send Pop Reply directly."
                    } else {
                        "Ruhusa ya SMS inahitajika kutuma Pop Reply moja kwa moja."
                    }
                )
            }
        }
    }

    // 2. Dedicated Contacts permission launcher
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemEnvironment()
        viewModel.showBanner(
            if (uiState.settings.language == AppLanguage.EN) {
                if (granted) "Contacts permission granted." else "Contacts permission was denied."
            } else {
                if (granted) "Ruhusa ya Mawasiliano (Contacts) imewashwa." else "Ruhusa ya Mawasiliano imekataliwa."
            }
        )
    }

    // 3. Dedicated Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemEnvironment()
        viewModel.showBanner(
            if (uiState.settings.language == AppLanguage.EN) {
                if (granted) "Microphone permission granted. Ready for voice dictation."
                else "Microphone permission was denied."
            } else {
                if (granted) "Ruhusa ya Maikrofoni (Mic) imewashwa. Unaweza kuongea ujumbe."
                else "Ruhusa ya Maikrofoni (Mic) imekataliwa."
            }
        )
    }

    // 4. Dedicated Storage Folder permission launcher (Google Play Policy-compliant Android SAF)
    val storageFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.onStorageFolderAuthorized(treeUri)
        }
    }

    // 5. Storage Access Framework single file import to AES-256 Hidden Vault
    val importStorageFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileFromStorageToVault(uri)
        }
    }

    // 6. Zero-permission Android Photo Picker import to AES-256 Hidden Vault
    val importPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileFromStorageToVault(uri)
        }
    }

    val defaultSmsRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemEnvironment()
        if (DefaultAppRoleHelper.isDefaultSmsApp(context)) {
            viewModel.showBanner(
                if (uiState.settings.language == AppLanguage.EN) {
                    "MengiTex is now your Default SMS App with full Android system control."
                } else {
                    "MengiTex sasa ni App Kuu ya SMS yenye umiliki kamili wa simu!"
                }
            )
            if (uiState.hasSmsPermission) {
                viewModel.importPhoneSystemSmsNow()
            }
        }
    }

    val callScreeningRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemEnvironment()
    }

    // 7. SAF CreateDocument launcher for exporting encrypted .mengitex backup files
    val exportBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToSafUri(uri, pendingExportPassphrase)
        }
        pendingExportPassphrase = null
    }

    // 8. SAF OpenDocument launcher for importing & restoring XML / XLM, .mengitex, JSON, CSV, and TXT files (100k+ streaming)
    val importBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.readSafBackupFile(uri) { payload ->
                pendingImportedBackupPayload = payload
            }
        }
    }

    // 9. SAF CreateDocument launcher for exporting standard Android XML / XLM SMS files
    val exportXmlFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/xml")
    ) { uri ->
        if (uri != null) {
            viewModel.exportXmlBackupToSafUri(uri)
        }
    }

    val requestAllPermissions: () -> Unit = {
        onWillLaunchExternalSystemUi()
        val perms = buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    // Automatic by Android System: auto-request missing runtime permissions + Default SMS role once per install (after Frame 1)
    var autoSystemCheckedThisSession by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.hasLoadedSettings, uiState.isLocked) {
        if (!uiState.hasLoadedSettings || uiState.isLocked || autoSystemCheckedThisSession) return@LaunchedEffect
        autoSystemCheckedThisSession = true
        withFrameNanos { }
        val sysPrefs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.getSharedPreferences("mengitex_auto_system_prefs", Context.MODE_PRIVATE)
        }
        val missingPerms = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.SEND_SMS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECEIVE_SMS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_SMS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missingPerms.isNotEmpty() && !sysPrefs.getBoolean("auto_runtime_perms_requested", false)) {
            sysPrefs.edit().putBoolean("auto_runtime_perms_requested", true).apply()
            onWillLaunchExternalSystemUi()
            permissionsLauncher.launch(missingPerms.toTypedArray())
        } else {
            val isDefaultAlready = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DefaultAppRoleHelper.isDefaultSmsApp(context)
            }
            if (!isDefaultAlready && !sysPrefs.getBoolean("auto_default_sms_requested", false)) {
                sysPrefs.edit().putBoolean("auto_default_sms_requested", true).apply()
                val roleIntent = DefaultAppRoleHelper.createDefaultSmsRoleIntent(context)
                if (roleIntent != null) {
                    onWillLaunchExternalSystemUi()
                    runCatching { defaultSmsRoleLauncher.launch(roleIntent) }
                }
            }
        }
    }

    val requestContactsPermission: () -> Unit = {
        onWillLaunchExternalSystemUi()
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    val requestMicPermission: () -> Unit = {
        onWillLaunchExternalSystemUi()
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val requestStoragePermission: () -> Unit = {
        onWillLaunchExternalSystemUi()
        runCatching { storageFolderLauncher.launch(null) }
    }

    val requestSmsPermissions: () -> Unit = {
        onWillLaunchExternalSystemUi()
        val perms = buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    val requestDefaultSmsRole: () -> Unit = {
        onWillLaunchExternalSystemUi()
        val roleIntent = DefaultAppRoleHelper.createDefaultSmsRoleIntent(context)
        if (roleIntent != null) {
            runCatching { defaultSmsRoleLauncher.launch(roleIntent) }
                .onFailure {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }
                }
        } else {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
    }

    val requestCallScreeningRole: () -> Unit = {
        onWillLaunchExternalSystemUi()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                callScreeningRoleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            } else {
                viewModel.showBanner(
                    if (uiState.settings.language == AppLanguage.EN) "Call Screening is unavailable on this device."
                    else "Huduma ya Call Screening haipatikani kwenye simu hii."
                )
            }
        } else {
            viewModel.showBanner(
                if (uiState.settings.language == AppLanguage.EN) "Silent call blocking requires Android 10 or higher."
                else "Kuzuia simu kimya kunahitaji Android 10 au zaidi."
            )
        }
    }

    // Opt-in Strict Anti-Screenshot / Anti-Recording Flag (manual opt-in so live screen streaming works cleanly)
    LaunchedEffect(uiState.settings.blockScreenshotsAndRecents) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        if (uiState.settings.blockScreenshotsAndRecents) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Android 13+ Recents thumbnail shield: conceals Recent Apps preview WITHOUT blacking out the live display
    LaunchedEffect(uiState.settings.privacyCurtainOnSwitch) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? Activity)?.setRecentsScreenshotEnabled(!uiState.settings.privacyCurtainOnSwitch)
        }
    }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissBanner()
    }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
        SiriSmsTheme(
            darkTheme = resolvedDarkTheme,
            dimLightEnabled = uiState.settings.dimLightEnabled,
            dimLightLevel = uiState.settings.dimLightLevel
        ) {
            NextGenAmbientBackdrop {
                AnimatedContent(
                    targetState = effectiveLocked,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "vault_lock_transition"
                ) { locked ->
                    if (locked) {
                        VaultLockScreen(
                            errorMessage = uiState.lockError,
                            failedAttempts = uiState.settings.failedUnlockAttempts,
                            lockoutUntilMillis = uiState.settings.lockoutUntilMillis,
                            wipeOnIntruderEnabled = uiState.settings.wipeOnIntruderEnabled,
                            onUnlock = viewModel::unlockWithPasscode
                        )
                    } else {
                    BackHandler(enabled = drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
                        drawerContent = {
                            val shouldRenderDrawer = drawerState.isOpen ||
                                drawerState.isAnimationRunning ||
                                drawerState.targetValue != DrawerValue.Closed
                            if (shouldRenderDrawer) {
                                val scheduledMessages by viewModel.scheduledMessages.collectAsStateWithLifecycle()
                                val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
                                SecurityDrawerContent(
                                    settings = uiState.settings,
                                    detectedSims = uiState.detectedSims,
                                    scheduledCount = scheduledMessages.size,
                                    trashCount = trashCount,
                                    hiddenFilesCount = uiState.vaultStats.totalEncryptedFiles,
                                    totalMessagesCount = activeMessages.size,
                                    isDefaultSmsApp = uiState.isDefaultSmsApp,
                                    hasUpgradeAvailable = uiState.hasUpgradeAvailable,
                                    latestUpgradeTag = uiState.latestUpgradeTag,
                                    onSecurityLevelChange = viewModel::setSecurityLevel,
                                    onNavigateDestination = { dest ->
                                        scope.launch { drawerState.close() }
                                        viewModel.openDestination(dest)
                                    },
                                    onLockNow = {
                                        scope.launch { drawerState.close() }
                                        if (uiState.settings.hasPasscode) {
                                            viewModel.lockAppNow()
                                        } else {
                                            viewModel.openDestination(DrawerDestination.SECURITY)
                                        }
                                    }
                                )
                            } else {
                                androidx.compose.material3.ModalDrawerSheet(
                                    modifier = Modifier.width(332.dp)
                                ) {}
                            }
                        }
                    ) {
                        val s = LocalAppStrings.current

                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            topBar = {
                                AnimatedVisibility(
                                    visible = uiState.activeDestination == DrawerDestination.NONE,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                IconButton(
                                                    onClick = { scope.launch { drawerState.open() } },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("top_bar_open_drawer_button")
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Menu,
                                                            contentDescription = s.cdOpenDrawer,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                AnimatedContent(
                                                    targetState = uiState.selectedTab,
                                                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                                                    label = "top_bar_title_anim"
                                                ) { tab ->
                                                    if (tab == MainTab.MESSAGES) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.testTag("top_bar_brand_logo")
                                                        ) {
                                                            MengiTexLogoMark(
                                                                size = 36.dp,
                                                                cornerRadius = 12.dp,
                                                                showGlow = false
                                                            )
                                                            Text(
                                                                text = "MengiTex",
                                                                style = MaterialTheme.typography.titleLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = s.contactsTitle,
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            Surface(
                                                onClick = { viewModel.openDestination(DrawerDestination.SECURITY) },
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                tonalElevation = 0.dp,
                                                modifier = Modifier.testTag("top_bar_security_pill")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Shield,
                                                        contentDescription = s.drawerSecurityCenter,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = "L${uiState.settings.securityLevel}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                // Slim 2-tab Premium Animated Bottom Bar ONLY: Messages & Contacts
                                AnimatedVisibility(
                                    visible = uiState.activeDestination == DrawerDestination.NONE,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None
                                ) {
                                    PremiumBottomNavBar(
                                        selectedTab = uiState.selectedTab,
                                        messagesLabel = s.tabMessages,
                                        contactsLabel = s.tabContacts,
                                        onSelectTab = viewModel::selectTab
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .consumeWindowInsets(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = uiState.activeDestination,
                                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                                    label = "destination_transition"
                                ) { dest ->
                                    when (dest) {
                                        DrawerDestination.CONVERSATION -> {
                                            val messagesByThreadKey by viewModel.messagesByThreadKey.collectAsStateWithLifecycle()
                                            val activeTemplates by viewModel.activeTemplates.collectAsStateWithLifecycle()
                                            val messageReactions by viewModel.messageReactions.collectAsStateWithLifecycle()
                                            val activeThreadKey = remember(uiState.activeConversationPhone, uiState.activeConversationName) {
                                                ContactsLoader.resolveCanonicalThreadKey(
                                                    uiState.activeConversationPhone,
                                                    uiState.activeConversationName
                                                )
                                            }
                                            ConversationScreen(
                                                contactName = uiState.activeConversationName,
                                                phoneNumber = uiState.activeConversationPhone,
                                                allActiveMessages = activeMessages,
                                                preIndexedThreadMessages = messagesByThreadKey[activeThreadKey],
                                                messageReactions = messageReactions,
                                                contacts = allContacts,
                                                templates = activeTemplates,
                                                detectedSims = uiState.detectedSims,
                                                settings = uiState.settings,
                                                hasSmsPermission = uiState.hasSmsPermission,
                                                hasMicPermission = uiState.hasMicPermission,
                                                activeSecretReader = uiState.activeSecretReaderMessage,
                                                onSelectSim = viewModel::selectSim,
                                                onToggleMessageReaction = viewModel::toggleMessageReaction,
                                                onSetMessageReaction = viewModel::setMessageReaction,
                                                onRemoveMessageReaction = viewModel::removeMessageReaction,
                                                onSendDirectMessage = { body, isSecret, fallback ->
                                                    viewModel.sendDirectConversationMessage(
                                                        contactName = uiState.activeConversationName,
                                                        phoneNumber = uiState.activeConversationPhone,
                                                        plainBody = body,
                                                        isSecretBurn = isSecret,
                                                        useSystemFallbackIfDenied = fallback
                                                    )
                                                },
                                                onForwardMessage = viewModel::forwardMessageToRecipients,
                                                onSaveContactName = viewModel::saveContactNameForConversation,
                                                onScheduleDraft = { draft ->
                                                    viewModel.openScheduleForConversation(
                                                        contactName = uiState.activeConversationName,
                                                        phoneNumber = uiState.activeConversationPhone,
                                                        draftBody = draft
                                                    )
                                                },
                                                onOpenSecretReader = viewModel::openSecretReader,
                                                onCloseSecretReader = viewModel::closeSecretReader,
                                                onMoveToTrash = viewModel::moveMessageToTrash,
                                                onMoveMessagesToTrash = viewModel::moveMessagesToTrash,
                                                onToggleDeliveryReport = viewModel::setDeliveryReportsEnabled,
                                                onChatSizeScaleChange = viewModel::setChatSizeScale,
                                                onSendAttachment = { uri, mime, label, caption ->
                                                    viewModel.sendAttachmentInConversation(
                                                        contactName = uiState.activeConversationName,
                                                        phoneNumber = uiState.activeConversationPhone,
                                                        uri = uri,
                                                        mimeType = mime,
                                                        displayLabel = label,
                                                        captionBody = caption
                                                    )
                                                },
                                                onRequestMicPermission = requestMicPermission,
                                                onShowMessage = viewModel::showBanner,
                                                onCopiedSensitiveText = viewModel::scheduleClipboardAutoClearIfEnabled,
                                                onBack = viewModel::closeDestination
                                            )
                                        }

                                        DrawerDestination.COMPOSE -> {
                                            val activeTemplates by viewModel.activeTemplates.collectAsStateWithLifecycle()
                                            ComposeScreen(
                                                contacts = allContacts,
                                                selectedRecipients = uiState.selectedRecipients,
                                                templates = activeTemplates,
                                                detectedSims = uiState.detectedSims,
                                                selectedSimSubId = uiState.settings.selectedSimSubId,
                                                bodyText = uiState.composeBody,
                                                partsCount = uiState.composePartsCount,
                                                isSecretBurn = uiState.composeIsSecretBurn,
                                                hasSmsPermission = uiState.hasSmsPermission,
                                                hasContactsPermission = uiState.hasContactsPermission,
                                                hasMicPermission = uiState.hasMicPermission,
                                                onBack = viewModel::closeDestination,
                                                onRequestPermissions = requestAllPermissions,
                                                onRequestMicPermission = requestMicPermission,
                                                onAppendVoiceText = viewModel::appendVoiceTranscriptToCompose,
                                                onShowMessage = viewModel::showBanner,
                                                onToggleRecipient = viewModel::toggleRecipient,
                                                onSelectAll = { viewModel.selectAllContacts(allContacts) },
                                                onClearAll = viewModel::clearSelectedRecipients,
                                                onAddManualRecipient = viewModel::addManualRecipient,
                                                onSelectSim = viewModel::selectSim,
                                                onBodyChange = viewModel::updateComposeBody,
                                                onSecretBurnChange = viewModel::setComposeSecretBurn,
                                                onUseTemplate = viewModel::applyTemplateToCompose,
                                                onSendNow = viewModel::sendBroadcastNow,
                                                onSchedule = viewModel::scheduleBroadcast
                                            )
                                        }

                                        DrawerDestination.SCHEDULE -> {
                                            val scheduledMessages by viewModel.scheduledMessages.collectAsStateWithLifecycle()
                                            ScheduleScreen(
                                                scheduledMessages = scheduledMessages,
                                                onBack = viewModel::closeDestination,
                                                onSendNow = viewModel::sendScheduledNow,
                                                onMoveToTrash = viewModel::moveMessageToTrash,
                                                onCreateNewSchedule = { viewModel.openDestination(DrawerDestination.COMPOSE) }
                                            )
                                        }

                                        DrawerDestination.TEMPLATES -> {
                                            val activeTemplates by viewModel.activeTemplates.collectAsStateWithLifecycle()
                                            TemplatesScreen(
                                                templates = activeTemplates,
                                                onBack = viewModel::closeDestination,
                                                onUseTemplate = viewModel::applyTemplateToCompose,
                                                onSaveTemplate = viewModel::saveTemplate,
                                                onMoveToTrash = viewModel::moveTemplateToTrash
                                            )
                                        }

                                        DrawerDestination.SECURITY -> {
                                            val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
                                            SecurityCenterScreen(
                                            settings = uiState.settings,
                                            detectedSims = uiState.detectedSims,
                                            simStateSummary = uiState.simStateSummary,
                                            vaultStats = uiState.vaultStats,
                                            isCallScreeningRoleHeld = uiState.isCallScreeningRoleHeld,
                                            trashCount = trashCount,
                                            onBack = viewModel::closeDestination,
                                            onSecurityLevelChange = viewModel::setSecurityLevel,
                                            onBlockScreenshotsChange = viewModel::setBlockScreenshotsAndRecents,
                                            onPrivacyCurtainChange = viewModel::setPrivacyCurtainOnSwitch,
                                            onClipboardAutoClearChange = viewModel::setClipboardAutoClear,
                                            onWipeOnIntruderChange = viewModel::setWipeOnIntruder,
                                            onSilentUnknownSmsChange = viewModel::setSilentUnknownSms,
                                            onAutoBlockAdsChange = viewModel::setAutoBlockAdsEnabled,
                                            onAutoSweepAdsNow = viewModel::autoSweepExistingAdsNow,
                                            onBlockUnknownCallsChange = viewModel::setBlockUnknownCalls,
                                            onRequestCallScreeningRole = requestCallScreeningRole,
                                            onAutoDeleteSecretChange = viewModel::setAutoDeleteSecret,
                                            onAutoLockIdleSecondsChange = viewModel::setAutoLockIdleSeconds,
                                            onLockOnScreenOffChange = viewModel::setLockOnScreenOff,
                                            onSetPasscode = viewModel::setOrChangePasscode,
                                            onRemovePasscode = viewModel::removePasscode,
                                            onRefreshSims = viewModel::refreshSystemEnvironment,
                                            onOpenIconStealthDialog = { showIconStealthDialog = true },
                                            onOpenHiddenLogs = { viewModel.openDestination(DrawerDestination.HIDDEN_LOGS) },
                                            onOpenTrash = { viewModel.openDestination(DrawerDestination.TRASH) }
                                        )
                                        }

                                        DrawerDestination.USAGE_DASHBOARD -> {
                                            val scheduledMessages by viewModel.scheduledMessages.collectAsStateWithLifecycle()
                                            val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
                                            UsageDashboardScreen(
                                                messages = activeMessages,
                                                scheduledMessages = scheduledMessages,
                                                trashedCount = trashCount,
                                                vaultStats = uiState.vaultStats,
                                                onOpenConversation = { name, phone ->
                                                    viewModel.openConversation(name, phone)
                                                },
                                                onQuickCompose = { viewModel.openDestination(DrawerDestination.COMPOSE) },
                                                onBack = viewModel::closeDestination
                                            )
                                        }

                                        DrawerDestination.TRASH -> {
                                            val trashedMessages by viewModel.trashedMessages.collectAsStateWithLifecycle()
                                            val trashedTemplates by viewModel.trashedTemplates.collectAsStateWithLifecycle()
                                            TrashScreen(
                                                trashedMessages = trashedMessages,
                                                trashedTemplates = trashedTemplates,
                                                onBack = viewModel::closeDestination,
                                                onRestoreMessage = viewModel::restoreMessage,
                                                onDeleteMessageForever = viewModel::permanentlyDeleteMessage,
                                                onRestoreTemplate = viewModel::restoreTemplate,
                                                onDeleteTemplateForever = viewModel::permanentlyDeleteTemplate,
                                                onEmptyAllTrash = viewModel::emptyTrash
                                            )
                                        }

                                        DrawerDestination.HIDDEN_LOGS -> HiddenLogsScreen(
                                            logs = uiState.vaultLogs,
                                            vaultFiles = uiState.vaultFiles,
                                            stats = uiState.vaultStats,
                                            onBack = viewModel::closeDestination,
                                            onSaveEncryptedNote = viewModel::saveEncryptedVaultNote,
                                            onImportStorageFile = {
                                                onWillLaunchExternalSystemUi()
                                                runCatching { importStorageFileLauncher.launch(arrayOf("*/*")) }
                                            },
                                            onImportPhotoMedia = {
                                                onWillLaunchExternalSystemUi()
                                                runCatching {
                                                    importPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                }
                                            },
                                            onReadEncryptedFile = viewModel::readEncryptedVaultNote,
                                            onDeleteEncryptedFile = viewModel::deleteEncryptedVaultFile,
                                            onClearLogs = viewModel::clearVaultLogs
                                        )

                                        DrawerDestination.BACKUP_RESTORE -> {
                                            val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
                                            BackupRestoreScreen(
                                                localSnapshots = uiState.localSnapshots,
                                                trashCount = trashCount,
                                                hasSmsPermission = uiState.hasSmsPermission,
                                                isBusy = uiState.isBackupBusy,
                                                importProgressText = uiState.importProgressText,
                                                pendingImportedPayload = pendingImportedBackupPayload,
                                                onClearPendingImportedPayload = { pendingImportedBackupPayload = null },
                                                observeCloudBackups = viewModel::observeCloudBackups,
                                                onUploadCloudBackup = viewModel::uploadCloudBackupNow,
                                                onRenameCloudBackup = viewModel::renameCloudBackup,
                                                onDeleteCloudBackup = viewModel::deleteCloudBackup,
                                                onCreateLocalSnapshot = viewModel::createLocalSnapshotNow,
                                                onReadLocalSnapshotPayload = viewModel::readLocalSnapshotPayload,
                                                onDeleteLocalSnapshot = viewModel::deleteLocalSnapshot,
                                                onExportBackupFileRequested = { pass ->
                                                    onWillLaunchExternalSystemUi()
                                                    pendingExportPassphrase = pass
                                                    val defaultName = "MengiTex_Backup_${System.currentTimeMillis()}.mengitex"
                                                    runCatching { exportBackupFileLauncher.launch(defaultName) }
                                                },
                                                onImportBackupFileRequested = {
                                                    onWillLaunchExternalSystemUi()
                                                    runCatching { importBackupFileLauncher.launch(arrayOf("*/*")) }
                                                },
                                                onImportXmlFileRequested = {
                                                    onWillLaunchExternalSystemUi()
                                                    runCatching { importBackupFileLauncher.launch(arrayOf("*/*")) }
                                                },
                                                onExportXmlFileRequested = {
                                                    onWillLaunchExternalSystemUi()
                                                    val defaultXmlName = "MengiTex_SMS_${System.currentTimeMillis()}.xml"
                                                    runCatching { exportXmlFileLauncher.launch(defaultXmlName) }
                                                },
                                                onShareBackupRequested = { pass ->
                                                    onWillLaunchExternalSystemUi()
                                                    viewModel.prepareShareableBackup(pass) { shareUri ->
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "application/octet-stream"
                                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                                            putExtra(Intent.EXTRA_SUBJECT, "MengiTex AES-256 Encrypted Backup")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        runCatching {
                                                            context.startActivity(
                                                                Intent.createChooser(shareIntent, "Share MengiTex Backup")
                                                            )
                                                        }
                                                    }
                                                },
                                                onRestoreEncryptedPayload = viewModel::restoreFromEncryptedPayload,
                                                onImportPhoneSystemSms = viewModel::importPhoneSystemSmsNow,
                                                onRequestSmsPermission = requestSmsPermissions,
                                                onOpenTrash = { viewModel.openDestination(DrawerDestination.TRASH) },
                                                onShowMessage = viewModel::showBanner,
                                                onBack = viewModel::closeDestination
                                            )
                                        }

                                        DrawerDestination.SETTINGS -> SettingsScreen(
                                            settings = uiState.settings,
                                            detectedSims = uiState.detectedSims,
                                            onBack = viewModel::closeDestination,
                                            onLanguageChange = viewModel::setLanguage,
                                            onAutoBlockAdsChange = viewModel::setAutoBlockAdsEnabled,
                                            onAutoSweepAdsNow = viewModel::autoSweepExistingAdsNow,
                                            onThemeModeChange = viewModel::setThemeMode,
                                            onDimLightChange = viewModel::setDimLightEnabled,
                                            onDimLightLevelChange = viewModel::setDimLightLevel,
                                            onChatSizeScaleChange = viewModel::setChatSizeScale,
                                            onOpenIconStealthDialog = { showIconStealthDialog = true },
                                            onSelectSim = viewModel::selectSim,
                                            onRefreshSims = viewModel::refreshSystemEnvironment,
                                            onOpenSecurityCenter = { viewModel.openDestination(DrawerDestination.SECURITY) },
                                            onOpenBackupRestore = { viewModel.openDestination(DrawerDestination.BACKUP_RESTORE) },
                                            onOpenUpgradeCenter = { viewModel.openDestination(DrawerDestination.UPGRADE) },
                                            onGithubRepoChange = viewModel::setGithubUpdateRepo,
                                            onGithubChannelChange = viewModel::setGithubUpdateChannel,
                                            onGithubAutoCheckChange = viewModel::setGithubAutoCheckUpdates,
                                            onGithubCacheRelease = viewModel::cacheGithubLatestRelease
                                        )

                                        DrawerDestination.UPGRADE -> UpgradeScreen(
                                            settings = uiState.settings,
                                            onBack = viewModel::closeDestination,
                                            onUpdateRepoChange = viewModel::setGithubUpdateRepo,
                                            onUpdateChannelChange = viewModel::setGithubUpdateChannel,
                                            onAutoCheckChange = viewModel::setGithubAutoCheckUpdates,
                                            onCacheLatestRelease = viewModel::cacheGithubLatestRelease
                                        )

                                        DrawerDestination.NONE -> {
                                            AnimatedContent(
                                                targetState = uiState.selectedTab,
                                                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                                                label = "main_tab_transition"
                                            ) { tab ->
                                                when (tab) {
                                                    MainTab.MESSAGES -> InboxAndReportsScreen(
                                                        messages = activeMessages,
                                                        contacts = allContacts,
                                                        precomputedGroupedInbox = groupedInboxData,
                                                        filter = uiState.inboxFilter,
                                                        settings = uiState.settings,
                                                        isDefaultSmsApp = uiState.isDefaultSmsApp,
                                                        isMessagesLoading = !uiState.isInitialMessagesLoaded,
                                                        activeSecretReader = uiState.activeSecretReaderMessage,
                                                        onSelectFilter = viewModel::setInboxFilter,
                                                        onOpenConversation = viewModel::openConversation,
                                                        onOpenSecretReader = viewModel::openSecretReader,
                                                        onCloseSecretReader = viewModel::closeSecretReader,
                                                        onMoveToTrash = viewModel::moveMessageToTrash,
                                                        onRequestDefaultSmsApp = requestDefaultSmsRole,
                                                        onSaveContactName = viewModel::saveContactNameForConversation,
                                                        onAutoSweepAds = viewModel::autoSweepExistingAdsNow,
                                                        onAllowBlockedAdSender = viewModel::allowBlockedAdSender,
                                                        onDeleteAllBlockedAds = viewModel::deleteAllBlockedAdsNow,
                                                        onDeleteConversations = viewModel::deleteConversationsByThreadKeys,
                                                        onMarkConversationsRead = viewModel::markConversationsReadByThreadKeys,
                                                        onToggleMuteConversations = viewModel::toggleMuteConversations,
                                                        onShowMessage = viewModel::showBanner,
                                                        onQuickCompose = { viewModel.openDestination(DrawerDestination.COMPOSE) }
                                                    )

                                                    MainTab.CONTACTS -> ContactsScreen(
                                                        contacts = allContacts,
                                                        selectedRecipients = uiState.selectedRecipients,
                                                        allMessages = activeMessages,
                                                        hasContactsPermission = uiState.hasContactsPermission,
                                                        onRequestPermissions = requestContactsPermission,
                                                        onToggleRecipient = viewModel::toggleRecipient,
                                                        onSelectAll = { viewModel.selectAllContacts(allContacts) },
                                                        onClearAll = viewModel::clearSelectedRecipients,
                                                        onAddManualFriend = viewModel::addManualRecipient,
                                                        onQuickMessageSingle = viewModel::openComposeForSingleContact,
                                                        onOpenConversation = viewModel::openConversation,
                                                        onSaveContactName = viewModel::saveContactNameForConversation,
                                                        onShowMessage = viewModel::showBanner,
                                                        onSendToSelected = { viewModel.openDestination(DrawerDestination.COMPOSE) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showIconStealthDialog) {
                        IconStealthDialog(
                            currentMode = uiState.settings.launcherIconMode,
                            onSelectMode = { mode ->
                                viewModel.setLauncherIconMode(mode)
                                showIconStealthDialog = false
                            },
                            onDismiss = { showIconStealthDialog = false }
                        )
                    }
                }
            }

                // Live In-App Pop Message Banner with Pop Icon + Inline Pop Reply
                InAppPopMessageOverlay(
                    message = if (!effectiveLocked && isAppInForeground) activePopMessage else null,
                    hidePreviewForPrivacy = uiState.settings.securityLevel >= 4,
                    autoDismissSeconds = uiState.settings.inAppPopDurationSeconds,
                    onOpenConversation = { name, phone ->
                        isUserTypingPopReply = false
                        viewModel.openConversation(name, phone)
                    },
                    onSendPopReply = { name, phone, replyBody ->
                        isUserTypingPopReply = false
                        if (SmsDispatchManager.hasSendSmsPermission(context)) {
                            viewModel.sendPopReplyFromBanner(
                                contactName = name,
                                phoneNumber = phone,
                                plainBody = replyBody
                            )
                        } else {
                            pendingPopReplyAfterPermission = Triple(name, phone, replyBody)
                            requestSmsPermissions()
                        }
                    },
                    onDismiss = {
                        isUserTypingPopReply = false
                        activePopMessage = null
                    },
                    onReplyingStateChange = { typing -> isUserTypingPopReply = typing },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Instant App-Switch Privacy Shield Curtain (triggers only when activity actually pauses/backgrounds)
                AnimatedVisibility(
                    visible = !isAppInForeground && uiState.settings.privacyCurtainOnSwitch && !effectiveLocked,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    PrivacyShieldCurtainOverlay()
                }
            }
        }
    }
}

@Composable
private fun IconStealthDialog(
    currentMode: LauncherIconMode,
    onSelectMode: (LauncherIconMode) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(s.iconDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    s.iconDialogDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconModeOptionRow(
                    selected = currentMode == LauncherIconMode.VISIBLE,
                    title = s.iconOptVisibleTitle,
                    subtitle = s.iconOptVisibleSub,
                    icon = Icons.Filled.Shield,
                    onClick = { onSelectMode(LauncherIconMode.VISIBLE) }
                )

                IconModeOptionRow(
                    selected = currentMode == LauncherIconMode.DISGUISE_CALC,
                    title = s.iconOptCalcTitle,
                    subtitle = s.iconOptCalcSub,
                    icon = Icons.Filled.Calculate,
                    onClick = { onSelectMode(LauncherIconMode.DISGUISE_CALC) }
                )

                IconModeOptionRow(
                    selected = currentMode == LauncherIconMode.HIDDEN,
                    title = s.iconOptHiddenTitle,
                    subtitle = s.iconOptHiddenSub,
                    icon = Icons.Filled.VisibilityOff,
                    onClick = { onSelectMode(LauncherIconMode.HIDDEN) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.btnClose) }
        }
    )
}

@Composable
private fun IconModeOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
