package com.mptube.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
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

@Composable
private fun CarPlayerControls(
    player: YouTubePlayer?,
    playerState: PlayerConstants.PlayerState,
    currentSecondState: State<Float>,
    durationState: State<Float>,
    playbackRate: PlayerConstants.PlaybackRate,
    playbackQualityLabel: String,
    nextEnabled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onPlaybackRateChange: (PlayerConstants.PlaybackRate) -> Unit,
    onNext: () -> Unit,
    onDownload: () -> Unit,
    onFullscreen: () -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var settingsOpen by remember { mutableStateOf(false) }
    val duration = durationState.value.coerceAtLeast(0f)
    val current = if (isSeeking) seekValue else currentSecondState.value.coerceIn(0f, duration.coerceAtLeast(0f))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlayerTime(current),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(42.dp)
                )

                Slider(
                    value = if (duration > 0f) current.coerceIn(0f, duration) else 0f,
                    onValueChange = { value ->
                        isSeeking = true
                        seekValue = value
                    },
                    onValueChangeFinished = {
                        if (duration > 0f) player?.seekTo(seekValue.coerceIn(0f, duration))
                        isSeeking = false
                    },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    enabled = player != null && duration > 0f,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatPlayerTime(duration),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(42.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarControlButton(
                    text = "−10",
                    contentDescription = "Lùi 10 giây"
                ) {
                    player?.seekTo((currentSecondState.value - 10f).coerceAtLeast(0f))
                }

                CarControlButton(
                    text = "+10",
                    contentDescription = "Tiến 10 giây"
                ) {
                    player?.seekTo((currentSecondState.value + 10f).coerceAtMost(duration.coerceAtLeast(currentSecondState.value + 10f)))
                }

                CarControlButton(
                    iconRes = if (playerState == PlayerConstants.PlayerState.PLAYING) R.drawable.ic_pause else R.drawable.ic_play,
                    contentDescription = if (playerState == PlayerConstants.PlayerState.PLAYING) "Tạm dừng" else "Phát"
                ) {
                    if (playerState == PlayerConstants.PlayerState.PLAYING) player?.pause() else player?.play()
                }

                CarControlButton(
                    text = ">>",
                    contentDescription = "Video tiếp theo",
                    enabled = nextEnabled,
                    onClick = onNext
                )

                Box {
                    CarControlButton(
                        iconRes = R.drawable.ic_settings,
                        contentDescription = "Cài đặt phát"
                    ) {
                        settingsOpen = true
                    }

                    DropdownMenu(
                        expanded = settingsOpen,
                        onDismissRequest = { settingsOpen = false }
                    ) {
                        Text(
                            text = "Chất lượng video",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "YouTube tự động • $playbackQualityLabel",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Tốc độ phát",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)
                        )
                        CAR_PLAYBACK_RATES.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (option.rate == playbackRate) "✓ ${option.label}" else option.label
                                    )
                                },
                                onClick = {
                                    settingsOpen = false
                                    player?.setPlaybackRate(option.rate)
                                    onPlaybackRateChange(option.rate)
                                }
                            )
                        }
                    }
                }

                CarControlButton(
                    iconRes = R.drawable.ic_download,
                    smallLabel = if (isDownloading) "$downloadProgress%" else ".mp3",
                    contentDescription = if (isDownloading) "Đang tải MP3" else "Tải MP3",
                    enabled = !isDownloading,
                    onClick = onDownload
                )

                CarControlButton(
                    text = "⛶",
                    contentDescription = "Toàn màn hình",
                    onClick = onFullscreen
                )
            }
        }
    }
}

@Composable
private fun CarControlButton(
    contentDescription: String,
    text: String? = null,
    iconRes: Int? = null,
    smallLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            iconRes != null && smallLabel != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = smallLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            iconRes != null -> Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
            text != null -> Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CarVideoTouchBlocker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerRefreshContainer(
    carPlayerMode: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    if (carPlayerMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

private fun formatPlayerTime(seconds: Float): String {
    val totalSeconds = seconds.coerceAtLeast(0f).toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun formatPlaybackQuality(quality: PlayerConstants.PlaybackQuality): String {
    return when (quality.name.lowercase()) {
        "tiny" -> "144p"
        "small" -> "240p"
        "medium" -> "360p"
        "large" -> "480p"
        "hd720" -> "720p"
        "hd1080" -> "1080p"
        "hd1440" -> "1440p"
        "hd2160" -> "2160p"
        "highres", "high_res" -> "Cao nhất"
        else -> "Tự động"
    }
}

private data class CarPlaybackRateOption(
    val label: String,
    val rate: PlayerConstants.PlaybackRate
)

private val CAR_PLAYBACK_RATES = listOf(
    CarPlaybackRateOption("0.75×", PlayerConstants.PlaybackRate.RATE_0_75),
    CarPlaybackRateOption("1×", PlayerConstants.PlaybackRate.RATE_1),
    CarPlaybackRateOption("1.25×", PlayerConstants.PlaybackRate.RATE_1_25),
    CarPlaybackRateOption("1.5×", PlayerConstants.PlaybackRate.RATE_1_5),
    CarPlaybackRateOption("2×", PlayerConstants.PlaybackRate.RATE_2)
)

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
    val metrics = rememberMpTubeUiMetrics()
    val carPlayerMode = remember { metrics.carLikeLandscape }
    val scope = rememberCoroutineScope()

    var isFullscreen by remember { mutableStateOf(false) }
    val carControlsActive = rememberUpdatedState(carPlayerMode && !isFullscreen)
    var activePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playerState by remember { mutableStateOf(PlayerConstants.PlayerState.UNKNOWN) }
    var playbackRate by remember { mutableStateOf(PlayerConstants.PlaybackRate.RATE_1) }
    var playbackQualityLabel by remember { mutableStateOf("Tự động") }
    val carCurrentSecond = remember { mutableFloatStateOf(0f) }
    val carDuration = remember { mutableFloatStateOf(0f) }
    val runtimeState = remember(activity) {
        PlayerRuntimeState(
            previousOrientation = activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        )
    }
    var isRefreshing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(video.videoId) {
        viewModel.start(video)
    }

    val playerListener = remember(viewModel, runtimeState, carPlayerMode) {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                activePlayer = youTubePlayer
                if (carPlayerMode && runtimeState.lastLoadedVideoId == null) {
                    runtimeState.lastLoadedVideoId = video.videoId
                    youTubePlayer.loadVideo(video.videoId, 0f)
                }
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                runtimeState.duration = duration
                if (carControlsActive.value) carDuration.floatValue = duration
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                runtimeState.currentSecond = second
                if (carControlsActive.value && (
                    runtimeState.lastCarUiSecond < 0f ||
                    second < runtimeState.lastCarUiSecond ||
                    second - runtimeState.lastCarUiSecond >= CAR_UI_PROGRESS_INTERVAL_SECONDS
                )) {
                    runtimeState.lastCarUiSecond = second
                    carCurrentSecond.floatValue = second
                }
                if (
                    runtimeState.lastProgressDispatchSecond < 0f ||
                    second < runtimeState.lastProgressDispatchSecond ||
                    second - runtimeState.lastProgressDispatchSecond >= PROGRESS_DISPATCH_INTERVAL_SECONDS
                ) {
                    runtimeState.lastProgressDispatchSecond = second
                    viewModel.onProgress(second, runtimeState.duration)
                }
            }

            override fun onPlaybackQualityChange(
                youTubePlayer: YouTubePlayer,
                playbackQuality: PlayerConstants.PlaybackQuality
            ) {
                if (carPlayerMode) {
                    val label = formatPlaybackQuality(playbackQuality)
                    runtimeState.playbackQualityLabel = label
                    if (carControlsActive.value) playbackQualityLabel = label
                }
            }

            override fun onPlaybackRateChange(
                youTubePlayer: YouTubePlayer,
                playbackRateValue: PlayerConstants.PlaybackRate
            ) {
                if (carPlayerMode) {
                    runtimeState.playbackRate = playbackRateValue
                    if (carControlsActive.value) playbackRate = playbackRateValue
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                if (carPlayerMode) {
                    runtimeState.playerState = state
                    if (carControlsActive.value) playerState = state
                }
                if (state == PlayerConstants.PlayerState.ENDED) viewModel.onVideoEnded()
            }
        }
    }

    var carPlayerView by remember(context, carPlayerMode) { mutableStateOf<YouTubePlayerView?>(null) }
    val phonePlayerView = remember(context, carPlayerMode) {
        if (carPlayerMode) null else YouTubePlayerView(context)
    }
    val playerView = if (carPlayerMode) carPlayerView else phonePlayerView

    LaunchedEffect(carPlayerMode, context) {
        if (!carPlayerMode || carPlayerView != null) return@LaunchedEffect

        withFrameNanos { }
        val view = YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
        }
        lifecycleOwner?.lifecycle?.addObserver(view)
        val playerOptions = IFramePlayerOptions.Builder(context)
            .controls(0)
            .ivLoadPolicy(3)
            .build()
        view.initialize(playerListener, true, playerOptions)
        carPlayerView = view
    }

    DisposableEffect(playerView, lifecycleOwner, carPlayerMode) {
        val view = playerView
        if (view != null && !carPlayerMode) {
            lifecycleOwner?.lifecycle?.addObserver(view)
            view.addYouTubePlayerListener(playerListener)
        }

        onDispose {
            if (view != null) {
                activePlayer = null
                lifecycleOwner?.lifecycle?.removeObserver(view)
                view.release()
            }
        }
    }

    LaunchedEffect(state.currentVideo?.videoId, activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        val videoId = state.currentVideo?.videoId ?: return@LaunchedEffect

        if (runtimeState.lastLoadedVideoId != videoId) {
            runtimeState.lastLoadedVideoId = videoId
            runtimeState.duration = 0f
            runtimeState.currentSecond = 0f
            runtimeState.lastProgressDispatchSecond = -1f
            runtimeState.lastCarUiSecond = -1f
            carCurrentSecond.floatValue = 0f
            carDuration.floatValue = 0f
            runtimeState.playerState = PlayerConstants.PlayerState.UNKNOWN
            runtimeState.playbackRate = PlayerConstants.PlaybackRate.RATE_1
            runtimeState.playbackQualityLabel = "Tự động"
            playerState = PlayerConstants.PlayerState.UNKNOWN
            playbackRate = PlayerConstants.PlaybackRate.RATE_1
            if (carPlayerMode) playbackQualityLabel = "Tự động"
            downloadMessage = null
            downloadProgress = 0
            player.loadVideo(videoId, 0f)
        }
    }

    LaunchedEffect(isFullscreen) {
        onFullscreenChanged(isFullscreen)
        if (carPlayerMode && !isFullscreen) {
            carCurrentSecond.floatValue = runtimeState.currentSecond
            carDuration.floatValue = runtimeState.duration
            playerState = runtimeState.playerState
            playbackRate = runtimeState.playbackRate
            playbackQualityLabel = runtimeState.playbackQualityLabel
            runtimeState.lastCarUiSecond = runtimeState.currentSecond
        }
        val currentActivity = activity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(
            currentActivity.window,
            currentActivity.window.decorView
        )

        if (isFullscreen) {
            if (!runtimeState.orientationForced) {
                runtimeState.previousOrientation = currentActivity.requestedOrientation
                runtimeState.orientationForced = true
            }
            currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            if (runtimeState.orientationForced) {
                currentActivity.requestedOrientation = runtimeState.previousOrientation
                runtimeState.orientationForced = false
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

                if (runtimeState.orientationForced) {
                    currentActivity.requestedOrientation = runtimeState.previousOrientation
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

    fun startCarDownload() {
        if (!carPlayerMode || isDownloading) return
        isDownloading = true
        downloadProgress = 0
        downloadMessage = null

        val videoToDownload = currentVideo
        scope.launch {
            try {
                var lastUiProgress = -5
                val result = Mp3DownloadManager.download(
                    context = context.applicationContext,
                    video = videoToDownload,
                    onProgress = { progress ->
                        if (progress == 100 || progress - lastUiProgress >= CAR_DOWNLOAD_UI_STEP) {
                            lastUiProgress = progress
                            scope.launch { downloadProgress = progress }
                        }
                    }
                )
                Toast.makeText(
                    context,
                    "Đã tải MP3 vào ${result.location}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: "Không tải được MP3",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isDownloading = false
            }
        }
    }

    fun refreshPlayer() {
        if (isFullscreen || isRefreshing) return
        isRefreshing = true
        val player = activePlayer
        val position = runtimeState.currentSecond.coerceAtLeast(0f)
        if (player != null) {
            runtimeState.duration = 0f
            runtimeState.lastProgressDispatchSecond = -1f
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
        PlayerRefreshContainer(
            carPlayerMode = carPlayerMode,
            isRefreshing = isRefreshing,
            onRefresh = ::refreshPlayer
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

                if (isFullscreen) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val availableRatio = if (maxHeight.value > 0f) {
                            maxWidth.value / maxHeight.value
                        } else {
                            VIDEO_ASPECT_RATIO
                        }
                        val playerModifier = if (availableRatio > VIDEO_ASPECT_RATIO) {
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(VIDEO_ASPECT_RATIO)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(VIDEO_ASPECT_RATIO)
                        }

                        if (playerView != null) {
                            AndroidView(
                                factory = { playerView },
                                modifier = playerModifier.align(Alignment.Center)
                            )
                        } else {
                            Text(
                                text = "Đang mở video...",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        if (carPlayerMode) {
                            CarVideoTouchBlocker(
                                modifier = playerModifier.align(Alignment.Center)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(64.dp)
                                .clickable { isFullscreen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                            .align(Alignment.CenterHorizontally)
                            .aspectRatio(VIDEO_ASPECT_RATIO),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerView != null) {
                            AndroidView(
                                factory = { playerView },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Đang mở video...",
                                    color = Color.White
                                )
                            }
                        }

                        if (carPlayerMode) {
                            CarVideoTouchBlocker(Modifier.fillMaxSize())
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(if (metrics.compactLandscape) 44.dp else 64.dp)
                                    .clickable { isFullscreen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⛶")
                            }
                        }
                    }
                }

                if (!isFullscreen && carPlayerMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(metrics.contentWidthFraction)
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp, start = 8.dp, end = 8.dp)
                    ) {
                        CarPlayerControls(
                            player = activePlayer,
                            playerState = playerState,
                            currentSecondState = carCurrentSecond,
                            durationState = carDuration,
                            playbackRate = playbackRate,
                            playbackQualityLabel = playbackQualityLabel,
                            nextEnabled = !state.autoplayFinished && !state.waitingForRelated,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            onPlaybackRateChange = { rate ->
                                runtimeState.playbackRate = rate
                                playbackRate = rate
                            },
                            onNext = viewModel::playNext,
                            onDownload = ::startCarDownload,
                            onFullscreen = { isFullscreen = true }
                        )
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

                    if (!carPlayerMode) {
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

private class PlayerRuntimeState(
    var currentSecond: Float = 0f,
    var duration: Float = 0f,
    var lastLoadedVideoId: String? = null,
    var lastProgressDispatchSecond: Float = -1f,
    var lastCarUiSecond: Float = -1f,
    var previousOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    var orientationForced: Boolean = false,
    var playerState: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNKNOWN,
    var playbackRate: PlayerConstants.PlaybackRate = PlayerConstants.PlaybackRate.RATE_1,
    var playbackQualityLabel: String = "Tự động"
)

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private const val PROGRESS_DISPATCH_INTERVAL_SECONDS = 1f
private const val CAR_UI_PROGRESS_INTERVAL_SECONDS = 1f
private const val CAR_DOWNLOAD_UI_STEP = 5
