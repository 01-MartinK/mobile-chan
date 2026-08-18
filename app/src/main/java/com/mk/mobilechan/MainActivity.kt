package com.mk.mobilechan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.remember
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
import coil.decode.ImageDecoderDecoder
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.BookmarksStore
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.ui.boards.BoardContextMenu
import com.mk.mobilechan.data.rememberBookmarksStore
import com.mk.mobilechan.ui.boards.BoardsScreen
import com.mk.mobilechan.ui.boards.BoardsUiState
import com.mk.mobilechan.ui.boards.excluding
import com.mk.mobilechan.ui.boards.rememberBoardsUiState
import com.mk.mobilechan.ui.bookmarks.BookmarksScreen
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
                .components { add(ImageDecoderDecoder.Factory()) }
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
    val bookmarksStore = rememberBookmarksStore()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var activeBoard by rememberSaveable(stateSaver = ActiveBoardSaver) { mutableStateOf(null) }
    var threadPage by rememberSaveable { mutableIntStateOf(1) }
    var activeThreadNo by rememberSaveable { mutableStateOf<Long?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val (boardsState, retryBoards) = rememberBoardsUiState()
    val visibleBoardsState = remember(boardsState, settings.excludedBoards) {
        boardsState.excluding(settings.excludedBoards)
    }

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
                boardsState = visibleBoardsState,
                activeBoard = activeBoard,
                enabledModules = settings.enabledModules,
                onRetryBoards = retryBoards,
                onBoardSelected = { board ->
                    selectBoard(board)
                    scope.launch { drawerState.close() }
                    },
                    onExcludeBoard = { boardTag ->
                        settings.addExcludedBoard(boardTag)
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
                    boardsState = visibleBoardsState,
                    activeBoard = activeBoard,
                    threadPage = threadPage,
                    activeThreadNo = activeThreadNo,
                    settingsOpen = settingsOpen,
                    settings = settings,
                    bookmarksStore = bookmarksStore,
                    onRetryBoards = retryBoards,
                    onBoardSelected = selectBoard,
                        onExcludeBoard = { boardTag ->
                            settings.addExcludedBoard(boardTag)
                        },
                    onThreadPageChange = { threadPage = it },
                    onThreadSelected = { activeThreadNo = it },
                    onNavigateToThread = { board, threadNo ->
                        activeBoard = board
                        activeThreadNo = threadNo
                        currentDestination = AppDestinations.THREADS
                    },
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
    boardsState: BoardsUiState,
    activeBoard: Board?,
    enabledModules: Set<AppModules>,
    onRetryBoards: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onExcludeBoard: (String) -> Unit = {},
) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        val modules = remember(enabledModules) {
            AppModules.entries.filter {
                enabledModules.isEmpty() || it in enabledModules
            }
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
                        AppDrawerBoardItem(
                            board = board,
                            selected = board.board == activeBoard?.board,
                            onClick = { onBoardSelected(board) },
                            onExcludeBoard = onExcludeBoard,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerBoardItem(
    board: Board,
    selected: Boolean,
    onClick: () -> Unit,
    onExcludeBoard: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
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
            selected = selected,
            onClick = onClick,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    onLongClickLabel = stringResource(R.string.board_menu),
                ),
        )
        BoardContextMenu(
            board = board,
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            onExcludeBoard = onExcludeBoard,
        )
    }
}

@Composable
private fun DestinationPane(
    modifier: Modifier = Modifier,
    destination: AppDestinations,
    boardsState: BoardsUiState,
    activeBoard: Board?,
    threadPage: Int,
    activeThreadNo: Long?,
    onRetryBoards: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onExcludeBoard: (String) -> Unit = {},
    onThreadPageChange: (Int) -> Unit,
    onThreadSelected: (Long) -> Unit,
    onNavigateToThread: (Board, Long) -> Unit,
    settingsOpen: Boolean,
    settings: UserSettings,
    bookmarksStore: BookmarksStore,
) {
    if (settingsOpen) {
        SettingsScreen(
            allowNsfw = settings.allowNsfw,
            onAllowNsfwChange = settings::updateAllowNsfw,
            theme = settings.theme,
            onThemeChange = settings::updateTheme,
            excludedBoards = settings.excludedBoards,
            onAddExcludedBoard = settings::addExcludedBoard,
            onRemoveExcludedBoard = settings::removeExcludedBoard,
            showImages = settings.showImages,
            onShowImagesChange = settings::updateShowImages,
            showVideos = settings.showVideos,
            onShowVideosChange = settings::updateShowVideos,
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
                onExcludeBoard = onExcludeBoard,
                modifier = modifier,
            )
        }
        AppDestinations.BOOKMARKS -> {
            BookmarksScreen(
                bookmarks = bookmarksStore.bookmarks,
                onThreadSelected = onNavigateToThread,
                onToggleBookmark = { board, thread ->
                    bookmarksStore.toggleBookmark(board, thread)
                },
                showImages = settings.showImages,
                showVideos = settings.showVideos,
                modifier = modifier,
            )
        }
        AppDestinations.THREADS -> if (activeBoard != null) {
            Box(modifier = modifier) {
                ThreadsScreen(
                    board = activeBoard,
                    page = threadPage,
                    onPageChange = onThreadPageChange,
                    onThreadSelected = onThreadSelected,
                    showImages = settings.showImages,
                    showVideos = settings.showVideos,
                    isBookmarked = { threadNo ->
                        bookmarksStore.isBookmarked(activeBoard.board, threadNo)
                    },
                    onToggleBookmark = { thread ->
                        bookmarksStore.toggleBookmark(activeBoard, thread)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (activeThreadNo != null) {
                    ThreadScreen(
                        board = activeBoard,
                        threadNo = activeThreadNo,
                        showImages = settings.showImages,
                        showVideos = settings.showVideos,
                        isBookmarked = bookmarksStore.isBookmarked(activeBoard.board, activeThreadNo),
                        onToggleBookmark = { thread ->
                            bookmarksStore.toggleBookmark(activeBoard, thread)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        AppDestinations.CATALOG -> when {
            activeBoard != null && activeThreadNo != null -> ThreadScreen(
                board = activeBoard,
                threadNo = activeThreadNo,
                showImages = settings.showImages,
                showVideos = settings.showVideos,
                isBookmarked = bookmarksStore.isBookmarked(activeBoard.board, activeThreadNo),
                onToggleBookmark = { thread ->
                    bookmarksStore.toggleBookmark(activeBoard, thread)
                },
                modifier = modifier,
            )
            activeBoard != null -> CatalogScreen(
                board = activeBoard,
                onThreadSelected = onThreadSelected,
                isBookmarked = { threadNo ->
                    bookmarksStore.isBookmarked(activeBoard.board, threadNo)
                },
                onToggleBookmark = { thread ->
                    bookmarksStore.toggleBookmark(activeBoard, thread)
                },
                modifier = modifier,
            )
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
