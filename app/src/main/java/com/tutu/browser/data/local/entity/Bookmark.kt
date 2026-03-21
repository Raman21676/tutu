package com.tutu.browser.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: String,
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val color: Long? = null,
    val order: Int = 0
)

// Default bookmarks for first launch
val DefaultBookmarks = listOf(
    Bookmark(
        id = "youtube",
        title = "YouTube",
        url = "https://www.youtube.com",
        color = 0xFFFF0000
    ),
    Bookmark(
        id = "tiktok",
        title = "TikTok",
        url = "https://m.tiktok.com",
        color = 0xFF000000
    ),
    Bookmark(
        id = "facebook",
        title = "Facebook",
        url = "https://www.facebook.com",
        color = 0xFF1877F2
    ),
    Bookmark(
        id = "instagram",
        title = "Instagram",
        url = "https://www.instagram.com",
        color = 0xFFE4405F
    ),
    Bookmark(
        id = "google",
        title = "Google",
        url = "https://www.google.com",
        color = 0xFF4285F4
    ),
    Bookmark(
        id = "twitter",
        title = "Twitter",
        url = "https://twitter.com",
        color = 0xFF1DA1F2
    )
)
