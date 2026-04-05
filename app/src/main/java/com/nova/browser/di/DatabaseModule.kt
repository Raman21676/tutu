package com.nova.browser.di

import android.content.Context
import com.nova.browser.data.local.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TutuDatabase {
        return TutuDatabase.getDatabase(context)
    }

    @Provides
    fun provideHistoryDao(database: TutuDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideDownloadDao(database: TutuDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideSiteSettingsDao(database: TutuDatabase): SiteSettingsDao {
        return database.siteSettingsDao()
    }
}
