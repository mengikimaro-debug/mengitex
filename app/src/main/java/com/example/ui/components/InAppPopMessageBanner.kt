package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedSmsMessage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.extractInitials
import com.example.ui.theme.android17AvatarColors
import kotlinx.coroutines.delay

/**
 * 2026 In-App Floating Pop Message Overlay:
 * - Shows a clear Pop Message Icon badge + Sender Monogram Avatar so the user immediately knows
 *   WHICH incoming message popped in (always preserving the sender's exact name, even for ads/services).
 * - Includes an expandable inline "Pop Reply" box so the user can reply right from the pop banner.
 * - Tapping the banner opens the conversation directly.
 */
@Composable
fun InAppPopMessageOverlay(
    message: DecryptedSmsMessage?,
    hidePreviewForPrivacy: Boolean,
    autoDismissSeconds: Int = 8,
    onOpenConversation: (contactName: String, phoneNumber: String) -> Unit,
    onSendPopReply: (contactName: String, phoneNumber: String, replyBody: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var showReplyInput by remember(message?.id) { mutableStateOf(false) }
    var replyText by remember(message?.id) { mutableStateOf("") }

    // Auto-dismiss after configured duration unless user is actively typing a Pop Reply
    LaunchedEffect(message?.id, showReplyInput, autoDismissSeconds) {
        if (message != null && !showReplyInput) {
            delay(autoDismissSeconds.coerceIn(3, 20) * 1000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val msg = message ?: return@AnimatedVisibility
        val displaySender = msg.contactName.ifBlank { msg.phoneNumber }
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val (avatarBg, avatarFg) = remember(msg.phoneNumber, isDark) {
            android17AvatarColors(msg.phoneNumber, isDark)
        }
        val shape = RoundedCornerShape(24.dp)

        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .testTag("in_app_pop_message_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onOpenConversation(displaySender, msg.phoneNumber)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = extractInitials(displaySender),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = avatarFg
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MarkChatUnread,
                                contentDescription = s.popBadgeLabel,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displaySender,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = s.popBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (hidePreviewForPrivacy || msg.isSecretBurnAfterRead) {
                                s.secretCardMask
                            } else {
                                msg.body
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pop_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = s.btnClose,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Action Row: Pop Reply + Open Chat
                if (!showReplyInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showReplyInput = true }
                                .testTag("pop_reply_expand_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = s.popReplyBtn,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s.popReplyBtn,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    onOpenConversation(displaySender, msg.phoneNumber)
                                }
                                .testTag("pop_open_chat_button")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.popOpenChat,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Expandable Inline Pop Reply Composer
                AnimatedVisibility(
                    visible = showReplyInput,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (replyText.isEmpty()) {
                                    Text(
                                        text = s.popReplyPlaceholder,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BasicTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    maxLines = 3,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pop_reply_text_input")
                                )
                            }
                        }

                        val canSend = replyText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                )
                                .clickable(enabled = canSend) {
                                    val outgoing = replyText.trim()
                                    replyText = ""
                                    showReplyInput = false
                                    onSendPopReply(displaySender, msg.phoneNumber, outgoing)
                                    onDismiss()
                                }
                                .testTag("pop_reply_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = s.popReplySend,
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
