package com.nova.browser.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WebViewState(
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
    val title: String = "",
    val favicon: Bitmap? = null
)

class WebViewModel(
    private val settingsRepository: SettingsRepository,
    private val initialUrl: String
) : ViewModel() {
    
    private val _state = MutableStateFlow(WebViewState(url = initialUrl))
    val state: StateFlow<WebViewState> = _state.asStateFlow()
    
    private val _fullscreen = MutableStateFlow(false)
    val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()
    
    private val _adBlockEnabled = MutableStateFlow(true)
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _desktopMode = MutableStateFlow(false)
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _fullscreen.value = settings.fullscreen
                _adBlockEnabled.value = settings.adBlockEnabled
                _desktopMode.value = settings.desktopMode
            }
        }
    }
    
    fun onPageStarted(url: String) {
        _state.value = _state.value.copy(
            url = url,
            isLoading = true,
            progress = 0,
            error = null
        )
    }
    
    fun onPageFinished(url: String) {
        _state.value = _state.value.copy(
            url = url,
            isLoading = false,
            progress = 100
        )
        // Save last URL
        viewModelScope.launch {
            settingsRepository.saveLastUrl(url)
        }
    }
    
    fun onUrlChanged(url: String) {
        _state.value = _state.value.copy(url = url)
    }
    
    fun onProgressChanged(progress: Int) {
        _state.value = _state.value.copy(progress = progress)
    }
    
    fun onReceivedError(errorCode: Int, description: String) {
        _state.value = _state.value.copy(
            isLoading = false,
            error = description
        )
    }
    
    fun onReceivedTitle(title: String) {
        _state.value = _state.value.copy(title = title)
    }
    
    fun onReceivedIcon(icon: Bitmap?) {
        _state.value = _state.value.copy(favicon = icon)
    }
    
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _state.value = _state.value.copy(
            canGoBack = canGoBack,
            canGoForward = canGoForward
        )
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun incrementBlockedCount() {
        _blockedCount.value = _blockedCount.value + 1
    }

    fun resetBlockedCount() {
        _blockedCount.value = 0
    }
    
    fun loadUrl(url: String) {
        _state.value = _state.value.copy(url = url, error = null)
    }
    
    fun reload() {
        // Trigger reload by emitting same URL
        val currentUrl = _state.value.url
        _state.value = _state.value.copy(url = "")
        _state.value = _state.value.copy(url = currentUrl, error = null)
    }
    
    fun setFullscreen(fullscreen: Boolean) {
        _fullscreen.value = fullscreen
    }
    
    class Factory(
        private val settingsRepository: SettingsRepository,
        private val initialUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WebViewModel::class.java)) {
                return WebViewModel(settingsRepository, initialUrl) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
