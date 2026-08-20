package com.mk.mobilechan.data

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EndChanBoard(
    val boardUri: String = "",
    val boardName: String = "",
    val inactive: Boolean = false,
)

data class EndChanBoardsData(
    val pageCount: Int = 1,
    val boards: List<EndChanBoard>? = null,
)

data class EndChanBoardsResponse(
    val status: String? = null,
    val data: EndChanBoardsData? = null,
    val pageCount: Int = 1,
    val boards: List<EndChanBoard>? = null,
) {
    val allBoards: List<EndChanBoard> get() = data?.boards ?: boards.orEmpty()
    val pages: Int get() = data?.pageCount ?: pageCount
}

data class EndChanFile(
    val originalName: String? = null,
    val path: String? = null,
    val thumb: String? = null,
    val mime: String? = null,
)

data class EndChanPosting(
    val threadId: Long = 0,
    val postId: Long = 0,
    val name: String? = null,
    val subject: String? = null,
    val markdown: String? = null,
    val message: String? = null,
    val creation: String? = null,
    val lastBump: String? = null,
    val locked: Boolean = false,
    val pinned: Boolean = false,
    val files: List<EndChanFile>? = null,
    val posts: List<EndChanPosting>? = null,
    @SerializedName(value = "omittedPosts", alternate = ["ommitedPosts"])
    val omittedPosts: Int = 0,
    val omittedFiles: Int = 0,
    val postCount: Int = 0,
    val fileCount: Int = 0,
    val thumb: String? = null,
    val mime: String? = null,
)

data class EndChanIndexResponse(
    val pageCount: Int = 1,
    val threads: List<EndChanPosting> = emptyList(),
)

interface EndChanApi {
    @GET("boards.js")
    suspend fun getBoards(
        @Query("json") json: Int = 1,
        @Query("page") page: Int = 1,
    ): EndChanBoardsResponse

    @GET("{board}/{page}.json")
    suspend fun getIndex(
        @Path("board") board: String,
        @Path("page") page: Int,
    ): EndChanIndexResponse

    @GET("{board}/catalog.json")
    suspend fun getCatalog(
        @Path("board") board: String,
    ): List<EndChanPosting>

    @GET("{board}/res/{no}.json")
    suspend fun getThread(
        @Path("board") board: String,
        @Path("no") no: Long,
    ): EndChanPosting
}

object EndChanMapper {
    fun boards(dtos: List<EndChanBoard>): List<Board> =
        dtos.mapNotNull { dto ->
            val uri = dto.boardUri.trim()
            if (uri.isEmpty()) return@mapNotNull null
            Board(
                board = uri,
                title = dto.boardName.ifBlank { uri },
            )
        }

    fun allBoards(pages: List<EndChanBoardsResponse>): List<Board> =
        boards(
            pages.flatMap { it.allBoards }.distinctBy { it.boardUri.lowercase() },
        )

    fun index(response: EndChanIndexResponse, siteUrl: String): IndexPageResponse =
        IndexPageResponse(
            threads = response.threads.map { posting ->
                toIndexThread(
                    posting = posting,
                    siteUrl = siteUrl,
                    replies = replyCount(posting),
                    images = imageCount(posting),
                    omitted = posting.omittedPosts,
                )
            },
            pageCount = response.pageCount,
        )

    fun catalog(items: List<EndChanPosting>, siteUrl: String): List<IndexThread> =
        items.map { posting ->
            toIndexThread(
                posting = posting,
                siteUrl = siteUrl,
                replies = posting.postCount,
                images = posting.fileCount,
            )
        }

    fun thread(thread: EndChanPosting, siteUrl: String): List<IndexThread> {
        val replies = thread.posts.orEmpty()
        val op = toIndexThread(
            posting = thread,
            siteUrl = siteUrl,
            resto = 0,
            replies = if (thread.postCount > 0) thread.postCount else replies.size,
            images = if (thread.fileCount > 0) {
                thread.fileCount
            } else {
                replies.count { !it.files.isNullOrEmpty() }
            },
        )
        return listOf(op) + replies.map { reply ->
            toIndexThread(
                posting = reply,
                siteUrl = siteUrl,
                resto = thread.threadId,
            )
        }
    }

    fun toIndexThread(
        posting: EndChanPosting,
        siteUrl: String,
        resto: Long = 0,
        replies: Int = 0,
        images: Int = 0,
        omitted: Int = 0,
    ): IndexThread {
        val (imageUrl, thumbnailUrl) = mediaUrls(posting, siteUrl)
        return IndexThread(
            posts = listOf(toPost(posting, resto, replies, images, omitted)),
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
        )
    }

    fun toPost(
        posting: EndChanPosting,
        resto: Long = 0,
        replies: Int = 0,
        images: Int = 0,
        omitted: Int = 0,
    ): Post {
        val file = posting.files.orEmpty().firstOrNull()
        val comment = posting.markdown?.takeIf { it.isNotBlank() }
            ?: posting.message?.replace("\n", "<br>")
        return Post(
            no = if (posting.postId != 0L) posting.postId else posting.threadId,
            resto = resto,
            now = formatTimestamp(posting.creation ?: posting.lastBump),
            name = posting.name?.takeIf { it.isNotBlank() } ?: "Anonymous",
            sub = posting.subject?.takeIf { it.isNotBlank() },
            com = comment,
            filename = file?.originalName,
            ext = extensionOf(file?.originalName, file?.path),
            mime = file?.mime ?: posting.mime,
            replies = replies,
            images = images,
            omitted_posts = omitted,
            sticky = if (posting.pinned) 1 else 0,
            closed = if (posting.locked) 1 else 0,
        )
    }

    fun absoluteUrl(siteUrl: String, path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val origin = siteUrl.trimEnd('/')
        return if (path.startsWith("/")) "$origin$path" else "$origin/$path"
    }

    fun formatTimestamp(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            TIMESTAMP_FORMAT.format(Instant.parse(iso))
        } catch (_: Exception) {
            iso
        }
    }

    fun extensionOf(originalName: String?, path: String?): String? {
        val source = originalName ?: path ?: return null
        val dot = source.lastIndexOf('.')
        if (dot < 0 || dot == source.lastIndex) return null
        val ext = source.substring(dot).lowercase()
        return ext.takeIf { it.length in 2..8 }
    }

    private fun mediaUrls(posting: EndChanPosting, siteUrl: String): Pair<String?, String?> {
        val file = posting.files.orEmpty().firstOrNull()
        val imageUrl = absoluteUrl(siteUrl, file?.path)
        val thumbnailUrl = absoluteUrl(siteUrl, file?.thumb ?: posting.thumb) ?: imageUrl
        return imageUrl to thumbnailUrl
    }

    private fun replyCount(posting: EndChanPosting): Int =
        if (posting.postCount > 0) {
            posting.postCount
        } else {
            posting.omittedPosts + posting.posts.orEmpty().size
        }

    private fun imageCount(posting: EndChanPosting): Int =
        if (posting.fileCount > 0) posting.fileCount else posting.omittedFiles

    private val TIMESTAMP_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM/dd/yy(EEE)HH:mm:ss", Locale.US)
            .withZone(ZoneOffset.UTC)
}

object EndChanSource : Source {
    override val id: SourceId = SourceId.END_CHAN
    override val siteUrl: String = "https://endchan.net/"

    private const val BOARD_FETCH_CONCURRENCY = 8

    val api: EndChanApi by lazy {
        Retrofit.Builder()
            .baseUrl(siteUrl)
            .client(ChanHttp.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EndChanApi::class.java)
    }

    override suspend fun getBoards(): List<Board> {
        val first = api.getBoards(page = 1)
        val remaining = if (first.pages > 1) {
            val semaphore = Semaphore(BOARD_FETCH_CONCURRENCY)
            coroutineScope {
                (2..first.pages).map { page ->
                    async {
                        semaphore.withPermit { api.getBoards(page = page) }
                    }
                }.awaitAll()
            }
        } else {
            emptyList()
        }
        return EndChanMapper.allBoards(listOf(first) + remaining)
    }

    override suspend fun getIndex(board: String, page: Int): IndexPageResponse =
        EndChanMapper.index(api.getIndex(board, page), siteUrl)

    override suspend fun getCatalog(board: String): List<IndexThread> =
        EndChanMapper.catalog(api.getCatalog(board), siteUrl)

    override suspend fun getThread(board: String, no: Long): List<IndexThread> =
        EndChanMapper.thread(api.getThread(board, no), siteUrl)
}
