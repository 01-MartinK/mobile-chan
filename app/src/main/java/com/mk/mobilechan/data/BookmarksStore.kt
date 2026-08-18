package com.mk.mobilechan.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.google.gson.Gson

private const val BOOKMARKS_PREFS = "bookmarks_prefs"
private const val KEY_BOOKMARKS = "bookmarked_threads"

data class BookmarkedThread(
    val board: Board,
    val thread: IndexThread,
) {
    val threadNo: Long? get() = thread.op?.no
}

class BookmarksStore internal constructor(
    initialBookmarks: List<BookmarkedThread>,
    private val prefs: SharedPreferences,
    private val gson: Gson = Gson(),
) {
    var bookmarks by mutableStateOf(initialBookmarks)
        private set

    fun isBookmarked(boardTag: String, threadNo: Long): Boolean {
        return bookmarks.any { it.board.board == boardTag && it.threadNo == threadNo }
    }

    fun toggleBookmark(board: Board, thread: IndexThread) {
        val threadNo = thread.op?.no ?: return
        if (isBookmarked(board.board, threadNo)) {
            removeBookmark(board.board, threadNo)
        } else {
            addBookmark(board, thread)
        }
    }

    fun addBookmark(board: Board, thread: IndexThread) {
        val threadNo = thread.op?.no ?: return
        if (isBookmarked(board.board, threadNo)) return
        val newBookmark = BookmarkedThread(board = board, thread = thread)
        bookmarks = bookmarks + newBookmark
        persistBookmarks()
    }

    fun removeBookmark(boardTag: String, threadNo: Long) {
        if (!isBookmarked(boardTag, threadNo)) return
        bookmarks = bookmarks.filterNot { it.board.board == boardTag && it.threadNo == threadNo }
        persistBookmarks()
    }

    private fun persistBookmarks() {
        val json = gson.toJson(bookmarks)
        prefs.edit { putString(KEY_BOOKMARKS, json) }
    }
}

@Composable
fun rememberBookmarksStore(): BookmarksStore {
    val context = LocalContext.current
    return remember {
        val prefs = context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_BOOKMARKS, null)
        val initialBookmarks = if (!json.isNullOrEmpty()) {
            try {
                val array = gson.fromJson(json, Array<BookmarkedThread>::class.java)
                array?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        BookmarksStore(
            initialBookmarks = initialBookmarks,
            prefs = prefs,
            gson = gson,
        )
    }
}
