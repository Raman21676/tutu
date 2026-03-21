package com.tutu.browser

import android.app.Application
import android.content.ComponentCallbacks2
import android.webkit.WebView
import com.tutu.browser.di.AppModule

class TutuApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Clear WebView caches on low memory
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                WebView(this).clearCache(true)
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                WebView(this).clearCache(false)
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear all caches
        WebView(this).clearCache(false)
    }
}
