package com.mk.mobilechan

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.Icon
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
import com.mk.mobilechan.ui.navigation.AppDestinations
import com.mk.mobilechan.ui.navigation.AppModules
import com.mk.mobilechan.ui.navigation.BottomNavBar
import com.mk.mobilechan.ui.navigation.TopNavBar
import com.mk.mobilechan.ui.theme.MobileChanTheme
import com.mk.mobilechan.ui.threads.ThreadsScreen
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
            MobileChanTheme {
                MobileChanApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MobileChanApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.BOARDS) }
    var activeBoard by rememberSaveable(stateSaver = ActiveBoardSaver) { mutableStateOf<Board?>(null) }
    var threadPage by rememberSaveable { mutableIntStateOf(1) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val (boardsState, retryBoards) = rememberBoardsUiState()

    val selectBoard: (Board) -> Unit = { board ->
        activeBoard = board
        threadPage = 1
        currentDestination = AppDestinations.BOARDS
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
            onDestinationSelected = { destination ->
                if (destination == AppDestinations.BOARDS) {
                    activeBoard = null
                }
                currentDestination = destination
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopNavBar(
                        title = topBarTitle(currentDestination, activeBoard),
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSearchClick = { currentDestination = AppDestinations.SEARCH },
                    )
                },
            ) { innerPadding ->
                DestinationPane(
                    destination = currentDestination,
                    boardsState = boardsState,
                    activeBoard = activeBoard,
                    threadPage = threadPage,
                    onRetryBoards = retryBoards,
                    onBoardSelected = selectBoard,
                    onThreadPageChange = { threadPage = it },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun topBarTitle(destination: AppDestinations, activeBoard: Board?): String {
    return if (destination == AppDestinations.BOARDS && activeBoard != null) {
        stringResource(R.string.board_item, activeBoard.board, activeBoard.title)
    } else {
        stringResource(destination.breadcrumbRes)
    }
}

@Composable
private fun AppDrawer(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit,
    boardsState: BoardsUiState,
    activeBoard: Board?,
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
        AppModules.entries.forEach { destination ->
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
    onRetryBoards: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onThreadPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestinations.BOARDS -> if (activeBoard != null) {
            ThreadsScreen(
                board = activeBoard,
                page = threadPage,
                onPageChange = onThreadPageChange,
                modifier = modifier,
            )
        } else {
            BoardsScreen(
                state = boardsState,
                onRetry = onRetryBoards,
                onBoardSelected = onBoardSelected,
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
    MobileChanTheme {
        MobileChanApp()
    }
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
