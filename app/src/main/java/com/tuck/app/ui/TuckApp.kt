package com.tuck.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tuck.app.ui.components.FloatingNavBar
import com.tuck.app.ui.components.FloatingNavBarSpace
import com.tuck.app.ui.components.rememberNavBarVisibility
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuck.app.ui.collections.CollectionsScreen
import com.tuck.app.ui.detail.ItemDetailScreen
import com.tuck.app.ui.favorites.FavoritesScreen
import com.tuck.app.ui.home.HomeScreen
import com.tuck.app.ui.travel.TravelScreen
import com.tuck.app.ui.navigation.BottomNavScreen
import com.tuck.app.ui.navigation.NavRoutes
import com.tuck.app.ui.search.SearchScreen
import com.tuck.app.ui.settings.SettingsScreen
import com.tuck.app.ui.settings.SettingsViewModel
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.trash.TrashScreen

@Composable
fun TuckApp(
    initialOpenItemId: Long? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(initialOpenItemId) {
        if (initialOpenItemId != null && initialOpenItemId > 0) {
            navController.navigate(NavRoutes.detail(initialOpenItemId))
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Optional arguments belong to a destination, not its identity in the dock.
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

    var captureSignal by remember { mutableIntStateOf(0) }

    val bottomNavScreens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Collections,
        BottomNavScreen.Travel,
        BottomNavScreen.Search
    )

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    TuckTheme(
        themeSetting = settingsState.settings.theme,
        themeFlavor = settingsState.settings.themeFlavor
    ) {
        val tuckColors = TuckTheme.colors

        val navBar = rememberNavBarVisibility()
        // A destination change is not a scroll, so the bar would otherwise stay hidden after
        // navigating away from a list the reader had scrolled down.
        LaunchedEffect(currentRoute) { navBar.reveal() }

        val barShowing = showBottomBar && navBar.visible

        /**
         * Clearance for the floating bar, given back to the content while the bar is away.
         *
         * Holding the space permanently kept the layout still, but it meant a band of nothing
         * sat at the bottom of every screen for as long as the bar was hidden — which is most
         * of the time someone is reading a list. Animating it instead keeps the layout calm
         * without the dead area, so long as it moves with the bar rather than snapping: the
         * durations here are the bar's own, so the content grows into the space at exactly the
         * rate the bar vacates it.
         */
        val navBarReserve by animateDpAsState(
            targetValue = if (barShowing) FloatingNavBarSpace else 0.dp,
            animationSpec = tween(durationMillis = if (barShowing) 220 else 180),
            label = "navBarReserve"
        )

        Scaffold(
            containerColor = tuckColors.background,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(navBar.connection)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + navBarReserve
                )
            ) {
                composable(NavRoutes.HOME) {
                    HomeScreen(
                        captureSignal = captureSignal,
                        onNavigateToCollections = { collectionId ->
                            val route = collectionId
                                ?.let { NavRoutes.collection(it) }
                                ?: NavRoutes.COLLECTIONS
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME)
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSearch = {
                            navController.navigate(NavRoutes.SEARCH) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToDetail = { itemId ->
                            navController.navigate(NavRoutes.detail(itemId))
                        },
                        onNavigateToSettings = {
                            navController.navigate(NavRoutes.SETTINGS)
                        }
                    )
                }

                composable(NavRoutes.TRAVEL) {
                    TravelScreen()
                }

                composable(
                    route = NavRoutes.COLLECTIONS_WITH_ARG,
                    arguments = listOf(
                        navArgument(NavRoutes.ARG_COLLECTION_ID) {
                            type = NavType.LongType
                            defaultValue = NavRoutes.NO_COLLECTION
                        }
                    )
                ) { backStackEntry ->
                    val requested = backStackEntry.arguments
                        ?.getLong(NavRoutes.ARG_COLLECTION_ID)
                        ?.takeIf { it != NavRoutes.NO_COLLECTION }
                    CollectionsScreen(
                        initialCollectionId = requested,
                        onNavigateToDetail = { itemId ->
                            navController.navigate(NavRoutes.detail(itemId))
                        }
                    )
                }

                composable(NavRoutes.SEARCH) {
                    SearchScreen(
                        onNavigateToDetail = { itemId ->
                            navController.navigate(NavRoutes.detail(itemId))
                        }
                    )
                }

                composable(NavRoutes.FAVORITES) {
                    FavoritesScreen(
                        onNavigateToDetail = { itemId ->
                            navController.navigate(NavRoutes.detail(itemId))
                        }
                    )
                }

                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        onNavigateToFilingRules = {
                            navController.navigate(NavRoutes.FILING_RULES)
                        },
                        onNavigateToVaultHealth = {
                            navController.navigate(NavRoutes.VAULT_HEALTH)
                        },
                        onNavigateToTrash = {
                            navController.navigate(NavRoutes.TRASH)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = NavRoutes.DETAIL,
                    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                ) {
                    ItemDetailScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.VAULT_HEALTH) {
                    com.tuck.app.ui.health.VaultHealthScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.FILING_RULES) {
                    com.tuck.app.ui.rules.FilingRulesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.TRASH) {
                    TrashScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            FloatingNavBar(
                screens = bottomNavScreens,
                currentRoute = currentRoute,
                visible = showBottomBar && navBar.visible,
                onSelect = { screen ->
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onCapture = {
                    if (currentRoute != NavRoutes.HOME) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    captureSignal++
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                        .padding(bottom = paddingValues.calculateBottomPadding())
                )
            }
        }
    }
}
