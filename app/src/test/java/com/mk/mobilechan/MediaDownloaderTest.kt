package com.mk.mobilechan

import com.mk.mobilechan.data.FourChanMedia
import com.mk.mobilechan.data.MediaDownloader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaDownloaderTest {
    @Test
    fun filenameUsesOriginalNameAndTim() {
        assertEquals(
            "homelab_1786665896682816.jpg",
            MediaDownloader.filename("homelab", ".jpg", 1786665896682816),
        )
    }

    @Test
    fun filenameFallsBackToTim() {
        assertEquals("1786665896682816.webm", MediaDownloader.filename(null, ".webm", 1786665896682816))
    }

    @Test
    fun filenameSanitizesPathCharacters() {
        assertEquals(
            "cool_cat_1.jpg",
            MediaDownloader.filename("cool/cat", ".jpg", 1),
        )
    }

    @Test
    fun mimeTypeMapsCommonExtensions() {
        assertEquals("image/jpeg", FourChanMedia.mimeType(".jpg"))
        assertEquals("video/webm", FourChanMedia.mimeType(".WEBM"))
        assertEquals("video/mp4", FourChanMedia.mimeType(".mp4"))
        assertNull(FourChanMedia.mimeType(null))
    }
}
