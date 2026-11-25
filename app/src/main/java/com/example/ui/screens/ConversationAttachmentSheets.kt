package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ContactItem
import com.example.ui.i18n.LocalAppStrings

data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val sizeLabel: String,
    val isPhoto: Boolean
)

/**
 * Resolves real filename and formatted size from a zero-permission Photo Picker or SAF Document URI.
 */
fun resolveUriFileMeta(context: Context, uri: Uri, fallbackName: String): Pair<String, String> {
    var name = fallbackName
    var sizeBytes = -1L
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }
    }
    val sizeLabel = when {
        sizeBytes <= 0L -> ""
        sizeBytes < 1024L -> "${sizeBytes} B"
        sizeBytes < 1024L * 1024L -> "${sizeBytes / 1024L} KB"
        else -> String.format("%.1f MB", sizeBytes / (1024f * 1024f))
    }
    return name to sizeLabel
}

/**
 * Highlights all case-insensitive occurrences of [query] inside [text] for instant search clarity.
 */
fun buildSearchHighlightedText(
    text: String,
    query: String,
    highlightColor: Color,
    highlightBg: Color
): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        val lowerText = text.lowercase()
        val lowerQ = q.lowercase()
        var startIndex = 0
        while (startIndex < lowerText.length) {
            val found = lowerText.indexOf(lowerQ, startIndex)
            if (found < 0) break
            addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    color = highlightColor,
                    background = highlightBg
                ),
                start = found,
                end = (found + lowerQ.length).coerceAtMost(text.length)
            )
            startIndex = found + lowerQ.length
        }
    }
}

/**
 * 2026 Left-Side `+` Attachment Picker Sheet inside ConversationScreen:
 * Choose Photo (Android Photo Picker), Document (SAF), Contact Phone Number, Template, Secret Burn, or Schedule SMS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationAttachmentSheet(
    onPickPhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onPickContactNumber: () -> Unit,
    onOpenTemplates: () -> Unit,
    onToggleSecret: () -> Unit,
    onScheduleSms: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = s.attachSheetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = s.btnClose)
                }
            }

            // Row 1: Photo • Document • Phone Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AttachmentOptionTile(
                    icon = Icons.Filled.PhotoLibrary,
                    label = s.attachPhoto,
                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6)),
                    onClick = {
                        onDismiss()
                        onPickPhoto()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_photo")
                )
                AttachmentOptionTile(
                    icon = Icons.Filled.Description,
                    label = s.attachDocument,
                    gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF9333EA)),
                    onClick = {
                        onDismiss()
                        onPickDocument()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_document")
                )
                AttachmentOptionTile(
                    icon = Icons.Filled.ContactPhone,
                    label = s.attachContactNumber,
                    gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                    onClick = {
                        onDismiss()
                        onPickContactNumber()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_number")
                )
            }

            // Row 2: Templates • Secret Burn • Schedule SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AttachmentOptionTile(
                    icon = Icons.Filled.ViewAgenda,
                    label = s.attachTemplate,
                    gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1)),
                    onClick = {
                        onDismiss()
                        onOpenTemplates()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_template")
                )
                AttachmentOptionTile(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = s.attachSecretSms,
                    gradientColors = listOf(Color(0xFFDB2777), Color(0xFFF43F5E)),
                    onClick = {
                        onDismiss()
                        onToggleSecret()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_secret")
                )
                AttachmentOptionTile(
                    icon = Icons.Filled.ScheduleSend,
                    label = s.attachScheduleSms,
                    gradientColors = listOf(Color(0xFF0D9488), Color(0xFF10B981)),
                    onClick = {
                        onDismiss()
                        onScheduleSms()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("attach_option_schedule")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AttachmentOptionTile(
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Contact Number Picker Sheet: select any friend or type a number to insert directly into the chat composer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactNumberPickerSheet(
    contacts: List<ContactItem>,
    onInsertContactCard: (formattedText: String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }

    val filtered = remember(contacts, search) {
        val q = search.trim()
        if (q.isEmpty()) contacts
        else {
            val qDigits = q.filter { it.isDigit() }
            contacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.phoneNumber.contains(q, ignoreCase = true) ||
                    (qDigits.length >= 3 && it.phoneNumber.filter { ch -> ch.isDigit() }.contains(qDigits))
            }
        }
    }

    val manualDigits = remember(search) { search.trim().filter { it.isDigit() } }

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
                    text = s.attachPickContactTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = s.btnClose)
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(18.dp)
                    )
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (manualDigits.length >= 4) {
                val raw = search.trim()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onInsertContactCard("📞 $raw")
                            onDismiss()
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${s.convForwardManualAdd}: $raw",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 330.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "pick_number_contact" }
                ) { contact ->
                    val avatarColors = remember(contact.phoneNumber) { avatarGradientForKey(contact.phoneNumber) }
                    val rowShape = RoundedCornerShape(16.dp)
                    Surface(
                        shape = rowShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .clickable {
                                onInsertContactCard("👤 ${contact.name} — ${contact.phoneNumber}")
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(avatarColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = extractInitials(contact.name),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
