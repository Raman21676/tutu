package com.nova.browser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.datastore.TutuSettings
import com.nova.browser.data.repository.BookmarkRepository
import com.nova.browser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    
    private val _settings = MutableStateFlow(TutuSettings())
    val settings: StateFlow<TutuSettings> = _settings.asStateFlow()
    
    private val _clearDataSuccess = MutableStateFlow(false)
    val clearDataSuccess: StateFlow<Boolean> = _clearDataSuccess.asStateFlow()
    
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
    
    class Factory(
        private val settingsRepository: SettingsRepository,
        private val bookmarkRepository: BookmarkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsRepository, bookmarkRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
