package com.nova.browser.util

import android.webkit.WebView

/**
 * DarkModeInjector
 *
 * Injects a CSS filter into any webpage to force dark mode.
 * Uses the classic invert + hue-rotate trick so images/videos
 * stay visually correct while text/backgrounds flip to dark.
 *
 * Usage:
 *   DarkModeInjector.inject(webView)   // turn on
 *   DarkModeInjector.remove(webView)   // turn off
 *
 * Call inject() from your WebViewClient.onPageFinished() so it
 * re-applies after every navigation.
 */
object DarkModeInjector {

    private const val STYLE_ID = "tutu-dark-mode"

    // CSS that inverts the whole page, then re-inverts media so
    // images / videos look natural.
    private val DARK_CSS = """
        html {
            filter: invert(1) hue-rotate(180deg) !important;
            background-color: #121212 !important;
        }
        img, video, canvas, picture, svg, iframe,
        [style*="background-image"] {
            filter: invert(1) hue-rotate(180deg) !important;
        }
    """.trimIndent()

    /** Injects dark mode into the given WebView. Safe to call multiple times. */
    fun inject(webView: WebView) {
        val escapedCss = DARK_CSS
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        val script = """
            (function() {
                if (document.getElementById('$STYLE_ID')) return;
                var s = document.createElement('style');
                s.id = '$STYLE_ID';
                s.textContent = `$escapedCss`;
                var target = document.head || document.documentElement;
                if (target) target.appendChild(s);
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    /** Removes dark mode from the given WebView. */
    fun remove(webView: WebView) {
        val script = """
            (function() {
                var s = document.getElementById('$STYLE_ID');
                if (s) s.parentNode.removeChild(s);
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    /**
     * Convenience toggle. Pass the current isDarkMode state;
     * this flips it and returns the NEW state.
     *
     * Example in WebScreen.kt:
     *   isDarkMode = DarkModeInjector.toggle(webView, isDarkMode)
     */
    fun toggle(webView: WebView, currentState: Boolean): Boolean {
        return if (currentState) {
            remove(webView)
            false
        } else {
            inject(webView)
            true
        }
    }
}
