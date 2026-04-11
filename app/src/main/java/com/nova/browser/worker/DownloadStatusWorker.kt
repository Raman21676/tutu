package com.nova.browser.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.domain.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Worker that monitors download progress and ensures status is updated correctly.
 * This acts as a fallback to ensure downloads don't get stuck in "Downloading" state.
 */
@HiltWorker
class DownloadStatusWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "download_status_worker"
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val TAG = "DownloadStatusWorker"
        private const val MAX_WAIT_TIME_MS = 5 * 60 * 1000L // 5 minutes max
        private const val CHECK_INTERVAL_MS = 1000L // Check every second
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId == -1L) {
            Log.e(TAG, "Invalid download ID")
            return Result.failure()
        }

        Log.d(TAG, "Starting status monitor for download $downloadId")
        
        val startTime = System.currentTimeMillis()
        var lastBytes = 0L
        var stallCount = 0
        
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > MAX_WAIT_TIME_MS) {
                Log.w(TAG, "Download $downloadId timed out after ${elapsed}ms")
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                return Result.failure()
            }

            val download = downloadRepository.getDownloadById(downloadId)
            
            when (download?.status) {
                DownloadEntity.STATUS_SUCCESSFUL -> {
                    Log.d(TAG, "Download $downloadId completed successfully")
                    return Result.success()
                }
                DownloadEntity.STATUS_FAILED -> {
                    Log.d(TAG, "Download $downloadId failed")
                    return Result.failure()
                }
                DownloadEntity.STATUS_PAUSED -> {
                    // Keep waiting while paused
                    delay(CHECK_INTERVAL_MS)
                    continue
                }
                DownloadEntity.STATUS_RUNNING -> {
                    // Check for stalled download (no progress for 30 seconds)
                    if (download.downloadedBytes == lastBytes) {
                        stallCount++
                        if (stallCount > 30) { // 30 seconds with no progress
                            Log.w(TAG, "Download $downloadId appears stalled, marking as failed")
                            downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                            return Result.failure()
                        }
                    } else {
                        stallCount = 0
                        lastBytes = download.downloadedBytes
                    }
                }
                else -> {
                    // Pending or unknown state, just wait
                }
            }
            
            delay(CHECK_INTERVAL_MS)
        }
    }
}
