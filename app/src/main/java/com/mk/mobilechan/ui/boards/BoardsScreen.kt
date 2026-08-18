package com.mk.mobilechan.ui.boards

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.ui.theme.MobileChanTheme
import kotlinx.coroutines.launch

sealed interface BoardsUiState {
    data object Loading : BoardsUiState
    data object Error : BoardsUiState
    data class Success(val boards: List<Board>) : BoardsUiState
}

fun BoardsUiState.excluding(boardTags: Collection<String>): BoardsUiState {
    if (this !is BoardsUiState.Success || boardTags.isEmpty()) return this
    val excluded = boardTags.map { it.lowercase() }.toSet()
    return copy(boards = boards.filter { it.board.lowercase() !in excluded })
}

@Composable
fun rememberBoardsUiState(): Pair<BoardsUiState, () -> Unit> {
    var state by remember { mutableStateOf<BoardsUiState>(BoardsUiState.Loading) }
    val scope = rememberCoroutineScope()

    val loadBoards: () -> Unit = {
        state = BoardsUiState.Loading
        scope.launch {
            state = try {
                BoardsUiState.Success(FourChanClient.api.getBoards().boards)
            } catch (_: Exception) {
                BoardsUiState.Error
            }
        }
    }

    LaunchedEffect(Unit) { loadBoards() }
    return state to loadBoards
}

@Composable
fun BoardsScreen(
    state: BoardsUiState,
    onRetry: () -> Unit,
    onBoardSelected: (Board) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoardsList(
        state = state,
        onRetry = onRetry,
        onBoardSelected = onBoardSelected,
        modifier = modifier,
    )
}

@Composable
private fun BoardsList(
    state: BoardsUiState,
    onRetry: () -> Unit,
    onBoardSelected: (Board) -> Unit,
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
                    BoardListItem(board, onClick = { onBoardSelected(board) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BoardListItem(board: Board, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.board_item, board.board, board.title),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
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
        )
    }
}
