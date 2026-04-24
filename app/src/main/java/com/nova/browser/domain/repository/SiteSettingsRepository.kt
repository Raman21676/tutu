package com.nova.browser.domain.repository

import com.nova.browser.data.local.db.SiteSettingsDao
import com.nova.browser.data.local.db.SiteSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSettingsRepository @Inject constructor(
    private val siteSettingsDao: SiteSettingsDao
) {
    suspend fun getAll(): List<SiteSettingsEntity> = siteSettingsDao.getAll()

    suspend fun getSettingsForDomain(domain: String): SiteSettingsEntity? {
        return siteSettingsDao.getSettingsForDomain(domain)
    }

    fun getSettingsForDomainFlow(domain: String): Flow<SiteSettingsEntity?> {
        return siteSettingsDao.getSettingsForDomainFlow(domain)
    }

    suspend fun saveSettings(settings: SiteSettingsEntity) {
        siteSettingsDao.insert(settings)
    }

    suspend fun updateJavaScriptEnabled(domain: String, enabled: Boolean) {
        val existing = siteSettingsDao.getSettingsForDomain(domain)
        val settings = existing?.copy(javaScriptEnabled = enabled)
            ?: SiteSettingsEntity(domain = domain, javaScriptEnabled = enabled)
        siteSettingsDao.insert(settings)
    }

    suspend fun updateAdBlockEnabled(domain: String, enabled: Boolean) {
        val existing = siteSettingsDao.getSettingsForDomain(domain)
        val settings = existing?.copy(adBlockEnabled = enabled)
            ?: SiteSettingsEntity(domain = domain, adBlockEnabled = enabled)
        siteSettingsDao.insert(settings)
    }

    suspend fun updateDesktopMode(domain: String, enabled: Boolean) {
        val existing = siteSettingsDao.getSettingsForDomain(domain)
        val settings = existing?.copy(desktopMode = enabled)
            ?: SiteSettingsEntity(domain = domain, desktopMode = enabled)
        siteSettingsDao.insert(settings)
    }

    suspend fun updateThirdPartyCookies(domain: String, enabled: Boolean) {
        val existing = siteSettingsDao.getSettingsForDomain(domain)
        val settings = existing?.copy(thirdPartyCookiesEnabled = enabled)
            ?: SiteSettingsEntity(domain = domain, thirdPartyCookiesEnabled = enabled)
        siteSettingsDao.insert(settings)
    }

    suspend fun deleteSettings(domain: String) {
        siteSettingsDao.deleteForDomain(domain)
    }

    suspend fun clearAllSettings() {
        siteSettingsDao.deleteAll()
    }
}
