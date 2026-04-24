package com.nova.browser.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * JavaScript bridge providing the Greasemonkey/Tampermonkey API to user scripts.
 */
class UserScriptGmApi(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nova_gm_values", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "UserScriptGmApi"
    }

    @JavascriptInterface
    fun gmAddStyle(css: String) {
        Log.d(TAG, "GM_addStyle: ${css.take(50)}...")
        // CSS is injected by the wrapping code via JS
    }

    @JavascriptInterface
    fun gmGetValue(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    @JavascriptInterface
    fun gmSetValue(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    @JavascriptInterface
    fun gmDeleteValue(key: String) {
        prefs.edit { remove(key) }
    }

    @JavascriptInterface
    fun gmListValues(): String {
        val keys = prefs.all.keys.toList()
        return JSONArray(keys).toString()
    }

    @JavascriptInterface
    fun gmLog(message: String) {
        Log.d(TAG, "GM_log: $message")
    }

    @JavascriptInterface
    fun gmOpenInTab(url: String) {
        Log.d(TAG, "GM_openInTab: $url")
        // Handled by the wrapping code
    }

    @JavascriptInterface
    fun gmNotification(detailsJson: String) {
        Log.d(TAG, "GM_notification: $detailsJson")
    }

    @JavascriptInterface
    fun gmXhr(detailsJson: String) {
        Log.d(TAG, "GM_xmlhttpRequest: $detailsJson")
        // Full XHR bridge would require async OkHttp calls
        // For now, log the request details
        try {
            val details = JSONObject(detailsJson)
            val url = details.optString("url")
            val method = details.optString("method", "GET")
            Log.d(TAG, "XHR $method $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse XHR details", e)
        }
    }
}
