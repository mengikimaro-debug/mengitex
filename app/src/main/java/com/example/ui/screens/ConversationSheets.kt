package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.DetectedSimCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.android17AvatarColors

/**
 * 2026 Next-Gen Contact Profile Bottom Sheet opened by tapping the person's header/avatar
 * inside ConversationScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileModalSheet(
    contactName: String,
    phoneNumber: String,
    totalMessages: Int,
    secretMessages: Int,
    activeSim: DetectedSimCard?,
    onSearchInChat: () -> Unit,
    onScheduleSms: () -> Unit,
    onSaveContactName: (String) -> Unit,
    onCopiedSensitiveText: () -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val isSw = s.btnCancel == "Ghairi"
    val normPhone = remember(phoneNumber) {
        ContactsLoader.normalizePhone(phoneNumber).ifEmpty { phoneNumber.trim() }
    }
    val contactKeystoreFingerprint = remember(normPhone, contactName) {
        VaultCryptoManager.computeKeystoreFingerprint("CONTACT_${normPhone}_$contactName")
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (avatarBg, avatarFg) = remember(normPhone, isDark) {
        android17AvatarColors(normPhone, isDark)
    }
    var showRenameDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("contact_profile_pop_view")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSw) "Taarifa za Contact (Pop View)" else "Contact Profile (Pop View)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = s.btnClose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                // Hero Profile Avatar + Verified AES-256 Lock Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = extractInitials(contactName),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = avatarFg
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Name & Phone Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = contactName.ifBlank { phoneNumber },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeSim != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SimCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = activeSim.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4-Column Quick Action Row (Call • Search • Schedule • Copy Number)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileQuickActionTile(
                    icon = Icons.Filled.Call,
                    label = s.convProfileCall,
                    accentColor = Color(0xFF2563EB),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_action_call"),
                    onClick = {
                        onDismiss()
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                        runCatching { context.startActivity(dialIntent) }
                    }
                )
                ProfileQuickActionTile(
                    icon = Icons.Filled.Search,
                    label = s.convProfileSearch,
                    accentColor = Color(0xFF4F46E5),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_action_search"),
                    onClick = {
                        onDismiss()
                        onSearchInChat()
                    }
                )
                ProfileQuickActionTile(
                    icon = Icons.Filled.ScheduleSend,
                    label = s.convProfileSchedule,
                    accentColor = Color(0xFF7C3AED),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_action_schedule"),
                    onClick = {
                        onDismiss()
                        onScheduleSms()
                    }
                )
                ProfileQuickActionTile(
                    icon = Icons.Filled.ContentCopy,
                    label = s.convProfileCopyNumber,
                    accentColor = Color(0xFF0EA5E9),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_action_copy_number"),
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("MengiTex Phone", phoneNumber))
                        onCopiedSensitiveText()
                        onDismiss()
                        onShowMessage(s.convProfileNumberCopied)
                    }
                )
            }

            // Conversation Stats Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalMessages.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = s.convProfileStatsMessages,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = secretMessages.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = s.convProfileStatsSecret,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // AES-256 Vault Security Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.convProfileEncryptionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${s.convProfileEncryptionSub} • $contactKeystoreFingerprint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Save / Label Contact Name Button
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showRenameDialog = true }
                    .testTag("profile_save_contact_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.convProfileSaveContact,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember(contactName) {
            mutableStateOf(if (contactName == phoneNumber) "" else contactName)
        }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(s.convProfileSaveContact) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(s.friendNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveContactName(newName)
                        showRenameDialog = false
                        onDismiss()
                    },
                    enabled = newName.trim().isNotEmpty()
                ) {
                    Text(s.btnAdd)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(s.btnCancel) }
            }
        )
    }
}

@Composable
private fun ProfileQuickActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(21.dp)
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

/**
 * 2026 Long-Press ("Hold") Message Action Sheet:
 * Complete hold options for any chat text:
 * - Copy Text, Forward Text, Delete Text, Share Text
 * - Detected Phone Numbers inside the text with 1-tap Call & Copy Number
 * - Quote & Reply to Text, Select & Copy Partial Text/Codes, Edit/Reuse in Composer,
 *   Select Multiple Messages, Schedule Message, and Message Info & Delivery Report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageLongPressActionSheet(
    message: DecryptedSmsMessage,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onForward: () -> Unit,
    onDetails: () -> Unit,
    onMoveToTrash: () -> Unit,
    onQuoteReply: () -> Unit = {},
    onSelectPartialText: () -> Unit = {},
    onEditInComposer: () -> Unit = {},
    onSelectMultiple: () -> Unit = {},
    onScheduleMessage: () -> Unit = {},
    onCopySpecificText: (String) -> Unit = {},
    onCallPhoneNumber: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val isSecret = message.isSecretBurnAfterRead
    val detectedPhones = remember(message.body, isSecret) {
        if (isSecret) emptyList() else ContactsLoader.extractPhoneNumbersFromText(message.body)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("message_hold_pop_view")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Pop View Header Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (isSw) "Chaguo za Ujumbe (Pop View)" else "Message Options (Pop View)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = s.btnClose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

            // Selected Message Preview Card (with native SelectionContainer so user can also highlight text directly)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSecret) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSecret) s.secretCardMask else message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Android Keystore AES-256-GCM Per-Message Verification Badge
            val messageKeystoreFingerprint = remember(message.id, message.phoneNumber) {
                VaultCryptoManager.computeKeystoreFingerprint("${message.phoneNumber}_${message.id}")
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EnhancedEncryption,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isSw) {
                            "Ulinzi wa Keystore AES-256-GCM • $messageKeystoreFingerprint"
                        } else {
                            "Keystore AES-256-GCM Protected • $messageKeystoreFingerprint"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Primary 4-Pill Action Row: Copy Text • Forward Text • Delete Text • Share Text
            if (!isSecret) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileQuickActionTile(
                        icon = Icons.Filled.ContentCopy,
                        label = if (isSw) "Copy Text" else "Copy Text",
                        accentColor = Color(0xFF2563EB),
                        onClick = onCopy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hold_action_copy")
                    )
                    ProfileQuickActionTile(
                        icon = Icons.AutoMirrored.Filled.Forward,
                        label = if (isSw) "Forward" else "Forward",
                        accentColor = Color(0xFF0EA5E9),
                        onClick = onForward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hold_action_forward")
                    )
                    ProfileQuickActionTile(
                        icon = Icons.Filled.DeleteOutline,
                        label = if (isSw) "Futa / Delete" else "Delete Text",
                        accentColor = MaterialTheme.colorScheme.error,
                        onClick = onMoveToTrash,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hold_action_delete_top")
                    )
                    ProfileQuickActionTile(
                        icon = Icons.Filled.Share,
                        label = s.convActionShare,
                        accentColor = Color(0xFF7C3AED),
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hold_action_share")
                    )
                }
            }

            // Detected Phone Numbers inside this Message (Instant Call or Copy Number)
            if (detectedPhones.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSw) "Namba za Simu Kwenye Ujumbe Huu" else "Phone Numbers in Message",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = { onCallPhoneNumber(num) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Call,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isSw) "Piga" else "Call",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { onCopySpecificText(num) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isSw) "Copy Namba" else "Copy",
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

            // Secondary List Actions: Quote & Reply, Select & Copy Partial Text, Edit in Composer, Select Multiple, Schedule, Info, Delete
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (!isSecret) {
                        HoldSheetActionRow(
                            icon = Icons.AutoMirrored.Filled.Reply,
                            title = if (isSw) "Jibu Ujumbe Huu (Quote & Reply)" else "Quote & Reply to Text",
                            tint = MaterialTheme.colorScheme.primary,
                            testTag = "hold_action_quote_reply",
                            onClick = onQuoteReply
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        HoldSheetActionRow(
                            icon = Icons.Filled.ContentCopy,
                            title = if (isSw) "Chagua & Nakili Sehemu ya Text / Namba" else "Select & Copy Part of Text / Numbers",
                            tint = Color(0xFF0EA5E9),
                            testTag = "hold_action_select_partial_text",
                            onClick = onSelectPartialText
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        HoldSheetActionRow(
                            icon = Icons.Filled.Edit,
                            title = if (isSw) "Hariri Kwenye Uwanja wa Kuandika" else "Edit / Reuse Text in Composer",
                            tint = Color(0xFF7C3AED),
                            testTag = "hold_action_edit_composer",
                            onClick = onEditInComposer
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        HoldSheetActionRow(
                            icon = Icons.Filled.ScheduleSend,
                            title = if (isSw) "Panga Muda wa Kutuma (Schedule Text)" else "Schedule This Text",
                            tint = Color(0xFF4F46E5),
                            testTag = "hold_action_schedule",
                            onClick = onScheduleMessage
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    HoldSheetActionRow(
                        icon = Icons.Filled.SelectAll,
                        title = if (isSw) "Chagua Jumbe Nyingi (Select Multiple)" else "Select Multiple Messages",
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "hold_action_select_multiple",
                        onClick = onSelectMultiple
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    HoldSheetActionRow(
                        icon = Icons.Filled.Info,
                        title = s.convActionInfo,
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "hold_action_details",
                        onClick = onDetails
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    HoldSheetActionRow(
                        icon = Icons.Filled.DeleteOutline,
                        title = if (isSw) "Futa Ujumbe (Delete Text)" else s.convActionTrash,
                        tint = MaterialTheme.colorScheme.error,
                        isDestructive = true,
                        testTag = "hold_action_trash",
                        onClick = onMoveToTrash
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun HoldSheetActionRow(
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
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dedicated Bottom Sheet opened when a user taps a phone number sent inside a chat message:
 * Allows 1-tap Call, Copy Phone Number, Insert in Chat, Send SMS, or Save to Contacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberQuickActionSheet(
    phoneNumber: String,
    onCall: (String) -> Unit,
    onCopy: (String) -> Unit,
    onInsertInComposer: (String) -> Unit,
    onSaveToContacts: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val isSw = s.btnCancel == "Ghairi"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 310.dp)
                .padding(horizontal = 24.dp)
                .testTag("phone_number_pop_view")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isSw) "Chagua kitendo" else "Choose an action",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = s.btnClose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Compact Action Buttons: Call & Copy Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            onDismiss()
                            onCall(phoneNumber)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16A34A),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phone_sheet_action_call")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSw) "Piga" else "Call",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            onDismiss()
                            onCopy(phoneNumber)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phone_sheet_action_copy")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSw) "Nakili" else "Copy",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        HoldSheetActionRow(
                            icon = Icons.Filled.Edit,
                            title = if (isSw) "Weka kwenye ujumbe" else "Insert into chat box",
                            tint = MaterialTheme.colorScheme.primary,
                            testTag = "phone_sheet_action_insert",
                            onClick = {
                                onDismiss()
                                onInsertInComposer(phoneNumber)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        HoldSheetActionRow(
                            icon = Icons.Filled.Sms,
                            title = if (isSw) "Tuma SMS" else "Send SMS",
                            tint = MaterialTheme.colorScheme.secondary,
                            testTag = "phone_sheet_action_sms",
                            onClick = {
                                onDismiss()
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                                runCatching { context.startActivity(smsIntent) }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        HoldSheetActionRow(
                            icon = Icons.Filled.PersonAdd,
                            title = if (isSw) "Hifadhi kwenye majina" else "Save to contacts",
                            tint = Color(0xFF7C3AED),
                            testTag = "phone_sheet_action_save_contact",
                            onClick = {
                                onDismiss()
                                onSaveToContacts(phoneNumber)
                                val addContactIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                                }
                                runCatching { context.startActivity(addContactIntent) }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog that lets the user highlight & copy any partial text from a message, OR tap 1-click pills
 * for any detected phone numbers, OTP/numeric codes, or lines in the message.
 */
@Composable
fun SelectAndCopyPartialTextDialog(
    messageBody: String,
    onCopyText: (String) -> Unit,
    onCallPhone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val detectedPhones = remember(messageBody) {
        ContactsLoader.extractPhoneNumbersFromText(messageBody)
    }
    val detectedCodes = remember(messageBody) {
        Regex("""\b[A-Z0-9]{4,12}\b""").findAll(messageBody)
            .map { it.value }
            .filter { token -> token.any { it.isDigit() } && token !in detectedPhones }
            .distinct()
            .take(8)
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSw) "Chagua & Nakili Maandishi au Namba" else "Select & Copy Text or Numbers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isSw) {
                        "Shikilia maandishi hapa chini kuchagua sehemu unayotaka, au gusa namba/kodi kunakili moja kwa moja:"
                    } else {
                        "Long-press the text below to highlight any portion, or tap a detected number/code below:"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = messageBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (detectedPhones.isNotEmpty()) {
                    Text(
                        text = if (isSw) "Namba za Simu:" else "Phone Numbers:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    detectedPhones.forEach { phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    onClick = {
                                        onDismiss()
                                        onCallPhone(phone)
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
                                        onCopyText(phone)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = if (isSw) "Copy Namba" else "Copy",
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

                if (detectedCodes.isNotEmpty()) {
                    Text(
                        text = if (isSw) "Namba / Kodi Ndani ya Ujumbe:" else "Codes / Numbers in Message:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        detectedCodes.forEach { code ->
                            Surface(
                                onClick = {
                                    onCopyText(code)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSw) "Nakili" else "Copy",
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopyText(messageBody)
                    onDismiss()
                }
            ) {
                Text(if (isSw) "Copy Ujumbe Wote" else "Copy Full Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.btnClose)
            }
        }
    )
}

/**
 * 2026 Forward-To Recipient Picker Sheet:
 * Search friends, select one or multiple contacts (or type any raw phone number),
 * and forward the SMS text offline in 1 tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageModalSheet(
    messageBody: String,
    contacts: List<ContactItem>,
    onForwardTo: (List<ContactItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ContactItem>() }

    val filtered = remember(contacts, search) {
        val q = search.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.name.contains(q, ignoreCase = true) || it.phoneNumber.contains(q, ignoreCase = true)
        }
    }

    val manualPhoneCandidate = remember(search) {
        val cleaned = search.trim()
        val digits = cleaned.filter { it.isDigit() }
        if (digits.length >= 4) cleaned else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = s.convForwardTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = s.btnClose)
                }
            }

            // Forwarded Text Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Search or Manual Number Input
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text(
                                text = s.convForwardSearchHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forward_search_input")
                        )
                    }
                    if (search.isNotEmpty()) {
                        IconButton(
                            onClick = { search = "" },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            // Optional 1-tap "Use This Number" when user types a raw number
            if (manualPhoneCandidate != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val norm = ContactsLoader.normalizePhone(manualPhoneCandidate)
                            if (selected.none { ContactsLoader.normalizePhone(it.phoneNumber) == norm }) {
                                selected.add(
                                    ContactItem(
                                        id = "manual_$norm",
                                        name = manualPhoneCandidate,
                                        phoneNumber = manualPhoneCandidate,
                                        isLocalSaved = false
                                    )
                                )
                            }
                            search = ""
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${s.convForwardManualAdd}: $manualPhoneCandidate",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Contacts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 310.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "forward_contact" }
                ) { contact ->
                    val norm = ContactsLoader.normalizePhone(contact.phoneNumber)
                    val isSelected = selected.any { ContactsLoader.normalizePhone(it.phoneNumber) == norm }
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val (avatarBg, avatarFg) = remember(contact.phoneNumber, isDark) {
                        android17AvatarColors(contact.phoneNumber, isDark)
                    }
                    val rowShape = RoundedCornerShape(18.dp)
                    Surface(
                        shape = rowShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .clickable {
                                if (isSelected) {
                                    selected.removeAll { ContactsLoader.normalizePhone(it.phoneNumber) == norm }
                                } else {
                                    selected.add(contact)
                                }
                            }
                            .testTag("forward_contact_${contact.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = extractInitials(contact.name),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = avatarFg
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Forward Now CTA
            val canForward = selected.isNotEmpty()
            val ctaShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ctaShape)
                    .background(
                        if (canForward) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                    .clickable(enabled = canForward) {
                        onForwardTo(selected.toList())
                    }
                    .padding(vertical = 14.dp)
                    .testTag("forward_confirm_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (canForward) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = s.convForwardSendBtn(selected.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canForward) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Interactive Hardware-Backed Android Keystore AES-256-GCM Chat & Option Security Inspector Dialog.
 * Strengthens Keystore transparency and protection across every chat and option.
 */
@Composable
fun KeystoreChatSecurityDialog(
    chatTitle: String,
    scopeKey: String,
    encryptedMessageCount: Int,
    isSecretBurnArmed: Boolean = false,
    onToggleSecretBurn: (() -> Unit)? = null,
    onCopyFingerprint: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val isSw = s.btnCancel == "Ghairi"
    val fingerprint = remember(scopeKey) {
        VaultCryptoManager.computeKeystoreFingerprint(scopeKey)
    }
    var selfTestPassed by remember(scopeKey) {
        mutableStateOf(VaultCryptoManager.verifyKeystoreSelfTest(scopeKey))
    }
    var verifyCount by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.EnhancedEncryption,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (isSw) "Ulinzi wa Android Keystore AES-256-GCM" else "Android Keystore AES-256-GCM Shield",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = chatTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = fingerprint,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selfTestPassed) {
                                if (isSw) "✓ Imethibitishwa (#$verifyCount): Ufunguo wa Hardware Keystore & Envelope AES-256-GCM Uko Imara"
                                else "✓ Verified (#$verifyCount): Hardware Keystore & AES-256-GCM Envelope Active"
                            } else {
                                if (isSw) "Inakagua Keystore..." else "Verifying Keystore..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• Alias: ${VaultCryptoManager.getKeystoreHardwareAlias()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Cipher: ${VaultCryptoManager.getKeystoreCipherSpec()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• ${VaultCryptoManager.getRealtimeLatencyBadge(isSw)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = if (isSw) {
                                "• SMS Zilizolindwa Kwenye Chat Hii: $encryptedMessageCount"
                            } else {
                                "• Protected SMS in This Chat: $encryptedMessageCount"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            selfTestPassed = VaultCryptoManager.verifyKeystoreSelfTest(scopeKey)
                            verifyCount++
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keystore_verify_now_button")
                    ) {
                        Text(
                            text = if (isSw) "Thibitisha Upya" else "Re-Verify Key",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                    Surface(
                        onClick = { onCopyFingerprint(fingerprint) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keystore_copy_fingerprint_button")
                    ) {
                        Text(
                            text = if (isSw) "Copy Fingerprint" else "Copy Fingerprint",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }

                if (onToggleSecretBurn != null) {
                    Surface(
                        onClick = onToggleSecretBurn,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSecretBurnArmed) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                text = if (isSecretBurnArmed) {
                                    if (isSw) "Secret Burn: IMEWASHWA kwa Chat Hii" else "Secret Burn: ARMED for This Chat"
                                } else {
                                    if (isSw) "Washa Secret Burn (Futa Baada ya Kusomwa)" else "Arm Secret Burn-After-Read"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(s.btnClose)
            }
        }
    )
}

