package com.nova.browser.ui.screens

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.nova.browser.util.QrGenerator

// =============================================================================
// QR SCANNER LAUNCHER
// =============================================================================

/**
 * Remembers an Activity Result launcher that opens the ZXing camera scanner.
 *
 * Usage in your WebScreen / composable:
 *
 *   val qrScanLauncher = rememberQrScanLauncher { scannedUrl ->
 *       viewModel.loadUrl(scannedUrl)
 *   }
 *   // Then trigger it:
 *   Button(onClick = { qrScanLauncher.launch() }) { Text("Scan QR") }
 */
@Composable
fun rememberQrScanLauncher(onResult: (String) -> Unit): QrScanLauncher {
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result: ScanIntentResult ->
        result.contents?.let { scanned ->
            if (scanned.isNotBlank()) onResult(scanned)
        }
    }

    return remember(launcher) {
        QrScanLauncher {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Point at a QR code")
                setBeepEnabled(true)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
                setCaptureActivity(QrCaptureActivity::class.java)
            }
            launcher.launch(options)
        }
    }
}

/** Thin wrapper so callers just call .launch() without knowing ScanOptions. */
class QrScanLauncher(private val launchFn: () -> Unit) {
    fun launch() = launchFn()
}

// =============================================================================
// QR SHARE DIALOG  (shows a generated QR for the current page URL)
// =============================================================================

/**
 * QrShareDialog
 *
 * Shows a bottom-sheet-style Dialog with a QR code for [currentUrl].
 * The user can scan it with another device to open the same page.
 *
 * Usage:
 *
 *   var showQrDialog by remember { mutableStateOf(false) }
 *
 *   if (showQrDialog) {
 *       QrShareDialog(
 *           currentUrl = webViewModel.currentUrl,
 *           onDismiss = { showQrDialog = false }
 *       )
 *   }
 */
@Composable
fun QrShareDialog(
    currentUrl: String,
    onDismiss: () -> Unit
) {
    // Generate the bitmap once, off the main thread concern is minimal for
    // 512px QR — ZXing is fast. For safety we use produceState.
    val qrBitmap: Bitmap? by produceState<Bitmap?>(initialValue = null, currentUrl) {
        value = QrGenerator.generate(currentUrl, sizePx = 512)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // ── Header ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Share Page",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Subtitle ─────────────────────────────────────────────
                    Text(
                        text = "Scan this QR with another device",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── QR Code ───────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            qrBitmap != null -> {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR code for $currentUrl",
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize()
                                )
                            }
                            currentUrl.isBlank() -> {
                                Text(
                                    text = "No URL to encode",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            else -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── URL label ─────────────────────────────────────────────
                    Text(
                        text = currentUrl,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Close button ─────────────────────────────────────────
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
