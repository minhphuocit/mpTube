package com.mptube.app

import com.mptube.app.model.Video

class SerpApiRepository {
    suspend fun getRelatedVideos(videoId: String): List<Video> {
        return SerpApi.service.getRelatedVideos(videoId)
            .videos
            .asSequence()
            .filter { it.videoId.isNotBlank() && it.videoId != videoId }
            .map {
                Video(
                    videoId = it.videoId,
                    title = it.title,
                    channel = it.channel,
                    thumbnailUrl = it.thumbnail
                )
            }
            .distinctBy { it.videoId }
            .take(20)
            .toList()
    }
}
