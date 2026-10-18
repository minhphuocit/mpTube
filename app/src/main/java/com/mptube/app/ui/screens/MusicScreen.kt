package com.mptube.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mptube.app.R
import com.mptube.app.ui.components.SearchStateMessage
import com.mptube.app.ui.components.VideoItem
import com.mptube.app.ui.util.rememberMpTubeUiMetrics
import com.mptube.app.viewmodel.MusicViewModel

@Composable
private fun MusicBrandHeader()
{
    val metrics = rememberMpTubeUiMetrics()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (metrics.compactLandscape) 16.dp else 24.dp,
                vertical = if (metrics.compactLandscape) 6.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Image(
            painter = painterResource(R.drawable.mptube_logo),
            contentDescription = "mpTube",
            modifier = Modifier.size(
                when {
                    metrics.compactLandscape -> 30.dp
                    metrics.largeControls -> 48.dp
                    else -> 38.dp
                }
            ),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "mp",
            fontSize = when {
                metrics.compactLandscape -> 18.sp
                metrics.largeControls -> 28.sp
                else -> 22.sp
            },
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFFF2020))
                .padding(
                    horizontal = when {
                        metrics.compactLandscape -> 4.dp
                        metrics.largeControls -> 7.dp
                        else -> 5.dp
                    },
                    vertical = 1.dp
                ),
            contentAlignment = Alignment.Center
        )
        {
            Text(
                text = "Tube",
                fontSize = when {
                    metrics.compactLandscape -> 17.sp
                    metrics.largeControls -> 27.sp
                    else -> 21.sp
                },
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onPlayerFullscreenChanged: (Boolean) -> Unit = {},
    onGoHome: () -> Unit = {},
    viewModel: MusicViewModel = viewModel()
)
{
    val state = viewModel.uiState
    val listState = rememberLazyListState()
    val metrics = rememberMpTubeUiMetrics()

    LaunchedEffect(state.listVersion)
    {
        if (state.listVersion > 0 && state.videos.isNotEmpty())
        {
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

    Column(
        modifier = Modifier.fillMaxSize()
    )
    {
        MusicBrandHeader()

        Text(
            text = "HOT TRENDING MUSICS",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = metrics.titleSize),
            modifier = Modifier.padding(
                horizontal = if (metrics.compactLandscape) 16.dp else 24.dp,
                vertical = if (metrics.compactLandscape) 4.dp else 8.dp
            )
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        )
        {
            when
            {
                state.isLoading && state.videos.isEmpty() ->
                {
                    SearchStateMessage(
                        title = "Đang tải...",
                        message = "Đang tải nhạc thịnh hành,,,"
                    )
                }

                state.errorMessage != null && state.videos.isEmpty() ->
                {
                    SearchStateMessage(
                        title = "Không thể tải nhạc",
                        message = state.errorMessage,
                        actionText = "Thử lại",
                        onAction = viewModel::retry
                    )
                }

                state.videos.isEmpty() ->
                {
                    SearchStateMessage(
                        title = "Chưa có dữ liệu",
                        message = "Kéo xuống để tải lại."
                    )
                }

                else ->
                {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    {
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
                            state.videos.size < MusicViewModel.MAX_VISIBLE_RESULTS
                        )
                        {
                            item(
                                key = "music_load_more",
                                contentType = "action"
                            )
                            {
                                Text(
                                    text = if (state.isLoadingMore)
                                    {
                                        "Đang tải..."
                                    }
                                    else
                                    {
                                        "Tải thêm video"
                                    },
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = !state.isLoadingMore
                                        )
                                        {
                                            viewModel.loadMore()
                                        }
                                        .padding(16.dp)
                                )
                            }
                        }

                        if (
                            state.errorMessage != null &&
                            state.videos.isNotEmpty()
                        )
                        {
                            item(
                                key = "music_error",
                                contentType = "message"
                            )
                            {
                                SearchStateMessage(
                                    title = "Không tải thêm được",
                                    message = state.errorMessage,
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