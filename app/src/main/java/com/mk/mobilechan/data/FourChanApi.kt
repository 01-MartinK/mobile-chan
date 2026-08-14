package com.mk.mobilechan.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Board(
    val board: String,
    val title: String,
)

data class BoardsResponse(
    val boards: List<Board>,
)

interface FourChanApi {
    @GET("boards.json")
    suspend fun getBoards(): BoardsResponse
}

object FourChanClient {
    private const val BASE_URL = "https://a.4cdn.org/"

    val api: FourChanApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
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
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FourChanApi::class.java)
    }
}
