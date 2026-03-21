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
}
