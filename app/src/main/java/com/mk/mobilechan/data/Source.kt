package com.mk.mobilechan.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class SourceId {
    FOUR_CHAN,
    END_CHAN,
}

interface Source {
    val id: SourceId
    val siteUrl: String

    suspend fun getBoards(): List<Board>
    suspend fun getIndex(board: String, page: Int): IndexPageResponse
    suspend fun getCatalog(board: String): List<IndexThread>
    suspend fun getThread(board: String, no: Long): List<IndexThread>
}

object Sources {
    fun of(id: SourceId): Source = when (id) {
        SourceId.FOUR_CHAN -> FourChanSource
        SourceId.END_CHAN -> EndChanSource
    }
}

object ChanHttp {
    const val USER_AGENT = "MobileChan/1.0"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build(),
                )
            }
            .build()
    }
}
