package com.nova.browser.util

import android.util.Log
import android.webkit.WebView
import com.nova.browser.data.local.db.UserScriptEntity
import java.util.regex.Pattern

/**
 * Engine for matching and injecting user scripts into web pages.
 */
class UserScriptEngine {

    companion object {
        private const val TAG = "UserScriptEngine"

        /**
         * Wraps user script code with the GM API context.
         */
        fun wrapScript(script: UserScriptEntity, apiObjectName: String): String {
            val gmApi = buildString {
                appendLine("const GM = {")
                appendLine("  addStyle: function(css) { var s = document.createElement('style'); s.textContent = css; document.head.appendChild(s); },")
                appendLine("  getValue: function(key, def) { return $apiObjectName.gmGetValue(key, def); },")
                appendLine("  setValue: function(key, val) { $apiObjectName.gmSetValue(key, val); },")
                appendLine("  deleteValue: function(key) { $apiObjectName.gmDeleteValue(key); },")
                appendLine("  listValues: function() { return JSON.parse($apiObjectName.gmListValues()); },")
                appendLine("  log: function(msg) { $apiObjectName.gmLog(String(msg)); },")
                appendLine("  openInTab: function(url) { $apiObjectName.gmOpenInTab(url); },")
                appendLine("  notification: function(details) { $apiObjectName.gmNotification(JSON.stringify(details)); },")
                appendLine("  xmlhttpRequest: function(details) { $apiObjectName.gmXhr(JSON.stringify(details)); }")
                appendLine("};")
                appendLine("const GM_addStyle = GM.addStyle;")
                appendLine("const GM_getValue = GM.getValue;")
                appendLine("const GM_setValue = GM.setValue;")
                appendLine("const GM_deleteValue = GM.deleteValue;")
                appendLine("const GM_listValues = GM.listValues;")
                appendLine("const GM_log = GM.log;")
                appendLine("const GM_openInTab = GM.openInTab;")
                appendLine("const GM_notification = GM.notification;")
                appendLine("const GM_xmlhttpRequest = GM.xmlhttpRequest;")
                appendLine("const unsafeWindow = window;")
            }

            return """
                (function() {
                    'use strict';
                    $gmApi
                    ${script.code}
                })();
            """.trimIndent()
        }
    }

    /**
     * Returns all scripts that match the given URL.
     */
    fun getMatchingScripts(scripts: List<UserScriptEntity>, url: String): List<UserScriptEntity> {
        return scripts.filter { script ->
            if (!script.enabled) return@filter false
            matchesUrl(script, url)
        }
    }

    /**
     * Inject matching scripts into the WebView at the given timing.
     */
    fun injectScripts(
        webView: WebView,
        scripts: List<UserScriptEntity>,
        url: String,
        timing: String, // "document-start" or "document-end"
        apiObjectName: String = "NovaGmApi"
    ) {
        val matching = getMatchingScripts(scripts, url)
            .filter { it.runAt == timing }

        if (matching.isEmpty()) return

        Log.d(TAG, "Injecting ${matching.size} scripts at $timing for $url")

        matching.forEach { script ->
            try {
                val wrapped = wrapScript(script, apiObjectName)
                webView.evaluateJavascript(wrapped, null)
                Log.d(TAG, "Injected: ${script.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject ${script.name}: ${e.message}")
            }
        }
    }

    /**
     * Check if a script matches the given URL based on its match/exclude patterns.
     */
    private fun matchesUrl(script: UserScriptEntity, url: String): Boolean {
        // First check exclude patterns
        if (script.excludePatterns.isNotBlank()) {
            val excludes = script.excludePatterns.split(",").map { it.trim() }
            if (excludes.any { pattern -> matchPattern(url, pattern) }) {
                return false
            }
        }

        // Then check match patterns
        if (script.matchPatterns.isNotBlank()) {
            val matches = script.matchPatterns.split(",").map { it.trim() }
            return matches.any { pattern -> matchPattern(url, pattern) }
        }

        return false
    }

    /**
     * Match a URL against a glob-like pattern (Tampermonkey @match style).
     * Pattern format: <scheme>://<host><path>
     * Special chars: * matches any sequence, ? matches single char
     */
    private fun matchPattern(url: String, pattern: String): Boolean {
        val trimmed = pattern.trim()
        if (trimmed.isBlank()) return false

        // Direct match (exact URL)
        if (trimmed == url) return true

        // Convert glob pattern to regex
        val regex = globToRegex(trimmed)
        return regex.matches(url)
    }

    /**
     * Convert a glob pattern to a Regex.
     * Supports: * (any chars), ? (single char), ** (any chars including /)
     */
    private fun globToRegex(pattern: String): Regex {
        val sb = StringBuilder()
        sb.append('^')

        var i = 0
        while (i < pattern.length) {
            when (val c = pattern[i]) {
                '*' -> {
                    if (i + 1 < pattern.length && pattern[i + 1] == '*') {
                        sb.append(".*")
                        i += 2
                        continue
                    } else {
                        sb.append("[^/]*")
                    }
                }
                '?' -> sb.append('.')
                '.' -> sb.append("\\.")
                '+' -> sb.append("\\+")
                '(' -> sb.append("\\(")
                ')' -> sb.append("\\)")
                '[' -> sb.append("\\[")
                ']' -> sb.append("\\]")
                '{' -> sb.append("\\{")
                '}' -> sb.append("\\}")
                '^' -> sb.append("\\^")
                '\$' -> sb.append("\\$")
                '|' -> sb.append("\\|")
                '\\' -> sb.append("\\\\")
                else -> sb.append(c)
            }
            i++
        }

        sb.append('$')
        return Regex(sb.toString())
    }
}
