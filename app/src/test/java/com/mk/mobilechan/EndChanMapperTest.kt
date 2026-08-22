package com.mk.mobilechan

import com.google.gson.Gson
import com.mk.mobilechan.data.EndChanBoardsResponse
import com.mk.mobilechan.data.EndChanIndexResponse
import com.mk.mobilechan.data.EndChanMapper
import com.mk.mobilechan.data.EndChanPosting
import com.mk.mobilechan.data.SourceId
import com.mk.mobilechan.data.Sources
import com.mk.mobilechan.ui.navigation.AppModules
import com.mk.mobilechan.ui.settings.activeSourceFromName
import com.mk.mobilechan.ui.settings.nextEnabledModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EndChanMapperTest {

    private val gson = Gson()
    private val siteUrl = "https://endchan.net/"

    @Test
    fun sourcesReturnsMatchingImplementations() {
        assertEquals(SourceId.FOUR_CHAN, Sources.of(SourceId.FOUR_CHAN).id)
        assertEquals(SourceId.END_CHAN, Sources.of(SourceId.END_CHAN).id)
        assertSame(Sources.of(SourceId.FOUR_CHAN), Sources.of(SourceId.FOUR_CHAN))
        assertSame(Sources.of(SourceId.END_CHAN), Sources.of(SourceId.END_CHAN))
    }

    @Test
    fun activeSourceUsesEnabledModule() {
        assertEquals(
            AppModules.END_CHAN,
            activeSourceFromName("END_CHAN", setOf(AppModules.FOUR_CHAN, AppModules.END_CHAN)),
        )
        assertEquals(
            AppModules.FOUR_CHAN,
            activeSourceFromName("END_CHAN", setOf(AppModules.FOUR_CHAN)),
        )
        assertEquals(
            AppModules.END_CHAN,
            activeSourceFromName(null, setOf(AppModules.END_CHAN)),
        )
    }

    @Test
    fun nextEnabledModulesKeepsAtLeastOne() {
        val both = setOf(AppModules.FOUR_CHAN, AppModules.END_CHAN)
        assertEquals(
            setOf(AppModules.END_CHAN),
            nextEnabledModules(both, AppModules.FOUR_CHAN, false),
        )
        assertEquals(
            both,
            nextEnabledModules(setOf(AppModules.FOUR_CHAN), AppModules.END_CHAN, true),
        )
        assertEquals(
            null,
            nextEnabledModules(setOf(AppModules.FOUR_CHAN), AppModules.FOUR_CHAN, false),
        )
    }

    @Test
    fun mapsUnwrappedBoardsResponse() {
        val response = gson.fromJson(
            """
            {
              "pageCount": 2,
              "boards": [
                {"boardUri": "news", "boardName": "News"},
                {"boardUri": "dead", "boardName": "Gone", "inactive": true},
                {"boardUri": "", "boardName": "Missing"}
              ]
            }
            """.trimIndent(),
            EndChanBoardsResponse::class.java,
        )

        assertEquals(2, response.pages)
        val boards = EndChanMapper.boards(response.allBoards)
        assertEquals(listOf("news", "dead"), boards.map { it.board })
        assertEquals("News", boards.first().title)
    }

    @Test
    fun mapsWrappedBoardsResponse() {
        val response = gson.fromJson(
            """
            {
              "status": "ok",
              "data": {
                "pageCount": 1,
                "boards": [{"boardUri": "int", "boardName": "International"}]
              }
            }
            """.trimIndent(),
            EndChanBoardsResponse::class.java,
        )

        assertEquals(1, response.pages)
        val boards = EndChanMapper.boards(response.allBoards)
        assertEquals("int", boards.single().board)
        assertEquals("International", boards.single().title)
    }

    @Test
    fun mergesPaginatedBoardResponses() {
        val page1 = gson.fromJson(
            """{"pageCount":2,"boards":[{"boardUri":"news","boardName":"News"},{"boardUri":"News","boardName":"Dup"}]}""",
            EndChanBoardsResponse::class.java,
        )
        val page2 = gson.fromJson(
            """{"pageCount":2,"boards":[{"boardUri":"int","boardName":"International"}]}""",
            EndChanBoardsResponse::class.java,
        )

        val boards = EndChanMapper.allBoards(listOf(page1, page2))
        assertEquals(listOf("news", "int"), boards.map { it.board })
    }

    @Test
    fun mapsIndexThreadToUnifiedModels() {
        val response = gson.fromJson(
            """
            {
              "pageCount": 14,
              "threads": [{
                "name": "Reader",
                "boardUri": "news",
                "threadId": 29048,
                "subject": "Iran war general thread",
                "markdown": "Latest news<br>",
                "creation": "2026-04-13T03:47:00.000Z",
                "locked": false,
                "pinned": true,
                "files": [{
                  "originalName": "rubble.png",
                  "path": "/.media/abc-imagepng.png",
                  "thumb": "/.media/t_abc-imagepng",
                  "mime": "image/png"
                }],
                "posts": [{
                  "postId": 29182,
                  "name": "Tardus",
                  "markdown": "this is a comment!",
                  "files": [{
                    "originalName": "clip.jpg",
                    "path": "/.media/clip.jpg",
                    "thumb": "/.media/t_clip",
                    "mime": "image/jpeg"
                  }]
                }],
                "omittedPosts": 14,
                "omittedFiles": 3
              }]
            }
            """.trimIndent(),
            EndChanIndexResponse::class.java,
        )

        val page = EndChanMapper.index(response, siteUrl)
        assertEquals(14, page.pageCount)
        val thread = page.threads.single()
        val op = thread.op!!
        assertEquals(29048L, op.no)
        assertEquals(0L, op.resto)
        assertEquals("Reader", op.name)
        assertEquals("Iran war general thread", op.sub)
        assertEquals("Latest news<br>", op.com)
        assertEquals(".png", op.ext)
        assertEquals("image/png", op.mime)
        assertEquals("rubble.png", op.filename)
        assertEquals(1, op.sticky)
        assertEquals(0, op.closed)
        assertEquals(15, op.replies)
        assertEquals(3, op.images)
        assertEquals(14, op.omitted_posts)
        assertEquals("https://endchan.net/.media/abc-imagepng.png", thread.imageUrl)
        assertEquals("https://endchan.net/.media/t_abc-imagepng", thread.thumbnailUrl)
        assertTrue(op.now.contains("04/13/26"))
        assertEquals(2, thread.posts.size)
        val lastReply = thread.posts[1]
        assertEquals(29182L, lastReply.no)
        assertEquals(29048L, lastReply.resto)
        assertEquals("Tardus", lastReply.name)
        assertEquals("this is a comment!", lastReply.com)
        assertEquals(".jpg", lastReply.ext)
        assertEquals("https://endchan.net/.media/clip.jpg", lastReply.imageUrl)
        assertEquals("https://endchan.net/.media/t_clip", lastReply.thumbnailUrl)
    }

    @Test
    fun mapsCatalogAndFullThread() {
        val catalogJson = """
            [{
              "threadId": 1017,
              "subject": "JSON API bugs",
              "markdown": "Parser thread",
              "postCount": 4,
              "fileCount": 1,
              "locked": true,
              "pinned": false,
              "thumb": "/.media/t_thumb"
            }]
        """.trimIndent()
        val catalog = gson.fromJson(catalogJson, Array<EndChanPosting>::class.java).toList()
        val catalogThread = EndChanMapper.catalog(catalog, siteUrl).single()
        assertEquals(1017L, catalogThread.op?.no)
        assertEquals(4, catalogThread.op?.replies)
        assertEquals(1, catalogThread.op?.images)
        assertEquals(1, catalogThread.op?.closed)
        assertEquals("https://endchan.net/.media/t_thumb", catalogThread.thumbnailUrl)
        assertNull(catalogThread.imageUrl)

        val thread = gson.fromJson(
            """
            {
              "threadId": 29048,
              "name": "Reader",
              "subject": "OP",
              "markdown": "hello",
              "files": [{
                "originalName": "clip.webm",
                "path": "/.media/clip.webm",
                "thumb": "/.media/t_clip",
                "mime": "video/webm"
              }],
              "posts": [{
                "postId": 29182,
                "name": "Tardus",
                "markdown": "<a class=\"quoteLink\" href=\"/news/res/29048.html#29181\">&gt&gt29181</a>",
                "files": []
              }]
            }
            """.trimIndent(),
            EndChanPosting::class.java,
        )
        val posts = EndChanMapper.thread(thread, siteUrl)
        assertEquals(2, posts.size)
        assertEquals(29048L, posts[0].op?.no)
        assertEquals(0L, posts[0].op?.resto)
        assertEquals(1, posts[0].op?.replies)
        assertEquals(".webm", posts[0].op?.ext)
        assertEquals("video/webm", posts[0].op?.mime)
        assertEquals(29182L, posts[1].op?.no)
        assertEquals(29048L, posts[1].op?.resto)
        assertTrue(posts[1].op?.com!!.contains("quoteLink"))
        assertNull(posts[1].imageUrl)
    }

    @Test
    fun absoluteUrlAndExtensionHelpers() {
        assertEquals(
            "https://endchan.net/.media/file.jpg",
            EndChanMapper.absoluteUrl(siteUrl, "/.media/file.jpg"),
        )
        assertEquals(
            "https://cdn.example/a.png",
            EndChanMapper.absoluteUrl(siteUrl, "https://cdn.example/a.png"),
        )
        assertNull(EndChanMapper.absoluteUrl(siteUrl, null))
        assertEquals(".png", EndChanMapper.extensionOf("rubble.png", null))
        assertEquals(".jpg", EndChanMapper.extensionOf(null, "/.media/hash.jpg"))
        assertNull(EndChanMapper.extensionOf(null, "/.media/t_hash"))
        assertFalse(com.mk.mobilechan.data.ChanMedia.isVideo(".png"))
        assertTrue(com.mk.mobilechan.data.ChanMedia.isVideo(".webm"))
        assertTrue(com.mk.mobilechan.data.ChanMedia.isVideo(null, "video/mp4"))
    }
}
