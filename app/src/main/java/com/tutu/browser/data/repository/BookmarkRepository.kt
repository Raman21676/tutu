package com.tutu.browser.data.repository

import com.tutu.browser.data.local.datastore.SettingsDataStore
import com.tutu.browser.data.local.entity.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class BookmarkRepository(private val dataStore: SettingsDataStore) {
    
    val bookmarks: Flow<List<Bookmark>> = dataStore.settings.map { it.bookmarks }
    
    suspend fun addBookmark(title: String, url: String, color: Long? = null) {
        val currentSettings = dataStore.settings.first()
        val currentList = currentSettings.bookmarks.toMutableList()
        
        val newBookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            color = color,
            order = currentList.size
        )
        
        currentList.add(newBookmark)
        dataStore.saveBookmarks(currentList)
    }
    
    suspend fun removeBookmark(bookmarkId: String) {
        val currentSettings = dataStore.settings.first()
        val updatedList = currentSettings.bookmarks.filter { it.id != bookmarkId }
        dataStore.saveBookmarks(updatedList)
    }
    
    suspend fun updateBookmarks(bookmarks: List<Bookmark>) {
        dataStore.saveBookmarks(bookmarks)
    }
    
    suspend fun reorderBookmarks(reorderedList: List<Bookmark>) {
        dataStore.saveBookmarks(reorderedList.mapIndexed { index, bookmark ->
            bookmark.copy(order = index)
        })
    }
}
