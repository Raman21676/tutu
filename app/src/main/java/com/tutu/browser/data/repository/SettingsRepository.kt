package com.tutu.browser.data.repository

import com.tutu.browser.data.local.datastore.SettingsDataStore
import com.tutu.browser.data.local.datastore.TutuSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {
    
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
    
    suspend fun clearAll() {
        dataStore.clearAll()
    }
    
    suspend fun setBackgroundPlayback(enabled: Boolean) {
        dataStore.updateBackgroundPlayback(enabled)
    }
    
    suspend fun setFloatingWindow(enabled: Boolean) {
        dataStore.updateFloatingWindow(enabled)
    }
}
