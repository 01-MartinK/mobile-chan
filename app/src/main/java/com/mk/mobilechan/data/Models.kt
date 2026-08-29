package com.mk.mobilechan.data

data class Board(
    val board: String,
    val title: String,
    val pages: Int = 10,
    val ws_board: Int = 1,
) {
    val isNsfw: Boolean get() = ws_board == 0
}

data class Post(
    val no: Long,
    val resto: Long = 0,
    val now: String = "",
    val name: String = "Anonymous",
    val trip: String? = null,
    val sub: String? = null,
    val com: String? = null,
    val tim: Long? = null,
    val filename: String? = null,
    val ext: String? = null,
    val mime: String? = null,
    val replies: Int = 0,
    val images: Int = 0,
    val omitted_posts: Int = 0,
    val sticky: Int = 0,
    val closed: Int = 0,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
) {
    val threadNo: Long get() = if (resto != 0L) resto else no
}

data class IndexThread(
    val posts: List<Post> = emptyList(),
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
) {
    val op: Post? get() = posts.firstOrNull()

    fun cardForPost(post: Post): IndexThread {
        val isOp = post.no == op?.no
        return IndexThread(
            posts = listOf(post),
            imageUrl = post.imageUrl ?: imageUrl.takeIf { isOp },
            thumbnailUrl = post.thumbnailUrl ?: thumbnailUrl.takeIf { isOp },
        )
    }
}

data class IndexPageResponse(
    val threads: List<IndexThread> = emptyList(),
    val pageCount: Int? = null,
)

object ChanMedia {
    fun isVideo(ext: String?, mime: String? = null): Boolean {
        if (mime?.lowercase()?.startsWith("video/") == true) return true
        val value = ext?.lowercase() ?: return false
        return value == ".webm" || value == ".mp4"
    }

    fun mimeType(ext: String?, mime: String? = null): String? {
        mime?.takeIf { it.isNotBlank() }?.let { return it }
        return when (ext?.lowercase()) {
            ".jpg", ".jpeg" -> "image/jpeg"
            ".png" -> "image/png"
            ".gif" -> "image/gif"
            ".webp" -> "image/webp"
            ".webm" -> "video/webm"
            ".mp4" -> "video/mp4"
            else -> null
        }
    }
}
