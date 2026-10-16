package com.mptube.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mptube.app.ui.components.AppHeader
import com.mptube.app.ui.components.SearchStateMessage
import com.mptube.app.ui.components.VideoItem
import com.mptube.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlayerFullscreenChanged: (Boolean) -> Unit = {},
    onGoHome: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state = viewModel.uiState
    val listState = rememberLazyListState()

    LaunchedEffect(state.listVersion) {
        if (
            state.listVersion > 0 &&
            state.videos.isNotEmpty()
        ) {
            listState.scrollToItem(0)
        }
    }

    state.selectedVideo?.let { selected ->
        PlayerScreen(
            video = selected,
            onBack = viewModel::closePlayer,
            onGoHome = {
                viewModel.closePlayer()
                onGoHome()
            },
            onFullscreenChanged = onPlayerFullscreenChanged
        )
        return
    }

    BackHandler(
        enabled = state.searchOpen || state.hasSearched
    ) {
        if (state.searchOpen) {
            viewModel.setSearchOpen(false)
        } else {
            viewModel.clearToHome()
        }
    }

    val isHomeEmpty =
        !state.hasSearched &&
                !state.isSearching &&
                state.videos.isEmpty() &&
                state.searchError == null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isHomeEmpty) {
                AppHeader(
                    searchText = state.searchText,
                    onSearchTextChange = viewModel::setSearchText,
                    onSearch = viewModel::search,
                    searchOpen = state.searchOpen,
                    onSearchOpenChange = viewModel::setSearchOpen,
                    suggestions = state.suggestions,
                    onSuggestionClick = viewModel::selectSuggestion,
                    centerSearch = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppHeader(
                        searchText = state.searchText,
                        onSearchTextChange = viewModel::setSearchText,
                        onSearch = viewModel::search,
                        searchOpen = state.searchOpen,
                        onSearchOpenChange = viewModel::setSearchOpen,
                        suggestions = state.suggestions,
                        onSuggestionClick = viewModel::selectSuggestion,
                        centerSearch = false
                    )

                    when {
                        state.isSearching -> {
                            SearchStateMessage(
                                title = "Đang tìm kiếm...",
                                message = "Đang tìm kiếm video..."
                            )
                        }

                        state.searchError != null &&
                                state.videos.isEmpty() -> {
                            SearchStateMessage(
                                title = "Không thể tìm video",
                                message = state.searchError,
                                actionText = "Thử lại",
                                onAction = viewModel::retry
                            )
                        }

                        state.hasSearched &&
                                state.videos.isEmpty() -> {
                            SearchStateMessage(
                                title = "Không tìm thấy video",
                                message = "Thử một từ khóa khác."
                            )
                        }

                        state.videos.isNotEmpty() -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement =
                                    Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = state.videos,
                                    key = { it.videoId },
                                    contentType = { "video" }
                                ) { video ->
                                    VideoItem(
                                        video = video,
                                        onClick = {
                                            viewModel.openVideo(video)
                                        }
                                    )
                                }

                                if (
                                    state.nextPageToken != null &&
                                    state.videos.size <
                                    HomeViewModel.MAX_VISIBLE_RESULTS
                                ) {
                                    item(
                                        key = "load_more",
                                        contentType = "action"
                                    ) {
                                        Text(
                                            text =
                                                if (state.isLoadingMore)
                                                    "Đang tải..."
                                                else
                                                    "Tải thêm video",
                                            textAlign = TextAlign.Center,
                                            style =
                                                MaterialTheme.typography.titleMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    enabled =
                                                        !state.isLoadingMore
                                                ) {
                                                    viewModel.loadMore()
                                                }
                                                .padding(16.dp)
                                        )
                                    }
                                }

                                if (
                                    state.searchError != null &&
                                    state.videos.isNotEmpty()
                                ) {
                                    item(
                                        key = "load_more_error",
                                        contentType = "message"
                                    ) {
                                        SearchStateMessage(
                                            title = "Không tải thêm được",
                                            message = state.searchError,
                                            actionText = "Thử lại",
                                            onAction = viewModel::loadMore
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}