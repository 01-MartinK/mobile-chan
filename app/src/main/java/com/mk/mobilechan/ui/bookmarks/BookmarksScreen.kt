package com.mk.mobilechan.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.BookmarkedThread
import com.mk.mobilechan.ui.threads.ThreadCard

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkedThread>,
    onThreadSelected: (Board, Long) -> Unit,
    onToggleBookmark: (Board, com.mk.mobilechan.data.IndexThread) -> Unit,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.bookmarks_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = bookmarks,
                key = { "${it.board.board}_${it.threadNo}" },
            ) { bookmark ->
                val threadNo = bookmark.threadNo
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.board_item,
                            bookmark.board.board,
                            bookmark.board.title,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    ThreadCard(
                        thread = bookmark.thread,
                        isBookmarked = true,
                        onToggleBookmark = {
                            onToggleBookmark(bookmark.board, bookmark.thread)
                        },
                        onClick = {
                            if (threadNo != null) {
                                onThreadSelected(bookmark.board, threadNo)
                            }
                        },
                        showImages = showImages,
                        showVideos = showVideos,
                    )
                }
            }
        }
    }
}
