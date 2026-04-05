package com.nova.browser.worker

import android.app.DownloadManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.domain.repository.DownloadRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun downloadRepository(): DownloadRepository
    }

    companion object {
        const val WORK_TAG = "download_tracker"
        const val KEY_DOWNLOAD_ID = "download_id"
        
        fun createInputData(downloadId: Long) = workDataOf(KEY_DOWNLOAD_ID to downloadId)
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return Result.failure()

        // Get repository via EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
        val downloadRepository = entryPoint.downloadRepository()

        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isComplete = false
        var attempts = 0
        val maxAttempts = 600 // 5 minutes max (600 * 500ms)

        while (!isComplete && attempts < maxAttempts) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor?.moveToFirst() == true) {
                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                
                val status = cursor.getInt(statusCol)
                val bytesDownloaded = cursor.getLong(bytesDownloadedCol)
                val bytesTotal = cursor.getLong(bytesTotalCol)
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val fileUri = cursor.getString(uriCol)
                        downloadRepository.updateProgress(downloadId, bytesDownloaded, DownloadEntity.STATUS_SUCCESSFUL)
                        // Save the file path/URI
                        fileUri?.let { uri ->
                            downloadRepository.updateFilePath(downloadId, uri)
                        }
                        isComplete = true
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloadRepository.updateProgress(downloadId, bytesDownloaded, DownloadEntity.STATUS_FAILED)
                        isComplete = true
                    }
                    else -> {
                        downloadRepository.updateProgress(downloadId, bytesDownloaded, DownloadEntity.STATUS_RUNNING)
                    }
                }
            }
            cursor?.close()
            
            if (!isComplete) {
                delay(500) // Poll every 500ms
                attempts++
            }
        }

        return if (isComplete) Result.success() else Result.retry()
    }
}
