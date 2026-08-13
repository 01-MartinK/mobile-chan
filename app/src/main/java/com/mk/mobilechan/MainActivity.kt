package com.mk.mobilechan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.ui.navigation.AppDestinations
import com.mk.mobilechan.ui.navigation.BottomNavBar
import com.mk.mobilechan.ui.navigation.TopNavBar
import com.mk.mobilechan.ui.theme.MobileChanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logBoards()
        setContent {
            MobileChanTheme {
                MobileChanApp()
            }
        }
    }

    private fun logBoards() {
        lifecycleScope.launch {
            try {
                val boards = FourChanClient.api.getBoards().boards
                Log.d(TAG, "Loaded ${boards.size} boards")
                boards.forEach { board ->
                    Log.d(TAG, "/${board.board}/ - ${board.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load boards", e)
            }
        }
    }

    companion object {
        private const val TAG = "FourChan"
    }
}

@PreviewScreenSizes
@Composable
fun MobileChanApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.BOARDS) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    currentDestination = destination
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        BottomNavBar(
            currentDestination = currentDestination,
            onDestinationSelected = { currentDestination = it },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopNavBar(
                        destination = currentDestination,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSearchClick = { currentDestination = AppDestinations.SEARCH },
                    )
                },
            ) { innerPadding ->
                DestinationPane(
                    destination = currentDestination,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun AppDrawer(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        AppDestinations.entries.forEach { destination ->
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DestinationPane(
    destination: AppDestinations,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MobileChanAppPreview() {
    MobileChanTheme {
        MobileChanApp()
    }
}
