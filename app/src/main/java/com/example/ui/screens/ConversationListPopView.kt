package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Floating Pop View shown when holding (long-pressing) any conversation in the conversation list.
 * Provides instant Mark All, Unmark All, Delete, Mark Read, Mute/Unmute, Select More, Call, Copy,
 * View Profile, and Allow Sender actions in a calm matte Off-White floating pop card.
 */
@Composable
fun ConversationListPopViewDialog(
    anchorThread: ConversationThread,
    selectedCount: Int,
    totalCount: Int,
    isAllSelected: Boolean,
    isAnchorMuted: Boolean,
    isSw: Boolean,
    canCallAnchor: Boolean,
    onMarkAll: () -> Unit,
    onUnmarkAll: () -> Unit,
    onSelectMoreOnList: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMarkSelectedRead: () -> Unit,
    onToggleMuteSelected: () -> Unit,
    onOpenAnchorChat: () -> Unit,
    onViewAnchorProfile: () -> Unit,
    onCallAnchor: () -> Unit,
    onCopyAnchorNumber: () -> Unit,
    onAllowBlockedAd: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val activeCount = selectedCount.coerceAtLeast(1)
    val isSingle = activeCount == 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("conversation_list_pop_view")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Pop View Header
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
                        Text(
                            text = if (isSingle) {
                                anchorThread.displayName.trim().take(2).uppercase().ifEmpty { "SM" }
                            } else {
                                activeCount.toString()
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSingle) {
                                anchorThread.displayName
                            } else {
                                if (isSw) "Mazungumzo $activeCount Yamechaguliwa"
                                else "$activeCount Conversations Selected"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isSingle) {
                                val msgWord = if (isSw) "ujumbe" else "msgs"
                                "${anchorThread.phoneNumber} • ${anchorThread.messageCount} $msgWord"
                            } else {
                                if (isSw) "Chagua hatua unayotaka kufanya mara moja"
                                else "Choose an action for the selected threads"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "$activeCount/$totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("pop_view_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (isSw) "Funga" else "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Quick Selection Pills: Mark All • Unmark All • Select More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PopQuickPillButton(
                        icon = Icons.Filled.SelectAll,
                        label = if (isSw) "Chagua Zote" else "Mark All",
                        highlighted = isAllSelected,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onMarkAll,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pop_view_mark_all_button")
                    )
                    PopQuickPillButton(
                        icon = Icons.Filled.Deselect,
                        label = if (isSw) "Ondoa Zote" else "Unmark All",
                        highlighted = false,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = onUnmarkAll,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pop_view_unmark_all_button")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // 3. Primary Actions inside Pop View
                PopActionRowItem(
                    icon = Icons.Filled.Delete,
                    title = if (isSingle) {
                        if (isSw) "Futa Mazungumzo (Hamishia Taka)" else "Delete Conversation"
                    } else {
                        if (isSw) "Futa Mazungumzo $activeCount" else "Delete $activeCount Conversations"
                    },
                    subtitle = if (isSw) "Ujumbe utahamishiwa kwenye Taka mara moja" else "Moves messages to Trash immediately",
                    tint = MaterialTheme.colorScheme.error,
                    containerBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f),
                    onClick = onDeleteSelected,
                    modifier = Modifier.testTag("pop_view_delete_button")
                )

                PopActionRowItem(
                    icon = Icons.Filled.Checklist,
                    title = if (isSw) "Chagua Mazungumzo Zaidi" else "Select More Conversations",
                    subtitle = if (isSw) "Gusa mazungumzo mengine kwenye orodha kuyawekea alama" else "Tap multiple threads on the list to batch edit",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onSelectMoreOnList,
                    modifier = Modifier.testTag("pop_view_select_more_button")
                )

                PopActionRowItem(
                    icon = Icons.Filled.MarkChatRead,
                    title = if (isSingle) {
                        if (isSw) "Weka Imesomwa" else "Mark as Read"
                    } else {
                        if (isSw) "Weka Zote Zimesomwa ($activeCount)" else "Mark Selected as Read ($activeCount)"
                    },
                    subtitle = if (isSw) "Ondoa alama za ujumbe ambao haujasomwa" else "Clear unread badges right away",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onMarkSelectedRead,
                    modifier = Modifier.testTag("pop_view_mark_read_button")
                )

                PopActionRowItem(
                    icon = if (isAnchorMuted) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                    title = if (isAnchorMuted) {
                        if (isSw) "Washa Arifa Tena" else "Unmute Notifications"
                    } else {
                        if (isSw) "Nyamazisha Arifa" else "Mute Notifications"
                    },
                    subtitle = if (isAnchorMuted) {
                        if (isSw) "Ruhusu mlio na pop-up kwa mtumaji huyu" else "Re-enable sound and pop-up alerts"
                    } else {
                        if (isSw) "Zima mlio na pop-up kwa mazungumzo haya" else "Silence sound and pop-up alerts"
                    },
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = onToggleMuteSelected,
                    modifier = Modifier.testTag("pop_view_mute_button")
                )

                if (isSingle && onAllowBlockedAd != null) {
                    PopActionRowItem(
                        icon = Icons.Filled.VerifiedUser,
                        title = if (isSw) "Ruhusu Mtumaji (Siyo Tangazo)" else "Allow Sender (Not an Ad)",
                        subtitle = if (isSw) "Rejesha ujumbe wake wote kwenye Kikasha" else "Restore messages back to your Inbox",
                        tint = Color(0xFF15803D),
                        onClick = onAllowBlockedAd,
                        modifier = Modifier.testTag("pop_view_allow_ad_button")
                    )
                }

                // Single-conversation quick extras
                if (isSingle) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PopMiniActionChip(
                            icon = Icons.Filled.Chat,
                            label = if (isSw) "Fungua" else "Open",
                            onClick = onOpenAnchorChat,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pop_view_open_chat_button")
                        )
                        if (canCallAnchor) {
                            PopMiniActionChip(
                                icon = Icons.Filled.Call,
                                label = if (isSw) "Piga" else "Call",
                                onClick = onCallAnchor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pop_view_call_button")
                            )
                        }
                        PopMiniActionChip(
                            icon = Icons.Filled.ContentCopy,
                            label = if (isSw) "Nakili" else "Copy",
                            onClick = onCopyAnchorNumber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pop_view_copy_button")
                        )
                        PopMiniActionChip(
                            icon = Icons.Filled.AccountCircle,
                            label = if (isSw) "Wasifu" else "Profile",
                            onClick = onViewAnchorProfile,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pop_view_profile_button")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Pop Action Bar displayed while multi-selecting conversations on the list.
 */
@Composable
fun ConversationMultiSelectFloatingBar(
    selectedCount: Int,
    totalCount: Int,
    isAllSelected: Boolean,
    isSw: Boolean,
    onMarkAllToggle: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onOpenMorePopView: () -> Unit,
    onExitSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 5.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("conversation_multiselect_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onExitSelection,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("multiselect_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = if (isSw) "Funga" else "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (isSw) "$selectedCount / $totalCount Zimechaguliwa"
                    else "$selectedCount / $totalCount Selected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onMarkAllToggle)
                        .testTag("multiselect_mark_all_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAllSelected) Icons.Filled.Deselect else Icons.Filled.SelectAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isAllSelected) {
                                if (isSw) "Ondoa Zote" else "Unmark All"
                            } else {
                                if (isSw) "Chagua Zote" else "Mark All"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PopQuickPillButton(
                    icon = Icons.Filled.MarkChatRead,
                    label = if (isSw) "Imesomwa" else "Read",
                    highlighted = false,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onMarkRead,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("multiselect_read_button")
                )
                PopQuickPillButton(
                    icon = Icons.Filled.Delete,
                    label = if (isSw) "Futa ($selectedCount)" else "Delete ($selectedCount)",
                    highlighted = true,
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("multiselect_delete_button")
                )
                PopQuickPillButton(
                    icon = Icons.Filled.MoreHoriz,
                    label = if (isSw) "Zaidi" else "More",
                    highlighted = false,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenMorePopView,
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("multiselect_more_button")
                )
            }
        }
    }
}

@Composable
private fun PopQuickPillButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    val bg = if (highlighted) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val border = if (highlighted) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Surface(
        shape = shape,
        color = bg,
        modifier = modifier
            .border(1.dp, border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PopActionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    containerBg: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = containerBg,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PopMiniActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
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
