package com.nova.browser.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.datastore.TutuSettings
import com.nova.browser.data.local.entity.Bookmark
import com.nova.browser.data.local.entity.DefaultBookmarks
import com.nova.browser.data.repository.BookmarkRepository
import com.nova.browser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    companion object {
        const val TAG = "HomeViewModel"
    }
    
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()
    
    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()
    
    private val _lastUrl = MutableStateFlow<String?>(null)
    val lastUrl: StateFlow<String?> = _lastUrl.asStateFlow()
    
    private val _lastUrlInput = MutableStateFlow<String?>(null)
    val lastUrlInput: StateFlow<String?> = _lastUrlInput.asStateFlow()
    
    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()
    
    private val _settings = MutableStateFlow(TutuSettings())
    val settings: StateFlow<TutuSettings> = _settings.asStateFlow()
    
    init {
        viewModelScope.launch {
            bookmarkRepository.bookmarks
                .catch { e ->
                    Log.e(TAG, "Error loading bookmarks", e)
                    emit(emptyList())
                }
                .collect {
                    _bookmarks.value = it
                }
        }
        
        viewModelScope.launch {
            settingsRepository.settings
                .catch { e ->
                    Log.e(TAG, "Error loading settings", e)
                    emit(TutuSettings())
                }
                .collect { s ->
                    Log.d(TAG, "Settings loaded: https=${s.httpsEnabled}, input=${s.lastUrlInput}")
                    _settings.value = s
                    _lastUrl.value = s.lastUrl
                    _lastUrlInput.value = s.lastUrlInput
                    _isFirstLaunch.value = s.isFirstLaunch
                    
                    // Load default bookmarks on first launch
                    if (s.isFirstLaunch) {
                        bookmarkRepository.updateBookmarks(DefaultBookmarks)
                    }
                    
                    if (!s.isFirstLaunch && s.lastUrl != null && !s.rememberChoice && !_showRestoreDialog.value) {
                        _showRestoreDialog.value = true
                    }
                }
        }
    }
    
    fun dismissRestoreDialog() {
        _showRestoreDialog.value = false
    }
    
    fun onRestoreChoice(shouldRestore: Boolean, remember: Boolean) {
        viewModelScope.launch {
            if (remember) {
                settingsRepository.setRememberChoice(true)
            }
            if (!shouldRestore) {
                settingsRepository.saveLastUrl(null)
            }
            dismissRestoreDialog()
        }
    }
    
    fun saveUrlInput(input: String) {
        viewModelScope.launch {
            Log.d(TAG, "Saving URL input: $input")
            settingsRepository.saveLastUrlInput(input)
        }
    }
    
    fun setHttpsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Setting HTTPS: $enabled")
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

    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAdBlockEnabled(enabled)
        }
    }

    fun setSearchEngine(engine: String) {
        viewModelScope.launch {
            settingsRepository.setSearchEngine(engine)
        }
    }
    
    fun addBookmark(title: String, url: String, color: Long? = null) {
        viewModelScope.launch {
            bookmarkRepository.addBookmark(title, url, color)
        }
    }
    
    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(bookmarkId)
        }
    }
    
    fun onFirstLaunchComplete() {
        viewModelScope.launch {
            settingsRepository.setFirstLaunchComplete()
            _isFirstLaunch.value = false
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            bookmarkRepository.updateBookmarks(DefaultBookmarks)
        }
    }
    
    class Factory(
        private val bookmarkRepository: BookmarkRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(bookmarkRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
