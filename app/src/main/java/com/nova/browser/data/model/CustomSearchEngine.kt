package com.nova.browser.data.model

import android.net.Uri
import kotlinx.serialization.Serializable

/**
 * A user-defined search engine.
 * The [searchUrl] must contain "%s" which will be replaced with the encoded query.
 */
@Serializable
data class CustomSearchEngine(
    val name: String,
    val searchUrl: String
) : SearchEngineOption {

    override val displayName: String get() = name

    override fun buildSearchUrl(query: String): String {
        return searchUrl.replace("%s", Uri.encode(query))
    }
}
