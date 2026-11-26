package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.FullContactDetails
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.android17AvatarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full Contact Details & Profile Bottom Sheet accessible both from:
 * 1. Contacts Tab (view ALL Android + MengiTex details for any contact)
 * 2. Messages Conversation List (view any sender's profile from OUTSIDE the chat text)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFullDetailsSheet(
    contactName: String,
    phoneNumber: String,
    isLocalSaved: Boolean = false,
    allMessages: List<DecryptedSmsMessage>,
    onOpenConversation: (name: String, phone: String) -> Unit,
    onSaveContactName: (phone: String, newName: String) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val normPhone = remember(phoneNumber) {
        ContactsLoader.normalizePhone(phoneNumber).ifEmpty { phoneNumber.trim() }
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (avatarBg, avatarFg) = remember(normPhone, isDark) {
        android17AvatarColors(normPhone, isDark)
    }
    val isAlphanumeric = remember(phoneNumber) { ContactsLoader.isAlphanumericSender(phoneNumber) }

    val details by produceState<FullContactDetails?>(initialValue = null, contactName, phoneNumber) {
        value = withContext(Dispatchers.IO) {
            ContactsLoader.loadFullContactDetails(context, contactName, phoneNumber)
        }
    }

    val photoBitmap by produceState<ImageBitmap?>(initialValue = null, details?.photoUri) {
        val uriStr = details?.photoUri
        value = if (!uriStr.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }

    val effectiveName = details?.displayName?.ifBlank { contactName } ?: contactName.ifBlank { phoneNumber }
    val effectivePhone = details?.primaryPhone?.ifBlank { phoneNumber } ?: phoneNumber
    val isSw = s.btnCancel == "Ghairi"
    val contactKeystoreFingerprint = remember(normPhone, effectiveName) {
        VaultCryptoManager.computeKeystoreFingerprint("CONTACT_${normPhone}_$effectiveName")
    }

    val threadMessages by produceState(
        initialValue = emptyList<DecryptedSmsMessage>(),
        key1 = allMessages,
        key2 = normPhone,
        key3 = details?.phones
    ) {
        val targetNorms = buildSet {
            add(ContactsLoader.resolveCanonicalThreadKey(phoneNumber, contactName))
            add(normPhone)
            details?.phones?.forEach { entry ->
                val n = ContactsLoader.normalizePhone(entry.number)
                if (n.isNotEmpty()) add(n)
            }
        }
        value = if (allMessages.size <= 3000) {
            allMessages.filter { msg -> msg.canonicalThreadKey in targetNorms }
        } else {
            withContext(Dispatchers.Default) {
                allMessages.filter { msg -> msg.canonicalThreadKey in targetNorms }
            }
        }
    }

    val sentCount = remember(threadMessages) { threadMessages.count { it.direction == MessageDirection.OUTGOING } }
    val recvCount = remember(threadMessages) { threadMessages.count { it.direction == MessageDirection.INCOMING } }
    val secretCount = remember(threadMessages) { threadMessages.count { it.isSecretBurnAfterRead } }
    val latestMsg = threadMessages.firstOrNull()

    var showRenameDialog by remember { mutableStateOf(false) }

    val sourceBadgeText = when {
        details?.isSavedInSystem == true -> s.contactDetailsSourceSystem
        isAlphanumeric -> s.contactDetailsSourceService
        isLocalSaved -> s.contactDetailsSourceLocal
        else -> s.contactDetailsSourceUnsaved
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("contact_full_details_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // Pop View Header Row with Close Button
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
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSw) "Contact Pop View • $contactKeystoreFingerprint" else "Contact Pop View • $contactKeystoreFingerprint",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            // 1. Hero Avatar (Real Contact Photo or Solid Monogram) + Starred Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap!!,
                            contentDescription = effectiveName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = extractInitials(effectiveName),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = avatarFg
                        )
                    }
                }
                if (details?.isStarred == true) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // 2. Name, Primary Phone & Source Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = effectiveName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (effectivePhone.isNotBlank() && effectivePhone != effectiveName) {
                    Text(
                        text = effectivePhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = sourceBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 3. 4-Column Quick Action Row (Message • Call • Copy • Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ContactDetailActionBtn(
                    icon = Icons.Filled.Sms,
                    label = s.tabMessages,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_details_action_chat"),
                    onClick = {
                        onDismiss()
                        onOpenConversation(effectiveName, effectivePhone)
                    }
                )
                if (!isAlphanumeric) {
                    ContactDetailActionBtn(
                        icon = Icons.Filled.Call,
                        label = s.convProfileCall,
                        accent = Color(0xFF059669),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_details_action_call"),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(effectivePhone)}"))
                                )
                            }
                        }
                    )
                }
                ContactDetailActionBtn(
                    icon = Icons.Filled.ContentCopy,
                    label = s.convActionCopy,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_details_action_copy"),
                    onClick = {
                        copyTextToClipboard(context, effectiveName, effectivePhone)
                        onShowMessage(s.convProfileNumberCopied)
                    }
                )
                ContactDetailActionBtn(
                    icon = Icons.Filled.Share,
                    label = s.contactDetailsShareContact,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_details_action_share"),
                    onClick = {
                        val shareSummary = buildString {
                            append(effectiveName)
                            details?.phones?.forEach { append("\n${it.number}${if (it.typeLabel.isNotBlank()) " (${it.typeLabel})" else ""}") }
                                ?: append("\n$effectivePhone")
                            details?.emails?.forEach { append("\n${it.email}") }
                            if (!details?.organization.isNullOrBlank()) append("\n${details?.organization}")
                        }
                        runCatching {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, effectiveName)
                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, s.contactDetailsShareContact))
                        }
                    }
                )
            }

            // 4. Complete Contact Details Card (Phones, Emails, Work, Address, Notes)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = s.contactDetailsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // All Phone Numbers
                    val phoneList = details?.phones?.takeIf { it.isNotEmpty() }
                        ?: listOf(com.example.service.ContactPhoneEntry(effectivePhone, ""))
                    phoneList.forEachIndexed { idx, entry ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = entry.typeLabel.ifBlank { s.contactDetailsPhones },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyTextToClipboard(context, effectiveName, entry.number)
                                    onShowMessage(s.convProfileNumberCopied)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = s.convActionCopy,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onOpenConversation(effectiveName, entry.number)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sms,
                                    contentDescription = s.contactDetailsOpenChat,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // All Email Addresses
                    details?.emails?.forEach { emailEntry ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${emailEntry.email}")))
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = emailEntry.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = emailEntry.typeLabel.ifBlank { s.contactDetailsEmails },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyTextToClipboard(context, effectiveName, emailEntry.email)
                                    onShowMessage(s.convProfileNumberCopied)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = s.convActionCopy,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Company / Job Title
                    val orgText = listOfNotNull(
                        details?.jobTitle?.takeIf { it.isNotBlank() },
                        details?.organization?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (orgText.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            icon = Icons.Filled.Business,
                            label = s.contactDetailsWork,
                            value = orgText
                        )
                    }

                    // Postal Address
                    val addr = details?.address
                    if (!addr.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            icon = Icons.Filled.LocationOn,
                            label = s.contactDetailsAddress,
                            value = addr
                        )
                    }

                    // Notes
                    val noteText = details?.notes
                    if (!noteText.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            icon = Icons.Filled.Notes,
                            label = s.contactDetailsNotes,
                            value = noteText
                        )
                    }
                }
            }

            // 5. MengiTex Conversation Stats Card (Visible even from outside the chat!)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = s.contactDetailsActivityTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (threadMessages.isEmpty()) {
                        Text(
                            text = s.contactDetailsNoMessagesYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatPill(label = s.dashboardTotalAll, value = threadMessages.size.toString(), modifier = Modifier.weight(1f))
                            MiniStatPill(label = s.dashboardKpiSent, value = sentCount.toString(), modifier = Modifier.weight(1f))
                            MiniStatPill(label = s.dashboardKpiReceived, value = recvCount.toString(), modifier = Modifier.weight(1f))
                            if (secretCount > 0) {
                                MiniStatPill(label = s.filterSecret, value = secretCount.toString(), modifier = Modifier.weight(1f))
                            }
                        }
                        if (latestMsg != null) {
                            val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
                            Text(
                                text = "${s.contactDetailsLastActivity}: ${fmt.format(Date(latestMsg.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (latestMsg.isSecretBurnAfterRead) s.secretCardMask else latestMsg.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. Bottom Action Buttons: Open Conversation + Save/Rename or Open in Phone Contacts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val lookupUri = details?.contactLookupUri
                        if (lookupUri != null) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
                            }
                        } else {
                            showRenameDialog = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_details_secondary_button")
                ) {
                    Icon(
                        imageVector = if (details?.contactLookupUri != null) Icons.Filled.OpenInNew else Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (details?.contactLookupUri != null) s.contactDetailsOpenSystemContact else s.convProfileSaveContact,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onOpenConversation(effectiveName, effectivePhone)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_details_open_chat_button")
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s.contactDetailsOpenChat,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (details?.contactLookupUri != null) {
                TextButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.testTag("contact_details_rename_local_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(s.convProfileSaveContact)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(if (effectiveName == effectivePhone) "" else effectiveName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(s.convProfileSaveContact) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(s.friendNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.trim().isNotEmpty()) {
                            onSaveContactName(effectivePhone, newName.trim())
                            showRenameDialog = false
                            onDismiss()
                        }
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
private fun ContactDetailActionBtn(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
