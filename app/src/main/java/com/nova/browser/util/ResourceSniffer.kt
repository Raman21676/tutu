package com.nova.browser.util

import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder

/**
 * Represents a media/resource detected on a webpage.
 */
data class DetectedResource(
    val url: String,
    val type: ResourceType,
    val title: String? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    val pageUrl: String = ""
) {
    val displayName: String
        get() = title ?: fileNameFromUrl(url)

    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase()

    companion object {
        fun fileNameFromUrl(url: String): String {
            return try {
                val decoded = URLDecoder.decode(url, "UTF-8")
                decoded.substringAfterLast('/').substringBefore('?').substringBefore('#')
                    .takeIf { it.isNotBlank() } ?: "resource"
            } catch (e: Exception) {
                url.substringAfterLast('/').substringBefore('?').substringBefore('#')
                    .takeIf { it.isNotBlank() } ?: "resource"
            }
        }
    }
}

enum class ResourceType {
    VIDEO, AUDIO, IMAGE, M3U8, BLOB, OTHER;

    fun label(): String = when (this) {
        VIDEO -> "Video"
        AUDIO -> "Audio"
        IMAGE -> "Image"
        M3U8 -> "Stream"
        BLOB -> "Blob"
        OTHER -> "Other"
    }
}

/**
 * Core resource sniffer that collects media URLs from web page requests
 * and JavaScript-reported elements.
 */
class ResourceSniffer(private val adBlocker: AdBlocker? = null) {

    companion object {
        private const val TAG = "ResourceSniffer"

        // Patterns that indicate video content
        private val VIDEO_PATTERNS = listOf(
            ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv",
            ".m4v", ".3gp", ".ogv", "video/", "application/vnd.apple.mpegurl"
        )

        // Patterns that indicate audio content
        private val AUDIO_PATTERNS = listOf(
            ".mp3", ".m4a", ".aac", ".ogg", ".oga", ".wav", ".flac",
            ".wma", ".opus", "audio/"
        )

        // Patterns that indicate image content
        private val IMAGE_PATTERNS = listOf(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg",
            ".ico", ".tiff", ".avif", "image/"
        )

        // Patterns that indicate HLS/DASH streams
        private val STREAM_PATTERNS = listOf(
            ".m3u8", ".m3u", ".mpd", ".ts", ".f4m"
        )

        // Patterns to ignore (ads, trackers, analytics)
        private val IGNORE_PATTERNS = listOf(
            ".js", ".css", ".woff", ".woff2", ".ttf", ".eot",
            "analytics", "tracking", "telemetry", "beacon", "pixel",
            "google-analytics", "googletagmanager", "facebook",
            "doubleclick", "adsystem", "adserver"
        )
    }

    private val _resources = MutableStateFlow<List<DetectedResource>>(emptyList())
    val resources: StateFlow<List<DetectedResource>> = _resources.asStateFlow()

    private val seenUrls = mutableSetOf<String>()
    private var currentPageUrl = ""

    /**
     * Call this when a new page starts loading.
     */
    fun onPageStarted(url: String) {
        currentPageUrl = url
        clear()
    }

    /**
     * Call this from WebViewClient.shouldInterceptRequest() for every request.
     */
    fun onRequest(url: String, mimeType: String? = null) {
        if (!shouldCollect(url)) return

        val type = classifyResource(url, mimeType)
        if (type == ResourceType.OTHER && mimeType == null) {
            // Without a MIME type and no extension match, skip unknown resources
            return
        }

        addResource(DetectedResource(
            url = url,
            type = type,
            mimeType = mimeType,
            pageUrl = currentPageUrl
        ))
    }

    /**
     * Call this when JS reports media elements found in the DOM.
     */
    fun onJsReport(url: String, type: String, title: String? = null) {
        if (!shouldCollect(url)) return

        val resourceType = when (type.lowercase()) {
            "video" -> ResourceType.VIDEO
            "audio" -> ResourceType.AUDIO
            "image" -> ResourceType.IMAGE
            "m3u8", "hls" -> ResourceType.M3U8
            "blob" -> ResourceType.BLOB
            else -> classifyResource(url, null)
        }

        addResource(DetectedResource(
            url = url,
            type = resourceType,
            title = title,
            pageUrl = currentPageUrl
        ))
    }

    /**
     * Clear all detected resources for the current page.
     */
    fun clear() {
        seenUrls.clear()
        _resources.value = emptyList()
    }

    fun getVideoCount(): Int = _resources.value.count { it.type == ResourceType.VIDEO || it.type == ResourceType.M3U8 || it.type == ResourceType.BLOB }
    fun getAudioCount(): Int = _resources.value.count { it.type == ResourceType.AUDIO }
    fun getImageCount(): Int = _resources.value.count { it.type == ResourceType.IMAGE }
    fun getTotalCount(): Int = _resources.value.size

    private fun shouldCollect(url: String): Boolean {
        if (url.isBlank()) return false
        if (seenUrls.contains(url)) return false
        if (url.startsWith("data:")) return false
        if (url.startsWith("javascript:")) return false
        if (url.startsWith("about:")) return false
        if (IGNORE_PATTERNS.any { url.contains(it, ignoreCase = true) }) return false

        // Skip if ad blocker says it's an ad
        if (adBlocker?.shouldBlock(url) == true) return false

        return true
    }

    private fun addResource(resource: DetectedResource) {
        seenUrls.add(resource.url)
        _resources.value = _resources.value + resource
        Log.d(TAG, "Detected ${resource.type}: ${resource.url}")
    }

    private fun classifyResource(url: String, mimeType: String?): ResourceType {
        val lowerUrl = url.lowercase()

        // Check MIME type first if available
        mimeType?.lowercase()?.let { mt ->
            when {
                mt.contains("video/") -> return ResourceType.VIDEO
                mt.contains("audio/") -> return ResourceType.AUDIO
                mt.contains("image/") -> return ResourceType.IMAGE
                mt.contains("application/vnd.apple.mpegurl") || mt.contains("application/x-mpegurl") -> return ResourceType.M3U8
                else -> { /* fall through to URL patterns */ }
            }
        }

        // Check URL patterns
        return when {
            STREAM_PATTERNS.any { lowerUrl.contains(it) } -> ResourceType.M3U8
            VIDEO_PATTERNS.any { lowerUrl.contains(it) } -> ResourceType.VIDEO
            AUDIO_PATTERNS.any { lowerUrl.contains(it) } -> ResourceType.AUDIO
            IMAGE_PATTERNS.any { lowerUrl.contains(it) } -> ResourceType.IMAGE
            lowerUrl.contains("blob:") -> ResourceType.BLOB
            else -> ResourceType.OTHER
        }
    }
}
