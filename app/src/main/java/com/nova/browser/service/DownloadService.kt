package com.nova.browser.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
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
import java.io.FileOutputStream
import java.io.OutputStream
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
        const val CHANNEL_COMPLETE_ID = "download_complete_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_DOWNLOAD"
        const val ACTION_PAUSE = "PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        const val EXTRA_URL = "url"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_MIME_TYPE = "mime_type"
        const val EXTRA_COOKIES = "cookies"
        const val EXTRA_USER_AGENT = "user_agent"
        const val EXTRA_REFERER = "referer"
        const val EXTRA_DIR_URI = "dir_uri"
        
        const val DOWNLOAD_FOLDER_NAME = "Nova Downloads"
        
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
        createNotificationChannels()
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
                val cookies = intent.getStringExtra(EXTRA_COOKIES)
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
                val referer = intent.getStringExtra(EXTRA_REFERER)
                val dirUri = intent.getStringExtra(EXTRA_DIR_URI)
                startDownload(downloadId, url, fileName, mimeType, cookies, userAgent, referer, dirUri)
            }
            ACTION_PAUSE -> pauseDownload(downloadId)
            ACTION_RESUME -> resumeDownload(downloadId)
            ACTION_CANCEL -> cancelDownload(downloadId)
        }
        
        return START_NOT_STICKY
    }

    /**
     * Get the download directory.
     * If a SAF tree URI is provided (user-chosen folder), convert it to a File path.
     * Falls back to public Downloads/Nova Downloads/ on any failure.
     */
    private fun getDownloadDir(dirUri: String? = null): File {
        if (!dirUri.isNullOrEmpty()) {
            val path = convertSAFUriToPath(dirUri)
            if (path != null) {
                val dir = File(path)
                if (dir.exists() || dir.mkdirs()) {
                    Log.d(TAG, "Using custom download dir: ${dir.absolutePath}")
                    return dir
                }
            }
        }
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val novaDir = File(publicDownloads, DOWNLOAD_FOLDER_NAME)
        if (!novaDir.exists()) {
            novaDir.mkdirs()
        }
        return novaDir
    }

    /**
     * Convert a SAF tree URI (content://com.android.externalstorage.documents/tree/primary%3A...)
     * to an absolute File path for primary (internal) storage.
     * Returns null for external SD cards or unknown authorities.
     */
    private fun convertSAFUriToPath(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.authority == "com.android.externalstorage.documents") {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val split = docId.split(":")
                if (split.size >= 2 && split[0] == "primary") {
                    val relativePath = split[1]
                    "/storage/emulated/0/$relativePath"
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Could not convert SAF URI to path: $uriString", e)
            null
        }
    }

    /**
     * Get a unique file name to avoid overwriting existing files.
     */
    private fun getUniqueFile(dir: File, originalName: String): File {
        var file = File(dir, originalName)
        if (!file.exists()) return file
        
        val nameWithoutExt = originalName.substringBeforeLast(".", originalName)
        val ext = if (originalName.contains(".")) ".${originalName.substringAfterLast(".")}" else ""
        
        var counter = 1
        while (file.exists()) {
            file = File(dir, "${nameWithoutExt}_($counter)$ext")
            counter++
        }
        return file
    }

    private fun startDownload(
        downloadId: Long, 
        url: String, 
        fileName: String, 
        mimeType: String,
        cookies: String?,
        userAgent: String?,
        referer: String? = null,
        dirUri: String? = null
    ) {
        Log.d(TAG, "startDownload called: id=$downloadId, fileName='$fileName', url=$url")
        
        if (activeDownloads.containsKey(downloadId)) {
            Log.d(TAG, "Download $downloadId already active")
            return
        }

        val downloadDir = getDownloadDir(dirUri)
        val file = getUniqueFile(downloadDir, fileName)
        Log.d(TAG, "Unique file determined: ${file.name} (original: '$fileName')")
        
        // Update the filename in database to match the actual unique filename
        serviceScope.launch {
            downloadRepository.updateFileName(downloadId, file.name)
        }

        val job = serviceScope.launch {
            try {
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_RUNNING)
                performDownload(downloadId, url, file, mimeType, cookies, userAgent, referer)
            } catch (e: CancellationException) {
                Log.d(TAG, "Download $downloadId cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                updateProgressState(downloadId, 0, -1, DownloadEntity.STATUS_FAILED)
            } finally {
                activeDownloads.remove(downloadId)
                updateNotification()
                if (activeDownloads.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }

        activeDownloads[downloadId] = DownloadJob(job, file, url, mimeType, cookies, userAgent, referer, dirUri)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private suspend fun performDownload(
        downloadId: Long,
        url: String,
        file: File,
        mimeType: String,
        cookies: String?,
        userAgent: String?,
        referer: String? = null
    ) {
        val existingBytes = if (file.exists()) file.length() else 0
        
        val requestBuilder = Request.Builder()
            .url(url)
        
        // Add user agent
        val ua = userAgent ?: "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        requestBuilder.header("User-Agent", ua)
        
        // Add cookies if available
        cookies?.let { requestBuilder.header("Cookie", it) }
        
        // Add Referer header so servers know which page triggered the download
        referer?.takeIf { it.startsWith("http") }?.let {
            requestBuilder.header("Referer", it)
            Log.d(TAG, "Download with Referer: $it")
        }
        
        // Resume support - request partial content
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1
            val totalBytes = if (response.code == 206) {
                // Partial content - total = existing + remaining
                contentLength + existingBytes
            } else {
                contentLength
            }
            
            if (totalBytes > 0) {
                downloadRepository.updateTotalBytes(downloadId, totalBytes)
            }

            val body = response.body ?: throw Exception("Empty response body")
            
            val randomAccessFile = RandomAccessFile(file, "rw")
            if (response.code == 206) {
                randomAccessFile.seek(existingBytes)
            } else {
                randomAccessFile.seek(0)
            }
            
            val buffer = ByteArray(8192)
            var downloadedBytes = if (response.code == 206) existingBytes else 0L
            var lastUpdateTime = System.currentTimeMillis()
            var lastBytes = downloadedBytes

            body.byteStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // Check if paused
                    while (activeDownloads[downloadId]?.isPaused == true) {
                        downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_PAUSED)
                        updateProgressState(downloadId, downloadedBytes, totalBytes, DownloadEntity.STATUS_PAUSED)
                        delay(200)
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
                        updateProgressState(downloadId, downloadedBytes, totalBytes, DownloadEntity.STATUS_RUNNING, speed)
                        updateNotification(file.name, downloadedBytes, totalBytes, activeDownloads.size)
                        
                        lastUpdateTime = currentTime
                        lastBytes = downloadedBytes
                    }
                }
            }
            
            randomAccessFile.close()
            
            // Download complete — register with MediaStore on Android 10+
            registerWithMediaStore(file, mimeType)
            
            // Update database - ensure status is set to SUCCESSFUL
            try {
                downloadRepository.updateProgress(downloadId, downloadedBytes, DownloadEntity.STATUS_SUCCESSFUL)
                downloadRepository.updateFilePath(downloadId, file.absolutePath)
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_SUCCESSFUL)
                updateProgressState(downloadId, downloadedBytes, totalBytes, DownloadEntity.STATUS_SUCCESSFUL)
                Log.d(TAG, "Download $downloadId status updated to SUCCESSFUL")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update download status: ${e.message}")
            }
            
            // Show completion notification
            showCompletionNotification(downloadId, file, mimeType)
            
            Log.d(TAG, "Download $downloadId completed: ${file.absolutePath} (${formatBytes(downloadedBytes)})")
        }
    }

    /**
     * Register the downloaded file with MediaStore so it appears in system file managers.
     * Only needed for Android 10+ (scoped storage).
     */
    private fun registerWithMediaStore(file: File, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER_NAME")
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                // Just insert metadata — the file already exists on disk
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                Log.d(TAG, "Registered ${file.name} with MediaStore")
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore registration failed (file still saved): ${e.message}")
            }
        }
    }

    private fun updateProgressState(
        downloadId: Long, 
        bytesDownloaded: Long, 
        totalBytes: Long, 
        status: Int, 
        speed: String = ""
    ) {
        _downloadProgress.value = _downloadProgress.value + (downloadId to DownloadProgress(
            downloadId = downloadId,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            status = status,
            speed = speed
        ))
    }

    private fun calculateSpeed(bytesDelta: Long, timeDeltaMs: Long): String {
        if (timeDeltaMs <= 0) return ""
        val speedBps = (bytesDelta * 1000) / timeDeltaMs
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
        val job = activeDownloads[downloadId]
        if (job != null) {
            // Resume an in-progress paused download
            job.isPaused = false
            serviceScope.launch {
                downloadRepository.updateStatus(downloadId, DownloadEntity.STATUS_RUNNING)
            }
        } else {
            // Re-start a previously paused download that lost its job (service restarted)
            serviceScope.launch {
                val download = downloadRepository.getDownloadById(downloadId)
                if (download != null) {
                    val intent = Intent(this@DownloadService, DownloadService::class.java).apply {
                        action = ACTION_START
                        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                        putExtra(EXTRA_URL, download.url)
                        putExtra(EXTRA_FILE_NAME, download.fileName)
                        putExtra(EXTRA_MIME_TYPE, download.mimeType)
                    }
                    startService(intent)
                }
            }
        }
        Log.d(TAG, "Download $downloadId resumed")
    }

    fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId]?.let { job ->
            job.job.cancel()
            // Delete the partial file
            if (job.file.exists()) {
                job.file.delete()
            }
        }
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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Progress channel (low importance, no sound)
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
                setSound(null, null)
            }
            
            // Completion channel (default importance, with sound)
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Download Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Download completion notifications"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
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
        if (activeDownloads.isEmpty()) return
        
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

    private fun showCompletionNotification(downloadId: Long, file: File, mimeType: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create intent to open the downloaded file
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", file
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val openPendingIntent = PendingIntent.getActivity(
                this, downloadId.toInt(), openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
                .setContentTitle("Download complete")
                .setContentText(file.name)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .addAction(0, "Open", openPendingIntent)
                .build()

            notificationManager.notify(downloadId.toInt(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show completion notification: ${e.message}")
        }
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
        val cookies: String?,
        val userAgent: String?,
        val referer: String? = null,
        val dirUri: String? = null,
        @Volatile var isPaused: Boolean = false
    )
}
