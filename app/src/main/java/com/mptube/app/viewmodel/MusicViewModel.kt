package com.mptube.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mptube.app.YoutubeRepository
import com.mptube.app.model.Video
import com.mptube.app.ui.util.getYoutubeErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Immutable
data class MusicUiState(
    val videos: List<Video> = emptyList(),
    val selectedVideo: Video? = null,
    val nextPageToken: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val listVersion: Int = 0
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeRepository(application.applicationContext)
    private var contentJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestId = 0L

    var uiState by mutableStateOf(MusicUiState())
        private set

    init {
        loadInitial()
    }

    fun openVideo(video: Video) {
        uiState = uiState.copy(selectedVideo = video)
    }

    fun closePlayer() {
        uiState = uiState.copy(selectedVideo = null)
    }

    fun retry() = loadInitial(forceRefresh = true)

    fun refresh() {
        if (uiState.isRefreshing || uiState.isLoading) return
        loadInitial(forceRefresh = true, refreshing = true)
    }

    fun loadMore() {
        val token = uiState.nextPageToken ?: return
        if (
            uiState.isLoadingMore ||
            uiState.isLoading ||
            uiState.isRefreshing ||
            uiState.videos.size >= MAX_VISIBLE_RESULTS
        ) return

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, errorMessage = null)
            try {
                val response = repository.getTrendingMusic(
                    pageToken = token,
                    maxResults = PAGE_SIZE
                )

                val newVideos = response.items.mapNotNull { item ->
                    if (item.id.isBlank()) null else Video(
                        videoId = item.id,
                        title = item.snippet.title,
                        channel = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.medium.url,
                        categoryId = MUSIC_CATEGORY_ID
                    )
                }

                val merged = (uiState.videos + newVideos)
                    .distinctBy { it.videoId }
                    .take(MAX_VISIBLE_RESULTS)

                uiState = uiState.copy(
                    videos = merged,
                    nextPageToken = if (merged.size >= MAX_VISIBLE_RESULTS) null else response.nextPageToken,
                    errorMessage = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                uiState = uiState.copy(errorMessage = getYoutubeErrorMessage(error))
            } finally {
                uiState = uiState.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadInitial(forceRefresh: Boolean = false, refreshing: Boolean = false) {
        contentJob?.cancel()
        loadMoreJob?.cancel()
        val currentRequest = ++requestId

        contentJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = !refreshing,
                isRefreshing = refreshing,
                isLoadingMore = false,
                errorMessage = null
            )

            try {
                val response = repository.getTrendingMusic(
                    maxResults = PAGE_SIZE,
                    forceRefresh = forceRefresh
                )

                if (currentRequest != requestId) return@launch

                val videos = response.items.mapNotNull { item ->
                    if (item.id.isBlank()) null else Video(
                        videoId = item.id,
                        title = item.snippet.title,
                        channel = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.medium.url,
                        categoryId = MUSIC_CATEGORY_ID
                    )
                }.distinctBy { it.videoId }

                uiState = uiState.copy(
                    videos = videos,
                    nextPageToken = response.nextPageToken,
                    errorMessage = null,
                    listVersion = uiState.listVersion + 1
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (currentRequest == requestId) {
                    uiState = uiState.copy(
                        videos = if (refreshing) uiState.videos else emptyList(),
                        nextPageToken = if (refreshing) uiState.nextPageToken else null,
                        errorMessage = getYoutubeErrorMessage(error)
                    )
                }
            } finally {
                if (currentRequest == requestId) {
                    uiState = uiState.copy(isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 25
        const val MAX_VISIBLE_RESULTS = 50
        private const val MUSIC_CATEGORY_ID = "10"
    }
}
