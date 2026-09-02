package com.tuck.app.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

    val bottomNavScreens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Inbox,
        BottomNavScreen.Collections,
        BottomNavScreen.Search
    )

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    TuckTheme(
        themeSetting = settingsState.settings.theme,
        themeFlavor = settingsState.settings.themeFlavor
    ) {
        val tuckColors = TuckTheme.colors

        Scaffold(
            containerColor = tuckColors.background,
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        tonalElevation = 3.dp,
                        color = tuckColors.surface
                    ) {
                        NavigationBar(
                            containerColor = tuckColors.surface,
                            tonalElevation = 0.dp
                        ) {
                            bottomNavScreens.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
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
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = stringResource(screen.titleRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(screen.titleRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = tuckColors.accent,
                                        selectedTextColor = tuckColors.accent,
                                        unselectedIconColor = tuckColors.textMuted,
                                        unselectedTextColor = tuckColors.textMuted,
                                        indicatorColor = tuckColors.accentContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(NavRoutes.HOME) {
                    HomeScreen(
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
        }
    }
}
