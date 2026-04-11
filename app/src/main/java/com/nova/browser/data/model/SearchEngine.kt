package com.nova.browser.data.model

import android.net.Uri

/**
 * Available search engines for the browser.
 * Each engine has a display name and a search URL template where %s is replaced with the query.
 */
enum class SearchEngine(
    val displayName: String,
    val searchUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=%s"),
    YANDEX("Yandex", "https://yandex.com/search/?text=%s");

    /**
     * Build a full search URL from a user query.
     */
    fun buildSearchUrl(query: String): String {
        return searchUrl.replace("%s", Uri.encode(query))
    }

    companion object {
        fun fromName(name: String): SearchEngine {
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                GOOGLE // Default fallback
            }
        }
    }
}
