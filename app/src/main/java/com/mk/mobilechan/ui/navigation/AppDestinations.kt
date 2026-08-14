package com.mk.mobilechan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector
import com.mk.mobilechan.R

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
    val breadcrumbRes: Int,
    val boardOnly: Boolean = false,
) {
    BOARDS(R.string.nav_boards, Icons.Outlined.GridView, R.string.breadcrumb_boards),
    CATALOG(R.string.nav_catalog, Icons.Outlined.ViewModule, R.string.breadcrumb_catalog, boardOnly = true),
    SEARCH(R.string.nav_search, Icons.Outlined.Search, R.string.breadcrumb_search);

    companion object {
        fun visibleEntries(insideBoard: Boolean): List<AppDestinations> =
            entries.filter { !it.boardOnly || insideBoard }
    }
}
