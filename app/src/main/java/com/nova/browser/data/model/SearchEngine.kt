package com.nova.browser.data.model

import android.net.Uri

/**
 * Built-in search engines for the browser.
 * Each engine has a display name and a search URL template where %s is replaced with the query.
 */
enum class SearchEngine(
    val searchName: String,
    val searchUrl: String
) : SearchEngineOption {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=%s"),
    YANDEX("Yandex", "https://yandex.com/search/?text=%s");

    override val displayName: String get() = searchName

    override fun buildSearchUrl(query: String): String {
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

/**
 * Resolve a search engine name (built-in or custom) to a [SearchEngineOption].
 * Custom engines are encoded as "CUSTOM:index".
 */
fun resolveSearchEngine(
    name: String,
    customEngines: List<CustomSearchEngine>
): SearchEngineOption {
    return when {
        name.startsWith("CUSTOM:") -> {
            val index = name.removePrefix("CUSTOM:").toIntOrNull()
            index?.let { customEngines.getOrNull(it) } ?: SearchEngine.GOOGLE
        }
        else -> SearchEngine.fromName(name)
    }
}
