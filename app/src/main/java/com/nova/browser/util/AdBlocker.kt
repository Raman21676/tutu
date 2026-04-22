package com.nova.browser.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * AdBlocker engine — two-layer blocking:
 *  1. Domain-based: 87,770 domains from Steven Black unified hosts file.
 *  2. URL keyword/pattern matching: covers ad networks not caught by domain alone.
 *
 * Also tracks a per-page blocked-request counter for the shield badge UI.
 */
class AdBlocker private constructor() {

    companion object {
        private const val TAG = "AdBlocker"
        private const val BLOCKLIST_FILE = "adblock_domains.txt"

        /**
         * URL substring patterns for ad/tracker networks.
         * Catches requests that use unpredictable subdomains or paths
         * not covered by the static domain list.
         */
        private val AD_URL_KEYWORDS = listOf(
            // Google advertising
            "googlesyndication.com", "doubleclick.net", "googleadservices.com",
            "pagead/js", "pagead2.googlesyndication", "adservice.google",
            // Facebook pixel & ads
            "facebook.com/tr", "connect.facebook.net/en_US/fbevents",
            // Analytics / tracking
            "google-analytics.com/collect", "google-analytics.com/j/collect",
            "analytics.js", "gtag/js", "gtm.js",
            "hotjar.com", "fullstory.com", "mouseflow.com",
            "clarity.ms", "quantserve.com", "scorecardresearch.com",
            "omtrdc.net", "demdex.net",
            // Ad exchanges
            "2mdn.net", "adnxs.com", "moatads.com",
            "criteo.com", "taboola.com", "outbrain.com",
            "amazon-adsystem.com", "media.net",
            // Misc tracking beacons
            "bat.bing.com", "sc-static.net/scevent",
            "/beacon?", "/pixel?", "/collect?"
        )

        @Volatile
        private var instance: AdBlocker? = null

        fun getInstance(): AdBlocker {
            return instance ?: synchronized(this) {
                instance ?: AdBlocker().also { instance = it }
            }
        }
    }

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val _blockedCount = AtomicInteger(0)
    private var isInitialized = false

    /** Initialize the ad blocker by loading the blocklist from assets. Call once on app startup. */
    suspend fun initialize(context: Context) {
        if (isInitialized) return
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                context.assets.open(BLOCKLIST_FILE).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        var count = 0
                        while (reader.readLine().also { line = it } != null) {
                            line?.let {
                                val domain = it.trim().lowercase()
                                if (domain.isNotEmpty() && !domain.startsWith("#")) {
                                    blockedDomains.add(domain)
                                    count++
                                }
                            }
                        }
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Loaded $count blocked domains in ${loadTime}ms")
                    }
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ad blocklist", e)
            }
        }
    }

    /**
     * Check if a URL should be blocked.
     * Layer 1: exact/parent domain match.
     * Layer 2: URL keyword pattern match.
     */
    fun shouldBlock(url: String): Boolean {
        if (!isInitialized || blockedDomains.isEmpty()) return false

        val domain = extractDomain(url)?.lowercase() ?: return false

        // Layer 1a — exact domain match
        if (blockedDomains.contains(domain)) {
            _blockedCount.incrementAndGet()
            Log.d(TAG, "Blocked (exact domain): $domain")
            return true
        }

        // Layer 1b — parent domain match (subdomain walk)
        var currentDomain = domain
        while (currentDomain.contains(".")) {
            if (blockedDomains.contains(currentDomain)) {
                _blockedCount.incrementAndGet()
                Log.d(TAG, "Blocked (parent domain): $currentDomain")
                return true
            }
            val dotIndex = currentDomain.indexOf(".")
            if (dotIndex == -1) break
            currentDomain = currentDomain.substring(dotIndex + 1)
        }

        // Layer 2 — URL keyword/pattern match
        val lowerUrl = url.lowercase()
        for (keyword in AD_URL_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                _blockedCount.incrementAndGet()
                Log.d(TAG, "Blocked (keyword '$keyword'): $url")
                return true
            }
        }

        return false
    }

    /** Returns current blocked count (for the current page). */
    fun getBlockedCount(): Int = _blockedCount.get()

    /** Resets the blocked count — call on each new page load. */
    fun resetBlockedCount() {
        _blockedCount.set(0)
    }

    /** Extract domain from a URL, stripping port and www prefix. */
    private fun extractDomain(url: String): String? {
        return try {
            val hostWithPort = when {
                url.startsWith("http://") -> {
                    val start = 7
                    val end = url.indexOf("/", start).takeIf { it != -1 } ?: url.length
                    url.substring(start, end)
                }
                url.startsWith("https://") -> {
                    val start = 8
                    val end = url.indexOf("/", start).takeIf { it != -1 } ?: url.length
                    url.substring(start, end)
                }
                else -> {
                    val end = url.indexOf("/").takeIf { it != -1 } ?: url.length
                    url.substring(0, end)
                }
            }
            // Strip port number (e.g. ads.example.com:443 -> ads.example.com)
            val host = hostWithPort.substringBefore(":")
            host.removePrefix("www.").lowercase()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting domain from: $url", e)
            null
        }
    }

    fun getBlockedDomainCount(): Int = blockedDomains.size
    fun isReady(): Boolean = isInitialized
    fun clear() {
        blockedDomains.clear()
        isInitialized = false
    }
}
