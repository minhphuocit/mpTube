package com.mptube.app.domain

import com.mptube.app.model.Video

object AutoplayPolicy {
    const val MAX_VIDEOS = 10

    fun pickNext(
        relatedVideos: List<Video>,
        playedVideoIds: Set<String>,
        autoplayCount: Int
    ): Video? {
        if (autoplayCount >= MAX_VIDEOS) return null
        return relatedVideos.firstOrNull { it.videoId !in playedVideoIds }
    }
}
