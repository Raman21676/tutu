# NOVA Browser - Final Build

A lightweight, privacy-focused Android web browser with theme support and video optimization.

## 🚀 What's Included

### Source Code (`app/`)
- Complete Android project ready to build
- All features implemented and tested

### Play Store Assets (`play-store-assets/`)
- Privacy Policy (required for Play Store)
- Store listing content (title, description)
- Screenshots guide
- Content rating guide
- Feature graphic specifications

### Documentation
- `PUBLISHING_GUIDE.md` - Complete step-by-step publishing instructions
- `SCREENSHOTS_GUIDE.md` - How to create required graphics
- `CONTENT_RATING_GUIDE.md` - Content rating questionnaire answers

## 🎨 Features

### Core Features
- ✅ Fast, lightweight web browsing
- ✅ Privacy-focused (local data storage only)
- ✅ Quick access bookmarks (YouTube, TikTok, Facebook, Instagram, Google, Twitter)
- ✅ Fullscreen video playback
- ✅ Light & Dark theme support
- ✅ System theme following

### Technical Features
- ✅ Hardware-accelerated rendering
- ✅ SPA navigation support (YouTube Shorts, TikTok)
- ✅ Modern fullscreen implementation (WindowInsets API)
- ✅ WebView optimization for video content

## 📋 Requirements

- Android 5.0+ (API 21)
- Target SDK: 34 (Android 14)
- Internet permission only

## 🛠️ Building

### Debug Build
```bash
cd ~/Desktop/NOVA-final
export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (for Play Store)
```bash
# Generate signing key (first time only)
keytool -genkey -v -keystore nova-browser.keystore -alias tutu -keyalg RSA -keysize 2048 -validity 10000

# Build release AAB
./gradlew bundleRelease
```

AAB: `app/build/outputs/bundle/release/app-release.aab`

## 📱 Testing Checklist

Before publishing, test these features:

### Basic Functionality
- [ ] Open YouTube and play videos
- [ ] Tap "Shorts" button - should display immediately
- [ ] Navigate to TikTok - videos should play
- [ ] Test fullscreen button on videos
- [ ] Test back navigation

### Theme Features
- [ ] Toggle "Follow System Theme" - should change with device
- [ ] Turn off system theme, toggle "Dark Mode" manually
- [ ] Verify colors change correctly

### Settings
- [ ] Toggle HTTPS
- [ ] Toggle Fullscreen mode
- [ ] Toggle Auto-rotate

### Bookmarks
- [ ] All 6 bookmarks visible on home screen
- [ ] Tap each bookmark - should load correctly

## 🌐 Play Store Publishing

See `PUBLISHING_GUIDE.md` for complete instructions.

Quick Steps:
1. Create Google Play Developer Account ($25)
2. Prepare assets (screenshots, feature graphic)
3. Build release AAB
4. Upload to Play Console
5. Complete content rating
6. Submit for review

## 📄 Privacy Policy

See `play-store-assets/privacy-policy/privacy-policy.md`

**Key Points:**
- No data collection
- No third-party tracking
- All data stored locally on device
- No analytics services

## 🎨 Theme Colors

- **Primary:** Fresh Green (#4CAF50)
- **Background:** White (Light) / Dark (#1C1B1F)
- **Accent:** Green variations

## 📝 Version

- **Current:** 1.0.0
- **Package:** com.nova.browser
- **Name:** NOVA Browser

## 🤝 Support

For issues or questions:
- Email: [your-email@example.com]
- GitHub: [your-repo-url]

## 📜 License

[Your License Here]

---

**Built with ❤️ using Kotlin & Jetpack Compose**
