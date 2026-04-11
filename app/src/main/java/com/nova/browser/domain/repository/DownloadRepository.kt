package com.nova.browser.domain.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nova.browser.data.local.db.DownloadDao
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.service.DownloadService
import com.nova.browser.worker.DownloadStatusWorker
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

    suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    /**
     * Start a new download using the custom download service.
     * Supports pause/resume via OkHttp with HTTP Range headers.
     */
    suspend fun startDownload(
        url: String,
        fileName: String,
        mimeType: String,
        cookies: String? = null,
        userAgent: String? = null
    ): Long {
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
            cookies?.let { putExtra(DownloadService.EXTRA_COOKIES, it) }
            userAgent?.let { putExtra(DownloadService.EXTRA_USER_AGENT, it) }
        }

        // Use startForegroundService on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // Enqueue status monitoring worker as fallback
        enqueueStatusWorker(downloadId)

        Log.d(TAG, "Started download $downloadId: $fileName (cookies=${cookies != null}, ua=${userAgent != null})")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
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

    suspend fun updateFileName(id: Long, fileName: String) {
        downloadDao.updateFileName(id, fileName)
    }

    /**
     * Enqueue a worker to monitor download status and ensure it doesn't get stuck.
     */
    private fun enqueueStatusWorker(downloadId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadStatusWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(DownloadStatusWorker.KEY_DOWNLOAD_ID, downloadId)
                    .build()
            )
            .addTag("download_$downloadId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${DownloadStatusWorker.WORK_NAME}_$downloadId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
