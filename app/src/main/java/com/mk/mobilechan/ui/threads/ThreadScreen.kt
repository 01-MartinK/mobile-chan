package com.mk.mobilechan.ui.threads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import com.mk.mobilechan.ui.theme.MobileChanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface ThreadUiState {
    data object Loading : ThreadUiState
    data object Error : ThreadUiState
    data class Success(val posts: List<IndexThread>) : ThreadUiState
}

@Composable
fun ThreadScreen(
    board: Board,
    threadNo: Long,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: Boolean = false,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    val (state, retry) = rememberThreadUiState(board.board, threadNo)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ThreadPosts(
            state = state,
            onRetry = retry,
            showImages = showImages,
            showVideos = showVideos,
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun rememberThreadUiState(
    board: String,
    threadNo: Long,
): Pair<ThreadUiState, () -> Unit> {
    var state by remember { mutableStateOf<ThreadUiState>(ThreadUiState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(board, threadNo, retryKey) {
        state = ThreadUiState.Loading
        state = try {
            ThreadUiState.Success(FourChanClient.getThread(board, threadNo))
        } catch (_: Exception) {
            ThreadUiState.Error
        }
    }

    return state to { retryKey++ }
}

@Composable
private fun ThreadPosts(
    state: ThreadUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: Boolean = false,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    when (state) {
        ThreadUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        ThreadUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.thread_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.thread_retry))
                }
            }
        }

        is ThreadUiState.Success -> {
            if (state.posts.isEmpty()) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.thread_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                ThreadPostList(
                    posts = state.posts,
                    showImages = showImages,
                    showVideos = showVideos,
                    isBookmarked = isBookmarked,
                    onToggleBookmark = onToggleBookmark,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun ThreadPostList(
    posts: List<IndexThread>,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: Boolean = false,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var highlightedPost by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(highlightedPost) {
        val target = highlightedPost ?: return@LaunchedEffect
        delay(1_500)
        if (highlightedPost == target) highlightedPost = null
    }

    val homeToPost: (Long) -> Unit = remember(posts) {
        { postNo ->
            val index = posts.indexOfFirst { it.op?.no == postNo }
            if (index >= 0) {
                highlightedPost = postNo
                scope.launch { listState.animateScrollToItem(index) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(posts, key = { it.op?.no ?: it.hashCode() }) { post ->
            val isOp = post.op?.resto == 0L || post == posts.firstOrNull()
            ThreadCard(
                thread = post,
                onQuoteClick = homeToPost,
                highlighted = post.op?.no == highlightedPost,
                showImages = showImages,
                showVideos = showVideos,
                isBookmarked = if (isOp) isBookmarked else false,
                onToggleBookmark = if (isOp && onToggleBookmark != null) {
                    { onToggleBookmark(post) }
                } else null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThreadPostsPreview() {
    MobileChanTheme {
        ThreadPosts(
            state = ThreadUiState.Success(
                listOf(
                    IndexThread(
                        posts = listOf(
                            Post(
                                no = 9823415,
                                now = "05/14/24(Tue)14:28:05",
                                name = "Anonymous",
                                sub = "Homelab thread",
                                com = "N100s are fine for basic stuff, but if you want real throughput you need 2.5GbE minimum.",
                                replies = 2,
                                images = 1,
                            ),
                        ),
                    ),
                    IndexThread(
                        posts = listOf(
                            Post(
                                no = 9823416,
                                resto = 9823415,
                                now = "05/14/24(Tue)14:31:12",
                                name = "Anonymous",
                                com = """<a href="#p9823415" class="quotelink">&gt;&gt;9823415</a><br>Get a used Lenovo Tiny.""",
                            ),
                        ),
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
