package com.mk.mobilechan.data

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri

object MediaDownloader {
    fun filename(originalName: String?, ext: String?, tim: Long?): String {
        val extension = ext.orEmpty()
        val safeName = originalName
            ?.replace(ILLEGAL_FILENAME_CHARS, "_")
            ?.trim('.', ' ')
            ?.take(MAX_FILENAME_LENGTH)
            ?.ifBlank { null }
        val base = when {
            safeName != null && tim != null -> "${safeName}_$tim"
            safeName != null -> safeName
            tim != null -> tim.toString()
            else -> "media"
        }
        return "$base$extension"
    }

    fun download(context: Context, thread: IndexThread): Boolean {
        val url = thread.imageUrl ?: return false
        val op = thread.op
        return enqueue(
            context = context,
            url = url,
            filename = filename(op?.filename, op?.ext, op?.tim),
            mimeType = FourChanMedia.mimeType(op?.ext),
        )
    }

    private fun enqueue(
        context: Context,
        url: String,
        filename: String,
        mimeType: String?,
    ): Boolean {
        val manager = context.getSystemService(DownloadManager::class.java) ?: return false
        val request = DownloadManager.Request(url.toUri())
            .setTitle(filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .addRequestHeader("User-Agent", FourChanClient.USER_AGENT)
            .addRequestHeader("Referer", REFERER)
        mimeType?.let { request.setMimeType(it) }
        return try {
            manager.enqueue(request)
            true
        } catch (_: Exception) {
            false
        }
    }

    private const val REFERER = "https://boards.4chan.org/"
    private const val MAX_FILENAME_LENGTH = 80
    private val ILLEGAL_FILENAME_CHARS = Regex("""[\\/:*?"<>|]""")
}
