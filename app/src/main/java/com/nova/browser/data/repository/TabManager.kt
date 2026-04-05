package com.nova.browser.data.repository

import android.graphics.Bitmap
import com.nova.browser.data.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Manages browser tabs state
 * Singleton pattern for app-wide tab management
 */
class TabManager private constructor() {

    companion object {
        @Volatile
        private var instance: TabManager? = null

        fun getInstance(): TabManager {
            return instance ?: synchronized(this) {
                instance ?: TabManager().also { instance = it }
            }
        }
    }

    private val _tabs = MutableStateFlow<List<Tab>>(listOf(createNewTab()))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _currentTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val currentTabId: StateFlow<String> = _currentTabId.asStateFlow()

    val currentTab: Tab?
        get() = _tabs.value.find { it.id == _currentTabId.value }

    val tabCount: Int
        get() = _tabs.value.size

    /**
     * Create a new tab with optional initial URL
     */
    fun createNewTab(url: String = "", isIncognito: Boolean = false): Tab {
        return Tab(
            id = UUID.randomUUID().toString(),
            url = url,
            title = if (url.isEmpty()) "New Tab" else url,
            isIncognito = isIncognito
        )
    }

    /**
     * Add a new tab and switch to it
     */
    fun addTab(url: String = "", isIncognito: Boolean = false): Tab {
        val newTab = createNewTab(url, isIncognito)
        _tabs.update { it + newTab }
        _currentTabId.value = newTab.id
        return newTab
    }

    /**
     * Close a tab by ID. If it's the last tab, create a new empty one.
     */
    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // Last tab - reset it instead of closing
            _tabs.value = listOf(createNewTab())
            _currentTabId.value = _tabs.value.first().id
            return
        }

        val index = currentTabs.indexOfFirst { it.id == tabId }
        _tabs.update { it.filter { tab -> tab.id != tabId } }

        // Switch to adjacent tab if we closed the current one
        if (_currentTabId.value == tabId) {
            val newIndex = index.coerceAtMost(_tabs.value.size - 1)
            _currentTabId.value = _tabs.value[newIndex.coerceAtLeast(0)].id
        }
    }

    /**
     * Switch to a specific tab
     */
    fun switchToTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _currentTabId.value = tabId
        }
    }

    /**
     * Update tab information
     */
    fun updateTab(
        tabId: String,
        url: String? = null,
        title: String? = null,
        favicon: Bitmap? = null,
        isLoading: Boolean? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null
    ) {
        _tabs.update { tabs ->
            tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = url ?: tab.url,
                        title = title ?: tab.title,
                        favicon = favicon ?: tab.favicon,
                        isLoading = isLoading ?: tab.isLoading,
                        canGoBack = canGoBack ?: tab.canGoBack,
                        canGoForward = canGoForward ?: tab.canGoForward,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    tab
                }
            }
        }
    }

    /**
     * Close all tabs and create a new empty one
     */
    fun closeAllTabs() {
        _tabs.value = listOf(createNewTab())
        _currentTabId.value = _tabs.value.first().id
    }

    /**
     * Close all tabs except the current one
     */
    fun closeOtherTabs() {
        val currentId = _currentTabId.value
        _tabs.update { listOfNotNull(it.find { tab -> tab.id == currentId }) }
    }

    /**
     * Duplicate a tab
     */
    fun duplicateTab(tabId: String): Tab? {
        val tabToDuplicate = _tabs.value.find { it.id == tabId } ?: return null
        val newTab = tabToDuplicate.copy(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis()
        )
        _tabs.update { it + newTab }
        _currentTabId.value = newTab.id
        return newTab
    }
}
