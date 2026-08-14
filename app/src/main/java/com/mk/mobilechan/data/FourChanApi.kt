package com.mk.mobilechan.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class Board(
    val board: String,
    val title: String,
    val pages: Int = 10,
)

data class BoardsResponse(
    val boards: List<Board>,
)

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
    val replies: Int = 0,
    val images: Int = 0,
    val omitted_posts: Int = 0,
    val sticky: Int = 0,
    val closed: Int = 0,
)

data class IndexThread(
    val posts: List<Post> = emptyList(),
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
) {
    val op: Post? get() = posts.firstOrNull()

    fun withImageUrls(board: String): IndexThread {
        val tim = op?.tim
        val ext = op?.ext
        if (tim == null || ext.isNullOrBlank()) return this
        return copy(
            imageUrl = FourChanMedia.imageUrl(board, tim, ext),
            thumbnailUrl = FourChanMedia.thumbnailUrl(board, tim),
        )
    }
}

data class IndexPageResponse(
    val threads: List<IndexThread> = emptyList(),
) {
    fun withImageUrls(board: String) = copy(
        threads = threads.map { it.withImageUrls(board) },
    )
}

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
}

object FourChanClient {
    private const val BASE_URL = "https://a.4cdn.org/"

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("User-Agent", "MobileChan/1.0")
                        .build()
                )
            }
            .build()
    }

    val api: FourChanApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FourChanApi::class.java)
    }

    suspend fun getIndex(board: String, page: Int): IndexPageResponse =
        api.getIndex(board, page).withImageUrls(board)
}
