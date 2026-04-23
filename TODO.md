# NOVA Browser - Feature Implementation TODO

## Goal: Match Via Browser's Feature Set
**Timeline:** 16-20 weeks | **Current Phase:** Phase 2 - History & Downloads

---

## ✅ Phase 1: Core Infrastructure (COMPLETED)
**Duration:** Week 1-2 | **Status:** DONE

### 1.1 Dependency Injection Setup
- [x] Add Hilt plugin to build.gradle
- [x] Add KSP plugin for Room
- [x] Create TutuApplication with @HiltAndroidApp
- [x] Create DatabaseModule
- [x] Create DataStoreModule

### 1.2 Room Database Setup
- [x] Create HistoryEntity
- [x] Create DownloadEntity  
- [x] Create SiteSettingsEntity
- [x] Create HistoryDao
- [x] Create DownloadDao
- [x] Create SiteSettingsDao
- [x] Create TutuDatabase

### 1.3 Repository Layer
- [x] Create HistoryRepository
- [x] Create DownloadRepository
- [x] Create SiteSettingsRepository
- [x] Update BookmarkRepository for Hilt
- [x] Update SettingsRepository for Hilt

### 1.4 Manifest & Permissions
- [x] Add WRITE_EXTERNAL_STORAGE permission
- [x] Add POST_NOTIFICATIONS permission

---

## ✅ Phase 2: History & Downloads (COMPLETED - With Known Issues)
**Duration:** Week 3-4 | **Status:** COMPLETE (see notes below)

### 2.1 History Manager ✅
- [x] Track page visits in WebView
- [x] Save history to database
- [x] Update history on page title change
- [x] Create HistoryViewModel
- [x] Create HistoryScreen UI
- [x] Add search functionality
- [x] Add clear history options
- [x] Integrate into navigation

### 2.2 Download Manager ✅ (Partial - See Issue Below)
- [x] Create custom DownloadWorker
- [x] Track download progress
- [x] Show download notifications
- [x] Create DownloadsViewModel
- [x] Create DownloadsScreen UI
- [x] Handle download clicks
- [x] Integrate into navigation
- [x] Fix JavaScript-triggered downloads (window.open)
- [x] Fix download repository injection
- [ ] **BUG: Download status stuck at "Downloading" (never shows "Completed")** ⚠️

---

## 🚀 Phase 3: Play Store Preparation (IN PROGRESS)
**Status:** Almost Ready

### 3.1 Assets ✅
- [x] Release AAB built (3.2 MB)
- [x] Feature graphic updated
- [x] Privacy policy live
- [x] Store listing prepared

### 3.2 Screenshots ⚠️
- [ ] Take new screenshots (current ones show old ToS-violating UI)
- [ ] Home screen (light)
- [ ] Home screen (dark)
- [ ] YouTube playing
- [ ] Web browsing

### 3.3 Submission ⏳
- [ ] Create Play Developer account
- [ ] Upload AAB to Play Console
- [ ] Complete content rating
- [ ] Submit for review

---

## ⏳ Phase 4: Future Features (PENDING)
**Duration:** Week 5-6 | **Status:** NOT STARTED

### 3.1 Ad Blocking
- [ ] Create AdBlockFilter class
- [ ] Parse ABP filter syntax
- [ ] Load EasyList filter
- [ ] Implement request blocking in WebView
- [ ] Element hiding CSS injection
- [ ] AdBlock toggle in settings
- [ ] Per-site ad block settings

### 3.2 Tracker Blocking
- [ ] Integrate Disconnect tracker list
- [ ] Block 3rd party cookies
- [ ] Add Do Not Track header
- [ ] Add GPC (Global Privacy Control)

### 3.3 True Incognito Mode
- [ ] Create incognito tab support
- [ ] Separate cookie storage for incognito
- [ ] No history in incognito
- [ ] Auto-clear on exit

---

## ⏳ Phase 4: Advanced Browsing (PENDING)
**Duration:** Week 7-8 | **Status:** NOT STARTED

### 4.1 Find in Page
- [ ] Add findInPage JavaScript interface
- [ ] Create FindInPage UI bar
- [ ] Highlight matches
- [ ] Navigate between matches
- [ ] Show match counter

### 4.2 Desktop Mode
- [ ] Create desktop user agent strings
- [ ] Toggle in menu
- [ ] Per-site desktop mode setting
- [ ] Save preference per site

### 4.3 Page Tools
- [ ] Save page as PDF
- [ ] Save page for offline
- [ ] Print page
- [ ] View page source
- [ ] Resource sniffer (images/videos)

### 4.4 Translation
- [ ] Integrate Google Translate API
- [ ] Detect page language
- [ ] Translate page option
- [ ] Show original/translation toggle

---

## ⏳ Phase 5: Customization (PENDING)
**Duration:** Week 9-10 | **Status:** NOT STARTED

### 5.1 Search Engines
- [ ] Create search engine data model
- [ ] Add popular search engines (Google, Bing, DuckDuckGo)
- [ ] Custom search engine support
- [ ] Search suggestions
- [ ] Search engine selector UI

### 5.2 Import/Export
- [ ] Export bookmarks to HTML
- [ ] Import bookmarks from HTML
- [ ] Export settings JSON
- [ ] Full data backup
- [ ] Cross-browser import (Chrome, Firefox)

### 5.3 User Interface
- [ ] True night mode for web content
- [ ] Custom themes support
- [ ] Homepage customization
- [ ] Toolbar customization options

---

## ⏳ Phase 6: User Scripts (PENDING)
**Duration:** Week 11-12 | **Status:** NOT STARTED

### 6.1 Script Engine
- [ ] Create userscript runtime
- [ ] Support @match/@include patterns
- [ ] GM_* API implementation
- [ ] Script injection timing
- [ ] Script sandbox

### 6.2 Script Manager
- [ ] Script manager UI
- [ ] Install from URL
- [ ] Install from file
- [ ] Script editor
- [ ] Enable/disable scripts
- [ ] Auto-update scripts

---

## ⏳ Phase 7: Data Sync (PENDING)
**Duration:** Week 13-14 | **Status:** NOT STARTED

### 7.1 WebDAV Sync
- [ ] WebDAV client implementation
- [ ] Sync bookmarks
- [ ] Sync settings
- [ ] Sync history (optional)
- [ ] Conflict resolution
- [ ] Encryption option

---

## ⏳ Phase 8: Polish & Optimization (PENDING)
**Duration:** Week 15-16 | **Status:** NOT STARTED

### 8.1 Performance
- [ ] Memory optimization
- [ ] Startup time optimization
- [ ] Cache management
- [ ] Image lazy loading
- [ ] Battery optimization

### 8.2 Accessibility
- [ ] Keyboard shortcuts
- [ ] Screen reader support
- [ ] Text size adjustments
- [ ] High contrast mode

### 8.3 Advanced Features
- [ ] Gesture customization
- [ ] Long-press menu customization
- [ ] Custom CSS injection
- [ ] Custom JavaScript injection

---

## 📊 Progress Summary

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: Infrastructure | ✅ COMPLETE | 100% |
| Phase 2: History & Downloads | ✅ COMPLETE* | 95% |
\* Download status update bug remains - see 2.2 notes
| Phase 3: Privacy & Security | ⏳ PENDING | 0% |
| Phase 4: Advanced Browsing | ⏳ PENDING | 0% |
| Phase 5: Customization | ⏳ PENDING | 0% |
| Phase 6: User Scripts | ⏳ PENDING | 0% |
| Phase 7: Data Sync | ⏳ PENDING | 0% |
| Phase 8: Polish | ⏳ PENDING | 0% |

**Overall Progress:** 18.75% (1.5/8 phases complete)

### Session History:
- **Session 1:** Removed ToS violations, basic tab/bookmark setup
- **Session 2:** Added Hilt/Room infrastructure, History Manager (COMPLETED)
- **Session 3:** Completed Downloads Manager UI, Play Store preparation
- **Session 4:** Fixed JS-triggered downloads, fixed repository injection, added download tracking
  - **Known Issue:** Download status stuck at "Downloading" (Worker not updating database)
- **Session 5 (2026-04-23):** TopAppBar overlap fix, Settings gear icon, RustPDF-style output directory dialog
  - **Completed:** WebScreen overflow menu, HomeScreen Settings access, 3-option download folder dialog
  - **Known Issue:** ColorOS file manager does not support folder navigation via intent (platform limitation)
  - **Build Fix:** Updated local.properties JDK path to Java 21
  - **See:** `2026-04-23.session-log.md` for full details

---

## 🎯 Current Sprint: Fix Download Status Bug

### Tasks for Next Session:
1. [ ] Debug DownloadWorker - verify it's running
2. [ ] Fix database status update (STATUS_SUCCESSFUL not being saved)
3. [ ] Test UI recomposition when status changes
4. [ ] Alternative: Use BroadcastReceiver instead of Worker

### Recent Changes (Session 4):
- Fixed `onCreateWindow` for JavaScript-triggered downloads
- Fixed `downloadRepository` null pointer in `setDownloadListener`
- Created `DownloadWorker` for tracking download progress
- Added status polling from DownloadManager

### Known Issues:
1. ⚠️ Download status stuck at "Downloading" (file saves, but UI never updates)
   - Location: `app/src/main/java/com/nova/browser/worker/DownloadWorker.kt`
   - Worker may not be running or database update failing

---

## 📝 Notes

- Use Hilt for dependency injection
- Follow MVVM architecture
- Use Jetpack Compose for UI
- Maintain clean separation of concerns
- Test each feature before moving to next
- Keep app size under 15MB target
