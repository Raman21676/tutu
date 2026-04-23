package com.nova.browser.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nova.browser.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Centralized helper for the browser's download directory.
 *
 * Inspired by RustPDF's OutputDirHelper.
 *
 * Default location (all Android versions):
 *   /storage/emulated/0/Download/Nova Downloads
 *
 * The user may override this via Settings → Downloads (SAF folder picker).
 * When a custom SAF URI is set we try to convert it back to a real File path
 * so that the folder can be opened in a file manager later.
 */
object DownloadDirHelper {

    const val DEFAULT_FOLDER_NAME = "Nova Downloads"
    private const val TAG = "DownloadDirHelper"

    /**
     * Return the absolute File path of the current download directory.
     * Creates the directory if it does not yet exist.
     */
    fun getDownloadDir(context: Context): File {
        val customUri = runBlocking {
            try {
                SettingsDataStore(context).settings.first().downloadDirUri
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read downloadDirUri setting", e)
                ""
            }
        }

        if (customUri.isNotEmpty()) {
            val path = convertSafUriToPath(customUri)
            if (path != null) {
                val dir = File(path)
                if (dir.exists() || dir.mkdirs()) {
                    Log.d(TAG, "Using custom download dir: ${dir.absolutePath}")
                    return dir
                }
            }
        }

        // Default fallback: public Downloads / Nova Downloads
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val novaDir = File(publicDownloads, DEFAULT_FOLDER_NAME)
        if (!novaDir.exists()) {
            novaDir.mkdirs()
        }
        return novaDir
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

    /**
     * Reset the custom directory back to default.
     */
    suspend fun resetToDefault(context: Context) {
        SettingsDataStore(context).updateDownloadDirectory("")
    }
}
