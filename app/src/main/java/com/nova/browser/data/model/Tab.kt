package com.nova.browser.data.model

import android.graphics.Bitmap
import java.util.UUID

/**
 * Represents a browser tab
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "",
    val favicon: Bitmap? = null,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() } ?: url.getDomain()

    private fun String.getDomain(): String {
        return try {
            val uri = android.net.Uri.parse(this)
            uri.host ?: this
        } catch (e: Exception) {
            this
        }
    }
}
