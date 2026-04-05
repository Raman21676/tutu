package com.nova.browser.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nova.browser.MainActivity
import com.nova.browser.R
import com.nova.browser.data.local.db.DownloadEntity
import com.nova.browser.domain.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = ConcurrentHashMap<Long, DownloadJob>()
    
    private val binder = LocalBinder()
    
    private val _downloadProgress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = _downloadProgress.asStateFlow()

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_DOWNLOAD"
        const val ACTION_PAUSE = "PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        const val EXTRA_URL = "url"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_MIME_TYPE = "mime_type"
        
        private const val TAG = "DownloadService"
    }

    data class DownloadProgress(
        val downloadId: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val status: Int,
        val speed: String = ""
    )

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "DownloadService created")
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1) ?: -1
        
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return START_NOT_STICKY
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "*/*"
                startDownload(downloadId, url, fileName, mimeType)
            }
            ACTION_PAUSE -> pauseDownload(downloadId)
            ACTION_RESUME -> resumeDownload(downloadId)
            ACTION_CANCEL -> cancelDownload(downloadId)
        }
        
        return START_NOT_STICKY
    }

    private fun startDownload(downloadId: Long, url: String, fileName: String, mimeType: String) {
        if (activeDownloads.containsKey(downloadId)) {
            Log.d(TAG, "Download $downloadId already active")
            return
        }

        val downloadDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: File(filesDir, "downloads")
        downloadDir.mkdirs()
        
        val file = File(downloadDir, fileName)

        val job = serviceScope.launch {
            try {
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_RUNNING)
                performDownload(downloadId, url, file, mimeType)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
            } finally {
                activeDownloads.remove(downloadId)
                updateNotification()
            }
        }

        activeDownloads[downloadId] = DownloadJob(job, file, url, mimeType)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private suspend fun performDownload(
        downloadId: Long,
        url: String,
        file: File,
        mimeType: String
    ) {
        val existingBytes = if (file.exists()) file.length() else 0
        
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) TuTu Browser")
        
        // Resume support - request partial content
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("HTTP ${response.code}")
                }

                val totalBytes = response.header("Content-Length")?.toLongOrNull()?.plus(existingBytes) 
                    ?: -1
                
                downloadRepository.updateTotalBytes(downloadId, totalBytes)

                val body = response.body ?: throw Exception("Empty response")
                
                val randomAccessFile = RandomAccessFile(file, "rw")
                randomAccessFile.seek(existingBytes)
                
                val buffer = ByteArray(8192)
                var downloadedBytes = existingBytes
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytes = existingBytes
                var isPaused = false

                body.byteStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if paused
                        while (activeDownloads[downloadId]?.isPaused == true) {
                            isPaused = true
                            delay(100)
                        }
                        
                        if (isPaused) {
                            // Update status to running after resume
                            downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_RUNNING)
                            isPaused = false
                        }

                        // Check if cancelled
                        if (!activeDownloads.containsKey(downloadId)) {
                            randomAccessFile.close()
                            return
                        }

                        randomAccessFile.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress every 500ms
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > 500) {
                            val speed = calculateSpeed(downloadedBytes - lastBytes, currentTime - lastUpdateTime)
                            
                            downloadRepository.updateProgress(downloadId, downloadedBytes, DownloadEntity.STATUS_RUNNING)
                            
                            _downloadProgress.value = _downloadProgress.value + (downloadId to DownloadProgress(
                                downloadId = downloadId,
                                bytesDownloaded = downloadedBytes,
                                totalBytes = totalBytes,
                                status = DownloadEntity.STATUS_RUNNING,
                                speed = speed
                            ))
                            
                            updateNotification(file.name, downloadedBytes, totalBytes, activeDownloads.size)
                            
                            lastUpdateTime = currentTime
                            lastBytes = downloadedBytes
                        }
                    }
                }
                
                randomAccessFile.close()
                
                // Download complete
                downloadRepository.updateProgress(downloadId, downloadedBytes, DownloadEntity.STATUS_SUCCESSFUL)
                downloadRepository.updateFilePath(downloadId, file.absolutePath)
                
                _downloadProgress.value = _downloadProgress.value + (downloadId to DownloadProgress(
                    downloadId = downloadId,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    status = DownloadEntity.STATUS_SUCCESSFUL,
                    speed = ""
                ))
                
                Log.d(TAG, "Download $downloadId completed: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}")
            downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
            throw e
        }
    }

    private fun calculateSpeed(bytesDelta: Long, timeDeltaMs: Long): String {
        val speedBps = (bytesDelta * 1000) / timeDeltaMs.coerceAtLeast(1)
        return when {
            speedBps >= 1024 * 1024 -> String.format("%.1f MB/s", speedBps / (1024.0 * 1024.0))
            speedBps >= 1024 -> String.format("%.1f KB/s", speedBps / 1024.0)
            else -> "$speedBps B/s"
        }
    }

    fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId]?.isPaused = true
        serviceScope.launch {
            downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_PAUSED)
        }
        Log.d(TAG, "Download $downloadId paused")
    }

    fun resumeDownload(downloadId: Long) {
        activeDownloads[downloadId]?.isPaused = false
        // Status will be updated to RUNNING in performDownload
        Log.d(TAG, "Download $downloadId resumed")
    }

    fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId]?.job?.cancel()
        activeDownloads.remove(downloadId)
        serviceScope.launch {
            downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
        }
        Log.d(TAG, "Download $downloadId cancelled")
        
        if (activeDownloads.isEmpty()) {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download notifications"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading...")
            .setContentText("Preparing download")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, 0, true)
            .build()
    }

    private fun updateNotification(
        fileName: String = "",
        downloadedBytes: Long = 0,
        totalBytes: Long = -1,
        activeCount: Int = 0
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val progress = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt()
        } else 0
        
        val sizeText = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            formatBytes(downloadedBytes)
        }
        
        val title = if (activeCount > 1) {
            "$activeCount downloads in progress"
        } else {
            "Downloading $fileName"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(sizeText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, totalBytes <= 0)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "DownloadService destroyed")
    }

    data class DownloadJob(
        val job: Job,
        val file: File,
        val url: String,
        val mimeType: String,
        @Volatile var isPaused: Boolean = false
    )
}
