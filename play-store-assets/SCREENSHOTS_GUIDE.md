# Play Store Screenshots & Graphics Guide

## 📱 REQUIRED SCREENSHOTS

You need screenshots for at least **2 form factors** (Phone is required):

### Phone Screenshots (REQUIRED)
- **Resolution:** 1080x1920 or 1920x1080 (16:9 aspect ratio)
- **Format:** PNG or JPEG
- **Max file size:** 8 MB per screenshot
- **Quantity:** 2-8 screenshots recommended
- **Naming:** phone_1.png, phone_2.png, etc.

#### Suggested Screenshots for TuTu Browser:

1. **Home Screen (phone_1.png)**
   - Show the clean home screen with URL input
   - Show the toggles (HTTPS, Fullscreen, Auto-Rotate)
   - Show Quick Access bookmarks (YouTube, TikTok, etc.)

2. **YouTube Browsing (phone_2.png)**
   - Show YouTube loaded in the browser
   - Highlight smooth video playback

3. **Dark Theme (phone_3.png)**
   - Show the app in dark mode
   - Can be either home screen or web page

4. **TikTok/YouTube Shorts (phone_4.png)**
   - Show short-form video playing
   - Demonstrates the key feature

5. **Fullscreen Mode (phone_5.png)**
   - Show a website in fullscreen mode
   - Clean, distraction-free browsing

6. **Settings/Theme Toggle (phone_6.png)**
   - Show the theme selection UI
   - Demonstrate customization options

### 7-inch Tablet Screenshots (Optional but recommended)
- Same specs as phone
- Naming: tablet_7_1.png, tablet_7_2.png, etc.

### 10-inch Tablet Screenshots (Optional)
- Same specs as phone
- Naming: tablet_10_1.png, tablet_10_2.png, etc.

---

## 🎨 FEATURE GRAPHIC (REQUIRED)

**File:** `play-store-assets/feature-graphic/feature_graphic.png`

- **Resolution:** 1024x500 pixels
- **Format:** PNG or JPEG (no alpha)
- **Max file size:** 15 MB
- **Safe zone:** Keep important content within 770x270 (center area)
- **Purpose:** Shown at the top of your store listing

### Design Suggestions:
- Use the green theme color (#4CAF50)
- Show "TuTu Browser" text prominently
- Include tagline: "Fast • Light • Private"
- Simple, clean design
- Avoid cluttered elements

---

## 🖼️ APP ICON (Already exists, verify specs)

**Location:** `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

- **Current:** Should be 512x512 pixels for Play Store
- **Format:** PNG or WEBP
- **Shape:** Adaptive icon (round and square masks)
- **Background:** Should work with any background

Verify these icon sizes exist:
- `mipmap-mdpi`: 48x48
- `mipmap-hdpi`: 72x72
- `mipmap-xhdpi`: 96x96
- `mipmap-xxhdpi`: 144x144
- `mipmap-xxxhdpi`: 192x192

---

## 📺 PROMO VIDEO (Optional)

- **Max duration:** 30 seconds
- **Resolution:** 1080p recommended
- **Format:** MP4 or WEBM
- **File size:** Max 100 MB
- **Content:** Show app features in action

---

## 📝 SCREENSHOT CAPTIONS (Optional)

You can add captions for each screenshot in Play Console:

| Screenshot | Suggested Caption |
|------------|-------------------|
| Home Screen | "Quick access to your favorite websites" |
| YouTube | "Seamless video playback" |
| Dark Mode | "Easy on the eyes with dark theme" |
| TikTok/Shorts | "Watch short videos without switching apps" |
| Fullscreen | "Immersive fullscreen browsing" |
| Settings | "Customize your experience" |

---

## ⚠️ IMPORTANT GUIDELINES

### DO:
- ✅ Use actual app screenshots (not mockups or renders)
- ✅ Show real content (not placeholder text)
- ✅ Capture on real devices when possible
- ✅ Ensure text is readable
- ✅ Use consistent styling across screenshots

### DON'T:
- ❌ Add promotional text or banners ON the screenshots
- ❌ Show fake or misleading content
- ❌ Include device frames (Play Store adds them automatically)
- ❌ Use transparent backgrounds
- ❌ Add pricing information

---

## 🛠️ TOOLS FOR CREATING SCREENSHOTS

### Android Studio:
- **Logcat > Screenshot icon** - Capture directly from emulator/device
- **Device Manager > Screen Capture**

### Command Line:
```bash
# Capture screenshot from device
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### Online Tools for Feature Graphic:
- **Canva:** https://www.canva.com (free templates)
- **Figma:** https://www.figma.com (free for personal use)
- **Google Play Feature Graphic Generator:** Various online tools

---

## ✅ SUBMISSION CHECKLIST

Before uploading to Play Store, verify:

- [ ] At least 2 phone screenshots (1080x1920)
- [ ] Feature graphic (1024x500)
- [ ] App icon is 512x512 minimum
- [ ] Screenshots show actual app content
- [ ] Feature graphic is clean and professional
- [ ] All text in screenshots is readable
- [ ] No device frames or promotional text on screenshots

---

## 📤 UPLOAD INSTRUCTIONS

1. Go to Google Play Console: https://play.google.com/console
2. Select your app
3. Go to **Grow > Store Presence > Main store listing**
4. Upload:
   - Screenshots in each form factor section
   - Feature graphic in "Graphics" section
   - App icon (if not already uploaded)
5. Click "Save" then "Publish"
