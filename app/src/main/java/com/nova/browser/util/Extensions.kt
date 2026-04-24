package com.nova.browser.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import com.nova.browser.data.model.SearchEngineOption

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

/**
 * Build a URL from user input. If the input looks like a search query,
 * it uses the selected search engine. Otherwise treats it as a URL.
 */
fun String.buildUrl(httpsEnabled: Boolean = true, searchEngine: SearchEngineOption = com.nova.browser.data.model.SearchEngine.GOOGLE): String {
    val trimmed = this.trim()
    return when {
        // Already has protocol
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        
        // It's a search query (has spaces or no dot)
        trimmed.isSearchQuery() -> {
            searchEngine.buildSearchUrl(trimmed)
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
