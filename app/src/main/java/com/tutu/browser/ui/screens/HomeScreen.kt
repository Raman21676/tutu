package com.tutu.browser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tutu.browser.R
import com.tutu.browser.data.local.entity.DefaultBookmarks
import com.tutu.browser.ui.components.AppIcon
import com.tutu.browser.ui.components.BookmarkCard
import com.tutu.browser.ui.components.ToggleSwitch
import com.tutu.browser.ui.components.UrlInputField
import com.tutu.browser.ui.theme.CoralRed
import com.tutu.browser.ui.theme.TutuTheme
import com.tutu.browser.ui.viewmodel.HomeViewModel
import com.tutu.browser.util.buildUrl
import com.tutu.browser.util.openInExternalBrowser

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToWeb: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val showRestoreDialog by viewModel.showRestoreDialog.collectAsState()
    val lastUrl by viewModel.lastUrl.collectAsState()
    val lastUrlInput by viewModel.lastUrlInput.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    // Use saved URL input as initial value, or the default URL
    var urlInput by remember { mutableStateOf(if (lastUrlInput.isNullOrBlank()) "https://1click.live/" else lastUrlInput!!) }
    
    // Update urlInput when lastUrlInput changes (e.g., on first load)
    LaunchedEffect(lastUrlInput) {
        val savedInput = lastUrlInput
        if (savedInput != null && urlInput == "https://1click.live/") {
            urlInput = savedInput
        }
    }
    
    // Save URL input whenever it changes
    LaunchedEffect(urlInput) {
        val savedInput = lastUrlInput
        if (urlInput != savedInput) {
            viewModel.saveUrlInput(urlInput)
        }
    }
    
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        AppIcon(
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            size = 120
        )
        
        Text(
            text = "tutu",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "lightweight browser",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // URL Input
        UrlInputField(
            value = urlInput,
            onValueChange = { urlInput = it },
            onSubmit = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled)
                    onNavigateToWeb(builtUrl)
                }
            },
            placeholder = stringResource(R.string.hint_enter_url)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Toggles - HTTPS, Fullscreen, Auto-Rotate
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
            icon = Icons.Default.ScreenRotation
        )
        
        // Theme Settings
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Background Playback Toggle
        ToggleSwitch(
            checked = settings.backgroundPlayback,
            onCheckedChange = { viewModel.setBackgroundPlayback(it) },
            title = "Background Playback",
            icon = Icons.Default.PlayCircle
        )
        
        // Floating Window Toggle
        ToggleSwitch(
            checked = settings.floatingWindow,
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (android.provider.Settings.canDrawOverlays(context)) {
                        // Permission already granted — enable it
                        viewModel.setFloatingWindow(true)
                        viewModel.setBackgroundPlayback(false)
                        context.stopService(android.content.Intent(context, com.tutu.browser.service.BackgroundPlayService::class.java))
                    } else {
                        // Permission not granted — open system settings
                        android.widget.Toast.makeText(
                            context,
                            "Please grant 'Draw over other apps', then tap the toggle again",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                } else {
                    viewModel.setFloatingWindow(false)
                    context.stopService(
                        android.content.Intent(context, com.tutu.browser.service.FloatingWindowService::class.java)
                    )
                }
            },
            title = "Floating Window",
            icon = Icons.Default.PictureInPictureAlt
        )
        
        if (settings.backgroundPlayback || settings.floatingWindow) {
            Text(
                text = if (settings.backgroundPlayback) 
                    "Audio will continue playing when app is minimized" 
                else 
                    "Video will play in floating window when minimized",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Primary Action Button
        Button(
            onClick = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled)
                    onNavigateToWeb(builtUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralRed
            ),
            enabled = urlInput.isNotBlank()
        ) {
            Text(
                text = stringResource(R.string.btn_open_webview),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Secondary Action Button
        OutlinedButton(
            onClick = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled)
                    context.openInExternalBrowser(builtUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            enabled = urlInput.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.btn_open_external),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
    }
    
    // Restore Session Dialog
    if (showRestoreDialog && lastUrl != null) {
        RestoreSessionDialog(
            url = lastUrl!!,
            onConfirm = { remember ->
                viewModel.onRestoreChoice(true, remember)
                onNavigateToWeb(lastUrl!!)
            },
            onDismiss = { remember ->
                viewModel.onRestoreChoice(false, remember)
            }
        )
    }
}

@Composable
private fun RestoreSessionDialog(
    url: String,
    onConfirm: (Boolean) -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    val domain = url.removePrefix("https://").removePrefix("http://").take(30)
    
    AlertDialog(
        onDismissRequest = { onDismiss(rememberChoice) },
        title = { Text(stringResource(R.string.dialog_restore_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_restore_message, domain))
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rememberChoice) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
            ) {
                Text(stringResource(R.string.dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(rememberChoice) }) {
                Text(stringResource(R.string.dialog_no))
            }
        }
    )
}

@Preview
@Composable
fun HomeScreenPreview() {
    TutuTheme {
        Text("Home Screen Preview")
    }
}
