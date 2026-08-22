package com.mk.mobilechan.ui.threads

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mk.mobilechan.R
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.MediaDownloader

@Composable
fun ThreadContextMenu(
    thread: IndexThread,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val canDownload = thread.imageUrl != null || thread.op?.imageUrl != null

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        if (onToggleBookmark != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isBookmarked) {
                                R.string.thread_menu_remove_bookmark
                            } else {
                                R.string.thread_menu_bookmark
                            },
                        ),
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleBookmark()
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isBookmarked) {
                            Icons.Outlined.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        contentDescription = null,
                    )
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.thread_menu_download_media)) },
            enabled = canDownload,
            onClick = {
                onDismissRequest()
                val started = MediaDownloader.download(context, thread)
                val message = if (started) {
                    R.string.thread_media_download_started
                } else {
                    R.string.thread_media_download_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.thread_menu_jump_to_op)) },
            onClick = onDismissRequest,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.VerticalAlignTop,
                    contentDescription = null,
                )
            },
        )
    }
}
