package com.nova.browser.data.repository

import com.nova.browser.data.local.datastore.SettingsDataStore
import com.nova.browser.data.local.datastore.TutuSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    
    val settings: Flow<TutuSettings> = dataStore.settings
    
    suspend fun setHttpsEnabled(enabled: Boolean) {
        dataStore.updateHttpsEnabled(enabled)
    }
    
    suspend fun setFullscreen(enabled: Boolean) {
        dataStore.updateFullscreen(enabled)
    }
    
    suspend fun setAutoRotate(enabled: Boolean) {
        dataStore.updateAutoRotate(enabled)
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.updateDarkMode(enabled)
    }
    
    suspend fun setFollowSystemTheme(enabled: Boolean) {
        dataStore.updateFollowSystemTheme(enabled)
    }
    
    suspend fun setRememberChoice(enabled: Boolean) {
        dataStore.updateRememberChoice(enabled)
    }
    
    suspend fun saveLastUrl(url: String?) {
        dataStore.saveLastUrl(url)
    }
    
    suspend fun saveLastUrlInput(input: String?) {
        dataStore.saveLastUrlInput(input)
    }
    
    suspend fun setFirstLaunchComplete() {
        dataStore.setFirstLaunchComplete()
    }
    
    suspend fun setSearchEngine(engine: String) {
        dataStore.updateSearchEngine(engine)
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        dataStore.updateAdBlockEnabled(enabled)
    }

    suspend fun setDownloadDirectory(uri: String) {
        dataStore.updateDownloadDirectory(uri)
    }

    suspend fun clearAll() {
        dataStore.clearAll()
    }
}
