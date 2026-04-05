package com.nova.browser.service

import android.app.DownloadManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.domain.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "download_worker"
        const val KEY_DOWNLOAD_ID = "download_id"
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId == -1L) {
            return Result.failure()
        }

        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Poll download progress
        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloading = false
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloading = false
                    }
                    else -> {
                        // Still downloading, wait and poll again
                        delay(500)
                    }
                }
            } else {
                downloading = false
            }
            cursor.close()
        }

        return Result.success()
    }
}
