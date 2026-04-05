package com.nova.browser.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntity::class,
        DownloadEntity::class,
        SiteSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TutuDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun siteSettingsDao(): SiteSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: TutuDatabase? = null

        fun getDatabase(context: Context): TutuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TutuDatabase::class.java,
                    "tutu_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
