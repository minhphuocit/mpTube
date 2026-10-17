package com.mptube.app

import retrofit2.http.GET
import retrofit2.http.Query

interface SerpApiService {
    @GET("related")
    suspend fun getRelatedVideos(
        @Query("v") videoId: String
    ): SerpApiRelatedResponse
}
