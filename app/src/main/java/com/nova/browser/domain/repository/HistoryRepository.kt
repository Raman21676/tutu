package com.nova.browser.domain.repository

import com.nova.browser.data.local.db.HistoryDao
import com.nova.browser.data.local.db.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        historyDao.searchHistory(query)

    fun getHistorySince(days: Int): Flow<List<HistoryEntity>> {
        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return historyDao.getHistorySince(startTime)
    }

    suspend fun addOrUpdateHistory(url: String, title: String) {
        val existing = historyDao.getHistoryByUrl(url)
        if (existing != null) {
            historyDao.incrementVisitCount(url)
        } else {
            historyDao.insert(
                HistoryEntity(
                    url = url,
                    title = title.ifBlank { url }
                )
            )
        }
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun deleteOlderThan(days: Int) {
        val beforeTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        historyDao.deleteOlderThan(beforeTime)
    }

    suspend fun clearAllHistory() {
        historyDao.deleteAll()
    }

    suspend fun insertHistory(entity: HistoryEntity) {
        historyDao.insert(entity)
    }

    fun getHistoryCount(): Flow<Int> = historyDao.getHistoryCount()
}
