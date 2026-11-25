package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedSmsMessage
import com.example.data.DecryptedTemplate
import com.example.ui.i18n.LocalAppStrings

@Composable
fun TrashScreen(
    trashedMessages: List<DecryptedSmsMessage>,
    trashedTemplates: List<DecryptedTemplate>,
    onBack: () -> Unit,
    onRestoreMessage: (DecryptedSmsMessage) -> Unit,
    onDeleteMessageForever: (DecryptedSmsMessage) -> Unit,
    onRestoreTemplate: (DecryptedTemplate) -> Unit,
    onDeleteTemplateForever: (DecryptedTemplate) -> Unit,
    onEmptyAllTrash: () -> Unit
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current
    val totalCount = trashedMessages.size + trashedTemplates.size

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.OneUi85SubScreenHeader(
            title = s.trashTitle,
            subtitle = s.trashSub(totalCount),
            backIcon = Icons.AutoMirrored.Filled.ArrowBack,
            backContentDescription = s.cdBack,
            backTestTag = "trash_back_button",
            onBack = onBack,
            trailingContent = if (totalCount > 0) {
                {
                    TextButton(
                        onClick = onEmptyAllTrash,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("empty_trash_button")
                    ) {
                        Text(s.btnEmptyAll)
                    }
                }
            } else null
        )

        if (totalCount == 0) {
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
                            Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(s.trashEmptyTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = s.trashEmptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (trashedMessages.isNotEmpty()) {
                    item {
                        Text(
                            s.trashDeletedMsgs(trashedMessages.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(trashedMessages, key = { "msg_${it.id}" }) { msg ->
                        TrashItemCard(
                            title = "${msg.contactName} (${msg.phoneNumber})",
                            preview = msg.body,
                            tag = "SMS",
                            restoreLabel = s.btnRestore,
                            deleteLabel = s.btnDelete,
                            onRestore = { onRestoreMessage(msg) },
                            onDeleteForever = { onDeleteMessageForever(msg) }
                        )
                    }
                }

                if (trashedTemplates.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            s.trashDeletedTmpls(trashedTemplates.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(trashedTemplates, key = { "tmpl_${it.id}" }) { tmpl ->
                        TrashItemCard(
                            title = tmpl.title,
                            preview = tmpl.body,
                            tag = tmpl.category,
                            restoreLabel = s.btnRestore,
                            deleteLabel = s.btnDelete,
                            onRestore = { onRestoreTemplate(tmpl) },
                            onDeleteForever = { onDeleteTemplateForever(tmpl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashItemCard(
    title: String,
    preview: String,
    tag: String,
    restoreLabel: String,
    deleteLabel: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(restoreLabel)
                }
                OutlinedButton(
                    onClick = onDeleteForever,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(deleteLabel)
                }
            }
        }
    }
}
