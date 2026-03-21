# TuTu Browser - Complete Play Store Publishing Guide

## 🚀 STEP-BY-STEP PUBLISHING PROCESS

---

## PHASE 1: PREPARATION (Before Play Console)

### 1.1 Verify App Checklist

- [ ] App builds successfully (`./gradlew assembleRelease`)
- [ ] App runs without crashes
- [ ] All features tested (YouTube, TikTok, Fullscreen, Themes)
- [ ] Privacy Policy created (included in `play-store-assets/`)
- [ ] App icon designed (already in `res/mipmap`)

### 1.2 Create Google Play Developer Account

1. Go to: https://play.google.com/apps/publish/signup/
2. Pay $25 one-time registration fee
3. Complete account verification
4. Wait for approval (usually 24-48 hours)

### 1.3 Prepare Required Assets

See `play-store-assets/` folder:
- Screenshots (phone: 1080x1920)
- Feature graphic (1024x500)
- Privacy Policy
- Store listing text

---

## PHASE 2: BUILD RELEASE APK/BUNDLE

### 2.1 Generate Signing Key (First Time Only)

```bash
cd ~/Desktop/TuTu-final

# Generate keystore (keep this file SECURE!)
keytool -genkey -v \
  -keystore tutu-browser.keystore \
  -alias tutu \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Enter details when prompted:
# - Password: [create strong password]
# - Name: [Your Name]
# - Organization: [Your Company/Name]
# - City/State/Country: [Your Location]
```

**⚠️ CRITICAL:** Backup `tutu-browser.keystore` file securely! Lost it = can't update app ever.

### 2.2 Configure Signing in build.gradle.kts

Add to `app/build.gradle.kts` (inside `android` block):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../tutu-browser.keystore")
        storePassword = "YOUR_KEYSTORE_PASSWORD"
        keyAlias = "tutu"
        keyPassword = "YOUR_KEY_PASSWORD"
    }
}

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
    // ... debug config
}
```

### 2.3 Build Release APK/AAB

```bash
# Option 1: Build APK
cd ~/Desktop/TuTu-final
export JAVA_HOME=/Users/kalikali/Library/Java/JavaVirtualMachines/jdk-21.0.6+7/Contents/Home
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk

# Option 2: Build App Bundle (RECOMMENDED for Play Store)
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

**Use App Bundle (AAB)** - it's smaller and optimized by Google Play.

---

## PHASE 3: PLAY CONSOLE SETUP

### 3.1 Create New App

1. Go to https://play.google.com/console
2. Click "Create app"
3. Fill:
   - App name: "TuTu Browser"
   - Default language: English
   - App or game: App
   - Free or paid: Free
   - Check declarations
4. Click "Create app"

### 3.2 Set Up App

#### Dashboard Checklist:

**Privacy & App Content**
- [ ] Privacy Policy: Upload `privacy-policy.md`
- [ ] App Access: "All functionality is available without special access"
- [ ] Ads: "This app does not contain ads"
- [ ] Content Rating: Complete questionnaire (see CONTENT_RATING_GUIDE.md)
- [ ] Target Audience: "Ages 13 and over"
- [ ] News Apps: No
- - [ ] COVID-19: No
- [ ] Data Safety: Complete form (no data collected)
- [ ] Government Apps: No
- [ ] Financial Features: No

**Store Presence**
- [ ] Main store listing (see store-listing.md)
- [ ] Upload screenshots
- [ ] Upload feature graphic
- [ ] Set category: Tools > Browsers
- [ ] Set contact details

**Set Up Release**
- [ ] Create release
- [ ] Upload AAB/APK
- [ ] Set version name: "1.0.0"
- [ ] Add release notes

---

## PHASE 4: RELEASE CONFIGURATION

### 4.1 Create Production Release

1. Go to **Release > Production**
2. Click "Create new release"
3. Upload your AAB file
4. Fill release notes:

```
Initial release of TuTu Browser!

Features:
• Fast and lightweight web browsing
• Quick access to YouTube, TikTok, and more
• Light & Dark theme support
• Fullscreen video playback
• Privacy-focused with local data storage
```

5. Click "Review release"
6. Check for any errors/warnings
7. Fix issues if any
8. Click "Start rollout to Production"

### 4.2 Internal Testing (Optional but Recommended)

Before production, test with internal testers:

1. Go to **Release > Internal testing**
2. Create release
3. Upload AAB
4. Add testers (your email)
5. Create and share link
6. Install and test on your device
7. If good, promote to production

---

## PHASE 5: POST-PUBLISH

### 5.1 What Happens Next

1. **Review Period:** Google reviews your app (1-7 days)
2. **Status Updates:** Check email and Play Console
3. **Approval:** App goes live on Play Store
4. **Rejection:** Fix issues and resubmit

### 5.2 Common Rejection Reasons & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| "App doesn't load" | Network issue in app | Test thoroughly |
| "Misleading description" | Over-promising | Keep description accurate |
| "Missing privacy policy" | No policy uploaded | Add privacy policy |
| "Unsafe browsing" | WebView without Safe Browsing | Implement Safe Browsing API |
| "Crashes on launch" | Bugs in code | Fix crashes, test more |

### 5.3 After Approval

- Monitor crashes in **Quality > Android Vitals**
- Read user reviews
- Respond to feedback
- Plan updates

---

## 📊 APP SIGNING (IMPORTANT!)

Google Play now manages app signing automatically. When you upload your first AAB:

1. Google will show "App Signing" dialog
2. Choose "Use Google-generated key" (recommended)
3. Or upload your own key
4. Google will sign the APKs distributed to users

**Keep your original keystore safe!** It's needed to prove app ownership.

---

## 🔄 UPDATING THE APP

When you need to update:

1. Update version in `app/build.gradle.kts`:
```kotlin
versionCode = 2  // Must increase by 1
versionName = "1.0.1"  // Human-readable
```

2. Build new release AAB:
```bash
./gradlew bundleRelease
```

3. Go to Play Console > Release > Production
4. Create new release
5. Upload new AAB
6. Add changelog
7. Review and publish

---

## 📈 OPTIMIZATION TIPS

### For Better Visibility:
1. Use relevant keywords in description (browser, fast, lightweight, privacy)
2. Respond to user reviews
3. Encourage happy users to rate
4. Update regularly with improvements

### For Better Retention:
1. Monitor crash reports
2. Fix bugs quickly
3. Add requested features
4. Keep app size small

---

## 🆘 TROUBLESHOOTING

### Build Errors:
```bash
# Clean build
./gradlew clean

# Rebuild
./gradlew assembleRelease

# Check dependencies
./gradlew dependencies
```

### Signing Errors:
- Verify keystore password is correct
- Check key alias matches
- Ensure keystore file path is correct

### Play Console Errors:
- Check file sizes (screenshots max 8MB, AAB max 150MB)
- Verify all required fields are filled
- Ensure APK/AAB version code is higher than previous

---

## ✅ FINAL CHECKLIST BEFORE SUBMITTING

- [ ] App builds successfully
- [ ] App tested on real device
- [ ] Release AAB generated
- [ ] Keystore backed up securely
- [ ] Privacy Policy uploaded
- [ ] Screenshots uploaded (minimum 2)
- [ ] Feature graphic uploaded
- [ ] Store listing complete
- [ ] Content rating completed
- [ ] Contact information provided
- [ ] Release notes written
- [ ] No errors in Play Console

---

## 📞 SUPPORT RESOURCES

- **Play Console Help:** https://support.google.com/googleplay/android-developer
- **Publish Checklist:** https://developer.android.com/studio/publish
- **App Signing Help:** https://developer.android.com/studio/publish/app-signing
- **Content Ratings:** https://support.google.com/googleplay/android-developer/answer/188189

---

**Good luck with your TuTu Browser launch! 🚀**
