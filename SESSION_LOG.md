# Nova Browser — Session Log

> **Date:** 2026-04-24
> **Device:** OPPO CPH1801, Android 7.1.1 (ColorOS), ADB ID `1089081` → `1089089`
> **Package:** `com.nova.browser.debug` (debug) / `com.nova.browser` (release)
> **JDK:** `/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home`
> **Build cmd:** `export JAVA_HOME=... && ./gradlew :app:assembleDebug :app:bundleRelease --no-daemon -x lint --quiet`

---

## 🎯 Today's Features Implemented

### 1. Print to PDF
- **File:** `WebScreen.kt` overflow menu → "Print page"
- **Implementation:** `WebView.createPrintDocumentAdapter(jobName)` + `PrintManager.print()`
- **Status:** ✅ Working. Opens Android system print dialog where user selects "Save as PDF"
- **Testing note:** Works when toolbar is visible (Fullscreen setting must be OFF)

### 2. Network Log
- **Files:**
  - `NetworkLog.kt` — Singleton `object` with `MutableStateFlow<List<NetworkLogEntry>>`
  - `NetworkLogScreen.kt` — Compose list showing method, timestamp, URL, mimeType
  - `WebScreen.kt` — hooks into `shouldInterceptRequest()` to feed `NetworkLog.onRequest()`
  - `MainActivity.kt` — `Screen.NetworkLog` route added to NavHost
  - `SettingsScreen.kt` — "Network Log" row in Privacy section
- **Status:** ✅ Working. Shows 559+ entries on TikTok test. Access via Settings → Network Log.

### 3. Custom Search Engines
- **Files:**
  - `CustomSearchEngine.kt` — `@Serializable` data class (name, searchUrl with `%s`)
  - `SearchEngineOption.kt` — Interface unifying built-in and custom engines
  - `SearchEngine.kt` — Updated to implement `SearchEngineOption`, added `resolveSearchEngine()`
  - `CustomSearchEnginesScreen.kt` — Add/edit/delete with FAB, shows built-in + custom sections
  - `SettingsDataStore.kt` — `customSearchEngines` JSON field added to `TutuSettings`
  - `SettingsRepository.kt` — `setCustomSearchEngines()`
  - `SettingsViewModel.kt` — CRUD methods for custom engines
  - `SettingsScreen.kt` — "Custom Search Engines" row + picker integration
  - `MainActivity.kt` — `Screen.CustomSearchEngines` route
  - `HomeScreen.kt` + `Extensions.kt` — Use `resolveSearchEngine()` instead of `SearchEngine.fromName()`
- **Storage format:** Custom engines encoded as `CUSTOM:index` in settings string
- **Status:** ✅ Working. Added "Brave" engine, verified search routing.

### 4. Import / Export User Data
- **Files:**
  - `BackupData.kt` — `@Serializable` model with `BackupSettings`, `BackupHistoryEntry`, `BackupSiteSettings`, `BackupUserScript`
  - `BackupManager.kt` — `exportToDownloads()` and `importFromUri()`
  - `SettingsViewModel.kt` — `exportData()` and `importData()` with `_backupResult` StateFlow
  - `SettingsScreen.kt` — "Export Data" row (direct save) + "Import Data" row (SAF picker + fallback dialog)
  - `SiteSettingsDao.kt` — Added `getAll()` query
  - `SiteSettingsRepository.kt` — Added `getAll()` method
  - `HistoryRepository.kt` — Added `insertHistory()` method
  - `MainActivity.kt` — Injected `siteSettingsRepository` and `userScriptRepository` into `TutuNavigation()` and `SettingsViewModel.Factory`
- **Export location:** `/storage/emulated/0/Nova Downloads/nova_backup_<timestamp>.json`
- **Import fallback:** Scans Nova Downloads folder for `nova_backup*.json` files and shows picker dialog (system `OpenDocument` picker doesn't always work on ColorOS)
- **Status:** ✅ Working. Exported and imported 8 history entries successfully.

---

## 🐛 Problems Encountered & Solutions

### Problem 1: Toolbar hidden → can't access overflow menu
- **Root cause:** `Fullscreen` setting is ON by default in `TutuSettings`. When `fullscreen=true`, `WebScreen` hides both TopAppBar and BottomNavigationBar.
- **Solution:** Navigate to Settings → toggle Fullscreen OFF. Toolbar becomes visible.
- **Lesson:** Always check `fullscreen` state when testing WebScreen features.

### Problem 2: Compose elements not in accessibility tree on Android 7.1
- **Symptom:** `uiautomator dump` only shows WebView content, no Compose buttons/icons.
- **Impact:** Hard to find tap coordinates for automation.
- **Workaround:** Use pixel analysis (`PIL.Image.getpixel()`) to detect UI regions, or brute-force tap grid search.
- **Lesson:** Compose accessibility on ColorOS/Android 7.1 is incomplete. Manual coordinate tapping is needed for E2E testing.

### Problem 3: `ActivityResultContracts.CreateDocument` doesn't open picker on test device
- **Symptom:** Tapping "Export Data" with `CreateDocument` launcher does nothing on ColorOS Android 7.1.
- **Solution:** Changed Export to write directly to `/storage/emulated/0/Nova Downloads/nova_backup_<timestamp>.json` using `File.writeText()`.
- **Lesson:** `CreateDocument`/`OpenDocument` contracts may fail on OEM-skinned Android. Always provide a direct-file fallback.

### Problem 4: MainActivity `TutuNavigation` is a separate `@Composable` function
- **Symptom:** Added `@Inject lateinit var` fields to `MainActivity`, but `Unresolved reference` inside `TutuNavigation()` composable.
- **Root cause:** `TutuNavigation` is a top-level `@Composable` function, not inside `MainActivity` class. It only receives parameters explicitly passed to it.
- **Solution:** Added `siteSettingsRepository` and `userScriptRepository` as parameters to `TutuNavigation()`, passed from `MainActivity`.

### Problem 5: Room entities not `@Serializable`
- **Symptom:** `BackupData` containing `SiteSettingsEntity` and `UserScriptEntity` failed compilation.
- **Solution:** Created separate `@Serializable` backup classes (`BackupSiteSettings`, `BackupUserScript`) and mapped to/from Room entities in `BackupManager`.
- **Lesson:** Never try to serialize Room entities directly. Use DTOs for import/export.

### Problem 6: Hilt-injected repositories vs Factory-based ViewModels
- **Symptom:** `SettingsViewModel` uses `ViewModelProvider.Factory` (not `@HiltViewModel`), but needs access to Hilt-injected repositories.
- **Solution:** Passed all required repositories + `Context` through the Factory constructor. Updated both Factory call sites in `MainActivity` (Settings + CustomSearchEngines routes).

### Problem 7: `SearchEngine` enum can't represent custom engines
- **Symptom:** `SearchEngine.fromName("CUSTOM:0")` returns `GOOGLE` (fallback).
- **Solution:** Created `SearchEngineOption` interface. Built-in enum implements it. Custom engine class implements it. Added `resolveSearchEngine(name, customEngines)` helper. Custom engines encoded as `CUSTOM:index` strings.

### Problem 8: `SettingsScreen` has two SettingsViewModel.Factory calls
- **Symptom:** Updated Factory in Settings route but forgot CustomSearchEngines route.
- **Solution:** Both `composable(Screen.Settings.route)` and `composable(Screen.CustomSearchEngines.route)` now use the same expanded Factory signature.

---

## 📁 Key File Paths

| File | Purpose |
|------|---------|
| `~/Desktop/nova/` | Main source code |
| `~/Desktop/nova-final/` | Release builds (APK + AAB) |
| `~/Desktop/nova/VIA_COMPARISON.md` | Feature gap analysis vs Via Browser |
| `~/Desktop/nova/SESSION_LOG.md` | This file — session history |

---

## 🔧 Build & Deploy Commands

```bash
# Build debug APK + release AAB
export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home
cd ~/Desktop/nova
./gradlew :app:assembleDebug :app:bundleRelease --no-daemon -x lint --quiet

# Copy to release folder
cp app/build/outputs/apk/debug/app-debug.apk ~/Desktop/nova-final/NOVA-debug.apk
cp app/build/outputs/bundle/release/app-release.aab ~/Desktop/nova-final/NOVA-release.aab

# Device deploy
adb -s 1089089 uninstall com.nova.browser.debug
adb -s 1089089 install ~/Desktop/nova-final/NOVA-debug.apk
```

---

## 🏗 Architecture Patterns Used Today

1. **StateFlow-backed singleton** — `NetworkLog` (similar to `ResourceSniffer`)
2. **Sealed interface + enum + data class** — `SearchEngineOption` unification
3. **DataStore JSON serialization** — `customSearchEngines` stored as JSON string
4. **Versioned backup DTOs** — `BackupData` with separate `@Serializable` classes
5. **ActivityResultContracts with fallback** — `OpenDocument` + direct file scan

---

## 📋 Remaining Gaps vs Via Browser (Updated)

| Feature | Priority |
|---------|----------|
| Gesture Controls (swipe toolbar, tab cycling) | P2 |
| Per-site Custom CSS/JS | P2 |
| Game Mode | P3 |
| Text Resize | P3 |
| Customizable Homepage | P3 |

**Next recommended feature:** Gesture Controls — Via's signature UX differentiator.

---

## 💡 Agent Tips for Next Session

1. **Always check `Fullscreen` setting** before testing WebScreen UI features.
2. **ColorOS Android 7.1** has poor Compose accessibility → use coordinate tapping or pixel analysis for E2E tests.
3. **Document pickers may not work** on this device → always verify with direct file fallback.
4. **Room entities ≠ serializable** → use backup DTOs for import/export.
5. **Factory-based ViewModels** need all deps passed through Factory constructor.
6. **Search engine resolution** goes through `resolveSearchEngine(name, customEngines)`, not `SearchEngine.fromName()`.
7. **TutuDatabase** is at version 2 with `fallbackToDestructiveMigration()` — schema changes wipe data.
8. **Release AAB** is signed with `nova-browser.keystore` (alias: `tutu`, password: `tutu12345`).
