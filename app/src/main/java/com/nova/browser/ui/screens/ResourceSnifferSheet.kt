package com.nova.browser.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.service.DownloadService
import com.nova.browser.util.DetectedResource
import com.nova.browser.util.ResourceType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceSnifferSheet(
    resources: List<DetectedResource>,
    onDismiss: () -> Unit,
    onDownload: (DetectedResource) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        "All" to resources.size,
        "Video" to resources.count { it.type == ResourceType.VIDEO || it.type == ResourceType.M3U8 || it.type == ResourceType.BLOB },
        "Audio" to resources.count { it.type == ResourceType.AUDIO },
        "Image" to resources.count { it.type == ResourceType.IMAGE },
        "Other" to resources.count { it.type == ResourceType.OTHER }
    ).filter { it.second > 0 }

    val filteredResources = when (selectedTab) {
        0 -> resources
        1 -> resources.filter { it.type == ResourceType.VIDEO || it.type == ResourceType.M3U8 || it.type == ResourceType.BLOB }
        2 -> resources.filter { it.type == ResourceType.AUDIO }
        3 -> resources.filter { it.type == ResourceType.IMAGE }
        4 -> resources.filter { it.type == ResourceType.OTHER }
        else -> resources
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Resources",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${resources.size} found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            if (tabs.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, (label, count) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text("$label ($count)") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Resource list
            if (filteredResources.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No resources in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredResources, key = { it.url }) { resource ->
                        ResourceItemCard(
                            resource = resource,
                            onDownload = { onDownload(resource) },
                            onOpen = { openResourceUrl(context, resource.url) },
                            onCopy = { copyUrlToClipboard(context, resource.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceItemCard(
    resource: DetectedResource,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    val typeColor = when (resource.type) {
        ResourceType.VIDEO, ResourceType.M3U8, ResourceType.BLOB -> MaterialTheme.colorScheme.error
        ResourceType.AUDIO -> MaterialTheme.colorScheme.primary
        ResourceType.IMAGE -> MaterialTheme.colorScheme.secondary
        ResourceType.OTHER -> MaterialTheme.colorScheme.outline
    }

    val typeIcon = when (resource.type) {
        ResourceType.VIDEO, ResourceType.M3U8, ResourceType.BLOB -> Icons.Default.PlayCircle
        ResourceType.AUDIO -> Icons.Default.MusicNote
        ResourceType.IMAGE -> Icons.Default.Image
        ResourceType.OTHER -> Icons.Default.InsertDriveFile
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.type.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    resource.extension.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Actions
            Row {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy URL",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun openResourceUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
    }
}

private fun copyUrlToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
}
