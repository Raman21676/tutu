package com.nova.browser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.R
import com.nova.browser.data.local.entity.DefaultBookmarks
import com.nova.browser.data.model.SearchEngine
import com.nova.browser.data.model.SearchEngineOption
import com.nova.browser.data.model.CustomSearchEngine
import com.nova.browser.data.model.resolveSearchEngine
import com.nova.browser.ui.components.AppIcon
import com.nova.browser.ui.components.BookmarkCard
import com.nova.browser.ui.components.UrlInputField
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.NovaTheme
import com.nova.browser.ui.viewmodel.HomeViewModel
import com.nova.browser.util.buildUrl
import com.nova.browser.util.openInExternalBrowser
import com.nova.browser.ui.screens.rememberQrScanLauncher

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToWeb: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToTabs: () -> Unit = {},
    onNavigateToIncognito: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
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
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val selectedSearchEngine = resolveSearchEngine(settings.searchEngine, settings.customSearchEngines)
    
    // QR Scanner launcher
    val qrScanLauncher = rememberQrScanLauncher { scannedUrl ->
        urlInput = scannedUrl
        val builtUrl = scannedUrl.buildUrl(settings.httpsEnabled, selectedSearchEngine)
        onNavigateToWeb(builtUrl)
    }
    
    // Add bookmark dialog state
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    
    // Mark first launch as complete when the screen is shown
    LaunchedEffect(Unit) {
        if (isFirstLaunch) {
            viewModel.onFirstLaunchComplete()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with History, Tabs, Downloads, and Bookmarks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History Button
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Tabs Button (Rectangle with +)
            IconButton(
                onClick = onNavigateToTabs,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tab,
                    contentDescription = "Tabs",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Downloads Button
            IconButton(
                onClick = onNavigateToDownloads,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Downloads",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Bookmarks Button
            IconButton(
                onClick = onNavigateToBookmarks,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Bookmarks",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Incognito Button
            IconButton(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        val builtUrl = urlInput.buildUrl(settings.httpsEnabled, selectedSearchEngine)
                        onNavigateToIncognito(builtUrl)
                    } else {
                        onNavigateToIncognito("https://www.google.com")
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Incognito",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Settings Button
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Logo
        AppIcon(
            modifier = Modifier.padding(top = 4.dp, bottom = 0.dp),
            size = 120
        )
        
        Text(
            text = "NOVA",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Fast & Private",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // URL Input with QR Scanner
        UrlInputField(
            value = urlInput,
            onValueChange = { urlInput = it },
            onSubmit = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled, selectedSearchEngine)
                    onNavigateToWeb(builtUrl)
                }
            },
            onScanQr = { qrScanLauncher.launch() },
            placeholder = stringResource(R.string.hint_enter_url)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Primary Action Button
        Button(
            onClick = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled, selectedSearchEngine)
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
                text = "Open",
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Secondary Action Button
        OutlinedButton(
            onClick = {
                if (urlInput.isNotBlank()) {
                    val builtUrl = urlInput.buildUrl(settings.httpsEnabled, selectedSearchEngine)
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bookmarks Section
        if (bookmarks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row {
                    // View all bookmarks button
                    TextButton(onClick = onNavigateToBookmarks) {
                        Text("View All")
                    }
                    
                    // Add bookmark button
                    IconButton(
                        onClick = { showAddBookmarkDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bookmark",
                            tint = CoralRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bookmarks grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        onClick = {
                            urlInput = bookmark.url
                            val builtUrl = bookmark.url.buildUrl(settings.httpsEnabled, selectedSearchEngine)
                            onNavigateToWeb(builtUrl)
                        },
                        onDelete = {
                            viewModel.removeBookmark(bookmark.id)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
    }
    
    // Restore Session Dialog
    if (showRestoreDialog && lastUrl != null) {
        RestoreSessionDialog(
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RestoreSessionDialog(
    onConfirm: (Boolean) -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    
    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = { onDismiss(rememberChoice) },
        modifier = Modifier.padding(24.dp)
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Restore Website",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message
                Text(
                    text = "Open last visited website?",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons Row
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button (outlined)
                    OutlinedButton(
                        onClick = { onDismiss(rememberChoice) },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CoralRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = CoralRed
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                    
                    // OK Button (filled)
                    Button(
                        onClick = { onConfirm(rememberChoice) },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralRed,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text(
                            "OK",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddBookmarkDialog(
    currentUrl: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(currentUrl) }
    
    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Add Bookmark",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title input
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // URL input
                androidx.compose.material3.OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { onConfirm(title, url) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    NovaTheme {
        Text("Home Screen Preview")
    }
}
