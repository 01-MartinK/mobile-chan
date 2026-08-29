package com.mk.mobilechan

import com.mk.mobilechan.data.Board
import com.mk.mobilechan.data.NsfwBoards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NsfwBoardsTest {

    @Test
    fun `contains known nsfw tags regardless of slashes or case`() {
        assertTrue(NsfwBoards.contains("b"))
        assertTrue(NsfwBoards.contains("/POL/"))
        assertTrue(NsfwBoards.contains("Gif"))
        assertFalse(NsfwBoards.contains("g"))
        assertFalse(NsfwBoards.contains("a"))
    }

    @Test
    fun `contains boards marked not worksafe even if tag is unknown`() {
        assertTrue(NsfwBoards.contains(Board("newnsfw", "Unknown", ws_board = 0)))
        assertFalse(NsfwBoards.contains(Board("g", "Technology", ws_board = 1)))
        assertTrue(NsfwBoards.contains(Board("b", "Random", ws_board = 1)))
    }

    @Test
    fun `hidden tags always include nsfw when nsfw is disallowed`() {
        val excluded = setOf("g")

        assertEquals(setOf("g"), NsfwBoards.hiddenTags(excluded, allowNsfw = true))
        assertTrue(NsfwBoards.hiddenTags(excluded, allowNsfw = false).containsAll(setOf("g", "b", "pol")))
    }

    @Test
    fun `visible excluded omits nsfw tags when nsfw is disallowed`() {
        val excluded = setOf("g", "b", "pol")

        assertEquals(setOf("g", "b", "pol"), NsfwBoards.visibleExcluded(excluded, allowNsfw = true))
        assertEquals(setOf("g"), NsfwBoards.visibleExcluded(excluded, allowNsfw = false))
    }
}
