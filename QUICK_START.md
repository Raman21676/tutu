# TuTu Browser - Quick Start Guide

## 🎯 For Testing (Right Now)

### Install Debug APK
```bash
# Option 1: Use adb
adb install ~/Desktop/TuTu-final/TuTu-Browser-v1.0.0-debug.apk

# Option 2: Direct install on device
# Transfer APK to phone, tap to install
```

### Test TikTok
1. Open TuTu Browser
2. Tap "TikTok" bookmark (or type tiktok.com)
3. TikTok should load
4. Videos should play in feed
5. Tap video for fullscreen

### Test YouTube Shorts
1. Tap "YouTube" bookmark
2. Once loaded, tap "Shorts" button
3. Shorts should display immediately
4. Fullscreen button should work

### Test Themes
1. On home screen, see toggles:
   - "Follow System Theme" - ON by default
   - "Dark Mode" - appears when system theme OFF
2. Toggle to see color changes

---

## 🚀 For Play Store Publishing

### Step 1: Create Assets (30 mins)
1. Take 4-6 screenshots of the app
2. Create feature graphic (1024x500)
3. Review Privacy Policy

### Step 2: Build Release (10 mins)
```bash
cd ~/Desktop/TuTu-final

# Generate signing key (save securely!)
keytool -genkey -v -keystore tutu-browser.keystore -alias tutu -keyalg RSA -keysize 2048 -validity 10000

# Add signing config to app/build.gradle.kts (see PUBLISHING_GUIDE.md)

# Build release AAB
export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home
./gradlew bundleRelease
```

### Step 3: Play Console (1 hour)
1. Go to https://play.google.com/console
2. Create new app "TuTu Browser"
3. Complete all sections using guides in `play-store-assets/`
4. Upload AAB file
5. Submit for review

---

## 📁 Folder Structure

```
TuTu-final/
├── app/                          # Source code
│   ├── src/main/java/           # Kotlin files
│   └── build.gradle.kts         # App config
│
├── play-store-assets/            # Play Store materials
│   ├── privacy-policy/          # Privacy policy
│   ├── screenshots/             # (Add your screenshots)
│   ├── feature-graphic/         # (Add feature graphic)
│   ├── store-listing.md         # Store text
│   ├── CONTENT_RATING_GUIDE.md  # Content rating answers
│   └── SCREENSHOTS_GUIDE.md     # Screenshot specs
│
├── TuTu-Browser-v1.0.0-debug.apk  # Debug APK (ready to test)
├── PUBLISHING_GUIDE.md          # Complete publishing guide
├── QUICK_START.md               # This file
└── README.md                    # Project overview
```

---

## ✅ Testing Checklist

### Basic Tests
- [ ] App opens without crash
- [ ] URL input works
- [ ] YouTube loads
- [ ] YouTube Shorts display
- [ ] TikTok loads
- [ ] TikTok videos play
- [ ] Back button works
- [ ] Home button works
- [ ] Refresh works

### Theme Tests
- [ ] Light theme looks good
- [ ] Dark theme looks good
- [ ] Theme toggle works
- [ ] System theme following works

### Fullscreen Tests
- [ ] Fullscreen toggle in settings
- [ ] Video fullscreen button works
- [ ] Exit fullscreen works
- [ ] Back button exits fullscreen

### Bookmark Tests
- [ ] All 6 bookmarks visible
- [ ] Each bookmark loads
- [ ] Quick access works

---

## 🐛 Known Issues & Solutions

### Issue: TikTok videos not playing
**Solution:** 
- Check internet connection
- TikTok web requires good connection
- Try reloading the page

### Issue: Fullscreen doesn't work
**Solution:**
- Some websites handle fullscreen differently
- Modern API is used, should work on Android 5+
- Check if "Fullscreen" toggle is enabled in settings

### Issue: Theme not changing
**Solution:**
- If "Follow System Theme" is ON, turn it OFF first
- Then toggle "Dark Mode"
- App needs to recreate activity to apply theme

---

## 📞 Need Help?

1. **Build Issues:** Check `PUBLISHING_GUIDE.md` troubleshooting section
2. **Play Store Help:** See `play-store-assets/` for all guides
3. **App Issues:** Check `TECHNICAL_SUMMARY_FOR_REVIEW.md`

---

## 🎉 What's Next?

1. Test the debug APK on your device
2. Create screenshots for Play Store
3. Create feature graphic
4. Follow `PUBLISHING_GUIDE.md` to publish

**Good luck! 🚀**
