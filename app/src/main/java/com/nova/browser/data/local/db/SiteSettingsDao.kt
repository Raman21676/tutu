package com.nova.browser.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteSettingsDao {
    @Query("SELECT * FROM site_settings")
    suspend fun getAll(): List<SiteSettingsEntity>

    @Query("SELECT * FROM site_settings WHERE domain = :domain LIMIT 1")
    suspend fun getSettingsForDomain(domain: String): SiteSettingsEntity?

    @Query("SELECT * FROM site_settings WHERE domain = :domain LIMIT 1")
    fun getSettingsForDomainFlow(domain: String): Flow<SiteSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SiteSettingsEntity)

    @Update
    suspend fun update(settings: SiteSettingsEntity)

    @Delete
    suspend fun delete(settings: SiteSettingsEntity)

    @Query("DELETE FROM site_settings WHERE domain = :domain")
    suspend fun deleteForDomain(domain: String)

    @Query("DELETE FROM site_settings")
    suspend fun deleteAll()
}
