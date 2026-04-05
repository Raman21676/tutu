package com.nova.browser.domain.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.nova.browser.data.local.db.DownloadDao
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    companion object {
        private const val TAG = "DownloadRepository"
    }

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadsByStatus(status: Int): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    fun getActiveDownloads(): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(DownloadEntity.STATUS_RUNNING)

    /**
     * Start a new download using the custom download service
     * This supports pause/resume and background downloads
     */
    suspend fun startDownload(url: String, fileName: String, mimeType: String): Long {
        // Generate unique ID
        val downloadId = System.currentTimeMillis()
        
        // Save to database first
        val download = DownloadEntity(
            id = downloadId,
            url = url,
            fileName = fileName,
            mimeType = mimeType,
            filePath = "",
            totalBytes = 0,
            downloadedBytes = 0,
            status = DownloadEntity.STATUS_PENDING
        )
        
        downloadDao.insert(download)
        
        // Start the download service
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILE_NAME, fileName)
            putExtra(DownloadService.EXTRA_MIME_TYPE, mimeType)
        }
        
        context.startService(intent)
        
        Log.d(TAG, "Started download $downloadId: $fileName")
        return downloadId
    }

    /**
     * Pause a running download
     */
    fun pauseDownload(id: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startService(intent)
        Log.d(TAG, "Paused download $id")
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload(id: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startService(intent)
        Log.d(TAG, "Resumed download $id")
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(id: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startService(intent)
        Log.d(TAG, "Cancelled download $id")
    }

    suspend fun deleteDownload(id: Long) {
        downloadDao.deleteById(id)
    }

    suspend fun clearCompletedDownloads() {
        downloadDao.deleteByStatus(DownloadEntity.STATUS_SUCCESSFUL)
    }

    suspend fun clearAllDownloads() {
        downloadDao.deleteAll()
    }

    suspend fun insertDownload(download: DownloadEntity) {
        downloadDao.insert(download)
    }

    suspend fun updateDownload(download: DownloadEntity) {
        downloadDao.update(download)
    }

    suspend fun updateProgress(id: Long, bytes: Long, status: Int) {
        downloadDao.updateProgress(id, bytes, status)
    }

    suspend fun updateStatus(id: Long, status: Int) {
        downloadDao.updateStatus(id, status)
    }

    suspend fun updateTotalBytes(id: Long, totalBytes: Long) {
        downloadDao.updateTotalBytes(id, totalBytes)
    }

    suspend fun updateFilePath(id: Long, filePath: String) {
        downloadDao.updateFilePath(id, filePath)
    }

    fun startTracking(downloadId: Long) {
        // Not needed with custom download service
        // Progress is tracked internally
    }
}
