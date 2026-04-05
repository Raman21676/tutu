package com.nova.browser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadEntity>> = _activeDownloads.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepository.getAllDownloads().collect { list ->
                _downloads.value = list
            }
        }

        viewModelScope.launch {
            downloadRepository.getDownloadsByStatus(DownloadEntity.STATUS_RUNNING).collect { list ->
                _activeDownloads.value = list
            }
        }
    }

    fun pauseDownload(id: Long) {
        downloadRepository.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        downloadRepository.resumeDownload(id)
    }

    fun cancelDownload(id: Long) {
        downloadRepository.cancelDownload(id)
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            downloadRepository.clearCompletedDownloads()
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadRepository.clearAllDownloads()
        }
    }

    fun getStatusText(status: Int): String {
        return when (status) {
            DownloadEntity.STATUS_PENDING -> "Pending"
            DownloadEntity.STATUS_RUNNING -> "Downloading"
            DownloadEntity.STATUS_PAUSED -> "Paused"
            DownloadEntity.STATUS_SUCCESSFUL -> "Completed"
            DownloadEntity.STATUS_FAILED -> "Failed"
            else -> "Unknown"
        }
    }

    fun getStatusColor(status: Int): androidx.compose.ui.graphics.Color {
        return when (status) {
            DownloadEntity.STATUS_PENDING -> androidx.compose.ui.graphics.Color.Gray
            DownloadEntity.STATUS_RUNNING -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
            DownloadEntity.STATUS_PAUSED -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            DownloadEntity.STATUS_SUCCESSFUL -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            DownloadEntity.STATUS_FAILED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }
}
