package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContactItem
import com.example.data.DecryptedTemplate
import com.example.security.VaultCryptoManager
import com.example.service.ContactsLoader
import com.example.service.DetectedSimCard
import com.example.service.VoiceDictationHelper
import com.example.ui.components.OneUi85SubScreenHeader
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.components.premiumBounceClickable
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.android17AvatarColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeScreen(
    contacts: List<ContactItem>,
    selectedRecipients: List<ContactItem>,
    templates: List<DecryptedTemplate>,
    detectedSims: List<DetectedSimCard>,
    selectedSimSubId: Int,
    bodyText: String,
    partsCount: Int,
    isSecretBurn: Boolean,
    hasSmsPermission: Boolean,
    hasContactsPermission: Boolean,
    hasMicPermission: Boolean,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onAppendVoiceText: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onToggleRecipient: (ContactItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onAddManualRecipient: (name: String, phone: String, saveToList: Boolean) -> Unit,
    onSelectSim: (Int) -> Unit,
    onBodyChange: (String) -> Unit,
    onSecretBurnChange: (Boolean) -> Unit,
    onUseTemplate: (DecryptedTemplate) -> Unit,
    onSendNow: (useSystemFallback: Boolean) -> Unit,
    onSchedule: (triggerAtMillis: Long) -> Unit
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val isSw = s.btnCancel == "Ghairi"

    var contactSearch by remember { mutableStateOf("") }
    var directPhoneInput by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showKeystoreDialog by remember { mutableStateOf(false) }
    var popViewedContact by remember { mutableStateOf<ContactItem?>(null) }
    // Always keep contacts & search bar visible so user can search or pick numbers anytime
    var expandContactsPicker by remember { mutableStateOf(true) }
    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceRmsLevel by remember { mutableFloatStateOf(0f) }
    var partialVoicePreview by remember { mutableStateOf("") }

    val composeKeystoreFingerprint = remember(selectedRecipients.size) {
        val scope = selectedRecipients.joinToString("_") { it.phoneNumber }.ifBlank { "NEW_MESSAGE_COMPOSE" }
        VaultCryptoManager.computeKeystoreFingerprint(scope)
    }

    val activeSim = remember(detectedSims, selectedSimSubId) {
        detectedSims.firstOrNull { it.subscriptionId == selectedSimSubId } ?: detectedSims.firstOrNull()
    }

    val voiceHelper = remember(context) {
        VoiceDictationHelper(
            context = context,
            onListeningChanged = { listening ->
                isVoiceListening = listening
                if (!listening) partialVoicePreview = ""
            },
            onRmsLevelChanged = { rms -> voiceRmsLevel = rms },
            onPartialTranscript = { partial -> partialVoicePreview = partial },
            onFinalTranscript = { transcript ->
                partialVoicePreview = ""
                onAppendVoiceText(transcript)
            },
            onError = { err -> onShowMessage(err) }
        )
    }

    DisposableEffect(voiceHelper) {
        onDispose { voiceHelper.stopListening() }
    }

    val filteredContacts = remember(contacts, contactSearch) {
        val q = contactSearch.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.phoneNumber.contains(q, ignoreCase = true)
        }
    }

    val searchDigitsCandidate = remember(contactSearch) {
        val cleaned = contactSearch.trim()
        val digits = cleaned.filter { it.isDigit() }
        if (digits.length >= 3) cleaned else null
    }

    val selectedNorms = remember(selectedRecipients) {
        selectedRecipients.map { ContactsLoader.normalizePhone(it.phoneNumber) }.toSet()
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        OneUi85SubScreenHeader(
            title = s.composeTitle,
            subtitle = null,
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "compose_back_button",
            onBack = onBack,
            trailingContent = {
                Surface(
                    onClick = { showKeystoreDialog = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("compose_keystore_shield_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AES-256",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        // 2. Scrollable Middle Content: SIM Selector + Direct Add Phone Number + Contact Search Bar + Contacts List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasSmsPermission || !hasContactsPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.permBannerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = s.permBannerSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRequestPermissions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("compose_request_permissions_button")
                            ) {
                                Text(s.btnGrant)
                            }
                        }
                    }
                }
            }

            // Detected SIM Card Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = s.simSelectorHeader,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detectedSims.isEmpty()) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = onRequestPermissions,
                                    label = { Text(s.simDefaultFallback) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.SimCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        } else {
                            items(detectedSims, key = { it.subscriptionId }) { sim ->
                                val isSelected = sim.subscriptionId == selectedSimSubId ||
                                    (selectedSimSubId == -1 && sim == detectedSims.first())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSim(sim.subscriptionId) },
                                    label = { Text(sim.displayName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.SimCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("sim_chip_${sim.slotIndex}")
                                )
                            }
                        }
                    }
                }
            }

            // Recipients + Direct Add Phone Number + Contact Search Bar + Scrollable Contacts Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = s.recipientsHeader(selectedRecipients.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { showAddFriendDialog = true },
                                    modifier = Modifier.testTag("compose_add_manual_number_button")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(s.btnAddNumber)
                                }
                                TextButton(onClick = { expandContactsPicker = !expandContactsPicker }) {
                                    Text(if (expandContactsPicker) s.btnHideList else s.btnShowList)
                                }
                            }
                        }

                        // Selected Recipients Chips (shows both Name and Phone Number)
                        if (selectedRecipients.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedRecipients.forEach { person ->
                                    val chipLabel = if (person.name != person.phoneNumber && person.phoneNumber.isNotBlank()) {
                                        "${person.name} (${person.phoneNumber})"
                                    } else {
                                        person.name
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = { onToggleRecipient(person) },
                                        label = {
                                            Text(
                                                text = chipLabel,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // A. Direct Add Phone Number Bar ("sehemu kuadd namba")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = directPhoneInput,
                                onValueChange = { directPhoneInput = it },
                                placeholder = {
                                    Text(
                                        text = if (isSw) "Andika namba ya simu kuongeza (mf. 07...)" else "Type phone number to add...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("compose_direct_phone_input")
                            )
                            Button(
                                onClick = {
                                    val cleaned = directPhoneInput.trim()
                                    if (cleaned.length >= 3) {
                                        onAddManualRecipient(cleaned, cleaned, false)
                                        directPhoneInput = ""
                                    }
                                },
                                enabled = directPhoneInput.trim().length >= 3,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("compose_direct_phone_add_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSw) "Ongeza" else "Add")
                            }
                        }

                        // B. Contact Search Bar + Scrollable Contacts List ("namba zitokee kwa contact na bar search")
                        AnimatedVisibility(
                            visible = expandContactsPicker,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = contactSearch,
                                    onValueChange = { contactSearch = it },
                                    placeholder = {
                                        Text(
                                            text = if (isSw) "Tafuta jina au namba kwenye Contacts..." else s.contactsSearchHint
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (contactSearch.isNotEmpty()) {
                                            IconButton(onClick = { contactSearch = "" }) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("compose_contact_search_input")
                                )

                                // If user typed digits in the search bar, offer 1-tap Add Number
                                if (searchDigitsCandidate != null) {
                                    Surface(
                                        onClick = {
                                            onAddManualRecipient(searchDigitsCandidate, searchDigitsCandidate, false)
                                            contactSearch = ""
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("compose_search_add_number_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PersonAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isSw) {
                                                    "+ Ongeza Namba Mpya: $searchDigitsCandidate"
                                                } else {
                                                    "+ Add Phone Number: $searchDigitsCandidate"
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSw) {
                                            "Majina & Namba za Contacts (${filteredContacts.size})"
                                        } else {
                                            "Contacts & Phone Numbers (${filteredContacts.size})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row {
                                        TextButton(
                                            onClick = onSelectAll,
                                            enabled = contacts.isNotEmpty(),
                                            modifier = Modifier.testTag("compose_select_all_contacts")
                                        ) {
                                            Text(s.btnAll)
                                        }
                                        TextButton(
                                            onClick = onClearAll,
                                            enabled = selectedRecipients.isNotEmpty()
                                        ) {
                                            Text(s.btnClear)
                                        }
                                    }
                                }

                                if (filteredContacts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.contactsEmptyHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        filteredContacts.take(150).forEach { contact ->
                                            val checked = selectedNorms.contains(ContactsLoader.normalizePhone(contact.phoneNumber))
                                            val (avatarBg, avatarFg) = remember(contact.phoneNumber, isDark) {
                                                android17AvatarColors(contact.phoneNumber, isDark)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (checked) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { onToggleRecipient(contact) }
                                                    .testTag("contact_row_${contact.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = checked,
                                                        onCheckedChange = { onToggleRecipient(contact) }
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(avatarBg),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = contact.name.take(1).uppercase(),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = avatarFg
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
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
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    // 1-tap Contact Pop View button ("Upande wa contact Niki view contact iwe kwa pop")
                                                    IconButton(
                                                        onClick = { popViewedContact = contact },
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .testTag("compose_contact_pop_view_${contact.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.AccountCircle,
                                                            contentDescription = s.contactDetailsViewProfileCd,
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(20.dp)
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
            }

            // Quick Template Picker Row
            if (templates.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = s.quickTemplatesHeader,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(templates.take(8), key = { it.id }) { tmpl ->
                                AssistChip(
                                    onClick = { onUseTemplate(tmpl) },
                                    label = { Text(tmpl.title) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Description,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact Bottom Pinned Authentic Android Phone SMS Composer Dock ("Hii bar ya kutuma sms mpya weka iwe ndogo")
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = { onSecretBurnChange(!isSecretBurn) },
                            shape = RoundedCornerShape(50),
                            color = if (isSecretBurn) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.testTag("compose_secret_burn_switch")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isSecretBurn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = s.secretBurnToggleTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSecretBurn) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSecretBurn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { showKeystoreDialog = true },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = s.charPartsCounter(bodyText.length, partsCount),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isVoiceListening,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PulsingStatusDot(color = MaterialTheme.colorScheme.error, size = 6.dp)
                                    Text(
                                        text = partialVoicePreview.ifBlank { s.voiceListeningHint },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                VoiceWaveformVisualizer(
                                    rmsLevel = voiceRmsLevel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val canAct = selectedRecipients.isNotEmpty() && bodyText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(enabled = canAct) { showScheduleDialog = true }
                                .testTag("compose_schedule_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScheduleSend,
                                contentDescription = s.btnSchedule,
                                tint = if (canAct) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
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
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = activeSim?.let { "SIM ${it.slotIndex + 1}" } ?: "SMS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                    if (bodyText.isEmpty()) {
                                        Text(
                                            text = s.unlimitedPlaceholder,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = bodyText,
                                        onValueChange = onBodyChange,
                                        maxLines = 5,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("compose_unlimited_message_input")
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isVoiceListening) MaterialTheme.colorScheme.errorContainer
                                            else Color.Transparent
                                        )
                                        .premiumBounceClickable {
                                            if (isVoiceListening) {
                                                voiceHelper.stopListening()
                                            } else if (hasMicPermission) {
                                                voiceHelper.startListening(s.language)
                                            } else {
                                                onRequestMicPermission()
                                            }
                                        }
                                        .testTag("compose_voice_mic_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isVoiceListening) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = s.voiceDictateBtn,
                                        tint = if (isVoiceListening) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canAct) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable(enabled = canAct) { onSendNow(false) }
                                .testTag("compose_send_now_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = s.btnSendCount(selectedRecipients.size),
                                tint = if (canAct) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                if (!hasSmsPermission && selectedRecipients.isNotEmpty() && bodyText.isNotBlank()) {
                    TextButton(
                        onClick = { onSendNow(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_system_fallback_button")
                    ) {
                        Text(s.btnSystemSmsFallback)
                    }
                }
            }
            }
        }
    }

    if (showAddFriendDialog) {
        AddRecipientDialog(
            onDismiss = { showAddFriendDialog = false },
            onAdd = { name, phone, save ->
                onAddManualRecipient(name, phone, save)
                showAddFriendDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleTimePickerDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { triggerMillis ->
                showScheduleDialog = false
                onSchedule(triggerMillis)
            }
        )
    }

    if (showKeystoreDialog) {
        KeystoreChatSecurityDialog(
            chatTitle = s.composeTitle,
            scopeKey = composeKeystoreFingerprint,
            encryptedMessageCount = selectedRecipients.size,
            isSecretBurnArmed = isSecretBurn,
            onToggleSecretBurn = { onSecretBurnChange(!isSecretBurn) },
            onCopyFingerprint = { fp ->
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("Keystore Fingerprint", fp))
                onShowMessage(fp)
            },
            onDismiss = { showKeystoreDialog = false }
        )
    }

    // Contact Pop View Dialog when viewing a contact from New Message ("Upande wa contact Niki view contact iwe kwa pop")
    val contactToPop = popViewedContact
    if (contactToPop != null) {
        ComposeContactQuickPopDialog(
            contact = contactToPop,
            isSelected = selectedNorms.contains(ContactsLoader.normalizePhone(contactToPop.phoneNumber)),
            onToggleSelect = {
                onToggleRecipient(contactToPop)
                popViewedContact = null
            },
            onShowMessage = onShowMessage,
            onDismiss = { popViewedContact = null }
        )
    }
}

@Composable
private fun ComposeContactQuickPopDialog(
    contact: ContactItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val isSw = s.btnCancel == "Ghairi"
    val fingerprint = remember(contact.phoneNumber) {
        VaultCryptoManager.computeKeystoreFingerprint("CONTACT_${contact.phoneNumber}")
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (avatarBg, avatarFg) = remember(contact.phoneNumber, isDark) {
        android17AvatarColors(contact.phoneNumber, isDark)
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
                .padding(horizontal = 20.dp)
                .testTag("compose_contact_pop_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSw) "Contact Pop View" else "Contact Pop View",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = s.btnClose, modifier = Modifier.size(18.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = avatarFg
                    )
                }

                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Keystore AES-256-GCM • $fingerprint",
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
                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                            runCatching { context.startActivity(dialIntent) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSw) "Piga" else "Call")
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText(contact.name, contact.phoneNumber))
                            onShowMessage(s.convProfileNumberCopied)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                }

                Button(
                    onClick = onToggleSelect,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSelected) {
                            if (isSw) "Ondoa Kwenye Wapokeaji" else "Remove Recipient"
                        } else {
                            if (isSw) "Chagua Kutuma SMS" else "Select for SMS"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRecipientDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, saveToList: Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var saveToList by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addFriendDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.friendNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(s.friendPhoneLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_phone_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = saveToList, onCheckedChange = { saveToList = it })
                    Text(s.saveFriendCheckbox, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, phone, saveToList) },
                enabled = phone.trim().length >= 3,
                modifier = Modifier.testTag("add_friend_confirm_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(s.btnAdd)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}

@Composable
private fun ScheduleTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val s = LocalAppStrings.current
    var selectedOffsetMinutes by remember { mutableIntStateOf(15) }
    val targetMillis = remember(selectedOffsetMinutes) {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, selectedOffsetMinutes)
        }.timeInMillis
    }
    val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.ScheduleSend, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(s.scheduleDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = s.scheduleWillSendAt(formatter.format(Date(targetMillis))),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.schedulePresets.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (mins, label) ->
                                FilterChip(
                                    selected = selectedOffsetMinutes == mins,
                                    onClick = { selectedOffsetMinutes = mins },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetMillis) },
                modifier = Modifier.testTag("confirm_schedule_sms_button")
            ) {
                Text(s.btnSaveSchedule)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}
