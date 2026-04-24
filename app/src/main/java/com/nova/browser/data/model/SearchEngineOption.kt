package com.nova.browser.data.model

/**
 * Unified interface for built-in and custom search engines.
 */
interface SearchEngineOption {
    val displayName: String
    fun buildSearchUrl(query: String): String
}
