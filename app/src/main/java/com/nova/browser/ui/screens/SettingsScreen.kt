package com.nova.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RememberMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.BuildConfig
import com.nova.browser.R
import com.nova.browser.data.model.SearchEngine
import com.nova.browser.ui.components.ToggleSwitch
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.TutuTheme
import com.nova.browser.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val clearDataSuccess by viewModel.clearDataSuccess.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General Section
            SectionTitle(stringResource(R.string.settings_general))
            
            ToggleSwitch(
                checked = settings.httpsEnabled,
                onCheckedChange = { viewModel.setHttpsEnabled(it) },
                title = "HTTPS",
                icon = Icons.Default.Lock
            )
            
            ToggleSwitch(
                checked = settings.fullscreen,
                onCheckedChange = { viewModel.setFullscreen(it) },
                title = stringResource(R.string.toggle_fullscreen),
                icon = Icons.Default.Fullscreen
            )
            
            ToggleSwitch(
                checked = settings.autoRotate,
                onCheckedChange = { viewModel.setAutoRotate(it) },
                title = stringResource(R.string.toggle_auto_rotate),
                icon = Icons.AutoMirrored.Filled.RotateRight
            )
            
            ToggleSwitch(
                checked = settings.rememberChoice,
                onCheckedChange = { viewModel.setRememberChoice(it) },
                title = stringResource(R.string.dialog_remember_choice),
                icon = Icons.Default.RememberMe
            )
            
            // Search Engine Selector
            val currentEngine = SearchEngine.fromName(settings.searchEngine)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSearchEngineDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search Engine",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = currentEngine.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy Section
            SectionTitle(stringResource(R.string.settings_privacy))
            
            // Ad Blocker Toggle
            ToggleSwitch(
                checked = settings.adBlockEnabled,
                onCheckedChange = { viewModel.setAdBlockEnabled(it) },
                title = "Block Ads & Trackers",
                icon = Icons.Default.Shield
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showClearDataDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.clear_data),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = stringResource(R.string.clear_data_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // About Section
            SectionTitle(stringResource(R.string.settings_about))
            
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.padding(vertical = 8.dp),
                tint = CoralRed
            )
            
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Search Engine Selection Dialog
    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { 
                Text(
                    "Search Engine", 
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    SearchEngine.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSearchEngine(engine.name)
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine.name,
                                onClick = {
                                    viewModel.setSearchEngine(engine.name)
                                    showSearchEngineDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralRed
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.clear_data)) },
            text = { Text("This will clear all bookmarks, settings, and browsing data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Success Dialog
    if (clearDataSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.onClearDataAcknowledged() },
            title = { Text("Success") },
            text = { Text("All data has been cleared successfully.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onClearDataAcknowledged() }) {
                    Text("OK")
                }
            }
        )
    }
    
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    TutuTheme {
        Text("Settings Preview")
    }
}
