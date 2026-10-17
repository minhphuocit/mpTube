package com.mptube.app

import com.google.gson.annotations.SerializedName

data class YoutubeTrendingResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<YoutubeTrendingItem> = emptyList()
)

data class YoutubeTrendingItem(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: YoutubeTrendingSnippet
)

data class YoutubeTrendingSnippet(
    @SerializedName("title") val title: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("thumbnails") val thumbnails: YoutubeTrendingThumbnails
)

data class YoutubeTrendingThumbnails(
    @SerializedName("default") val default: YoutubeThumbnail,
    @SerializedName("medium") val medium: YoutubeThumbnail,
    @SerializedName("high") val high: YoutubeThumbnail
)
