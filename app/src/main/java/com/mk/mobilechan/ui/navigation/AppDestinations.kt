package com.mk.mobilechan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.mk.mobilechan.R

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
    val breadcrumbRes: Int,
) {
    BOARDS(R.string.nav_boards, Icons.Outlined.GridView, R.string.breadcrumb_boards),
    SEARCH(R.string.nav_search, Icons.Outlined.Search, R.string.breadcrumb_search),
    ACTIVITY(R.string.nav_activity, Icons.Outlined.Notifications, R.string.breadcrumb_activity),
    PROFILE(R.string.nav_profile, Icons.Outlined.Person, R.string.breadcrumb_profile),
}
