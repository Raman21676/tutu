package com.nova.browser.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * AdBlocker engine that blocks requests to known ad and tracker domains.
 * Loads blocklist from assets and checks URLs against it.
 */
class AdBlocker private constructor() {

    companion object {
        private const val TAG = "AdBlocker"
        private const val BLOCKLIST_FILE = "adblock_domains.txt"
        
        @Volatile
        private var instance: AdBlocker? = null
        
        fun getInstance(): AdBlocker {
            return instance ?: synchronized(this) {
                instance ?: AdBlocker().also { instance = it }
            }
        }
    }

    // Using ConcurrentHashMap for thread-safe access
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private var isInitialized = false

    /**
     * Initialize the ad blocker by loading the blocklist from assets.
     * Should be called once on app startup.
     */
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
                                // Skip empty lines and comments
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
     * @param url The URL to check
     * @return true if the URL should be blocked, false otherwise
     */
    fun shouldBlock(url: String): Boolean {
        if (!isInitialized || blockedDomains.isEmpty()) {
            return false
        }

        val domain = extractDomain(url)?.lowercase() ?: return false
        
        // Check exact match
        if (blockedDomains.contains(domain)) {
            Log.d(TAG, "Blocked (exact): $url")
            return true
        }
        
        // Check if domain or any parent domain is in blocklist
        var currentDomain = domain
        while (currentDomain.contains(".")) {
            if (blockedDomains.contains(currentDomain)) {
                Log.d(TAG, "Blocked (domain): $url")
                return true
            }
            // Move to parent domain
            val dotIndex = currentDomain.indexOf(".")
            if (dotIndex == -1) break
            currentDomain = currentDomain.substring(dotIndex + 1)
        }
        
        return false
    }

    /**
     * Extract domain from a URL.
     */
    private fun extractDomain(url: String): String? {
        return try {
            when {
                url.startsWith("http://") -> {
                    val start = 7
                    val end = url.indexOf("/", start).takeIf { it != -1 } ?: url.length
                    url.substring(start, end).removePrefix("www.")
                }
                url.startsWith("https://") -> {
                    val start = 8
                    val end = url.indexOf("/", start).takeIf { it != -1 } ?: url.length
                    url.substring(start, end).removePrefix("www.")
                }
                else -> {
                    // No protocol, extract domain part
                    val end = url.indexOf("/").takeIf { it != -1 } ?: url.length
                    url.substring(0, end).removePrefix("www.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting domain from: $url", e)
            null
        }
    }

    /**
     * Get the number of blocked domains loaded.
     */
    fun getBlockedDomainCount(): Int = blockedDomains.size

    /**
     * Check if the blocker is initialized and ready.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Clear all blocked domains (useful for testing or reloading).
     */
    fun clear() {
        blockedDomains.clear()
        isInitialized = false
    }
}
