package com.mptube.app.download

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object DownloadNetwork
{
    private val baseClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    val extractor: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val stream: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
