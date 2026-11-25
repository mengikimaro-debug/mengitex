package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedTemplate
import com.example.ui.components.NextGenGradientFab
import com.example.ui.components.OneUi85SubScreenHeader
import com.example.ui.i18n.LocalAppStrings

@Composable
fun TemplatesScreen(
    templates: List<DecryptedTemplate>,
    onBack: () -> Unit,
    onUseTemplate: (DecryptedTemplate) -> Unit,
    onSaveTemplate: (id: Long?, title: String, category: String, body: String) -> Unit,
    onMoveToTrash: (DecryptedTemplate) -> Unit
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current

    var search by remember { mutableStateOf("") }
    var editingTemplate by remember { mutableStateOf<DecryptedTemplate?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = remember(templates, search) {
        if (search.isBlank()) templates
        else templates.filter {
            it.title.contains(search, ignoreCase = true) ||
                it.category.contains(search, ignoreCase = true) ||
                it.body.contains(search, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OneUi85SubScreenHeader(
                title = s.templatesTitle,
                subtitle = "Keystore AES-256",
                backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                backContentDescription = s.cdBack,
                backTestTag = "templates_back_button",
                onBack = onBack
            )

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(s.templatesSearchHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("templates_search_input")
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { tmpl ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("template_card_${tmpl.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tmpl.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = tmpl.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.EnhancedEncryption,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${s.charsWord} ${tmpl.body.length}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingTemplate = tmpl }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(onClick = { onMoveToTrash(tmpl) }) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tmpl.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onUseTemplate(tmpl) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("use_template_${tmpl.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.btnUseTemplate)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }

        NextGenGradientFab(
            text = s.btnNewTemplate,
            icon = Icons.Filled.Add,
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_template_fab")
        )
    }

    if (showCreateDialog || editingTemplate != null) {
        TemplateEditorDialog(
            initial = editingTemplate,
            onDismiss = {
                showCreateDialog = false
                editingTemplate = null
            },
            onSave = { title, category, body ->
                onSaveTemplate(editingTemplate?.id, title, category, body)
                showCreateDialog = false
                editingTemplate = null
            }
        )
    }
}

@Composable
private fun TemplateEditorDialog(
    initial: DecryptedTemplate?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, body: String) -> Unit
) {
    val s = LocalAppStrings.current
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Kazi") }
    var body by remember(initial) { mutableStateOf(initial?.body.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) s.dialogCreateTemplate else s.dialogEditTemplate) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.templateTitleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(s.templateCategoryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(s.templateBodyLabel(body.length)) },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, category, body) },
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.testTag("save_template_confirm_button")
            ) {
                Text(s.btnSaveAes)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.btnCancel) }
        }
    )
}
