package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Mic
import com.example.BubbleChatActivity
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.data.DecryptedTemplate
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.security.SecuritySettings
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.DetectedSimCard
import com.example.service.SmsDispatchManager
import com.example.service.VoiceDictationHelper
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.android17AvatarColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val chatBubbleTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val chatTimelineDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
}

private val chatDetailDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
}

private sealed interface ChatTimelineItem {
    data class DateHeader(val label: String) : ChatTimelineItem
    data class MessageRow(val message: DecryptedSmsMessage) : ChatTimelineItem
}

/**
 * Premium Conversation Chat Screen:
 * - Luxury gradient outgoing bubbles & layered frosted incoming bubbles
 * - Centered date separator pills (Today / Yesterday / Date)
 * - Secret burn-after-read masked cards with FLAG_SECURE reader
 * - Tap any message bubble for full delivery report, SIM details, Copy & Move to Trash
 * - Floating unlimited-typing chat dock with Secret Burn toggle, Quick Templates, Voice Mic & SIM dispatch
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    contactName: String,
    phoneNumber: String,
    allActiveMessages: List<DecryptedSmsMessage>,
    preIndexedThreadMessages: List<DecryptedSmsMessage>? = null,
    contacts: List<ContactItem> = emptyList(),
    templates: List<DecryptedTemplate>,
    detectedSims: List<DetectedSimCard>,
    settings: SecuritySettings,
    hasSmsPermission: Boolean,
    hasMicPermission: Boolean,
    activeSecretReader: DecryptedSmsMessage?,
    onSelectSim: (Int) -> Unit,
    onSendDirectMessage: (body: String, isSecretBurn: Boolean, fallbackIfDenied: Boolean) -> Unit,
    onForwardMessage: (recipients: List<ContactItem>, body: String, fallbackIfDenied: Boolean) -> Unit = { _, _, _ -> },
    onSaveContactName: (newName: String, phone: String) -> Unit = { _, _ -> },
    onScheduleDraft: (draftBody: String) -> Unit,
    onOpenSecretReader: (DecryptedSmsMessage) -> Unit,
    onCloseSecretReader: (burnNow: Boolean) -> Unit,
    onMoveToTrash: (DecryptedSmsMessage) -> Unit,
    onMoveMessagesToTrash: (List<DecryptedSmsMessage>) -> Unit = { list -> list.forEach(onMoveToTrash) },
    onToggleDeliveryReport: (Boolean) -> Unit = {},
    onChatSizeScaleChange: (Float) -> Unit = {},
    onSendAttachment: (uri: Uri, mimeType: String, displayLabel: String, caption: String) -> Unit = { _, _, _, _ -> },
    onRequestMicPermission: () -> Unit,
    onShowMessage: (String) -> Unit,
    onCopiedSensitiveText: () -> Unit = {},
    onBack: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val normPhone = remember(phoneNumber, contactName) {
        ContactsLoader.resolveCanonicalThreadKey(phoneNumber, contactName)
    }

    var draftText by remember { mutableStateOf("") }
    var isSecretBurnArmed by remember { mutableStateOf(false) }
    var showTemplatesTray by remember { mutableStateOf(false) }
    var inspectedMessage by remember { mutableStateOf<DecryptedSmsMessage?>(null) }
    var showContactProfile by remember { mutableStateOf(false) }
    var showInChatSearch by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var heldActionMessage by remember { mutableStateOf<DecryptedSmsMessage?>(null) }
    var forwardingMessageBody by remember { mutableStateOf<String?>(null) }
    var activePhoneSheetTarget by remember { mutableStateOf<String?>(null) }
    var partialCopyTargetMessage by remember { mutableStateOf<DecryptedSmsMessage?>(null) }
    var showKeystoreSecurityDialog by remember { mutableStateOf(false) }
    val chatKeystoreFingerprint = remember(normPhone) {
        VaultCryptoManager.computeKeystoreFingerprint(normPhone)
    }
    val chatSizeScale = settings.chatSizeScale.coerceIn(0.75f, 1.45f)

    // Left-side `+` Attachment Picker state & zero-permission Android Photo/Document launchers
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showContactNumberPicker by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val (name, size) = resolveUriFileMeta(context, uri, s.attachPhoto)
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            pendingAttachment = PendingAttachment(
                uri = uri,
                mimeType = mime,
                fileName = name,
                sizeLabel = size,
                isPhoto = true
            )
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (name, size) = resolveUriFileMeta(context, uri, s.attachDocument)
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            pendingAttachment = PendingAttachment(
                uri = uri,
                mimeType = mime,
                fileName = name,
                sizeLabel = size,
                isPhoto = false
            )
        }
    }

    // Hold Multi-Select state (Mark All • Unmark All • Delete All • Copy • Share • Forward)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    BackHandler {
        when {
            isSelectionMode -> {
                isSelectionMode = false
                selectedMessageIds = emptySet()
            }
            showInChatSearch -> {
                showInChatSearch = false
                inChatSearchQuery = ""
            }
            else -> onBack()
        }
    }

    val cleanTargetName = remember(contactName, normPhone) {
        val cleaned = ContactsLoader.cleanSenderIdentifier(contactName)
        if (cleaned.equals(normPhone, ignoreCase = true)) "" else cleaned
    }

    // 6G O(1) Thread Resolution: when ViewModel provides the pre-indexed thread bucket, opening or
    // updating a chat takes ~0.01ms even with 600,000+ SMS (zero linear scans, zero coroutine hops!).
    val threadMessages = if (preIndexedThreadMessages != null) {
        remember(preIndexedThreadMessages, allActiveMessages, normPhone, cleanTargetName) {
            val optimisticInFlight = allActiveMessages.takeWhile { it.id < 0L }
            if (optimisticInFlight.isEmpty()) {
                preIndexedThreadMessages
            } else {
                val matchingOptimistic = filterConversationMessagesFast(optimisticInFlight, normPhone, cleanTargetName)
                if (matchingOptimistic.isEmpty()) {
                    preIndexedThreadMessages
                } else {
                    val existingIds = preIndexedThreadMessages.mapTo(HashSet(preIndexedThreadMessages.size + 4)) { it.id }
                    val extras = matchingOptimistic.filter { it.id !in existingIds }
                    if (extras.isEmpty()) preIndexedThreadMessages else preIndexedThreadMessages + extras
                }
            }
        }
    } else if (allActiveMessages.size <= 6000) {
        remember(allActiveMessages, normPhone, cleanTargetName) {
            filterConversationMessagesFast(allActiveMessages, normPhone, cleanTargetName)
        }
    } else {
        val asyncFiltered by androidx.compose.runtime.produceState(
            initialValue = remember(normPhone, cleanTargetName) {
                filterConversationMessagesFast(allActiveMessages, normPhone, cleanTargetName)
            },
            key1 = allActiveMessages,
            key2 = normPhone,
            key3 = cleanTargetName
        ) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                filterConversationMessagesFast(allActiveMessages, normPhone, cleanTargetName)
            }
        }
        remember(asyncFiltered, allActiveMessages, normPhone, cleanTargetName) {
            val optimisticInFlight = allActiveMessages.takeWhile { it.id < 0L }
            if (optimisticInFlight.isEmpty()) {
                asyncFiltered
            } else {
                val matchingOptimistic = filterConversationMessagesFast(optimisticInFlight, normPhone, cleanTargetName)
                if (matchingOptimistic.isEmpty()) {
                    asyncFiltered
                } else {
                    val existingIds = asyncFiltered.mapTo(HashSet(asyncFiltered.size)) { it.id }
                    val extras = matchingOptimistic.filter { it.id !in existingIds }
                    if (extras.isEmpty()) asyncFiltered else asyncFiltered + extras
                }
            }
        }
    }

    val secretMessagesCount = remember(threadMessages) {
        threadMessages.count { it.isSecretBurnAfterRead }
    }

    val latestIncomingReplyTimestamp = remember(threadMessages) {
        threadMessages.lastOrNull { it.direction == MessageDirection.INCOMING }?.timestamp ?: 0L
    }

    val displayedMessages = remember(threadMessages, inChatSearchQuery) {
        val q = inChatSearchQuery.trim()
        if (q.isEmpty()) threadMessages
        else threadMessages.filter { !it.isSecretBurnAfterRead && it.body.contains(q, ignoreCase = true) }
    }

    val selectedMessages = remember(displayedMessages, selectedMessageIds) {
        displayedMessages.filter { it.id in selectedMessageIds }
    }

    val selectedPlainText = remember(selectedMessages) {
        selectedMessages.filter { !it.isSecretBurnAfterRead }.joinToString("\n") { it.body }
    }

    val timelineItems = remember(displayedMessages, s.convToday, s.convYesterday) {
        buildTimelineItems(displayedMessages, s.convToday, s.convYesterday)
    }

    val activeSim = remember(detectedSims, settings.selectedSimSubId) {
        detectedSims.firstOrNull { it.subscriptionId == settings.selectedSimSubId }
            ?: detectedSims.firstOrNull()
    }

    val copyMessageBody: (String) -> Unit = { plainBody ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("MengiTex SMS", plainBody))
        onCopiedSensitiveText()
        onShowMessage(
            if (settings.language == AppLanguage.EN) {
                if (settings.clipboardAutoClearEnabled) "Message copied (auto-clears in 30s)."
                else "Message copied."
            } else {
                if (settings.clipboardAutoClearEnabled) "Ujumbe umenakiliwa (utafutwa clipboard baada ya sek 30)."
                else "Ujumbe umenakiliwa."
            }
        )
    }

    val shareMessageBody: (String) -> Unit = { plainBody ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, plainBody)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, s.convActionShare))
        }
    }

    // Voice dictation inside conversation
    var isListeningVoice by remember { mutableStateOf(false) }
    var voiceRms by remember { mutableFloatStateOf(0f) }
    val voiceHelper = remember(context) {
        VoiceDictationHelper(
            context = context,
            onListeningChanged = { listening -> isListeningVoice = listening },
            onRmsLevelChanged = { rms -> voiceRms = rms },
            onPartialTranscript = {},
            onFinalTranscript = { spoken ->
                val clean = spoken.trim()
                if (clean.isNotEmpty()) {
                    draftText = if (draftText.isBlank()) clean else "$draftText $clean"
                }
            },
            onError = { err -> onShowMessage(err) }
        )
    }
    DisposableEffect(voiceHelper) {
        onDispose { voiceHelper.stopListening() }
    }

    val listState = rememberLazyListState()
    var hasInitialScrolled by remember(normPhone) { mutableStateOf(false) }
    LaunchedEffect(timelineItems.size) {
        if (timelineItems.isNotEmpty()) {
            hasInitialScrolled = true
            listState.scrollToItem(timelineItems.lastIndex)
        }
    }

    // Zero-IPC pure-Kotlin part counter so typing and tapping Send never block the UI thread
    val partCount = remember(draftText) {
        if (draftText.isEmpty()) 0
        else SmsDispatchManager.estimateSmsPartsFast(draftText)
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (convAvatarBg, convAvatarFg) = remember(normPhone, isDarkTheme) {
        android17AvatarColors(normPhone, isDarkTheme)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // 1. Authentic Full-Width Android 17 Google Messages Top Bar
        Surface(
            color = if (isSelectionMode) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.background
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSelectionMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedMessageIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("selection_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = s.btnClose,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = s.convSelectedCount(selectedMessageIds.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = "${selectedMessageIds.size}/${displayedMessages.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionTopChip(
                                icon = Icons.Filled.SelectAll,
                                label = s.convMarkAll,
                                accentColor = MaterialTheme.colorScheme.primary,
                                enabled = displayedMessages.isNotEmpty(),
                                onClick = {
                                    selectedMessageIds = displayedMessages.map { it.id }.toSet()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("selection_mark_all_button")
                            )
                            SelectionTopChip(
                                icon = Icons.Filled.Deselect,
                                label = s.convUnmarkAll,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                enabled = selectedMessageIds.isNotEmpty(),
                                onClick = {
                                    selectedMessageIds = emptySet()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("selection_unmark_all_button")
                            )
                            SelectionTopChip(
                                icon = Icons.Filled.DeleteSweep,
                                label = s.convDeleteAll,
                                accentColor = MaterialTheme.colorScheme.error,
                                enabled = displayedMessages.isNotEmpty(),
                                onClick = {
                                    if (selectedMessageIds.isEmpty()) {
                                        selectedMessageIds = displayedMessages.map { it.id }.toSet()
                                    }
                                    showDeleteSelectedConfirm = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("selection_delete_all_button")
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("conversation_back_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = s.cdBack,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Tappable Contact Header Target
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { showContactProfile = true }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .testTag("conversation_open_profile_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(convAvatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = extractInitials(contactName),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = convAvatarFg
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = contactName.ifBlank { phoneNumber },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = s.convProfileTitle,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (contactName.isNotBlank() && contactName != phoneNumber) {
                                        Text(
                                            text = phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                    if (activeSim != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable(enabled = detectedSims.size > 1) {
                                                    val idx = detectedSims.indexOfFirst { it.subscriptionId == activeSim.subscriptionId }
                                                    val nextSim = detectedSims[(idx + 1) % detectedSims.size]
                                                    onSelectSim(nextSim.subscriptionId)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.SimCard,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = activeSim.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
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

        // 1b. Optional Live In-Chat Search Pill
        AnimatedVisibility(
            visible = showInChatSearch,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (inChatSearchQuery.isEmpty()) {
                            Text(
                                text = s.convSearchInChatHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = inChatSearchQuery,
                            onValueChange = { inChatSearchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_search_input")
                        )
                    }
                    if (inChatSearchQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${displayedMessages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            inChatSearchQuery = ""
                            showInChatSearch = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = s.btnClose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Conversation Timeline Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (timelineItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(convAvatarBg)
                            .clickable { showContactProfile = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = extractInitials(contactName),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = convAvatarFg
                        )
                    }
                    Text(
                        text = s.convNoMessagesYet,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = timelineItems,
                        key = { item ->
                            when (item) {
                                is ChatTimelineItem.DateHeader -> "date_${item.label}"
                                is ChatTimelineItem.MessageRow -> "msg_${item.message.id}"
                            }
                        },
                        contentType = { item ->
                            when (item) {
                                is ChatTimelineItem.DateHeader -> "date_header"
                                is ChatTimelineItem.MessageRow -> if (item.message.direction == MessageDirection.OUTGOING) "outgoing" else "incoming"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ChatTimelineItem.DateHeader -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            is ChatTimelineItem.MessageRow -> {
                                val msgId = item.message.id
                                val isRowSelected = isSelectionMode && msgId in selectedMessageIds
                                PremiumChatBubble(
                                    message = item.message,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isRowSelected,
                                    chatSizeScale = chatSizeScale,
                                    latestIncomingReplyTimestamp = latestIncomingReplyTimestamp,
                                    highlightQuery = inChatSearchQuery,
                                    onTapSecret = {
                                        if (isSelectionMode) {
                                            selectedMessageIds = if (isRowSelected) {
                                                selectedMessageIds - msgId
                                            } else {
                                                selectedMessageIds + msgId
                                            }
                                        } else {
                                            onOpenSecretReader(item.message)
                                        }
                                    },
                                    onTapNormal = {
                                        if (isSelectionMode) {
                                            selectedMessageIds = if (isRowSelected) {
                                                selectedMessageIds - msgId
                                            } else {
                                                selectedMessageIds + msgId
                                            }
                                        } else {
                                            inspectedMessage = item.message
                                        }
                                    },
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isSelectionMode) {
                                            selectedMessageIds = if (isRowSelected) {
                                                selectedMessageIds - msgId
                                            } else {
                                                selectedMessageIds + msgId
                                            }
                                        } else {
                                            heldActionMessage = item.message
                                        }
                                    },
                                    onCallPhoneNumber = { num ->
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                        runCatching { context.startActivity(dialIntent) }
                                    },
                                    onCopyPhoneNumber = { num ->
                                        copyMessageBody(num)
                                    },
                                    onOpenPhoneNumberOptions = { num ->
                                        activePhoneSheetTarget = num
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Optional Secret Burn Armed Strip
        AnimatedVisibility(
            visible = isSecretBurnArmed,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = s.convSecretArmed,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Optional Live Voice Waveform Bar
        AnimatedVisibility(
            visible = isListeningVoice,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VoiceWaveformVisualizer(
                        rmsLevel = voiceRms,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = s.voiceListeningHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 5. Optional Quick Templates Strip
        AnimatedVisibility(
            visible = showTemplatesTray && templates.isNotEmpty(),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                items(templates, key = { it.id }) { tmpl ->
                    Surface(
                        onClick = {
                            draftText = if (draftText.isBlank()) tmpl.body else "$draftText ${tmpl.body}"
                            showTemplatesTray = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 180.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tmpl.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tmpl.body,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Bottom Dock: Full-Width Android 17 Multi-Select Action Bar OR Composer Dock
        if (isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hasCopyableText = selectedPlainText.isNotBlank()
                    SelectionBottomActionButton(
                        icon = Icons.Filled.ContentCopy,
                        label = s.convActionCopy,
                        accentColor = MaterialTheme.colorScheme.primary,
                        enabled = hasCopyableText,
                        onClick = {
                            copyMessageBody(selectedPlainText)
                            isSelectionMode = false
                            selectedMessageIds = emptySet()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("selection_action_copy")
                    )
                    SelectionBottomActionButton(
                        icon = Icons.Filled.Share,
                        label = s.convActionShare,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        enabled = hasCopyableText,
                        onClick = {
                            shareMessageBody(selectedPlainText)
                            isSelectionMode = false
                            selectedMessageIds = emptySet()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("selection_action_share")
                    )
                    SelectionBottomActionButton(
                        icon = Icons.AutoMirrored.Filled.Forward,
                        label = s.convActionForward,
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        enabled = hasCopyableText,
                        onClick = {
                            forwardingMessageBody = selectedPlainText
                            isSelectionMode = false
                            selectedMessageIds = emptySet()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("selection_action_forward")
                    )
                    if (selectedMessages.size == 1) {
                        SelectionBottomActionButton(
                            icon = Icons.Filled.Info,
                            label = s.convActionInfo,
                            accentColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            onClick = {
                                inspectedMessage = selectedMessages.first()
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("selection_action_info")
                        )
                    } else {
                        SelectionBottomActionButton(
                            icon = Icons.Filled.DeleteOutline,
                            label = s.btnDelete,
                            accentColor = MaterialTheme.colorScheme.error,
                            enabled = selectedMessages.isNotEmpty(),
                            onClick = { showDeleteSelectedConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("selection_action_delete")
                        )
                    }
                }
            }
        } else {
        Surface(
            color = if (isSecretBurnArmed) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Compact Top Action Ribbon inside Composer Dock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val secretBg by animateColorAsState(
                            targetValue = if (isSecretBurnArmed) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(20),
                            label = "dock_secret_bg"
                        )
                        val secretFg by animateColorAsState(
                            targetValue = if (isSecretBurnArmed) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(20),
                            label = "dock_secret_fg"
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = secretBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { isSecretBurnArmed = !isSecretBurnArmed }
                                .testTag("conversation_secret_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = s.secretBurnToggleTitle,
                                    tint = secretFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = s.convToolSecret,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSecretBurnArmed) FontWeight.Bold else FontWeight.Medium,
                                    color = secretFg
                                )
                            }
                        }

                        val tmplBg by animateColorAsState(
                            targetValue = if (showTemplatesTray) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(20),
                            label = "dock_tmpl_bg"
                        )
                        val tmplFg = if (showTemplatesTray) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tmplBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showTemplatesTray = !showTemplatesTray }
                                .testTag("conversation_templates_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = s.drawerTemplates,
                                    tint = tmplFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = s.convToolTemplates,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (showTemplatesTray) FontWeight.Bold else FontWeight.Medium,
                                    color = tmplFg
                                )
                            }
                        }

                        val micBg by animateColorAsState(
                            targetValue = if (isListeningVoice) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(20),
                            label = "dock_mic_bg"
                        )
                        val micFg = if (isListeningVoice) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = micBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (!hasMicPermission) {
                                        onRequestMicPermission()
                                        return@clickable
                                    }
                                    if (isListeningVoice) {
                                        voiceHelper.stopListening()
                                    } else {
                                        voiceHelper.startListening(settings.language)
                                    }
                                }
                                .testTag("conversation_mic_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListeningVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                                    contentDescription = s.voiceDictateBtn,
                                    tint = micFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = s.convToolVoice,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isListeningVoice) FontWeight.Bold else FontWeight.Medium,
                                    color = micFg
                                )
                            }
                        }

                        val reportBg by animateColorAsState(
                            targetValue = if (settings.deliveryReportsEnabled) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(20),
                            label = "dock_report_bg"
                        )
                        val reportFg = if (settings.deliveryReportsEnabled) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = reportBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onToggleDeliveryReport(!settings.deliveryReportsEnabled) }
                                .testTag("conversation_delivery_report_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = s.settingsDeliveryReportTitle,
                                    tint = reportFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = s.convToolDeliveryReport,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (settings.deliveryReportsEnabled) FontWeight.Bold else FontWeight.Medium,
                                    color = reportFg,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showKeystoreSecurityDialog = true }
                                .testTag("conversation_keystore_dock_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EnhancedEncryption,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                if (draftText.isNotEmpty()) {
                                    Text(
                                        text = s.charPartsCounter(draftText.length, partCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Pending Photo / Document Attachment Chip
                    val attached = pendingAttachment
                    AnimatedVisibility(
                        visible = attached != null,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        if (attached != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (attached.isPhoto) Icons.Filled.PhotoLibrary else Icons.Filled.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attached.fileName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (attached.sizeLabel.isNotEmpty()) {
                                                "${s.attachReadyLabel} • ${attached.sizeLabel}"
                                            } else {
                                                s.attachReadyLabel
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingAttachment = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = s.btnClose,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Compact Android 17 Google Messages Composer Row
                    var isComposerExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showAttachmentSheet = true }
                                .testTag("conversation_attach_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = s.attachSheetTitle,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            if (detectedSims.size > 1) {
                                                val currentIdx = detectedSims.indexOfFirst { it.subscriptionId == activeSim?.subscriptionId }
                                                val nextSim = detectedSims[(currentIdx + 1) % detectedSims.size]
                                                onSelectSim(nextSim.subscriptionId)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SimCard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = activeSim?.let { "SIM ${it.slotIndex + 1}" } ?: "SMS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (draftText.isEmpty()) {
                                        Text(
                                            text = s.convInputPlaceholder,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = draftText,
                                        onValueChange = { draftText = it },
                                        maxLines = if (isComposerExpanded) Int.MAX_VALUE else 6,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("conversation_message_input")
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListeningVoice) MaterialTheme.colorScheme.errorContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            if (!hasMicPermission) {
                                                onRequestMicPermission()
                                                return@clickable
                                            }
                                            if (isListeningVoice) {
                                                voiceHelper.stopListening()
                                            } else {
                                                voiceHelper.startListening(settings.language)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListeningVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = s.voiceDictateBtn,
                                        tint = if (isListeningVoice) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                if (draftText.length > 120 || draftText.contains('\n')) {
                                    IconButton(
                                        onClick = { isComposerExpanded = !isComposerExpanded },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isComposerExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val canSend = draftText.isNotBlank() || pendingAttachment != null
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                )
                                .combinedClickable(
                                    enabled = canSend,
                                    onClick = {
                                        val currentAtt = pendingAttachment
                                        val textToSend = draftText
                                        if (currentAtt != null) {
                                            val label = if (currentAtt.sizeLabel.isNotEmpty()) {
                                                "${currentAtt.fileName} (${currentAtt.sizeLabel})"
                                            } else {
                                                currentAtt.fileName
                                            }
                                            pendingAttachment = null
                                            draftText = ""
                                            onSendAttachment(
                                                currentAtt.uri,
                                                currentAtt.mimeType,
                                                label,
                                                textToSend
                                            )
                                        } else {
                                            draftText = ""
                                            onSendDirectMessage(textToSend, isSecretBurnArmed, !hasSmsPermission)
                                        }
                                    },
                                    onLongClick = {
                                        if (draftText.isNotBlank()) {
                                            onScheduleDraft(draftText)
                                        }
                                    }
                                )
                                .testTag("conversation_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = s.btnSendCount(1),
                                tint = if (canSend) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // Batch Delete Selected Confirmation Dialog
    if (showDeleteSelectedConfirm && selectedMessages.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(s.convDeleteSelectedTitle) },
            text = { Text(s.convDeleteSelectedBody(selectedMessages.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedMessages.toList()
                        showDeleteSelectedConfirm = false
                        isSelectionMode = false
                        selectedMessageIds = emptySet()
                        onMoveMessagesToTrash(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(s.btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(s.btnCancel)
                }
            }
        )
    }

    // Secret Burn-After-Read Reader Dialog
    if (activeSecretReader != null) {
        SecretBurnReaderDialog(
            message = activeSecretReader,
            autoBurnOnClose = activeSecretReader.isSecretBurnAfterRead || settings.autoDeleteSecretAfterRead,
            onClose = onCloseSecretReader
        )
    }

    // Message Details & Delivery Report Dialog (on single bubble tap)
    val detailMsg = inspectedMessage
    if (detailMsg != null) {
        MessageDetailsDialog(
            message = detailMsg,
            latestIncomingReplyTimestamp = latestIncomingReplyTimestamp,
            onCopy = {
                copyMessageBody(detailMsg.body)
                inspectedMessage = null
            },
            onMoveToTrash = {
                inspectedMessage = null
                onMoveToTrash(detailMsg)
            },
            onDismiss = { inspectedMessage = null }
        )
    }

    // Contact Profile Sheet (tapping avatar/name in header)
    if (showContactProfile) {
        ContactProfileModalSheet(
            contactName = contactName,
            phoneNumber = phoneNumber,
            totalMessages = threadMessages.size,
            secretMessages = secretMessagesCount,
            activeSim = activeSim,
            onSearchInChat = { showInChatSearch = true },
            onScheduleSms = { onScheduleDraft(draftText) },
            onSaveContactName = { newName -> onSaveContactName(newName, phoneNumber) },
            onCopiedSensitiveText = onCopiedSensitiveText,
            onShowMessage = onShowMessage,
            onDismiss = { showContactProfile = false }
        )
    }

    // Long-Press ("Hold") Action Sheet (Copy Text, Forward Text, Delete Text, Share, Quote & Reply, Select Partial Text, Call/Copy Numbers, etc.)
    val heldMsg = heldActionMessage
    if (heldMsg != null) {
        MessageLongPressActionSheet(
            message = heldMsg,
            onCopy = {
                copyMessageBody(heldMsg.body)
                heldActionMessage = null
            },
            onShare = {
                shareMessageBody(heldMsg.body)
                heldActionMessage = null
            },
            onForward = {
                forwardingMessageBody = heldMsg.body
                heldActionMessage = null
            },
            onDetails = {
                inspectedMessage = heldMsg
                heldActionMessage = null
            },
            onMoveToTrash = {
                onMoveToTrash(heldMsg)
                heldActionMessage = null
            },
            onQuoteReply = {
                val snippet = heldMsg.body.replace('\n', ' ').take(90)
                val quoted = "\"$snippet\"\n"
                draftText = if (draftText.isBlank()) quoted else "$quoted$draftText"
                heldActionMessage = null
            },
            onSelectPartialText = {
                partialCopyTargetMessage = heldMsg
                heldActionMessage = null
            },
            onEditInComposer = {
                draftText = if (draftText.isBlank()) heldMsg.body else "$draftText\n${heldMsg.body}"
                heldActionMessage = null
            },
            onSelectMultiple = {
                isSelectionMode = true
                selectedMessageIds = selectedMessageIds + heldMsg.id
                heldActionMessage = null
            },
            onScheduleMessage = {
                val bodyToSchedule = heldMsg.body
                heldActionMessage = null
                onScheduleDraft(bodyToSchedule)
            },
            onCopySpecificText = { specificText ->
                copyMessageBody(specificText)
                heldActionMessage = null
            },
            onCallPhoneNumber = { num ->
                heldActionMessage = null
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                runCatching { context.startActivity(dialIntent) }
            },
            onDismiss = { heldActionMessage = null }
        )
    }

    // Phone Number Quick Action Sheet (when tapping a phone number sent inside a chat message)
    val targetPhoneForSheet = activePhoneSheetTarget
    if (targetPhoneForSheet != null) {
        PhoneNumberQuickActionSheet(
            phoneNumber = targetPhoneForSheet,
            onCall = { num ->
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                runCatching { context.startActivity(dialIntent) }
            },
            onCopy = { num ->
                copyMessageBody(num)
            },
            onInsertInComposer = { num ->
                draftText = if (draftText.isBlank()) num else "$draftText $num"
            },
            onSaveToContacts = { num ->
                onSaveContactName(num, num)
            },
            onDismiss = { activePhoneSheetTarget = null }
        )
    }

    // Select & Copy Part of Text / Numbers Dialog
    val partialMsg = partialCopyTargetMessage
    if (partialMsg != null) {
        SelectAndCopyPartialTextDialog(
            messageBody = partialMsg.body,
            onCopyText = { textToCopy ->
                copyMessageBody(textToCopy)
            },
            onCallPhone = { num ->
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                runCatching { context.startActivity(dialIntent) }
            },
            onDismiss = { partialCopyTargetMessage = null }
        )
    }

    // Forward Message Multi-Recipient Picker Sheet
    val forwardBody = forwardingMessageBody
    if (forwardBody != null) {
        ForwardMessageModalSheet(
            messageBody = forwardBody,
            contacts = contacts,
            onForwardTo = { recipients ->
                onForwardMessage(recipients, forwardBody, !hasSmsPermission)
                forwardingMessageBody = null
            },
            onDismiss = { forwardingMessageBody = null }
        )
    }

    // Left `+` Choose What To Send Sheet (Photo • Document • Contact Number • Template • Secret • Schedule)
    if (showAttachmentSheet) {
        ConversationAttachmentSheet(
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickDocument = {
                documentPickerLauncher.launch(arrayOf("*/*"))
            },
            onPickContactNumber = {
                showContactNumberPicker = true
            },
            onOpenTemplates = {
                showTemplatesTray = true
            },
            onToggleSecret = {
                isSecretBurnArmed = !isSecretBurnArmed
            },
            onScheduleSms = {
                onScheduleDraft(draftText)
            },
            onDismiss = { showAttachmentSheet = false }
        )
    }

    // Pick Contact / Number Sheet (inserts formatted contact number into SMS composer)
    if (showContactNumberPicker) {
        ContactNumberPickerSheet(
            contacts = contacts,
            onInsertContactCard = { formatted ->
                draftText = if (draftText.isBlank()) formatted else "$draftText\n$formatted"
            },
            onDismiss = { showContactNumberPicker = false }
        )
    }

    // Real-Time Android Keystore AES-256-GCM AAD-v3 Chat Security Inspector Pop Dialog
    if (showKeystoreSecurityDialog) {
        KeystoreChatSecurityDialog(
            chatTitle = "${contactName.ifBlank { phoneNumber }} ($phoneNumber)",
            scopeKey = normPhone,
            encryptedMessageCount = threadMessages.size,
            isSecretBurnArmed = isSecretBurnArmed,
            onToggleSecretBurn = { isSecretBurnArmed = !isSecretBurnArmed },
            onCopyFingerprint = { fp ->
                copyMessageBody(fp)
            },
            onDismiss = { showKeystoreSecurityDialog = false }
        )
    }
}

@Composable
private fun SelectionTopChip(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectionBottomActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumChatBubble(
    message: DecryptedSmsMessage,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    chatSizeScale: Float = 1.0f,
    latestIncomingReplyTimestamp: Long = 0L,
    highlightQuery: String = "",
    onTapSecret: () -> Unit,
    onTapNormal: () -> Unit,
    onLongPress: () -> Unit,
    onCallPhoneNumber: (String) -> Unit = {},
    onCopyPhoneNumber: (String) -> Unit = {},
    onOpenPhoneNumberOptions: (String) -> Unit = {}
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val isOutgoing = message.direction == MessageDirection.OUTGOING
    val timeText = remember(message.timestamp) {
        (chatBubbleTimeFormatter.get() ?: SimpleDateFormat("HH:mm", Locale.getDefault()))
            .format(Date(message.timestamp))
    }
    val detectedPhoneSpans = remember(message.body, message.isSecretBurnAfterRead) {
        if (message.isSecretBurnAfterRead) emptyList()
        else ContactsLoader.extractPhoneNumberSpans(message.body)
    }
    val detectedPhoneNumbers = remember(detectedPhoneSpans) {
        if (detectedPhoneSpans.isEmpty()) emptyList()
        else detectedPhoneSpans.map { it.rawMatch }.distinct()
    }

    val scaledMaxWidth = (312f * chatSizeScale).coerceIn(248f, 360f).dp
    val scaledHorizontalPad = (14f * chatSizeScale).coerceIn(10f, 19f).dp
    val scaledVerticalPad = (10f * chatSizeScale).coerceIn(7f, 15f).dp
    val scaledBodyFontSize = (14.5f * chatSizeScale).sp
    val scaledBodyLineHeight = (20.2f * chatSizeScale).sp
    val scaledMetaFontSize = (10.5f * chatSizeScale).coerceIn(9.2f, 13.5f).sp

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 22.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(horizontal = if (isSelectionMode) 4.dp else 0.dp, vertical = if (isSelected) 3.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (isSelectionMode && !isOutgoing) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp)
            )
        }
        if (message.isSecretBurnAfterRead) {
            Surface(
                shape = bubbleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .widthIn(max = scaledMaxWidth)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = onTapSecret,
                        onLongClick = onLongPress
                    )
                    .testTag("chat_secret_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = scaledHorizontalPad, vertical = scaledVerticalPad),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = s.filterSecret,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = s.secretCardMask,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = scaledBodyFontSize,
                            lineHeight = scaledBodyLineHeight
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } else if (isOutgoing) {
            val outBubbleBg = MaterialTheme.colorScheme.primaryContainer
            val outBubbleFg = MaterialTheme.colorScheme.onPrimaryContainer
            val outPhoneColor = MaterialTheme.colorScheme.primary
            val outPhoneBg = MaterialTheme.colorScheme.surfaceContainerLowest
            val outHighlightColor = MaterialTheme.colorScheme.onTertiaryContainer
            val outHighlightBg = MaterialTheme.colorScheme.tertiaryContainer
            val outAnnotatedText = remember(
                message.body,
                highlightQuery,
                detectedPhoneSpans,
                outPhoneColor,
                outPhoneBg,
                outHighlightColor,
                outHighlightBg
            ) {
                buildPhoneAndSearchHighlightedText(
                    text = message.body,
                    query = highlightQuery,
                    phoneColor = outPhoneColor,
                    phoneBg = outPhoneBg,
                    highlightColor = outHighlightColor,
                    highlightBg = outHighlightBg,
                    precomputedPhoneSpans = detectedPhoneSpans
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = scaledMaxWidth)
                    .clip(bubbleShape)
                    .background(outBubbleBg)
                    .combinedClickable(
                        onClick = {
                            if (!isSelectionMode && detectedPhoneNumbers.size == 1 && message.body.trim().length <= 18) {
                                onOpenPhoneNumberOptions(detectedPhoneNumbers.first())
                            } else {
                                onTapNormal()
                            }
                        },
                        onLongClick = onLongPress
                    )
                    .padding(horizontal = scaledHorizontalPad, vertical = scaledVerticalPad)
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = outAnnotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = scaledBodyFontSize,
                            lineHeight = scaledBodyLineHeight
                        ),
                        color = outBubbleFg
                    )
                    if (detectedPhoneNumbers.isNotEmpty()) {
                        DetectedPhoneNumbersBubbleBar(
                            phoneNumbers = detectedPhoneNumbers,
                            isOutgoingBubble = true,
                            isSw = isSw,
                            onCall = onCallPhoneNumber,
                            onCopy = onCopyPhoneNumber,
                            onOpenOptions = onOpenPhoneNumberOptions
                        )
                    }

                    // Clean Tick + Delivery Status Badge (100% solid matte Android 17 tonal colors)
                    val hasRepliedAfter = latestIncomingReplyTimestamp > message.timestamp
                    val (tickIcon, tickTint, deliveryLabel) = when {
                        message.status == MessageStatus.DELIVERED -> Triple(
                            Icons.Filled.DoneAll,
                            MaterialTheme.colorScheme.primary,
                            s.deliveryLabelDelivered
                        )
                        message.status == MessageStatus.SENT && hasRepliedAfter -> Triple(
                            Icons.Filled.DoneAll,
                            MaterialTheme.colorScheme.secondary,
                            s.deliveryLabelDelivered
                        )
                        message.status == MessageStatus.SENDING -> Triple(
                            Icons.Filled.Schedule,
                            MaterialTheme.colorScheme.tertiary,
                            s.deliveryLabelSending
                        )
                        message.status == MessageStatus.FAILED -> Triple(
                            Icons.Filled.ErrorOutline,
                            MaterialTheme.colorScheme.error,
                            s.deliveryLabelFailed
                        )
                        else -> Triple(
                            Icons.Filled.Done,
                            outBubbleFg,
                            s.deliveryLabelSent
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.partCount > 1) {
                            Text(
                                text = "${message.partCount}p •",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                                color = outBubbleFg
                            )
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                            color = outBubbleFg
                        )
                        Text(
                            text = "• $deliveryLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                            fontWeight = FontWeight.SemiBold,
                            color = tickTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = deliveryLabel,
                            tint = tickTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            val highlightColor = MaterialTheme.colorScheme.primary
            val highlightBg = MaterialTheme.colorScheme.primaryContainer
            Surface(
                shape = bubbleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = scaledMaxWidth)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {
                            if (!isSelectionMode && detectedPhoneNumbers.size == 1 && message.body.trim().length <= 18) {
                                onOpenPhoneNumberOptions(detectedPhoneNumbers.first())
                            } else {
                                onTapNormal()
                            }
                        },
                        onLongClick = onLongPress
                    )
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = scaledHorizontalPad, vertical = scaledVerticalPad),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cleanName = ContactsLoader.cleanSenderIdentifier(message.contactName)
                    val cleanPhone = ContactsLoader.cleanSenderIdentifier(message.phoneNumber)
                    val senderHeader = when {
                        cleanName.isNotEmpty() && cleanPhone.isNotEmpty() && !cleanName.equals(cleanPhone, ignoreCase = true) ->
                            "$cleanName • $cleanPhone"
                        cleanName.isNotEmpty() -> cleanName
                        cleanPhone.isNotEmpty() -> cleanPhone
                        else -> ""
                    }
                    if (senderHeader.isNotEmpty() || message.status == MessageStatus.BLOCKED_AD) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (senderHeader.isNotEmpty()) {
                                Text(
                                    text = senderHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (message.status == MessageStatus.BLOCKED_AD) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "ADS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (message.isSilentIntercepted && message.status != MessageStatus.BLOCKED_AD) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = s.statusSilentUnknown,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    val inPhoneColor = MaterialTheme.colorScheme.primary
                    val inPhoneBg = MaterialTheme.colorScheme.primaryContainer
                    val inAnnotatedText = remember(
                        message.body,
                        highlightQuery,
                        detectedPhoneSpans,
                        inPhoneColor,
                        inPhoneBg,
                        highlightColor,
                        highlightBg
                    ) {
                        buildPhoneAndSearchHighlightedText(
                            text = message.body,
                            query = highlightQuery,
                            phoneColor = inPhoneColor,
                            phoneBg = inPhoneBg,
                            highlightColor = highlightColor,
                            highlightBg = highlightBg,
                            precomputedPhoneSpans = detectedPhoneSpans
                        )
                    }
                    Text(
                        text = inAnnotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = scaledBodyFontSize,
                            lineHeight = scaledBodyLineHeight
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (detectedPhoneNumbers.isNotEmpty()) {
                        DetectedPhoneNumbersBubbleBar(
                            phoneNumbers = detectedPhoneNumbers,
                            isOutgoingBubble = false,
                            isSw = isSw,
                            onCall = onCallPhoneNumber,
                            onCopy = onCopyPhoneNumber,
                            onOpenOptions = onOpenPhoneNumberOptions
                        )
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = scaledMetaFontSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        if (isSelectionMode && isOutgoing) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
            )
        }
    }
}

/**
 * Interactive Phone Number Pill Bar rendered inside BOTH sent (outgoing) and received (incoming)
 * chat bubbles whenever the message contains one or more phone numbers.
 * Gives instant 1-tap Call ("Piga"), Copy ("Copy"), and Options ("Chaguo") for every number.
 */
@Composable
internal fun DetectedPhoneNumbersBubbleBar(
    phoneNumbers: List<String>,
    isOutgoingBubble: Boolean,
    isSw: Boolean,
    onCall: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenOptions: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        phoneNumbers.forEach { phone ->
            val containerBg = if (isOutgoingBubble) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val numberTextColor = if (isOutgoingBubble) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }

            Surface(
                onClick = { onOpenOptions(phone) },
                shape = RoundedCornerShape(12.dp),
                color = containerBg,
                modifier = Modifier.testTag("bubble_phone_pill_$phone")
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = numberTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = numberTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onCall(phone) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("bubble_phone_call_$phone")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = if (isSw) "Piga" else "Call",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { onCopy(phone) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("bubble_phone_copy_$phone")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun buildPhoneAndSearchHighlightedText(
    text: String,
    query: String,
    phoneColor: Color,
    phoneBg: Color,
    highlightColor: Color,
    highlightBg: Color,
    precomputedPhoneSpans: List<ContactsLoader.DetectedPhoneSpan>? = null
): androidx.compose.ui.text.AnnotatedString {
    val phoneSpans = precomputedPhoneSpans ?: ContactsLoader.extractPhoneNumberSpans(text)
    if (phoneSpans.isEmpty() && query.isBlank()) {
        return androidx.compose.ui.text.AnnotatedString(text)
    }
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        for (span in phoneSpans) {
            if (span.startIndex in 0 until text.length && span.endIndex <= text.length && span.startIndex < span.endIndex) {
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = phoneColor,
                        background = phoneBg,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    start = span.startIndex,
                    end = span.endIndex
                )
            }
        }
        val q = query.trim()
        if (q.isNotEmpty()) {
            var startIdx = text.indexOf(q, ignoreCase = true)
            while (startIdx >= 0) {
                val endIdx = (startIdx + q.length).coerceAtMost(text.length)
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = highlightColor,
                        background = highlightBg,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    start = startIdx,
                    end = endIdx
                )
                startIdx = text.indexOf(q, startIdx + q.length, ignoreCase = true)
            }
        }
    }
}

private fun filterConversationMessagesFast(
    allActiveMessages: List<DecryptedSmsMessage>,
    normPhone: String,
    cleanTargetName: String
): List<DecryptedSmsMessage> {
    if (allActiveMessages.isEmpty()) return emptyList()
    val hasNameMatch = cleanTargetName.isNotEmpty()
    val matched = ArrayList<DecryptedSmsMessage>(64)
    // Iterate backwards because activeMessages is ordered by timestamp DESC, so reversing gives ascending chronological order without sorting!
    for (i in allActiveMessages.size - 1 downTo 0) {
        val m = allActiveMessages[i]
        if (m.canonicalThreadKey == normPhone ||
            (hasNameMatch && m.contactName.equals(cleanTargetName, ignoreCase = true))
        ) {
            matched.add(m)
        }
    }
    if (matched.size > 1) {
        matched.sortBy { it.timestamp }
    }
    return matched
}

@Composable
private fun MessageDetailsDialog(
    message: DecryptedSmsMessage,
    latestIncomingReplyTimestamp: Long = 0L,
    onCopy: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val fullDate = remember(message.timestamp) {
        (chatDetailDateFormatter.get() ?: SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()))
            .format(Date(message.timestamp))
    }
    val statusText = when (message.status) {
        MessageStatus.DELIVERED -> s.statusDelivered
        MessageStatus.SENT -> s.statusSent
        MessageStatus.SENDING -> s.statusSending
        MessageStatus.FAILED -> s.statusFailed
        MessageStatus.RECEIVED -> if (message.isSilentIntercepted) s.statusSilentUnknown else s.statusReceived
        else -> message.status
    }

    val cleanStatusNote = remember(message.statusNote) {
        val raw = message.statusNote.trim()
        val lower = raw.lowercase(Locale.ROOT)
        if (lower.contains("amekublock") ||
            lower.contains("hajakublock") ||
            lower.contains("inawezekana") ||
            lower.contains("haijafika") ||
            lower.contains("blocked")
        ) {
            ""
        } else {
            raw
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.convMessageReportTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$statusText • ${message.simSlotLabel} • ${s.partsWord} ${message.partCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = fullDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cleanStatusNote.isNotBlank()) {
                    Text(
                        text = "${s.reportPrefix}: $cleanStatusNote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.convCopyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onMoveToTrash,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.convMoveToTrash, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.btnClose) }
        }
    )
}

private fun buildTimelineItems(
    messages: List<DecryptedSmsMessage>,
    todayLabel: String,
    yesterdayLabel: String
): List<ChatTimelineItem> {
    if (messages.isEmpty()) return emptyList()
    val now = Calendar.getInstance()
    val nowYear = now.get(Calendar.YEAR)
    val nowDayOfYear = now.get(Calendar.DAY_OF_YEAR)
    val cal = Calendar.getInstance()
    val dateFmt = chatTimelineDateFormatter.get() ?: SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val result = ArrayList<ChatTimelineItem>(messages.size + 8)
    var lastYear = -1
    var lastDayOfYear = -1

    for (i in 0 until messages.size) {
        val msg = messages[i]
        cal.timeInMillis = msg.timestamp
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        if (year != lastYear || dayOfYear != lastDayOfYear) {
            lastYear = year
            lastDayOfYear = dayOfYear
            val sameYear = nowYear == year
            val diff = nowDayOfYear - dayOfYear
            val headerLabel = when {
                sameYear && diff == 0 -> todayLabel
                sameYear && diff == 1 -> yesterdayLabel
                else -> dateFmt.format(Date(msg.timestamp))
            }
            result.add(ChatTimelineItem.DateHeader(headerLabel))
        }
        result.add(ChatTimelineItem.MessageRow(msg))
    }
    return result
}
