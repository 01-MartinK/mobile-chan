package com.mk.mobilechan

import com.google.gson.Gson
import com.mk.mobilechan.data.IndexPageResponse
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import com.mk.mobilechan.data.withFourChanMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FourChanIndexTest {

    private val gson = Gson()

    @Test
    fun parsesAllPostsFromIndexPage() {
        val response = gson.fromJson(INDEX_JSON, IndexPageResponse::class.java)

        assertEquals(2, response.threads.size)
        assertEquals(listOf(570368L, 574933L), response.threads.map { it.op?.no })

        val sticky = response.threads[0]
        assertEquals(3, sticky.posts.size)
        assertEquals(listOf(570368L, 570370L, 570371L), sticky.posts.map { it.no })
        assertEquals(0L, sticky.posts[0].resto)
        assertEquals(570368L, sticky.posts[1].resto)
        assertEquals("<b>FAQs about papercraft</b>", sticky.posts[1].com)

        val live = response.threads[1]
        assertEquals(6, live.posts.size)
        assertEquals(223, live.op?.omitted_posts)
        assertEquals("this is a comment!", live.posts[2].com)
        assertEquals("This is a comment!", live.posts[3].com)
        assertNull(live.posts[2].filename)
        assertEquals(".jpg", live.posts[4].ext)
    }

    @Test
    fun attachesMediaUrlsToEveryIndexPost() {
        val thread = IndexThread(
            posts = listOf(
                Post(no = 1, tim = 111, ext = ".png"),
                Post(no = 2, resto = 1, com = "this is a comment!"),
                Post(no = 3, resto = 1, tim = 333, ext = ".jpg"),
            ),
        ).withFourChanMedia("po")

        assertEquals("https://i.4cdn.org/po/111.png", thread.imageUrl)
        assertEquals("https://i.4cdn.org/po/111s.jpg", thread.thumbnailUrl)
        assertEquals("https://i.4cdn.org/po/111.png", thread.posts[0].imageUrl)
        assertNull(thread.posts[1].imageUrl)
        assertEquals("https://i.4cdn.org/po/333.jpg", thread.posts[2].imageUrl)
        assertEquals("https://i.4cdn.org/po/333s.jpg", thread.posts[2].thumbnailUrl)

        val replyCard = thread.cardForPost(thread.posts[2])
        assertEquals(3L, replyCard.op?.no)
        assertEquals("https://i.4cdn.org/po/333.jpg", replyCard.imageUrl)
        val textCard = thread.cardForPost(thread.posts[1])
        assertEquals("this is a comment!", textCard.op?.com)
        assertNull(textCard.imageUrl)
    }

    companion object {
        private val INDEX_JSON = """
            {
              "threads": [{
                "posts": [{
                  "no": 570368,
                  "sticky": 1,
                  "resto": 0,
                  "com": "Welcome to /po/",
                  "filename": "yotsuba_folding",
                  "ext": ".png",
                  "tim": 1546293948883,
                  "replies": 2,
                  "images": 2
                }, {
                  "no": 570370,
                  "resto": 570368,
                  "com": "<b>FAQs about papercraft</b>",
                  "filename": "papercraft faq",
                  "ext": ".png",
                  "tim": 1546294496751
                }, {
                  "no": 570371,
                  "resto": 570368,
                  "com": "<b>FAQs about origami</b>"
                }]
              }, {
                "posts": [{
                  "no": 574933,
                  "resto": 0,
                  "com": "Let's start a new thread",
                  "replies": 228,
                  "images": 44,
                  "omitted_posts": 223
                }, {
                  "no": 576173,
                  "resto": 574933,
                  "com": "His twitter or Facebook"
                }, {
                  "no": 576175,
                  "resto": 574933,
                  "com": "this is a comment!"
                }, {
                  "no": 576176,
                  "resto": 574933,
                  "com": "This is a comment!"
                }, {
                  "no": 576178,
                  "resto": 574933,
                  "filename": "photo",
                  "ext": ".jpg",
                  "tim": 1566530744175
                }, {
                  "no": 576181,
                  "resto": 574933,
                  "filename": "another",
                  "ext": ".jpg",
                  "tim": 1566530948220
                }]
              }]
            }
        """.trimIndent()
    }
}
