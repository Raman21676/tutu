package com.nova.browser.ui.screens

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nova.browser.data.repository.TabManager
import com.nova.browser.domain.repository.HistoryRepository
import kotlinx.coroutines.launch
import com.nova.browser.ui.components.IndeterminateProgressIndicator
import com.nova.browser.ui.components.LinearProgressIndicatorCustom
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.viewmodel.WebViewModel
import com.nova.browser.domain.repository.DownloadRepository
import com.nova.browser.util.DarkModeInjector
import com.nova.browser.util.ReadingModeInjector
import com.nova.browser.util.AdBlocker
import com.nova.browser.util.AdBlockCosmeticInjector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    viewModel: WebViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    pipEnabled: Boolean = false,
    historyRepository: HistoryRepository? = null,
    downloadRepository: DownloadRepository? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val fullscreen by viewModel.fullscreen.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val desktopMode by viewModel.desktopMode.collectAsState()
    
    // Get AdBlocker instance
    val adBlocker = remember { AdBlocker.getInstance() }
    
    // Tab manager for tracking tab state
    val tabManager = remember { TabManager.getInstance() }
    val tabs by tabManager.tabs.collectAsState()
    val currentTabId by tabManager.currentTabId.collectAsState()
    val currentTab = tabs.find { it.id == currentTabId }
    
    // Get coroutine scope for history operations
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Single WebView reference
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Fullscreen video handling
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    // Feature states
    var isReadingMode by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as Activity
    
    // QR Scanner launcher
    val qrScanLauncher = rememberQrScanLauncher { scannedUrl ->
        webView?.loadUrl(scannedUrl)
    }
    
    // Pending download info for permission handling
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    
    // Permission launcher for storage permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownload?.let { download ->
                executeDownload(context, download, downloadRepository)
                pendingDownload = null
            }
        } else {
            Toast.makeText(context, "Storage permission required for downloads", Toast.LENGTH_LONG).show()
        }
    }
    
    // Helper function to check and request permission before download
    fun checkAndDownload(download: PendingDownload, downloadRepository: DownloadRepository? = null) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ - use app-specific directory without permission
                executeDownload(context, download, downloadRepository)
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                executeDownload(context, download, downloadRepository)
            }
            else -> {
                // Store pending download and request permission
                pendingDownload = download
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    // Helper function to enter fullscreen mode
    fun enterFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
    
    // Helper function to exit fullscreen mode
    fun exitFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    }
    
    // Helper function to enter PiP mode (only on Android 8.0+)
    fun enterPipMode() {
        if (!pipEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use native PiP on Android 8.0+
            try {
                val aspectRatio = Rational(16, 9)
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                activity.enterPictureInPictureMode(pipParams)
                Log.d("WebScreen", "Entered native PiP mode")
            } catch (e: Exception) {
                Log.e("WebScreen", "Error entering PiP mode", e)
            }
        } else {
            // PiP not supported on Android 7.1
            Toast.makeText(context, "Picture-in-Picture requires Android 8.0 or higher", Toast.LENGTH_LONG).show()
            Log.d("WebScreen", "PiP not supported on Android ${Build.VERSION.SDK_INT}")
        }
    }
    
    // Handle back button
    BackHandler {
        when {
            customView != null -> {
                // Exit fullscreen video
                customViewCallback?.onCustomViewHidden()
                customView?.let { view ->
                    val decorView = activity.window.decorView as FrameLayout
                    decorView.removeView(view)
                }
                customView = null
                customViewCallback = null
                
                // If app is in fullscreen mode, stay in fullscreen
                // Otherwise, exit fullscreen
                if (fullscreen) {
                    enterFullscreen()
                } else {
                    exitFullscreen()
                }
            }
            webView?.canGoBack() == true -> {
                webView?.goBack()
            }
            else -> {
                onNavigateBack()
            }
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!fullscreen) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = state.title.takeIf { it.isNotBlank() } ?: "Loading...",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.url.getDomainName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        },
                        actions = {
                            // New Tab button - allows opening new tab without leaving current video
                            IconButton(onClick = {
                                tabManager.addTab()
                                onNavigateToTabs()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Tab"
                                )
                            }
                            
                            // Reading Mode toggle
                            IconButton(onClick = {
                                webView?.let { isReadingMode = ReadingModeInjector.toggle(it, isReadingMode) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Reading Mode",
                                    tint = if (isReadingMode) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // PiP button (show if enabled - works on Android 8.0+)
                            if (pipEnabled) {
                                IconButton(onClick = { enterPipMode() }) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPicture,
                                        contentDescription = "Picture-in-Picture"
                                    )
                                }
                            }

                            // Ad Block shield — shows live blocked-request count for this page
                            if (adBlockEnabled) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    IconButton(onClick = {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (blockedCount > 0) "$blockedCount ads blocked on this page" else "Ad blocking active",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Ad blocking active",
                                            tint = if (blockedCount > 0)
                                                androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                    }
                                    if (blockedCount > 0) {
                                        Surface(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(end = 6.dp, top = 4.dp),
                                            shape = CircleShape,
                                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (blockedCount > 99) "99" else blockedCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // History button
                            IconButton(onClick = onNavigateToHistory) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History"
                                )
                            }
                            
                            // Downloads button
                            IconButton(onClick = onNavigateToDownloads) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloads"
                                )
                            }
                            
                            // Bookmark button
                            IconButton(onClick = {
                                // Add current page to bookmarks
                                if (state.url.isNotBlank() && state.title.isNotBlank()) {
                                    tabManager.currentTab?.let { currentTab ->
                                        // We would need to add bookmark functionality here
                                        // For now, just show a toast
                                        Toast.makeText(
                                            context,
                                            "Bookmarked: ${state.title}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Bookmark"
                                )
                            }

                            // Share current page via Android share sheet
                            IconButton(onClick = {
                                if (state.url.isNotBlank()) {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, state.url)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, state.title.ifBlank { state.url })
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share page"
                                )
                            }

                            // Find in Page toggle
                            IconButton(onClick = {
                                showFindBar = !showFindBar
                                if (!showFindBar) {
                                    webView?.clearMatches()
                                    findQuery = ""
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Find in page",
                                    tint = if (showFindBar) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    
                    when {
                        state.isLoading && state.progress < 100 -> {
                            LinearProgressIndicatorCustom(
                                progress = state.progress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        state.isLoading -> {
                            IndeterminateProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Find-in-Page bar
                    if (showFindBar) {
                        androidx.compose.material3.OutlinedTextField(
                            value = findQuery,
                            onValueChange = { q ->
                                findQuery = q
                                if (q.isNotBlank()) webView?.findAllAsync(q)
                                else webView?.clearMatches()
                            },
                            placeholder = { Text("Find in page...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { webView?.findNext(false) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                                    }
                                    IconButton(onClick = { webView?.findNext(true) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                                    }
                                    IconButton(onClick = {
                                        showFindBar = false
                                        findQuery = ""
                                        webView?.clearMatches()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!fullscreen) {
                BottomNavigationBar(
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    tabCount = tabs.size,
                    onBack = { webView?.goBack() },
                    onForward = { webView?.goForward() },
                    onRefresh = { webView?.reload() },
                    onHome = onNavigateHome,
                    onTabs = onNavigateToTabs,
                    onScanQr = { qrScanLauncher.launch() },
                    onShowQr = { showQrDialog = true }
                )
            }
        }
    ) { paddingValues ->
        // CRITICAL FIX: In fullscreen mode, don't apply padding
        val boxModifier = if (fullscreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize().padding(paddingValues)
        }
        
        Box(modifier = boxModifier) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        
                        // CRITICAL FIX: Force hardware rendering for SPA navigation
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowFileAccess = true
                            allowContentAccess = true
                            
                            // Handle window.open()
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                            
                            // Critical for video playback (YouTube, TikTok)
                            mediaPlaybackRequiresUserGesture = false
                            
                            // Enable WebGL and media features for TikTok
                            setNeedInitialFocus(true)
                            
                            // Additional settings for compatibility
                            loadsImagesAutomatically = true
                            blockNetworkImage = false
                            
                            // Performance optimizations for TikTok
                            setRenderPriority(WebSettings.RenderPriority.HIGH)
                            
                            // CRITICAL FIX: Strip WebView markers from User Agent
                            // TikTok detects WebView by "; wv" and "Version/4.0" in UA
                            val defaultUA = userAgentString ?: ""
                            val cleanUA = defaultUA
                                .replace("; wv", "")
                                .replace("Version/4.0 ", "")
                            userAgentString = cleanUA
                        }
                        
                        // Enable hardware acceleration for WebGL/Canvas
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        // Enable third-party cookies (critical for TikTok, Facebook)
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        
                        // Add JavaScript interface for download handling
                        addJavascriptInterface(
                            DownloadJsInterface(ctx, downloadRepository) { url, fileName, mimeType ->
                                val download = PendingDownload(
                                    url = url,
                                    fileName = fileName,
                                    mimeType = mimeType,
                                    userAgent = settings.userAgentString
                                )
                                checkAndDownload(download, downloadRepository)
                            },
                            "AndroidDownload"
                        )
                        
                        // Handle file downloads
                        setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                            Log.d("WebScreen", "=== DownloadListener triggered: $downloadUrl")
                            // Validate URL
                            if (downloadUrl.isBlank() || downloadUrl.startsWith("javascript:") || downloadUrl.startsWith("blob:")) {
                                android.widget.Toast.makeText(ctx, "Cannot download this file type", android.widget.Toast.LENGTH_SHORT).show()
                                return@setDownloadListener
                            }
                            
                            val uri = android.net.Uri.parse(downloadUrl)
                            if (uri.scheme != "http" && uri.scheme != "https") {
                                android.widget.Toast.makeText(ctx, "Invalid download URL", android.widget.Toast.LENGTH_SHORT).show()
                                return@setDownloadListener
                            }
                            
                            // Determine file name
                            val fileName = if (!contentDisposition.isNullOrBlank()) {
                                android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                            } else {
                                // Extract filename from URL or use default
                                val urlPath = uri.lastPathSegment ?: "download"
                                if (urlPath.contains(".")) urlPath else "$urlPath.pdf"
                            }
                            
                            val cookie = cookieManager.getCookie(downloadUrl)
                            val download = PendingDownload(
                                url = downloadUrl,
                                fileName = fileName,
                                userAgent = userAgent,
                                cookies = cookie,
                                mimeType = mimetype,
                                referer = this.url  // current WebView page URL as HTTP Referer
                            )
                            
                            // Use checkAndDownload to handle permission
                            checkAndDownload(download, downloadRepository)
                        }
                        
                        webViewClient = object : WebViewClient() {
                            private val TAG = "WebViewClient"
                            
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()
                                Log.d(TAG, "=== shouldOverrideUrlLoading: $url")
                                
                                // Block native app schemes (TikTok snssdk)
                                if (url.startsWith("snssdk")) {
                                    Log.d(TAG, "Blocked snssdk: $url")
                                    return true
                                }
                                
                                // Block app-store redirect URLs (TikTok install prompts)
                                // These don't affect feed pagination
                                if (url.contains("onelink.me") || 
                                    url.contains("apps.apple.com") || 
                                    url.contains("play.google.com/store")) {
                                    Log.d(TAG, "Blocked app redirect: $url")
                                    return true
                                }
                                
                                // Handle intent:// URLs
                                if (url.startsWith("intent://")) {
                                    return try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        if (fallbackUrl != null) view.loadUrl(fallbackUrl)
                                        true
                                    } catch (e: Exception) { true }
                                }
                                
                                // Handle direct file downloads by extension ONLY.
                                // Do NOT intercept by "/download" path — that grabs pages before
                                // the server response, causing HTML to be downloaded instead of the file.
                                // setDownloadListener handles server-sent Content-Disposition: attachment.
                                val downloadExtensions = listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".rar", ".apk", ".mp3", ".mp4", ".epub", ".csv")
                                val lowerUrl = url.lowercase()
                                if (downloadExtensions.any { lowerUrl.endsWith(it) }) {
                                    val fileName = android.webkit.URLUtil.guessFileName(url, null, null)
                                    val cookie = cookieManager.getCookie(url)
                                    val download = PendingDownload(
                                        url = url,
                                        fileName = fileName,
                                        userAgent = request.requestHeaders?.get("User-Agent") ?: view.settings.userAgentString,
                                        cookies = cookie,
                                        referer = view.url  // current page URL as HTTP Referer
                                    )
                                    checkAndDownload(download, downloadRepository)
                                    return true
                                }
                                
                                // Handle custom URL schemes (whatsapp, fb, etc)
                                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("intent://")) {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        view.context.startActivity(intent)
                                        true
                                    } catch (e: Exception) {
                                        // App not installed, let WebView handle it
                                        false
                                    }
                                }
                                
                                // Let WebView handle all http/https - this includes SPA navigation
                                return false
                            }
                            
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                Log.d(TAG, "Page started: $url")
                                url?.let { 
                                    viewModel.onPageStarted(it)
                                }
                                // Reset reading mode and blocked counter on each new page
                                isReadingMode = false
                                viewModel.resetBlockedCount()
                                
                                // CRITICAL: Zero JS injection for TikTok!
                                // User's Attempt 3 proved: no JS injection = feed works beyond 2 reels
                                // ANY navigator/window modification is detected by webmssdk.js
                            }
                            
                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                super.onPageCommitVisible(view, url)
                                // NOTE: Disabled navigator spoofing - was breaking TikTok feed
                                // UserAgent is already cleaned in WebSettings
                                Log.d(TAG, "onPageCommitVisible: $url")
                            }
                            
                            override fun onPageFinished(view: WebView, url: String) {
                                Log.d(TAG, "Page finished: $url")
                                viewModel.onPageFinished(url)
                                viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                
                                // Force proper redraw within Compose's AndroidView
                                view.requestLayout()
                                view.post { view.invalidate() }
                                
                                // Inject download interceptor + cosmetic ad filter (safe for non-TikTok)
                                if (!url.contains("tiktok")) {
                                    injectDownloadInterceptor(view)
                                    AdBlockCosmeticInjector.inject(view)
                                }
                            }
                            
                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                // Log resources for debugging
                                if (url?.contains("tiktok") == true || url?.contains("googlevideo") == true) {
                                    Log.d(TAG, "Loading resource: $url")
                                }
                            }
                            
                            override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                                // Check if ad blocking is enabled
                                if (adBlockEnabled && request?.url != null) {
                                    val url = request.url.toString()
                                    if (adBlocker.shouldBlock(url)) {
                                        // Increment badge counter (thread-safe — called from IO thread)
                                        viewModel.incrementBlockedCount()
                                        // Return empty response for blocked ads/trackers
                                        return android.webkit.WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            java.io.ByteArrayInputStream("".toByteArray())
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                            
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                // CRITICAL: Update URL state for SPA navigation (YouTube, TikTok)
                                url?.let {
                                    Log.d(TAG, "History updated: $url (reload: $isReload)")
                                    viewModel.onUrlChanged(it)
                                    viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                }
                                // NOTE: Removed re-injection on navigation - breaks TikTok feed
                                // TikTok's SPA navigation needs clean event handling
                            }
                            
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                Log.e(TAG, "Error loading page: $errorCode - $description")
                                viewModel.onReceivedError(errorCode, description ?: "Unknown error")
                            }
                        }
                        
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.onProgressChanged(newProgress)
                            }
                            
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                title?.let { pageTitle ->
                                    viewModel.onReceivedTitle(pageTitle)
                                    // Save to history
                                    view?.url?.let { url ->
                                        historyRepository?.let { repo ->
                                            coroutineScope.launch {
                                                repo.addOrUpdateHistory(url, pageTitle)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                viewModel.onReceivedIcon(icon)
                            }
                            
                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                customView?.let {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                
                                customView = view
                                customViewCallback = callback
                                
                                // Add to decor view
                                val decorView = activity.window.decorView as FrameLayout
                                decorView.addView(view, FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                ))
                                
                                enterFullscreen()
                                Log.d("WebScreen", "Entered fullscreen video")
                            }
                            
                            override fun onHideCustomView() {
                                customView?.let { view ->
                                    val decorView = activity.window.decorView as FrameLayout
                                    decorView.removeView(view)
                                }
                                customView = null
                                customViewCallback = null
                                
                                if (fullscreen) {
                                    enterFullscreen()
                                } else {
                                    exitFullscreen()
                                }
                                
                                Log.d("WebScreen", "Exited fullscreen video")
                            }
                            
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                // Handle window.open() - usually for downloads or popups
                                resultMsg?.let { msg ->
                                    val transport = msg.obj as? WebView.WebViewTransport
                                    transport?.let {
                                        // Get the URL that would be loaded in the new window
                                        val newWebView = WebView(ctx).apply {
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                    url?.let { newUrl ->
                                                        Log.d("WebScreen", "=== onCreateWindow onPageStarted: $newUrl")
                                                        
                                                        // Check if this is a download link
                                                        val downloadExtensions = listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".rar", ".apk")
                                                        val lowerUrl = newUrl.lowercase()
                                                        
                                                        if (downloadExtensions.any { lowerUrl.contains(it) } || 
                                                            lowerUrl.contains("download") ||
                                                            newUrl.startsWith("blob:")) {
                                                            // This is a download - cancel loading and use DownloadManager
                                                            view?.stopLoading()
                                                            
                                                            val fileName = newUrl.substringAfterLast("/", "download").takeIf { it.isNotEmpty() && it.contains(".") } 
                                                                ?: newUrl.substringBeforeLast("/").substringAfterLast("/", "download").takeIf { it.isNotEmpty() }
                                                                ?: "download.pdf"
                                                            val download = PendingDownload(
                                                                url = newUrl,
                                                                fileName = fileName,
                                                                userAgent = settings.userAgentString
                                                            )
                                                            checkAndDownload(download, downloadRepository)
                                                        } else {
                                                            // Not a download - load in current WebView
                                                            webView?.loadUrl(newUrl)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        it.webView = newWebView
                                        msg.sendToTarget()
                                    }
                                }
                                return true
                            }
                        }
                        

                        loadUrl(state.url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { wv ->
                    webView = wv
                    // Apply desktop/mobile UA switch reactively when the setting changes
                    val desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    val currentUA = wv.settings.userAgentString ?: ""
                    if (desktopMode && !currentUA.contains("X11; Linux x86_64")) {
                        wv.settings.userAgentString = desktopUA
                        wv.reload()
                    } else if (!desktopMode && currentUA.contains("X11; Linux x86_64")) {
                        // Restore cleaned mobile UA
                        val defaultUA = android.webkit.WebSettings.getDefaultUserAgent(wv.context)
                        wv.settings.userAgentString = defaultUA.replace("; wv", "").replace("Version/4.0 ", "")
                        wv.reload()
                    }
                }
            )
            
            if (state.error != null) {
                ErrorView(
                    error = state.error!!,
                    onRetry = { webView?.reload() }
                )
            }
        }
    }
    
    // QR Share Dialog
    if (showQrDialog) {
        QrShareDialog(
            currentUrl = state.url,
            onDismiss = { showQrDialog = false }
        )
    }
}

// Data class to hold pending download information
data class PendingDownload(
    val url: String,
    val fileName: String,
    val userAgent: String? = null,
    val cookies: String? = null,
    val mimeType: String? = null,
    val referer: String? = null  // HTTP Referer: the page that triggered the download
)

// Execute the download using custom DownloadService (OkHttp with pause/resume)
// Downloads are saved to the "Nova Downloads" folder (or user-configured folder)
private fun executeDownload(
    context: android.content.Context, 
    download: PendingDownload,
    downloadRepository: DownloadRepository? = null
) {
    downloadRepository?.let { repo ->
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val downloadId = repo.startDownload(
                    url = download.url,
                    fileName = download.fileName,
                    mimeType = download.mimeType ?: "application/octet-stream",
                    cookies = download.cookies,
                    userAgent = download.userAgent,
                    referer = download.referer
                )
                Log.d("WebScreen", "=== Download started via DownloadService: ${download.fileName} (id=$downloadId)")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Downloading ${download.fileName}...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WebScreen", "=== Download failed: ${e.message}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    } ?: run {
        Log.w("WebScreen", "=== downloadRepository is null, cannot start download")
        Toast.makeText(context, "Download not available", Toast.LENGTH_SHORT).show()
    }
}


@Composable
private fun BottomNavigationBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onScanQr: () -> Unit,
    onShowQr: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            enabled = canGoBack,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        IconButton(
            onClick = onForward,
            enabled = canGoForward,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward",
                tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }
        
        // Tabs button with count badge
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onTabs) {
                Icon(
                    imageVector = Icons.Default.Tab,
                    contentDescription = "Tabs"
                )
            }
            // Tab count badge
            if (tabCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 4.dp)
                        .size(18.dp),
                    shape = CircleShape,
                    color = CoralRed
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tabCount > 99) "99+" else tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // QR Scanner button
        IconButton(
            onClick = onScanQr,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Scan QR Code"
            )
        }
        
        // QR Share button
        IconButton(
            onClick = onShowQr,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share as QR"
            )
        }
        
        IconButton(
            onClick = onHome,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(CoralRed)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error loading page",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry"
                )
            }
        }
    }
}

private fun String.getDomainName(): String {
    return try {
        val uri = android.net.Uri.parse(this)
        uri.host ?: this
    } catch (e: Exception) {
        this
    }
}

/**
 * Inject JavaScript to intercept downloads from blob: URLs and fetch() calls
 */
private fun injectDownloadInterceptor(webView: WebView) {
    webView.evaluateJavascript("""
        (function() {
            if (window.__downloadInterceptorInstalled) return;
            window.__downloadInterceptorInstalled = true;
            
            console.log('[Nova] Download interceptor installed');
            
            // NOTE: We do NOT intercept <a href download> clicks here.
            // setDownloadListener() handles those correctly with Referer + cookies.
            // This interceptor ONLY handles dynamically-generated blob: URLs that
            // the native downloader cannot catch.
            
            // Monitor URL.createObjectURL for blob downloads
            var originalCreateObjectURL = URL.createObjectURL;
            URL.createObjectURL = function(blob) {
                var url = originalCreateObjectURL.call(URL, blob);
                console.log('[Nova] Blob URL created:', url, 'type:', blob.type);
                if (blob.type && (blob.type.includes('pdf') || blob.type.includes('octet-stream') ||
                    blob.type.includes('zip') || blob.type.includes('msword') ||
                    blob.type.includes('spreadsheet') || blob.type.includes('presentation'))) {
                    AndroidDownload.onBlobCreated(url, blob.type || 'application/octet-stream');
                }
                return url;
            };
            
            // Intercept fetch() responses that are file downloads
            var originalFetch = window.fetch;
            window.fetch = function() {
                var url = arguments[0];
                return originalFetch.apply(this, arguments).then(function(response) {
                    var ct = response.headers.get('content-type') || '';
                    if (ct.includes('application/pdf') || ct.includes('octet-stream') ||
                        ct.includes('application/zip')) {
                        console.log('[Nova] fetch download detected:', url);
                        AndroidDownload.onFetchDetected(url.toString(), ct);
                    }
                    return response;
                });
            };
            
            console.log('[Nova] Blob/fetch interceptors installed');
        })();
    """.trimIndent(), null)
}

/**
 * JavaScript Interface for handling download events from WebView
 */
class DownloadJsInterface(
    private val context: android.content.Context,
    private val downloadRepository: com.nova.browser.domain.repository.DownloadRepository?,
    private val onDownloadIntercepted: (String, String, String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onDownloadClick(url: String, fileName: String) {
        Log.d("WebScreen", "=== JS: Download click - url=$url, fileName=$fileName")
        val mimeType = when {
            url.endsWith(".pdf", true) -> "application/pdf"
            url.endsWith(".doc", true) -> "application/msword"
            url.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
        onDownloadIntercepted(url, fileName, mimeType)
    }

    @android.webkit.JavascriptInterface
    fun onFetchDetected(url: String, mimeType: String) {
        Log.d("WebScreen", "=== JS: Fetch download detected - url=$url, mimeType=$mimeType")
        val fileName = url.substringAfterLast("/", "download").takeIf { it.contains(".") } ?: "download.pdf"
        onDownloadIntercepted(url, fileName, mimeType)
    }

    @android.webkit.JavascriptInterface
    fun onXhrDetected(url: String) {
        Log.d("WebScreen", "=== JS: XHR download detected - url=$url")
        val fileName = url.substringAfterLast("/", "download").takeIf { it.contains(".") } ?: "download"
        val mimeType = "application/octet-stream"
        onDownloadIntercepted(url, fileName, mimeType)
    }

    @android.webkit.JavascriptInterface
    fun onWindowOpen(url: String) {
        Log.d("WebScreen", "=== JS: Window open (download) - url=$url")
        val fileName = url.substringAfterLast("/", "download").takeIf { it.contains(".") } ?: "download.pdf"
        val mimeType = when {
            url.endsWith(".pdf", true) -> "application/pdf"
            url.endsWith(".doc", true) -> "application/msword"
            else -> "application/octet-stream"
        }
        onDownloadIntercepted(url, fileName, mimeType)
    }

    @android.webkit.JavascriptInterface
    fun onBlobCreated(blobUrl: String, mimeType: String) {
        Log.d("WebScreen", "=== JS: Blob URL created - blobUrl=$blobUrl, mimeType=$mimeType")
        // Blob URLs need special handling - we'll read them via JS and pass the data back
        onDownloadIntercepted(blobUrl, "download.pdf", mimeType)
    }

    @android.webkit.JavascriptInterface
    fun logClick(tag: String, href: String, download: String, onclick: String) {
        Log.d("WebScreen", "=== CLICK DIAGNOSTIC: tag=$tag, href=$href, download=$download, onclick=$onclick")
    }
}
