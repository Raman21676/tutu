package com.nova.browser.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["timestamp"])]
)
data class DownloadEntity(
    @PrimaryKey
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long = 0,
    val status: Int = STATUS_PENDING,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_RUNNING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_SUCCESSFUL = 3
        const val STATUS_FAILED = 4
    }

    val progress: Int
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt()
        } else 0
}
