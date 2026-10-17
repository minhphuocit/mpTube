package com.mptube.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class Video(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val categoryId: String? = null
)
