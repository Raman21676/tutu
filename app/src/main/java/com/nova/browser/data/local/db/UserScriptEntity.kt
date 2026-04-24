package com.nova.browser.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user scripts (Greasemonkey/Tampermonkey style).
 */
@Entity(tableName = "user_scripts")
data class UserScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val namespace: String = "",
    val version: String = "1.0",
    val description: String = "",
    // Comma-separated list of @match patterns, e.g. "*://*.youtube.com/*,*://*.tiktok.com/*"
    val matchPatterns: String = "",
    // Comma-separated list of @exclude patterns
    val excludePatterns: String = "",
    // The JavaScript code to inject
    val code: String = "",
    // Whether the script is enabled
    val enabled: Boolean = true,
    // When to run: "document-start", "document-end", "document-idle"
    val runAt: String = "document-end",
    // URL to check for updates
    val updateUrl: String = "",
    // Last update timestamp
    val lastUpdated: Long = System.currentTimeMillis()
)
