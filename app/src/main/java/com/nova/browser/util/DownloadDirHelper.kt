package com.nova.browser.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

/**
 * Centralized helper for the browser's download directory.
 *
 * Inspired by RustPDF's OutputDirHelper.
 *
 * Uses SharedPreferences for synchronous reads (avoiding runBlocking on the
 * main thread).  Default location:
 *   /storage/emulated/0/Download/Nova Downloads
 *
 * The user may override this via Settings → Downloads.
 */
object DownloadDirHelper {

    const val DEFAULT_FOLDER_NAME = "Nova Downloads"
    private const val TAG = "DownloadDirHelper"
    private const val PREFS_NAME = "nova_download_prefs"
    private const val KEY_DIR_PATH = "download_dir_path"
    private const val KEY_CUSTOM_URI = "download_dir_uri" // legacy SAF URI fallback

    /**
     * Return the absolute File path of the current download directory.
     * Creates the directory if it does not yet exist.
     *
     * Reads from SharedPreferences synchronously — safe to call on the main thread.
     */
    fun getDownloadDir(context: Context): File {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Try the explicit saved path first (new system)
        val savedPath = prefs.getString(KEY_DIR_PATH, null)
        if (!savedPath.isNullOrEmpty()) {
            val dir = File(savedPath)
            if (dir.exists() || dir.mkdirs()) {
                Log.d(TAG, "Using custom download dir: ${dir.absolutePath}")
                return dir
            }
        }

        // 2. Try legacy SAF URI (old system, converts to path)
        val legacyUri = prefs.getString(KEY_CUSTOM_URI, null)
        if (!legacyUri.isNullOrEmpty()) {
            val path = convertSafUriToPath(legacyUri)
            if (path != null) {
                val dir = File(path)
                if (dir.exists() || dir.mkdirs()) {
                    Log.d(TAG, "Using legacy SAF dir: ${dir.absolutePath}")
                    return dir
                }
            }
        }

        // 3. Default fallback: public Downloads / Nova Downloads
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val novaDir = File(publicDownloads, DEFAULT_FOLDER_NAME)
        if (!novaDir.exists()) {
            novaDir.mkdirs()
        }
        return novaDir
    }

    /**
     * Save the custom directory path to SharedPreferences.
     */
    fun setDownloadDir(context: Context, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DIR_PATH, path)
            .apply()
    }

    /**
     * Reset to default (clear custom path and legacy URI).
     */
    fun resetToDefault(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DIR_PATH)
            .remove(KEY_CUSTOM_URI)
            .apply()
    }

    /**
     * Convert a SAF tree URI (e.g. content://com.android.externalstorage.documents/tree/primary%3ADownload%2FMyFolder)
     * to an absolute File path for primary (internal) storage.
     *
     * Returns null for external SD cards or unknown authorities.
     */
    fun convertSafUriToPath(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.authority) {
                "com.android.externalstorage.documents" -> {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val split = docId.split(":")
                    if (split.size >= 2 && split[0] == "primary") {
                        val relativePath = split[1]
                        "/storage/emulated/0/$relativePath"
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not convert SAF URI to path: $uriString", e)
            null
        }
    }

    /**
     * Convert an absolute file path to a DocumentsUI content URI that can be used
     * with Intent.ACTION_VIEW to open the folder in the system file manager.
     *
     * Example:
     *   /storage/emulated/0/Download/Nova Downloads
     *   → content://com.android.externalstorage.documents/document/primary:Download/Nova%20Downloads
     */
    fun pathToDocumentsUiUri(path: String): Uri? {
        val relative = when {
            path.startsWith("/storage/emulated/0/") -> path.removePrefix("/storage/emulated/0/")
            path.startsWith("/sdcard/") -> path.removePrefix("/sdcard/")
            else -> return null
        }
        return Uri.parse("content://com.android.externalstorage.documents/document/primary:${Uri.encode(relative)}")
    }

    /**
     * Get a human-readable display path for UI labels.
     */
    fun getDisplayPath(context: Context): String {
        return getDownloadDir(context).absolutePath
    }
}
