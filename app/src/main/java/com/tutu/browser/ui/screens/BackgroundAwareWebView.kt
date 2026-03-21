package com.tutu.browser.ui.screens

import android.content.Context
import android.webkit.WebView

/**
 * A WebView that refuses to pause itself when background play is enabled.
 *
 * Android calls onPause() and onWindowVisibilityChanged() automatically through
 * the view hierarchy when the Activity pauses. Overriding them here is the ONLY
 * way to prevent WebView from freezing its media pipeline.
 */
class BackgroundAwareWebView(context: Context) : WebView(context) {

    var backgroundPlayEnabled = false

    override fun onPause() {
        if (!backgroundPlayEnabled) {
            super.onPause()
        }
        // When backgroundPlayEnabled = true, do absolutely nothing.
        // Do NOT call super.onPause(). This prevents the JS timer freeze
        // and media pipeline pause that kills audio.
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (!backgroundPlayEnabled) {
            super.onWindowVisibilityChanged(visibility)
        }
        // Never tell the WebView the window is invisible.
        // YouTube checks this to decide whether to pause playback.
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!backgroundPlayEnabled) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
        // Block the "focus lost" signal — Chromium uses this as a secondary
        // trigger to suspend audio on older Android versions.
    }
}
