package com.mptube.app

data class YoutubeSearchResponse(
    val nextPageToken: String?,
    val items: List<YoutubeSearchItem>
)

data class YoutubeSearchItem(
    val id: YoutubeVideoId,
    val snippet: YoutubeSnippet
)

data class YoutubeVideoId(
    val videoId: String
)

data class YoutubeSnippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: YoutubeThumbnails
)

data class YoutubeThumbnails(
    val default: YoutubeThumbnail,
    val medium: YoutubeThumbnail,
    val high: YoutubeThumbnail
)
data class YoutubeThumbnail(
    val url: String,
    val width: Int,
    val height: Int
)