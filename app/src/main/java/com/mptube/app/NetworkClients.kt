package com.mptube.app

import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClients
{
    private val baseClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    val youtube: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        baseClient.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val serpApi: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        baseClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .build()
    }

    val suggestions: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
    {
        baseClient.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .build()
    }

    val gson: Gson by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Gson() }
}
