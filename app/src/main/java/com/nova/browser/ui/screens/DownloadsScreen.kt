package com.nova.browser.ui.screens

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.viewmodel.DownloadsViewModel
import com.nova.browser.util.DownloadDirHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    
    // Multi-selection state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectionMode) {
                        Text("${selectedItems.size} selected")
                    } else {
                        Text("Downloads")
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { 
                            selectionMode = false
                            selectedItems.clear()
                        }) {
                            Icon(Icons.Default.Close, "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        // Select all button
                        IconButton(onClick = {
                            if (selectedItems.size == downloads.size) {
                                selectedItems.clear()
                            } else {
                                selectedItems.clear()
                                selectedItems.addAll(downloads.map { it.id })
                            }
                        }) {
                            Icon(
                                if (selectedItems.size == downloads.size) 
                                    Icons.Default.Deselect 
                                else 
                                    Icons.Default.SelectAll, 
                                "Select all"
                            )
                        }
                        // Delete selected button
                        if (selectedItems.isNotEmpty()) {
                            IconButton(onClick = { showBulkDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, "Delete selected", tint = CoralRed)
                            }
                        }
                    } else {
                        if (downloads.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, "Clear all")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                EmptyDownloadsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        val isSelected = selectedItems.contains(item.id)
                        DownloadItemCard(
                            item = item,
                            statusText = viewModel.getStatusText(item.status),
                            statusColor = viewModel.getStatusColor(item.status),
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            viewModel = viewModel,
                            onCancel = { viewModel.cancelDownload(item.id) },
                            onDelete = { showDeleteConfirm = item.id },
                            onOpenFile = { openDownloadedFile(context, item) },
                            onOpenLocation = { openFileLocation(context, item) },
                            onLongPress = {
                                selectionMode = true
                                selectedItems.add(item.id)
                            },
                            onSelectToggle = {
                                if (selectedItems.contains(item.id)) {
                                    selectedItems.remove(item.id)
                                    if (selectedItems.isEmpty()) {
                                        selectionMode = false
                                    }
                                } else {
                                    selectedItems.add(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Clear all dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Downloads") },
            text = { Text("Choose what to clear:") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            viewModel.clearCompletedDownloads()
                            showClearDialog = false
                        }
                    ) {
                        Text("Completed only")
                    }
                    TextButton(
                        onClick = {
                            viewModel.clearAllDownloads()
                            showClearDialog = false
                        }
                    ) {
                        Text("All downloads", color = CoralRed)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single delete confirmation
    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Download") },
            text = { Text("Are you sure you want to delete this download record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownload(id)
                        showDeleteConfirm = null
                    }
                ) {
                    Text("Delete", color = CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Bulk delete confirmation
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${selectedItems.size} items") },
            text = { Text("Are you sure you want to delete these download records?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedItems.forEach { id ->
                            viewModel.deleteDownload(id)
                        }
                        selectedItems.clear()
                        selectionMode = false
                        showBulkDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    selectionMode: Boolean,
    viewModel: DownloadsViewModel,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenLocation: () -> Unit,
    onLongPress: () -> Unit,
    onSelectToggle: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val isActive = item.status == DownloadEntity.STATUS_PENDING || 
                   item.status == DownloadEntity.STATUS_RUNNING ||
                   item.status == DownloadEntity.STATUS_PAUSED
    val isCompleted = item.status == DownloadEntity.STATUS_SUCCESSFUL
    val isPaused = item.status == DownloadEntity.STATUS_PAUSED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection checkbox or file icon
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectToggle() },
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    // File icon or Circular Progress
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onLongPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive || isPaused) {
                            CircularProgressIndicator(
                                progress = { item.progress / 100f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "${item.progress}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            if (selectionMode) onSelectToggle()
                        }
                ) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status indicator
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Action buttons
                when {
                    isActive || item.status == DownloadEntity.STATUS_PAUSED -> {
                        Row(modifier = Modifier.wrapContentWidth()) {
                            // Pause/Resume button
                            if (item.status == DownloadEntity.STATUS_PAUSED) {
                                IconButton(
                                    onClick = { viewModel.resumeDownload(item.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.pauseDownload(item.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Cancel button
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = CoralRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    isCompleted -> {
                        // Three action buttons in a row
                        Row(modifier = Modifier.wrapContentWidth()) {
                            // Open file button
                            IconButton(
                                onClick = onOpenFile,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open file",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Open folder button
                            IconButton(
                                onClick = onOpenLocation,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "Open location",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Delete button
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Failed or other status - show delete button
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Progress details for active downloads
            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.totalBytes > 0) 
                            "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
                        else 
                            "Downloading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${item.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Linear progress bar
                Spacer(modifier = Modifier.height(4.dp))
                if (item.totalBytes > 0) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    // Indeterminate bar when total size unknown
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No downloads yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Downloaded files will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Open the downloaded file with appropriate app
 */
private fun openDownloadedFile(context: Context, item: DownloadEntity) {
    val file = when {
        item.filePath.isNotEmpty() -> File(item.filePath)
        else -> {
            val downloadDir = DownloadDirHelper.getDownloadDir(context)
            val f = File(downloadDir, item.fileName)
            if (f.exists()) f else File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                item.fileName
            )
        }
    }

    if (!file.exists()) {
        Toast.makeText(context, "File not found: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        return
    }

    val mimeType = when {
        file.name.lowercase().endsWith(".apk") -> "application/vnd.android.package-archive"
        item.mimeType.isNotEmpty() && item.mimeType != "application/octet-stream" -> item.mimeType
        else -> getMimeTypeFromFileName(file.name) ?: "*/*"
    }

    // ── Attempt 1: FileProvider with explicit URI grants (required for chooser) ──
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // KEY FIX: Grant permission explicitly to every app that can handle this
        val resolvedApps = context.packageManager
            .queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in resolvedApps) {
            context.grantUriPermission(
                info.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val chooser = Intent.createChooser(viewIntent, "Open with")
        context.startActivity(chooser)
        Log.d("DownloadsScreen", "Opened file via FileProvider+explicit grants")
        return

    } catch (e: Exception) {
        Log.w("DownloadsScreen", "FileProvider attempt failed: ${e.message}")
    }

    // ── Attempt 2: file:// URI — works on Android ≤ 7.1.1 (API 25) ──
    try {
        @Suppress("DEPRECATION")
        val fileUri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open with")
        context.startActivity(chooser)
        Log.d("DownloadsScreen", "Opened file via file:// URI")
        return
    } catch (e: Exception) {
        Log.w("DownloadsScreen", "file:// attempt failed: ${e.message}")
    }

    Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
}

/**
 * Open the file manager at the download location.
 *
 * Tries multiple strategies (most reliable first) and falls back
 * to opening the file directly or copying the path to clipboard.
 */
private fun openFileLocation(context: Context, item: DownloadEntity) {
    Log.d("DownloadsScreen", "=== openFileLocation clicked for: ${item.fileName}, path: ${item.filePath}")

    try {
        // Determine the actual file and its parent folder
        val file = when {
            item.filePath.isNotEmpty() -> File(item.filePath)
            else -> {
                val downloadDir = DownloadDirHelper.getDownloadDir(context)
                val novaFile = File(downloadDir, item.fileName)
                if (novaFile.exists()) novaFile
                else File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), item.fileName)
            }
        }

        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val parentDir = file.parentFile ?: DownloadDirHelper.getDownloadDir(context)
        Log.d("DownloadsScreen", "Opening folder: ${parentDir.absolutePath}")
        Toast.makeText(context, "Opening folder...", Toast.LENGTH_SHORT).show()

        // ── Strategy 1: DocumentsUI with exact path derived from file location ──
        try {
            val docUri = DownloadDirHelper.pathToDocumentsUiUri(parentDir.absolutePath)
            if (docUri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d("DownloadsScreen", "Opened folder via DocumentsUI: $docUri")
                    return
                }
            }
        } catch (e: Exception) {
            Log.d("DownloadsScreen", "DocumentsUI failed: ${e.message}")
        }

        // ── Strategy 2: DocumentsUI with specific package (Google Files / DocumentsUI) ──
        try {
            val docUri = DownloadDirHelper.pathToDocumentsUiUri(parentDir.absolutePath)
            if (docUri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("com.google.android.documentsui")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d("DownloadsScreen", "Opened folder via Google DocumentsUI")
                    return
                }
            }
        } catch (e: Exception) {
            Log.d("DownloadsScreen", "Google DocumentsUI failed: ${e.message}")
        }

        // ── Strategy 3: Try com.android.documentsui (AOSP variant) ──
        try {
            val docUri = DownloadDirHelper.pathToDocumentsUiUri(parentDir.absolutePath)
            if (docUri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("com.android.documentsui")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d("DownloadsScreen", "Opened folder via AOSP DocumentsUI")
                    return
                }
            }
        } catch (e: Exception) {
            Log.d("DownloadsScreen", "AOSP DocumentsUI failed: ${e.message}")
        }

        // ── Strategy 4: file:// with Uri.fromFile() — properly encodes spaces ──
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(parentDir), "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d("DownloadsScreen", "Opened folder via file:// VIEW (Uri.fromFile)")
                return
            }
        } catch (e: Exception) {
            Log.d("DownloadsScreen", "file:// VIEW failed: ${e.message}")
        }

        // ── Strategy 5: Open file directly with explicit URI permission grants ──
        try {
            val mimeType = when {
                file.name.lowercase().endsWith(".apk") -> "application/vnd.android.package-archive"
                item.mimeType.isNotEmpty() && item.mimeType != "application/octet-stream" -> item.mimeType
                else -> getMimeTypeFromFileName(file.name) ?: "*/*"
            }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Explicitly grant permission to every app before showing chooser
            context.packageManager
                .queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { info ->
                    context.grantUriPermission(
                        info.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            context.startActivity(Intent.createChooser(viewIntent, "Open with"))
            Log.d("DownloadsScreen", "Opened file with explicit URI grants")
            return
        } catch (e: Exception) {
            Log.w("DownloadsScreen", "FileProvider+grants failed: ${e.message}")
            // Fallback: file:// URI
            try {
                val mimeType = when {
                    file.name.lowercase().endsWith(".apk") -> "application/vnd.android.package-archive"
                    else -> getMimeTypeFromFileName(file.name) ?: "*/*"
                }
                @Suppress("DEPRECATION")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open with"))
                Log.d("DownloadsScreen", "Opened file via file:// fallback")
                return
            } catch (e2: Exception) {
                Log.w("DownloadsScreen", "file:// fallback failed: ${e2.message}")
            }
        }

        // ── Strategy 6: System Downloads app ──
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("DownloadsScreen", "Opened system Downloads app")
            return
        } catch (e: Exception) {
            Log.d("DownloadsScreen", "System Downloads app failed: ${e.message}")
        }

        // ── Final Fallback: copy path to clipboard ──
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("File Location", parentDir.absolutePath)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied path: ${parentDir.absolutePath}", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        Log.e("DownloadsScreen", "Error opening location: ${e.message}")
        Toast.makeText(context, "Cannot open file location", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Get MIME type from file extension
 */
private fun getMimeTypeFromFileName(fileName: String): String? {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "apk" -> "application/vnd.android.package-archive"
        else -> if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else null
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
