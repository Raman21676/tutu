package com.nova.browser.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.browser.data.local.db.UserScriptEntity
import com.nova.browser.ui.viewmodel.UserScriptsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserScriptsViewModel = hiltViewModel()
) {
    val scripts by viewModel.scripts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<UserScriptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Scripts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add script")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add script")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (scripts.isEmpty()) {
                EmptyScriptsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        ScriptItemCard(
                            script = script,
                            onToggle = { viewModel.toggleEnabled(script) },
                            onEdit = { editingScript = script },
                            onDelete = { viewModel.delete(script) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ScriptEditDialog(
            script = null,
            onDismiss = { showAddDialog = false },
            onSave = { script ->
                viewModel.save(script)
                showAddDialog = false
            }
        )
    }

    editingScript?.let { script ->
        ScriptEditDialog(
            script = script,
            onDismiss = { editingScript = null },
            onSave = { updated ->
                viewModel.save(updated)
                editingScript = null
            }
        )
    }
}

@Composable
private fun EmptyScriptsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No user scripts yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add scripts to customize websites.\nSupports @match patterns like *://*.youtube.com/*",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun ScriptItemCard(
    script: UserScriptEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = script.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name.ifBlank { "Untitled Script" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = script.matchPatterns.take(40).plus(if (script.matchPatterns.length > 40) "..." else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${script.code.length} chars · v${script.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptEditDialog(
    script: UserScriptEntity?,
    onDismiss: () -> Unit,
    onSave: (UserScriptEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(script?.name ?: "") }
    var matchPatterns by remember { mutableStateOf(script?.matchPatterns ?: "*://*/*") }
    var code by remember { mutableStateOf(script?.code ?: sampleScript) }
    var runAt by remember { mutableStateOf(script?.runAt ?: "document-end") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (script == null) "Add Script" else "Edit Script") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = matchPatterns,
                    onValueChange = { matchPatterns = it },
                    label = { Text("Match Patterns (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = runAt,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Run At") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("document-start", "document-end", "document-idle").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    runAt = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("JavaScript Code") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || code.isBlank()) {
                        Toast.makeText(context, "Name and code are required", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    onSave(
                        (script ?: UserScriptEntity()).copy(
                            name = name,
                            matchPatterns = matchPatterns,
                            code = code,
                            runAt = runAt
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val sampleScript = """// ==UserScript==
// @name         My Script
// @match        *://*/*
// @grant        none
// ==/UserScript==

(function() {
    'use strict';
    // Your code here
    console.log('Hello from User Script!');
})();"""
