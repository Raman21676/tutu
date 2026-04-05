package com.nova.browser.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_settings")
data class SiteSettingsEntity(
    @PrimaryKey
    val domain: String,
    val javaScriptEnabled: Boolean? = null,
    val adBlockEnabled: Boolean? = null,
    val desktopMode: Boolean? = null,
    val thirdPartyCookiesEnabled: Boolean? = null,
    val zoomLevel: Float? = null,
    val userAgent: String? = null
)
