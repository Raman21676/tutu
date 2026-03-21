package com.tutu.browser.di

import android.content.Context
import com.tutu.browser.data.local.datastore.SettingsDataStore
import com.tutu.browser.data.repository.BookmarkRepository
import com.tutu.browser.data.repository.SettingsRepository

/**
 * Manual Dependency Injection container.
 * Keeping it simple to reduce APK size and avoid Hilt overhead.
 */
object AppModule {
    
    @Volatile
    private var settingsDataStore: SettingsDataStore? = null
    
    @Volatile
    private var settingsRepository: SettingsRepository? = null
    
    @Volatile
    private var bookmarkRepository: BookmarkRepository? = null
    
    fun provideSettingsDataStore(context: Context): SettingsDataStore {
        return settingsDataStore ?: synchronized(this) {
            settingsDataStore ?: SettingsDataStore(context.applicationContext).also {
                settingsDataStore = it
            }
        }
    }
    
    fun provideSettingsRepository(context: Context): SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(
                provideSettingsDataStore(context)
            ).also {
                settingsRepository = it
            }
        }
    }
    
    fun provideBookmarkRepository(context: Context): BookmarkRepository {
        return bookmarkRepository ?: synchronized(this) {
            bookmarkRepository ?: BookmarkRepository(
                provideSettingsDataStore(context)
            ).also {
                bookmarkRepository = it
            }
        }
    }
    
    // Clear all singletons (useful for testing)
    fun clear() {
        synchronized(this) {
            settingsDataStore = null
            settingsRepository = null
            bookmarkRepository = null
        }
    }
}
