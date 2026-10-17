package com.mptube.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mptube.app.SerpApiRepository
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
    private val serpApiRepository = SerpApiRepository()
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
        requestNextVideo()
    }

    fun playNext() {
        requestNextVideo()
    }

    private fun requestNextVideo() {
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
                val related = loadRelatedVideos(video)

                if (uiState.currentVideo?.videoId != video.videoId) return@launch

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

    private suspend fun loadRelatedVideos(video: Video): List<Video> {
        return if (isMusicVideo(video)) {
            loadMusicRelatedVideos(video)
        } else {
            loadFallbackRelatedVideos(video, musicOnly = false)
        }
    }

    private suspend fun isMusicVideo(video: Video): Boolean {
        if (video.categoryId != null) return video.categoryId == MUSIC_CATEGORY_ID

        return try {
            repository.getVideoCategoryId(video.videoId) == MUSIC_CATEGORY_ID
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun loadMusicRelatedVideos(video: Video): List<Video> {
        try {
            val playedIds = uiState.playedVideoIds
            val serpRelated = serpApiRepository.getRelatedVideos(video.videoId)
                .filterNot { it.videoId in playedIds }
            val musicRelated = filterMusicVideos(serpRelated)
            if (musicRelated.isNotEmpty()) return musicRelated
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }

        return loadFallbackRelatedVideos(video, musicOnly = true)
    }

    private suspend fun filterMusicVideos(videos: List<Video>): List<Video> {
        if (videos.isEmpty()) return emptyList()

        val unknownIds = videos
            .filter { it.categoryId == null }
            .map { it.videoId }

        val categories = if (unknownIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                repository.getVideoCategoryIds(unknownIds)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyMap()
            }
        }

        return videos
            .asSequence()
            .filter { video ->
                video.categoryId == MUSIC_CATEGORY_ID || categories[video.videoId] == MUSIC_CATEGORY_ID
            }
            .map { video ->
                if (video.categoryId == MUSIC_CATEGORY_ID) video else video.copy(categoryId = MUSIC_CATEGORY_ID)
            }
            .distinctBy { it.videoId }
            .toList()
    }

    private suspend fun loadFallbackRelatedVideos(video: Video, musicOnly: Boolean): List<Video> {
        val response = repository.getRelatedVideos(
            videoId = video.videoId,
            videoTitle = video.title,
            musicOnly = musicOnly
        )

        return response.items.mapNotNull { item ->
            val id = item.id.videoId
            if (id.isBlank()) null else Video(
                videoId = id,
                title = item.snippet.title,
                channel = item.snippet.channelTitle,
                thumbnailUrl = item.snippet.thumbnails.medium.url,
                categoryId = if (musicOnly) MUSIC_CATEGORY_ID else null
            )
        }.distinctBy { it.videoId }
    }

    companion object {
        private const val RELATED_PREFETCH_PROGRESS = 0.99f
        private const val MUSIC_CATEGORY_ID = "10"
    }
}
