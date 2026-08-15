package com.mk.mobilechan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.ImageLoader
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.ui.boards.BoardsScreen
import com.mk.mobilechan.ui.boards.BoardsUiState
import com.mk.mobilechan.ui.boards.rememberBoardsUiState
import com.mk.mobilechan.ui.catalog.CatalogScreen
import com.mk.mobilechan.ui.navigation.AppDestinations
import com.mk.mobilechan.ui.navigation.AppModules
import com.mk.mobilechan.ui.navigation.BottomNavBar
import com.mk.mobilechan.ui.navigation.TopNavBar
import com.mk.mobilechan.ui.settings.SettingsScreen
import com.mk.mobilechan.ui.settings.UserSettings
import com.mk.mobilechan.ui.settings.rememberUserSettings
import com.mk.mobilechan.ui.theme.MobileChanTheme
import com.mk.mobilechan.ui.threads.ThreadScreen
import com.mk.mobilechan.ui.threads.ThreadsScreen
import com.mk.mobilechan.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(FourChanClient.httpClient)
                .build(),
        )
        enableEdgeToEdge()
        setContent {
            MobileChanApp()
        }
    }
}

@PreviewScreenSizes
@Composable
fun MobileChanApp() {
    val settings = rememberUserSettings()
    MobileChanTheme(theme = settings.theme) {
        if (!settings.welcomeCompleted) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                WelcomeScreen(
                    onFinished = settings::completeWelcome,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        } else {
            MobileChanAppContent(settings = settings)
        }
    }
}

@Composable
private fun MobileChanAppContent(settings: UserSettings) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var activeBoard by rememberSaveable(stateSaver = ActiveBoardSaver) { mutableStateOf<Board?>(null) }
    var threadPage by rememberSaveable { mutableIntStateOf(1) }
    var activeThreadNo by rememberSaveable { mutableStateOf<Long?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val (boardsState, retryBoards) = rememberBoardsUiState()

    val selectBoard: (Board) -> Unit = { board ->
        activeBoard = board
        threadPage = 1
        activeThreadNo = null
        settingsOpen = false
        currentDestination = AppDestinations.THREADS
    }

    BackHandler(enabled = settingsOpen || activeThreadNo != null) {
        if (settingsOpen) {
            settingsOpen = false
        } else {
            activeThreadNo = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    currentDestination = destination
                    scope.launch { drawerState.close() }
                },
                boardsState = boardsState,
                activeBoard = activeBoard,
                enabledModules = settings.enabledModules,
                onRetryBoards = retryBoards,
                onBoardSelected = { board ->
                    selectBoard(board)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        BottomNavBar(
            currentDestination = currentDestination,
            insideBoard = activeBoard != null,
            onDestinationSelected = { destination ->
                settingsOpen = false
                activeThreadNo = null
                currentDestination = destination
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopNavBar(
                        title = topBarTitle(
                            currentDestination,
                            activeBoard,
                            activeThreadNo,
                            settingsOpen,
                        ),
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSettingsClick = if (settingsOpen) null else { { settingsOpen = true } },
                        onBackClick = when {
                            settingsOpen -> { { settingsOpen = false } }
                            activeThreadNo != null -> { { activeThreadNo = null } }
                            else -> null
                        },
                    )
                },
            ) { innerPadding ->
                DestinationPane(
                    destination = currentDestination,
                    boardsState = boardsState,
                    activeBoard = activeBoard,
                    threadPage = threadPage,
                    activeThreadNo = activeThreadNo,
                    settingsOpen = settingsOpen,
                    settings = settings,
                    onRetryBoards = retryBoards,
                    onBoardSelected = selectBoard,
                    onThreadPageChange = { threadPage = it },
                    onThreadSelected = { activeThreadNo = it },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun topBarTitle(
    destination: AppDestinations,
    activeBoard: Board?,
    activeThreadNo: Long?,
    settingsOpen: Boolean,
): String {
    return when {
        settingsOpen -> stringResource(R.string.breadcrumb_settings)
        activeBoard != null && activeThreadNo != null ->
            stringResource(R.string.board_thread, activeBoard.board, activeThreadNo)
        destination == AppDestinations.CATALOG && activeBoard != null ->
            stringResource(R.string.board_catalog, activeBoard.board)
        destination == AppDestinations.THREADS && activeBoard != null ->
            stringResource(R.string.board_item, activeBoard.board, activeBoard.title)
        else -> stringResource(destination.breadcrumbRes)
    }
}

@Composable
private fun AppDrawer(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit,
    boardsState: BoardsUiState,
    activeBoard: Board?,
    enabledModules: Set<AppModules>,
    onRetryBoards: () -> Unit,
    onBoardSelected: (Board) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        val modules = AppModules.entries.filter {
            enabledModules.isEmpty() || it in enabledModules
        }
        modules.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.labelRes)) },
                selected = true,
                onClick = { },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.nav_boards),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        )
        when (boardsState) {
            BoardsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            BoardsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.boards_error),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetryBoards) {
                        Text(stringResource(R.string.boards_retry))
                    }
                }
            }

            is BoardsUiState.Success -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(boardsState.boards, key = { it.board }) { board ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = stringResource(
                                        R.string.board_item,
                                        board.board,
                                        board.title,
                                    ),
                                )
                            },
                            selected = board.board == activeBoard?.board,
                            onClick = { onBoardSelected(board) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationPane(
    destination: AppDestinations,
    boardsState: BoardsUiState,
    activeBoard: Board?,
    threadPage: Int,
    activeThreadNo: Long?,
    onRetryBoards: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onThreadPageChange: (Int) -> Unit,
    onThreadSelected: (Long) -> Unit,
    settingsOpen: Boolean,
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    if (settingsOpen) {
        SettingsScreen(
            allowNsfw = settings.allowNsfw,
            onAllowNsfwChange = settings::updateAllowNsfw,
            theme = settings.theme,
            onThemeChange = settings::updateTheme,
            modifier = modifier,
        )
        return
    }

    if (activeBoard != null && activeThreadNo != null) {
        ThreadScreen(
            board = activeBoard,
            threadNo = activeThreadNo,
            modifier = modifier,
        )
        return
    }

    when (destination) {
        AppDestinations.HOME -> {
            BoardsScreen(
                state = boardsState,
                onRetry = onRetryBoards,
                onBoardSelected = onBoardSelected,
                modifier = modifier,
            )
        }
        AppDestinations.THREADS -> if (activeBoard != null) {
            ThreadsScreen(
                board = activeBoard,
                page = threadPage,
                onPageChange = onThreadPageChange,
                onThreadSelected = onThreadSelected,
                modifier = modifier,
            )
        }
        AppDestinations.CATALOG -> if (activeBoard != null) {
            CatalogScreen(
                board = activeBoard,
                onThreadSelected = onThreadSelected,
                modifier = modifier,
            )
        }
        else -> Box(
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
}

@Preview(showBackground = true)
@Composable
private fun MobileChanAppPreview() {
    MobileChanApp()
}

private val ActiveBoardSaver = listSaver<Board?, String>(
    save = { board ->
        if (board == null) emptyList()
        else listOf(board.board, board.title, board.pages.toString())
    },
    restore = { saved ->
        if (saved.size < 2) null
        else Board(saved[0], saved[1], saved.getOrNull(2)?.toIntOrNull() ?: 10)
    },
)
