package com.tutu.browser.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil

fun String.isValidUrl(): Boolean {
    return URLUtil.isValidUrl(this) || 
           (this.startsWith("www.") && URLUtil.isValidUrl("https://$this")) ||
           (this.contains(".") && !this.contains(" "))
}

fun String.isSearchQuery(): Boolean {
    val lowerCase = this.lowercase()
    val isFileExtension = lowerCase.endsWith(".apk") || 
                          lowerCase.endsWith(".pdf") || 
                          lowerCase.endsWith(".zip") ||
                          lowerCase.endsWith(".mp4") ||
                          lowerCase.endsWith(".mp3")
                          
    val hasNoProtocol = !lowerCase.startsWith("http://") && !lowerCase.startsWith("https://")
    
    if (isFileExtension && hasNoProtocol) {
        return true
    }
    
    return this.contains(" ") || !this.contains(".") || this.length < 4
}

fun String.buildUrl(httpsEnabled: Boolean = true): String {
    val trimmed = this.trim()
    return when {
        // Already has protocol
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        
        // It's a search query (has spaces or no dot)
        trimmed.isSearchQuery() -> {
            "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
        
        // It's a URL - add protocol based on HTTPS setting
        else -> {
            val protocol = if (httpsEnabled) "https://" else "http://"
            when {
                trimmed.startsWith("www.") -> "$protocol$trimmed"
                else -> "$protocol$trimmed"
            }
        }
    }
}

fun Context.openInExternalBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun String.getDomainName(): String {
    return try {
        val uri = Uri.parse(this)
        uri.host?.removePrefix("www.") ?: this
    } catch (e: Exception) {
        this
    }
}

fun String.getFaviconUrl(): String {
    val domain = this.getDomainName()
    return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
}
