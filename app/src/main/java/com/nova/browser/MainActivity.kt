package com.nova.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nova.browser.data.repository.TabManager
import com.nova.browser.di.AppModule
import com.nova.browser.domain.repository.HistoryRepository
import com.nova.browser.domain.repository.DownloadRepository
import com.nova.browser.domain.repository.SiteSettingsRepository
import com.nova.browser.domain.repository.UserScriptRepository
import com.nova.browser.ui.screens.BookmarksScreen
import com.nova.browser.ui.screens.DownloadsScreen
import com.nova.browser.ui.screens.HistoryScreen
import com.nova.browser.ui.screens.HomeScreen
import com.nova.browser.ui.screens.IncognitoWebScreen
import com.nova.browser.ui.screens.CustomSearchEnginesScreen
import com.nova.browser.ui.screens.NetworkLogScreen
import com.nova.browser.ui.screens.SettingsScreen
import com.nova.browser.ui.screens.TabsScreen
import com.nova.browser.ui.screens.UserScriptsScreen
import com.nova.browser.ui.screens.WebScreen
import com.nova.browser.ui.theme.NovaTheme
import com.nova.browser.ui.viewmodel.HomeViewModel
import com.nova.browser.ui.viewmodel.SettingsViewModel
import com.nova.browser.ui.viewmodel.ThemeViewModel
import com.nova.browser.ui.viewmodel.WebViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_PERMISSION = "nova_permission_prefs"
        private const val KEY_MANAGE_STORAGE_SHOWN = "manage_storage_shown"
    }

    /**
     * On Android 11+ (API 30+), redirect the user to system settings to grant
     * "All Files Access" permission. This is required for writing to arbitrary
     * public directories like /sdcard/Download/Nova Downloads/.
     */
    private fun requestAllFilesAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val prefs = getSharedPreferences(PREFS_PERMISSION, MODE_PRIVATE)
                val alreadyShown = prefs.getBoolean(KEY_MANAGE_STORAGE_SHOWN, false)
                if (!alreadyShown) {
                    prefs.edit().putBoolean(KEY_MANAGE_STORAGE_SHOWN, true).apply()
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not open MANAGE_EXTERNAL_STORAGE settings", e)
                    }
                }
            }
        }
    }

    @Inject
    lateinit var historyRepository: HistoryRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var siteSettingsRepository: com.nova.browser.domain.repository.SiteSettingsRepository

    @Inject
    lateinit var userScriptRepository: com.nova.browser.domain.repository.UserScriptRepository

    private lateinit var settingsRepository: com.nova.browser.data.repository.SettingsRepository
    private lateinit var bookmarkRepository: com.nova.browser.data.repository.BookmarkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Initialize repositories
        settingsRepository = AppModule.provideSettingsRepository(this)
        bookmarkRepository = AppModule.provideBookmarkRepository(this)

        // Request MANAGE_EXTERNAL_STORAGE on Android 11+ if not already granted
        requestAllFilesAccessIfNeeded()

        // Get URL from intent (if opened via external link)
        val intentUrl = intent?.data?.toString()

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

            NovaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TutuNavigation(
                        settingsRepository = settingsRepository,
                        bookmarkRepository = bookmarkRepository,
                        historyRepository = historyRepository,
                        downloadRepository = downloadRepository,
                        siteSettingsRepository = siteSettingsRepository,
                        userScriptRepository = userScriptRepository,
                        initialUrl = intentUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun TutuNavigation(
    settingsRepository: com.nova.browser.data.repository.SettingsRepository,
    bookmarkRepository: com.nova.browser.data.repository.BookmarkRepository,
    historyRepository: HistoryRepository,
    downloadRepository: DownloadRepository,
    siteSettingsRepository: SiteSettingsRepository,
    userScriptRepository: UserScriptRepository,
    initialUrl: String? = null
) {
    val navController = rememberNavController()
    val tabManager = TabManager.getInstance()

    // Navigate to initial URL if provided
    LaunchedEffect(initialUrl) {
        initialUrl?.let { url ->
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            navController.navigate(Screen.Web.createRoute(encodedUrl))
        }
    }

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
                    // Add a new tab when navigating from home
                    tabManager.addTab(url)
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Web.createRoute(encodedUrl))
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                },
                onNavigateToTabs = {
                    navController.navigate(Screen.Tabs.route)
                },
                onNavigateToIncognito = { url ->
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Incognito.createRoute(encodedUrl))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
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

            WebScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onNavigateToTabs = {
                    navController.navigate(Screen.Tabs.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                historyRepository = historyRepository,
                downloadRepository = downloadRepository
            )
        }

        composable(Screen.Tabs.route) {
            TabsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeb = { url ->
                    if (url.isNotEmpty()) {
                        // Existing tab with URL — pop Tabs and go to that page
                        navController.popBackStack()
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screen.Web.createRoute(encodedUrl))
                    } else {
                        // New/empty tab — go to Home so user can type a URL fresh
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                }
            )
        }


        composable(Screen.Bookmarks.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            BookmarksScreen(
                bookmarksFlow = bookmarkRepository.bookmarks,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeb = { url ->
                    navController.popBackStack()
                    tabManager.addTab(url)
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Web.createRoute(encodedUrl))
                },
                onAddBookmark = { title, url ->
                    scope.launch { bookmarkRepository.addBookmark(title, url) }
                },
                onDeleteBookmark = { id ->
                    scope.launch { bookmarkRepository.removeBookmark(id) }
                },
                onUpdateBookmark = { bookmark ->
                    scope.launch { bookmarkRepository.updateBookmark(bookmark) }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeb = { url ->
                    navController.popBackStack()
                    tabManager.addTab(url)
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Web.createRoute(encodedUrl))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    settingsRepository = settingsRepository,
                    bookmarkRepository = bookmarkRepository,
                    historyRepository = historyRepository,
                    siteSettingsRepository = siteSettingsRepository,
                    userScriptRepository = userScriptRepository,
                    context = androidx.compose.ui.platform.LocalContext.current
                )
            )

            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserScripts = { navController.navigate(Screen.UserScripts.route) },
                onNavigateToNetworkLog = { navController.navigate(Screen.NetworkLog.route) },
                onNavigateToCustomSearchEngines = { navController.navigate(Screen.CustomSearchEngines.route) }
            )
        }
        
        composable(Screen.CustomSearchEngines.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    settingsRepository = settingsRepository,
                    bookmarkRepository = bookmarkRepository,
                    historyRepository = historyRepository,
                    siteSettingsRepository = siteSettingsRepository,
                    userScriptRepository = userScriptRepository,
                    context = androidx.compose.ui.platform.LocalContext.current
                )
            )
            CustomSearchEnginesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.UserScripts.route) {
            UserScriptsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.NetworkLog.route) {
            NetworkLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Incognito.route,
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

            IncognitoWebScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                downloadRepository = downloadRepository
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
    data object Tabs : Screen("tabs")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
    data object UserScripts : Screen("user_scripts")
    data object NetworkLog : Screen("network_log")
    data object CustomSearchEngines : Screen("custom_search_engines")
    data object Incognito : Screen("incognito/{url}") {
        fun createRoute(url: String) = "incognito/$url"
    }
}
