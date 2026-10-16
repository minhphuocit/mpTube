package com.mptube.app

data class YoutubeTrendingResponse(
    val nextPageToken: String? = null,
    val items: List<YoutubeTrendingItem> = emptyList()
)

data class YoutubeTrendingItem(
    val id: String,
    val snippet: YoutubeTrendingSnippet
)

data class YoutubeTrendingSnippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: YoutubeTrendingThumbnails
)

data class YoutubeTrendingThumbnails(
    val default: YoutubeThumbnail,
    val medium: YoutubeThumbnail,
    val high: YoutubeThumbnail
)
