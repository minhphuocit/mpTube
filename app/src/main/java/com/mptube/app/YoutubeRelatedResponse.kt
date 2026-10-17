package com.mptube.app

import com.google.gson.annotations.SerializedName

data class YoutubeRelatedResponse(
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("items") val items: List<YoutubeSearchItem>
)
