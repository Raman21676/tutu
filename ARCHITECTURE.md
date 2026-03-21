# Tutu - Lightweight WebView Browser Architecture

## Project Overview

**Tutu** is a lightweight, beautiful WebView wrapper browser for Android designed to run on low-end devices. It allows users to browse websites (YouTube, Facebook, Instagram, etc.) in an app-like experience with desktop mode support.

---

## 🎯 Core Principles

1. **Lightweight** - Minimum APK size, minimal dependencies
2. **Fast** - Optimized rendering, efficient memory management
3. **Compatible** - Works on Android 5.0+ (API 21+)
4. **Beautiful** - Modern Material You design with smooth animations
5. **Battery Efficient** - Smart resource management

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Jetpack Compose)              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Home Screen │  │  Web Screen  │  │  Settings    │       │
│  │  (Bookmarks) │  │  (WebView)   │  │  Screen      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                    ViewModel Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ HomeVM       │  │ WebViewVM    │  │ SettingsVM   │       │
│  │ (Bookmarks)  │  │ (Web State)  │  │ (Preferences)│       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                    Repository/Use Cases                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ BookmarkRepo │  │ WebViewRepo  │  │ PrefsRepo    │       │
│  │ (Room DB)    │  │ (WebView API)│  │ (DataStore)  │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                    Core Components                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ WebViewPool  │  │ CacheManager │  │ UA Manager   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

---

## 📱 Technology Stack

### Language & Framework
| Component | Choice | Reason |
|-----------|--------|--------|
| Language | **Kotlin** | Modern, concise, null-safe |
| UI Toolkit | **Jetpack Compose** | Declarative, efficient, smaller APK |
| Architecture | **MVVM + Repository** | Testable, maintainable |
| DI | **Manual (No Hilt)** | Reduce APK size, simpler for this scope |

### Dependencies (Minimal)
```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Compose
implementation("androidx.activity:activity-compose:1.8.2")
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")

// WebView (AndroidX for compatibility)
implementation("androidx.webkit:webkit:1.9.0")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Data Persistence (Lightweight)
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Icons
implementation("androidx.compose.material:material-icons-extended")
```

---

## 🎨 UI/UX Design System

### Color Palette (Material You)
```kotlin
// Primary colors
Primary: Color(0xFFF25C54)        // Coral Red (like Dodo)
OnPrimary: Color.White
PrimaryContainer: Color(0xFFFFDAD6)

// Secondary
Secondary: Color(0xFF775652)
OnSecondary: Color.White

// Background
Background: Color(0xFF1C1B1F)     // Dark mode default
Surface: Color(0xFF1C1B1F)

// Accent
Tertiary: Color(0xFF715B2E)
```

### Typography
- **Display**: Roboto Bold, 32sp (App name)
- **Headline**: Roboto Medium, 24sp (Screen titles)
- **Title**: Roboto Medium, 20sp (Card titles)
- **Body**: Roboto Regular, 16sp (Content)
- **Label**: Roboto Medium, 14sp (Buttons, captions)

### Animations
- **Page transitions**: Fade + slide, 200ms
- **Button press**: Scale 0.95, 100ms
- **Loading**: Circular progress with brand color
- **Ripple**: Material ripple effect

---

## 🔧 Core Features Architecture

### 1. WebView Configuration
```kotlin
class WebViewConfig {
    companion object {
        // Desktop User Agent for YouTube/Facebook compatibility
        const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        
        // Mobile UA (default)
        const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 10; K) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
        
        fun configure(webView: WebView, settings: TutuSettings) {
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // Desktop mode
                userAgentString = if (settings.desktopMode) DESKTOP_UA else MOBILE_UA
            }
        }
    }
}
```

### 2. Memory Management Strategy
```kotlin
class MemoryManager(private val webView: WebView) {
    
    fun onLowMemory() {
        // Clear non-critical caches
        webView.clearCache(false) // Keep disk cache, clear memory
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    }
    
    fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                webView.clearCache(true) // Clear all cache
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                webView.clearCache(false)
            }
        }
    }
}
```

### 3. State Management
```kotlin
// WebView State
data class WebViewState(
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

// App Settings
data class TutuSettings(
    val desktopMode: Boolean = true,      // Default desktop for compatibility
    val fullscreen: Boolean = false,
    val autoRotate: Boolean = true,
    val blockAds: Boolean = false,        // Future feature
    val lastUrl: String? = null
)
```

---

## 📂 Project Structure

```
tutu/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tutu/browser/
│   │   │   │   ├── TutuApplication.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── datastore/
│   │   │   │   │   │   │   └── SettingsDataStore.kt
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │       └── Bookmark.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── BookmarkRepository.kt
│   │   │   │   │       └── SettingsRepository.kt
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt (Manual DI)
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── WebPage.kt
│   │   │   │   │   └── usecase/
│   │   │   │   │       └── GetBookmarksUseCase.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── BookmarkCard.kt
│   │   │   │   │   │   ├── UrlInputField.kt
│   │   │   │   │   │   ├── WebViewContainer.kt
│   │   │   │   │   │   └── ProgressIndicator.kt
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── WebScreen.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   └── viewmodel/
│   │   │   │   │       ├── HomeViewModel.kt
│   │   │   │   │       ├── WebViewModel.kt
│   │   │   │   │       └── SettingsViewModel.kt
│   │   │   │   └── util/
│   │   │   │       ├── WebViewPool.kt
│   │   │   │       ├── Constants.kt
│   │   │   │       └── Extensions.kt
│   │   │   └── res/
│   │   │       ├── mipmap-xxxhdpi/
│   │   │       ├── values/
│   │   │       │   └── strings.xml
│   │   │       └── xml/
│   │   │           └── network_security_config.xml
│   │   └── androidTest/
│   │   └── test/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🔋 Performance Optimizations

### 1. APK Size Optimization
```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    bundle {
        language {
            enableSplit = false // Include all languages in single APK
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}
```

### 2. Runtime Performance
- **WebView Pool**: Reuse WebView instances (max 2)
- **Lazy Loading**: Compose lazy lists for bookmarks
- **State Hoisting**: Minimize recompositions
- **Coroutines**: All I/O on Dispatchers.IO

### 3. Battery Optimization
- **Smart Cache**: Use LOAD_CACHE_ELSE_NETWORK when offline
- **Pause WebView**: onPause() when app backgrounded
- **Reduce Animations**: Respect system animation settings

---

## 🛡️ Security Considerations

```kotlin
// WebView security config
webView.settings.apply {
    // Disable potentially dangerous features
    allowFileAccess = false
    allowContentAccess = false
    setGeolocationEnabled(false) // Request permission first
}

// Safe browsing
if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
    WebViewCompat.startSafeBrowsing(context) { success ->
        // Safe browsing enabled
    }
}

// SSL handling
webView.webViewClient = object : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        // Show dialog to user, don't auto-proceed
        handler.cancel()
    }
}
```

---

## 📊 App Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   App Launch    │────▶│ Check Last URL  │────▶│  Prompt User    │
│                 │     │  in Preferences │     │ (Yes/No/Cancel) │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                    ┌────────────────────────────────────┼────────────────────────────────────┐
                    │                                    │                                    │
                    ▼                                    ▼                                    ▼
           ┌─────────────────┐                  ┌─────────────────┐                  ┌─────────────────┐
           │  Restore Session│                  │  Home Screen    │                  │   New Session   │
           │  (Load Last URL)│                  │  (Bookmarks)    │                  │  (Clear URL)    │
           └────────┬────────┘                  └────────┬────────┘                  └────────┬────────┘
                    │                                    │                                    │
                    └────────────────────────────────────┼────────────────────────────────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────┐
                                              │  Enter URL /    │
                                              │ Select Bookmark │
                                              └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │   Web Screen    │
                                              │  (WebView Load) │
                                              └─────────────────┘
```

---

## 🎯 Target Specifications

| Metric | Target |
|--------|--------|
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 34 (Android 14) |
| APK Size | < 5 MB |
| Memory Usage | < 100 MB typical |
| Launch Time | < 1 second |
| Supported Sites | YouTube, Facebook, Instagram, Twitter, Google |

---

## 🚀 Future Enhancements (v2+)

1. **Ad Blocker** - Basic hosts-based blocking
2. **Dark Mode Support** - WebView force dark mode
3. **Download Manager** - File downloads with notifications
4. **Multiple Windows** - Tab-like interface
5. **Password Manager** - Basic credential storage
6. **Reader Mode** - Article text extraction

---

## 📝 Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| No Hilt/Dagger | Manual DI | -300KB APK, simpler for this scope |
| No Room | DataStore only | Bookmarks stored as JSON in prefs, simpler |
| Single Activity | Compose Navigation | Smaller APK, modern pattern |
| No Fragments | 100% Compose | Smaller APK, consistent architecture |
| Desktop Mode Default | ON | YouTube/Instagram need this |
| WebView Pool | Max 2 instances | Balance memory vs speed |

---

*Last Updated: March 2026*
*Author: Raman21676*
