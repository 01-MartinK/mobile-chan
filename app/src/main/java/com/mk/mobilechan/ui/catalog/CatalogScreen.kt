package com.mk.mobilechan.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.FourChanClient
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import com.mk.mobilechan.ui.theme.MobileChanTheme
import com.mk.mobilechan.ui.threads.ThreadContextMenu

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data object Error : CatalogUiState
    data class Success(val threads: List<IndexThread>) : CatalogUiState
}

@Composable
fun CatalogScreen(
    board: Board,
    onThreadSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: ((Long) -> Boolean)? = null,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    val (state, retry) = rememberCatalogUiState(board.board)

    CatalogGrid(
        state = state,
        onRetry = retry,
        onThreadSelected = onThreadSelected,
        isBookmarked = isBookmarked,
        onToggleBookmark = onToggleBookmark,
        modifier = modifier,
    )
}

@Composable
private fun rememberCatalogUiState(board: String): Pair<CatalogUiState, () -> Unit> {
    var state by remember { mutableStateOf<CatalogUiState>(CatalogUiState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(board, retryKey) {
        state = CatalogUiState.Loading
        state = try {
            CatalogUiState.Success(FourChanClient.getCatalog(board))
        } catch (_: Exception) {
            CatalogUiState.Error
        }
    }

    return state to { retryKey++ }
}

@Composable
private fun CatalogGrid(
    state: CatalogUiState,
    onRetry: () -> Unit,
    onThreadSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: ((Long) -> Boolean)? = null,
    onToggleBookmark: ((IndexThread) -> Unit)? = null,
) {
    when (state) {
        CatalogUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        CatalogUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.catalog_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.catalog_retry))
                }
            }
        }

        is CatalogUiState.Success -> {
            if (state.threads.isEmpty()) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.catalog_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.threads, key = { it.op?.no ?: it.hashCode() }) { thread ->
                        val threadNo = thread.op?.no
                        CatalogCard(
                            thread = thread,
                            onClick = { threadNo?.let(onThreadSelected) },
                            isBookmarked = threadNo != null && (isBookmarked?.invoke(threadNo) == true),
                            onToggleBookmark = if (onToggleBookmark != null) {
                                { onToggleBookmark(thread) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(
    thread: IndexThread,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
) {
    val op = thread.op ?: return
    val subject = remember(op.sub) {
        op.sub?.takeIf { it.isNotBlank() }?.let(::unescapeHtml)
    }
    val snippet = remember(op.com) {
        op.com?.takeIf { it.isNotBlank() }?.let(::unescapeHtml)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    onLongClickLabel = stringResource(R.string.thread_menu),
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                CatalogThumbnail(
                    thumbnailUrl = thread.thumbnailUrl,
                    filename = op.filename,
                )
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.thread_stats, op.replies, op.images),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (subject != null) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (snippet != null) {
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        ThreadContextMenu(
            thread = thread,
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark,
        )
    }
}

@Composable
private fun CatalogThumbnail(
    thumbnailUrl: String?,
    filename: String?,
) {
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    if (thumbnailUrl == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    AsyncImage(
        model = thumbnailUrl,
        contentDescription = filename ?: stringResource(R.string.thread_image),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape),
        contentScale = ContentScale.Crop,
    )
}

private fun unescapeHtml(value: String): String =
    HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()

@Preview(showBackground = true)
@Composable
private fun CatalogGridPreview() {
    MobileChanTheme {
        CatalogGrid(
            state = CatalogUiState.Success(
                listOf(
                    IndexThread(
                        posts = listOf(
                            Post(
                                no = 9823415,
                                sub = "Homelab thread",
                                com = "N100s are fine for basic stuff, but if you want real throughput between nodes you need 2.5GbE minimum.",
                                replies = 42,
                                images = 8,
                            ),
                        ),
                    ),
                    IndexThread(
                        posts = listOf(
                            Post(
                                no = 9823416,
                                com = "What mini PC should I buy?",
                                replies = 12,
                                images = 1,
                            ),
                        ),
                    ),
                ),
            ),
            onRetry = {},
            onThreadSelected = {},
        )
    }
}
