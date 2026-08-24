package com.tuck.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavScreen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Search : BottomNavScreen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    data object Categories : BottomNavScreen(
        route = "categories",
        title = "Categories",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )

    data object Favorites : BottomNavScreen(
        route = "favorites",
        title = "Favorites",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarBorder
    )

    data object Settings : BottomNavScreen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val DETAIL = "detail/{itemId}"
    const val CATEGORY_DETAIL = "category_detail/{collectionId}/{collectionName}"

    fun detail(itemId: Long) = "detail/$itemId"
    fun categoryDetail(collectionId: Long, name: String) = "category_detail/$collectionId/${android.net.Uri.encode(name)}"
}
