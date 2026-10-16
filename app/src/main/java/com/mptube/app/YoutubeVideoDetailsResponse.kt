package com.mptube.app

data class YoutubeVideoDetailsResponse(
    val items: List<YoutubeVideoDetailsItem>
)

data class YoutubeVideoDetailsItem(
    val snippet: YoutubeVideoDetailsSnippet
)

data class YoutubeVideoDetailsSnippet(
    val categoryId: String,
    val title: String
)
