# NOVA Browser vs Via Browser - Feature Comparison

## Overview

| Feature | NOVA Browser | Via Browser | Notes |
|---------|-------------|-------------|-------|
| **App Size** | ~5-8 MB | ~1-2 MB | Via is ultra-lightweight |
| **Startup Speed** | Fast | Millisecond-level | Via has faster cold start |
| **WebView** | Android System WebView | Android System WebView | Both use native WebView |
| **Target SDK** | 34 (Android 14) | 34+ | Both modern |

---

## Core Browsing Features

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Tab Management** | ✅ Full support | ✅ Full support | **Parity** |
| - Tab switcher | ✅ Yes | ✅ Yes | **Parity** |
| - Tab count badge | ✅ Yes | ✅ Yes | **Parity** |
| - New tab | ✅ Yes | ✅ Yes | **Parity** |
| - Close tab | ✅ Yes | ✅ Yes | **Parity** |
| - Duplicate tab | ✅ Yes | ✅ Yes | **Parity** |
| **Bookmarks** | ✅ Full manager | ✅ Full manager | **Parity** |
| - Add/Edit/Delete | ✅ Yes | ✅ Yes | **Parity** |
| - Search bookmarks | ✅ Yes | ✅ Yes | **Parity** |
| - Folders support | ❌ No | ✅ Yes | **Via wins** |
| - Import/Export | ❌ No | ✅ Yes | **Via wins** |
| **History** | ❌ Not implemented | ✅ Full history | **Via wins** |
| **Downloads** | ⚠️ Basic (system DM) | ✅ Built-in manager | **Via wins** |

---

## Privacy & Security

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Ad Blocking** | ❌ Not implemented | ✅ Built-in ABP filters | **Via wins** |
| **Tracker Blocking** | ❌ Not implemented | ✅ Anti-tracking | **Via wins** |
| **Incognito Mode** | ⚠️ Tab-level only | ✅ Full incognito | **Via wins** |
| **Do Not Track** | ❌ Not implemented | ✅ GPC support | **Via wins** |
| **3rd Party Cookies** | ⚠️ WebView default | ✅ Block option | **Via wins** |
| **HTTPS Enforcement** | ✅ Toggle available | ✅ HTTPS-First | **Parity** |
| **Certificate Viewer** | ❌ Not implemented | ✅ View SSL cert | **Via wins** |

---

## Customization

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Themes** | ✅ Dark/Light/Follow System | ✅ + Custom themes | **Via wins** |
| **Homepage** | Fixed layout | ✅ Fully customizable | **Via wins** |
| **Toolbar** | Bottom fixed | ✅ Customizable position | **Via wins** |
| **Search Engines** | ❌ Fixed (Google) | ✅ Multiple + Custom | **Via wins** |
| **User Agent** | ⚠️ Clean WebView UA | ✅ Multiple + Custom | **Via wins** |
| **Desktop Mode** | ❌ Not implemented | ✅ Per-site toggle | **Via wins** |
| **Font Size** | ❌ Not implemented | ✅ Adjustable | **Via wins** |
| **Force Zoom** | ❌ Not implemented | ✅ Available | **Via wins** |

---

## Advanced Features

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Reader Mode** | ✅ Yes | ✅ Yes | **Parity** |
| **Night Mode** | ❌ Not implemented | ✅ Advanced night mode | **Via wins** |
| **Page Translation** | ❌ Not implemented | ✅ Built-in translate | **Via wins** |
| **Find in Page** | ❌ Not implemented | ✅ Yes | **Via wins** |
| **Save Page as PDF** | ❌ Not implemented | ✅ Yes | **Via wins** |
| **Offline Pages** | ❌ Not implemented | ✅ Save for offline | **Via wins** |
| **Print Page** | ❌ Not implemented | ✅ Yes | **Via wins** |
| **QR Scanner** | ✅ Yes | ✅ Yes | **Parity** |
| **QR Code Share** | ✅ Yes | ✅ Yes | **Parity** |
| **Resource Sniffing** | ❌ Not implemented | ✅ Yes | **Via wins** |
| **View Source** | ❌ Not implemented | ✅ Yes | **Via wins** |

---

## User Scripts & Extensions

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **User Scripts** | ❌ Not implemented | ✅ Full GreaseMonkey support | **Via wins** |
| **Script Manager** | ❌ N/A | ✅ Yes | **Via wins** |
| **Filter Lists** | ❌ N/A | ✅ AdBlock filters | **Via wins** |
| **AI Support** | ❌ Not implemented | ✅ AI features (Play Store) | **Via wins** |

---

## Site Settings

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Per-site Config** | ❌ Not implemented | ✅ Granular control | **Via wins** |
| - JavaScript toggle | ❌ No | ✅ Per-site | **Via wins** |
| - Cookie management | ❌ No | ✅ Per-site | **Via wins** |
| - Ad blocking | ❌ N/A | ✅ Per-site | **Via wins** |
| - Desktop mode | ❌ No | ✅ Per-site | **Via wins** |
| **Permissions Manager** | ⚠️ System default | ✅ In-app control | **Via wins** |

---

## Data & Sync

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **Cloud Sync** | ❌ Not implemented | ✅ WebDAV sync | **Via wins** |
| **Data Export** | ❌ Not implemented | ✅ Full data export | **Via wins** |
| **Data Import** | ❌ Not implemented | ✅ Full data import | **Via wins** |
| **Auto Backup** | ❌ Not implemented | ✅ Available | **Via wins** |

---

## ToS Compliance (Critical for Play Store)

| Feature | NOVA Browser | Via Browser | Status |
|---------|-------------|-------------|--------|
| **YouTube Background Play** | ❌ **REMOVED** | ❌ Not present | **Parity** |
| **TikTok Patches** | ❌ **REMOVED** | ❌ Not present | **Parity** |
| **Request Interception** | ❌ **REMOVED** | ⚠️ For ad blocking only | **NOVA cleaner** |
| **Clean WebView** | ✅ Yes | ✅ Yes | **Parity** |

---

## Summary

### NOVA Browser Advantages
1. **Clean slate** - No ToS violations, safe for Play Store
2. **Simple codebase** - Easier to maintain
3. **Modern architecture** - Jetpack Compose, MVVM
4. **Fast development** - Kotlin-first approach

### Via Browser Advantages
1. **Mature feature set** - 6+ years of development
2. **Ultra-lightweight** - ~1-2 MB vs ~5-8 MB
3. **Advanced privacy** - Built-in ad/tracker blocking
4. **Highly customizable** - Power user paradise
5. **Script support** - GreaseMonkey compatibility
6. **Sync capability** - WebDAV support

### Recommendation

**For Play Store Submission:** NOVA Browser ✅
- Clean implementation without ToS violations
- Core browser features present (tabs, bookmarks, reader mode)
- Modern Android architecture
- Easier to get approved

**For Power Users:** Via Browser
- More features than any other lightweight browser
- Highly customizable
- Active development (updates every 2-4 weeks)
- Established user base

---

## Play Store Positioning

**NOVA Browser Positioning:**
> "Fast & Private Browser - Clean, lightweight browsing without distractions"

**Key Selling Points:**
1. No ads, no tracking
2. Clean minimal interface
3. Tab management
4. Bookmark manager
5. Reader mode
6. QR scanner
7. Dark mode support

**Target Users:**
- Users who want a simple, clean browser
- Privacy-conscious users
- Users frustrated with bloated browsers
- Users who need basic browsing without complications
