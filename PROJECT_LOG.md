# TuTu Companion - Project Log & Status

**Last Updated:** March 23, 2026, 11:20 PM  
**Session Owner:** Raman Subedi (ramansubedi0309@gmail.com)  
**GitHub Repo:** https://github.com/Raman21676/tutu  
**Privacy Policy:** https://raman21676.github.io/privacy-policies/tutu-browser/

---

## 📱 PROJECT OVERVIEW

**TuTu Companion** is an Android app that provides quick access to websites and videos with unique features like background playback and floating window multitasking.

**Key Differentiator:** NOT positioned as a "browser" (to avoid Google Play rejection) but as a "Video & Web Companion" tool.

---

## ✅ WHAT WAS COMPLETED TODAY

### 1. UI/UX Improvements
- ✅ Reduced unwanted spacing on HomeScreen
- ✅ Removed HorizontalDivider between settings and media options
- ✅ Changed app name display to "TuTu" with subtitle "Fast & Private"
- ✅ Changed button text from "Open WebView" to "Open"

### 2. Play Store Repositioning (CRITICAL)
**Reason:** Google Play rejects simple WebView "browsers"

**Changes Made:**
- ❌ "TuTu Browser" → ✅ "TuTu Companion"
- ❌ "Fast & Lightweight Web Browser" → ✅ "Fast & Private"
- ❌ Category: Browsers → ✅ Category: Video Players & Editors
- ❌ Tags: Browser, Web Browser → ✅ Tags: Video Player, Web Companion, Background Play

### 3. Privacy Policy Setup
- ✅ Created privacy policy hub at GitHub Pages
- ✅ URL: https://raman21676.github.io/privacy-policies/tutu-browser/
- ✅ Email updated: ramansubedi0309@gmail.com
- ✅ States "No data collection" policy

### 4. Play Store Assets
- ✅ App icon (512x512): `play-store-assets/icons/TuTu-icon-512x512.png`
- ✅ Feature graphic (1024x500): `play-store-assets/feature-graphic/feature-graphic.png`
- ⚠️ Feature graphic text needs update (still says "Browser")
- ✅ 2 Screenshots taken and saved
- ✅ Store listing text written

### 5. Build & Release
- ✅ Release APK: `TuTu-release.apk` (1.7 MB)
- ✅ Release AAB: `TuTu-release.aab` (2.4 MB) - **UPLOAD THIS TO PLAY STORE**
- ✅ All changes committed and pushed to GitHub

### 6. GitHub Repo Updated
- ✅ All code changes pushed
- ✅ New positioning committed
- ✅ Play Store guide added: `PLAY_STORE_READY.md`

---

## 📁 IMPORTANT FILE LOCATIONS

### Project Root: `~/Desktop/TuTu-final/`

| File/Folder | Purpose |
|-------------|---------|
| `TuTu-release.aab` | **Play Store upload file** |
| `TuTu-release.apk` | Test APK for device |
| `PLAY_STORE_READY.md` | Complete submission guide |
| `PROJECT_LOG.md` | This file - project status |
| `play-store-assets/` | All store assets |
| `privacy-hub/` | Privacy policy files |
| `app/src/main/` | Source code |

### Key Source Files:
- HomeScreen: `app/src/main/java/com/tutu/browser/ui/screens/HomeScreen.kt`
- Strings: `app/src/main/res/values/strings.xml`
- Build config: `app/build.gradle.kts`

---

## ⚠️ PENDING TASKS (FOR TOMORROW)

### HIGH PRIORITY (Must Do Before Publishing)

1. **Update Feature Graphic Text**
   - File: `play-store-assets/feature-graphic/feature-graphic.png`
   - Current text: "TuTu Browser - Fast & Lightweight Web Browser"
   - NEW text: "TuTu - Video & Web Companion"
   - Tool: Use Canva (https://www.canva.com) - free
   - Keep same design/colors, just update text

2. **Take 2 More Screenshots**
   - Need: YouTube playing in the app
   - Need: Dark theme mode enabled
   - Save to: `play-store-assets/screenshots/`
   - Format: 1080x1920 preferred (or any 16:9)

3. **Update Privacy Policy Hub Main Page**
   - Add TuTu to the list of apps
   - File: `~/privacy-policies/index.html` (in separate repo)
   - Currently only shows template

### MEDIUM PRIORITY (Can Do After Initial Submission)

4. **Create Google Play Developer Account**
   - URL: https://play.google.com/apps/publish/signup/
   - Cost: $25 one-time
   - Time: 24-48 hours for approval

5. **Submit to Play Console**
   - URL: https://play.google.com/console
   - Create app: "TuTu - Video & Web Companion"
   - Upload AAB file
   - Fill store listing (copy from `play-store-assets/store-listing.md`)
   - Complete Data Safety form
   - Complete Content Rating
   - Submit for review

### LOW PRIORITY (Nice to Have)

6. **More Screenshots**
   - Tablet screenshots (7-inch, 10-inch)
   - Additional phone screenshots

7. **Promotional Video**
   - 30-second demo video
   - Shows background play and floating window features

---

## 🎯 KEY DECISIONS MADE TODAY

### 1. App Positioning Strategy
**Decision:** Position as "Video & Web Companion" NOT "Browser"
**Reason:** Google Play rejects WebView browsers that compete with Chrome
**Result:** Higher chance of approval

### 2. Category Selection
**Decision:** Tools > Video Players & Editors
**Reason:** Less scrutiny than Browsers category
**Benefit:** Focuses on unique video features (background play, floating window)

### 3. Privacy-First Approach
**Decision:** No data collection, everything local
**Reason:** Easier compliance, user trust
**Implementation:** Privacy policy clearly states no data collection

### 4. Feature Prioritization
**Kept:** Background playback, floating window, fullscreen, themes
**Removed:** Quick access bookmarks from bottom (already accessible via URL)
**Added:** Better spacing, cleaner UI

---

## 🚨 IMPORTANT WARNINGS

### DO NOT:
- ❌ Change app name back to "TuTu Browser"
- ❌ Change category back to "Browsers"
- ❌ Emphasize "web browser" in description
- ❌ Remove background play/floating window features (these are the unique value)

### DO:
- ✅ Keep positioning as "Video & Web Companion"
- ✅ Emphasize video features in all descriptions
- ✅ Highlight unique features (background play, floating window)
- ✅ Keep privacy policy updated

---

## 📋 PLAY STORE SUBMISSION INFO

Copy these EXACTLY into Play Console:

**App Name:** TuTu - Video & Web Companion

**Short Description (80 chars):**
Quick access to videos & websites with background play & floating window features!

**Full Description:** See `play-store-assets/store-listing.md`

**Category:** Tools > Video Players & Editors

**Tags:** Video Player, Web Companion, Background Play, Floating Window, Lightweight

**Email:** ramansubedi0309@gmail.com

**Website:** https://raman21676.github.io/privacy-policies/

**Privacy Policy:** https://raman21676.github.io/privacy-policies/tutu-browser/

---

## 🔧 TECHNICAL DETAILS

### App Specifications:
- **Package Name:** com.tutu.browser
- **Version:** 1.0.0 (versionCode: 1)
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 34 (Android 14)
- **APK Size:** ~1.7 MB
- **AAB Size:** ~2.4 MB

### Key Features Implemented:
- ✅ Background audio playback
- ✅ Floating window (PiP-style)
- ✅ Fullscreen video support
- ✅ HTTPS/HTTP toggle
- ✅ Dark/Light theme
- ✅ Auto-rotation toggle
- ✅ Session restore

### Architecture:
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM + Repository
- Data: DataStore for preferences
- Web: Android WebView

---

## 📞 CONTACT & SUPPORT

**Developer:** Raman Subedi  
**Email:** ramansubedi0309@gmail.com  
**GitHub:** https://github.com/Raman21676  

---

## 🚀 NEXT SESSION CHECKLIST (FOR NEW AI AGENT)

When starting fresh tomorrow, the AI should:

1. **Read this file first** (`PROJECT_LOG.md`)
2. **Check GitHub repo** for latest commits
3. **Review `PLAY_STORE_READY.md`** for submission guide
4. **Complete pending tasks:**
   - Update feature graphic text
   - Take 2 more screenshots
   - Update privacy policy hub
5. **Proceed to Play Console submission**

---

## 📝 NOTES FOR FUTURE REFERENCE

### Why This Approach Works:
1. **Avoids direct competition** with Chrome (impossible to win)
2. **Focuses on unique features** (background play, floating window)
3. **Positions as utility tool** (Video Companion) not browser replacement
4. **Emphasizes lightweight** nature (1.7 MB vs Chrome 100+ MB)

### Potential Review Questions from Google:
- Q: "How is this different from Chrome?"
- A: "TuTu focuses on video companion features like background playback and floating window multitasking, not replacing full browser functionality."

- Q: "Why should users download this?"
- A: "Unique background audio and floating window features for multitasking while watching videos."

---

**END OF LOG**

*Next update: After Play Store submission*
