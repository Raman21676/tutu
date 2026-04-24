package com.nova.browser.util

import android.content.Context
import android.net.Uri
import com.nova.browser.data.local.datastore.SettingsDataStore
import com.nova.browser.data.local.db.HistoryEntity
import com.nova.browser.data.local.db.SiteSettingsEntity
import com.nova.browser.data.local.db.UserScriptEntity
import com.nova.browser.data.model.*
import com.nova.browser.data.repository.BookmarkRepository
import com.nova.browser.data.repository.SettingsRepository
import com.nova.browser.domain.repository.HistoryRepository
import com.nova.browser.domain.repository.SiteSettingsRepository
import com.nova.browser.domain.repository.UserScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val siteSettingsRepository: SiteSettingsRepository,
    private val userScriptRepository: UserScriptRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.settings.first()
            val bookmarks = settings.bookmarks
            val history = historyRepository.getAllHistory().first().map {
                BackupHistoryEntry(
                    url = it.url,
                    title = it.title,
                    timestamp = it.timestamp,
                    visitCount = it.visitCount
                )
            }
            val siteSettings = siteSettingsRepository.getAll().map {
                BackupSiteSettings(
                    domain = it.domain,
                    javaScriptEnabled = it.javaScriptEnabled,
                    adBlockEnabled = it.adBlockEnabled,
                    desktopMode = it.desktopMode,
                    thirdPartyCookiesEnabled = it.thirdPartyCookiesEnabled,
                    zoomLevel = it.zoomLevel,
                    userAgent = it.userAgent
                )
            }
            val userScripts = userScriptRepository.getAllScripts().first().map {
                BackupUserScript(
                    name = it.name,
                    namespace = it.namespace,
                    version = it.version,
                    description = it.description,
                    matchPatterns = it.matchPatterns,
                    excludePatterns = it.excludePatterns,
                    code = it.code,
                    enabled = it.enabled,
                    runAt = it.runAt,
                    updateUrl = it.updateUrl,
                    lastUpdated = it.lastUpdated
                )
            }

            val backup = BackupData(
                version = 1,
                settings = BackupSettings(
                    httpsEnabled = settings.httpsEnabled,
                    fullscreen = settings.fullscreen,
                    autoRotate = settings.autoRotate,
                    darkMode = settings.darkMode,
                    followSystemTheme = settings.followSystemTheme,
                    rememberChoice = settings.rememberChoice,
                    searchEngine = settings.searchEngine,
                    adBlockEnabled = settings.adBlockEnabled,
                    desktopMode = settings.desktopMode
                ),
                bookmarks = bookmarks,
                history = history,
                siteSettings = siteSettings,
                userScripts = userScripts,
                customSearchEngines = settings.customSearchEngines
            )

            val jsonString = json.encodeToString(backup)
            val fileName = "nova_backup_${System.currentTimeMillis()}.json"
            val downloadDir = java.io.File(android.os.Environment.getExternalStorageDirectory(), "Nova Downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val file = java.io.File(downloadDir, fileName)
            file.writeText(jsonString, Charsets.UTF_8)

            Result.success("Exported to ${file.absolutePath}\n${bookmarks.size} bookmarks, ${history.size} history entries, ${userScripts.size} scripts")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.settings.first()
            val bookmarks = settings.bookmarks
            val history = historyRepository.getAllHistory().first().map {
                BackupHistoryEntry(
                    url = it.url,
                    title = it.title,
                    timestamp = it.timestamp,
                    visitCount = it.visitCount
                )
            }
            val siteSettings = siteSettingsRepository.getAll().map {
                BackupSiteSettings(
                    domain = it.domain,
                    javaScriptEnabled = it.javaScriptEnabled,
                    adBlockEnabled = it.adBlockEnabled,
                    desktopMode = it.desktopMode,
                    thirdPartyCookiesEnabled = it.thirdPartyCookiesEnabled,
                    zoomLevel = it.zoomLevel,
                    userAgent = it.userAgent
                )
            }
            val userScripts = userScriptRepository.getAllScripts().first().map {
                BackupUserScript(
                    name = it.name,
                    namespace = it.namespace,
                    version = it.version,
                    description = it.description,
                    matchPatterns = it.matchPatterns,
                    excludePatterns = it.excludePatterns,
                    code = it.code,
                    enabled = it.enabled,
                    runAt = it.runAt,
                    updateUrl = it.updateUrl,
                    lastUpdated = it.lastUpdated
                )
            }

            val backup = BackupData(
                version = 1,
                settings = BackupSettings(
                    httpsEnabled = settings.httpsEnabled,
                    fullscreen = settings.fullscreen,
                    autoRotate = settings.autoRotate,
                    darkMode = settings.darkMode,
                    followSystemTheme = settings.followSystemTheme,
                    rememberChoice = settings.rememberChoice,
                    searchEngine = settings.searchEngine,
                    adBlockEnabled = settings.adBlockEnabled,
                    desktopMode = settings.desktopMode
                ),
                bookmarks = bookmarks,
                history = history,
                siteSettings = siteSettings,
                userScripts = userScripts,
                customSearchEngines = settings.customSearchEngines
            )

            val jsonString = json.encodeToString(backup)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Result.success("Exported ${bookmarks.size} bookmarks, ${history.size} history entries, ${userScripts.size} scripts")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            val backup = json.decodeFromString<BackupData>(jsonString)

            // Restore settings
            val dataStore = SettingsDataStore(context)
            dataStore.updateHttpsEnabled(backup.settings.httpsEnabled)
            dataStore.updateFullscreen(backup.settings.fullscreen)
            dataStore.updateAutoRotate(backup.settings.autoRotate)
            dataStore.updateDarkMode(backup.settings.darkMode)
            dataStore.updateFollowSystemTheme(backup.settings.followSystemTheme)
            dataStore.updateRememberChoice(backup.settings.rememberChoice)
            dataStore.updateSearchEngine(backup.settings.searchEngine)
            dataStore.updateAdBlockEnabled(backup.settings.adBlockEnabled)
            dataStore.updateDesktopMode(backup.settings.desktopMode)

            // Restore bookmarks
            bookmarkRepository.updateBookmarks(backup.bookmarks)

            // Restore custom search engines
            dataStore.updateCustomSearchEngines(backup.customSearchEngines)

            // Restore history
            historyRepository.clearAllHistory()
            backup.history.forEach { entry ->
                historyRepository.insertHistory(
                    HistoryEntity(
                        url = entry.url,
                        title = entry.title,
                        timestamp = entry.timestamp,
                        visitCount = entry.visitCount
                    )
                )
            }

            // Restore site settings
            siteSettingsRepository.clearAllSettings()
            backup.siteSettings.forEach {
                siteSettingsRepository.saveSettings(
                    SiteSettingsEntity(
                        domain = it.domain,
                        javaScriptEnabled = it.javaScriptEnabled,
                        adBlockEnabled = it.adBlockEnabled,
                        desktopMode = it.desktopMode,
                        thirdPartyCookiesEnabled = it.thirdPartyCookiesEnabled,
                        zoomLevel = it.zoomLevel,
                        userAgent = it.userAgent
                    )
                )
            }

            // Restore user scripts (clear existing first to avoid duplicates)
            val existingScripts = userScriptRepository.getAllScripts().first()
            existingScripts.forEach { userScriptRepository.delete(it) }
            backup.userScripts.forEach { script ->
                userScriptRepository.insert(
                    UserScriptEntity(
                        name = script.name,
                        namespace = script.namespace,
                        version = script.version,
                        description = script.description,
                        matchPatterns = script.matchPatterns,
                        excludePatterns = script.excludePatterns,
                        code = script.code,
                        enabled = script.enabled,
                        runAt = script.runAt,
                        updateUrl = script.updateUrl,
                        lastUpdated = script.lastUpdated
                    )
                )
            }

            Result.success("Restored ${backup.bookmarks.size} bookmarks, ${backup.history.size} history entries, ${backup.userScripts.size} scripts")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
