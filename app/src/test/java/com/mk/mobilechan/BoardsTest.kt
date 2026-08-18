package com.mk.mobilechan

import com.mk.mobilechan.data.Board
import com.mk.mobilechan.ui.boards.BoardsUiState
import com.mk.mobilechan.ui.boards.excluding
import org.junit.Assert.assertEquals
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
}
