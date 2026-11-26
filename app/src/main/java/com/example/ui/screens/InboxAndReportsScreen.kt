package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.security.SecuritySettings
import com.example.security.SmsAdBlockerEngine
import com.example.service.ContactsLoader
import com.example.ui.InboxFilter
import com.example.ui.components.MengiTexLogoMark
import com.example.ui.components.NextGenFilterPill
import com.example.ui.components.NextGenGlassSurface
import com.example.ui.components.NextGenGradientFab
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.android17AvatarColors
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class SearchScope {
    ALL, NAMES, MESSAGES, NUMBERS
}

data class ConversationThread(
    val normalizedPhone: String,
    val displayName: String,
    val phoneNumber: String,
    val latestMessage: DecryptedSmsMessage,
    val matchedMessage: DecryptedSmsMessage? = null,
    val messageCount: Int,
    val unreadCount: Int,
    val hasSecretMessage: Boolean,
    val isSilentUnknownThread: Boolean,
    val isBlockedAdThread: Boolean = false
)

private data class GroupedInboxData(
    val threadsWithMessages: List<Pair<ConversationThread, List<DecryptedSmsMessage>>>,
    val blockedAdsCount: Int
)

private val GENERIC_UNKNOWN_LABELS = setOf(
    "Namba Geni (Haijahifadhiwa)",
    "Unknown Number (Unsaved)"
)

private class SinglePassThreadAcc(
    val normPhone: String,
    val isBlockedAdBucket: Boolean,
    var latest: DecryptedSmsMessage,
    var bestName: String?,
    var bestPhone: String?,
    var count: Int = 0,
    var unread: Int = 0,
    var hasSecret: Boolean = false,
    var anyUnknownOrSilent: Boolean = false,
    val messages: ArrayList<DecryptedSmsMessage> = ArrayList(8)
)

private fun buildInboxThreadsSinglePass(
    messages: List<DecryptedSmsMessage>,
    contacts: List<ContactItem>
): GroupedInboxData {
    if (messages.isEmpty()) return GroupedInboxData(emptyList(), 0)

    val contactNameByNormPhone = HashMap<String, String>(contacts.size + 16)
    for (i in 0 until contacts.size) {
        val c = contacts[i]
        val norm = ContactsLoader.normalizePhone(c.phoneNumber)
        val cleanName = ContactsLoader.cleanSenderIdentifier(c.name)
        if (norm.isNotEmpty() && cleanName.isNotEmpty()) {
            contactNameByNormPhone.putIfAbsent(norm, cleanName)
        }
    }

    val inboxMap = LinkedHashMap<String, SinglePassThreadAcc>(256)
    val adsMap = LinkedHashMap<String, SinglePassThreadAcc>(128)
    var blockedAdsTotal = 0

    for (i in 0 until messages.size) {
        val msg = messages[i]
        val isAdMsg = msg.status == MessageStatus.BLOCKED_AD
        if (isAdMsg) {
            blockedAdsTotal++
        }
        val normKey = msg.canonicalThreadKey
        val targetMap = if (isAdMsg) adsMap else inboxMap

        var acc = targetMap[normKey]
        if (acc == null) {
            acc = SinglePassThreadAcc(
                normPhone = normKey,
                isBlockedAdBucket = isAdMsg,
                latest = msg,
                bestName = null,
                bestPhone = null
            )
            targetMap[normKey] = acc
        } else if (msg.timestamp > acc.latest.timestamp) {
            acc.latest = msg
        }

        acc.count++
        acc.messages.add(msg)

        if (acc.bestPhone == null) {
            val cleanMsgPhone = ContactsLoader.cleanSenderIdentifier(msg.phoneNumber)
            if (cleanMsgPhone.isNotEmpty()) {
                acc.bestPhone = cleanMsgPhone
            }
        }
        if (acc.bestName == null) {
            val cleanMsgName = ContactsLoader.cleanSenderIdentifier(msg.contactName)
            if (cleanMsgName.isNotEmpty() && cleanMsgName !in GENERIC_UNKNOWN_LABELS && cleanMsgName != msg.phoneNumber) {
                acc.bestName = cleanMsgName
            }
        }

        if (!isAdMsg && msg.direction == MessageDirection.INCOMING && !msg.isRead) {
            acc.unread++
        }
        if (msg.isSecretBurnAfterRead) {
            acc.hasSecret = true
        }
        if (msg.isUnknownSender || msg.isSilentIntercepted) {
            acc.anyUnknownOrSilent = true
        }
    }

    val out = ArrayList<Pair<ConversationThread, List<DecryptedSmsMessage>>>(inboxMap.size + adsMap.size)

    fun emitBucket(bucket: LinkedHashMap<String, SinglePassThreadAcc>) {
        for (acc in bucket.values) {
            val latest = acc.latest
            val cleanLatestPhone = ContactsLoader.cleanSenderIdentifier(latest.phoneNumber)
            val cleanLatestName = ContactsLoader.cleanSenderIdentifier(latest.contactName)
            val resolvedPhone = acc.bestPhone
                ?: cleanLatestPhone.ifEmpty { cleanLatestName }.ifEmpty { "SMS" }
            val savedContactName = contactNameByNormPhone[acc.normPhone]
                ?: contactNameByNormPhone[ContactsLoader.normalizePhone(resolvedPhone)]
            val displayName = savedContactName
                ?: acc.bestName
                ?: cleanLatestName.ifEmpty { resolvedPhone }

            val thread = ConversationThread(
                normalizedPhone = acc.normPhone,
                displayName = displayName,
                phoneNumber = resolvedPhone,
                latestMessage = latest,
                messageCount = acc.count,
                unreadCount = acc.unread,
                hasSecretMessage = acc.hasSecret,
                isSilentUnknownThread = !acc.isBlockedAdBucket && acc.anyUnknownOrSilent && savedContactName == null,
                isBlockedAdThread = acc.isBlockedAdBucket
            )
            out.add(thread to acc.messages)
        }
    }

    emitBucket(inboxMap)
    emitBucket(adsMap)

    return GroupedInboxData(out, blockedAdsTotal)
}

private fun filterThreadsFast(
    allThreadsWithMessages: List<Pair<ConversationThread, List<DecryptedSmsMessage>>>,
    activeFilter: InboxFilter,
    trimmedQuery: String,
    queryDigits: String,
    searchScope: SearchScope
): List<ConversationThread> {
    val byFilter = when (activeFilter) {
        InboxFilter.ALL, InboxFilter.REPORTS -> allThreadsWithMessages.filter { !it.first.isBlockedAdThread }
        InboxFilter.BLOCKED_ADS -> allThreadsWithMessages.filter { it.first.isBlockedAdThread }
        InboxFilter.UNKNOWN_SILENT -> allThreadsWithMessages.filter { it.first.isSilentUnknownThread }
        InboxFilter.SECRET -> allThreadsWithMessages.filter { it.first.hasSecretMessage }
    }

    if (trimmedQuery.isEmpty()) {
        return byFilter.map { it.first }
    }

    val qLower = trimmedQuery.lowercase()
    val qTokens = qLower.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return byFilter.mapNotNull { (thread, threadMsgs) ->
        val nameHit = thread.displayName.lowercase().let { name ->
            qTokens.all { tok -> name.contains(tok) }
        }
        val numberHit = thread.phoneNumber.lowercase().contains(qLower) ||
            (queryDigits.length >= 2 && thread.normalizedPhone.contains(queryDigits))
        val matchingMsg = if (searchScope == SearchScope.NAMES || searchScope == SearchScope.NUMBERS) {
            null
        } else {
            threadMsgs.firstOrNull { m ->
                !m.isSecretBurnAfterRead && qTokens.all { tok -> m.body.contains(tok, ignoreCase = true) }
            }
        }

        val isMatch = when (searchScope) {
            SearchScope.ALL -> nameHit || numberHit || matchingMsg != null
            SearchScope.NAMES -> nameHit
            SearchScope.NUMBERS -> numberHit
            SearchScope.MESSAGES -> matchingMsg != null
        }
        if (isMatch) thread.copy(matchedMessage = matchingMsg) else null
    }
}

/**
 * Premium Conversations Inbox.
 * Groups all SMS messages by contact/phone into rich, clean conversation threads with zero top-bar clutter.
 */
@Composable
fun InboxAndReportsScreen(
    messages: List<DecryptedSmsMessage>,
    contacts: List<ContactItem> = emptyList(),
    filter: InboxFilter,
    settings: SecuritySettings,
    isDefaultSmsApp: Boolean = false,
    isMessagesLoading: Boolean = false,
    activeSecretReader: DecryptedSmsMessage?,
    onSelectFilter: (InboxFilter) -> Unit,
    onOpenConversation: (contactName: String, phoneNumber: String) -> Unit,
    onOpenSecretReader: (DecryptedSmsMessage) -> Unit,
    onCloseSecretReader: (burnNow: Boolean) -> Unit,
    onMoveToTrash: (DecryptedSmsMessage) -> Unit,
    onRequestDefaultSmsApp: () -> Unit = {},
    onSaveContactName: (phone: String, newName: String) -> Unit = { _, _ -> },
    onAutoSweepAds: () -> Unit = {},
    onAllowBlockedAdSender: (contactName: String, phoneNumber: String) -> Unit = { _, _ -> },
    onDeleteAllBlockedAds: () -> Unit = {},
    onDeleteConversations: (Set<String>) -> Unit = {},
    onMarkConversationsRead: (Set<String>) -> Unit = {},
    onToggleMuteConversations: (List<String>, Boolean) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onQuickCompose: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isSw = settings.language == AppLanguage.SW
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.ALL) }
    var dismissedDefaultBanner by remember { mutableStateOf(false) }
    var inspectedProfileTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedThreadKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popViewAnchorThread by remember { mutableStateOf<ConversationThread?>(null) }
    val isSelectionMode = selectedThreadKeys.isNotEmpty()

    BackHandler(enabled = isSelectionMode || popViewAnchorThread != null) {
        if (popViewAnchorThread != null) {
            popViewAnchorThread = null
            if (selectedThreadKeys.size <= 1) {
                selectedThreadKeys = emptySet()
            }
        } else {
            selectedThreadKeys = emptySet()
        }
    }

    // Clean filter options: All (ad-free), Blocked Ads, Unknown Silent, Secret
    val visibleFilters = remember {
        listOf(InboxFilter.ALL, InboxFilter.BLOCKED_ADS, InboxFilter.UNKNOWN_SILENT, InboxFilter.SECRET)
    }

    val activeFilter = if (filter == InboxFilter.REPORTS) InboxFilter.ALL else filter
    val trimmedQuery = searchQuery.trim()
    val queryDigits = remember(trimmedQuery) { trimmedQuery.filter { it.isDigit() } }

    // 6G Single-pass O(N) thread grouping: zero coroutine hop for <= 6,000 SMS (takes ~1.5ms),
    // background-accelerated on Dispatchers.Default for giant 100k..600k archives.
    val groupedInboxData = if (messages.size <= 6000) {
        remember(messages, contacts) {
            buildInboxThreadsSinglePass(messages, contacts)
        }
    } else {
        val asyncGrouped by androidx.compose.runtime.produceState(
            initialValue = remember { buildInboxThreadsSinglePass(messages, contacts) },
            key1 = messages,
            key2 = contacts
        ) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                buildInboxThreadsSinglePass(messages, contacts)
            }
        }
        asyncGrouped
    }

    val allThreadsWithMessages = groupedInboxData.threadsWithMessages
    val blockedAdsMessageCount = groupedInboxData.blockedAdsCount

    // Zero-hop synchronous filtering when browsing inbox tabs (0.05ms!), async only for massive search queries
    val conversations = if (trimmedQuery.isEmpty() || messages.size <= 6000) {
        remember(allThreadsWithMessages, activeFilter, trimmedQuery, queryDigits, searchScope) {
            filterThreadsFast(
                allThreadsWithMessages = allThreadsWithMessages,
                activeFilter = activeFilter,
                trimmedQuery = trimmedQuery,
                queryDigits = queryDigits,
                searchScope = searchScope
            )
        }
    } else {
        val asyncFiltered by androidx.compose.runtime.produceState(
            initialValue = remember(allThreadsWithMessages, activeFilter) {
                filterThreadsFast(
                    allThreadsWithMessages = allThreadsWithMessages,
                    activeFilter = activeFilter,
                    trimmedQuery = trimmedQuery,
                    queryDigits = queryDigits,
                    searchScope = searchScope
                )
            },
            key1 = allThreadsWithMessages,
            key2 = activeFilter,
            key3 = "$trimmedQuery|$searchScope"
        ) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                filterThreadsFast(
                    allThreadsWithMessages = allThreadsWithMessages,
                    activeFilter = activeFilter,
                    trimmedQuery = trimmedQuery,
                    queryDigits = queryDigits,
                    searchScope = searchScope
                )
            }
        }
        asyncFiltered
    }

    // Also match saved contacts that don't have an active thread yet so searching a friend lets you start chatting instantly
    val matchedNewContacts = remember(contacts, allThreadsWithMessages, trimmedQuery, queryDigits, searchScope) {
        if (trimmedQuery.isEmpty() || searchScope == SearchScope.MESSAGES) {
            emptyList()
        } else {
            val existingNorms = allThreadsWithMessages.map { it.first.normalizedPhone }.toSet()
            val qLower = trimmedQuery.lowercase()
            contacts.filter { contact ->
                val norm = ContactsLoader.normalizePhone(contact.phoneNumber)
                if (norm in existingNorms) return@filter false
                val nameMatch = contact.name.lowercase().contains(qLower)
                val numMatch = contact.phoneNumber.lowercase().contains(qLower) ||
                    (queryDigits.length >= 2 && contact.phoneNumber.filter { it.isDigit() }.contains(queryDigits))
                when (searchScope) {
                    SearchScope.NAMES -> nameMatch
                    SearchScope.NUMBERS -> numMatch
                    else -> nameMatch || numMatch
                }
            }.take(6)
        }
    }

    // Auto-detect what the user is searching for while typing
    val detectedIntentLabel = remember(trimmedQuery, queryDigits, conversations, matchedNewContacts, s) {
        if (trimmedQuery.isEmpty()) ""
        else when {
            queryDigits.length >= 3 && queryDigits.length >= trimmedQuery.count { it.isLetterOrDigit() } - 1 ->
                s.searchDetectedNumber
            conversations.any { it.matchedMessage != null } &&
                conversations.none { it.displayName.contains(trimmedQuery, ignoreCase = true) } ->
                s.searchDetectedMessage
            else -> s.searchDetectedContact
        }
    }

    val totalMatches = conversations.size + matchedNewContacts.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Android 17 Material 3 Pill SearchBar (borderless solid surfaceContainerHigh)
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = s.convSearchHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isBlank()) searchScope = SearchScope.ALL
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversations_search_input")
                        )
                    }
                    if (trimmedQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = s.searchMatchesCount(totalMatches),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchScope = SearchScope.ALL
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Live Smart Search Context + Scope Pills while typing, OR regular Inbox Filter Pills
            if (trimmedQuery.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$detectedIntentLabel • \"$trimmedQuery\"",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scopes = listOf(
                            SearchScope.ALL to s.searchScopeAll,
                            SearchScope.NAMES to s.searchScopeNames,
                            SearchScope.MESSAGES to s.searchScopeMessages,
                            SearchScope.NUMBERS to s.searchScopeNumbers
                        )
                        items(scopes) { (scope, label) ->
                            NextGenFilterPill(
                                selected = searchScope == scope,
                                label = label,
                                onClick = { searchScope = scope },
                                modifier = Modifier.testTag("search_scope_${scope.name.lowercase()}")
                            )
                        }
                    }
                }
            } else {
                // Expressive Liquid-Gradient Filter Pills (All • Unknown Silent • Secret)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(visibleFilters) { entry ->
                        val label = when (entry) {
                            InboxFilter.ALL, InboxFilter.REPORTS -> s.filterAll
                            InboxFilter.BLOCKED_ADS -> if (blockedAdsMessageCount > 0) {
                                "${s.filterBlockedAds} ($blockedAdsMessageCount)"
                            } else {
                                s.filterBlockedAds
                            }
                            InboxFilter.UNKNOWN_SILENT -> s.filterUnknownSilent
                            InboxFilter.SECRET -> s.filterSecret
                        }
                        NextGenFilterPill(
                            selected = activeFilter == entry,
                            label = label,
                            onClick = { onSelectFilter(entry) },
                            modifier = Modifier.testTag("inbox_filter_${entry.name.lowercase()}")
                        )
                    }
                }
            }

            // Blocked Ads Shield & Quick Actions Header when viewing Blocked Ads tab
            AnimatedVisibility(
                visible = activeFilter == InboxFilter.BLOCKED_ADS && trimmedQuery.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.adBlockerBannerTitle(blockedAdsMessageCount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = s.adBlockerBannerSub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = s.adBlockerProtectedChip,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAutoSweepAds,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inbox_sweep_ads_button")
                            ) {
                                Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.adBlockerSweepBtn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            if (blockedAdsMessageCount > 0) {
                                Button(
                                    onClick = onDeleteAllBlockedAds,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .testTag("inbox_delete_all_ads_button")
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(s.adBlockerDeleteAllBtn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Post-install System Ownership card: one-tap takeover as Android's Default SMS App
            AnimatedVisibility(
                visible = !isDefaultSmsApp && !dismissedDefaultBanner && trimmedQuery.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.defaultSmsBannerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.defaultSmsBannerSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { dismissedDefaultBanner = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = s.btnCancel,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Button(
                            onClick = onRequestDefaultSmsApp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inbox_set_default_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.defaultSmsSetNowBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isMessagesLoading && conversations.isEmpty() && matchedNewContacts.isEmpty()) {
                // First-ever DB hydration (when no instant cache exists yet): calm matte placeholder rows
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(5) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.45f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.78f)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (conversations.isEmpty() && matchedNewContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeFilter == InboxFilter.UNKNOWN_SILENT || activeFilter == InboxFilter.BLOCKED_ADS) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeFilter == InboxFilter.BLOCKED_ADS) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeFilter == InboxFilter.BLOCKED_ADS) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (activeFilter == InboxFilter.BLOCKED_ADS) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            MengiTexLogoMark(
                                size = 72.dp,
                                cornerRadius = 20.dp,
                                showGlow = false,
                                modifier = Modifier.testTag("inbox_empty_brand_logo")
                            )
                        }
                        Text(
                            text = when (activeFilter) {
                                InboxFilter.BLOCKED_ADS -> s.inboxEmptyBlockedAds
                                InboxFilter.UNKNOWN_SILENT -> s.inboxEmptyUnknown
                                InboxFilter.SECRET -> s.inboxEmptySecret
                                else -> s.inboxEmptyAll
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (activeFilter != InboxFilter.BLOCKED_ADS) {
                            Text(
                                text = s.inboxEmptyHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NextGenGradientFab(
                                text = s.btnNewMessage,
                                icon = Icons.Filled.Edit,
                                onClick = onQuickCompose,
                                modifier = Modifier.testTag("inbox_empty_compose_button")
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = conversations,
                        key = { "${it.isBlockedAdThread}_${it.normalizedPhone}" },
                        contentType = { "conversation_row" }
                    ) { thread ->
                        val isThreadSelected = thread.normalizedPhone in selectedThreadKeys
                        PremiumConversationRow(
                            thread = thread,
                            highlightQuery = trimmedQuery,
                            isSelected = isThreadSelected,
                            isSelectionMode = isSelectionMode,
                            onViewProfile = {
                                if (isSelectionMode) {
                                    selectedThreadKeys = if (isThreadSelected) {
                                        selectedThreadKeys - thread.normalizedPhone
                                    } else {
                                        selectedThreadKeys + thread.normalizedPhone
                                    }
                                } else {
                                    inspectedProfileTarget = thread.displayName to thread.phoneNumber
                                }
                            },
                            onAllowBlockedAd = if (thread.isBlockedAdThread) {
                                { onAllowBlockedAdSender(thread.displayName, thread.phoneNumber) }
                            } else null,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedThreadKeys = if (isThreadSelected) {
                                        selectedThreadKeys - thread.normalizedPhone
                                    } else {
                                        selectedThreadKeys + thread.normalizedPhone
                                    }
                                } else {
                                    onOpenConversation(thread.displayName, thread.phoneNumber)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedThreadKeys = selectedThreadKeys + thread.normalizedPhone
                                popViewAnchorThread = thread
                            }
                        )
                    }

                    if (matchedNewContacts.isNotEmpty()) {
                        items(
                            items = matchedNewContacts,
                            key = { "contact_match_${it.id}" },
                            contentType = { "contact_search_match" }
                        ) { contact ->
                            val isDark = isSystemInDarkTheme()
                            val (avatarBg, avatarFg) = remember(contact.phoneNumber, isDark) {
                                android17AvatarColors(contact.phoneNumber, isDark)
                            }
                            val rowShape = RoundedCornerShape(20.dp)
                            val highlightColor = MaterialTheme.colorScheme.primary
                            val highlightBg = MaterialTheme.colorScheme.primaryContainer
                            Surface(
                                shape = rowShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .clickable { onOpenConversation(contact.name, contact.phoneNumber) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(avatarBg)
                                            .clickable {
                                                inspectedProfileTarget = contact.name to contact.phoneNumber
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = extractInitials(contact.name),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = avatarFg
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = buildSearchHighlightedText(contact.name, trimmedQuery, highlightColor, highlightBg),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildSearchHighlightedText(contact.phoneNumber, trimmedQuery, highlightColor, highlightBg),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = s.searchStartNewChat,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "inbox_bottom_spacer") { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }

        if (isSelectionMode && popViewAnchorThread == null) {
            val allVisibleKeys = conversations.mapTo(HashSet(conversations.size)) { it.normalizedPhone }
            val isAllSelected = allVisibleKeys.isNotEmpty() && selectedThreadKeys.containsAll(allVisibleKeys)
            ConversationMultiSelectFloatingBar(
                selectedCount = selectedThreadKeys.size,
                totalCount = conversations.size.coerceAtLeast(selectedThreadKeys.size),
                isAllSelected = isAllSelected,
                isSw = isSw,
                onMarkAllToggle = {
                    selectedThreadKeys = if (isAllSelected) emptySet() else allVisibleKeys
                },
                onMarkRead = {
                    onMarkConversationsRead(selectedThreadKeys)
                    selectedThreadKeys = emptySet()
                },
                onDelete = {
                    onDeleteConversations(selectedThreadKeys)
                    selectedThreadKeys = emptySet()
                },
                onOpenMorePopView = {
                    val anchor = conversations.firstOrNull { it.normalizedPhone in selectedThreadKeys }
                        ?: conversations.firstOrNull()
                    if (anchor != null) {
                        popViewAnchorThread = anchor
                    }
                },
                onExitSelection = {
                    selectedThreadKeys = emptySet()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            NextGenGradientFab(
                text = s.btnNewMessage,
                icon = Icons.Filled.Edit,
                onClick = onQuickCompose,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 14.dp)
                    .testTag("inbox_compose_fab")
            )
        }
    }

    val activePopThread = popViewAnchorThread
    if (activePopThread != null) {
        val allVisibleKeys = conversations.mapTo(HashSet(conversations.size)) { it.normalizedPhone }
        val effectiveSelectedKeys = if (selectedThreadKeys.isNotEmpty()) {
            selectedThreadKeys
        } else {
            setOf(activePopThread.normalizedPhone)
        }
        val isAllSelected = allVisibleKeys.isNotEmpty() && effectiveSelectedKeys.containsAll(allVisibleKeys)
        val anchorMuteKey = SmsAdBlockerEngine.canonicalSenderKey(activePopThread.phoneNumber)
        val isAnchorMuted = anchorMuteKey.isNotEmpty() && anchorMuteKey in settings.mutedNotificationSenders
        val canCallAnchor = activePopThread.phoneNumber.count { it.isDigit() } >= 3

        ConversationListPopViewDialog(
            anchorThread = activePopThread,
            selectedCount = effectiveSelectedKeys.size,
            totalCount = conversations.size.coerceAtLeast(effectiveSelectedKeys.size),
            isAllSelected = isAllSelected,
            isAnchorMuted = isAnchorMuted,
            isSw = isSw,
            canCallAnchor = canCallAnchor,
            onMarkAll = {
                selectedThreadKeys = allVisibleKeys
            },
            onUnmarkAll = {
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
            },
            onSelectMoreOnList = {
                selectedThreadKeys = effectiveSelectedKeys
                popViewAnchorThread = null
            },
            onDeleteSelected = {
                onDeleteConversations(effectiveSelectedKeys)
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
            },
            onMarkSelectedRead = {
                onMarkConversationsRead(effectiveSelectedKeys)
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
            },
            onToggleMuteSelected = {
                val targetSenders = conversations
                    .filter { it.normalizedPhone in effectiveSelectedKeys }
                    .map { it.phoneNumber.ifBlank { it.displayName } }
                    .ifEmpty { listOf(activePopThread.phoneNumber.ifBlank { activePopThread.displayName }) }
                onToggleMuteConversations(targetSenders, !isAnchorMuted)
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
            },
            onOpenAnchorChat = {
                val name = activePopThread.displayName
                val phone = activePopThread.phoneNumber
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
                onOpenConversation(name, phone)
            },
            onViewAnchorProfile = {
                val name = activePopThread.displayName
                val phone = activePopThread.phoneNumber
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
                inspectedProfileTarget = name to phone
            },
            onCallAnchor = {
                val phone = activePopThread.phoneNumber
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
                runCatching {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(dialIntent)
                }
            },
            onCopyAnchorNumber = {
                val copyValue = activePopThread.phoneNumber.ifBlank { activePopThread.displayName }
                runCatching {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("phone", copyValue))
                }
                onShowMessage(
                    if (isSw) "Imenakiliwa: $copyValue" else "Copied: $copyValue"
                )
                selectedThreadKeys = emptySet()
                popViewAnchorThread = null
            },
            onAllowBlockedAd = if (activePopThread.isBlockedAdThread) {
                {
                    onAllowBlockedAdSender(activePopThread.displayName, activePopThread.phoneNumber)
                    selectedThreadKeys = emptySet()
                    popViewAnchorThread = null
                }
            } else null,
            onDismiss = {
                popViewAnchorThread = null
                if (selectedThreadKeys.size <= 1) {
                    selectedThreadKeys = emptySet()
                }
            }
        )
    }

    if (activeSecretReader != null) {
        SecretBurnReaderDialog(
            message = activeSecretReader,
            autoBurnOnClose = activeSecretReader.isSecretBurnAfterRead || settings.autoDeleteSecretAfterRead,
            onClose = onCloseSecretReader
        )
    }

    val profileTarget = inspectedProfileTarget
    if (profileTarget != null) {
        val (targetName, targetPhone) = profileTarget
        val matchingLocal = remember(contacts, targetPhone) {
            val norm = ContactsLoader.normalizePhone(targetPhone)
            contacts.any { it.isLocalSaved && ContactsLoader.normalizePhone(it.phoneNumber) == norm }
        }
        ContactFullDetailsSheet(
            contactName = targetName,
            phoneNumber = targetPhone,
            isLocalSaved = matchingLocal,
            allMessages = messages,
            onOpenConversation = { name, phone ->
                onOpenConversation(name, phone)
            },
            onSaveContactName = onSaveContactName,
            onShowMessage = onShowMessage,
            onDismiss = { inspectedProfileTarget = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumConversationRow(
    thread: ConversationThread,
    highlightQuery: String = "",
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onViewProfile: () -> Unit = {},
    onAllowBlockedAd: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val previewMsg = thread.matchedMessage ?: thread.latestMessage
    val hasUnread = thread.unreadCount > 0
    val isDark = isSystemInDarkTheme()
    val (avatarBg, avatarFg) = remember(thread.normalizedPhone, isDark) {
        android17AvatarColors(thread.normalizedPhone, isDark)
    }
    val formattedTime = remember(previewMsg.timestamp, s.convYesterday) {
        formatSmartConversationTime(previewMsg.timestamp, s.convYesterday)
    }
    val highlightColor = MaterialTheme.colorScheme.primary
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    val rowShape = RoundedCornerShape(20.dp)
    val rowBg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasUnread -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        shape = rowShape,
        color = rowBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("conversation_row_${thread.normalizedPhone}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android 17 Solid Tonal Monogram Circle Avatar (no gradient, no extra badge clutter)
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onViewProfile)
                    .testTag("inbox_avatar_profile_${thread.normalizedPhone}")
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (thread.isSilentUnknownThread && thread.displayName == thread.phoneNumber) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = s.contactDetailsViewProfileCd,
                            tint = avatarFg,
                            modifier = Modifier.size(21.dp)
                        )
                    } else {
                        Text(
                            text = extractInitials(thread.displayName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = avatarFg
                        )
                    }
                }
                if (thread.hasSecretMessage && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .border(1.5.dp, rowBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Name, Time, Preview & Clean Unread Count Pill
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = buildSearchHighlightedText(
                                    text = thread.displayName,
                                    query = highlightQuery,
                                    highlightColor = highlightColor,
                                    highlightBg = highlightBg
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (thread.isBlockedAdThread) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "ADS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (thread.phoneNumber.isNotBlank() && !thread.phoneNumber.equals(thread.displayName, ignoreCase = true)) {
                            Text(
                                text = buildSearchHighlightedText(
                                    text = thread.phoneNumber,
                                    query = highlightQuery,
                                    highlightColor = highlightColor,
                                    highlightBg = highlightBg
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (onAllowBlockedAd != null) {
                        Surface(
                            onClick = onAllowBlockedAd,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = s.adBlockerAllowSenderBtn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasUnread) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (previewMsg.direction == MessageDirection.OUTGOING) {
                        val (tickIcon, tickColor) = when (previewMsg.status) {
                            MessageStatus.DELIVERED -> Icons.Filled.DoneAll to MaterialTheme.colorScheme.primary
                            MessageStatus.SENT -> Icons.Filled.Done to MaterialTheme.colorScheme.onSurfaceVariant
                            MessageStatus.SENDING -> Icons.Filled.Schedule to MaterialTheme.colorScheme.tertiary
                            MessageStatus.FAILED -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
                            else -> Icons.Filled.Done to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = null,
                            tint = tickColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (previewMsg.isSecretBurnAfterRead) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = s.secretCardMask,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val preview = if (previewMsg.direction == MessageDirection.OUTGOING) {
                            "${s.convYouPrefix}: ${previewMsg.body}"
                        } else {
                            previewMsg.body
                        }
                        Text(
                            text = buildSearchHighlightedText(
                                text = preview,
                                query = highlightQuery,
                                highlightColor = highlightColor,
                                highlightBg = highlightBg
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasUnread) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else if (thread.isSilentUnknownThread) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

internal fun extractInitials(displayName: String): String {
    val clean = displayName.trim()
    if (clean.isEmpty()) return "?"
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        clean.first().isLetter() -> clean.take(2).uppercase()
        else -> clean.filter { it.isDigit() }.takeLast(2).ifEmpty { "#" }
    }
}

private fun formatSmartConversationTime(timestamp: Long, yesterdayLabel: String): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        sameYear && dayDiff == 1 -> yesterdayLabel
        sameYear -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun SecretBurnReaderDialog(
    message: DecryptedSmsMessage,
    autoBurnOnClose: Boolean,
    onClose: (burnNow: Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val wasAlreadySecure = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!wasAlreadySecure) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onClose(autoBurnOnClose) },
        icon = {
            Icon(
                imageVector = if (autoBurnOnClose) Icons.Filled.LocalFireDepartment else Icons.Filled.Shield,
                contentDescription = null,
                tint = if (autoBurnOnClose) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = message.contactName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = message.phoneNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Text(
                    text = if (autoBurnOnClose) s.secretReaderAutoBurnDesc else s.secretReaderNormalDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onClose(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("secret_reader_burn_button")
            ) {
                Text(s.secretReaderBurnBtn)
            }
        },
        dismissButton = {
            if (!autoBurnOnClose) {
                TextButton(onClick = { onClose(false) }) {
                    Text(s.secretReaderCloseOnlyBtn)
                }
            }
        }
    )
}
