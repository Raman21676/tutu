package com.nova.browser.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

/**
 * The three download location modes available in Settings.
 */
enum class DownloadLocationType {
    PHONE_STORAGE,   // Root of internal storage → /sdcard/Nova Downloads
    SD_CARD,         // Root of SD card → /storage/XXXX-XXXX/Nova Downloads
    CUSTOM           // User-specified base path + user-specified folder name
}

/**
 * Preference helpers for download directory settings.
 * Uses SharedPreferences for synchronous reads.
 */
object DownloadDirPreference {
    private const val PREFS_NAME = "nova_output_prefs"
    private const val KEY_LOCATION_TYPE = "download_location_type"
    private const val KEY_FOLDER_NAME = "download_folder_name"
    private const val KEY_CUSTOM_PATH = "download_custom_path"
    private const val KEY_CUSTOM_FOLDER_NAME = "download_custom_folder_name"

    fun getLocationType(context: Context): DownloadLocationType {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCATION_TYPE, null)
        return when (saved) {
            DownloadLocationType.SD_CARD.name -> DownloadLocationType.SD_CARD
            DownloadLocationType.CUSTOM.name -> DownloadLocationType.CUSTOM
            else -> DownloadLocationType.PHONE_STORAGE
        }
    }

    fun saveLocationType(context: Context, type: DownloadLocationType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCATION_TYPE, type.name).apply()
    }

    fun getFolderName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_NAME, DownloadDirHelper.DEFAULT_FOLDER_NAME)
            ?: DownloadDirHelper.DEFAULT_FOLDER_NAME
    }

    fun saveFolderName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_FOLDER_NAME, name).apply()
    }

    fun getCustomPath(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_PATH, null)
    }

    fun saveCustomPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_PATH, path).apply()
    }

    fun clearCustomPath(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CUSTOM_PATH).apply()
    }

    fun getCustomFolderName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_FOLDER_NAME, DownloadDirHelper.DEFAULT_FOLDER_NAME)
            ?: DownloadDirHelper.DEFAULT_FOLDER_NAME
    }

    fun saveCustomFolderName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_FOLDER_NAME, name).apply()
    }
}

/**
 * Helper to get the correct app download directory based on user's Settings choice.
 *
 * - PHONE_STORAGE : Environment.getExternalStorageDirectory() + folder name
 *   → /storage/emulated/0/Nova Downloads
 *
 * - SD_CARD : removable storage volume root + folder name
 *   → /storage/XXXX-XXXX/Nova Downloads
 *
 * - CUSTOM : user's chosen base directory + user's chosen folder name.
 */
object DownloadDirHelper {

    const val DEFAULT_FOLDER_NAME = "Nova Downloads"
    private const val TAG = "DownloadDirHelper"
    private const val OLD_PREFS_NAME = "nova_download_prefs"
    private const val KEY_DIR_PATH = "download_dir_path"
    private const val KEY_CUSTOM_URI = "download_dir_uri"

    fun getDownloadDir(context: Context): File {
        // 1. Try new 3-option system
        val type = DownloadDirPreference.getLocationType(context)
        val dir = when (type) {
            DownloadLocationType.PHONE_STORAGE -> phoneStorageDir(context)
            DownloadLocationType.SD_CARD -> sdCardDir(context)
            DownloadLocationType.CUSTOM -> customDir(context)
        }
        if (dir.exists() || dir.mkdirs()) {
            Log.d(TAG, "Using download dir: ${dir.absolutePath}")
            return dir
        }

        // 2. Try old system (legacy path from SharedPreferences)
        val oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        val savedPath = oldPrefs.getString(KEY_DIR_PATH, null)
        if (!savedPath.isNullOrEmpty()) {
            val oldDir = File(savedPath)
            if (oldDir.exists() || oldDir.mkdirs()) {
                Log.d(TAG, "Using legacy download dir: ${oldDir.absolutePath}")
                return oldDir
            }
        }
        val legacyUri = oldPrefs.getString(KEY_CUSTOM_URI, null)
        if (!legacyUri.isNullOrEmpty()) {
            val path = convertSafUriToPath(legacyUri)
            if (path != null) {
                val oldDir = File(path)
                if (oldDir.exists() || oldDir.mkdirs()) {
                    Log.d(TAG, "Using legacy SAF dir: ${oldDir.absolutePath}")
                    return oldDir
                }
            }
        }

        // 3. Default fallback: public Downloads / Nova Downloads
        val defaultDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DEFAULT_FOLDER_NAME
        )
        defaultDir.mkdirs()
        return defaultDir
    }

    fun getDisplayPath(context: Context): String = getDownloadDir(context).absolutePath

    // ── Phone storage ──────────────────────────────────────────────────────────

    private fun phoneStorageDir(context: Context): File {
        val folderName = DownloadDirPreference.getFolderName(context).ifBlank { DEFAULT_FOLDER_NAME }
        val root = Environment.getExternalStorageDirectory() // /storage/emulated/0
        return File(root, folderName).also { if (!it.exists()) it.mkdirs() }
    }

    // ── SD card ────────────────────────────────────────────────────────────────

    private fun sdCardDir(context: Context): File {
        val folderName = DownloadDirPreference.getFolderName(context).ifBlank { DEFAULT_FOLDER_NAME }
        val sdPath = getSdCardPath(context)
        return if (sdPath != null) {
            File(sdPath, folderName).also { if (!it.exists()) it.mkdirs() }
        } else {
            // No SD card — fall back to phone storage
            phoneStorageDir(context)
        }
    }

    /**
     * Returns the absolute path of the first mounted removable storage volume,
     * or null if no SD card is present.
     */
    fun getSdCardPath(context: Context): String? {
        return try {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sm.storageVolumes
                    .firstOrNull { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
                    ?.let { vol ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            vol.directory?.absolutePath
                        } else {
                            @Suppress("DiscouragedPrivateApi")
                            vol.javaClass.getMethod("getPath").invoke(vol) as? String
                        }
                    }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasSdCard(context: Context): Boolean = getSdCardPath(context) != null

    // ── Custom ─────────────────────────────────────────────────────────────────

    private fun customDir(context: Context): File {
        val basePath = DownloadDirPreference.getCustomPath(context)
        val folderName = DownloadDirPreference.getCustomFolderName(context)
            .ifBlank { DEFAULT_FOLDER_NAME }
        return if (!basePath.isNullOrBlank()) {
            File(basePath, folderName).also { if (!it.exists()) it.mkdirs() }
        } else {
            // No path set yet — fall back to phone storage
            phoneStorageDir(context)
        }
    }

    /**
     * Fallback directory used when the configured output dir cannot be created.
     * Uses app-private external storage → always writable, no special permission needed.
     */
    private fun fallbackDir(context: Context): File {
        val folderName = DownloadDirPreference.getFolderName(context).ifBlank { DEFAULT_FOLDER_NAME }
        return (context.getExternalFilesDir(null)?.let { File(it, folderName) }
            ?: File(context.filesDir, folderName))
            .also { if (!it.exists()) it.mkdirs() }
    }

    // ── Legacy helpers ─────────────────────────────────────────────────────────

    fun setDownloadDir(context: Context, path: String) {
        context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DIR_PATH, path).apply()
    }

    fun resetToDefault(context: Context) {
        // Clear new prefs
        context.getSharedPreferences("nova_output_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        // Clear old prefs
        context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_DIR_PATH).remove(KEY_CUSTOM_URI).apply()
    }

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

    fun pathToDocumentsUiUri(path: String): Uri? {
        val relative = when {
            path.startsWith("/storage/emulated/0/") -> path.removePrefix("/storage/emulated/0/")
            path.startsWith("/sdcard/") -> path.removePrefix("/sdcard/")
            else -> return null
        }
        return Uri.parse("content://com.android.externalstorage.documents/document/primary:${Uri.encode(relative)}")
    }
}
