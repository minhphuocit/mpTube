package com.mptube.app.viewmodel

import android.app.Application
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

data class HomeUiState(
    val searchText: String = "",
    val searchOpen: Boolean = false,
    val videos: List<Video> = emptyList(),
    val searchQuery: String = "",
    val hasSearched: Boolean = false,
    val selectedVideo: Video? = null,
    val nextPageToken: String? = null,
    val isLoadingMore: Boolean = false,
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchError: String? = null,
    val listVersion: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeRepository(application.applicationContext)
    private var contentJob: Job? = null
    private var loadMoreJob: Job? = null
    private var contentRequestId = 0L

    var uiState by mutableStateOf(HomeUiState())
        private set

    fun setSearchText(value: String) {
        uiState = uiState.copy(searchText = value)
    }

    fun setSearchOpen(value: Boolean) {
        uiState = uiState.copy(searchOpen = value)
    }

    fun openVideo(video: Video) {
        uiState = uiState.copy(selectedVideo = video)
    }

    fun closePlayer() {
        uiState = uiState.copy(selectedVideo = null)
    }

    fun clearToHome() {
        contentJob?.cancel()
        loadMoreJob?.cancel()
        contentRequestId++
        uiState = HomeUiState(searchText = uiState.searchText)
    }

    fun retry() {
        val query = uiState.searchQuery.ifBlank { uiState.searchText.trim() }
        if (query.isNotBlank()) search(query, forceRefresh = true)
    }

    fun refresh() {
        val query = uiState.searchQuery
        if (query.isBlank() || uiState.isRefreshing || uiState.isSearching) return
        search(query, forceRefresh = true, refreshing = true)
    }

    fun search(
        rawQuery: String = uiState.searchText,
        forceRefresh: Boolean = false,
        refreshing: Boolean = false
    ) {
        val query = rawQuery.trim().replace(Regex("\\s+"), " ")
        if (query.isEmpty()) return

        contentJob?.cancel()
        loadMoreJob?.cancel()
        val requestId = ++contentRequestId

        contentJob = viewModelScope.launch {
            uiState = uiState.copy(
                isSearching = !refreshing,
                isRefreshing = refreshing,
                isLoadingMore = false,
                searchError = null,
                searchQuery = query,
                searchText = query,
                hasSearched = true,
                videos = if (refreshing) uiState.videos else emptyList(),
                nextPageToken = if (refreshing) uiState.nextPageToken else null,
                listVersion = if (refreshing) uiState.listVersion else uiState.listVersion + 1
            )

            try {
                val response = repository.searchVideos(
                    query = query,
                    maxResults = SEARCH_PAGE_SIZE,
                    forceRefresh = forceRefresh
                )

                if (requestId != contentRequestId) return@launch

                val videos = response.items.mapNotNull { item ->
                    val id = item.id.videoId
                    if (id.isBlank()) null else Video(
                        videoId = id,
                        title = item.snippet.title,
                        channel = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.medium.url
                    )
                }.distinctBy { it.videoId }

                uiState = uiState.copy(
                    videos = videos,
                    nextPageToken = response.nextPageToken,
                    searchError = null,
                    listVersion = if (refreshing) uiState.listVersion + 1 else uiState.listVersion
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestId == contentRequestId) {
                    uiState = uiState.copy(
                        videos = if (refreshing) uiState.videos else emptyList(),
                        nextPageToken = if (refreshing) uiState.nextPageToken else null,
                        searchError = getYoutubeErrorMessage(error)
                    )
                }
            } finally {
                if (requestId == contentRequestId) {
                    uiState = uiState.copy(isSearching = false, isRefreshing = false)
                }
            }
        }
    }

    fun loadMore() {
        val token = uiState.nextPageToken ?: return
        val query = uiState.searchQuery
        if (
            query.isBlank() ||
            uiState.isLoadingMore ||
            uiState.isSearching ||
            uiState.isRefreshing ||
            uiState.videos.size >= MAX_VISIBLE_RESULTS
        ) return

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, searchError = null)

            try {
                val response = repository.searchVideos(
                    query = query,
                    pageToken = token,
                    maxResults = SEARCH_PAGE_SIZE
                )

                if (query != uiState.searchQuery) return@launch

                val newVideos = response.items.mapNotNull { item ->
                    val id = item.id.videoId
                    if (id.isBlank()) null else Video(
                        videoId = id,
                        title = item.snippet.title,
                        channel = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.medium.url
                    )
                }

                val merged = (uiState.videos + newVideos)
                    .distinctBy { it.videoId }
                    .take(MAX_VISIBLE_RESULTS)

                uiState = uiState.copy(
                    videos = merged,
                    nextPageToken = if (merged.size >= MAX_VISIBLE_RESULTS) null else response.nextPageToken,
                    searchError = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (query == uiState.searchQuery) {
                    uiState = uiState.copy(searchError = getYoutubeErrorMessage(error))
                }
            } finally {
                if (query == uiState.searchQuery) {
                    uiState = uiState.copy(isLoadingMore = false)
                }
            }
        }
    }

    companion object {
        const val SEARCH_PAGE_SIZE = 25
        const val MAX_VISIBLE_RESULTS = 50
    }
}
