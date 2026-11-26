package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.cloud.CloudBackupRecord
import com.example.cloud.GoogleAuthHelper
import com.example.security.LocalBackupSnapshot
import com.example.security.PortableBackupCrypto
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.premiumBounceClickable
import com.example.ui.i18n.LocalAppStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface CloudBackupsUiState {
    data object Loading : CloudBackupsUiState
    data class Success(val items: List<CloudBackupRecord>) : CloudBackupsUiState
    data class Error(val message: String) : CloudBackupsUiState
}

private enum class BackupCreationTarget {
    CLOUD,
    LOCAL_SNAPSHOT,
    EXPORT_FILE,
    SHARE_FILE
}

private data class PendingRestoreRequest(
    val sourceLabel: String,
    val encryptedPayload: String
)

@Composable
fun BackupRestoreScreen(
    localSnapshots: List<LocalBackupSnapshot>,
    trashCount: Int,
    hasSmsPermission: Boolean,
    isBusy: Boolean,
    importProgressText: String? = null,
    pendingImportedPayload: String?,
    onClearPendingImportedPayload: () -> Unit,
    observeCloudBackups: (String) -> Flow<List<CloudBackupRecord>>,
    onUploadCloudBackup: (label: String, passphrase: String?) -> Unit,
    onRenameCloudBackup: (backupId: String, newLabel: String) -> Unit,
    onDeleteCloudBackup: (backupId: String) -> Unit,
    onCreateLocalSnapshot: (label: String, passphrase: String?) -> Unit,
    onReadLocalSnapshotPayload: (fileName: String, onReady: (String?) -> Unit) -> Unit,
    onDeleteLocalSnapshot: (fileName: String) -> Unit,
    onExportBackupFileRequested: (passphrase: String?) -> Unit,
    onImportBackupFileRequested: () -> Unit,
    onImportXmlFileRequested: () -> Unit = onImportBackupFileRequested,
    onExportXmlFileRequested: () -> Unit = {},
    onShareBackupRequested: (passphrase: String?) -> Unit,
    onRestoreEncryptedPayload: (payload: String, passphrase: String?, replaceExisting: Boolean, sourceLabel: String) -> Unit,
    onImportPhoneSystemSms: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onOpenTrash: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = s.btnCancel == "Ghairi"

    var selectedTab by remember { mutableIntStateOf(1) } // Default to Device & XML/All Ways for instant 1-tap access
    val firebaseAuth = remember(context) {
        if (com.example.cloud.CloudBackupRepository.ensureFirebaseInitialized(context)) {
            runCatching { Firebase.auth }.getOrNull()
        } else {
            null
        }
    }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(firebaseAuth?.currentUser) }
    var isAuthLoading by remember { mutableStateOf(false) }

    var activeCreationTarget by remember { mutableStateOf<BackupCreationTarget?>(null) }
    var pendingRestore by remember { mutableStateOf<PendingRestoreRequest?>(null) }
    var renamingCloudItem by remember { mutableStateOf<CloudBackupRecord?>(null) }

    DisposableEffect(firebaseAuth) {
        val authInstance = firebaseAuth ?: return@DisposableEffect onDispose {}
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        authInstance.addAuthStateListener(listener)
        onDispose { authInstance.removeAuthStateListener(listener) }
    }

    // Attempt silent auto sign-in once when user opens Backup & Restore
    LaunchedEffect(firebaseAuth) {
        if (firebaseAuth != null && firebaseAuth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(context)
        }
    }

    // When MainActivity finishes reading a password-protected .mengitex SAF file, open Restore dialog
    LaunchedEffect(pendingImportedPayload) {
        val payload = pendingImportedPayload
        if (!payload.isNullOrBlank()) {
            pendingRestore = PendingRestoreRequest(
                sourceLabel = "File (.mengitex)",
                encryptedPayload = payload
            )
            onClearPendingImportedPayload()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Samsung One UI 8.5 Sub-Screen Header
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.backupCenterTitle,
            subtitle = null,
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "backup_back_button",
            onBack = onBack,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "600k+ Turbo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = isBusy || isAuthLoading || !importProgressText.isNullOrBlank(),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (!importProgressText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = importProgressText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BackupSegmentPill(
                label = s.backupTabDevice,
                icon = Icons.Filled.PhoneAndroid,
                selected = selectedTab == 1,
                testTag = "backup_tab_device",
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            BackupSegmentPill(
                label = s.backupTabCloud,
                icon = Icons.Filled.CloudDone,
                selected = selectedTab == 0,
                testTag = "backup_tab_cloud",
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            label = "backup_tab_content",
            modifier = Modifier.weight(1f)
        ) { tab ->
            if (tab == 0) {
                CloudBackupTabContent(
                    currentUser = currentUser,
                    isAuthLoading = isAuthLoading,
                    isBusy = isBusy,
                    observeCloudBackups = observeCloudBackups,
                    onSignInClicked = {
                        scope.launch {
                            isAuthLoading = true
                            val result = GoogleAuthHelper.signInWithGoogle(context)
                            isAuthLoading = false
                            if (result.isFailure) {
                                val ex = result.exceptionOrNull()
                                if (ex !is GetCredentialCancellationException) {
                                    onShowMessage(ex?.message ?: "Google Sign-In failed")
                                }
                            }
                        }
                    },
                    onSignOutClicked = {
                        scope.launch {
                            isAuthLoading = true
                            GoogleAuthHelper.signOut(context)
                            isAuthLoading = false
                        }
                    },
                    onCreateCloudBackup = { activeCreationTarget = BackupCreationTarget.CLOUD },
                    onRestoreCloudBackup = { record ->
                        pendingRestore = PendingRestoreRequest(
                            sourceLabel = "Cloud (${record.label})",
                            encryptedPayload = record.encryptedPayload
                        )
                    },
                    onRenameCloudBackup = { record -> renamingCloudItem = record },
                    onDeleteCloudBackup = { record -> onDeleteCloudBackup(record.id) }
                )
            } else {
                DeviceAllWaysBackupContent(
                    localSnapshots = localSnapshots,
                    trashCount = trashCount,
                    hasSmsPermission = hasSmsPermission,
                    isBusy = isBusy,
                    isSw = isSw,
                    onImportXmlFile = onImportXmlFileRequested,
                    onExportXmlFile = onExportXmlFileRequested,
                    onExportFile = { activeCreationTarget = BackupCreationTarget.EXPORT_FILE },
                    onImportFile = onImportBackupFileRequested,
                    onShareBackup = { activeCreationTarget = BackupCreationTarget.SHARE_FILE },
                    onCreateLocalSnapshot = { activeCreationTarget = BackupCreationTarget.LOCAL_SNAPSHOT },
                    onRestoreLocalSnapshot = { snap ->
                        onReadLocalSnapshotPayload(snap.fileName) { payload ->
                            if (payload != null) {
                                pendingRestore = PendingRestoreRequest(
                                    sourceLabel = "Snapshot (${snap.label})",
                                    encryptedPayload = payload
                                )
                            } else {
                                onShowMessage("Could not read snapshot.")
                            }
                        }
                    },
                    onDeleteLocalSnapshot = { snap -> onDeleteLocalSnapshot(snap.fileName) },
                    onImportPhoneSystemSms = {
                        if (hasSmsPermission) {
                            onImportPhoneSystemSms()
                        } else {
                            onRequestSmsPermission()
                        }
                    },
                    onOpenTrash = onOpenTrash
                )
            }
        }
    }

    // Dialog 1: Create Backup (Label + Optional Passphrase)
    val creationTarget = activeCreationTarget
    if (creationTarget != null) {
        CreateBackupDialog(
            needsLabel = creationTarget == BackupCreationTarget.CLOUD || creationTarget == BackupCreationTarget.LOCAL_SNAPSHOT,
            onDismiss = { activeCreationTarget = null },
            onConfirm = { label, pass ->
                activeCreationTarget = null
                val cleanPass = pass.trim().ifBlank { null }
                when (creationTarget) {
                    BackupCreationTarget.CLOUD -> onUploadCloudBackup(label, cleanPass)
                    BackupCreationTarget.LOCAL_SNAPSHOT -> onCreateLocalSnapshot(label, cleanPass)
                    BackupCreationTarget.EXPORT_FILE -> onExportBackupFileRequested(cleanPass)
                    BackupCreationTarget.SHARE_FILE -> onShareBackupRequested(cleanPass)
                }
            }
        )
    }

    // Dialog 2: Restore Confirmation (Passphrase if required + Merge/Replace mode)
    val restoreReq = pendingRestore
    if (restoreReq != null) {
        RestoreBackupDialog(
            sourceLabel = restoreReq.sourceLabel,
            requiresPassphrase = PortableBackupCrypto.isPasswordProtected(restoreReq.encryptedPayload),
            onDismiss = { pendingRestore = null },
            onConfirm = { pass, replaceAll ->
                pendingRestore = null
                onRestoreEncryptedPayload(
                    restoreReq.encryptedPayload,
                    pass.trim().ifBlank { null },
                    replaceAll,
                    restoreReq.sourceLabel
                )
            }
        )
    }

    // Dialog 3: Rename Cloud Backup
    val renameTarget = renamingCloudItem
    if (renameTarget != null) {
        RenameBackupDialog(
            currentLabel = renameTarget.label,
            onDismiss = { renamingCloudItem = null },
            onConfirm = { newLabel ->
                renamingCloudItem = null
                onRenameCloudBackup(renameTarget.id, newLabel)
            }
        )
    }
}

@Composable
private fun CloudBackupTabContent(
    currentUser: FirebaseUser?,
    isAuthLoading: Boolean,
    isBusy: Boolean,
    observeCloudBackups: (String) -> Flow<List<CloudBackupRecord>>,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onCreateCloudBackup: () -> Unit,
    onRestoreCloudBackup: (CloudBackupRecord) -> Unit,
    onRenameCloudBackup: (CloudBackupRecord) -> Unit,
    onDeleteCloudBackup: (CloudBackupRecord) -> Unit
) {
    val s = LocalAppStrings.current

    if (currentUser == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BackupHeroCard(
                    title = s.backupCenterHeroTitle,
                    subtitle = s.backupCenterHeroSub
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = s.backupCloudTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = s.backupSignInDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onSignInClicked,
                            enabled = !isAuthLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("backup_google_sign_in_button")
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.backupSignInGoogleBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    // Authenticated Cloud Backup UI strictly keyed by currentUser.uid
    key(currentUser.uid) {
        var cloudState by remember { mutableStateOf<CloudBackupsUiState>(CloudBackupsUiState.Loading) }

        LaunchedEffect(currentUser.uid) {
            cloudState = CloudBackupsUiState.Loading
            observeCloudBackups(currentUser.uid)
                .catch { e ->
                    cloudState = CloudBackupsUiState.Error(e.message ?: s.backupCloudError)
                }
                .collect { items ->
                    cloudState = CloudBackupsUiState.Success(items)
                }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connected Google Account + Backup Now Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PulsingStatusDot(color = Color(0xFF10B981), size = 7.dp)
                                    Text(
                                        text = currentUser.displayName?.takeIf { it.isNotBlank() } ?: s.backupSignedInAs,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = currentUser.email ?: currentUser.uid.take(12),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onSignOutClicked,
                                enabled = !isAuthLoading,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("backup_google_sign_out_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(s.backupSignOutBtn, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = onCreateCloudBackup,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("backup_upload_cloud_now_button")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.backupNowCloudBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            when (val st = cloudState) {
                is CloudBackupsUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = s.backupCloudLoading,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is CloudBackupsUiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${s.backupCloudError}\n${st.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                is CloudBackupsUiState.Success -> {
                    if (st.items.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = s.backupCloudEmpty,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(st.items, key = { it.id }) { record ->
                            CloudBackupItemCard(
                                record = record,
                                onRestore = { onRestoreCloudBackup(record) },
                                onRename = { onRenameCloudBackup(record) },
                                onDelete = { onDeleteCloudBackup(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAllWaysBackupContent(
    localSnapshots: List<LocalBackupSnapshot>,
    trashCount: Int,
    hasSmsPermission: Boolean,
    isBusy: Boolean,
    isSw: Boolean = true,
    onImportXmlFile: () -> Unit = {},
    onExportXmlFile: () -> Unit = {},
    onExportFile: () -> Unit,
    onImportFile: () -> Unit,
    onShareBackup: () -> Unit,
    onCreateLocalSnapshot: () -> Unit,
    onRestoreLocalSnapshot: (LocalBackupSnapshot) -> Unit,
    onDeleteLocalSnapshot: (LocalBackupSnapshot) -> Unit,
    onImportPhoneSystemSms: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val s = LocalAppStrings.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Dedicated XML / XLM 600k+ Hyper-Turbo Stream Import & Export
        item {
            BackupMethodCard(
                icon = Icons.Filled.Sms,
                title = if (isSw) "Import & Export XML / XLM (600k+ Turbo)" else "XML / XLM SMS Import & Export (600k+ Turbo)",
                subtitle = if (isSw) {
                    "Chambua na kuingiza au kutoa hadi SMS 600,000+ kwa sekunde chache sana (Multi-Core Turbo) kutoka faili la XML / XLM, JSON, au CSV."
                } else {
                    "Multi-core Turbo stream up to 600,000+ SMS in just seconds directly from XML / XLM, JSON, or CSV files."
                },
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onImportXmlFile,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_import_xml_button")
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSw) "Import XML / XLM" else "Import XML / XLM",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = onExportXmlFile,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_export_xml_button")
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSw) "Export XML / XLM" else "Export XML / XLM",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 1. Encrypted File (.mengitex) Export & Import (Android SAF - Offline)
        item {
            BackupMethodCard(
                icon = Icons.Filled.FolderZip,
                title = s.backupFileTitle,
                subtitle = s.backupFileSub,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportFile,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_export_file_button")
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.backupExportFileBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onImportFile,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_import_file_button")
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.backupImportFileBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 2. Share / Transfer Backup (Bluetooth, Quick Share, Email)
        item {
            BackupMethodCard(
                icon = Icons.Filled.Share,
                title = s.backupShareTitle,
                subtitle = s.backupShareSub,
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                FilledTonalButton(
                    onClick = onShareBackup,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_share_file_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.backupShareBtn)
                }
            }
        }

        // 3. Instant Local Vault Snapshots
        item {
            BackupMethodCard(
                icon = Icons.Filled.History,
                title = s.backupLocalTitle,
                subtitle = s.backupLocalSub,
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                Button(
                    onClick = onCreateLocalSnapshot,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_create_local_snapshot_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.backupCreateSnapshotBtn)
                }

                if (localSnapshots.isEmpty()) {
                    Text(
                        text = s.backupLocalEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localSnapshots.forEach { snap ->
                            LocalSnapshotRow(
                                snapshot = snap,
                                onRestore = { onRestoreLocalSnapshot(snap) },
                                onDelete = { onDeleteLocalSnapshot(snap) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Phone System SMS Import
        item {
            BackupMethodCard(
                icon = Icons.Filled.Sms,
                title = s.backupSystemSmsTitle,
                subtitle = s.backupSystemSmsSub,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                OutlinedButton(
                    onClick = onImportPhoneSystemSms,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_import_system_sms_button")
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasSmsPermission) s.backupImportSystemSmsBtn else s.btnGrant
                    )
                }
            }
        }

        // 5. Trash Quick Restore Shortcut
        item {
            BackupMethodCard(
                icon = Icons.Filled.DeleteOutline,
                title = s.backupTrashTitle,
                subtitle = s.backupTrashSub(trashCount),
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                OutlinedButton(
                    onClick = onOpenTrash,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_open_trash_button")
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${s.backupOpenTrashBtn} ($trashCount)")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun BackupHeroCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BackupSegmentPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .premiumBounceClickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BackupMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun CloudBackupItemCard(
    record: CloudBackupRecord,
    onRestore: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateStr = record.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "..."

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateStr • ${record.deviceModel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (record.passwordProtected) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = if (record.passwordProtected) s.backupBadgePassProtected else s.backupBadgeAesAuto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = s.backupItemsSummary(
                    record.messageCount.toInt(),
                    record.templateCount.toInt(),
                    record.contactCount.toInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(s.btnRestore)
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = s.backupRenameTitle)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = s.btnDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalSnapshotRow(
    snapshot: LocalBackupSnapshot,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    val dateFmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${dateFmt.format(Date(snapshot.createdAt))} • ${
                        s.backupItemsSummary(
                            snapshot.messageCount,
                            snapshot.templateCount,
                            snapshot.contactCount
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onRestore,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(s.btnRestore, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = s.btnDelete,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    needsLabel: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (label: String, passphrase: String) -> Unit
) {
    val s = LocalAppStrings.current
    val defaultName = remember {
        "MengiTex " + SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())
    }
    var label by remember { mutableStateOf(defaultName) }
    var passphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.EnhancedEncryption, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(s.backupDialogCreateTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needsLabel) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it.take(100) },
                        label = { Text(s.backupDialogLabelField) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(s.backupDialogPassField) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = s.backupDialogPassHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim().ifEmpty { defaultName }, passphrase) },
                modifier = Modifier.testTag("backup_dialog_confirm_create")
            ) {
                Text(s.backupBtnConfirmBackup)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}

@Composable
private fun RestoreBackupDialog(
    sourceLabel: String,
    requiresPassphrase: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (passphrase: String, replaceExisting: Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    var passphrase by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("${s.backupDialogRestoreTitle} • $sourceLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (requiresPassphrase) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(s.backupDialogRestorePassLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Surface(
                    onClick = { replaceExisting = false },
                    shape = RoundedCornerShape(14.dp),
                    color = if (!replaceExisting) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !replaceExisting, onClick = { replaceExisting = false })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.backupRestoreModeMerge, style = MaterialTheme.typography.titleSmall)
                            Text(
                                s.backupRestoreModeMergeSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    onClick = { replaceExisting = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (replaceExisting) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.backupRestoreModeReplace, style = MaterialTheme.typography.titleSmall)
                            Text(
                                s.backupRestoreModeReplaceSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passphrase, replaceExisting) },
                enabled = !requiresPassphrase || passphrase.isNotBlank(),
                modifier = Modifier.testTag("backup_dialog_confirm_restore")
            ) {
                Text(s.backupBtnConfirmRestore)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}

@Composable
private fun RenameBackupDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val s = LocalAppStrings.current
    var text by remember { mutableStateOf(currentLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.backupRenameTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(100) },
                label = { Text(s.backupRenameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text(s.btnSaveAes)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}
