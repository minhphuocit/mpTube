package com.mptube.app

data class SerpApiRelatedResponse(
    val source: String = "",
    val videos: List<SerpApiRelatedVideo> = emptyList()
)

data class SerpApiRelatedVideo(
    val videoId: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val channel: String = "",
    val duration: String = ""
)
