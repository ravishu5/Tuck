package com.tuck.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tuck.app.R

sealed class BottomNavScreen(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavScreen(
        route = "home",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Travel : BottomNavScreen(
        route = "travel",
        titleRes = R.string.nav_travel,
        selectedIcon = Icons.Filled.Flight,
        unselectedIcon = Icons.Outlined.Flight
    )

    data object Collections : BottomNavScreen(
        route = "collections",
        titleRes = R.string.nav_collections,
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )

    data object Search : BottomNavScreen(
        route = "search",
        titleRes = R.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
}

object NavRoutes {
    const val HOME = "home"
    const val TRAVEL = "travel"
    const val COLLECTIONS = "collections"
    /**
     * The Collections tab, optionally opened straight onto one collection.
     *
     * The bare [COLLECTIONS] route still matches — the argument defaults to
     * [NO_COLLECTION] — so the bottom bar is unaffected.
     */
    const val COLLECTIONS_WITH_ARG = "collections?collectionId={collectionId}"
    const val ARG_COLLECTION_ID = "collectionId"
    const val NO_COLLECTION = -1L
    const val CATEGORIES = "collections" // Alias for backwards compatibility
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val FILING_RULES = "filing_rules"
    const val VAULT_HEALTH = "vault_health"
    const val DETAIL = "detail/{itemId}"
    const val CATEGORY_DETAIL = "category_detail/{collectionId}/{collectionName}"

    fun detail(itemId: Long) = "detail/$itemId"
    fun collection(collectionId: Long) = "collections?collectionId=$collectionId"
    fun categoryDetail(collectionId: Long, name: String) = "category_detail/$collectionId/${android.net.Uri.encode(name)}"
}
