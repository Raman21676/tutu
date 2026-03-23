package com.tutu.browser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tutu.browser.di.AppModule
import com.tutu.browser.service.BackgroundPlayService
import com.tutu.browser.service.FloatingWindowService
import com.tutu.browser.ui.screens.HomeScreen
import com.tutu.browser.util.WebViewHolder
import com.tutu.browser.ui.screens.SettingsScreen
import com.tutu.browser.ui.screens.WebScreen
import com.tutu.browser.ui.theme.TutuTheme
import com.tutu.browser.ui.viewmodel.HomeViewModel
import com.tutu.browser.ui.viewmodel.SettingsViewModel
import com.tutu.browser.ui.viewmodel.ThemeViewModel
import com.tutu.browser.ui.viewmodel.WebViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
    
    private lateinit var settingsRepository: com.tutu.browser.data.repository.SettingsRepository
    private lateinit var bookmarkRepository: com.tutu.browser.data.repository.BookmarkRepository
    
    // Track current webview state for background services
    private var currentUrl: String = ""
    private var currentTitle: String = ""
    private var isInWebScreen: Boolean = false
    
    // Permission launcher for overlay (Android 11+)
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission granted. You can now use Floating Window.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Overlay permission denied. Floating Window will not work.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Initialize repositories
        settingsRepository = AppModule.provideSettingsRepository(this)
        bookmarkRepository = AppModule.provideBookmarkRepository(this)
        
        // Get URL from intent (if opened via external link or notification)
        val intentUrl = intent?.data?.toString() 
            ?: intent?.getStringExtra("restore_url")
        
        Log.d(TAG, "onCreate - intentUrl: $intentUrl")
        
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.Factory(settingsRepository)
            )
            val themeState by themeViewModel.themeState.collectAsState()
            
            val darkTheme = when {
                themeState.followSystemTheme -> isSystemInDarkTheme()
                else -> themeState.darkMode
            }
            
            TutuTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TutuNavigation(
                        settingsRepository = settingsRepository,
                        bookmarkRepository = bookmarkRepository,
                        initialUrl = intentUrl,
                        onWebViewStateChanged = { url, title ->
                            currentUrl = url
                            currentTitle = title
                        },
                        onScreenChanged = { screen ->
                            val webRoutePrefix = Screen.Web.route.substringBefore("/")
                            isInWebScreen = screen.startsWith(webRoutePrefix)
                            Log.d(TAG, "Screen changed: $screen, isInWebScreen: $isInWebScreen")
                        }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: ${intent?.getStringExtra("restore_url")}")
    }
    
    override fun onPause() {
        super.onPause()
        
        // Stop timestamp tracking when leaving app
        WebViewHolder.stopTimestampTracking()
        
        if (WebViewHolder.backgroundPlayEnabled) {
            // postDelayed is REQUIRED. Android's internal WebViewChromium.pauseTimers()
            // is dispatched ~10-30ms AFTER super.onPause() returns.
            // Calling onResume() synchronously fires before that, so Android pauses it again.
            // 50ms delay ensures all internal pause machinery is done before we resume.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                WebViewHolder.webView?.onResume()
                WebViewHolder.webView?.resumeTimers()
                android.util.Log.d("MainActivity", "WebView resume fired after delay")
            }, 50)
            startBackgroundPlayService(currentUrl, currentTitle)
        } else if (WebViewHolder.floatingWindowEnabled && isInWebScreen) {
            // Use the periodically-saved timestamp
            val holderUrl = WebViewHolder.currentUrl
            val url = if (holderUrl.isNotBlank()) holderUrl else currentUrl
            val timestamp = WebViewHolder.currentTimestamp
            Log.d(TAG, "onPause: Starting floating window with timestamp=$timestamp, url=$url")
            startFloatingWindowService(url, currentTitle)
        } else {
            Log.d(TAG, "onPause: Not starting floating window. floatingEnabled: ${WebViewHolder.floatingWindowEnabled}, isInWebScreen: $isInWebScreen")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - timestamp to restore: ${WebViewHolder.currentTimestamp}")
        
        // Stop background services when app comes to foreground
        stopBackgroundServices()
        
        // Restore video timestamp if we have one (from floating window return)
        if (WebViewHolder.currentTimestamp > 0) {
            val timestamp = WebViewHolder.currentTimestamp
            WebViewHolder.webView?.evaluateJavascript(
                "(function() { var v = document.querySelector('video'); if (v) { v.currentTime = $timestamp; return 'set to $timestamp'; } return 'no video'; })();"
            ) { result ->
                Log.d(TAG, "onResume: Restored timestamp result: $result")
            }
            // Clear timestamp after restoring
            WebViewHolder.currentTimestamp = 0f
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
    
    private fun startBackgroundPlayService(url: String, title: String) {
        Log.d(TAG, "Starting BackgroundPlayService")
        val intent = Intent(this, BackgroundPlayService::class.java).apply {
            action = BackgroundPlayService.ACTION_START
            putExtra("url", url)
            putExtra("title", title)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun startFloatingWindowService(url: String, title: String) {
        Log.d(TAG, "Starting FloatingWindowService with url: $url, timestamp: ${WebViewHolder.currentTimestamp}")
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            putExtra(FloatingWindowService.EXTRA_URL, url)
            putExtra(FloatingWindowService.EXTRA_TIMESTAMP, WebViewHolder.currentTimestamp)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopBackgroundServices() {
        Log.d(TAG, "Stopping background services")
        
        // Stop background playback service
        if (BackgroundPlayService.isRunning) {
            val intent = Intent(this, BackgroundPlayService::class.java).apply {
                action = BackgroundPlayService.ACTION_STOP
            }
            startService(intent)
        }
        
        // Stop floating window service
        stopService(Intent(this, FloatingWindowService::class.java))
    }
    
    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Request overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                return false
            }
        }
        return true
    }
}

@Composable
private fun TutuNavigation(
    settingsRepository: com.tutu.browser.data.repository.SettingsRepository,
    bookmarkRepository: com.tutu.browser.data.repository.BookmarkRepository,
    initialUrl: String? = null,
    onWebViewStateChanged: (String, String) -> Unit = { _, _ -> },
    onScreenChanged: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Track current screen
    LaunchedEffect(navController.currentBackStackEntryFlow) {
        navController.currentBackStackEntryFlow.collect { entry ->
            onScreenChanged(entry.destination.route ?: "")
        }
    }
    
    // Navigate to initial URL if provided
    LaunchedEffect(initialUrl) {
        initialUrl?.let { url ->
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            navController.navigate(Screen.Web.createRoute(encodedUrl))
        }
    }
    
    // DISABLED: Auto-restore last URL was causing issues
    // Users can manually navigate to their desired website from Home screen
    // LaunchedEffect(Unit) {
    //     settingsRepository.settings.collect { settings ->
    //         val savedUrl = settings.lastUrl
    //         if (!savedUrl.isNullOrEmpty() && initialUrl == null) {
    //             val encoded = URLEncoder.encode(savedUrl, StandardCharsets.UTF_8.toString())
    //             navController.navigate(Screen.Web.createRoute(encoded)) {
    //                 launchSingleTop = true
    //             }
    //         }
    //         return@collect
    //     }
    // }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = tween(200)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(200)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(200)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(200)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(200)
            )
        }
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    bookmarkRepository = bookmarkRepository,
                    settingsRepository = settingsRepository
                )
            )
            
            HomeScreen(
                viewModel = viewModel,
                onNavigateToWeb = { url ->
                    // URL is already processed by buildUrl in HomeScreen
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Web.createRoute(encodedUrl))
                }
            )
        }
        
        composable(
            route = Screen.Web.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: "https://www.google.com"
            val decodedUrl = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.toString())
            
            val viewModel: WebViewModel = viewModel(
                factory = WebViewModel.Factory(
                    settingsRepository = settingsRepository,
                    initialUrl = decodedUrl
                )
            )
            
            // Track webview state
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.url, state.title) {
                onWebViewStateChanged(state.url, state.title)
            }
            
            // Get settings
            val settings by settingsRepository.settings.collectAsState(initial = null)
            val backgroundPlayback = settings?.backgroundPlayback ?: false
            val floatingWindow = settings?.floatingWindow ?: false
            
            WebScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                backgroundPlayEnabled = backgroundPlayback,
                floatingWindowEnabled = floatingWindow
            )
        }
        
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    settingsRepository = settingsRepository,
                    bookmarkRepository = bookmarkRepository
                )
            )
            
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Navigation routes
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Web : Screen("web/{url}") {
        fun createRoute(url: String) = "web/$url"
    }
    data object Settings : Screen("settings")
}
