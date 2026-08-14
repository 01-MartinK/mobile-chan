package com.mk.mobilechan.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun BottomNavBar(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit,
    insideBoard: Boolean = false,
    content: @Composable () -> Unit,
) {
    val navItemColors = bottomNavItemColors()

    NavigationSuiteScaffold(
        navigationItems = {
            AppDestinations.visibleEntries(insideBoard).forEach { destination ->
                NavigationSuiteItem(
                    selected = destination == currentDestination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    colors = navItemColors,
                )
            }
        },
        content = content,
    )
}

@Composable
private fun bottomNavItemColors(): NavigationItemColors {
    val scheme = MaterialTheme.colorScheme
    return ShortNavigationBarItemDefaults.colors(
        selectedIconColor = scheme.onPrimary,
        selectedTextColor = scheme.onPrimary,
        selectedIndicatorColor = scheme.primary,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
}
