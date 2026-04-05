package com.nova.browser

import android.os.Bundle
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
import com.nova.browser.ui.screens.BookmarksScreen
import com.nova.browser.ui.screens.DownloadsScreen
import com.nova.browser.ui.screens.HistoryScreen
import com.nova.browser.ui.screens.HomeScreen
import com.nova.browser.ui.screens.SettingsScreen
import com.nova.browser.ui.screens.TabsScreen
import com.nova.browser.ui.screens.WebScreen
import com.nova.browser.ui.theme.TutuTheme
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
    }

    @Inject
    lateinit var historyRepository: HistoryRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    private lateinit var settingsRepository: com.nova.browser.data.repository.SettingsRepository
    private lateinit var bookmarkRepository: com.nova.browser.data.repository.BookmarkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Initialize repositories
        settingsRepository = AppModule.provideSettingsRepository(this)
        bookmarkRepository = AppModule.provideBookmarkRepository(this)

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

            TutuTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TutuNavigation(
                        settingsRepository = settingsRepository,
                        bookmarkRepository = bookmarkRepository,
                        historyRepository = historyRepository,
                        downloadRepository = downloadRepository,
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
                historyRepository = historyRepository,
                downloadRepository = downloadRepository
            )
        }

        composable(Screen.Tabs.route) {
            TabsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeb = { url ->
                    navController.popBackStack()
                    if (url.isNotEmpty()) {
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screen.Web.createRoute(encodedUrl))
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
    data object Tabs : Screen("tabs")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
}
