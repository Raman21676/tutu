package com.tutu.browser.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tutu.browser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ThemeState(
    val darkMode: Boolean = false,
    val followSystemTheme: Boolean = true
)

class ThemeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    companion object {
        const val TAG = "ThemeViewModel"
    }
    
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.settings
                .catch { e ->
                    Log.e(TAG, "Error loading settings", e)
                    emit(com.tutu.browser.data.local.datastore.TutuSettings())
                }
                .collect { settings ->
                    _themeState.value = ThemeState(
                        darkMode = settings.darkMode,
                        followSystemTheme = settings.followSystemTheme
                    )
                }
        }
    }
    
    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
                return ThemeViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
