package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ContactItem
import com.example.data.DecryptedSmsMessage
import com.example.service.ContactsLoader
import com.example.ui.components.NextGenGradientFab
import com.example.ui.components.NextGenIconOrb
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.android17AvatarColors

/**
 * Dedicated Contacts tab (2nd bottom bar tab) supporting viewing full contact details,
 * searching, adding friends, multi-selecting multiple friends, and launching offline SMS in 1 tap.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactItem>,
    selectedRecipients: List<ContactItem>,
    allMessages: List<DecryptedSmsMessage> = emptyList(),
    hasContactsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onToggleRecipient: (ContactItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onAddManualFriend: (name: String, phone: String, saveToList: Boolean) -> Unit,
    onQuickMessageSingle: (ContactItem) -> Unit,
    onOpenConversation: (name: String, phone: String) -> Unit = { _, _ -> },
    onSaveContactName: (phone: String, newName: String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onSendToSelected: () -> Unit
) {
    val s = LocalAppStrings.current
    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectedContact by remember { mutableStateOf<ContactItem?>(null) }

    val filtered = remember(contacts, search) {
        if (search.isBlank()) contacts
        else contacts.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.phoneNumber.contains(search, ignoreCase = true)
        }
    }

    val selectedNorms = remember(selectedRecipients) {
        selectedRecipients.map { ContactsLoader.normalizePhone(it.phoneNumber) }.toSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !hasContactsPermission,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.permContactsLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = s.permContactsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("contacts_grant_permission_button")
                        ) {
                            Text(s.btnGrant)
                        }
                    }
                }
            }

            // Android 17 M3 Search Pill & Solid Tonal Add Friend Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.weight(1f)
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
                            if (search.isEmpty()) {
                                Text(
                                    text = s.contactsSearchHint,
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
                                    .testTag("contacts_search_input")
                            )
                        }
                        AnimatedVisibility(
                            visible = search.isNotEmpty(),
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
                            IconButton(
                                onClick = { search = "" },
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
                NextGenIconOrb(
                    icon = Icons.Filled.PersonAdd,
                    contentDescription = s.contactsAddFriend,
                    onClick = { showAddDialog = true },
                    size = 48.dp,
                    accent = true,
                    modifier = Modifier.testTag("contacts_add_friend_button")
                )
            }

            // Selection controls strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = s.contactsSelectedCount(selectedRecipients.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedRecipients.isNotEmpty()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(
                        onClick = onSelectAll,
                        enabled = contacts.isNotEmpty(),
                        modifier = Modifier.testTag("contacts_select_all_button")
                    ) {
                        Text(s.contactsSelectAll)
                    }
                    TextButton(
                        onClick = onClearAll,
                        enabled = selectedRecipients.isNotEmpty()
                    ) {
                        Text(s.contactsClearAll)
                    }
                }
            }

            if (filtered.isEmpty()) {
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
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Contacts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(s.contactsEmptyTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = s.contactsEmptyHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.contactsAddFriend)
                        }
                    }
                }
            } else {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val cardShape = remember { RoundedCornerShape(24.dp) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filtered,
                        key = { it.id },
                        contentType = { "contact_row" }
                    ) { contact ->
                        val isSelected = remember(contact.phoneNumber, selectedNorms) {
                            selectedNorms.contains(ContactsLoader.normalizePhone(contact.phoneNumber))
                        }
                        val cardBg = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        }
                        val cardBorder = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                        val (avatarBg, avatarFg) = remember(contact.phoneNumber, isDark) {
                            android17AvatarColors(contact.phoneNumber, isDark)
                        }
                        val initials = remember(contact.name) { extractInitials(contact.name) }
                        Surface(
                            shape = cardShape,
                            color = cardBg,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorder),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .combinedClickable(
                                    onClick = {
                                        if (selectedRecipients.isNotEmpty()) {
                                            onToggleRecipient(contact)
                                        } else {
                                            inspectedContact = contact
                                        }
                                    },
                                    onLongClick = { onToggleRecipient(contact) }
                                )
                                .testTag("contacts_tab_item_${contact.id}")
                        ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.BottomEnd,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { onToggleRecipient(contact) }
                                            .testTag("contacts_select_avatar_${contact.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else avatarFg
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .border(1.5.dp, cardBg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleMedium,
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
                                    IconButton(
                                        onClick = { inspectedContact = contact },
                                        modifier = Modifier.testTag("contacts_view_details_${contact.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AccountCircle,
                                                contentDescription = s.contactDetailsViewProfileCd,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onQuickMessageSingle(contact) },
                                        modifier = Modifier.testTag("contacts_quick_sms_${contact.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Sms,
                                                contentDescription = s.contactsQuickSms,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                    }
                    item(key = "contacts_bottom_spacer") { Spacer(modifier = Modifier.height(104.dp)) }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedRecipients.isNotEmpty(),
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            NextGenGradientFab(
                text = s.contactsSendToSelected(selectedRecipients.size),
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onSendToSelected,
                modifier = Modifier.testTag("contacts_send_selected_fab")
            )
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var save by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(s.addFriendDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(s.friendNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(s.friendPhoneLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = save, onCheckedChange = { save = it })
                        Text(s.saveFriendCheckbox, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddManualFriend(name, phone, save)
                        showAddDialog = false
                    },
                    enabled = phone.trim().length >= 3
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.btnAdd)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(s.btnCancel) }
            }
        )
    }

    val activeContact = inspectedContact
    if (activeContact != null) {
        ContactFullDetailsSheet(
            contactName = activeContact.name,
            phoneNumber = activeContact.phoneNumber,
            isLocalSaved = activeContact.isLocalSaved,
            allMessages = allMessages,
            onOpenConversation = { name, phone ->
                onOpenConversation(name, phone)
            },
            onSaveContactName = onSaveContactName,
            onShowMessage = onShowMessage,
            onDismiss = { inspectedContact = null }
        )
    }
}
