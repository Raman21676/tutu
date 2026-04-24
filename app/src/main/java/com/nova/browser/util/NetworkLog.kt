package com.nova.browser.util

import android.webkit.WebResourceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing a single network request log entry.
 */
data class NetworkLogEntry(
    val id: Long = System.currentTimeMillis(),
    val url: String,
    val method: String = "GET",
    val statusCode: Int? = null,
    val mimeType: String? = null,
    val contentLength: Long? = null,
    val pageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)

/**
 * Singleton collector for network request logs.
 * Hooks into shouldInterceptRequest and onLoadResource to build a
 * chronological list of all network activity.
 */
object NetworkLog {

    private val _entries = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    val entries: StateFlow<List<NetworkLogEntry>> = _entries.asStateFlow()

    private val seenIds = mutableSetOf<Long>()
    private var currentPageUrl = ""

    fun onPageStarted(url: String) {
        currentPageUrl = url
    }

    fun onRequest(request: WebResourceRequest?, isBlocked: Boolean = false) {
        if (request == null) return
        val url = request.url?.toString() ?: return
        if (url.isBlank() || url.startsWith("data:") || url.startsWith("javascript:")) return

        val id = request.url.toString().hashCode().toLong() + request.method.hashCode()
        if (!seenIds.add(id)) return

        val entry = NetworkLogEntry(
            id = id,
            url = url,
            method = request.method,
            mimeType = request.requestHeaders?.get("Accept"),
            pageUrl = currentPageUrl,
            isBlocked = isBlocked
        )
        _entries.value = _entries.value + entry
    }

    fun onLoadResource(url: String?) {
        if (url.isNullOrBlank()) return
        if (url.startsWith("data:") || url.startsWith("javascript:")) return

        val id = url.hashCode().toLong()
        if (!seenIds.add(id)) return

        val entry = NetworkLogEntry(
            id = id,
            url = url,
            pageUrl = currentPageUrl
        )
        _entries.value = _entries.value + entry
    }

    fun clear() {
        seenIds.clear()
        _entries.value = emptyList()
    }

    fun getTotalCount(): Int = _entries.value.size
}
