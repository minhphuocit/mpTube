package com.mptube.app

import com.google.gson.annotations.SerializedName

data class YoutubeSearchResponse(
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("items") val items: List<YoutubeSearchItem>
)

data class YoutubeSearchItem(
    @SerializedName("id") val id: YoutubeVideoId,
    @SerializedName("snippet") val snippet: YoutubeSnippet
)

data class YoutubeVideoId(
    @SerializedName("videoId") val videoId: String
)

data class YoutubeSnippet(
    @SerializedName("title") val title: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("thumbnails") val thumbnails: YoutubeThumbnails
)

data class YoutubeThumbnails(
    @SerializedName("default") val default: YoutubeThumbnail,
    @SerializedName("medium") val medium: YoutubeThumbnail,
    @SerializedName("high") val high: YoutubeThumbnail
)

data class YoutubeThumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)
