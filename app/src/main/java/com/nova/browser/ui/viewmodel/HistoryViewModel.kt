package com.nova.browser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.db.HistoryEntity
import com.nova.browser.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val history: StateFlow<List<HistoryEntity>> = _history.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredHistory = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val filteredHistory: StateFlow<List<HistoryEntity>> = _filteredHistory.asStateFlow()

    companion object {
        const val MAX_HISTORY_DAYS = 30
    }

    init {
        viewModelScope.launch {
            // Auto-delete history older than 30 days on init
            historyRepository.deleteOlderThan(MAX_HISTORY_DAYS)
            
            historyRepository.getAllHistory().collect { list ->
                _history.value = list
                filterHistory()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterHistory()
    }

    private fun filterHistory() {
        val query = _searchQuery.value.trim()
        _filteredHistory.value = if (query.isEmpty()) {
            _history.value
        } else {
            _history.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }

    fun clearHistoryOlderThan(days: Int) {
        viewModelScope.launch {
            historyRepository.deleteOlderThan(days)
        }
    }
}
