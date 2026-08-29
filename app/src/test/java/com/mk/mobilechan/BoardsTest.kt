package com.mk.mobilechan

import com.google.gson.Gson
import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.BoardsResponse
import com.mk.mobilechan.data.NsfwBoards
import com.mk.mobilechan.ui.boards.BoardsUiState
import com.mk.mobilechan.ui.boards.excluding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardsTest {

    @Test
    fun `excluding filters boards state correctly`() {
        val boards = listOf(
            Board("a", "Anime & Manga"),
            Board("b", "Random"),
            Board("g", "Technology"),
            Board("pol", "Politically Incorrect"),
        )
        val state = BoardsUiState.Success(boards)
        val excluded = setOf("b", "pol")

        val result = state.excluding(excluded)

        require(result is BoardsUiState.Success)
        assertEquals(2, result.boards.size)
        assertEquals(listOf("a", "g"), result.boards.map { it.board })
    }

    @Test
    fun `excluding handles case insensitive board tags`() {
        val boards = listOf(
            Board("a", "Anime & Manga"),
            Board("g", "Technology"),
        )
        val state = BoardsUiState.Success(boards)
        val excluded = setOf("A", "G")

        val result = state.excluding(excluded)

        require(result is BoardsUiState.Success)
        assertEquals(0, result.boards.size)
    }

    @Test
    fun `excluding non success state returns same state`() {
        val loadingState: BoardsUiState = BoardsUiState.Loading
        val errorState: BoardsUiState = BoardsUiState.Error

        assertEquals(loadingState, loadingState.excluding(setOf("b")))
        assertEquals(errorState, errorState.excluding(setOf("b")))
    }

    @Test
    fun `hiding nsfw drops known nsfw tags and ws_board zero`() {
        val boards = listOf(
            Board("a", "Anime & Manga", ws_board = 1),
            Board("b", "Random", ws_board = 0),
            Board("g", "Technology", ws_board = 1),
            Board("newnsfw", "Unknown NSFW", ws_board = 0),
        )
        val state = BoardsUiState.Success(boards)

        val result = state.excluding(emptyList(), hideNsfw = true)

        require(result is BoardsUiState.Success)
        assertEquals(listOf("a", "g"), result.boards.map { it.board })
    }

    @Test
    fun `parses ws_board from boards json`() {
        val response = Gson().fromJson(
            """
            {
              "boards": [
                {"board": "a", "title": "Anime & Manga", "ws_board": 1, "pages": 10},
                {"board": "b", "title": "Random", "ws_board": 0, "pages": 10}
              ]
            }
            """.trimIndent(),
            BoardsResponse::class.java,
        )

        assertFalse(response.boards[0].isNsfw)
        assertTrue(response.boards[1].isNsfw)
        assertTrue(NsfwBoards.contains(response.boards[1]))
    }
}
