package com.nova.browser.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.view.WindowCompat
import com.nova.browser.domain.repository.DownloadRepository
import com.nova.browser.ui.components.IndeterminateProgressIndicator
import com.nova.browser.ui.components.LinearProgressIndicatorCustom
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.viewmodel.WebViewModel
import com.nova.browser.util.AdBlocker
import com.nova.browser.util.AdBlockCosmeticInjector

/**
 * Incognito Web Screen - Private browsing mode.
 * 
 * Features:
 * - Separate cookie storage (isolated from normal browsing)
 * - No history tracking
 * - No URL/session saving
 * - Auto-clear data when leaving
 * - Visual incognito indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncognitoWebScreen(
    viewModel: WebViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    downloadRepository: DownloadRepository? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    
    // Get AdBlocker instance
    val adBlocker = remember { AdBlocker.getInstance() }
    
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
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
    
    // Auto-hide system bars on launch for immersive experience
    LaunchedEffect(Unit) {
        enterFullscreen()
    }
    
    // Handle back button
    BackHandler {
        if (customView != null) {
            // Exit fullscreen video first
            customViewCallback?.onCustomViewHidden()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            // Clear incognito data before exiting
            webView?.clearIncognitoData()
            onNavigateBack()
        }
    }
    
    // Fullscreen incognito mode - minimal UI
    Box(modifier = modifier.fillMaxSize()) {
        // Progress indicator at top
        if (state.isLoading) {
            if (state.progress > 0) {
                LinearProgressIndicatorCustom(
                    progress = state.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            } else {
                IndeterminateProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
        
        // WebView filling entire screen
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    
                    // Force fill parent layout
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Incognito settings
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            allowFileAccess = true
                            allowContentAccess = true
                            
                            // Video and viewport settings for proper display
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            
                            // Handle window.open()
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                            
                            // Enable WebGL and media features
                            setNeedInitialFocus(true)
                            
                            // Additional settings for compatibility
                            loadsImagesAutomatically = true
                            blockNetworkImage = false
                            
                            // Enable hardware acceleration for smooth video
                            setRenderPriority(WebSettings.RenderPriority.HIGH)
                            
                            // Text scaling for better display
                            textZoom = 100
                        }
                        
                        // Disable cookies in incognito mode
                        CookieManager.getInstance().setAcceptCookie(false)
                        
                        webViewClient = object : WebViewClient() {
                            private val TAG = "IncognitoWebView"
                            
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()
                                
                                // Block custom schemes
                                if (url.startsWith("intent://")) {
                                    return try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        if (fallbackUrl != null) view.loadUrl(fallbackUrl)
                                        true
                                    } catch (e: Exception) { true }
                                }
                                
                                // Handle external app schemes
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        view.context.startActivity(intent)
                                        true
                                    } catch (e: Exception) { false }
                                }
                                
                                return false
                            }
                            
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                Log.d(TAG, "Incognito page started: $url")
                                url?.let { viewModel.onPageStarted(it) }
                                viewModel.resetBlockedCount()
                            }
                            
                            override fun onPageFinished(view: WebView, url: String) {
                                Log.d(TAG, "Incognito page finished: $url")
                                viewModel.onPageFinished(url)
                                viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                // Cosmetic ad filter (safe — no download interceptor needed in incognito)
                                if (adBlockEnabled) AdBlockCosmeticInjector.inject(view)
                            }
                            
                            override fun shouldInterceptRequest(
                                view: WebView?, 
                                request: WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                // Ad blocking in incognito mode
                                if (adBlockEnabled && request?.url != null) {
                                    val url = request.url.toString()
                                    if (adBlocker.shouldBlock(url)) {
                                        viewModel.incrementBlockedCount()
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
                                url?.let {
                                    viewModel.onUrlChanged(it)
                                    viewModel.updateNavigationState(view.canGoBack(), view.canGoForward())
                                }
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
                                
                                val decorView = activity.window.decorView as FrameLayout
                                decorView.addView(view, FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                ))
                                
                                enterFullscreen()
                            }
                            
                            override fun onHideCustomView() {
                                customView?.let { view ->
                                    val decorView = activity.window.decorView as FrameLayout
                                    decorView.removeView(view)
                                }
                                customView = null
                                customViewCallback = null
                                exitFullscreen()
                            }
                        }
                        
                        // Load initial URL
                        loadUrl(state.url)
                    }
                },
                update = { view ->
                    // Reload if URL changed
                    if (view.url != state.url && state.url.isNotBlank()) {
                        view.loadUrl(state.url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
        // Floating close button (minimal UI)
        IconButton(
            onClick = {
                webView?.clearIncognitoData()
                onNavigateBack()
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Incognito",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Private badge (floating)
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Private",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

/**
 * Extension function to clear all incognito browsing data.
 */
private fun WebView.clearIncognitoData() {
    Log.d("IncognitoWebScreen", "Clearing incognito data")
    
    // Clear cache
    clearCache(true)
    
    // Clear history
    clearHistory()
    
    // Clear form data
    clearFormData()
    
    // Clear cookies for this WebView's session
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    
    // Stop loading
    stopLoading()
    
    // Load blank page
    loadUrl("about:blank")
}
