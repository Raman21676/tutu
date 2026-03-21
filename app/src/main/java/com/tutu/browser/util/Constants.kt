package com.tutu.browser.util

object Constants {
    
    // User Agents - Updated for better YouTube compatibility
    // Using a recent Chrome desktop UA that YouTube recognizes
    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    
    // Mobile UA with better device detection
    const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S908B) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
    
    // URLs
    const val DEFAULT_URL = "https://www.google.com"
    const val HOME_URL = "about:blank"
    
    // Animation durations
    const val ANIMATION_DURATION_SHORT = 150
    const val ANIMATION_DURATION_MEDIUM = 200
    const val ANIMATION_DURATION_LONG = 300
    
    // WebView settings
    const val WEB_VIEW_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
    const val MAX_WEB_VIEW_POOL_SIZE = 2
    
    // UI constants
    const val BOOKMARKS_GRID_COLUMNS = 2
    const val MAX_BOOKMARKS = 20
}
