package com.tuck.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
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

    data object Inbox : BottomNavScreen(
        route = "inbox",
        titleRes = R.string.nav_inbox,
        selectedIcon = Icons.Filled.Inbox,
        unselectedIcon = Icons.Outlined.Inbox
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
    const val INBOX = "inbox"
    const val COLLECTIONS = "collections"
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
    fun categoryDetail(collectionId: Long, name: String) = "category_detail/$collectionId/${android.net.Uri.encode(name)}"
}
