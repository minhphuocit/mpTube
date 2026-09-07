package com.mptube.app

data class YoutubeTrendingResponse(
    val items: List<YoutubeTrendingItem>
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