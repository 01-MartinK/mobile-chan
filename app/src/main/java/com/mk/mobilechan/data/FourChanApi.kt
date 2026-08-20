package com.mk.mobilechan.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class BoardsResponse(
    val boards: List<Board>,
)

data class CatalogPage(
    val page: Int,
    val threads: List<Post> = emptyList(),
)

data class ThreadResponse(
    val posts: List<Post> = emptyList(),
)

object FourChanMedia {
    fun imageUrl(board: String, tim: Long, ext: String): String =
        "https://i.4cdn.org/$board/$tim$ext"

    fun thumbnailUrl(board: String, tim: Long): String =
        "https://i.4cdn.org/$board/${tim}s.jpg"
}

interface FourChanApi {
    @GET("boards.json")
    suspend fun getBoards(): BoardsResponse

    @GET("{board}/{page}.json")
    suspend fun getIndex(
        @Path("board") board: String,
        @Path("page") page: Int,
    ): IndexPageResponse

    @GET("{board}/catalog.json")
    suspend fun getCatalog(
        @Path("board") board: String,
    ): List<CatalogPage>

    @GET("{board}/thread/{no}.json")
    suspend fun getThread(
        @Path("board") board: String,
        @Path("no") no: Long,
    ): ThreadResponse
}

object FourChanSource : Source {
    override val id: SourceId = SourceId.FOUR_CHAN
    override val siteUrl: String = "https://boards.4chan.org/"

    private const val BASE_URL = "https://a.4cdn.org/"

    val api: FourChanApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(ChanHttp.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FourChanApi::class.java)
    }

    override suspend fun getBoards(): List<Board> = api.getBoards().boards

    override suspend fun getIndex(board: String, page: Int): IndexPageResponse =
        api.getIndex(board, page).withFourChanMedia(board)

    override suspend fun getCatalog(board: String): List<IndexThread> =
        api.getCatalog(board)
            .flatMap { it.threads }
            .map { IndexThread(posts = listOf(it)).withFourChanMedia(board) }

    override suspend fun getThread(board: String, no: Long): List<IndexThread> =
        api.getThread(board, no).posts.map {
            IndexThread(posts = listOf(it)).withFourChanMedia(board)
        }
}

private fun IndexThread.withFourChanMedia(board: String): IndexThread {
    val tim = op?.tim
    val ext = op?.ext
    if (tim == null || ext.isNullOrBlank()) return this
    return copy(
        imageUrl = FourChanMedia.imageUrl(board, tim, ext),
        thumbnailUrl = FourChanMedia.thumbnailUrl(board, tim),
    )
}

private fun IndexPageResponse.withFourChanMedia(board: String) = copy(
    threads = threads.map { it.withFourChanMedia(board) },
)
