package com.mptube.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mptube.app.R
import com.mptube.app.download.Mp3DownloadManager
import com.mptube.app.model.Video
import com.mptube.app.ui.util.rememberMpTubeUiMetrics
import com.mptube.app.viewmodel.PlayerViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun PlayerBrandHeader(onGoHome: () -> Unit)
{
    val metrics = rememberMpTubeUiMetrics()

    Row(
        modifier = Modifier
            .clickable(onClick = onGoHome)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Image(
            painter = painterResource(R.drawable.mptube_logo),
            contentDescription = "Về Home",
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
                .padding(horizontal = 5.dp, vertical = 1.dp),
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
fun PlayerScreen(
    video: Video,
    onBack: () -> Unit,
    onGoHome: () -> Unit = {},
    onFullscreenChanged: (Boolean) -> Unit = {},
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = context as? LifecycleOwner
    val state = viewModel.uiState
    val currentState = rememberUpdatedState(state)
    val metrics = rememberMpTubeUiMetrics()
    val scope = rememberCoroutineScope()

    var isFullscreen by remember { mutableStateOf(false) }
    var activePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playerState by remember { mutableStateOf(PlayerConstants.PlayerState.UNKNOWN) }
    var lastLoadedVideoId by remember { mutableStateOf<String?>(null) }
    val progressState = remember { PlayerProgressState() }
    var isRefreshing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var previousOrientation by remember {
        mutableIntStateOf(activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }
    var orientationForced by remember { mutableStateOf(false) }

    LaunchedEffect(video.videoId) {
        viewModel.start(video)
    }

    val playerView = remember(context) { YouTubePlayerView(context) }

    DisposableEffect(playerView, lifecycleOwner) {
        lifecycleOwner?.lifecycle?.addObserver(playerView)

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                activePlayer = youTubePlayer
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                progressState.duration = duration
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                progressState.currentSecond = second
                viewModel.onProgress(second, progressState.duration)
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                playerState = state
                if (state == PlayerConstants.PlayerState.ENDED) viewModel.onVideoEnded()
            }
        }

        playerView.addYouTubePlayerListener(listener)

        onDispose {
            activePlayer = null
            lifecycleOwner?.lifecycle?.removeObserver(playerView)
            playerView.release()
        }
    }

    LaunchedEffect(currentState.value.currentVideo?.videoId, activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        val videoId = currentState.value.currentVideo?.videoId ?: return@LaunchedEffect

        if (lastLoadedVideoId != videoId) {
            lastLoadedVideoId = videoId
            progressState.duration = 0f
            progressState.currentSecond = 0f
            downloadMessage = null
            downloadProgress = 0
            player.loadVideo(videoId, 0f)
        }
    }

    LaunchedEffect(isFullscreen) {
        onFullscreenChanged(isFullscreen)
        val currentActivity = activity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(
            currentActivity.window,
            currentActivity.window.decorView
        )

        if (isFullscreen) {
            if (!orientationForced) {
                previousOrientation = currentActivity.requestedOrientation
                orientationForced = true
            }
            currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            if (orientationForced) {
                currentActivity.requestedOrientation = previousOrientation
                orientationForced = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
            onFullscreenChanged(false)
            activity?.let { currentActivity ->
                WindowInsetsControllerCompat(
                    currentActivity.window,
                    currentActivity.window.decorView
                ).show(WindowInsetsCompat.Type.systemBars())

                if (orientationForced) {
                    currentActivity.requestedOrientation = previousOrientation
                }
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    val currentVideo = state.currentVideo ?: video

    fun refreshPlayer() {
        if (isFullscreen || isRefreshing) return
        isRefreshing = true
        val player = activePlayer
        val position = progressState.currentSecond.coerceAtLeast(0f)
        if (player != null) {
            progressState.duration = 0f
            player.loadVideo(currentVideo.videoId, position)
        }
        scope.launch {
            delay(500)
            isRefreshing = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refreshPlayer,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                }
            ) {
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (metrics.compactLandscape) 48.dp else 64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .size(if (metrics.compactLandscape) 48.dp else 64.dp)
                                .clickable(onClick = onBack)
                                .padding(
                                    start = if (metrics.compactLandscape) 12.dp else 16.dp,
                                    top = if (metrics.compactLandscape) 11.dp else 18.dp
                                )
                        )

                        PlayerBrandHeader(onGoHome = onGoHome)
                    }
                }

                BoxWithConstraints(
                    modifier = if (isFullscreen) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                            .align(Alignment.CenterHorizontally)
                            .aspectRatio(VIDEO_ASPECT_RATIO)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    val availableRatio = if (maxHeight.value > 0f) {
                        maxWidth.value / maxHeight.value
                    } else {
                        VIDEO_ASPECT_RATIO
                    }

                    val playerModifier = if (!isFullscreen) {
                        Modifier.fillMaxSize()
                    } else if (availableRatio > VIDEO_ASPECT_RATIO) {
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(VIDEO_ASPECT_RATIO)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(VIDEO_ASPECT_RATIO)
                    }

                    AndroidView(
                        factory = { playerView },
                        modifier = playerModifier.align(Alignment.Center)
                    )

                    if (!isFullscreen && metrics.carLikeLandscape && activePlayer != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.58f))
                                .clickable {
                                    val player = activePlayer ?: return@clickable
                                    if (playerState == PlayerConstants.PlayerState.PLAYING) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playerState == PlayerConstants.PlayerState.PLAYING) {
                                        R.drawable.ic_pause
                                    } else {
                                        R.drawable.ic_play
                                    }
                                ),
                                contentDescription = if (playerState == PlayerConstants.PlayerState.PLAYING) {
                                    "Tạm dừng"
                                } else {
                                    "Phát"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(if (isFullscreen) Alignment.TopEnd else Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(if (!isFullscreen && metrics.compactLandscape) 44.dp else 64.dp)
                            .clickable { isFullscreen = !isFullscreen },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isFullscreen) "✕" else "⛶")
                    }
                }

                if (!isFullscreen) {
                    Text(
                        text = currentVideo.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = metrics.titleSize),
                        modifier = Modifier
                            .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )

                    Text(
                        text = currentVideo.channel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodySize),
                        modifier = Modifier
                            .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::playNext,
                            enabled = !state.autoplayFinished && !state.waitingForRelated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (state.waitingForRelated) "Đang chuyển..." else "Video tiếp theo  ›",
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (isDownloading) return@OutlinedButton
                                isDownloading = true
                                downloadProgress = 0
                                downloadMessage = null

                                scope.launch {
                                    try {
                                        val result = Mp3DownloadManager.download(
                                            context = context.applicationContext,
                                            video = currentVideo,
                                            onProgress = { progress ->
                                                scope.launch { downloadProgress = progress }
                                            }
                                        )
                                        downloadMessage = "Đã lưu: ${result.displayName}"
                                        Toast.makeText(
                                            context,
                                            "Đã tải MP3 vào ${result.location}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } catch (e: Exception) {
                                        val message = e.message ?: "Không tải được MP3"
                                        downloadMessage = message
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            },
                            enabled = !isDownloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isDownloading) "$downloadProgress%" else ".mp3",
                                maxLines = 1
                            )
                        }
                    }

                    downloadMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = metrics.bodySize),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (state.relatedLoading) {
                        Text(
                            text = "Đang chuẩn bị video tiếp theo...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = metrics.bodySize),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (state.autoplayFinished) {
                        Text(
                            text = "Đã phát hết video liên quan",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodySize),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private class PlayerProgressState(
    var currentSecond: Float = 0f,
    var duration: Float = 0f
)

private const val VIDEO_ASPECT_RATIO = 16f / 9f
