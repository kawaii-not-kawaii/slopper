package io.stashapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.designsystem.theme.StashTheme
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.ui.nav.MainBottomBar
import io.stashapp.android.core.ui.nav.MoreSheet
import io.stashapp.android.core.ui.nav.NavCustomizeSheet
import io.stashapp.android.core.ui.nav.Routes
import io.stashapp.android.feature.browse.BrowseScreen
import io.stashapp.android.feature.connection.ConnectionScreen
import io.stashapp.android.feature.detail.DetailScreen
import io.stashapp.android.feature.home.HomeScreen
import io.stashapp.android.feature.library.LibraryScreen
import io.stashapp.android.feature.player.PlayerScreen
import io.stashapp.android.feature.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        connectionRepository: ConnectionRepository,
        val uiPreferences: UiPreferences,
    ) : ViewModel() {
        private val _start = MutableStateFlow<String?>(null)
        val start: StateFlow<String?> = _start.asStateFlow()

        init {
            viewModelScope.launch {
                connectionRepository.activeServer().collectLatest { server ->
                    _start.value = if (server == null) Routes.Connection else Routes.Home
                }
            }
        }

        fun saveVisibleNavIds(ids: List<String>) {
            viewModelScope.launch { uiPreferences.setBottomNavVisibleIds(ids) }
        }
    }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Pattern A (per D-05): flipped to true once RootViewModel.start emits non-null.
    // Lives on the Activity instance so the SplashScreen keep-condition lambda
    // can read it without touching the Hilt graph before super.onCreate.
    private val appReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen MUST be called BEFORE super.onCreate per the
        // SplashScreen migration guide. The library swaps the activity's theme
        // back to postSplashScreenTheme (Theme.Stash) at this point.
        val splashScreen: SplashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !appReady.get() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        setContent {
            StashTheme {
                val rootViewModel: RootViewModel = hiltViewModel()
                StashAppContent(rootViewModel = rootViewModel, appReady = appReady)
            }
        }
    }

    /**
     * Opt in to the display's highest available refresh rate. Samsung/Pixel
     * devices with 120 Hz panels default to 60 Hz unless the window requests
     * otherwise — this is the main reason Compose animations feel jittery on
     * those devices out of the box.
     *
     * On Android 11+ we pick the display mode with the highest refresh rate
     * that shares our resolution. On Android 14+ we also set
     * `frameRateBoostOnTouchEnabled` so touch interactions get the full
     * refresh rate even on apps whose content is otherwise detected as
     * low-motion.
     */
    private fun requestHighestRefreshRate() {
        val window = window ?: return
        val display =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

        val supportedModes = display.supportedModes
        val currentMode = display.mode
        val bestMode =
            supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }.maxByOrNull { it.refreshRate }
                ?: currentMode

        val lp = window.attributes
        lp.preferredDisplayModeId = bestMode.modeId
        // preferredRefreshRate is a softer hint; pair with modeId for maximum
        // compatibility across older OEM variants.
        lp.preferredRefreshRate = bestMode.refreshRate
        window.attributes = lp

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            runCatching { window.setFrameRateBoostOnTouchEnabled(true) }
        }
    }
}

/** Routes whose chrome includes the bottom navigation bar. Others (player,
 *  scene detail, connection) are full-bleed and hide it. */
private fun isMainTabRoute(route: String?): Boolean =
    when {
        route == null -> false
        route == Routes.Home -> true
        route.startsWith("library") -> true
        route.startsWith("browse/") -> true
        route == Routes.Settings -> true
        else -> false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StashAppContent(
    rootViewModel: RootViewModel,
    appReady: AtomicBoolean,
) {
    val navController = rememberNavController()
    val start by rootViewModel.start.collectAsState()
    // Pattern A gate-flip (per CONTEXT.md D-05 + REVIEWS SHOULD FIX #2 Case B):
    // reuse the existing `start` collection rather than introducing a duplicate.
    // Keyed on COLLECTED VALUE (String?), NOT the StateFlow object, to defeat
    // strong-skipping memoization per RESEARCH §E7.
    LaunchedEffect(start) {
        if (start != null) appReady.set(true)
    }
    // Safety timeout (per RESEARCH §A4 / §E3): even if the StateFlow ever fails
    // to emit, dismiss splash after 3s to avoid ANR. In practice the
    // DataStore-backed flow emits in <100ms. Intentional SPEC deviation —
    // documented in commit body.
    LaunchedEffect(Unit) {
        delay(3000)
        appReady.set(true)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        val startDestination = start
        if (startDestination == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoutePattern = backStack?.destination?.route
            // Build the resolved route (e.g. "browse/studios" not "browse/{kind}")
            // so the bottom bar can highlight the correct tab.
            val currentRoute =
                backStack?.let { entry ->
                    var resolved = entry.destination.route ?: ""
                    entry.arguments?.let { args ->
                        for (key in args.keySet()) {
                            val value = args.getString(key) ?: continue
                            resolved = resolved.replace("{$key}", value)
                        }
                    }
                    resolved
                }
            val showBottomBar = isMainTabRoute(currentRoutePattern)

            val visibleIds by rootViewModel.uiPreferences.bottomNavVisibleIds
                .collectAsState(initial = UiPreferences.DefaultVisible)

            var showMoreSheet by remember { mutableStateOf(false) }
            var showCustomizeSheet by remember { mutableStateOf(false) }
            val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val customizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        MainBottomBar(
                            currentRoute = currentRoute,
                            visibleIds = visibleIds,
                            onNavigate = { route ->
                                navController.tabNavigate(route, startDestination)
                            },
                            onOpenMore = { showMoreSheet = true },
                        )
                    }
                },
                // Let the nav host's routes decide their own edge-to-edge behavior.
                // For main routes, we pad by `inner`; for full-bleed routes
                // (player), we ignore it.
            ) { inner ->
                // Main screens own their own TopAppBar (which handles status bar
                // insets via WindowInsets.systemBars). Re-applying `inner`'s top
                // padding here would double it, creating a blank strip above
                // each screen's app bar — so we only pass the bottom padding
                // (for the nav bar) through to the nav host.
                val bottomOnly =
                    PaddingValues(
                        top = 0.dp,
                        bottom = if (showBottomBar) inner.calculateBottomPadding() else 0.dp,
                    )
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    contentPadding = bottomOnly,
                )
            }

            if (showMoreSheet) {
                MoreSheet(
                    sheetState = moreSheetState,
                    visibleIds = visibleIds,
                    onNavigate = { route ->
                        navController.tabNavigate(route, startDestination)
                    },
                    onOpenSettings = {
                        navController.tabNavigate(Routes.Settings, startDestination)
                    },
                    onCustomize = { showCustomizeSheet = true },
                    onDismiss = { showMoreSheet = false },
                )
            }

            if (showCustomizeSheet) {
                NavCustomizeSheet(
                    sheetState = customizeSheetState,
                    visibleIds = visibleIds,
                    onApply = { rootViewModel.saveVisibleNavIds(it) },
                    onDismiss = { showCustomizeSheet = false },
                )
            }
        }
    }
}

/** Switch tabs without piling up back-stack entries.
 *
 *  NOTE: `launchSingleTop` is deliberately omitted. The Browse routes
 *  (performers / studios / tags) all resolve to the same `browse/{kind}`
 *  pattern. Navigation considers pattern-level identity for singleTop, so
 *  switching from `browse/studios` to `browse/performers` was silently
 *  swallowed. `popUpTo + saveState` already prevents back-stack pile-up,
 *  so dropping singleTop has no downside. */
private fun NavHostController.tabNavigate(
    route: String,
    startDestination: String,
) {
    navigate(route) {
        // popUpTo prevents back-stack pile-up when switching between tabs.
        // saveState is intentionally OFF — the Browse routes share a single
        // `browse/{kind}` pattern, so saving/restoring state would restore
        // the wrong kind's data when switching between Studios ↔ People ↔ Tags.
        // Trade-off: tab scroll position is lost on switch. Acceptable since
        // the lists are network-backed and load fast.
        popUpTo(graph.findStartDestination().id) { inclusive = false }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
    ) {
        composable(Routes.Connection) {
            ConnectionScreen(
                onConnected = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Connection) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                onSceneClick = { sceneId ->
                    navController.navigate(Routes.sceneDetail(sceneId))
                },
                onPlayQueue = { ids, idx ->
                    val start = ids.getOrNull(idx) ?: return@HomeScreen
                    navController.navigate(Routes.player(start, ids, idx))
                },
                onSettingsClick = { navController.navigate(Routes.Settings) },
            )
        }

        composable(
            Routes.LibraryPattern,
            arguments =
                listOf(
                    navArgument("preset") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) {
            LibraryScreen(
                onSceneClick = { sceneId, _, _ ->
                    navController.navigate(Routes.sceneDetail(sceneId))
                },
                onPlayQueue = { ids, idx ->
                    val start = ids.getOrNull(idx) ?: return@LibraryScreen
                    navController.navigate(Routes.player(start, ids, idx))
                },
                onSettingsClick = { navController.navigate(Routes.Settings) },
            )
        }

        composable(
            Routes.BrowsePattern,
            arguments = listOf(navArgument("kind") { type = NavType.StringType }),
        ) {
            BrowseScreen(
                onBack = { navController.popBackStack() },
                onPerformerClick = { id ->
                    android.util.Log.i("StashNav", "performer click id=$id → ${Routes.libraryWithPreset("performer:$id")}")
                    navController.navigate(Routes.libraryWithPreset("performer:$id"))
                },
                onStudioClick = { id ->
                    android.util.Log.i("StashNav", "studio click id=$id → ${Routes.libraryWithPreset("studio:$id")}")
                    navController.navigate(Routes.libraryWithPreset("studio:$id"))
                },
                onTagClick = { id ->
                    android.util.Log.i("StashNav", "tag click id=$id → ${Routes.libraryWithPreset("tag:$id")}")
                    navController.navigate(Routes.libraryWithPreset("tag:$id"))
                },
            )
        }

        composable(
            Routes.DetailPattern,
            arguments = listOf(navArgument("sceneId") { type = NavType.StringType }),
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onPlay = { sceneId, startSeconds ->
                    navController.navigate(Routes.player(sceneId, startSeconds = startSeconds))
                },
            )
        }

        composable(
            Routes.PlayerPattern,
            arguments =
                listOf(
                    navArgument("sceneId") { type = NavType.StringType },
                    navArgument("queueIds") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("index") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("startMs") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) {
            PlayerScreen(onExit = { navController.popBackStack() })
        }

        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDisconnected = {
                    navController.navigate(Routes.Connection) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBrowsePerformers = { navController.navigate(Routes.browse("performers")) },
                onBrowseStudios = { navController.navigate(Routes.browse("studios")) },
                onBrowseTags = { navController.navigate(Routes.browse("tags")) },
            )
        }
    }
}
