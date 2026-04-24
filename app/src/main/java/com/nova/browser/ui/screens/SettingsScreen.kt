package com.nova.browser.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RememberMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.BuildConfig
import com.nova.browser.R
import com.nova.browser.data.model.SearchEngine
import com.nova.browser.ui.components.ToggleSwitch
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.TutuTheme
import com.nova.browser.ui.viewmodel.SettingsViewModel
import com.nova.browser.util.DownloadDirHelper
import com.nova.browser.util.DownloadDirPreference
import com.nova.browser.util.DownloadLocationType

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
    var showDirPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                checked = settings.desktopMode,
                onCheckedChange = { viewModel.setDesktopMode(it) },
                title = "Desktop Mode",
                icon = Icons.Default.Computer
            )

            ToggleSwitch(
                checked = settings.rememberChoice,
                onCheckedChange = { viewModel.setRememberChoice(it) },
                title = stringResource(R.string.dialog_remember_choice),
                icon = Icons.Default.RememberMe
            )

            ToggleSwitch(
                checked = settings.followSystemTheme,
                onCheckedChange = { viewModel.setFollowSystemTheme(it) },
                title = "Follow System Theme",
                icon = Icons.Default.SettingsBrightness
            )

            if (!settings.followSystemTheme) {
                ToggleSwitch(
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode
                )
            }

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

            // Downloads Section
            SectionTitle("Downloads")

            // Download folder row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable { showDirPicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Folder",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = DownloadDirHelper.getDisplayPath(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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

    // Output Directory Dialog
    if (showDirPicker) {
        OutputDirectoryDialog(
            context = context,
            onDismiss = { showDirPicker = false }
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

/**
 * Output Directory Selection Dialog
 *
 * Three options:
 *  0 = Phone Storage  → root of internal storage (/sdcard/Nova Downloads)
 *  1 = SD Card        → root of removable SD card (/storage/XXXX/Nova Downloads)
 *  2 = Custom         → user-specified base path + user-specified folder name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputDirectoryDialog(
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    var selectedOption by remember {
        mutableIntStateOf(
            when (DownloadDirPreference.getLocationType(context)) {
                DownloadLocationType.SD_CARD -> 1
                DownloadLocationType.CUSTOM -> 2
                else -> 0  // PHONE_STORAGE (default)
            }
        )
    }

    var folderNameText by remember {
        mutableStateOf(DownloadDirPreference.getFolderName(context))
    }

    var customPathText by remember {
        mutableStateOf(DownloadDirPreference.getCustomPath(context) ?: "")
    }
    var customFolderNameText by remember {
        mutableStateOf(DownloadDirPreference.getCustomFolderName(context))
    }

    val hasSdCard = remember { DownloadDirHelper.hasSdCard(context) }
    val sdCardPath = remember { DownloadDirHelper.getSdCardPath(context) }

    
    val noSdCardText = stringResource(R.string.output_sdcard_no_card)
    val phoneDesc = remember(folderNameText) {
        val name = folderNameText.ifBlank { DownloadDirHelper.DEFAULT_FOLDER_NAME }
        "/storage/emulated/0/$name"
    }
    val sdDesc = remember(folderNameText, hasSdCard, noSdCardText) {
        if (hasSdCard && sdCardPath != null) {
            val name = folderNameText.ifBlank { DownloadDirHelper.DEFAULT_FOLDER_NAME }
            "$sdCardPath/$name"
        } else {
            noSdCardText
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_output_folder_location)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.desc_output_folder),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Option 0: Phone Storage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = 0 }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == 0,
                        onClick = { selectedOption = 0 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.output_option_phone),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = phoneDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Option 1: SD Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = 1 }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == 1,
                        onClick = { selectedOption = 1 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.output_option_sdcard),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = sdDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Option 2: Custom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = 2 }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == 2,
                        onClick = { selectedOption = 2 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.output_option_custom),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.output_option_custom_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Folder name field (Phone / SD card)
                if (selectedOption == 0 || selectedOption == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = folderNameText,
                        onValueChange = { folderNameText = it },
                        label = { Text(stringResource(R.string.output_folder_name_label)) },
                        placeholder = { Text(DownloadDirHelper.DEFAULT_FOLDER_NAME) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Custom fields
                if (selectedOption == 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customPathText,
                        onValueChange = { customPathText = it },
                        label = { Text(stringResource(R.string.output_folder_path_label)) },
                        placeholder = { Text(stringResource(R.string.hint_custom_path)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFolderNameText,
                        onValueChange = { customFolderNameText = it },
                        label = { Text(stringResource(R.string.output_folder_name_label)) },
                        placeholder = { Text(DownloadDirHelper.DEFAULT_FOLDER_NAME) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // MANAGE_EXTERNAL_STORAGE hint (Android 11+)
                if ((selectedOption == 0 || selectedOption == 1) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.output_permission_required_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.output_permission_required_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.output_grant_permission),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedOption) {
                        0 -> { // Phone Storage
                            DownloadDirPreference.saveLocationType(context, DownloadLocationType.PHONE_STORAGE)
                            val name = folderNameText.ifBlank { DownloadDirHelper.DEFAULT_FOLDER_NAME }
                            DownloadDirPreference.saveFolderName(context, name)
                            DownloadDirHelper.getDownloadDir(context)
                            Toast.makeText(context, context.getString(R.string.toast_folder_set), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        1 -> { // SD Card
                            if (!hasSdCard) {
                                Toast.makeText(context, context.getString(R.string.output_sdcard_no_card), Toast.LENGTH_SHORT).show()
                            } else {
                                DownloadDirPreference.saveLocationType(context, DownloadLocationType.SD_CARD)
                                val name = folderNameText.ifBlank { DownloadDirHelper.DEFAULT_FOLDER_NAME }
                                DownloadDirPreference.saveFolderName(context, name)
                                DownloadDirHelper.getDownloadDir(context)
                                Toast.makeText(context, context.getString(R.string.toast_folder_set), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                        2 -> { // Custom
                            if (customPathText.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.toast_enter_path), Toast.LENGTH_SHORT).show()
                            } else {
                                DownloadDirPreference.saveLocationType(context, DownloadLocationType.CUSTOM)
                                DownloadDirPreference.saveCustomPath(context, customPathText)
                                val name = customFolderNameText.ifBlank { DownloadDirHelper.DEFAULT_FOLDER_NAME }
                                DownloadDirPreference.saveCustomFolderName(context, name)
                                DownloadDirHelper.getDownloadDir(context)
                                Toast.makeText(context, context.getString(R.string.toast_folder_set), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
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
