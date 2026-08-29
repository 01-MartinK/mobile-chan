package com.mk.mobilechan.ui.boards

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.NsfwBoards
import com.mk.mobilechan.data.Source
import com.mk.mobilechan.ui.navigation.LocalSource
import com.mk.mobilechan.ui.theme.MobileChanTheme

sealed interface BoardsUiState {
    data object Loading : BoardsUiState
    data object Error : BoardsUiState
    data class Success(val boards: List<Board>) : BoardsUiState
}

fun BoardsUiState.excluding(
    boardTags: Collection<String>,
    hideNsfw: Boolean = false,
): BoardsUiState {
    if (this !is BoardsUiState.Success) return this
    if (boardTags.isEmpty() && !hideNsfw) return this
    val excluded = boardTags.map { it.lowercase() }.toSet()
    return copy(
        boards = boards.filter { board ->
            board.board.lowercase() !in excluded && (!hideNsfw || !NsfwBoards.contains(board))
        },
    )
}

@Composable
fun rememberBoardsUiState(
    source: Source = LocalSource.current,
): Pair<BoardsUiState, () -> Unit> {
    var state by remember { mutableStateOf<BoardsUiState>(BoardsUiState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(source.id, retryKey) {
        state = BoardsUiState.Loading
        state = try {
            BoardsUiState.Success(source.getBoards())
        } catch (_: Exception) {
            BoardsUiState.Error
        }
    }

    return state to { retryKey++ }
}

@Composable
fun BoardsScreen(
    state: BoardsUiState,
    onRetry: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onExcludeBoard: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoardsList(
        state = state,
        onRetry = onRetry,
        onBoardSelected = onBoardSelected,
        onExcludeBoard = onExcludeBoard,
        modifier = modifier,
    )
}

@Composable
private fun BoardsList(
    state: BoardsUiState,
    onRetry: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    onExcludeBoard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        BoardsUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        BoardsUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.boards_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.boards_retry))
                }
            }
        }

        is BoardsUiState.Success -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(state.boards, key = { it.board }) { board ->
                    BoardListItem(
                        board = board,
                        onClick = { onBoardSelected(board) },
                        onExcludeBoard = onExcludeBoard,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BoardListItem(
    board: Board,
    onClick: () -> Unit,
    onExcludeBoard: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.board_item, board.board, board.title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            modifier = Modifier.combinedClickable(
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

@Preview(showBackground = true)
@Composable
private fun BoardsListPreview() {
    MobileChanTheme {
        BoardsList(
            state = BoardsUiState.Success(
                listOf(
                    Board("a", "Anime & Manga"),
                    Board("g", "Technology"),
                    Board("wsg", "Worksafe GIF"),
                ),
            ),
            onRetry = {},
            onBoardSelected = {},
            onExcludeBoard = {},
        )
    }
}
