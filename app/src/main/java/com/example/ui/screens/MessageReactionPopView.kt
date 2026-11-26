package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.data.MessageReactionEntry
import com.example.data.MessageStatus
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.MengiTexNotificationManager
import com.example.ui.i18n.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥")
private val EXTRA_REACTION_EMOJIS = listOf(
    "👏", "🎉", "💯", "😍", "😭", "🤔",
    "😎", "🥳", "🫶", "✨", "👌", "💪",
    "🤝", "🙌", "✅", "❌", "💔", "😡",
    "👀", "🫡", "🥹", "🤭", "☕", "🚀"
)

private val popDetailDateFmt = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.getDefault())
}

/**
 * Floating Pop View opened when tapping or holding any SMS message inside Conversation Chat.
 * Combines:
 * 1. Emoji Reactions + direct 1-tap Emoji SMS Reply
 * 2. Selectable message preview + detected OTP code & phone numbers
 * 3. Full SMS Actions (Quote Reply, Copy, Forward, Share, Partial Select, Edit, Schedule, Multi-select, Delete)
 * 4. Complete SMS Details ("Taarifa Zote Kamili" — every field, SIM, encoding, timestamps, status & Keystore)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionPopView(
    message: DecryptedSmsMessage,
    threadContactName: String,
    currentReaction: MessageReactionEntry?,
    latestIncomingReplyTimestamp: Long,
    onReactEmoji: (String) -> Unit,
    onRemoveReaction: () -> Unit,
    onSendEmojiReplySms: (emoji: String, payload: String) -> Unit,
    onInsertEmojiReplyInComposer: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onForward: () -> Unit,
    onMoveToTrash: () -> Unit,
    onQuoteReply: () -> Unit,
    onSelectPartialText: () -> Unit,
    onEditInComposer: () -> Unit,
    onSelectMultiple: () -> Unit,
    onScheduleMessage: () -> Unit,
    onCopySpecificText: (String) -> Unit,
    onCallPhoneNumber: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val isSecret = message.isSecretBurnAfterRead
    val isIncoming = message.direction == MessageDirection.INCOMING

    val scaleAnim = remember { Animatable(0.92f) }
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(message.id) {
        scaleAnim.snapTo(0.92f)
        alphaAnim.snapTo(0f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)
        )
    }
    LaunchedEffect(message.id) {
        alphaAnim.animateTo(1f, animationSpec = tween(140))
    }

    var showMoreEmojis by remember { mutableStateOf(false) }
    var chosenReplyEmoji by remember(message.id, currentReaction?.emoji) {
        mutableStateOf(currentReaction?.emoji ?: "❤️")
    }

    val detectedPhones = remember(message.body, isSecret) {
        if (isSecret) emptyList() else ContactsLoader.extractPhoneNumbersFromText(message.body)
    }
    val detectedOtp = remember(message.body, isSecret) {
        if (isSecret) null else MengiTexNotificationManager.extractOtpCode(message.body)
    }
    val metrics = remember(message.body) { computeSmsMetrics(message.body) }
    val fingerprint = remember(message.id, message.phoneNumber) {
        VaultCryptoManager.computeKeystoreFingerprint("${message.phoneNumber}_${message.id}")
    }
    val statusLabel = resolveStatusDisplayLabel(message, latestIncomingReplyTimestamp, s)
    val fullDateStr = remember(message.timestamp) {
        (popDetailDateFmt.get() ?: SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.getDefault()))
            .format(Date(message.timestamp))
    }
    val relativeTimeStr = remember(message.timestamp, isSw) {
        formatRelativeAge(message.timestamp, isSw)
    }
    val scheduledDateStr = remember(message.scheduledFor) {
        message.scheduledFor?.let {
            (popDetailDateFmt.get() ?: SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.getDefault()))
                .format(Date(it))
        }
    }
    val reactedAtStr = remember(currentReaction?.reactedAtMillis) {
        currentReaction?.reactedAtMillis?.takeIf { it > 0L }?.let {
            (popDetailDateFmt.get() ?: SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.getDefault()))
                .format(Date(it))
        }
    }

    val displaySenderName = remember(message.contactName, threadContactName, message.phoneNumber) {
        ContactsLoader.cleanSenderIdentifier(message.contactName)
            .ifBlank { ContactsLoader.cleanSenderIdentifier(threadContactName) }
            .ifBlank { message.phoneNumber }
    }

    val cleanStatusNote = remember(message.statusNote) {
        message.statusNote.trim()
    }

    val quotedSnippet = remember(message.body, isSecret) {
        if (isSecret) "" else message.body.replace('\n', ' ').trim().take(48)
    }

    val buildAllDetailsCopyText = {
        buildString {
            appendLine("=== MENGITEX SMS DETAILS ===")
            appendLine("${if (isSw) "Mtumaji/Mpokeaji" else "Contact"}: $displaySenderName (${message.phoneNumber})")
            appendLine("${if (isSw) "Maelekezo" else "Direction"}: ${if (isIncoming) "Incoming (Received)" else "Outgoing (Sent)"}")
            appendLine("${if (isSw) "Hali" else "Status"}: $statusLabel")
            appendLine("${if (isSw) "Tarehe & Saa" else "Timestamp"}: $fullDateStr ($relativeTimeStr)")
            if (scheduledDateStr != null) {
                appendLine("${if (isSw) "Muda Uliopangwa" else "Scheduled For"}: $scheduledDateStr")
            }
            appendLine("${if (isSw) "SIM Card" else "SIM Slot"}: ${message.simSlotLabel} (SubID: ${message.simSubId})")
            appendLine("${if (isSw) "Vipimo" else "Metrics"}: ${metrics.charCount} chars • ${metrics.wordCount} words • ${metrics.lineCount} lines • ${message.partCount} parts (${metrics.encodingLabel})")
            appendLine("${if (isSw) "Imesomwa" else "Read Status"}: ${if (message.isRead) "Read" else "Unread"}")
            appendLine("${if (isSw) "Namba Ngeni" else "Unknown Sender"}: ${if (message.isUnknownSender) "Yes" else "No"}")
            appendLine("${if (isSw) "Kimya (Silent)" else "Silent Intercepted"}: ${if (message.isSilentIntercepted) "Yes" else "No"}")
            appendLine("${if (isSw) "Secret Burn" else "Secret Burn"}: ${if (isSecret) "Enabled" else "No"}")
            if (currentReaction != null) {
                appendLine("Reaction: ${currentReaction.emoji}")
            }
            if (cleanStatusNote.isNotBlank()) {
                appendLine("${if (isSw) "Ripoti" else "Status Note"}: $cleanStatusNote")
            }
            appendLine("Record ID: #${message.id} • Key: ${message.canonicalThreadKey}")
            appendLine("Keystore AES-256-GCM: $fingerprint")
            if (!isSecret) {
                appendLine("---")
                appendLine(message.body)
            }
        }.trim()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Floating Emoji Reaction Pop Bar (docked above main card like Android Messages / WhatsApp Pop View)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_reaction_emoji_bar")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSw) "Jibu kwa Emoji (Reaction)" else "React & Reply with Emoji",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentReaction != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onRemoveReaction() }
                                    .testTag("reaction_clear_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = currentReaction.emoji, fontSize = 13.sp)
                                    Text(
                                        text = if (isSw) "Ondoa" else "Remove",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QUICK_REACTION_EMOJIS.forEach { emoji ->
                            val isChosen = currentReaction?.emoji == emoji
                            EmojiReactionChip(
                                emoji = emoji,
                                selected = isChosen,
                                onClick = {
                                    chosenReplyEmoji = emoji
                                    onReactEmoji(emoji)
                                },
                                modifier = Modifier.testTag("reaction_emoji_$emoji")
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showMoreEmojis) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { showMoreEmojis = !showMoreEmojis }
                                .testTag("reaction_more_emojis_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showMoreEmojis) Icons.Filled.ExpandLess else Icons.Filled.Add,
                                contentDescription = if (isSw) "Emoji Zaidi" else "More Emojis",
                                tint = if (showMoreEmojis) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showMoreEmojis,
                        enter = expandVertically(tween(150)) + fadeIn(tween(150)),
                        exit = shrinkVertically(tween(120))
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EXTRA_REACTION_EMOJIS.forEach { emoji ->
                                val isChosen = currentReaction?.emoji == emoji
                                EmojiReactionChip(
                                    emoji = emoji,
                                    selected = isChosen,
                                    onClick = {
                                        chosenReplyEmoji = emoji
                                        onReactEmoji(emoji)
                                    },
                                    modifier = Modifier.testTag("reaction_emoji_$emoji")
                                )
                            }
                        }
                    }

                    // Instant 1-tap Send Emoji as SMS Reply buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val replySmsPayload = if (quotedSnippet.isNotEmpty()) {
                            "$chosenReplyEmoji \"$quotedSnippet\""
                        } else {
                            chosenReplyEmoji
                        }
                        Button(
                            onClick = { onSendEmojiReplySms(chosenReplyEmoji, replySmsPayload) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .heightIn(min = 44.dp)
                                .testTag("reaction_send_quoted_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isSw) "Tuma Jibu $chosenReplyEmoji" else "Send $chosenReplyEmoji Reply",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = { onSendEmojiReplySms(chosenReplyEmoji, chosenReplyEmoji) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("reaction_send_only_emoji_button")
                        ) {
                            Text(
                                text = if (isSw) "$chosenReplyEmoji Pekee" else "$chosenReplyEmoji Only",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = { onInsertEmojiReplyInComposer(replySmsPayload) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("reaction_compose_emoji_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = if (isSw) "Hariri na Emoji" else "Edit with Emoji",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Main Scrollable Pop Card: Preview + Actions + ALL Complete SMS Details
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .testTag("message_hold_pop_view")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Direction Badge + Close button
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isIncoming) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isIncoming) {
                                        if (isSw) "Uliopokelewa" else "Incoming SMS"
                                    } else {
                                        if (isSw) "Uliotumwa" else "Outgoing SMS"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncoming) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = displaySenderName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pop_message_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = s.btnClose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Selectable Message Preview Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSecret) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = s.secretCardMask,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = message.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (detectedOtp != null) {
                                Surface(
                                    onClick = { onCopySpecificText(detectedOtp) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.testTag("pop_copy_otp_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isSw) "Nakili Kodi (OTP): $detectedOtp" else "Copy OTP Code: $detectedOtp",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick 4-Tile Action Grid: Reply • Copy • Forward • Share
                    if (!isSecret) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PopQuickTile(
                                icon = Icons.AutoMirrored.Filled.Reply,
                                label = if (isSw) "Jibu" else "Reply",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = onQuoteReply,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hold_action_quote_reply")
                            )
                            PopQuickTile(
                                icon = Icons.Filled.ContentCopy,
                                label = if (isSw) "Copy" else "Copy",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = onCopy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hold_action_copy")
                            )
                            PopQuickTile(
                                icon = Icons.AutoMirrored.Filled.Forward,
                                label = if (isSw) "Forward" else "Forward",
                                accent = MaterialTheme.colorScheme.secondary,
                                onClick = onForward,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hold_action_forward")
                            )
                            PopQuickTile(
                                icon = Icons.Filled.Share,
                                label = s.convActionShare,
                                accent = MaterialTheme.colorScheme.tertiary,
                                onClick = onShare,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hold_action_share")
                            )
                        }
                    }

                    // Detected Phone Numbers Quick Action Card
                    if (detectedPhones.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isSw) "Namba Kwenye Ujumbe Huu" else "Numbers in Message",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                detectedPhones.forEach { num ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = num,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                onClick = { onCallPhoneNumber(num) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondary
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Call,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = if (isSw) "Piga" else "Call",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondary
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = { onCopySpecificText(num) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ContentCopy,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Copy",
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
                    }

                    // Complete Message Details ("Taarifa Zote Kamili za SMS") — EVERY field displayed cleanly
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pop_full_message_details_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = if (isSw) "Taarifa Zote za Ujumbe" else "Complete Message Details",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    onClick = { onCopySpecificText(buildAllDetailsCopyText()) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.testTag("pop_copy_all_details_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (isSw) "Nakili Zote" else "Copy All",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            DetailSpecRow(
                                label = if (isSw) "Hali ya Ujumbe" else "Message Status",
                                value = statusLabel,
                                valueColor = MaterialTheme.colorScheme.primary
                            )
                            DetailSpecRow(
                                label = if (isSw) "Maelekezo (Type)" else "Direction",
                                value = if (isIncoming) {
                                    if (isSw) "Ujumbe Uliopokelewa (Inbox)" else "Incoming (Received SMS)"
                                } else {
                                    if (isSw) "Ujumbe Uliotumwa (Outbox)" else "Outgoing (Sent SMS)"
                                }
                            )
                            DetailSpecRow(
                                label = if (isIncoming) {
                                    if (isSw) "Mtumaji (Sender)" else "Sender"
                                } else {
                                    if (isSw) "Mpokeaji (Recipient)" else "Recipient"
                                },
                                value = if (displaySenderName == message.phoneNumber) {
                                    message.phoneNumber
                                } else {
                                    "$displaySenderName • ${message.phoneNumber}"
                                }
                            )
                            DetailSpecRow(
                                label = if (isSw) "Thread Key" else "Canonical Key",
                                value = message.canonicalThreadKey
                            )
                            DetailSpecRow(
                                label = if (isSw) "Tarehe & Saa Kamili" else "Exact Date & Time",
                                value = "$fullDateStr ($relativeTimeStr)"
                            )
                            if (scheduledDateStr != null) {
                                DetailSpecRow(
                                    label = if (isSw) "Muda wa Ratiba" else "Scheduled For",
                                    value = scheduledDateStr
                                )
                            }
                            DetailSpecRow(
                                label = if (isSw) "SIM Iliyotumika" else "SIM Card & Sub ID",
                                value = if (message.simSubId >= 0) {
                                    "${message.simSlotLabel} (Sub ID: ${message.simSubId})"
                                } else {
                                    message.simSlotLabel
                                }
                            )
                            DetailSpecRow(
                                label = if (isSw) "Urefu & Encoding" else "Size & Encoding",
                                value = if (isSw) {
                                    "${metrics.charCount} herufi • ${metrics.wordCount} maneno • ${metrics.lineCount} mistari • ${message.partCount} sehemu (${metrics.encodingLabel})"
                                } else {
                                    "${metrics.charCount} chars • ${metrics.wordCount} words • ${metrics.lineCount} lines • ${message.partCount} part(s) (${metrics.encodingLabel})"
                                }
                            )
                            DetailSpecRow(
                                label = if (isSw) "Hali ya Kusomwa" else "Read State",
                                value = if (message.isRead) {
                                    if (isSw) "Imesomwa ✓" else "Read ✓"
                                } else {
                                    if (isSw) "Haijasomwa (Unread)" else "Unread"
                                }
                            )
                            DetailSpecRow(
                                label = if (isSw) "Mtumaji Mgeni / Kimya" else "Unknown / Silent",
                                value = buildString {
                                    append(
                                        if (message.isUnknownSender) {
                                            if (isSw) "Namba Ngeni" else "Unknown Sender"
                                        } else {
                                            if (isSw) "Contact Iliyohifadhiwa" else "Saved Contact"
                                        }
                                    )
                                    if (message.isSilentIntercepted) {
                                        append(if (isSw) " • Kimya (Silent)" else " • Silent Intercepted")
                                    }
                                    if (message.status == MessageStatus.BLOCKED_AD) {
                                        append(if (isSw) " • Tangazo Lililozuiwa" else " • Ad Blocked")
                                    }
                                }
                            )
                            DetailSpecRow(
                                label = if (isSw) "Secret Burn-After-Read" else "Secret Burn-After-Read",
                                value = if (isSecret) {
                                    if (isSw) "Imewashwa 🔥 (Futa baada ya kusoma)" else "Active 🔥 (Burn after reading)"
                                } else {
                                    if (isSw) "Hapana (Ujumbe wa kudumu)" else "Off (Permanent vault storage)"
                                }
                            )
                            if (currentReaction != null) {
                                DetailSpecRow(
                                    label = "Reaction",
                                    value = if (reactedAtStr != null) "${currentReaction.emoji} • $reactedAtStr" else currentReaction.emoji,
                                    valueColor = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (cleanStatusNote.isNotBlank()) {
                                DetailSpecRow(
                                    label = if (isSw) "Ripoti ya Mtandao" else "Carrier / Delivery Note",
                                    value = cleanStatusNote
                                )
                            }
                            DetailSpecRow(
                                label = if (isSw) "Kitambulisho & Keystore" else "Record ID & Keystore",
                                value = "#${message.id} • AES-256-GCM AAD-v3 • $fingerprint"
                            )
                        }
                    }

                    // More Actions List (Partial Copy, Edit, Schedule, Multi-Select, Trash)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (!isSecret) {
                                PopDetailActionRow(
                                    icon = Icons.Filled.ContentCopy,
                                    title = if (isSw) "Chagua & Nakili Sehemu ya Text / Namba" else "Select & Copy Part of Text / Numbers",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    testTag = "hold_action_select_partial_text",
                                    onClick = onSelectPartialText
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                PopDetailActionRow(
                                    icon = Icons.Filled.Edit,
                                    title = if (isSw) "Hariri Kwenye Uwanja wa Kuandika" else "Edit / Reuse Text in Composer",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    testTag = "hold_action_edit_composer",
                                    onClick = onEditInComposer
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                PopDetailActionRow(
                                    icon = Icons.Filled.ScheduleSend,
                                    title = if (isSw) "Panga Muda wa Kutuma (Schedule Text)" else "Schedule This Text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    testTag = "hold_action_schedule",
                                    onClick = onScheduleMessage
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            PopDetailActionRow(
                                icon = Icons.Filled.SelectAll,
                                title = if (isSw) "Chagua Jumbe Nyingi (Select Multiple)" else "Select Multiple Messages",
                                tint = MaterialTheme.colorScheme.primary,
                                testTag = "hold_action_select_multiple",
                                onClick = onSelectMultiple
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            PopDetailActionRow(
                                icon = Icons.Filled.DeleteOutline,
                                title = if (isSw) "Futa Ujumbe (Hamishia Taka)" else s.convActionTrash,
                                tint = MaterialTheme.colorScheme.error,
                                isDestructive = true,
                                testTag = "hold_action_trash",
                                onClick = onMoveToTrash
                            )
                        }
                    }

                    // Keystore Footer Verification Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Keystore AES-256-GCM • $fingerprint",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiReactionChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 19.sp)
        }
    }
}

@Composable
private fun PopQuickTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailSpecRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PopDetailActionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    isDestructive: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

internal data class SmsTextMetrics(
    val charCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val isGsm7: Boolean,
    val encodingLabel: String
)

private const val GSM7_BASIC_AND_EXT_CHARS =
    "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà" +
        "^{}\\[~]|€"

internal fun computeSmsMetrics(body: String): SmsTextMetrics {
    val chars = body.length
    val words = if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    val lines = if (body.isEmpty()) 0 else body.count { it == '\n' } + 1
    var allGsm7 = true
    for (i in 0 until body.length) {
        if (GSM7_BASIC_AND_EXT_CHARS.indexOf(body[i]) < 0) {
            allGsm7 = false
            break
        }
    }
    val enc = if (allGsm7) "GSM-7 (160/part)" else "Unicode UCS-2 (70/part)"
    return SmsTextMetrics(
        charCount = chars,
        wordCount = words,
        lineCount = lines,
        isGsm7 = allGsm7,
        encodingLabel = enc
    )
}

private fun resolveStatusDisplayLabel(
    message: DecryptedSmsMessage,
    latestIncomingReplyTimestamp: Long,
    s: com.example.ui.i18n.AppStrings
): String {
    return when {
        message.status == MessageStatus.DELIVERED -> "${s.statusDelivered} ✓✓"
        message.status == MessageStatus.SENT &&
            message.direction == MessageDirection.OUTGOING &&
            latestIncomingReplyTimestamp > message.timestamp -> "${s.statusDelivered} ✓✓"
        message.status == MessageStatus.SENT -> "${s.statusSent} ✓"
        message.status == MessageStatus.SENDING -> s.statusSending
        message.status == MessageStatus.FAILED -> s.statusFailed
        message.status == MessageStatus.BLOCKED_AD -> s.filterBlockedAds
        message.status == MessageStatus.SCHEDULED -> s.drawerSchedule
        message.status == MessageStatus.RECEIVED ->
            if (message.isSilentIntercepted) s.statusSilentUnknown else s.statusReceived
        else -> message.status
    }
}

private fun formatRelativeAge(timestampMillis: Long, isSw: Boolean): String {
    val diffMs = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    return when {
        mins < 1L -> if (isSw) "Sasa hivi" else "Just now"
        mins < 60L -> if (isSw) "Dakika $mins zilizopita" else "${mins}m ago"
        hours < 24L -> if (isSw) "Saa $hours zilizopita" else "${hours}h ago"
        else -> if (isSw) "Siku $days zilizopita" else "${days}d ago"
    }
}
