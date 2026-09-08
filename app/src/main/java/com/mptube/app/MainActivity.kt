    package com.mptube.app

    import android.os.Bundle
    import java.io.IOException
    import java.net.SocketTimeoutException
    import java.net.UnknownHostException
    import androidx.compose.foundation.layout.size
    import androidx.activity.ComponentActivity
    import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.fillMaxHeight
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.aspectRatio
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.width
    import androidx.activity.compose.setContent

    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.foundation.layout.BoxWithConstraints
    import android.app.Activity
    import androidx.compose.runtime.rememberUpdatedState
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.rememberLazyListState
    import android.content.res.Configuration
    import androidx.compose.ui.platform.LocalConfiguration
    import androidx.compose.foundation.shape.RoundedCornerShape

    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.foundation.text.KeyboardOptions

    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Home
    import androidx.compose.material.icons.filled.Info
    import androidx.compose.material.icons.filled.MusicNote
    import androidx.compose.material.icons.filled.Search

    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.NavigationBar
    import androidx.compose.material3.NavigationBarItem
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.OutlinedTextFieldDefaults
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.material3.darkColorScheme
    import androidx.activity.compose.BackHandler
    import androidx.core.view.WindowInsetsCompat
    import androidx.core.view.WindowInsetsControllerCompat
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue

    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.mutableIntStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.focus.FocusRequester
    import androidx.compose.ui.focus.focusRequester
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalFocusManager
    import androidx.lifecycle.LifecycleOwner
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.viewinterop.AndroidView

    import coil3.compose.AsyncImage
    import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
    import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
    import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

    import kotlinx.coroutines.launch

    import kotlin.time.Duration.Companion.milliseconds
    import android.content.Intent
    import android.net.Uri

    import androidx.compose.foundation.layout.ColumnScope
    import androidx.compose.material.icons.filled.CameraAlt
    import androidx.compose.material.icons.filled.Chat
    import androidx.compose.material.icons.filled.Code
    import androidx.compose.material.icons.filled.Person
    import androidx.compose.material.icons.filled.Public
    import androidx.compose.material.icons.filled.Settings
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults

    import androidx.compose.ui.graphics.vector.ImageVector

    // =========================================================
    // THEME
    // =========================================================

    private val MpTubeBackground =
        Color(0xFF0F0F0F)

    @Composable
    fun MpTubeTheme(
        content: @Composable () -> Unit
    ) {
        MaterialTheme(
            colorScheme =
                darkColorScheme(
                    background =
                        MpTubeBackground,

                    surface =
                        MpTubeBackground
                ),

            content =
                content
        )
    }


    // =========================================================
    // MAIN ACTIVITY
    // =========================================================

    class MainActivity : ComponentActivity() {

        override fun onCreate(
            savedInstanceState: Bundle?
        ) {
            super.onCreate(
                savedInstanceState
            )

            setContent {

                MpTubeTheme {

                    var showSplash by remember {
                        mutableStateOf(true)
                    }

                    LaunchedEffect(Unit) {

                        kotlinx.coroutines.delay(
                            800.milliseconds
                        )

                        showSplash =
                            false
                    }

                    if (showSplash) {

                        SplashScreen()

                    } else {

                        MainScreen()
                    }
                }
            }
        }
    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private fun getYoutubeErrorMessage(
        error: Throwable
    ): String {

        return when (error) {

            is IllegalStateException -> {
                "Chưa cấu hình YouTube API key.\n\n" +
                        "Hãy thêm YOUTUBE_API_KEY vào local.properties rồi Sync Project với Gradle."
            }

            is UnknownHostException -> {
                "Không có kết nối mạng.\n\n" +
                        "Hãy kiểm tra Wi-Fi hoặc dữ liệu di động."
            }

            is SocketTimeoutException -> {
                "Kết nối YouTube hết thời gian.\n\n" +
                        "Vui lòng kiểm tra mạng rồi thử lại."
            }

            is IOException -> {
                "Không thể kết nối đến YouTube.\n\n" +
                        "Vui lòng kiểm tra kết nối mạng rồi thử lại."
            }

            else -> {
                "Đã xảy ra lỗi khi kết nối YouTube."
            }
        }
    }


    // =========================================================
    // SEARCH STATE MESSAGE
    // =========================================================

    @Composable
    private fun SearchStateMessage(
        title: String,
        message: String,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 40.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(

                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                text =
                    message,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    Color.Gray,

                textAlign =
                    TextAlign.Center
            )

            if (
                actionText != null &&
                onAction != null
            ) {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(

                    text =
                        actionText,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    modifier =
                        Modifier
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .clickable {
                                onAction()
                            }
                            .padding(
                                horizontal = 20.dp,
                                vertical = 10.dp
                            )
                )
            }
        }
    }


    // =========================================================
    // APP HEADER
    // =========================================================

    @Composable
    fun AppHeader(
        searchText: String,
        onSearchTextChange: (String) -> Unit,
        onSearch: () -> Unit,
        searchOpen: Boolean,
        onSearchOpenChange: (Boolean) -> Unit
    ) {
        val focusRequester = remember {
            FocusRequester()
        }

        val focusManager = LocalFocusManager.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(
                    id = R.drawable.mptube_logo
                ),
                contentDescription = "mpTube",
                modifier = Modifier
                    .width(48.dp)
                    .height(38.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            if (searchOpen) {

                OutlinedTextField(
                    value = searchText,

                    onValueChange =
                        onSearchTextChange,

                    placeholder = {
                        Text(
                            text = "Tìm kiếm...",
                            maxLines = 1
                        )
                    },

                    singleLine = true,

                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(
                            focusRequester
                        ),

                    shape =
                        RoundedCornerShape(10.dp),

                    keyboardOptions =
                        KeyboardOptions(
                            imeAction =
                                ImeAction.Search
                        ),

                    keyboardActions =
                        KeyboardActions(
                            onSearch = {

                                if (
                                    searchText
                                        .trim()
                                        .isNotEmpty()
                                ) {
                                    onSearch()

                                    onSearchOpenChange(
                                        false
                                    )

                                    focusManager
                                        .clearFocus()
                                }
                            }
                        ),

                    trailingIcon = {

                        if (
                            searchText.isNotEmpty()
                        ) {

                            IconButton(
                                onClick = {
                                    onSearchTextChange("")
                                }
                            ) {
                                Text("X")
                            }
                        }
                    },

                    colors =
                        OutlinedTextFieldDefaults
                            .colors(
                                focusedContainerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface,

                                unfocusedContainerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface,

                                cursorColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                )

            } else {

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )
            }

            IconButton(
                onClick = {

                    if (searchOpen) {

                        val query =
                            searchText.trim()

                        if (query.isEmpty()) {

                            onSearchOpenChange(
                                false
                            )

                            focusManager
                                .clearFocus()

                        } else {

                            onSearch()

                            onSearchOpenChange(
                                false
                            )

                            focusManager
                                .clearFocus()
                        }

                    } else {

                        onSearchOpenChange(
                            true
                        )
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Search,

                    contentDescription =
                        "Tìm kiếm"
                )
            }
        }

        LaunchedEffect(searchOpen) {

            if (searchOpen) {
                focusRequester.requestFocus()
            }
        }
    }


    // =========================================================
    // SPLASH SCREEN
    // =========================================================

    @Composable
    fun SplashScreen() {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Image(

                painter =
                    painterResource(
                        id =
                            R.drawable.splash_background
                    ),

                contentDescription =
                    null,

                modifier =
                    Modifier.fillMaxSize(),

                contentScale =
                    ContentScale.Crop
            )

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.30f
                            )
                        )
            )

            Column(

                modifier =
                    Modifier.fillMaxSize(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Image(

                    painter =
                        painterResource(
                            id =
                                R.drawable.mptube_logo
                        ),

                    contentDescription =
                        "mpTube",

                    modifier =
                        Modifier.height(
                            150.dp
                        ),

                    contentScale =
                        ContentScale.Fit
                )

                Text(

                    text =
                        "mpTube",

                    color =
                        Color.White,

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,

                    modifier =
                        Modifier.padding(
                            top = 12.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Text(

                    text =
                        "Dev By wlug.bim",

                    color =
                        Color.White,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    modifier =
                        Modifier.padding(
                            bottom = 40.dp
                        )
                )
            }
        }
    }


    // =========================================================
    // VIDEO MODEL
    // =========================================================

    data class Video(

        val videoId:
        String,

        val title:
        String,

        val channel:
        String,

        val thumbnailUrl:
        String
    )


    // =========================================================
    // VIDEO ITEM
    // =========================================================

    @Composable
    fun VideoItem(
        video: Video,
        onClick: () -> Unit
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp
                    )
                    .clickable {
                        onClick()
                    }
        ) {

            AsyncImage(

                model =
                    video.thumbnailUrl,

                contentDescription =
                    "Video thumbnail",

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            16f / 9f
                        )
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        ),

                contentScale =
                    ContentScale.Crop
            )

            Text(

                text =
                    video.title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                maxLines =
                    2,

                modifier =
                    Modifier.padding(
                        top = 8.dp
                    )
            )

            Text(

                text =
                    video.channel,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                maxLines =
                    1
            )
        }
    }


    // =========================================================
    // MAIN SCREEN
    // =========================================================

    @Composable
    fun MainScreen() {

        var selectedTab by remember {
            mutableIntStateOf(0)
        }

        var globalSearchQuery by remember {
            mutableStateOf<String?>(null)
        }
        var isPlayerFullscreen by remember {
            mutableStateOf(false)
        }
        val configuration =
            LocalConfiguration.current

        val isLandscape =
            configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
        Scaffold(

            bottomBar = {

                if (!isPlayerFullscreen) {

                    if (isLandscape) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(60.dp)
                                    .background(
                                        Color.Transparent
                                    ),
                            horizontalArrangement =
                                Arrangement.SpaceEvenly,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            // Tất cả
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedTab = 0
                                        },
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Home,
                                    contentDescription =
                                        "Tất cả",
                                    modifier =
                                        Modifier.size(20.dp)
                                )

                                Text(
                                    text = "Tất cả",
                                    style =
                                        MaterialTheme.typography.labelSmall
                                )
                            }

                            // Âm nhạc
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedTab = 1
                                        },
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.MusicNote,
                                    contentDescription =
                                        "Âm nhạc",
                                    modifier =
                                        Modifier.size(20.dp)
                                )

                                Text(
                                    text = "Âm nhạc",
                                    style =
                                        MaterialTheme.typography.labelSmall
                                )
                            }

                            // Thông tin
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedTab = 2
                                        },
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Info,
                                    contentDescription =
                                        "Thông tin",
                                    modifier =
                                        Modifier.size(20.dp)
                                )

                                Text(
                                    text = "Thông tin",
                                    style =
                                        MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                    } else {

                        // DỌC: giữ NavigationBar Material 3 như cũ
                        NavigationBar(
                            containerColor = Color.Transparent
                        ) {

                            NavigationBarItem(
                                selected =
                                    selectedTab == 0,

                                onClick = {
                                    selectedTab = 0
                                },

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.Home,
                                        contentDescription =
                                            "Tất cả"
                                    )
                                },

                                label = {
                                    Text("Thịnh Hành")
                                }
                            )

                            NavigationBarItem(
                                selected =
                                    selectedTab == 1,

                                onClick = {
                                    selectedTab = 1
                                },

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.MusicNote,
                                        contentDescription =
                                            "Âm nhạc"
                                    )
                                },

                                label = {
                                    Text("Âm Nhạc")
                                }
                            )

                            NavigationBarItem(
                                selected =
                                    selectedTab == 2,

                                onClick = {
                                    selectedTab = 2
                                },

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.Info,
                                        contentDescription =
                                            "Thông tin"
                                    )
                                },

                                label = {
                                    Text("Thông Tin")
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
            ) {

                when (selectedTab) {

                    0 -> {

                        HomeScreen(

                            externalSearchQuery =
                                globalSearchQuery,

                            onSearchFromPlayer = {
                                    query ->

                                globalSearchQuery =
                                    query

                                selectedTab =
                                    0
                            },

                            onPlayerFullscreenChanged = {
                                    fullscreen ->

                                isPlayerFullscreen =
                                    fullscreen
                            }
                        )
                    }

                    1 -> {

                        MusicScreen(

                            onSearchFromPlayer = {
                                    query ->

                                globalSearchQuery =
                                    query

                                selectedTab =
                                    0
                            },

                            onPlayerFullscreenChanged = {
                                    fullscreen ->

                                isPlayerFullscreen =
                                    fullscreen
                            }
                        )
                    }

                    2 -> {

                        AboutScreen()
                    }
                }
            }
        }
    }


    // =========================================================
    // HOME SCREEN
    // =========================================================

    @Composable
    fun HomeScreen(
        externalSearchQuery:
        String? = null,

        onSearchFromPlayer:
            (String) -> Unit = {},

        onPlayerFullscreenChanged:
            (Boolean) -> Unit = {}
    ) {

        val context =
            LocalContext.current

        val repository = remember(context) {
            YoutubeRepository(context.applicationContext)
        }

        var searchText by remember {
            mutableStateOf("")
        }

        var searchOpen by remember {
            mutableStateOf(false)
        }
        var consumedExternalSearchQuery by remember {
            mutableStateOf<String?>(null)
        }
        var videos by remember {
            mutableStateOf<List<Video>>(
                emptyList()
            )
        }

        var searchQuery by remember {
            mutableStateOf("")
        }

        var hasSearched by remember {
            mutableStateOf(false)
        }

        var searchTrigger by remember {
            mutableStateOf(0)
        }

        var selectedVideo by remember {
            mutableStateOf<Video?>(null)
        }

        var showPlayer by remember {
            mutableStateOf(false)
        }

        var nextPageToken by remember {
            mutableStateOf<String?>(null)
        }

        var isLoadingMore by remember {
            mutableStateOf(false)
        }

        var isSearching by remember {
            mutableStateOf(false)
        }

        var searchError by remember {
            mutableStateOf<String?>(null)
        }

        var apiResult by remember {
            mutableStateOf("")
        }

        val listState =
            rememberLazyListState()

        val scope =
            rememberCoroutineScope()

        // =========================================================
        // SEARCH FROM PLAYER
        // =========================================================

        LaunchedEffect(
            externalSearchQuery
        ) {

            val query =
                externalSearchQuery
                    ?.trim()

            if (
                !query.isNullOrEmpty() &&
                query != consumedExternalSearchQuery
            ) {

                consumedExternalSearchQuery =
                    query

                searchText =
                    query

                searchQuery =
                    query

                hasSearched =
                    true

                searchTrigger++
            }
        }

        // =========================================================
        // SEARCH
        // =========================================================

        LaunchedEffect(searchTrigger) {

            isSearching = true
            searchError = null
            apiResult = ""

            try {

                // =====================================================
                // TRANG CHỦ: VIDEO THỊNH HÀNH
                // =====================================================

                if (searchTrigger == 0) {

                    val trendingVideos =
                        repository.getTrendingVideos()

                    videos =
                        trendingVideos

                    nextPageToken =
                        null

                    apiResult =
                        "   Trending..."

                }

                // =====================================================
                // SEARCH
                // =====================================================

                else {

                    val query =
                        searchQuery.trim()

                    if (query.isEmpty()) {
                        return@LaunchedEffect
                    }

                    videos =
                        emptyList()

                    nextPageToken =
                        null

                    listState.scrollToItem(0)

                    val response =
                        repository.searchVideos(
                            query = query
                        )

                    videos =
                        response.items.mapNotNull { item ->

                            val videoId =
                                item.id.videoId

                            if (videoId.isBlank()) {
                                null
                            } else {

                                Video(
                                    videoId = videoId,

                                    title =
                                        item.snippet.title,

                                    channel =
                                        item.snippet.channelTitle,

                                    thumbnailUrl =
                                        item.snippet
                                            .thumbnails
                                            .high
                                            .url
                                )
                            }
                        }

                    nextPageToken =
                        response.nextPageToken

                    apiResult =
                        "Kết quả cho ${videos.size} video"
                }

            } catch (e: Exception) {

                videos =
                    emptyList()

                nextPageToken =
                    null

                searchError =
                    getYoutubeErrorMessage(e)

            } finally {

                isSearching =
                    false
            }
        }

        // =========================================================
        // PLAYER
        // =========================================================

        if (
            showPlayer &&
            selectedVideo != null
        ) {

            PlayerScreen(

                video =
                    selectedVideo!!,

                onBack = {

                    showPlayer =
                        false

                    selectedVideo =
                        null
                },

                onSearch = {
                        query ->

                    showPlayer =
                        false

                    selectedVideo =
                        null

                    onSearchFromPlayer(
                        query
                    )
                },

                onFullscreenChanged = {
                        fullscreen ->

                    onPlayerFullscreenChanged(
                        fullscreen
                    )
                }
            )

            return
        }

        // =========================================================
        // HOME UI
        // =========================================================

        Surface(

            modifier =
                Modifier.fillMaxSize(),

            color =
                MaterialTheme
                    .colorScheme
                    .background,

            contentColor =
                MaterialTheme
                    .colorScheme
                    .onBackground
        ) {

            Column(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                // -------------------------------------------------
                // HEADER
                // -------------------------------------------------

                AppHeader(

                    searchText =
                        searchText,

                    onSearchTextChange = {
                        searchText =
                            it
                    },

                    onSearch = {

                        val query =
                            searchText.trim()

                        if (
                            query.isNotEmpty()
                        ) {

                            searchQuery =
                                query

                            hasSearched =
                                true

                            searchTrigger++
                        }
                    },

                    searchOpen =
                        searchOpen,

                    onSearchOpenChange = {
                        searchOpen =
                            it
                    }
                )

                // -------------------------------------------------
                // LOADING
                // -------------------------------------------------

                if (isSearching) {

                    SearchStateMessage(

                        title =
                            if (hasSearched) {
                                "Đang tìm kiếm..."
                            } else {
                                "Đang tải..."
                            },

                        message =
                            if (hasSearched) {
                                "Đang tìm video trên YouTube."
                            } else {
                                "Đang tải video."
                            }
                    )
                }

                // -------------------------------------------------
                // ERROR
                // -------------------------------------------------

                else if (
                    searchError != null
                ) {

                    SearchStateMessage(

                        title =
                            "Không thể tải video",

                        message =
                            searchError
                                ?: "Đã xảy ra lỗi.",

                        actionText =
                            "Thử lại",

                        onAction = {

                            searchTrigger++
                        }
                    )
                }

                // -------------------------------------------------
                // NO RESULTS
                // -------------------------------------------------

                else if (
                    videos.isEmpty()
                ) {

                    if (hasSearched) {
                        SearchStateMessage(
                            title = "Không tìm thấy video",
                            message = "Thử một từ khóa khác nhé."
                        )
                    } else {
                        SearchStateMessage(
                            title = "Không có video",
                            message = "Không thể tải video thịnh hành."
                        )
                    }
                }

                // -------------------------------------------------
                // RESULTS
                // -------------------------------------------------

                else {

                    if (
                        apiResult.isNotEmpty()
                    ) {

                        Text(

                            text =
                                apiResult,

                            color =
                                Color.White,

                            modifier =
                                Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 4.dp
                                )
                        )
                    }

                    LazyColumn(

                        state =
                            listState,

                        verticalArrangement =
                            Arrangement.spacedBy(
                                16.dp
                            ),

                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        items(
                            items = videos,
                            key = { it.videoId }
                        ) { video ->

                            VideoItem(

                                video =
                                    video,

                                onClick = {

                                    selectedVideo =
                                        video

                                    showPlayer =
                                        true
                                }
                            )
                        }

                        // -------------------------------------------------
                        // LOAD MORE
                        // -------------------------------------------------

                        if (
                            nextPageToken != null &&
                            videos.size < 50
                        ) {

                            item {

                                Text(

                                    text =
                                        if (
                                            isLoadingMore
                                        ) {
                                            "Đang tải..."
                                        } else {
                                            "Tải thêm video"
                                        },

                                    textAlign =
                                        TextAlign.Center,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {

                                                if (
                                                    isLoadingMore
                                                ) {
                                                    return@clickable
                                                }

                                                val token =
                                                    nextPageToken
                                                        ?: return@clickable

                                                isLoadingMore =
                                                    true

                                                scope.launch {

                                                    try {

                                                        val response =
                                                            repository.searchVideos(

                                                                query =
                                                                    searchQuery,

                                                                pageToken =
                                                                    token
                                                            )

                                                        val newVideos =
                                                            response.items.map {
                                                                    item ->

                                                                Video(

                                                                    videoId =
                                                                        item.id.videoId,

                                                                    title =
                                                                        item.snippet.title,

                                                                    channel =
                                                                        item.snippet.channelTitle,

                                                                    thumbnailUrl =
                                                                        item.snippet
                                                                            .thumbnails
                                                                            .medium
                                                                            .url
                                                                )
                                                            }

                                                        val remaining =
                                                            50 -
                                                                    videos.size

                                                        videos =
                                                            videos +
                                                                    newVideos.take(
                                                                        remaining
                                                                    )

                                                        nextPageToken =
                                                            if (
                                                                videos.size >= 50
                                                            ) {
                                                                null
                                                            } else {
                                                                response
                                                                    .nextPageToken
                                                            }

                                                        apiResult =
                                                            "Kết quả cho ${videos.size} video"

                                                        searchError =
                                                            null

                                                    } catch (
                                                        e: Exception
                                                    ) {

                                                        searchError =
                                                            getYoutubeErrorMessage(
                                                                e
                                                            )

                                                    } finally {

                                                        isLoadingMore =
                                                            false
                                                    }
                                                }
                                            }
                                            .padding(
                                                16.dp
                                            )
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // =========================================================
    // MUSIC SCREEN
    // =========================================================

    @Composable
    fun MusicScreen(
        onSearchFromPlayer:
            (String) -> Unit = {},

        onPlayerFullscreenChanged:
            (Boolean) -> Unit = {}
    ) {

        val context =
            LocalContext.current

        val repository = remember(context) {
            YoutubeRepository(context.applicationContext)
        }

        var selectedVideo by remember {
            mutableStateOf<Video?>(null)
        }

        var showPlayer by remember {
            mutableStateOf(false)
        }

        var musicVideos by remember {
            mutableStateOf<List<Video>>(
                emptyList()
            )
        }

        var musicNextPageToken by remember {
            mutableStateOf<String?>(null)
        }

        var isLoading by remember {
            mutableStateOf(false)
        }

        var musicErrorMessage by remember {
            mutableStateOf<String?>(null)
        }

        var searchText by remember {
            mutableStateOf("")
        }

        var searchOpen by remember {
            mutableStateOf(false)
        }

        val scope =
            rememberCoroutineScope()

        // =========================================================
        // LOAD MUSIC
        // =========================================================

        LaunchedEffect(Unit) {

            try {

                musicErrorMessage =
                    null

                val response =
                    repository.searchVideos(

                        query =
                            "nhạc Việt Nam",

                        videoCategoryId =
                            "10"
                    )

                musicVideos =
                    response.items.map {
                            item ->

                        Video(

                            videoId =
                                item.id.videoId,

                            title =
                                item.snippet.title,

                            channel =
                                item.snippet.channelTitle,

                            thumbnailUrl =
                                item.snippet
                                    .thumbnails
                                    .medium
                                    .url
                        )
                    }

                musicNextPageToken =
                    response.nextPageToken

            } catch (e: Exception) {

                musicErrorMessage =
                    getYoutubeErrorMessage(
                        e
                    )

                musicVideos =
                    emptyList()

                musicNextPageToken =
                    null
            }
        }

        // =========================================================
        // PLAYER
        // =========================================================

        if (
            showPlayer &&
            selectedVideo != null
        ) {

            PlayerScreen(

                video =
                    selectedVideo!!,

                onBack = {

                    showPlayer =
                        false

                    selectedVideo =
                        null
                },

                onSearch = {
                        query ->

                    showPlayer =
                        false

                    selectedVideo =
                        null

                    onSearchFromPlayer(
                        query
                    )
                },

                onFullscreenChanged = {
                        fullscreen ->

                    onPlayerFullscreenChanged(
                        fullscreen
                    )
                }
            )

            return
        }

        Column(

            modifier =
                Modifier.fillMaxSize()
        ) {

            AppHeader(

                searchText =
                    searchText,

                onSearchTextChange = {
                    searchText =
                        it
                },

                onSearch = {

                    val query =
                        searchText.trim()

                    if (
                        query.isNotEmpty()
                    ) {

                        onSearchFromPlayer(
                            query
                        )
                    }
                },

                searchOpen =
                    searchOpen,

                onSearchOpenChange = {
                    searchOpen =
                        it
                }
            )

            Text(

                text =
                    "Âm nhạc",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                modifier =
                    Modifier.padding(
                        16.dp
                    )
            )

            musicErrorMessage?.let {
                    message ->

                SearchStateMessage(

                    title =
                        "Không thể tải nhạc",

                    message =
                        message
                )
            }

            if (
                musicVideos.isEmpty() &&
                musicErrorMessage == null
            ) {

                SearchStateMessage(

                    title =
                        "Đang tải...",

                    message =
                        "Đang tìm nhạc ..."
                )

            } else {

                LazyColumn(

                    verticalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        ),

                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    items(
                        items = musicVideos,
                        key = { it.videoId }
                    ) { video ->

                        VideoItem(

                            video =
                                video,

                            onClick = {

                                selectedVideo =
                                    video

                                showPlayer =
                                    true
                            }
                        )
                    }

                    if (
                        musicNextPageToken != null &&
                        musicVideos.size < 50
                    ) {

                        item {

                            Text(

                                text =
                                    if (
                                        isLoading
                                    ) {
                                        "Đang tải..."
                                    } else {
                                        "Tải thêm 5 video"
                                    },

                                textAlign =
                                    TextAlign.Center,

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            if (
                                                isLoading
                                            ) {
                                                return@clickable
                                            }

                                            val token =
                                                musicNextPageToken
                                                    ?: return@clickable

                                            isLoading =
                                                true

                                            scope.launch {

                                                try {

                                                    val response =
                                                        repository.searchVideos(

                                                            query =
                                                                "nhạc Việt Nam",

                                                            videoCategoryId =
                                                                "10",

                                                            pageToken =
                                                                token
                                                        )

                                                    val newVideos =
                                                        response.items.map {
                                                                item ->

                                                            Video(

                                                                videoId =
                                                                    item.id.videoId,

                                                                title =
                                                                    item.snippet.title,

                                                                channel =
                                                                    item.snippet.channelTitle,

                                                                thumbnailUrl =
                                                                    item.snippet
                                                                        .thumbnails
                                                                        .medium
                                                                        .url
                                                            )
                                                        }

                                                    val remaining =
                                                        50 -
                                                                musicVideos.size

                                                    musicVideos =
                                                        musicVideos +
                                                                newVideos.take(
                                                                    remaining
                                                                )

                                                    musicNextPageToken =
                                                        if (
                                                            musicVideos.size >= 50
                                                        ) {
                                                            null
                                                        } else {
                                                            response
                                                                .nextPageToken
                                                        }

                                                } catch (
                                                    e: Exception
                                                ) {

                                                    musicErrorMessage =
                                                        getYoutubeErrorMessage(
                                                            e
                                                        )

                                                } finally {

                                                    isLoading =
                                                        false
                                                }
                                            }
                                        }
                                        .padding(
                                            16.dp
                                        )
                            )
                        }
                    }
                }
            }
        }
    }


    // =========================================================
    // ABOUT
    // =========================================================

    @Composable
    fun AboutScreen() {

        val context =
            LocalContext.current

        fun openLink(
            url: String
        ) {

            if (
                url.startsWith(
                    "YOUR_"
                )
            ) {
                return
            }

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            context.startActivity(
                intent
            )
        }

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 20.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // =====================================================
            // APP HEADER
            // =====================================================

            Image(
                painter =
                    painterResource(
                        id =
                            R.drawable.mptube_logo
                    ),

                contentDescription =
                    "mpTube",

                modifier =
                    Modifier
                        .size(76.dp)
                        .clip(
                            RoundedCornerShape(
                                22.dp
                            )
                        ),

                contentScale =
                    ContentScale.Fit
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text = "mpTube",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            Text(
                text =
                    "YouTube Client tối giản cho Android",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text = "v1.26.01",
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            // =====================================================
            // GIỚI THIỆU MP TUBE
            // =====================================================

            AboutCard(
                title = "mpTube Information",
                icon = Icons.Default.Info
            ) {

                Text(
                    text =
                        "mpTube là ứng dụng YouTube " +
                                "tối giản dành cho hệ điều hành Android, " +
                                "tập trung vào trải nghiệm xem " +
                                "video nhanh, nhẹ và thuận tiện giúp các máy yếu hoạt động tốt!",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                AboutLinkItem(
                    icon =
                        Icons.Default.Code,

                    title =
                        "Mã nguồn trên GitHub",

                    onClick = {

                        openLink(
                            "https://github.com/minhphuocit/mpTube"
                        )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            // =====================================================
            // TÁC GIẢ
            // =====================================================

            AboutCard(
                title = "Author",
                icon = Icons.Default.Person
            ) {

                Text(
                    text =
                        "Được phát triển bởi @wlug.bim với sự tập trung " +
                                "vào trải nghiệm đơn giản, mượt mà " +
                                "và thân thiện.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                AboutLinkItem(
                    icon =
                        Icons.Default.Public,

                    title =
                        "Facebook",

                    onClick = {

                        openLink(
                            "https://www.facebook.com/bimmer.26th9/"
                        )
                    }
                )

                AboutLinkItem(
                    icon =
                        Icons.Default.CameraAlt,

                    title =
                        "Instagram",

                    onClick = {

                        openLink(
                            "https://www.instagram.com/wlug.bim?stkn=MXJhMGdvZWhxYnE1"
                        )
                    }
                )

                AboutLinkItem(
                    icon =
                        Icons.Default.Chat,

                    title =
                        "Zalo",

                    onClick = {

                        openLink(
                            "https://chat.zalo.me/"
                        )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            // =====================================================
            // THÔNG TIN ỨNG DỤNG
            // =====================================================

            AboutCard(
                title = "Thông tin ứng dụng",
                icon = Icons.Default.Settings
            ) {

                AboutInfoRow(
                    title =
                        "Phiên bản",

                    value =
                        "v1.26.01"
                )

                AboutInfoRow(
                    title =
                        "Nền tảng",

                    value =
                        "Android + Kolin"
                )

                AboutInfoRow(
                    title =
                        "Trạng thái",

                    value =
                        "Đang phát triển..."
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            // =====================================================
            // FOOTER
            // =====================================================

            Text(
                text = "mpTube",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text =
                    "Made with ♥ for Android",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text =
                    "© 2026",

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )
        }
    }
    @Composable
    fun AboutCard(
        title: String,
        icon: ImageVector,
        content: @Composable ColumnScope.() -> Unit
    ) {

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    16.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            16.dp
                        )
            ) {

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            icon,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                22.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )

                    Text(
                        text = title,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                content()
            }
        }
    }
    @Composable
    fun AboutLinkItem(
        icon: ImageVector,
        title: String,
        onClick: () -> Unit
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .clickable {
                        onClick()
                    }
                    .padding(
                        vertical = 12.dp,
                        horizontal = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    icon,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(
                        20.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Text(
                text = title,

                modifier =
                    Modifier.weight(1f),

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            Text(
                text = "›",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
    @Composable
    fun AboutInfoRow(
        title: String,
        value: String
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 7.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = title,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text = value,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }
    }

    // =========================================================
    // PLAYER SCREEN
    // =========================================================

    @Composable
    fun PlayerScreen(

        video: Video,

        onBack: () -> Unit,

        onSearch: (String) -> Unit,

        onFullscreenChanged:
            (Boolean) -> Unit = {}

    ) {

        val context =
            LocalContext.current

        val repository =
            remember(context) {

                YoutubeRepository(
                    context.applicationContext
                )
            }

        val lifecycleOwner =
            context as? LifecycleOwner

        var searchText by remember {

            mutableStateOf("")
        }

        var searchOpen by remember {

            mutableStateOf(false)
        }

        var isFullscreen by remember {

            mutableStateOf(false)
        }

        // ---------------------------------------------------------
        // AUTOPLAY STATE
        // ---------------------------------------------------------
        var relatedVideos by remember {
            mutableStateOf<List<Video>>(
                emptyList()
            )
        }

        var relatedLoading by remember(
            video.videoId
        ) {
            mutableStateOf(false)
        }

        var relatedLoaded by remember(
            video.videoId
        ) {
            mutableStateOf(false)
        }

        var autoplayWaitingForRelated by remember(
            video.videoId
        ) {
            mutableStateOf(false)
        }

        var activePlayer by remember(
            video.videoId
        ) {
            mutableStateOf<YouTubePlayer?>(null)
        }
        var autoplayVideo by remember(
            video.videoId
        ) {

            mutableStateOf(video)
        }

        var nextAutoplayVideo by remember(
            video.videoId
        ) {

            mutableStateOf<Video?>(null)
        }

        var autoplayLoading by remember(
            video.videoId
        ) {

            mutableStateOf(false)
        }

        var autoplayPrepared by remember(
            video.videoId
        ) {

            mutableStateOf(false)
        }

        var videoDuration by remember(
            video.videoId
        ) {

            mutableStateOf(0f)
        }

        var autoplayCount by remember(
            video.videoId
        ) {

            mutableStateOf(1)
        }

        var autoplayFinished by remember(
            video.videoId
        ) {

            mutableStateOf(false)
        }
        var currentVideoPosition by remember(
            video.videoId
        ) {
            mutableStateOf(0f)
        }
        val currentRelatedVideos =
            rememberUpdatedState(
                relatedVideos
            )

        val currentNextAutoplayVideo =
            rememberUpdatedState(
                nextAutoplayVideo
            )

        val currentAutoplayFinished =
            rememberUpdatedState(
                autoplayFinished
            )

        val currentAutoplayPrepared =
            rememberUpdatedState(
                autoplayPrepared
            )

        val currentAutoplayLoading =
            rememberUpdatedState(
                autoplayLoading
            )

        val currentVideoDuration =
            rememberUpdatedState(
                videoDuration
            )

        val currentAutoplayCount =
            rememberUpdatedState(
                autoplayCount
            )

        val currentAutoplayVideo =
            rememberUpdatedState(
                autoplayVideo
            )

        // =========================================================
        // LOAD RELATED VIDEOS
        // =========================================================

        LaunchedEffect(
            video.videoId
        ) {

            relatedVideos =
                emptyList()

            relatedLoaded =
                false

            relatedLoading =
                true

            autoplayWaitingForRelated =
                false

            try {

                val response =
                    repository.getRelatedVideos(
                        videoId =
                            video.videoId,

                        videoTitle =
                            video.title
                    )

                relatedVideos =
                    response.items.mapNotNull { item ->

                        val id =
                            item.id.videoId

                        if (id.isBlank()) {
                            null
                        } else {
                            Video(
                                videoId = id,
                                title = item.snippet.title,
                                channel = item.snippet.channelTitle,
                                thumbnailUrl =
                                    item.snippet.thumbnails.medium.url
                            )
                        }
                    }

            } catch (_: Exception) {

                relatedVideos =
                    emptyList()

            } finally {

                relatedLoaded =
                    true

                relatedLoading =
                    false
            }
        }

        // Continue automatically if the video ended while Related was loading.
        LaunchedEffect(
            relatedVideos,
            autoplayWaitingForRelated,
            activePlayer
        ) {

            if (!autoplayWaitingForRelated) {
                return@LaunchedEffect
            }

            val player =
                activePlayer
                    ?: return@LaunchedEffect

            val nextIndex =
                autoplayCount - 1

            val nextVideo =
                relatedVideos.getOrNull(nextIndex)

            if (
                nextVideo != null &&
                autoplayCount <= 10
            ) {

                autoplayVideo =
                    nextVideo

                autoplayCount++

                autoplayFinished =
                    false

                autoplayWaitingForRelated =
                    false

                videoDuration =
                    0f

                player.loadVideo(
                    nextVideo.videoId,
                    0f
                )

            } else if (relatedLoaded && !relatedLoading) {

                autoplayWaitingForRelated =
                    false

                autoplayFinished =
                    true
            }
        }

        // =========================================================
        // SYSTEM BARS
        // =========================================================

        LaunchedEffect(
            isFullscreen
        ) {

            onFullscreenChanged(
                isFullscreen
            )

            val activity =
                context as? Activity
                    ?: return@LaunchedEffect

            val controller =
                WindowInsetsControllerCompat(

                    activity.window,

                    activity.window.decorView
                )

            if (isFullscreen) {

                controller.hide(
                    WindowInsetsCompat
                        .Type
                        .systemBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            } else {

                controller.show(
                    WindowInsetsCompat
                        .Type
                        .systemBars()
                )
            }
        }

        // =========================================================
        // BACK FROM FULLSCREEN
        // =========================================================

        BackHandler(
            enabled =
                isFullscreen
        ) {

            isFullscreen =
                false
        }

        // =========================================================
        // FULLSCREEN
        // =========================================================

        if (isFullscreen) {

            BoxWithConstraints(

                modifier =
                    Modifier.fillMaxSize()
            ) {

                val playerWidth =
                    minOf(
                        maxWidth,
                        maxHeight * (16f / 9f)
                    )

                val playerHeight =
                    playerWidth * (9f / 16f)

                AndroidView(

                    factory = { playerContext ->

                        YouTubePlayerView(
                            playerContext
                        ).apply {

                            lifecycleOwner
                                ?.lifecycle
                                ?.addObserver(
                                    this
                                )

                            addYouTubePlayerListener(

                                object :
                                    AbstractYouTubePlayerListener() {

                                    override fun onReady(
                                        youTubePlayer:
                                        YouTubePlayer
                                    ) {

                                        activePlayer =
                                            youTubePlayer

                                        youTubePlayer.loadVideo(
                                            currentAutoplayVideo.value.videoId,
                                            currentVideoPosition
                                        )
                                    }

                                    override fun onVideoDuration(
                                        youTubePlayer:
                                        YouTubePlayer,

                                        duration:
                                        Float
                                    ) {

                                        videoDuration =
                                            duration
                                    }

                                    override fun onCurrentSecond(
                                        youTubePlayer: YouTubePlayer,
                                        second: Float
                                    ) {

                                        currentVideoPosition =
                                            second
                                    }

                                    override fun onStateChange(
                                        youTubePlayer: YouTubePlayer,
                                        state: PlayerConstants.PlayerState
                                    ) {

                                        if (
                                            state !=
                                            PlayerConstants
                                                .PlayerState
                                                .ENDED
                                        ) {
                                            return
                                        }

                                        val nextIndex =
                                            currentAutoplayCount.value - 1

                                        val nextVideo =
                                            currentRelatedVideos.value
                                                .getOrNull(nextIndex)

                                        if (
                                            nextVideo != null &&
                                            currentAutoplayCount.value <= 10
                                        ) {

                                            autoplayVideo =
                                                nextVideo

                                            autoplayCount =
                                                currentAutoplayCount.value + 1

                                            autoplayFinished =
                                                false

                                            videoDuration =
                                                0f
                                            currentVideoPosition =
                                                0f
                                            youTubePlayer.loadVideo(
                                                nextVideo.videoId,
                                                0f
                                            )

                                        } else if (
                                            !relatedLoaded ||
                                            relatedLoading
                                        ) {

                                            autoplayWaitingForRelated =
                                                true

                                        } else {

                                            autoplayFinished =
                                                true
                                        }
                                    }
                                }
                            )
                        }
                    },

                    modifier =
                        Modifier
                            .width(
                                playerWidth
                            )
                            .height(
                                playerHeight
                            )
                            .align(
                                Alignment.Center
                            ),

                    onRelease = {
                            playerView ->

                        lifecycleOwner
                            ?.lifecycle
                            ?.removeObserver(
                                playerView
                            )

                        playerView.release()
                    }
                )

                // -----------------------------------------------------
                // EXIT FULLSCREEN
                // -----------------------------------------------------

                Box(

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopEnd
                            )
                            .padding(8.dp)
                            .size(48.dp)
                            .clickable {

                                isFullscreen =
                                    false
                            },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "✕"
                    )
                }
            }

            return
        }

        // =========================================================
        // NORMAL MODE
        // =========================================================

        Surface(

            modifier =
                Modifier.fillMaxSize(),

            color =
                MaterialTheme
                    .colorScheme
                    .background,

            contentColor =
                MaterialTheme
                    .colorScheme
                    .onBackground
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {

                // -----------------------------------------------------
                // HEADER
                // -----------------------------------------------------

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(

                        text =
                            "←",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        modifier =
                            Modifier
                                .padding(
                                    start = 16.dp
                                )
                                .clickable {
                                    onBack()
                                }
                    )

                    AppHeader(

                        searchText =
                            searchText,

                        onSearchTextChange = {
                            searchText =
                                it
                        },

                        onSearch = {

                            val query =
                                searchText.trim()

                            if (
                                query.isNotEmpty()
                            ) {

                                onSearch(
                                    query
                                )
                            }
                        },

                        searchOpen =
                            searchOpen,

                        onSearchOpenChange = {
                            searchOpen =
                                it
                        }
                    )
                }

                // -----------------------------------------------------
                // PLAYER
                // -----------------------------------------------------

                Box(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(
                                16f / 9f
                            )
                ) {

                    AndroidView(

                        factory = { playerContext ->

                            YouTubePlayerView(
                                playerContext
                            ).apply {

                                lifecycleOwner
                                    ?.lifecycle
                                    ?.addObserver(
                                        this
                                    )

                                addYouTubePlayerListener(

                                    object :
                                        AbstractYouTubePlayerListener() {

                                        override fun onReady(
                                            youTubePlayer:
                                            YouTubePlayer
                                        ) {

                                        activePlayer =
                                            youTubePlayer

                                            youTubePlayer.loadVideo(
                                                currentAutoplayVideo.value.videoId,
                                                currentVideoPosition
                                            )
                                        }

                                        override fun onVideoDuration(
                                            youTubePlayer:
                                            YouTubePlayer,

                                            duration:
                                            Float
                                        ) {

                                            videoDuration =
                                                duration
                                        }

                                        override fun onCurrentSecond(
                                            youTubePlayer: YouTubePlayer,
                                            second: Float
                                        ) {

                                            currentVideoPosition =
                                                second
                                        }

                                    override fun onStateChange(
                                        youTubePlayer: YouTubePlayer,
                                        state: PlayerConstants.PlayerState
                                    ) {

                                        if (
                                            state !=
                                            PlayerConstants
                                                .PlayerState
                                                .ENDED
                                        ) {
                                            return
                                        }

                                        val nextIndex =
                                            currentAutoplayCount.value - 1

                                        val nextVideo =
                                            currentRelatedVideos.value
                                                .getOrNull(nextIndex)

                                        if (
                                            nextVideo != null &&
                                            currentAutoplayCount.value <= 10
                                        ) {

                                            autoplayVideo =
                                                nextVideo

                                            autoplayCount =
                                                currentAutoplayCount.value + 1

                                            autoplayFinished =
                                                false

                                            videoDuration =
                                                0f
                                            currentVideoPosition =
                                                0f
                                            youTubePlayer.loadVideo(
                                                nextVideo.videoId,
                                                0f
                                            )

                                        } else if (
                                            !relatedLoaded ||
                                            relatedLoading
                                        ) {

                                            autoplayWaitingForRelated =
                                                true

                                        } else {

                                            autoplayFinished =
                                                true
                                        }
                                    }
                                    }
                                )
                            }
                        },

                        modifier =
                            Modifier.fillMaxSize(),

                        onRelease = {
                                playerView ->

                            lifecycleOwner
                                ?.lifecycle
                                ?.removeObserver(
                                    playerView
                                )

                            playerView.release()
                        }
                    )

                    // -------------------------------------------------
                    // FULLSCREEN BUTTON
                    // -------------------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomEnd
                                )
                                .padding(8.dp)
                                .size(48.dp)
                                .clickable {

                                    isFullscreen =
                                        true
                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "⛶"
                        )
                    }
                }

                // -----------------------------------------------------
                // VIDEO INFO
                // -----------------------------------------------------

                Text(

                    text =
                        autoplayVideo.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    modifier =
                        Modifier.padding(
                            16.dp
                        )
                )

                Text(

                    text =
                        autoplayVideo.channel,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                )

                if (autoplayFinished) {

                    Text(

                        text =
                            "Đã phát hết danh sách video liên quan",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 16.dp
                            )
                    )
                }

                Spacer(

                    modifier =
                        Modifier.height(
                            24.dp
                        )
                )
            }
        }
    }
