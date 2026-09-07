package com.mptube.app

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object YoutubeApi {

    private const val BASE_URL =
        "https://www.googleapis.com/"

    private val httpClient =
        OkHttpClient.Builder()

            // Thời gian tối đa chờ kết nối tới Google
            .connectTimeout(
                10,
                TimeUnit.SECONDS
            )

            // Thời gian tối đa chờ server trả dữ liệu
            .readTimeout(
                15,
                TimeUnit.SECONDS
            )

            // Thời gian tối đa gửi request
            .writeTimeout(
                15,
                TimeUnit.SECONDS
            )

            // Tổng thời gian tối đa của request
            .callTimeout(
                20,
                TimeUnit.SECONDS
            )

            .build()

    val service: YoutubeService by lazy {

        Retrofit.Builder()

            .baseUrl(
                BASE_URL
            )

            .client(
                httpClient
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(
                YoutubeService::class.java
            )
    }
}