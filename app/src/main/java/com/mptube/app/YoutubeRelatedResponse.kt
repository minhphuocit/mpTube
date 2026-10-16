package com.mptube.app

data class YoutubeRelatedResponse(
    val nextPageToken: String?,
    val items: List<YoutubeSearchItem>
)