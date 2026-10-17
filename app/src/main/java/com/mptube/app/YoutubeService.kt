package com.mptube.app

import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeService {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResult: Int = 25,
        @Query("pageToken") pageToken: String? = null,
        @Query("relevanceLanguage") relevanceLanguage: String = "vi",
        @Query("regionCode") regionCode: String = "VN",
        @Query("videoCategoryId") videoCategoryId: String? = null
    ): YoutubeSearchResponse

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet",
        @Query("id") videoIds: String
    ): YoutubeVideoDetailsResponse

    @GET("youtube/v3/videos")
    suspend fun getTrendingVideos(
        @Query("part") part: String = "snippet",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "VN",
        @Query("videoCategoryId") videoCategoryId: String? = null,
        @Query("maxResults") maxResults: Int = 25,
        @Query("pageToken") pageToken: String? = null
    ): YoutubeTrendingResponse
}
