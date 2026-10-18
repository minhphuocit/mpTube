package com.mptube.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SerpApi {
    private const val BASE_URL = "https://mptube-api.phuocnsm2695.workers.dev/"

    val service: SerpApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(NetworkClients.serpApi)
            .addConverterFactory(GsonConverterFactory.create(NetworkClients.gson))
            .build()
            .create(SerpApiService::class.java)
    }
}
