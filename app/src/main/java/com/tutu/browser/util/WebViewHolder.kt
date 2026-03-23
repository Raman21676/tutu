package com.tutu.browser.util

import android.webkit.WebView

/**
 * Minimal singleton that lets MainActivity access the active WebView synchronously.
 * Only one WebView is ever active at a time.
 */
object WebViewHolder {
    var webView: WebView? = null
    var backgroundPlayEnabled: Boolean = false
    var floatingWindowEnabled: Boolean = false
    var currentUrl: String = ""
    var currentTimestamp: Float = 0f  // Video timestamp in seconds
    
    // Periodic timestamp updater - saves timestamp every 5 seconds while video plays
    private var timestampRunnable: Runnable? = null
    private var handler: android.os.Handler? = null
    
    fun startTimestampTracking(webView: WebView?) {
        stopTimestampTracking()
        handler = android.os.Handler(android.os.Looper.getMainLooper())
        timestampRunnable = object : Runnable {
            override fun run() {
                webView?.evaluateJavascript(
                    "(function() { var v = document.querySelector('video'); return v ? v.currentTime : 0; })();"
                ) { result ->
                    val cleanResult = result?.trim('"', ' ')
                    val timestamp = cleanResult?.toFloatOrNull() ?: 0f
                    if (timestamp > 0) {
                        currentTimestamp = timestamp
                        android.util.Log.d("WebViewHolder", "Periodic save: timestamp=$timestamp")
                    }
                }
                handler?.postDelayed(this, 5000) // Save every 5 seconds
            }
        }
        timestampRunnable?.run()
    }
    
    fun stopTimestampTracking() {
        timestampRunnable?.let { handler?.removeCallbacks(it) }
        timestampRunnable = null
        handler = null
    }
}
