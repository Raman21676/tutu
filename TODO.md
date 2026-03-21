# Tutu - Development Todo List

## 🎯 Phase 0: Research & Planning ✅ IN PROGRESS

### Jobs
- [x] Research WebView best practices for Android
- [x] Research Jetpack Compose performance optimization
- [x] Create Architecture Document
- [ ] Create detailed UI mockups/wireframes
- [x] Define color scheme and design system

### Deliverables
- [x] `ARCHITECTURE.md` - Complete architecture documentation
- [ ] `UI_DESIGN.md` - Visual design specifications

---

## 🏗️ Phase 1: Project Setup & Base Structure

### Sub-Phase 1.1: Initialize Project
- [ ] Create Android project with Kotlin + Jetpack Compose
- [ ] Configure minimum SDK (API 21) and target SDK (API 34)
- [ ] Set up ProGuard/R8 for release builds
- [ ] Configure build.gradle with all dependencies
- [ ] Initialize local Git repository

### Sub-Phase 1.2: Core Structure
- [ ] Create package structure (data, ui, util, etc.)
- [ ] Set up Material3 theme with custom colors
- [ ] Create Typography definitions
- [ ] Create App constants file
- [ ] Set up manual DI container

### Sub-Phase 1.3: Data Layer
- [ ] Create Settings data class
- [ ] Implement Settings DataStore
- [ ] Create Bookmark data model
- [ ] Implement Bookmark repository
- [ ] Create Settings repository

### Deliverables
- [ ] Project compiles successfully
- [ ] Basic navigation between screens works
- [ ] Theme colors applied correctly
- [ ] Data persistence tested

---

## 🎨 Phase 2: UI Components & Home Screen

### Sub-Phase 2.1: Common Components
- [ ] Create Logo component (Tutu branding)
- [ ] Create URL Input Field component
- [ ] Create Toggle Switch component (styled)
- [ ] Create Primary/Secondary Button components
- [ ] Create Bookmark Card component
- [ ] Create Progress Indicator component
- [ ] Create Empty State component

### Sub-Phase 2.2: Home Screen
- [ ] Implement HomeScreen layout
- [ ] Create bookmark grid (LazyVerticalGrid)
- [ ] Add default bookmarks (YouTube, Facebook, Instagram, Google)
- [ ] Implement add/remove bookmark functionality
- [ ] Add URL input with validation
- [ ] Implement "Open WebView" button
- [ ] Add toggle switches (Desktop Mode, Fullscreen, Auto-Rotate)

### Sub-Phase 2.3: Navigation
- [ ] Set up Compose Navigation
- [ ] Create navigation routes
- [ ] Implement navigation between Home and Web screens
- [ ] Handle back navigation properly

### Deliverables
- [ ] Home screen visually complete
- [ ] All interactive elements work
- [ ] Navigation smooth and animated
- [ ] Component previews work in Android Studio

---

## 🌐 Phase 3: WebView Implementation

### Sub-Phase 3.1: WebView Configuration
- [ ] Create WebView factory/configurator
- [ ] Implement Desktop/Mobile User Agent switching
- [ ] Configure WebSettings for performance
- [ ] Enable JavaScript and DOM storage
- [ ] Set up cache management
- [ ] Create WebViewPool for instance reuse

### Sub-Phase 3.2: WebView Client
- [ ] Create custom WebViewClient
- [ ] Handle page loading callbacks
- [ ] Implement progress tracking
- [ ] Handle SSL errors securely
- [ ] Add safe browsing support
- [ ] Implement error page for failed loads

### Sub-Phase 3.3: Chrome Client
- [ ] Create custom WebChromeClient
- [ ] Handle fullscreen video requests
- [ ] Implement console logging (debug builds)
- [ ] Handle JavaScript alerts/confirms

### Sub-Phase 3.4: Web Screen UI
- [ ] Create WebScreen layout
- [ ] Add address bar with current URL
- [ ] Implement navigation controls (Back, Forward, Refresh, Home)
- [ ] Add progress bar
- [ ] Implement fullscreen mode
- [ ] Create loading/error states

### Deliverables
- [ ] YouTube loads in desktop mode
- [ ] Facebook/Instagram work correctly
- [ ] Navigation controls functional
- [ ] Memory management tested

---

## ⚙️ Phase 4: Settings & Session Management

### Sub-Phase 4.1: Settings Screen
- [ ] Create SettingsScreen layout
- [ ] Add settings categories
- [ ] Implement toggle preferences
- [ ] Add "Clear Data" option
- [ ] Add "About" section
- [ ] Implement version display

### Sub-Phase 4.2: Session Persistence
- [ ] Save last visited URL on exit
- [ ] Restore session prompt on launch
- [ ] Implement "Open Previous Website" dialog
- [ ] Add "Remember my choice" option

### Sub-Phase 4.3: Advanced WebView Features
- [ ] Implement pull-to-refresh
- [ ] Add gesture-based navigation
- [ ] Implement zoom controls
- [ ] Add find-in-page functionality
- [ ] Implement download handling

### Deliverables
- [ ] Settings persist across sessions
- [ ] Session restore works correctly
- [ ] All settings functional
- [ ] Pull-to-refresh works

---

## 🔋 Phase 5: Performance Optimization

### Sub-Phase 5.1: Memory Optimization
- [ ] Implement onTrimMemory handling
- [ ] Create WebView disposal strategy
- [ ] Optimize bitmap/cache usage
- [ ] Add memory leak detection (debug)
- [ ] Profile memory usage

### Sub-Phase 5.2: APK Size Optimization
- [ ] Configure R8 rules properly
- [ ] Enable resource shrinking
- [ ] Remove unused resources
- [ ] Optimize PNGs/vector drawables
- [ ] Measure final APK size

### Sub-Phase 5.3: Runtime Performance
- [ ] Optimize Compose recompositions
- [ ] Add lazy loading where needed
- [ ] Optimize WebView initialization
- [ ] Reduce startup time
- [ ] Profile CPU usage

### Sub-Phase 5.4: Battery Optimization
- [ ] Pause WebView when backgrounded
- [ ] Optimize location of wake locks
- [ ] Reduce background activity
- [ ] Test battery drain

### Deliverables
- [ ] APK size < 5 MB
- [ ] Memory usage < 100 MB
- [ ] Launch time < 1 second
- [ ] Smooth 60fps animations

---

## 🧪 Phase 6: Testing & Quality Assurance

### Sub-Phase 6.1: Unit Tests
- [ ] Test ViewModels
- [ ] Test Repositories
- [ ] Test Use Cases
- [ ] Test Utilities

### Sub-Phase 6.2: UI Tests
- [ ] Test HomeScreen interactions
- [ ] Test WebScreen navigation
- [ ] Test Settings functionality
- [ ] Test edge cases (no internet, etc.)

### Sub-Phase 6.3: Device Testing
- [ ] Test on Android 5.0 (API 21) device
- [ ] Test on Android 8.0 (API 26) device
- [ ] Test on Android 13+ (API 33+) device
- [ ] Test on low-end device (1GB RAM)
- [ ] Test on tablet

### Sub-Phase 6.4: Website Compatibility
- [ ] Test YouTube (video playback, fullscreen)
- [ ] Test Facebook (login, feed, photos)
- [ ] Test Instagram (stories, posts)
- [ ] Test Twitter/X (timeline, media)
- [ ] Test Google Search
- [ ] Test other popular sites

### Deliverables
- [ ] All tests passing
- [ ] Compatibility report
- [ ] Performance benchmarks
- [ ] Bug fixes completed

---

## 🚀 Phase 7: Play Store Preparation

### Sub-Phase 7.1: Assets
- [ ] Design app icon (adaptive icon)
- [ ] Create feature graphic
- [ ] Take screenshots (phone + tablet)
- [ ] Create promotional video/GIF
- [ ] Write app description
- [ ] Write privacy policy

### Sub-Phase 7.2: Final Polish
- [ ] Review all strings for typos
- [ ] Check accessibility labels
- [ ] Verify all permissions are necessary
- [ ] Add analytics (optional)
- [ ] Review code for security issues

### Sub-Phase 7.3: Release Build
- [ ] Create signed release APK/AAB
- [ ] Test release build thoroughly
- [ ] Upload to Play Store Internal Testing
- [ ] Fix any issues found

### Deliverables
- [ ] Release APK/AAB ready
- [ ] Play Store listing complete
- [ ] Privacy policy published
- [ ] App submitted for review

---

## 📋 Git Commit Strategy

| Phase | Commit Message Prefix |
|-------|----------------------|
| Phase 1 | `setup:` |
| Phase 2 | `ui:` |
| Phase 3 | `webview:` |
| Phase 4 | `settings:` |
| Phase 5 | `perf:` |
| Phase 6 | `test:` |
| Phase 7 | `release:` |

---

## 🎯 Success Criteria

- ✅ APK size under 5 MB
- ✅ Works on Android 5.0+
- ✅ YouTube desktop mode functional
- ✅ Smooth 60fps UI
- ✅ Memory usage under 100MB
- ✅ Passes Play Store review
- ✅ 4.0+ star rating potential

---

*Last Updated: March 2026*
*Next Action: Phase 1.1 - Initialize Project*
