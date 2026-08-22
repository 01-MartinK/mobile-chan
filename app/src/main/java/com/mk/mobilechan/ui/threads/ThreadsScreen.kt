package com.mk.mobilechan.ui.threads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import com.mk.mobilechan.data.Source
import com.mk.mobilechan.ui.navigation.LocalSource
import com.mk.mobilechan.ui.theme.MobileChanTheme

sealed interface ThreadsUiState {
    data object Loading : ThreadsUiState
    data object Error : ThreadsUiState
    data class Success(val threads: List<IndexThread>, val pageCount: Int) : ThreadsUiState
}

@Composable
fun ThreadsScreen(
    board: Board,
    page: Int,
    onPageChange: (Int) -> Unit,
    onThreadSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: ((Long) -> Boolean)? = null,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    val (state, retry) = rememberThreadsUiState(board, page)

    ThreadsList(
        state = state,
        page = page,
        pageCount = (state as? ThreadsUiState.Success)?.pageCount ?: board.pages,
        onRetry = retry,
        onPageChange = onPageChange,
        onThreadSelected = onThreadSelected,
        showImages = showImages,
        showVideos = showVideos,
        isBookmarked = isBookmarked,
        onToggleBookmark = onToggleBookmark,
        modifier = modifier,
    )
}

@Composable
private fun rememberThreadsUiState(
    board: Board,
    page: Int,
    source: Source = LocalSource.current,
): Pair<ThreadsUiState, () -> Unit> {
    var state by remember { mutableStateOf<ThreadsUiState>(ThreadsUiState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(source.id, board.board, page, retryKey) {
        state = ThreadsUiState.Loading
        state = try {
            val response = source.getIndex(board.board, page)
            ThreadsUiState.Success(
                threads = response.threads,
                pageCount = response.pageCount ?: board.pages,
            )
        } catch (_: Exception) {
            ThreadsUiState.Error
        }
    }

    return state to { retryKey++ }
}

@Composable
private fun ThreadsList(
    state: ThreadsUiState,
    page: Int,
    pageCount: Int,
    onRetry: () -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: ((Long) -> Boolean)? = null,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    when (state) {
        ThreadsUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        ThreadsUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.threads_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.threads_retry))
                }
            }
        }

        is ThreadsUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.threads.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.threads_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    items(
                        items = state.threads.flatMap { thread ->
                            thread.posts.map { post -> thread to post }
                        },
                        key = { (_, post) -> post.no },
                    ) { (thread, post) ->
                        val threadNo = thread.op?.no ?: post.threadNo
                        ThreadCard(
                            thread = thread.cardForPost(post),
                            onClick = { onThreadSelected(threadNo) },
                            showImages = showImages,
                            showVideos = showVideos,
                            isBookmarked = isBookmarked?.invoke(threadNo) == true,
                            onToggleBookmark = if (onToggleBookmark != null) {
                                { onToggleBookmark(thread) }
                            } else null,
                            modifier = if (post.resto != 0L) {
                                Modifier.padding(start = 16.dp)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
                item {
                    PageControls(
                        page = page,
                        pageCount = pageCount,
                        onPageChange = onPageChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageControls(
    page: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onPageChange(page - 1) },
            enabled = page > 1,
        ) {
            Text(stringResource(R.string.thread_page_prev))
        }
        Text(
            text = stringResource(R.string.thread_page, page),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { onPageChange(page + 1) },
            enabled = page < pageCount,
        ) {
            Text(stringResource(R.string.thread_page_next))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThreadsListPreview() {
    MobileChanTheme {
        ThreadsList(
            state = ThreadsUiState.Success(
                listOf(
                    IndexThread(
                        posts = listOf(
                            Post(
                                no = 9823415,
                                now = "05/14/24(Tue)14:28:05",
                                name = "Anonymous",
                                com = """<a href="#p9823401" class="quotelink">&gt;&gt;9823401</a><br><span class="quote">&gt;I want to keep the physical footprint as small as possible while maximizing throughput.</span><br>N100s are fine for basic stuff, but if you want real throughput between nodes you need 2.5GbE minimum.""",
                                replies = 42,
                                images = 8,
                                omitted_posts = 40,
                            ),
                            Post(
                                no = 9823501,
                                resto = 9823415,
                                now = "05/14/24(Tue)15:02:11",
                                name = "Anonymous",
                                com = "this is a comment!",
                            ),
                        ),
                    ),
                ),
                pageCount = 10,
            ),
            page = 1,
            pageCount = 10,
            onRetry = {},
            onPageChange = {},
            onThreadSelected = {},
        )
    }
}
