package com.nova.browser.data.local.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nova.browser.data.local.entity.Bookmark
import com.nova.browser.data.local.entity.DefaultBookmarks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tutu_settings")

class SettingsDataStore(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        const val TAG = "SettingsDataStore"
        
        // Toggles
        val HTTPS_ENABLED = booleanPreferencesKey("https_enabled")
        val FULLSCREEN = booleanPreferencesKey("fullscreen")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FOLLOW_SYSTEM_THEME = booleanPreferencesKey("follow_system_theme")
        
        // Session persistence
        val LAST_URL = stringPreferencesKey("last_url")
        val LAST_URL_INPUT = stringPreferencesKey("last_url_input")
        
        // Other settings
        val REMEMBER_CHOICE = booleanPreferencesKey("remember_choice")
        val BOOKMARKS = stringPreferencesKey("bookmarks")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val DOWNLOAD_DIR_URI = stringPreferencesKey("download_dir_uri")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
    }
    
    val settings: Flow<TutuSettings> = context.dataStore.data
        .catch { exception ->
            // If there's an error reading preferences, emit defaults
            Log.e(TAG, "Error reading preferences", exception)
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs ->
            try {
                TutuSettings(
                    httpsEnabled = prefs[HTTPS_ENABLED] ?: true,
                    fullscreen = prefs[FULLSCREEN] ?: true,
                    autoRotate = prefs[AUTO_ROTATE] ?: true,
                    darkMode = prefs[DARK_MODE] ?: false,
                    followSystemTheme = prefs[FOLLOW_SYSTEM_THEME] ?: true,
                    rememberChoice = prefs[REMEMBER_CHOICE] ?: false,
                    lastUrl = prefs[LAST_URL],
                    lastUrlInput = prefs[LAST_URL_INPUT],
                    bookmarks = prefs[BOOKMARKS]?.let { 
                        try {
                            json.decodeFromString(it)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing bookmarks", e)
                            DefaultBookmarks
                        }
                    } ?: DefaultBookmarks,
                    isFirstLaunch = prefs[FIRST_LAUNCH] ?: true,
                    searchEngine = prefs[SEARCH_ENGINE] ?: "GOOGLE",
                    adBlockEnabled = prefs[AD_BLOCK_ENABLED] ?: true,
                    downloadDirUri = prefs[DOWNLOAD_DIR_URI] ?: "",
                    desktopMode = prefs[DESKTOP_MODE] ?: false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error creating settings", e)
                TutuSettings() // Return defaults
            }
        }
    
    suspend fun updateHttpsEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[HTTPS_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving HTTPS setting", e)
        }
    }
    
    suspend fun updateFullscreen(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[FULLSCREEN] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving fullscreen setting", e)
        }
    }
    
    suspend fun updateAutoRotate(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[AUTO_ROTATE] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving auto-rotate setting", e)
        }
    }
    
    suspend fun updateDarkMode(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[DARK_MODE] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dark mode setting", e)
        }
    }
    
    suspend fun updateFollowSystemTheme(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[FOLLOW_SYSTEM_THEME] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving follow system theme setting", e)
        }
    }
    
    suspend fun updateRememberChoice(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[REMEMBER_CHOICE] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving remember choice setting", e)
        }
    }
    
    suspend fun saveLastUrl(url: String?) {
        try {
            context.dataStore.edit { prefs ->
                if (url != null) {
                    prefs[LAST_URL] = url
                } else {
                    prefs.remove(LAST_URL)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving last URL", e)
        }
    }
    
    suspend fun saveLastUrlInput(input: String?) {
        try {
            context.dataStore.edit { prefs ->
                if (input != null && input.isNotBlank()) {
                    prefs[LAST_URL_INPUT] = input
                } else {
                    prefs.remove(LAST_URL_INPUT)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving URL input", e)
        }
    }
    
    suspend fun saveBookmarks(bookmarks: List<Bookmark>) {
        try {
            context.dataStore.edit { prefs ->
                prefs[BOOKMARKS] = json.encodeToString(bookmarks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bookmarks", e)
        }
    }
    
    suspend fun setFirstLaunchComplete() {
        try {
            context.dataStore.edit { prefs ->
                prefs[FIRST_LAUNCH] = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving first launch", e)
        }
    }
    
    suspend fun updateSearchEngine(engine: String) {
        try {
            context.dataStore.edit { prefs ->
                prefs[SEARCH_ENGINE] = engine
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving search engine", e)
        }
    }

    suspend fun updateAdBlockEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[AD_BLOCK_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ad block setting", e)
        }
    }

    suspend fun updateDownloadDirectory(uri: String) {
        try {
            context.dataStore.edit { prefs ->
                if (uri.isEmpty()) {
                    prefs.remove(DOWNLOAD_DIR_URI)
                } else {
                    prefs[DOWNLOAD_DIR_URI] = uri
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving download directory", e)
        }
    }

    suspend fun updateDesktopMode(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[DESKTOP_MODE] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving desktop mode setting", e)
        }
    }

    suspend fun clearAll() {
        try {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data", e)
        }
    }
}

data class TutuSettings(
    val httpsEnabled: Boolean = true,
    val fullscreen: Boolean = true,
    val autoRotate: Boolean = true,
    val darkMode: Boolean = false,
    val followSystemTheme: Boolean = true,
    val rememberChoice: Boolean = false,
    val lastUrl: String? = null,
    val lastUrlInput: String? = null,
    val bookmarks: List<Bookmark> = DefaultBookmarks,
    val isFirstLaunch: Boolean = true,
    val searchEngine: String = "GOOGLE",
    val adBlockEnabled: Boolean = true,
    val downloadDirUri: String = "",
    val desktopMode: Boolean = false
)
