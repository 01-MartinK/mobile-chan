package com.mk.mobilechan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector
import com.mk.mobilechan.R

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
    val breadcrumbRes: Int,
    val boardOnly: Boolean = false,
) {
    HOME(R.string.nav_home, Icons.Outlined.GridView, R.string.breadcrumb_boards),
    BOOKMARKS(R.string.nav_bookmarks, Icons.Outlined.BookmarkBorder, R.string.breadcrumb_bookmarks),
    THREADS(R.string.nav_thread, Icons.Outlined.AlternateEmail, R.string.breadcrumb_boards, boardOnly = true),
    CATALOG(R.string.nav_catalog, Icons.Outlined.ViewModule, R.string.breadcrumb_catalog, boardOnly = true);

    companion object {
        fun visibleEntries(insideBoard: Boolean): List<AppDestinations> =
            entries.filter { !it.boardOnly || insideBoard }
    }
}
