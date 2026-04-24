package com.nova.browser.data.model

import com.nova.browser.data.local.entity.Bookmark
import kotlinx.serialization.Serializable

/**
 * Serializable representation of all user data for import/export.
 * Versioned for forward compatibility.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val settings: BackupSettings = BackupSettings(),
    val bookmarks: List<Bookmark> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList(),
    val siteSettings: List<BackupSiteSettings> = emptyList(),
    val userScripts: List<BackupUserScript> = emptyList(),
    val customSearchEngines: List<CustomSearchEngine> = emptyList()
)

@Serializable
data class BackupSettings(
    val httpsEnabled: Boolean = true,
    val fullscreen: Boolean = true,
    val autoRotate: Boolean = true,
    val darkMode: Boolean = false,
    val followSystemTheme: Boolean = true,
    val rememberChoice: Boolean = false,
    val searchEngine: String = "GOOGLE",
    val adBlockEnabled: Boolean = true,
    val desktopMode: Boolean = false
)

@Serializable
data class BackupHistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long,
    val visitCount: Int
)

@Serializable
data class BackupSiteSettings(
    val domain: String,
    val javaScriptEnabled: Boolean? = null,
    val adBlockEnabled: Boolean? = null,
    val desktopMode: Boolean? = null,
    val thirdPartyCookiesEnabled: Boolean? = null,
    val zoomLevel: Float? = null,
    val userAgent: String? = null
)

@Serializable
data class BackupUserScript(
    val name: String = "",
    val namespace: String = "",
    val version: String = "1.0",
    val description: String = "",
    val matchPatterns: String = "",
    val excludePatterns: String = "",
    val code: String = "",
    val enabled: Boolean = true,
    val runAt: String = "document-end",
    val updateUrl: String = "",
    val lastUpdated: Long = 0
)
