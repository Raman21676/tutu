package com.nova.browser.domain.repository

import com.nova.browser.data.local.db.UserScriptDao
import com.nova.browser.data.local.db.UserScriptEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserScriptRepository @Inject constructor(
    private val userScriptDao: UserScriptDao
) {
    fun getAllScripts(): Flow<List<UserScriptEntity>> = userScriptDao.getAll()
    fun getEnabledScripts(): Flow<List<UserScriptEntity>> = userScriptDao.getAllEnabled()
    suspend fun getById(id: Long): UserScriptEntity? = userScriptDao.getById(id)
    suspend fun insert(script: UserScriptEntity): Long = userScriptDao.insert(script)
    suspend fun update(script: UserScriptEntity) = userScriptDao.update(script)
    suspend fun delete(script: UserScriptEntity) = userScriptDao.delete(script)
    suspend fun deleteById(id: Long) = userScriptDao.deleteById(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = userScriptDao.setEnabled(id, enabled)
}
