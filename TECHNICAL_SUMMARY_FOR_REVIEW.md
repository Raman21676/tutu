# Tutu Browser - Technical Summary for Professor Review

## 1. Project Overview

**Tutu Browser** is a lightweight Android WebView browser built with:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository pattern
- **Minimum SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

### Key Features
- Basic web browsing with URL input
- HTTPS/HTTP toggle
- **Fullscreen toggle** (affects WebScreen UI)
- Auto-rotate toggle
- Quick access bookmarks
- Session restore dialog
- External browser opening

---

## 2. Architecture

### 2.1 Navigation Structure (MainActivity.kt)
```
NavHost (Jetpack Compose Navigation)
├── HomeScreen (/) - URL input, toggles, bookmarks
├── WebScreen (/web/{url}) - WebView browser
└── SettingsScreen (/settings) - Additional settings
```

### 2.2 Data Flow
```
UI Layer (Compose Screens)
    ↕
ViewModel Layer (StateFlow)
    ↕
Repository Layer
    ↕
Data Layer (DataStore preferences)
```

### 2.3 Key Components

| Component | Responsibility |
|-----------|---------------|
| `HomeScreen` | URL input, settings toggles, bookmark grid |
| `WebScreen` | WebView display, navigation controls |
| `WebViewModel` | Manages WebView state (URL, loading, errors) |
| `SettingsRepository` | Persists settings to DataStore |
| `WebViewPool` | WebView instance management (currently unused) |

---

## 3. Current WebScreen Implementation

### 3.1 WebView Configuration
```kotlin
settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = true
    setSupportMultipleWindows(true)      // For window.open()
    javaScriptCanOpenWindowsAutomatically = true
    mediaPlaybackRequiresUserGesture = false  // Critical for YouTube
    userAgentString = MOBILE_USER_AGENT
}
```

### 3.2 Key Implementation Details

**Single WebView approach (current):**
- Uses Jetpack Compose `AndroidView` to embed WebView
- Forces hardware rendering: `setLayerType(View.LAYER_TYPE_HARDWARE, null)`
- Forces redraw after page load: `view.invalidate()` in `onPageFinished`
- Handles `window.open()` by extracting URL and loading in same WebView

**Fullscreen video handling:**
```kotlin
override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
    // Adds video view to Activity's decorView (not local container)
    val decorView = (context as Activity).window.decorView as FrameLayout
    decorView.addView(view, ...)
    // Enters immersive mode
}
```

---

## 4. The Problem: YouTube Shorts Not Displaying

### 4.1 Symptom
When user taps the "Shorts" button on YouTube:
1. URL changes to `https://m.youtube.com/shorts/...` (confirmed in logs)
2. **Display stays on YouTube home page** (Shorts content doesn't appear)
3. Tap back button → Shorts suddenly appear (as if they were loaded but invisible)

### 4.2 Observations
- Issue occurs when "Fullscreen" toggle is enabled in HomeScreen
- Regular page navigation works fine
- Only affects SPA (Single Page Application) navigation like YouTube Shorts
- `onPageFinished` is called, `view.invalidate()` is called, but display doesn't update

### 4.3 Root Cause Theories

#### Theory 1: Compose + WebView Rendering Sync Issue
- YouTube Shorts uses `history.pushState()` (SPA navigation)
- WebView loads content internally but Compose doesn't trigger recomposition
- Back button causes Android lifecycle event that forces redraw

#### Theory 2: Hardware Acceleration/Invalidation Failure
- `setLayerType(LAYER_TYPE_HARDWARE, null)` and `invalidate()` may not be sufficient
- WebView may be rendering off-screen or to a cached bitmap

#### Theory 3: Window/DecorView Conflicts
- The "Fullscreen" toggle in HomeScreen affects how WebScreen is displayed
- When `fullscreen = true`, WebScreen uses `Modifier.fillMaxSize()` without padding
- May cause edge case with WindowInsets or decorView

---

## 5. Attempted Solutions

### Solution 1: WebView Stack (Multiple WebViews)
**Approach**: Create new WebView for each `window.open()`, manage stack
**Result**: Failed - added complexity without solving the core issue
**Code**: Used `FrameLayout` with visibility toggling (`View.INVISIBLE/VISIBLE`)

### Solution 2: Single WebView + Hardware Layer + Invalidate
**Approach**: Single WebView, force hardware rendering, invalidate on page finish
**Result**: Still failing - Shorts don't display until back button pressed
**Current Code**:
```kotlin
setLayerType(View.LAYER_TYPE_HARDWARE, null)

override fun onPageFinished(view: WebView, url: String) {
    viewModel.onPageFinished(url)
    view.invalidate()  // Force redraw
}
```

### Solution 3: Window Focus/Layout Params Manipulation
**Approach**: Request focus, force layout passes
**Result**: Not tested - needs professor guidance

---

## 6. Reproduction Steps

1. Build and install APK: `./gradlew assembleDebug`
2. Open Tutu Browser app
3. **Enable "Fullscreen" toggle** on HomeScreen (critical!)
4. Type "youtube.com" in URL field
5. Tap "Open in WebView"
6. Once YouTube loads, tap "Shorts" button (bottom navigation)
7. **Observe**: URL changes to `/shorts/...` but display stays on home page
8. Tap Android back button
9. **Observe**: Shorts content suddenly appears

---

## 7. File Locations

| File | Path |
|------|------|
| WebScreen | `app/src/main/java/com/tutu/browser/ui/screens/WebScreen.kt` |
| WebViewModel | `app/src/main/java/com/tutu/browser/ui/viewmodel/WebViewModel.kt` |
| MainActivity | `app/src/main/java/com/tutu/browser/MainActivity.kt` |
| HomeScreen | `app/src/main/java/com/tutu/browser/ui/screens/HomeScreen.kt` |
| Settings | `app/src/main/java/com/tutu/browser/data/local/datastore/SettingsDataStore.kt` |
| Constants | `app/src/main/java/com/tutu/browser/util/Constants.kt` |

---

## 8. Key Code Sections for Review

### 8.1 WebScreen WebView Setup (Lines 98-200)
Contains:
- `setLayerType(View.LAYER_TYPE_HARDWARE, null)`
- `view.invalidate()` in `onPageFinished`
- `onCreateWindow` handling
- Fullscreen video `onShowCustomView`

### 8.2 WebViewModel Fullscreen State (Lines 32-40)
```kotlin
private val _fullscreen = MutableStateFlow(false)
val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()

init {
    viewModelScope.launch {
        settingsRepository.settings.collect { settings ->
            _fullscreen.value = settings.fullscreen
        }
    }
}
```

### 8.3 HomeScreen Fullscreen Toggle (Lines 141-146)
```kotlin
ToggleSwitch(
    checked = settings.fullscreen,
    onCheckedChange = { viewModel.setFullscreen(it) },
    title = stringResource(R.string.toggle_fullscreen),
    icon = Icons.Default.Fullscreen
)
```

---

## 9. Questions for Professor

1. **Is `invalidate()` sufficient to force WebView redraw in Compose?**
   - Should we use `requestLayout()` or `forceLayout()` instead?

2. **Could the issue be related to Compose's `AndroidView` disposal/recreation?**
   - Does `AndroidView` cache the WebView's drawing surface?

3. **Is there a known issue with WebView + SPA navigation in Compose?**
   - Any recommended patterns for forcing recomposition?

4. **Should we use `setLayerType(LAYER_TYPE_SOFTWARE, null)` instead?**
   - Trade-offs between hardware/software rendering?

5. **Could the Fullscreen toggle logic be interfering?**
   - WebScreen observes fullscreen state and adjusts padding

---

## 10. Build Instructions

```bash
cd ~/Desktop/tutu
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk

# Install:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**Summary**: The WebView loads YouTube Shorts content (URL changes correctly), but the display doesn't update until back button is pressed. This suggests a rendering/synchronization issue between WebView and Jetpack Compose's `AndroidView` wrapper. Multiple approaches attempted without success.
