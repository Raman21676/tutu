package com.nova.browser.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.datastore.TutuSettings
import com.nova.browser.data.repository.BookmarkRepository
import com.nova.browser.data.repository.SettingsRepository
import com.nova.browser.domain.repository.HistoryRepository
import com.nova.browser.domain.repository.SiteSettingsRepository
import com.nova.browser.domain.repository.UserScriptRepository
import com.nova.browser.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val siteSettingsRepository: SiteSettingsRepository,
    private val userScriptRepository: UserScriptRepository,
    context: Context
) : ViewModel() {
    
    private val backupManager = BackupManager(
        context,
        settingsRepository,
        bookmarkRepository,
        historyRepository,
        siteSettingsRepository,
        userScriptRepository
    )
    
    private val _settings = MutableStateFlow(TutuSettings())
    val settings: StateFlow<TutuSettings> = _settings.asStateFlow()
    
    private val _clearDataSuccess = MutableStateFlow(false)
    val clearDataSuccess: StateFlow<Boolean> = _clearDataSuccess.asStateFlow()

    private val _backupResult = MutableStateFlow<String?>(null)
    val backupResult: StateFlow<String?> = _backupResult.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.settings.collect {
                _settings.value = it
            }
        }
    }
    
    fun setHttpsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHttpsEnabled(enabled)
        }
    }
    
    fun setFullscreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFullscreen(enabled)
        }
    }
    
    fun setAutoRotate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoRotate(enabled)
        }
    }
    
    fun setRememberChoice(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRememberChoice(enabled)
        }
    }
    
    fun clearData() {
        viewModelScope.launch {
            settingsRepository.clearAll()
            bookmarkRepository.updateBookmarks(emptyList())
            _clearDataSuccess.value = true
        }
    }
    
    fun onClearDataAcknowledged() {
        _clearDataSuccess.value = false
    }

    fun setSearchEngine(engine: String) {
        viewModelScope.launch {
            settingsRepository.setSearchEngine(engine)
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAdBlockEnabled(enabled)
        }
    }

    fun setDownloadDirectory(uri: String) {
        viewModelScope.launch {
            settingsRepository.setDownloadDirectory(uri)
        }
    }

    fun setDesktopMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDesktopMode(enabled)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFollowSystemTheme(enabled)
        }
    }

    fun addCustomSearchEngine(name: String, url: String) {
        viewModelScope.launch {
            val current = _settings.value.customSearchEngines.toMutableList()
            current.add(com.nova.browser.data.model.CustomSearchEngine(name, url))
            settingsRepository.setCustomSearchEngines(current)
        }
    }

    fun deleteCustomSearchEngine(index: Int) {
        viewModelScope.launch {
            val current = _settings.value.customSearchEngines.toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                settingsRepository.setCustomSearchEngines(current)
            }
        }
    }

    fun updateCustomSearchEngine(index: Int, name: String, url: String) {
        viewModelScope.launch {
            val current = _settings.value.customSearchEngines.toMutableList()
            if (index in current.indices) {
                current[index] = com.nova.browser.data.model.CustomSearchEngine(name, url)
                settingsRepository.setCustomSearchEngines(current)
            }
        }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            val result = backupManager.exportToDownloads(context)
            _backupResult.value = result.getOrElse { "Export failed: ${it.message}" }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportToUri(uri)
            _backupResult.value = result.getOrElse { "Export failed: ${it.message}" }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importFromUri(uri)
            _backupResult.value = result.getOrElse { "Import failed: ${it.message}" }
        }
    }

    fun onBackupResultAcknowledged() {
        _backupResult.value = null
    }
    
    class Factory(
        private val settingsRepository: SettingsRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val historyRepository: HistoryRepository,
        private val siteSettingsRepository: SiteSettingsRepository,
        private val userScriptRepository: UserScriptRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    settingsRepository,
                    bookmarkRepository,
                    historyRepository,
                    siteSettingsRepository,
                    userScriptRepository,
                    context
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
