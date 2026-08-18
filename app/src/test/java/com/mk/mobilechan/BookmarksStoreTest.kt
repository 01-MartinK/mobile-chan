package com.mk.mobilechan

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.BookmarksStore
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookmarksStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var gson: Gson

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        gson = Gson()
    }

    @Test
    fun testAddAndRemoveBookmark() {
        val store = BookmarksStore(initialBookmarks = emptyList(), prefs = fakePrefs, gson = gson)
        val board = Board(board = "g", title = "Technology")
        val thread = IndexThread(posts = listOf(Post(no = 123456L, com = "Hello world")))

        assertFalse(store.isBookmarked("g", 123456L))
        assertEquals(0, store.bookmarks.size)

        store.addBookmark(board, thread)
        assertTrue(store.isBookmarked("g", 123456L))
        assertEquals(1, store.bookmarks.size)

        // Adding duplicate thread should have no effect
        store.addBookmark(board, thread)
        assertEquals(1, store.bookmarks.size)

        store.removeBookmark("g", 123456L)
        assertFalse(store.isBookmarked("g", 123456L))
        assertEquals(0, store.bookmarks.size)
    }

    @Test
    fun testToggleBookmark() {
        val store = BookmarksStore(initialBookmarks = emptyList(), prefs = fakePrefs, gson = gson)
        val board = Board(board = "v", title = "Video Games")
        val thread = IndexThread(posts = listOf(Post(no = 654321L, com = "Game discussion")))

        store.toggleBookmark(board, thread)
        assertTrue(store.isBookmarked("v", 654321L))

        store.toggleBookmark(board, thread)
        assertFalse(store.isBookmarked("v", 654321L))
    }

    @Test
    fun testPersistence() {
        val store = BookmarksStore(initialBookmarks = emptyList(), prefs = fakePrefs, gson = gson)
        val board = Board(board = "a", title = "Anime & Manga")
        val thread = IndexThread(posts = listOf(Post(no = 999L, com = "Anime post")))

        store.addBookmark(board, thread)

        val savedJson = fakePrefs.getString("bookmarked_threads", null)
        assertTrue(savedJson != null && savedJson.contains("999"))

        // Re-create store with saved json
        val loadedArray = gson.fromJson(savedJson, Array<com.mk.mobilechan.data.BookmarkedThread>::class.java)
        val newStore = BookmarksStore(initialBookmarks = loadedArray.toList(), prefs = fakePrefs, gson = gson)

        assertTrue(newStore.isBookmarked("a", 999L))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values

        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class Editor : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) tempMap[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) tempMap.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                tempMap.clear()
                return this
            }

            override fun commit(): Boolean {
                values.putAll(tempMap)
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
