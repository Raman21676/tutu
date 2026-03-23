package com.tutu.browser.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tutu.browser.service.BackgroundPlayService
import com.tutu.browser.ui.components.IndeterminateProgressIndicator
import com.tutu.browser.ui.components.LinearProgressIndicatorCustom
import com.tutu.browser.ui.theme.CoralRed
import com.tutu.browser.ui.viewmodel.WebViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    viewModel: WebViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    pipEnabled: Boolean = false,
    backgroundPlayEnabled: Boolean = false,
    floatingWindowEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val fullscreen by viewModel.fullscreen.collectAsState()
    
    // Single WebView reference
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Fullscreen video handling
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as Activity
    
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
                            // PiP button (show if enabled - works on Android 8.0+)
                            if (pipEnabled) {
                                IconButton(onClick = { enterPipMode() }) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPicture,
                                        contentDescription = "Picture-in-Picture"
                                    )
                                }
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
                }
            }
        },
        bottomBar = {
            if (!fullscreen) {
                BottomNavigationBar(
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    onBack = { webView?.goBack() },
                    onForward = { webView?.goForward() },
                    onRefresh = { webView?.reload() },
                    onHome = onNavigateHome
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
                    BackgroundAwareWebView(ctx).apply {
                        this.backgroundPlayEnabled = backgroundPlayEnabled
                        
                        // Store reference for MainActivity to access synchronously
                        com.tutu.browser.util.WebViewHolder.webView = this
                        com.tutu.browser.util.WebViewHolder.backgroundPlayEnabled = backgroundPlayEnabled
                        com.tutu.browser.util.WebViewHolder.floatingWindowEnabled = floatingWindowEnabled
                        com.tutu.browser.util.WebViewHolder.currentUrl = state.url
                        
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
                            
                            // Standard Chrome mobile UA (TikTok UA is set dynamically)
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                        }
                        
                        // Enable third-party cookies (critical for TikTok, Facebook)
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        
                        // Handle file downloads
                        setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                            try {
                                val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl)).apply {
                                    setMimeType(mimetype)
                                    addRequestHeader("cookie", cookieManager.getCookie(downloadUrl))
                                    addRequestHeader("User-Agent", userAgent)
                                    setDescription("Downloading file...")
                                    setTitle(android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype))
                                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(
                                        android.os.Environment.DIRECTORY_DOWNLOADS,
                                        android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                                    )
                                }
                                val dm = ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                dm.enqueue(request)
                                android.widget.Toast.makeText(ctx, "Downloading file...", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(ctx, "Failed to start download", android.widget.Toast.LENGTH_SHORT).show()
                                android.util.Log.e("WebScreen", "Download failed: ${e.message}")
                            }
                        }
                        
                        webViewClient = object : WebViewClient() {
                            private val TAG = "WebViewClient"
                            
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()
                                Log.d(TAG, "Loading URL: $url")
                                
                                // Block TikTok app redirect schemes
                                if (url.startsWith("snssdk") ||
                                    url.contains("onelink.me") ||
                                    (url.startsWith("market://") && url.contains("tiktok")) ||
                                    (url.startsWith("intent://") && url.contains("tiktok"))) {
                                    Log.d(TAG, "Blocked TikTok app redirect: $url")
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
                                    
                                    // For TikTok: switch to iPad UA to bypass guest limits
                                    if (it.contains("tiktok.com")) {
                                        view?.settings?.userAgentString = "Mozilla/5.0 (iPad; CPU OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                                    } else {
                                        view?.settings?.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                                    }
                                }
                                // Inject early so YouTube's JS loads with overrides already in place
                                view?.evaluateJavascript(PAGE_VISIBILITY_OVERRIDE_SCRIPT, null)
                            }
                            
                            override fun onPageFinished(view: WebView, url: String) {
                                Log.d(TAG, "Page finished: $url")
                                viewModel.onPageFinished(url)
                                viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                
                                // Force proper redraw within Compose's AndroidView
                                view.requestLayout()
                                view.post { view.invalidate() }
                                
                                // Override Page Visibility API to prevent YouTube from detecting background state
                                // This allows background play to work when the app is minimized
                                Log.d(TAG, "Injecting Page Visibility override script")
                                view.evaluateJavascript(PAGE_VISIBILITY_OVERRIDE_SCRIPT, null)
                                
                                // Unmute videos
                                view.evaluateJavascript(
                                    "document.querySelectorAll('video').forEach(v => { v.muted = false; v.volume = 1.0; });",
                                    null
                                )
                                
                                // TikTok: Comprehensive scroll unlock and popup suppression
                                if (url.contains("tiktok.com")) {
                                    view.evaluateJavascript(TIKTOK_UNBLOCK_SCRIPT, null)
                                }
                            }
                            
                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                // Log resources for debugging
                                if (url?.contains("tiktok") == true || url?.contains("googlevideo") == true) {
                                    Log.d(TAG, "Loading resource: $url")
                                }
                            }
                            
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                // CRITICAL: Update URL state for SPA navigation (YouTube, TikTok)
                                url?.let {
                                    Log.d(TAG, "History updated: $url (reload: $isReload)")
                                    viewModel.onUrlChanged(it)
                                    viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                }
                                // Re-inject Page Visibility override on every SPA navigation
                                // This ensures YouTube can't detect background state after navigation
                                Log.d(TAG, "Re-injecting Page Visibility override on navigation")
                                view.evaluateJavascript(PAGE_VISIBILITY_OVERRIDE_SCRIPT, null)
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
                                title?.let { viewModel.onReceivedTitle(it) }
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
                                return false
                            }
                        }
                        
                        loadUrl(state.url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { wv ->
                    webView = wv
                    // Update WebViewHolder whenever the view updates
                    com.tutu.browser.util.WebViewHolder.webView = wv
                    com.tutu.browser.util.WebViewHolder.backgroundPlayEnabled = backgroundPlayEnabled
                    com.tutu.browser.util.WebViewHolder.floatingWindowEnabled = floatingWindowEnabled
                    com.tutu.browser.util.WebViewHolder.currentUrl = state.url
                    // Don't reload - WebView manages its own navigation.
                    // SPA changes are handled via doUpdateVisitedHistory.
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
    
    // Background play handling with foreground service
    // CRITICAL: This must be outside the Scaffold to work properly
    DisposableEffect(backgroundPlayEnabled, floatingWindowEnabled, webView) {
        // Update WebViewHolder whenever settings change
        com.tutu.browser.util.WebViewHolder.backgroundPlayEnabled = backgroundPlayEnabled
        com.tutu.browser.util.WebViewHolder.floatingWindowEnabled = floatingWindowEnabled
        if (webView != null) {
            com.tutu.browser.util.WebViewHolder.webView = webView
        }
        Log.d("WebScreen", "WebViewHolder updated - bgPlay: $backgroundPlayEnabled, floating: $floatingWindowEnabled")
        
        // Start timestamp tracking for floating window feature
        if (floatingWindowEnabled && webView != null) {
            com.tutu.browser.util.WebViewHolder.startTimestampTracking(webView)
            Log.d("WebScreen", "Timestamp tracking started")
        }
        
        val serviceIntent = Intent(context, BackgroundPlayService::class.java)
        
        if (backgroundPlayEnabled) {
            // Start foreground service to keep process alive
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d("WebScreen", "Background play service started")
            } catch (e: Exception) {
                Log.e("WebScreen", "Failed to start background service", e)
            }
        } else {
            // Stop the service when disabled
            context.stopService(serviceIntent)
            Log.d("WebScreen", "Background play service stopped")
        }
        
        // CRITICAL: Periodically re-inject script to keep overriding YouTube's detection
        // This handles cases where YouTube's JS re-registers event listeners
        val scriptJob = if (backgroundPlayEnabled && webView != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                while (isActive) {
                    delay(1000) // Every 1 second
                    webView?.evaluateJavascript(PAGE_VISIBILITY_OVERRIDE_SCRIPT, null)
                    webView?.evaluateJavascript(
                        """
                        (function() {
                            // Keep video playing
                            document.querySelectorAll('video').forEach(v => {
                                if (v.paused && v.currentTime > 0 && !v.ended) {
                                    v.play();
                                }
                                v.muted = false;
                                v.volume = 1.0;
                            });
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
        } else null
        
        onDispose {
            scriptJob?.cancel()
            // Stop timestamp tracking
            com.tutu.browser.util.WebViewHolder.stopTimestampTracking()
            Log.d("WebScreen", "Timestamp tracking stopped")
            if (!backgroundPlayEnabled) {
                context.stopService(serviceIntent)
            }
            // Clear WebViewHolder reference when WebScreen is disposed
            if (!backgroundPlayEnabled) {
                com.tutu.browser.util.WebViewHolder.webView = null
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                tint = androidx.compose.ui.graphics.Color.White,
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

// Page Visibility API override script - prevents YouTube from detecting background state
// This allows background play to work when the app is minimized
private val PAGE_VISIBILITY_OVERRIDE_SCRIPT = """
(function() {
    // 1. Override Page Visibility API
    try {
        Object.defineProperty(document, 'hidden', {
            get: function() { return false; }, configurable: true
        });
        Object.defineProperty(document, 'visibilityState', {
            get: function() { return 'visible'; }, configurable: true
        });
    } catch(e) {}

    // 2. Block all background-detection events
    var blocked = ['visibilitychange', 'freeze', 'pagehide', 'blur'];
    var origAdd = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function(type, fn, opts) {
        if (blocked.indexOf(type) !== -1) return;
        return origAdd.call(this, type, fn, opts);
    };
    window.onblur = null;
    document.onvisibilitychange = null;

    // 3. Override AudioContext to prevent suspension - THIS IS THE KEY FIX
    if (window.AudioContext || window.webkitAudioContext) {
        var OriginalAudioContext = window.AudioContext || window.webkitAudioContext;
        var PatchedAudioContext = function() {
            var ctx = new OriginalAudioContext();
            // Override suspend - prevent YouTube from suspending audio
            ctx.suspend = function() { return Promise.resolve(); };
            // If context gets suspended, immediately resume it
            Object.defineProperty(ctx, 'state', {
                get: function() { return 'running'; },
                configurable: true
            });
            return ctx;
        };
        window.AudioContext = PatchedAudioContext;
        window.webkitAudioContext = PatchedAudioContext;
    }

    // 4. Override MediaSession to prevent YouTube pausing via media controls
    if (navigator.mediaSession) {
        navigator.mediaSession.setActionHandler = function(action, handler) {
            if (action === 'pause' || action === 'stop') return;
            // Allow play, seekforward, seekbackward, nexttrack, previoustrack
            var orig = Object.getPrototypeOf(navigator.mediaSession)
                .setActionHandler;
            if (orig) orig.call(navigator.mediaSession, action, handler);
        };
    }

    // 5. Keep videos unmuted and playing
    function keepPlaying() {
        document.querySelectorAll('video').forEach(function(v) {
            v.muted = false;
            v.volume = 1.0;
        });
    }
    keepPlaying();
    setInterval(keepPlaying, 2000);
})();
""".trimIndent()

// TikTok unblock script (kept for reference)
private val TIKTOK_UNBLOCK_SCRIPT = """
(function() {
    if (window.__tutuTikTokFixed) return;
    window.__tutuTikTokFixed = true;
    
    var style = document.createElement('style');
    style.id = 'tutu-tiktok-fix';
    style.textContent = '
        nav, [class*="SideNavBar"], [class*="side-nav"],
        [class*="sideNav"], [class*="sidebar"],
        [data-e2e="nav-sidebar"],
        aside {
            display: none !important;
            width: 0 !important;
            min-width: 0 !important;
            max-width: 0 !important;
            overflow: hidden !important;
        }
        html, body, #app, #app > div, main,
        [class*="DivBodyContainer"],
        [class*="DivContentContainer"],
        [class*="DivMainContainer"] {
            width: 100vw !important;
            max-width: 100vw !important;
            margin: 0 !important;
            padding: 0 !important;
            overflow-y: auto !important;
            -webkit-overflow-scrolling: touch !important;
        }
        [class*="DivItemContainer"],
        [class*="video-card"],
        [class*="VideoCard"] {
            width: 100vw !important;
            height: 100vh !important;
            max-width: 100vw !important;
        }
        video {
            width: 100vw !important;
            height: 100vh !important;
            object-fit: cover !important;
        }
        [class*="DivContentContainer"] > div,
        [class*="main-content"] > div {
            margin-left: 0 !important;
            padding-left: 0 !important;
        }
        [class*="DraftPanel"], [class*="draft-panel"],
        [class*="DownloadGuide"], [class*="download-guide"],
        [class*="BottomDrawer"], [class*="bottom-drawer"],
        [class*="login-modal"], [class*="LoginModal"],
        div[class*="modal"][class*="download"],
        div[class*="open-in-app"],
        div[class*="install-app"],
        div[class*="guide-modal"],
        div[class*="download-bar"],
        div[class*="verify-bar"],
        div[class*="login-banner"],
        div[class*="bottom-banner"],
        div[class*="overlay-mask"],
        div[class*="mask-enter"],
        div[class*="popup"],
        div[class*="Modal"],
        div[class*="modal"],
        .download-guide, .open-in-app-banner, .guide-modal,
        [class*="Banner"], [class*="banner"] {
            display: none !important;
            opacity: 0 !important;
            visibility: hidden !important;
            pointer-events: none !important;
        }
    ';
    document.head.appendChild(style);
    
    function hideObstructions() {
        var selectors = [
            '[class*="DraftPanel"]', '[class*="DownloadGuide"]',
            '[class*="BottomDrawer"]', '[class*="login-modal"]',
            '.download-guide', '.open-in-app-banner',
            '.guide-modal', '[class*="Banner"]',
            'div[class*="modal"]', 'div[class*="Modal"]'
        ];
        selectors.forEach(function(selector) {
            document.querySelectorAll(selector).forEach(function(el) {
                el.style.display = 'none';
                el.remove();
            });
        });
    }
    
    setInterval(hideObstructions, 1000);
    hideObstructions();
    
    document.addEventListener('DOMContentLoaded', hideObstructions);
    window.addEventListener('load', hideObstructions);
    
    var observer = new MutationObserver(hideObstructions);
    observer.observe(document.body, { childList: true, subtree: true });
    
    document.body.style.overflow = 'auto';
    document.body.style.overscrollBehavior = 'auto';
    document.documentElement.style.overflow = 'auto';
    
    window.addEventListener('touchmove', function(e) {
        e.stopPropagation();
    }, { passive: true });
    
    window.addEventListener('scroll', function(e) {
        e.stopPropagation();
    }, { passive: true });
})();
""".trimIndent()
