package com.nova.browser.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM user_scripts ORDER BY name ASC")
    fun getAll(): Flow<List<UserScriptEntity>>

    @Query("SELECT * FROM user_scripts WHERE enabled = 1 ORDER BY name ASC")
    fun getAllEnabled(): Flow<List<UserScriptEntity>>

    @Query("SELECT * FROM user_scripts WHERE id = :id")
    suspend fun getById(id: Long): UserScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: UserScriptEntity): Long

    @Update
    suspend fun update(script: UserScriptEntity)

    @Delete
    suspend fun delete(script: UserScriptEntity)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE user_scripts SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
