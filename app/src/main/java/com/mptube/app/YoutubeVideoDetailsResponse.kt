package com.mptube.app

import com.google.gson.annotations.SerializedName

data class YoutubeVideoDetailsResponse(
    @SerializedName("items") val items: List<YoutubeVideoDetailsItem>
)

data class YoutubeVideoDetailsItem(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: YoutubeVideoDetailsSnippet
)

data class YoutubeVideoDetailsSnippet(
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("title") val title: String
)
