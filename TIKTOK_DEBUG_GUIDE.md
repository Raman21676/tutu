# TikTok Loading Debug Guide

## Changes Made in v1.0.4

### 1. Updated TikTok URL
- Changed from `https://www.tiktok.com` to `https://m.tiktok.com`
- Mobile URL may work better in WebView

### 2. Updated User Agent
- Added TikTok-specific identifier to User Agent
- Now includes: `TikTok/30.5.0`

### 3. Added Debug Logging
- Added Log.d statements to track URL loading
- Check logcat to see what's happening

### 4. Added Render Priority
- Set to HIGH for better performance

---

## How to Debug

### Check Logcat Output

Run this command while trying to load TikTok:

```bash
adb logcat -s WebViewClient:D *:S
```

Look for:
- `Loading URL: https://m.tiktok.com/...`
- `Page started: https://m.tiktok.com/...`
- `Page finished: https://m.tiktok.com/...`
- `Error loading: ...`

### Common TikTok Issues

#### 1. Blank Page / Not Loading
**Possible causes:**
- User Agent blocked by TikTok
- Missing JavaScript support
- Cookies not accepted
- Redirect loop

**Check logcat for:**
```
Error loading: ERR_CONNECTION_REFUSED
Error loading: ERR_SSL_PROTOCOL_ERROR
```

#### 2. Infinite Redirect
TikTok may redirect between:
- `https://www.tiktok.com` → `https://m.tiktok.com`
- `https://m.tiktok.com` → `https://www.tiktok.com`

**Check logcat for multiple redirects.**

#### 3. Shows "Open in App" Popup
TikTok wants users to use their native app.

**Solutions to try:**
- Different User Agent
- Force mobile site

---

## Alternative Solutions to Try

### Solution A: Force Desktop Site for TikTok
Change User Agent back to Desktop ONLY for TikTok:

```kotlin
// In shouldOverrideUrlLoading, detect TikTok and change UA
if (url.contains("tiktok.com")) {
    settings.userAgentString = Constants.DESKTOP_USER_AGENT
}
```

### Solution B: Use Alternative TikTok URL
Try these URLs instead:
- `https://tiktok.com`
- `https://vm.tiktok.com` (short URL service)
- `https://vt.tiktok.com`

### Solution C: Add WebView Debugging
Enable WebView debugging to see console errors:

```kotlin
// In WebView setup
if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Then use Chrome DevTools:
1. Open Chrome on your computer
2. Go to `chrome://inspect`
3. Find your WebView
4. See console errors

---

## Current Configuration (v1.0.4)

| Setting | Value |
|---------|-------|
| URL | https://m.tiktok.com |
| User Agent | Mozilla/5.0 (Linux; Android 14; SM-S918B)... TikTok/30.5.0 |
| JavaScript | Enabled |
| Cookies | Enabled (third-party allowed) |
| DOM Storage | Enabled |
| Cleartext Traffic | Enabled |
| Cache | Default |
| Mixed Content | Allowed |

---

## Next Steps

1. **Install v1.0.4**
   ```bash
   adb install ~/Desktop/TuTu-final/TuTu-Browser-v1.0.4-debug.apk
   ```

2. **Clear app data** (important!)
   - Settings > Apps > TuTu Browser > Storage > Clear Data
   - Or uninstall and reinstall

3. **Test TikTok**
   - Open app
   - Tap TikTok bookmark
   - Wait 10-15 seconds

4. **If still not working:**
   - Run logcat: `adb logcat -s WebViewClient:D`
   - Share the output with me
   - I'll analyze and fix

---

## Known Working TikTok WebView Settings

If current settings don't work, here are settings known to work with TikTok:

```kotlin
settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    cacheMode = WebSettings.LOAD_DEFAULT
    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}

CookieManager.getInstance().apply {
    setAcceptCookie(true)
    setAcceptThirdPartyCookies(webView, true)
}
```

---

## Quick Fix to Try

If TikTok still doesn't work, try manually typing these URLs:

1. `tiktok.com` (without www)
2. `vm.tiktok.com`
3. `www.tiktok.com/foryou`

If one works, we can update the bookmark to use that URL.

---

**Please test v1.0.4 and share logcat output if it still doesn't work!**
