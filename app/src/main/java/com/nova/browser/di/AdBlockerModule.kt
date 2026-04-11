package com.nova.browser.di

import android.content.Context
import com.nova.browser.util.AdBlocker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Hilt module for providing AdBlocker instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object AdBlockerModule {

    @Provides
    @Singleton
    fun provideAdBlocker(@ApplicationContext context: Context): AdBlocker {
        val adBlocker = AdBlocker.getInstance()
        // Initialize on a background thread
        runBlocking {
            adBlocker.initialize(context)
        }
        return adBlocker
    }
}
