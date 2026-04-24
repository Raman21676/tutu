# Nova Browser vs Via Browser — Feature Comparison

> Last updated: 2026-04-24
> Nova commit: latest (Print PDF + Network Log implemented)

---

## 📊 Head-to-Head Summary

| Category | Via Browser | Nova Browser (TuTu) | Gap |
|----------|------------|---------------------|-----|
| **App Size** | ~1 MB | ~20 MB (debug) / ~4.4 MB (AAB) | ⚠️ Larger |
| **Engine** | Android WebView | Android WebView | ✅ Same |
| **Ad Blocking** | Built-in + filter subscriptions | Domain + keyword + cosmetic | ✅ Comparable |
| **Resource Sniffer** | ✅ Yes | ✅ Yes | ✅ Closed |
| **User Scripts** | ✅ Full Tampermonkey | ✅ Tampermonkey Lite | ✅ Closed |
| **Reader Mode** | ✅ Yes | ✅ Yes | ✅ Same |
| **Night Mode** | ✅ Yes | ✅ Yes (Dark Mode) | ✅ Same |
| **Desktop Mode** | ✅ Yes | ✅ Yes | ✅ Same |
| **Incognito** | ✅ Yes | ✅ Yes | ✅ Same |
| **QR Scanner** | ✅ Yes | ✅ Yes | ✅ Same |
| **Find in Page** | ✅ Yes | ✅ Yes | ✅ Same |
| **Print PDF** | ✅ Yes | ✅ Yes (NEW) | ✅ Closed |
| **Network Log** | ✅ Yes | ✅ Yes (NEW) | ✅ Closed |
| **Game Mode** | ✅ Yes | ❌ No | 🔴 Missing |
| **Custom CSS/JS per site** | ✅ Yes | ❌ No (scripts are global) | 🔴 Missing |
| **Per-site Settings** | ✅ Granular | ⚠️ DB exists, partially wired | 🟡 Partial |
| **Gesture Controls** | ✅ Rich (swipe toolbar, etc.) | ❌ No | 🔴 Missing |
| **Custom Search Engines** | ✅ Yes | ❌ Fixed list (5 engines) | 🔴 Missing |
| **Import/Export Data** | ✅ Settings + bookmarks | ❌ No | 🔴 Missing |
| **Text Resize** | ✅ Yes | ❌ No | 🔴 Missing |
| **Custom Homepage** | ✅ Highly customizable | ⚠️ Logo + search bar only | 🟡 Partial |
| **Tab Management** | Swipe to cycle, endless loop | Grid list, add/close/switch | 🟡 Different |
| **Picture-in-Picture** | ✅ Yes | ✅ Yes | ✅ Same |
| **Download Manager** | Basic | ✅ Rich (pause/resume/notify) | ✅ Better |
| **Session Restore** | ✅ Yes | ✅ Yes | ✅ Same |
| **Bookmarks** | Import/export | Add/edit/delete/search | 🟡 No import/export |
| **History Cleanup** | ✅ Yes | ✅ Auto 30-day + selective | ✅ Better |
| **Cloud Sync** | ❌ No | ❌ No | ✅ Same |
| **Extension Support** | ❌ No | ❌ No | ✅ Same |

---

## ✅ Where Nova Matches or Exceeds Via

### 1. Ad Blocking
| Aspect | Via | Nova |
|--------|-----|------|
| Domain block list | Yes | ✅ 87,770 domains (Steven Black) |
| Keyword blocking | Yes | ✅ URL substring patterns |
| Cosmetic filtering | Yes | ✅ CSS + MutationObserver (YouTube, Google, etc.) |
| Live counter | No | ✅ Shield badge with blocked count |
| Per-site toggle | Yes | ⚠️ DB schema exists, UI partially wired |

### 2. Resource Sniffer
| Aspect | Via | Nova |
|--------|-----|------|
| Network interception | Yes | ✅ `shouldInterceptRequest()` |
| JS media detection | Yes | ✅ Hooks `URL.createObjectURL`, `MediaSource`, `fetch`, `XHR` |
| Categorization | Video/Audio/Image/Other | ✅ Video/Audio/Image/M3U8/Blob/Other |
| Download action | Yes | ✅ Copy URL + Download |
| Bottom sheet UI | Yes | ✅ Compose bottom sheet |

### 3. User Scripts
| Aspect | Via | Nova |
|--------|-----|------|
| Tampermonkey compatible | Yes | ✅ `@match`, `@include`, `@exclude` |
| Script editor | Yes | ✅ In-app editor with metadata parsing |
| Run-at timing | Yes | ✅ `document-start` / `document-end` / `document-idle` |
| GM API | Full | ✅ `GM_getValue`, `GM_setValue`, `GM_addStyle`, `GM_log`, `GM_openInTab`, `GM_notification`, `GM_xmlhttpRequest` |
| Script manager UI | Yes | ✅ List with toggle/delete + FAB add |

### 4. Print to PDF
| Aspect | Via | Nova |
|--------|-----|------|
| WebView print adapter | Yes | ✅ `WebView.createPrintDocumentAdapter()` |
| System print dialog | Yes | ✅ `PrintManager` with "Save as PDF" |
| API requirement | 21+ | ✅ 21+ |
| Overflow menu access | Yes | ✅ "Print page" in WebScreen menu |

### 5. Network Log
| Aspect | Via | Nova |
|--------|-----|------|
| Request interception | Yes | ✅ `shouldInterceptRequest()` |
| Method display | Yes | ✅ GET/POST/PUT/DELETE |
| Timestamp | Yes | ✅ HH:mm:ss.mmm |
| URL display | Yes | ✅ Full URL with truncation |
| MIME type | Yes | ✅ From request headers |
| Blocked indicator | Yes | ✅ `isBlocked` flag |
| Clear log | Yes | ✅ TopAppBar clear button |
| Settings access | Yes | ✅ Settings → Network Log |

### 6. Download Manager
| Aspect | Via | Nova |
|--------|-----|------|
| Pause/Resume | No | ✅ HTTP Range headers |
| Progress notification | Basic | ✅ Foreground service + circular progress |
| Custom folder | Yes | ✅ Phone / SD Card / Custom path |
| MediaStore registration | ? | ✅ Android 10+ gallery visibility |
| Open file location | ? | ✅ ColorOS-aware file manager deep-link |

### 7. TikTok Optimization
| Aspect | Via | Nova |
|--------|-----|------|
| WebView detection evasion | ? | ✅ UA cleaning (`; wv` + `Version/4.0` stripped) |
| Zero JS injection | ? | ✅ Explicitly disabled on TikTok domains |
| SPA navigation | ? | ✅ `doUpdateVisitedHistory` captures URL changes |
| Hardware rendering | ? | ✅ Forced `LAYER_TYPE_HARDWARE` |

---

## 🔴 Major Gaps vs Via

### 1. Import / Export Data
Via allows backing up and restoring **all settings, bookmarks, and site configurations** as a file. Nova has no import/export functionality at all. This is a significant user-lock-in concern.

### 2. Custom Search Engines
Via lets users add arbitrary search engines by query string. Nova only supports 5 hardcoded engines (Google, Bing, DuckDuckGo, Yahoo, Yandex).

### 3. Gesture Controls
Via has rich gesture customization:
- Swipe toolbar to cycle tabs (endless loop)
- Long-press gestures
- Quick touch gestures
- Navigation button in fullscreen

Nova has **zero gesture support** beyond standard Android back navigation.

### 4. Per-Site Custom CSS / JS
Via allows injecting custom CSS and JavaScript on a **per-domain basis** independently of Tampermonkey scripts. Nova only supports global user scripts via the script manager.

### 5. Game Mode
Via has a dedicated game mode (exact behavior unclear, likely performance optimization). Nova has no equivalent.

### 6. Text Resize / Reflow
Via offers text resizing independent of system font size. Nova relies entirely on WebView zoom.

### 7. App Size
Via is famously ~1 MB. Nova debug APK is ~20 MB, release AAB is ~4.4 MB. The size difference comes from:
- Compose/Material3 dependencies
- Room database + OkHttp + Hilt
- Embedded ad-block domain list (~88K domains)
- Multiple feature modules

### 8. Customizable Homepage
Via allows changing:
- Logo size
- Search box border radius and color
- Background

Nova's homepage is fixed (logo + search bar + bookmarks grid).

---

## 🟡 Partial / Almost There

### Per-Site Settings
Nova has a `SiteSettingsEntity` Room table with fields for:
- JavaScript enable/disable
- Ad block enable/disable
- Desktop mode
- Third-party cookies
- Zoom level
- User-Agent override

However, the UI to actually configure these per-site is **not fully wired**. The data layer exists but there's no site-settings screen in the app.

### Bookmark Import/Export
Nova bookmarks are stored in Room but there's no HTML/netscape bookmark import/export. Via supports this.

---

## 🎯 Priority Rankings for Closing Gaps

| Priority | Feature | Effort | Impact |
|----------|---------|--------|--------|
| **P0** | Import/Export bookmarks & settings | Medium | 🔥 Critical — user data portability |
| **P1** | Custom search engines | Low | High — power user essential |
| **P1** | Per-site settings UI | Medium | High — completes existing DB work |
| **P2** | Swipe toolbar to cycle tabs | Medium | Medium — Via's signature UX |
| **P3** | Custom homepage styling | Low | Low — nice to have |
| **P3** | Game Mode | Unknown | Low — niche |
| **P3** | Text resize | Low | Low — system font scaling covers most users |

---

## 🏁 Verdict

**Nova is now feature-competitive with Via on all major differentiators.**

With the recent additions of **Resource Sniffer**, **User Scripts**, **Print PDF**, and **Network Log**, Nova has closed the four largest feature gaps that previously separated it from Via Browser.

The remaining gaps are **quality-of-life and power-user features** rather than core browsing capability:
1. **Data portability** (import/export) — biggest remaining gap
2. **Custom search engines** — easy win
3. **Gesture navigation** — would differentiate Nova
4. **Per-site CSS/JS overrides** — completes the customization story

If those 4 are implemented, Nova would be a genuine alternative to Via for advanced Android users, with the added benefits of a richer download manager, better TikTok compatibility, and a modern Compose UI.
