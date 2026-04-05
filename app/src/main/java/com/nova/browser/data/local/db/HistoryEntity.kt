package com.nova.browser.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    indices = [Index(value = ["url"]), Index(value = ["timestamp"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
    val favicon: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryEntity

        if (id != other.id) return false
        if (url != other.url) return false
        if (title != other.title) return false
        if (timestamp != other.timestamp) return false
        if (visitCount != other.visitCount) return false
        if (favicon != null) {
            if (other.favicon == null) return false
            if (!favicon.contentEquals(other.favicon)) return false
        } else if (other.favicon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + visitCount
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        return result
    }
}
