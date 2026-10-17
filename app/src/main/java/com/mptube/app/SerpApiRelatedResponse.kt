package com.mptube.app

import com.google.gson.annotations.SerializedName

data class SerpApiRelatedResponse(
    @SerializedName("source") val source: String = "",
    @SerializedName("videos") val videos: List<SerpApiRelatedVideo> = emptyList()
)

data class SerpApiRelatedVideo(
    @SerializedName("videoId") val videoId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("channel") val channel: String = "",
    @SerializedName("duration") val duration: String = ""
)
