# Background Audio Playback - Fix Summary

## Issue
Audio stopped playing when the app was minimized, despite enabling "Background Playback" in settings.

## Root Cause
Multiple layered issues:
1. **Timing issue**: Android's internal `WebViewChromium.pauseTimers()` is dispatched ~10-30ms AFTER `super.onPause()` returns
2. **Synchronous calls failed**: Calling `webView.onResume()` synchronously in `onPause()` fired BEFORE Android's internal pause, so Android would pause it again right after
3. **post{} doesn't work on Android 7**: The message queue is throttled immediately when Activity pauses
4. **YouTube's JavaScript**: YouTube detects `document.hidden` and pauses audio via Page Visibility API

## Solution

### 1. WebViewHolder Singleton (`app/src/main/java/com/tutu/browser/util/WebViewHolder.kt`)
Created a singleton to hold WebView reference for synchronous access from MainActivity:
```kotlin
object WebViewHolder {
    var webView: WebView? = null
    var backgroundPlayEnabled: Boolean = false
}
```

### 2. MainActivity.onPause() with 50ms Delay
The key fix - using `postDelayed(50)` instead of synchronous call:
```kotlin
override fun onPause() {
    super.onPause()
    if (WebViewHolder.backgroundPlayEnabled) {
        // postDelayed is REQUIRED. Android's internal WebViewChromium.pauseTimers()
        // is dispatched ~10-30ms AFTER super.onPause() returns.
        Handler(Looper.getMainLooper()).postDelayed({
            WebViewHolder.webView?.onResume()
            WebViewHolder.webView?.resumeTimers()
        }, 50)
        startBackgroundPlayService(currentUrl, currentTitle)
    }
}
```

### 3. Page Visibility Override Script
Inject JavaScript to override `document.hidden` and prevent YouTube from detecting background state:
```javascript
Object.defineProperty(document, 'hidden', {
    get: function() { return false; }, configurable: true
});
Object.defineProperty(document, 'visibilityState', {
    get: function() { return 'visible'; }, configurable: true
});
```

### 4. Periodic Script Re-injection
Re-inject the override script every 1 second to handle YouTube re-registering event listeners:
```kotlin
CoroutineScope(Dispatchers.Main).launch {
    while (isActive) {
        delay(1000)
        webView?.evaluateJavascript(PAGE_VISIBILITY_OVERRIDE_SCRIPT, null)
    }
}
```

### 5. BackgroundAwareWebView
Custom WebView that blocks pause signals when background play is enabled:
```kotlin
override fun onPause() {
    if (!backgroundPlayEnabled) {
        super.onPause()
    }
}
```

### 6. Foreground Service (BackgroundPlayService)
Keeps the app process alive when in background:
- Audio focus request
- WakeLock acquisition
- Persistent notification

## Files Changed
1. `app/src/main/java/com/tutu/browser/util/WebViewHolder.kt` (NEW)
2. `app/src/main/java/com/tutu/browser/MainActivity.kt`
3. `app/src/main/java/com/tutu/browser/ui/screens/WebScreen.kt`
4. `app/src/main/java/com/tutu/browser/ui/screens/BackgroundAwareWebView.kt`
5. `app/src/main/java/com/tutu/browser/service/BackgroundPlayService.kt`
6. `gradle/wrapper/gradle-wrapper.properties`

## Test Results
✅ Audio continues playing when app is minimized
✅ Video state/timeline preserved when app reopens
✅ Notification shows "Playing in background"
✅ No app reset when tapping notification

## Device Tested
- Android 7.1 (API 25) - OPPO/ColorOS

## Commits
- 89dc4ef - Disable auto-restore of last URL
- bfbf744 - Fix: Use postDelayed(50ms) for WebView resume
- e7fa1fc - Add aggressive periodic script injection
- 1793ed4 - Definitive fix for background audio
- b0e7316 - Fix background audio - synchronous service start
- 2a671b2 - Add background play lifecycle fixes
- 8706574 - Fix background playback and app reset issues

## Date Fixed
March 21, 2026
