package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.data.SiriSmsDatabase
import com.example.data.SiriSmsRepository
import com.example.security.HiddenVaultLogger
import com.example.security.SecurityPreferences
import com.example.security.SecuritySettings
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.SmsDispatchManager
import com.example.service.SmsStealthReceiver
import com.example.ui.i18n.AppLanguage
import com.example.ui.screens.avatarGradientForKey
import com.example.ui.screens.extractInitials
import com.example.ui.theme.SiriSmsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BubbleThreadSummary(
    val canonicalKey: String,
    val displayName: String,
    val phoneNumber: String,
    val latestMessage: DecryptedSmsMessage,
    val messageCount: Int,
    val unreadCount: Int,
    val isBlockedAdThread: Boolean
)

private fun buildBubbleThreadsFast(
    messages: List<DecryptedSmsMessage>,
    contactMap: Map<String, String>
): List<BubbleThreadSummary> {
    if (messages.isEmpty()) return emptyList()

    class Acc(
        val key: String,
        val isAdBucket: Boolean,
        var latest: DecryptedSmsMessage,
        var bestName: String? = null,
        var bestPhone: String? = null,
        var count: Int = 0,
        var unread: Int = 0
    )

    val inboxMap = LinkedHashMap<String, Acc>(128)
    val adsMap = LinkedHashMap<String, Acc>(64)

    for (i in 0 until messages.size) {
        val m = messages[i]
        val isAd = m.status == MessageStatus.BLOCKED_AD
        val key = m.canonicalThreadKey
        val bucket = if (isAd) adsMap else inboxMap
        var acc = bucket[key]
        if (acc == null) {
            acc = Acc(key = key, isAdBucket = isAd, latest = m)
            bucket[key] = acc
        } else if (m.timestamp > acc.latest.timestamp) {
            acc.latest = m
        }
        acc.count++
        if (!isAd && m.direction == MessageDirection.INCOMING && !m.isRead) {
            acc.unread++
        }
        if (acc.bestPhone == null || acc.bestName == null) {
            val cleanPhone = ContactsLoader.cleanSenderIdentifier(m.phoneNumber)
            val cleanName = ContactsLoader.cleanSenderIdentifier(m.contactName)
            if (acc.bestPhone == null && cleanPhone.isNotEmpty()) {
                acc.bestPhone = cleanPhone
            }
            if (acc.bestName == null && cleanName.isNotEmpty() && cleanName != cleanPhone) {
                acc.bestName = cleanName
            }
        }
    }

    val out = ArrayList<BubbleThreadSummary>(inboxMap.size + adsMap.size)
    fun appendFrom(map: LinkedHashMap<String, Acc>) {
        for (acc in map.values) {
            val latest = acc.latest
            val cleanLatestPhone = ContactsLoader.cleanSenderIdentifier(latest.phoneNumber)
            val cleanLatestName = ContactsLoader.cleanSenderIdentifier(latest.contactName)
            val resolvedPhone = acc.bestPhone ?: cleanLatestPhone.ifEmpty { cleanLatestName }.ifEmpty { "SMS" }
            val savedName = contactMap[acc.key] ?: contactMap[ContactsLoader.normalizePhone(resolvedPhone)]
            val displayName = savedName ?: acc.bestName ?: cleanLatestName.ifEmpty { resolvedPhone }
            out.add(
                BubbleThreadSummary(
                    canonicalKey = acc.key,
                    displayName = displayName,
                    phoneNumber = resolvedPhone,
                    latestMessage = latest,
                    messageCount = acc.count,
                    unreadCount = acc.unread,
                    isBlockedAdThread = acc.isAdBucket
                )
            )
        }
    }
    appendFrom(inboxMap)
    appendFrom(adsMap)
    return out
}

/**
 * Floating Android Notification Bubble & Pop Reply activity (`allowEmbedded="true"`,
 * `resizeableActivity="true"`).
 *
 * Shows ALL conversations right inside the Bubble:
 * - Horizontal Quick-Switch Conversation Strip at the top to jump between any chat in 1 tap
 * - Full "Mazungumzo Yote / All Conversations" directory view (with search + All/Ads filter)
 * - Complete scrollable chat message timeline for the active conversation (every message reads by number & name)
 * - Instant unlimited Pop Reply composer at the bottom
 */
class BubbleChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SENDER_NAME = "extra_bubble_sender_name"
        const val EXTRA_PHONE_NUMBER = "extra_bubble_phone"
        const val EXTRA_LAST_MESSAGE = "extra_bubble_last_message"
    }

    private var requestedSenderNameState by mutableStateOf("")
    private var requestedPhoneNumberState by mutableStateOf("")
    private var requestedLastMessageState by mutableStateOf("")
    private var intentVersionState by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractIntentExtras(intent)

        val appCtx = applicationContext
        val crypto = VaultCryptoManager(appCtx)
        val logger = HiddenVaultLogger(appCtx, crypto)
        val dao = SiriSmsDatabase.getInstance(appCtx).dao()
        val repository = SiriSmsRepository(dao, crypto, logger)

        setContent {
            var settings by remember { mutableStateOf(SecuritySettings()) }
            var contactMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    settings = SecurityPreferences(appCtx).getSnapshot()
                    val map = HashMap<String, String>(256)
                    runCatching { dao.getLocalContactsOnce() }.getOrDefault(emptyList()).forEach { c ->
                        val norm = ContactsLoader.normalizePhone(c.phoneNumber)
                        val name = ContactsLoader.cleanSenderIdentifier(c.name)
                        if (norm.isNotEmpty() && name.isNotEmpty()) map[norm] = name
                    }
                    runCatching { ContactsLoader.loadDeviceContacts(appCtx) }.getOrDefault(emptyList()).forEach { c: ContactItem ->
                        val norm = ContactsLoader.normalizePhone(c.phoneNumber)
                        val name = ContactsLoader.cleanSenderIdentifier(c.name)
                        if (norm.isNotEmpty() && name.isNotEmpty()) map.putIfAbsent(norm, name)
                    }
                    contactMap = map
                }
            }

            val allMessages by repository.activeMessages.collectAsState(initial = emptyList())

            val resolvedBubbleDark = settings.resolveIsDarkTheme(androidx.compose.foundation.isSystemInDarkTheme())
            SiriSmsTheme(
                darkTheme = resolvedBubbleDark,
                dimLightEnabled = settings.dimLightEnabled,
                dimLightLevel = settings.dimLightLevel
            ) {
                BubbleChatContent(
                    initialSenderName = requestedSenderNameState,
                    initialPhoneNumber = requestedPhoneNumberState,
                    initialLastMessage = requestedLastMessageState,
                    intentVersion = intentVersionState,
                    allMessages = allMessages,
                    contactMap = contactMap,
                    repository = repository,
                    language = settings.language,
                    selectedSimSubId = settings.selectedSimSubId,
                    onOpenFullApp = { targetName, targetPhone ->
                        val launchIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            if (targetPhone.isNotBlank() || targetName.isNotBlank()) {
                                putExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE, targetPhone.ifBlank { targetName })
                                putExtra(SmsStealthReceiver.EXTRA_OPEN_NAME, targetName.ifBlank { targetPhone })
                            }
                        }
                        startActivity(launchIntent)
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentExtras(intent)
    }

    private fun extractIntentExtras(intent: Intent?) {
        if (intent == null) return
        val newSender = intent.getStringExtra(EXTRA_SENDER_NAME)
            ?: intent.getStringExtra(SmsStealthReceiver.EXTRA_OPEN_NAME)
            ?: ""
        val newPhone = intent.getStringExtra(EXTRA_PHONE_NUMBER)
            ?: intent.getStringExtra(SmsStealthReceiver.EXTRA_OPEN_PHONE)
            ?: ""
        val newLastMsg = intent.getStringExtra(EXTRA_LAST_MESSAGE).orEmpty()
        requestedSenderNameState = newSender
        requestedPhoneNumberState = newPhone
        requestedLastMessageState = newLastMsg
        intentVersionState++
    }
}

private fun matchesActivePersonMessage(
    msg: DecryptedSmsMessage,
    activeCanonicalKey: String,
    activeCleanName: String,
    contactMap: Map<String, String>
): Boolean {
    if (activeCanonicalKey.isEmpty() || activeCanonicalKey == "UNKNOWN") return false
    val msgKey = msg.canonicalThreadKey
    if (msgKey == activeCanonicalKey) return true

    if (activeCleanName.isNotEmpty() && !activeCleanName.equals(activeCanonicalKey, ignoreCase = true)) {
        if (msg.contactName.equals(activeCleanName, ignoreCase = true)) return true
        val mappedContactName = contactMap[msgKey]
        if (mappedContactName != null && mappedContactName.equals(activeCleanName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

@Composable
private fun BubbleChatContent(
    initialSenderName: String,
    initialPhoneNumber: String,
    initialLastMessage: String,
    intentVersion: Int,
    allMessages: List<DecryptedSmsMessage>,
    contactMap: Map<String, String>,
    repository: SiriSmsRepository,
    language: AppLanguage,
    selectedSimSubId: Int,
    onOpenFullApp: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW

    var activeSenderName by remember {
        mutableStateOf(
            ContactsLoader.cleanSenderIdentifier(initialSenderName)
                .ifEmpty { ContactsLoader.cleanSenderIdentifier(initialPhoneNumber) }
        )
    }
    var activePhoneNumber by remember {
        mutableStateOf(
            ContactsLoader.cleanSenderIdentifier(initialPhoneNumber)
                .ifEmpty { ContactsLoader.cleanSenderIdentifier(initialSenderName) }
        )
    }
    // Always open directly into the active person's full chat timeline so all messages with that person are immediately visible
    var showAllConversationsDirectory by remember {
        mutableStateOf(false)
    }
    var showAdsInDirectory by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var heldBubbleMessage by remember { mutableStateOf<DecryptedSmsMessage?>(null) }

    val copyBubbleText: (String) -> Unit = { textToCopy ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("MengiTex SMS", textToCopy))
        statusText = if (isSw) "Ujumbe / Namba imenakiliwa ✓" else "Copied to clipboard ✓"
    }

    val callBubblePhone: (String) -> Unit = { phoneToCall ->
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneToCall.trim()}"))
        runCatching { context.startActivity(dialIntent) }
    }

    // Update active person immediately when a new Bubble intent arrives
    LaunchedEffect(intentVersion, initialSenderName, initialPhoneNumber) {
        val cleanName = ContactsLoader.cleanSenderIdentifier(initialSenderName)
        val cleanPhone = ContactsLoader.cleanSenderIdentifier(initialPhoneNumber)
        if (cleanName.isNotEmpty() || cleanPhone.isNotEmpty()) {
            activeSenderName = cleanName.ifEmpty { cleanPhone }
            activePhoneNumber = cleanPhone.ifEmpty { cleanName }
            showAllConversationsDirectory = false
        }
    }

    BackHandler(enabled = !showAllConversationsDirectory) {
        showAllConversationsDirectory = true
    }

    // Build all conversation threads on Dispatchers.Default so even 100k+ SMS is instant
    val allThreads by produceState(
        initialValue = remember(allMessages, contactMap) {
            if (allMessages.size <= 2500) buildBubbleThreadsFast(allMessages, contactMap) else emptyList()
        },
        key1 = allMessages,
        key2 = contactMap
    ) {
        value = if (allMessages.size <= 2500) {
            buildBubbleThreadsFast(allMessages, contactMap)
        } else {
            withContext(Dispatchers.Default) {
                buildBubbleThreadsFast(allMessages, contactMap)
            }
        }
    }

    // Auto-resolve the active person's display name & phone number, or default to the most recent active chat
    LaunchedEffect(allThreads, contactMap) {
        if (activePhoneNumber.isBlank() && activeSenderName.isBlank() && allThreads.isNotEmpty()) {
            val first = allThreads.firstOrNull { !it.isBlockedAdThread } ?: allThreads.first()
            activeSenderName = first.displayName
            activePhoneNumber = first.phoneNumber
        } else if (activePhoneNumber.isNotBlank() || activeSenderName.isNotBlank()) {
            val key = ContactsLoader.resolveCanonicalThreadKey(activePhoneNumber, activeSenderName)
            val matched = allThreads.firstOrNull {
                it.canonicalKey == key ||
                    (activeSenderName.isNotBlank() && it.displayName.equals(activeSenderName, ignoreCase = true))
            }
            if (matched != null) {
                activeSenderName = matched.displayName
                activePhoneNumber = matched.phoneNumber
            } else {
                val mappedName = contactMap[key]
                if (!mappedName.isNullOrBlank()) {
                    activeSenderName = mappedName
                }
            }
        }
    }

    val activeCanonicalKey = remember(activePhoneNumber, activeSenderName) {
        ContactsLoader.resolveCanonicalThreadKey(activePhoneNumber, activeSenderName)
    }
    val activeCleanName = remember(activeSenderName, activeCanonicalKey, contactMap) {
        val mapped = contactMap[activeCanonicalKey]
        ContactsLoader.cleanSenderIdentifier(mapped ?: activeSenderName)
    }
    val bubbleKeystoreFingerprint = remember(activeCanonicalKey) {
        VaultCryptoManager.computeKeystoreFingerprint(activeCanonicalKey)
    }

    // Mark active conversation messages read in a single batch SQLite transaction when viewing
    LaunchedEffect(activeCanonicalKey, activeCleanName, showAllConversationsDirectory, allMessages.size) {
        if (!showAllConversationsDirectory && activeCanonicalKey.isNotEmpty() && activeCanonicalKey != "UNKNOWN") {
            withContext(Dispatchers.IO) {
                val unreadIds = ArrayList<Long>()
                for (i in 0 until allMessages.size) {
                    val m = allMessages[i]
                    if (!m.isRead && matchesActivePersonMessage(m, activeCanonicalKey, activeCleanName, contactMap)) {
                        unreadIds.add(m.id)
                    }
                }
                if (unreadIds.isNotEmpty()) {
                    repository.markReadBatch(unreadIds)
                }
            }
        }
    }

    // ALL chat messages (both incoming and outgoing) for the person the user is currently chatting with
    val activeThreadMessages by produceState(
        initialValue = remember(activeCanonicalKey, activeCleanName) {
            if (allMessages.size <= 3000) {
                val out = ArrayList<DecryptedSmsMessage>()
                for (i in allMessages.size - 1 downTo 0) {
                    val m = allMessages[i]
                    if (matchesActivePersonMessage(m, activeCanonicalKey, activeCleanName, contactMap)) {
                        out.add(m)
                    }
                }
                out
            } else emptyList()
        },
        key1 = allMessages,
        key2 = activeCanonicalKey,
        key3 = activeCleanName
    ) {
        value = if (allMessages.size <= 3000) {
            val out = ArrayList<DecryptedSmsMessage>()
            for (i in allMessages.size - 1 downTo 0) {
                val m = allMessages[i]
                if (matchesActivePersonMessage(m, activeCanonicalKey, activeCleanName, contactMap)) {
                    out.add(m)
                }
            }
            out
        } else {
            withContext(Dispatchers.Default) {
                val out = ArrayList<DecryptedSmsMessage>()
                for (i in allMessages.size - 1 downTo 0) {
                    val m = allMessages[i]
                    if (matchesActivePersonMessage(m, activeCanonicalKey, activeCleanName, contactMap)) {
                        out.add(m)
                    }
                }
                out
            }
        }
    }

    val inboxThreads = remember(allThreads) { allThreads.filter { !it.isBlockedAdThread } }
    val adThreads = remember(allThreads) { allThreads.filter { it.isBlockedAdThread } }

    val filteredDirectoryThreads = remember(inboxThreads, adThreads, showAdsInDirectory, searchQuery) {
        val base = if (showAdsInDirectory) adThreads else inboxThreads
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) base
        else base.filter { t ->
            t.displayName.lowercase().contains(q) ||
                t.phoneNumber.lowercase().contains(q) ||
                t.latestMessage.body.lowercase().contains(q)
        }
    }

    val avatarColors = remember(activeCanonicalKey) {
        avatarGradientForKey(activeCanonicalKey.ifBlank { activeSenderName.ifBlank { "MengiTex" } })
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. BUBBLE TOP HEADER BAR (Shows Active Sender OR All Conversations Header + Quick Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (!showAllConversationsDirectory) {
                        IconButton(
                            onClick = { showAllConversationsDirectory = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("bubble_all_chats_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isSw) "Mazungumzo Yote" else "All Conversations",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(avatarColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = extractInitials(
                                    if (showAllConversationsDirectory) "MT" else activeSenderName.ifBlank { activePhoneNumber.ifBlank { "MT" } }
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))))
                                .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MarkChatUnread,
                                contentDescription = "Pop Bubble",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (showAllConversationsDirectory) {
                                    if (isSw) "Mazungumzo Yote (${allThreads.size})" else "All Conversations (${allThreads.size})"
                                } else {
                                    activeSenderName.ifBlank { activePhoneNumber.ifBlank { "MengiTex" } }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "BUBBLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        val subLabel = if (showAllConversationsDirectory) {
                            if (isSw) "Gusa mazungumzo yoyote kusoma na kujibu" else "Tap any conversation to read & reply"
                        } else if (activePhoneNumber.isNotBlank() && !activePhoneNumber.equals(activeSenderName, ignoreCase = true)) {
                            "$activePhoneNumber • ${activeThreadMessages.size} SMS"
                        } else {
                            "${activePhoneNumber.ifBlank { activeSenderName }} • ${activeThreadMessages.size} SMS"
                        }
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle button between Active Chat and All Conversations List
                    Surface(
                        onClick = { showAllConversationsDirectory = !showAllConversationsDirectory },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showAllConversationsDirectory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.testTag("bubble_toggle_all_conversations_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = if (showAllConversationsDirectory) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSw) "Zote (${allThreads.size})" else "All (${allThreads.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showAllConversationsDirectory) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { onOpenFullApp(activeSenderName, activePhoneNumber) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = if (isSw) "Fungua App" else "Expand App",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (isSw) "Funga" else "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. ALWAYS-VISIBLE HORIZONTAL CONVERSATION SWITCHER STRIP (Switch to any conversation in 1 tap!)
            if (allThreads.isNotEmpty() && !showAllConversationsDirectory) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bubble_conversations_strip")
                ) {
                    items(
                        items = allThreads,
                        key = { "${it.isBlockedAdThread}_${it.canonicalKey}" }
                    ) { thread ->
                        val isSelected = thread.canonicalKey == activeCanonicalKey
                        val chipColors = remember(thread.canonicalKey) { avatarGradientForKey(thread.canonicalKey) }
                        Surface(
                            onClick = {
                                activeSenderName = thread.displayName
                                activePhoneNumber = thread.phoneNumber
                                showAllConversationsDirectory = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(chipColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = extractInitials(thread.displayName),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = thread.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected || thread.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 105.dp)
                                        )
                                        if (thread.isBlockedAdThread) {
                                            Text(
                                                text = "ADS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (!thread.phoneNumber.equals(thread.displayName, ignoreCase = true)) {
                                        Text(
                                            text = thread.phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 105.dp)
                                        )
                                    }
                                }
                                if (thread.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = thread.unreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. MAIN BUBBLE CONTENT: EITHER ALL CONVERSATIONS DIRECTORY OR ACTIVE CHAT TIMELINE
            if (showAllConversationsDirectory) {
                // Search Bar + Inbox / Ads Filter inside the Bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (isSw) "Tafuta jina, namba au SMS..." else "Search name, number or SMS...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { showAdsInDirectory = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showAdsInDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (isSw) "Zote (${inboxThreads.size})" else "All (${inboxThreads.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!showAdsInDirectory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        onClick = { showAdsInDirectory = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showAdsInDirectory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Ads (${adThreads.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (showAdsInDirectory) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                // Full Scrollable List of ALL Conversations inside the Bubble
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("bubble_all_conversations_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredDirectoryThreads,
                        key = { "${it.isBlockedAdThread}_${it.canonicalKey}" }
                    ) { thread ->
                        val rowAvatarColors = remember(thread.canonicalKey) { avatarGradientForKey(thread.canonicalKey) }
                        val timeFormatted = remember(thread.latestMessage.timestamp) {
                            SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(thread.latestMessage.timestamp))
                        }
                        Surface(
                            onClick = {
                                activeSenderName = thread.displayName
                                activePhoneNumber = thread.phoneNumber
                                showAllConversationsDirectory = false
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (thread.unreadCount > 0) 1.25.dp else 1.dp,
                                    color = if (thread.unreadCount > 0) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(rowAvatarColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = extractInitials(thread.displayName),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = thread.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (thread.isBlockedAdThread) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = "ADS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = timeFormatted,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!thread.phoneNumber.equals(thread.displayName, ignoreCase = true)) {
                                        Text(
                                            text = thread.phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = thread.latestMessage.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (thread.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = thread.unreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Full Active Conversation Message Timeline inside the Bubble
                val listState = rememberLazyListState()
                LaunchedEffect(activeThreadMessages.size, activeCanonicalKey) {
                    if (activeThreadMessages.isNotEmpty()) {
                        listState.scrollToItem(activeThreadMessages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("bubble_active_conversation_timeline"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (activeThreadMessages.isEmpty() && initialLastMessage.isNotBlank()) {
                        item(key = "fallback_initial_message") {
                            BubbleMessageRow(
                                senderHeader = if (!activePhoneNumber.equals(activeSenderName, ignoreCase = true) && activePhoneNumber.isNotBlank()) {
                                    "$activeSenderName • $activePhoneNumber"
                                } else {
                                    activeSenderName.ifBlank { activePhoneNumber }
                                },
                                body = initialLastMessage,
                                timestamp = System.currentTimeMillis(),
                                isOutgoing = false,
                                status = MessageStatus.RECEIVED,
                                isSw = isSw,
                                onCallPhone = callBubblePhone,
                                onCopyText = copyBubbleText,
                                onLongPress = {}
                            )
                        }
                    } else {
                        items(
                            items = activeThreadMessages,
                            key = { it.id }
                        ) { msg ->
                            val cleanName = ContactsLoader.cleanSenderIdentifier(msg.contactName)
                            val cleanPhone = ContactsLoader.cleanSenderIdentifier(msg.phoneNumber)
                            val senderHeader = when {
                                cleanName.isNotEmpty() && cleanPhone.isNotEmpty() && !cleanName.equals(cleanPhone, ignoreCase = true) ->
                                    "$cleanName • $cleanPhone"
                                cleanName.isNotEmpty() -> cleanName
                                cleanPhone.isNotEmpty() -> cleanPhone
                                else -> activeSenderName.ifBlank { activePhoneNumber }
                            }
                            BubbleMessageRow(
                                senderHeader = senderHeader,
                                body = msg.body,
                                timestamp = msg.timestamp,
                                isOutgoing = msg.direction == MessageDirection.OUTGOING,
                                status = msg.status,
                                isSw = isSw,
                                onCallPhone = callBubblePhone,
                                onCopyText = copyBubbleText,
                                onLongPress = { heldBubbleMessage = msg }
                            )
                        }
                    }
                }

                // Real-time Keystore AES-256-GCM AAD-v3 status strip above Android Phone Bubble SMS Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Keystore AES-256 • $bubbleKeystoreFingerprint",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    statusText?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                // Compact Bottom Authentic Android Phone SMS Composer Bar inside Bubble Pop ("Hii bar ya kutuma sms mpya weka iwe ndogo")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .border(
                                1.dp,
                                if (replyText.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "SMS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
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
                                if (replyText.isEmpty()) {
                                    Text(
                                        text = if (isSw) {
                                            "Andika SMS kwa ${activeSenderName.ifBlank { activePhoneNumber }}..."
                                        } else {
                                            "Text message to ${activeSenderName.ifBlank { activePhoneNumber }}..."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    maxLines = 4,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.5.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    val targetDest = activePhoneNumber.ifBlank { activeSenderName }
                    val canSend = replyText.isNotBlank() && targetDest.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF7C3AED))
                                    )
                                } else {
                                    SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                                }
                            )
                            .clickable(enabled = canSend) {
                                val text = replyText.trim()
                                if (text.isEmpty() || targetDest.isBlank()) return@clickable
                                scope.launch(Dispatchers.IO) {
                                    val parts = SmsDispatchManager.calculateSmsParts(context, text, selectedSimSubId)
                                    val msgId = repository.insertOutgoingOrScheduled(
                                        contactName = activeSenderName.ifBlank { targetDest },
                                        phoneNumber = targetDest,
                                        plainBody = text,
                                        status = MessageStatus.SENDING,
                                        statusNote = if (isSw) "Imejibiwa kupitia Bubble" else "Replied via Bubble",
                                        simSlotLabel = "SIM",
                                        simSubId = selectedSimSubId,
                                        partCount = parts
                                    )
                                    val result = SmsDispatchManager.sendMultipartSms(
                                        context = context.applicationContext,
                                        messageId = msgId,
                                        phoneNumber = targetDest,
                                        body = text,
                                        simSubId = selectedSimSubId
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            replyText = ""
                                            statusText = if (isSw) "Ujumbe umetumwa! ✓" else "Reply sent! ✓"
                                        } else {
                                            SmsDispatchManager.openSystemSmsFallback(context, listOf(targetDest), text)
                                            statusText = if (isSw) "Imefunguliwa kwenye mfumo wa SMS" else "Opened in system SMS"
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isSw) "Tuma" else "Send",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }

    // Floating Hold Pop View inside Bubble Pop Chat ("Izo option kwenye chat Niki hold ziwe kwa pop view")
    val heldMsg = heldBubbleMessage
    if (heldMsg != null) {
        val detectedPhones = remember(heldMsg.body) {
            ContactsLoader.extractPhoneNumbersFromText(heldMsg.body)
        }
        Dialog(
            onDismissRequest = { heldBubbleMessage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                shadowElevation = 22.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSw) "Chaguo za Ujumbe (Pop View)" else "Message Options (Pop View)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { heldBubbleMessage = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = heldMsg.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    if (detectedPhones.isNotEmpty()) {
                        detectedPhones.forEach { phone ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        onClick = {
                                            heldBubbleMessage = null
                                            callBubblePhone(phone)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Text(
                                            text = if (isSw) "Piga" else "Call",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            copyBubbleText(phone)
                                            heldBubbleMessage = null
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyBubbleText(heldMsg.body)
                                heldBubbleMessage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                        OutlinedButton(
                            onClick = {
                                val snippet = heldMsg.body.replace("\n", " ").take(45)
                                replyText = "❝ $snippet ❞\n$replyText"
                                heldBubbleMessage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSw) "Jibu" else "Reply")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val bodyToShare = heldMsg.body
                                heldBubbleMessage = null
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, bodyToShare)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share SMS"))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        Button(
                            onClick = {
                                val msgToDelete = heldMsg
                                heldBubbleMessage = null
                                scope.launch(Dispatchers.IO) {
                                    repository.moveMessageToTrash(msgToDelete)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSw) "Futa" else "Delete")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleMessageRow(
    senderHeader: String,
    body: String,
    timestamp: Long,
    isOutgoing: Boolean,
    status: String,
    isSw: Boolean,
    onCallPhone: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onLongPress: () -> Unit
) {
    val timeText = remember(timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
    val detectedPhones = remember(body) {
        ContactsLoader.extractPhoneNumbersFromText(body)
    }
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (isOutgoing) {
            Box(
                modifier = Modifier
                    .widthIn(max = 285.dp)
                    .clip(bubbleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1E40AF), Color(0xFF2563EB)))
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    if (detectedPhones.isNotEmpty()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
                        detectedPhones.forEach { num ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = num,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        onClick = { onCallPhone(num) },
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Text(
                                            text = if (isSw) "Piga" else "Call",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { onCopyText(num) },
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val (tickIcon, tickTint, deliveryReportLabel) = when (status) {
                        MessageStatus.DELIVERED -> Triple(
                            Icons.Filled.DoneAll,
                            Color(0xFF6EE7B7),
                            if (isSw) "Imefika" else "Delivered"
                        )
                        MessageStatus.FAILED -> Triple(
                            Icons.Filled.ErrorOutline,
                            Color(0xFFFCA5A5),
                            if (isSw) "Imeshindwa" else "Failed"
                        )
                        MessageStatus.SENDING -> Triple(
                            Icons.Filled.Schedule,
                            Color(0xFFFDE68A),
                            if (isSw) "Inatuma..." else "Sending..."
                        )
                        else -> Triple(
                            Icons.Filled.Done,
                            Color(0xE6FFFFFF),
                            if (isSw) "Imetumwa" else "Sent"
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xCCFFFFFF)
                        )
                        Text(
                            text = "• $deliveryReportLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tickTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = deliveryReportLabel,
                            tint = tickTint,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = bubbleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .widthIn(max = 285.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                        bubbleShape
                    )
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (senderHeader.isNotBlank()) {
                            Text(
                                text = senderHeader,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (status == MessageStatus.BLOCKED_AD) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "ADS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (detectedPhones.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        detectedPhones.forEach { num ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = num,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        onClick = { onCallPhone(num) },
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Text(
                                            text = if (isSw) "Piga" else "Call",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { onCopyText(num) },
                                        shape = RoundedCornerShape(7.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
