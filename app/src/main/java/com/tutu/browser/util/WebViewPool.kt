package com.tutu.browser.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.tutu.browser.data.local.datastore.TutuSettings

/**
 * WebView pool for efficient WebView reuse.
 * Prevents memory leaks and improves performance on low-end devices.
 */
class WebViewPool(private val context: Context) {
    
    private val pool = ArrayDeque<WebView>(Constants.MAX_WEB_VIEW_POOL_SIZE)
    private var currentSettings: TutuSettings? = null
    
    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(settings: TutuSettings): WebView {
        currentSettings = settings
        
        // Try to get from pool
        val webView = pool.removeFirstOrNull() ?: createWebView()
        
        // Configure with current settings
        configureWebView(webView, settings)
        
        return webView
    }
    
    fun release(webView: WebView) {
        if (pool.size < Constants.MAX_WEB_VIEW_POOL_SIZE) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            pool.addLast(webView)
        } else {
            webView.destroy()
        }
    }
    
    fun clear() {
        pool.forEach { it.destroy() }
        pool.clear()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(context.applicationContext).apply {
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
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                
                // Critical for YouTube Shorts
                mediaPlaybackRequiresUserGesture = false
                
                // Use mobile user agent for best compatibility
                userAgentString = Constants.MOBILE_USER_AGENT
            }
        }
    }
    
    private fun configureWebView(webView: WebView, settings: TutuSettings) {
        // WebView is already configured in createWebView
        // Additional configuration based on settings can be added here
    }
}
