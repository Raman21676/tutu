package com.nova.browser.data.local.entity

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

// Default bookmarks for first launch - empty by default
val DefaultBookmarks = listOf<Bookmark>()
