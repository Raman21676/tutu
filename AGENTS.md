# NOVA Browser — AI Agent Development Log

> **Purpose of this file**: This is the canonical log for all AI-assisted development sessions.
> Every future AI agent should READ THIS FILE FIRST before starting any work.
> It documents every problem encountered, every solution attempted, and the current build status.

---

## Project Overview

| Field | Value |
|---|---|
| **App Name** | NOVA Browser |
| **Package** | `com.nova.browser` |
| **Debug Package** | `com.nova.browser.debug` |
| **Architecture** | MVVM + Hilt + Room + OkHttp + Jetpack Compose |
| **Min SDK** | 24 |
| **Target SDK** | 34 |
| **Language** | Kotlin |
| **Repository** | `git@github.com:Raman21676/nova.git` |
| **Local Path** | `/Users/kalikali/Desktop/nova` |

### Key People
- **Owner / Tester**: Raman Subedi
- **Test Portfolio Site**: https://ramansubedi.com (used for download testing — has PDF button)
- **Test Device**: OPPO (ADB ID: `1089089`)

---

## Build & Deploy Commands

```bash
# Java path required!
export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home

# Build debug APK
cd /Users/kalikali/Desktop/nova
./gradlew :app:assembleDebug -x lint --quiet

# ADB path
ADB=/Users/kalikali/Library/Android/sdk/platform-tools/adb

# Install
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app (clean start)
$ADB shell am start -n com.nova.browser.debug/com.nova.browser.MainActivity

# Launch to specific URL (for testing)
$ADB shell am start -a android.intent.action.VIEW -d "https://ramansubedi.com" \
  -n com.nova.browser.debug/com.nova.browser.MainActivity

# Tap download button on ramansubedi.com (720x1280 screen coords)
$ADB shell input tap 605 220

# Read app-specific logs
$ADB logcat -d | grep -iE "WebScreen|DownloadService|DownloadRepo|AdBlocker"

# Check downloaded files
$ADB shell "ls -la '/sdcard/Download/Nova Downloads/'"
```

---

## Architecture Reference

```
app/src/main/java/com/nova/browser/
├── MainActivity.kt              ← Navigation host, all screen routes, tab navigation
├── NovaApplication.kt           ← App init: AdBlocker, notification channels
├── di/
│   ├── AppModule.kt             ← Manual DI (SettingsRepo, BookmarkRepo)
│   ├── DataStoreModule.kt       ← Hilt: provides SettingsDataStore singleton
│   ├── DatabaseModule.kt        ← Hilt: provides Room DB, DAOs
│   ├── NetworkModule.kt         ← Hilt: provides OkHttpClient
│   └── AdBlockerModule.kt       ← Hilt: provides AdBlocker singleton
├── data/
│   ├── local/
│   │   ├── datastore/
│   │   │   └── SettingsDataStore.kt   ← ALL app preferences (DataStore)
│   │   └── db/
│   │       ├── AppDatabase.kt
│   │       ├── DownloadDao.kt
│   │       └── DownloadEntity.kt      ← STATUS_PENDING/RUNNING/PAUSED/SUCCESSFUL/FAILED
│   ├── model/
│   │   ├── Tab.kt
│   │   ├── SearchEngine.kt
│   │   └── Bookmark.kt
│   └── repository/
│       ├── SettingsRepository.kt
│       ├── BookmarkRepository.kt
│       └── TabManager.kt              ← Singleton, manages open tabs
├── domain/repository/
│   ├── DownloadRepository.kt    ← Hilt @Singleton, starts downloads, injects SettingsDataStore
│   └── HistoryRepository.kt
├── service/
│   └── DownloadService.kt       ← Foreground service, OkHttp download, pause/resume
├── ui/
│   ├── screens/
│   │   ├── WebScreen.kt         ← Main browser screen (WebView, toolbar, download handling)
│   │   ├── HomeScreen.kt        ← New tab / home page
│   │   ├── SettingsScreen.kt    ← All settings (privacy, downloads, about)
│   │   ├── DownloadsScreen.kt   ← Download list UI
│   │   ├── TabsScreen.kt        ← Tab switcher UI
│   │   ├── BookmarksScreen.kt
│   │   └── HistoryScreen.kt
│   └── viewmodel/
│       ├── WebViewModel.kt
│       ├── SettingsViewModel.kt
│       ├── HomeViewModel.kt
│       └── ThemeViewModel.kt
├── util/
│   ├── AdBlocker.kt             ← Loads blocklist from assets, shouldInterceptRequest hook
│   └── Extensions.kt
└── worker/
    └── DownloadStatusWorker.kt  ← WorkManager worker (currently has Hilt injection issues)
```

---

## Session Log

---

### Session: April 21, 2026

**Agent Used**: Antigravity (Google DeepMind)

#### Problems Encountered & Solutions

---

#### PROBLEM 1: Downloads downloading HTML instead of actual file ✅ FIXED

**Symptom**: Tapping "Brief Resume" button on ramansubedi.com downloaded an HTML page instead of the PDF.

**Root Cause (2 bugs found)**:
1. `shouldOverrideUrlLoading` in `WebScreen.kt` had overly broad check:
   ```kotlin
   // BAD — catches any URL with "/download" in path (e.g. /download-page, /product/download)
   if (downloadExtensions.any { lowerUrl.endsWith(it) || lowerUrl.contains("/download") || lowerUrl.contains("download/") })
   ```
   This intercepted the download URL BEFORE the server responds, with no Referer header, so servers returned HTML error/login pages.

2. No `Referer` header was sent — many servers (including nginx/Apache static file servers) require `Referer` to serve files directly rather than redirecting to HTML.

**Fix Applied**:
- `WebScreen.kt`: Removed `lowerUrl.contains("/download")` — only intercept by explicit extension (`.pdf`, `.zip`, etc.)
- `WebScreen.kt`: Pass `view.url` (current page URL) as `referer` in both `setDownloadListener` and `shouldOverrideUrlLoading`
- `WebScreen.kt`: Pass `cookieManager.getCookie(url)` in `shouldOverrideUrlLoading` interceptor
- `WebScreen.kt`: Added `referer: String? = null` field to `PendingDownload` data class
- `DownloadRepository.kt`: Added `referer: String? = null` param to `startDownload()`; reads `downloadDirUri` from `settingsDataStore.settings.first()`
- `DownloadService.kt`: Added `EXTRA_REFERER` and `EXTRA_DIR_URI` constants; reads them in `onStartCommand`; adds `Referer` header to OkHttp `Request.Builder`

**Verification**: `%PDF` magic bytes confirmed. File size 214.22 KB (correct). Log shows:
```
D DownloadService: Download with Referer: https://www.ramansubedi.com/
D DownloadService: Download 1776749847313 status updated to SUCCESSFUL
D DownloadService: Download 1776749847313 completed: /storage/emulated/0/Download/Nova Downloads/Subedi-Raman-Brief-Resume_(2).pdf (214.22 KB)
```

---

#### PROBLEM 2: Duplicate downloads (2 files downloaded per tap) ✅ FIXED

**Symptom**: Each download tap created 2 identical files (e.g., `Resume.pdf` and `Resume_(1).pdf`).

**Root Cause**: Two download triggers both fired:
1. JS click interceptor in `injectDownloadInterceptor` → `onDownloadClick` → download #1
2. `setDownloadListener` (native WebView) → download #2

**Fix Applied**:
- `WebScreen.kt` → `injectDownloadInterceptor()`: Removed the `document.addEventListener('click', ...)` handler entirely. `setDownloadListener` correctly handles all `<a href="...pdf" download>` links now that Referer+cookies are in place.
- Kept only **blob URL** and **fetch() response** monitoring for dynamically-generated downloads that can't be caught natively.

---

#### PROBLEM 3: Ad blocking not working ✅ FIXED

**Symptom**: Ad blocking toggle in Settings had no visible effect.

**Root Cause (2 bugs)**:
1. `adblock_domains.txt` only had 244 entries — far too few to block anything meaningful.
2. `AdBlocker.extractDomain()` did not strip port numbers — `ads.example.com:443` would fail the domain lookup.

**Fix Applied**:
- `adblock_domains.txt`: Replaced with **87,770 domains** from Steven Black's unified hosts file (combines AdAway + MVPS + Yoyo.org + malware domains). Downloaded from: https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
- `AdBlocker.kt` → `extractDomain()`: Added `.substringBefore(":")` to strip port numbers before matching.

**Verification**: Logcat shows `Loaded 87770 blocked domains in 687ms` on app launch.

---

#### PROBLEM 4: No download directory configuration ✅ FIXED

**Symptom**: Downloads always went to system Downloads folder with no way for user to change it.

**Fix Applied**:
- `SettingsDataStore.kt`: Added `DOWNLOAD_DIR_URI` preference key + `downloadDirUri: String = ""` to `TutuSettings` + `updateDownloadDirectory()` method
- `SettingsRepository.kt`: Added `setDownloadDirectory(uri: String)`
- `SettingsViewModel.kt`: Added `setDownloadDirectory(uri: String)` (launches coroutine)
- `SettingsScreen.kt`: Added **"Downloads"** section between Privacy and About sections. Uses `ActivityResultContracts.OpenDocumentTree()` folder picker. Shows folder name (or "Nova Downloads (Default)"). Has reset (X) button.
- `DownloadRepository.kt`: Injected `SettingsDataStore` (Hilt singleton). Reads `downloadDirUri` from settings via `settings.first()` before starting each download. Passes as `EXTRA_DIR_URI` intent extra.
- `DownloadService.kt`: `getDownloadDir(dirUri: String?)` converts SAF tree URI to File path for primary storage (`/storage/emulated/0/...`). Falls back to `Nova Downloads/` on failure or if not set. Added `convertSAFUriToPath()` helper.

**Default folder**: `/sdcard/Download/Nova Downloads/` (created automatically)

---

#### PROBLEM 5: New tab showed previous page (ramansubedi.com) ✅ FIXED

**Symptom**: Opening a new tab from the tab switcher (+ button) or bottom nav showed the previously-visited website instead of a fresh home screen.

**Root Cause**:
- `TabsScreen.kt` FAB and `TabCard` both called `onNavigateToWeb("")` with empty string for new tabs
- `MainActivity.kt` handler for Tabs route:
  ```kotlin
  onNavigateToWeb = { url ->
      navController.popBackStack()  // ← This is the bug!
      if (url.isNotEmpty()) { ... }
  }
  ```
  `popBackStack()` fired unconditionally → went back to the existing WebScreen (with old page loaded). Since URL was empty, no new navigation happened. User sees old page.

**Fix Applied** (`MainActivity.kt`):
```kotlin
onNavigateToWeb = { url ->
    if (url.isNotEmpty()) {
        // Existing tab — pop Tabs and go to that page
        navController.popBackStack()
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        navController.navigate(Screen.Web.createRoute(encodedUrl))
    } else {
        // New/empty tab — go to Home so user can type a URL fresh
        navController.popBackStack(Screen.Home.route, inclusive = false)
    }
}
```

---

#### PROBLEM 6: DownloadStatusWorker crashes silently ⚠️ PARTIALLY FIXED

**Symptom**: `E WM-WorkerFactory: Could not instantiate com.nova.browser.worker.DownloadStatusWorker`
This error appears in logcat every time a download starts.

**Root Cause**: Hilt cannot inject into the WorkManager worker due to missing `@HiltWorker` setup or missing `HiltWorkerFactory` in Application class.

**Current Status**: Downloads still complete correctly because `DownloadService` updates status directly. The Worker is only a safety-net fallback.

**Fix Applied**: Wrapped `enqueueStatusWorker()` in try-catch so the error is logged as a warning instead of silently corrupting state.

**Remaining Work**: Either fix Hilt injection for the Worker properly (add `@HiltWorker` + `@AssistedInject` + register `HiltWorkerFactory`) or remove the Worker entirely since `DownloadService` already handles all status updates correctly.

---

## Current Build Status (as of April 21, 2026)

| Feature | Status |
|---|---|
| Basic browsing | ✅ Working |
| Download (real file) | ✅ Fixed |
| Download folder config | ✅ Fixed (Nova Downloads default) |
| Ad blocking | ✅ Fixed (87,770 domains) |
| New tab navigation | ✅ Fixed |
| Download status in DB | ✅ Working (DownloadService updates directly) |
| Download status UI | ✅ Showing in Downloads screen |
| Pause/Resume download | ✅ Working (HTTP Range headers via OkHttp) |
| Bookmarks | ✅ Working |
| History | ✅ Working |
| Incognito mode | ✅ Working |
| Tab management | ✅ Working |
| Search engine selection | ✅ Working (Google, Bing, DuckDuckGo, etc.) |
| Dark/Light theme | ✅ Working |
| JS-triggered downloads (blob:) | ✅ Intercepted via `injectDownloadInterceptor` |
| DownloadStatusWorker | ⚠️ Hilt injection fails — non-fatal, downloads complete OK |
| Download progress bar | ❌ Not yet implemented |
| Open downloaded files | ❌ Not yet implemented (needs FileProvider) |
| Play Store publish | ❌ Not done yet |

---

## Known Issues & Next Steps for Next Agent

### Priority 1: Open Downloaded Files
- Tap a completed download in the Downloads screen → open the file
- Requires `FileProvider` (already declared in `AndroidManifest.xml` and `file_paths.xml`)
- Use `Intent.ACTION_VIEW` with `FileProvider.getUriForFile()`
- Add `openFile(download: DownloadEntity)` to `DownloadsScreen.kt`

### Priority 2: Download Progress Bar
- `DownloadService` already calls `downloadRepository.updateProgress(id, bytes, status)` during download
- `DownloadsScreen.kt` reads from Room DB via Flow — should auto-update
- Need to add a `LinearProgressIndicator` in the download list item based on `downloadedBytes / totalBytes`

### Priority 3: Fix DownloadStatusWorker
Options:
1. Add `@HiltWorker` to `DownloadStatusWorker` and use `@AssistedInject` constructor
2. Register `HiltWorkerFactory` in `NovaApplication.kt`: `WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())`
3. Or simply remove the Worker — `DownloadService` already handles status updates reliably

### Priority 4: Play Store Preparation
- Check `nova-final/` folder on Desktop for Play Store strategy docs
- Need signed release AAB (keystore already exists: `nova-browser.keystore`)
- Package: `com.nova.browser`

---

## Important Technical Notes for Next Agent

1. **`adb` is NOT in PATH.** Always use full path: `/Users/kalikali/Library/Android/sdk/platform-tools/adb`
2. **Java must be set.** Always `export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home` before Gradle commands
3. **Two DI systems coexist**: Hilt (`@Inject`, `@AndroidEntryPoint`) AND manual DI (`AppModule.kt`). `SettingsRepository` and `BookmarkRepository` use manual DI; `DownloadRepository` and `HistoryRepository` use Hilt.
4. **`TabManager` is a manual singleton** (not Hilt) — use `TabManager.getInstance()`
5. **Downloads folder** is `/sdcard/Download/Nova Downloads/` by default. User can override in Settings → Downloads → Download Folder.
6. **Ad block list** is at `app/src/main/assets/adblock_domains.txt` — 87,770 domains, one per line, no prefix
7. **`last_url` DataStore key** — app saves last visited URL and restores it on next launch. This is by design.
8. **The test site** `ramansubedi.com` is Raman's (the owner's) portfolio. It was used for download testing only. The app does NOT have it hardcoded anywhere.
9. **`DownloadService`** is a foreground service — it updates download status directly in the DB. The `DownloadStatusWorker` is only a fallback.
10. **Referer header** — all downloads now send the page URL as `Referer`. This fixed the "HTML instead of file" bug.

---

## Files Modified (April 21, 2026 Session)

| File | What Changed |
|---|---|
| `app/src/main/assets/adblock_domains.txt` | 244 → 87,770 domains (Steven Black unified) |
| `util/AdBlocker.kt` | Fixed port stripping in `extractDomain()` |
| `data/local/datastore/SettingsDataStore.kt` | Added `DOWNLOAD_DIR_URI` key + `downloadDirUri` field + `updateDownloadDirectory()` |
| `data/repository/SettingsRepository.kt` | Added `setDownloadDirectory()` |
| `ui/viewmodel/SettingsViewModel.kt` | Added `setDownloadDirectory()` |
| `ui/screens/SettingsScreen.kt` | Added Downloads section with folder picker, `getFolderDisplayName()` helper |
| `domain/repository/DownloadRepository.kt` | Injected `SettingsDataStore`; added `referer` param; reads `dirUri` from settings; try-catch on worker enqueue |
| `service/DownloadService.kt` | Added `EXTRA_REFERER`, `EXTRA_DIR_URI`; `Referer` header in OkHttp; `getDownloadDir(dirUri)`; `convertSAFUriToPath()`; `DocumentsContract` import |
| `ui/screens/WebScreen.kt` | Removed `/download` URL intercept; added cookies+referer to both download paths; fixed `PendingDownload`; removed duplicate JS click interceptor; cleaned up `injectDownloadInterceptor` |
| `MainActivity.kt` | Fixed new tab navigation: empty URL now goes to Home instead of previous WebScreen |
