package com.mptube.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mptube.app.YoutubeRepository
import com.mptube.app.domain.AutoplayPolicy
import com.mptube.app.model.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PlayerUiState(
    val initialVideoId: String? = null,
    val currentVideo: Video? = null,
    val relatedVideos: List<Video> = emptyList(),
    val relatedLoading: Boolean = false,
    val relatedLoaded: Boolean = false,
    val waitingForRelated: Boolean = false,
    val playedVideoIds: Set<String> = emptySet(),
    val autoplayCount: Int = 0,
    val autoplayFinished: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeRepository(application.applicationContext)
    private var relatedJob: Job? = null

    var uiState by mutableStateOf(PlayerUiState())
        private set

    fun start(video: Video) {
        relatedJob?.cancel()
        uiState = PlayerUiState(
            initialVideoId = video.videoId,
            currentVideo = video,
            playedVideoIds = setOf(video.videoId),
            autoplayCount = 1
        )
    }


    fun stop() {
        relatedJob?.cancel()
        relatedJob = null
    }

    fun onProgress(currentSecond: Float, duration: Float) {
        if (duration <= 0f || currentSecond < 0f) return
        if (currentSecond / duration >= RELATED_PREFETCH_PROGRESS) ensureRelatedLoaded()
    }

    fun onVideoEnded() {
        val state = uiState
        val next = AutoplayPolicy.pickNext(
            relatedVideos = state.relatedVideos,
            playedVideoIds = state.playedVideoIds,
            autoplayCount = state.autoplayCount
        )

        when {
            next != null -> advanceTo(next)
            state.relatedLoading -> uiState = state.copy(waitingForRelated = true)
            !state.relatedLoaded -> {
                uiState = state.copy(waitingForRelated = true)
                ensureRelatedLoaded()
            }
            else -> uiState = state.copy(
                waitingForRelated = false,
                autoplayFinished = true
            )
        }
    }

    private fun advanceTo(video: Video) {
        val current = uiState
        uiState = current.copy(
            currentVideo = video,
            relatedVideos = emptyList(),
            relatedLoading = false,
            relatedLoaded = false,
            waitingForRelated = false,
            playedVideoIds = current.playedVideoIds + video.videoId,
            autoplayCount = current.autoplayCount + 1,
            autoplayFinished = false
        )
    }

    private fun ensureRelatedLoaded() {
        val state = uiState
        val video = state.currentVideo ?: return
        if (state.relatedLoaded || state.relatedLoading || state.autoplayFinished) return
        if (state.autoplayCount >= AutoplayPolicy.MAX_VIDEOS) {
            uiState = state.copy(autoplayFinished = true)
            return
        }

        uiState = state.copy(relatedLoading = true)
        relatedJob?.cancel()
        relatedJob = viewModelScope.launch {
            try {
                val response = repository.getRelatedVideos(
                    videoId = video.videoId,
                    videoTitle = video.title
                )

                if (uiState.currentVideo?.videoId != video.videoId) return@launch

                val related = response.items.mapNotNull { item ->
                    val id = item.id.videoId
                    if (id.isBlank()) null else Video(
                        videoId = id,
                        title = item.snippet.title,
                        channel = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.medium.url
                    )
                }.distinctBy { it.videoId }

                val updated = uiState.copy(
                    relatedVideos = related,
                    relatedLoading = false,
                    relatedLoaded = true
                )
                uiState = updated

                if (updated.waitingForRelated) {
                    val next = AutoplayPolicy.pickNext(
                        relatedVideos = related,
                        playedVideoIds = updated.playedVideoIds,
                        autoplayCount = updated.autoplayCount
                    )

                    if (next != null) {
                        advanceTo(next)
                    } else {
                        uiState = uiState.copy(
                            waitingForRelated = false,
                            autoplayFinished = true
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (uiState.currentVideo?.videoId != video.videoId) return@launch
                uiState = uiState.copy(
                    relatedVideos = emptyList(),
                    relatedLoading = false,
                    relatedLoaded = true,
                    waitingForRelated = false,
                    autoplayFinished = uiState.waitingForRelated
                )
            }
        }
    }

    companion object {
        private const val RELATED_PREFETCH_PROGRESS = 0.82f
    }
}
