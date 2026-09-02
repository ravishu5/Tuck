package com.tuck.app.ui

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
import com.tuck.app.ui.inbox.InboxScreen
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
    val currentRoute = navBackStackEntry?.destination?.route

    var captureSignal by remember { mutableIntStateOf(0) }

    val bottomNavScreens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Collections,
        BottomNavScreen.Inbox,
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
                    // Reserved whether or not the bar is showing, so nothing reflows under the
                    // reader's thumb when it slides away.
                    bottom = paddingValues.calculateBottomPadding() +
                        if (showBottomBar) FloatingNavBarSpace else 0.dp
                )
            ) {
                composable(NavRoutes.HOME) {
                    HomeScreen(
                        captureSignal = captureSignal,
                        onNavigateToCollections = {
                            navController.navigate(NavRoutes.COLLECTIONS) {
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

                composable(NavRoutes.INBOX) {
                    InboxScreen(
                        onNavigateToDetail = { itemId ->
                            navController.navigate(NavRoutes.detail(itemId))
                        }
                    )
                }

                composable(NavRoutes.COLLECTIONS) {
                    CollectionsScreen(
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
