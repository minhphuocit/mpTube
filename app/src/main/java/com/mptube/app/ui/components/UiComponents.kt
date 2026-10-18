package com.mptube.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mptube.app.R
import com.mptube.app.model.Video
import com.mptube.app.ui.util.rememberMpTubeUiMetrics

@Composable
fun SearchStateMessage(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val metrics = rememberMpTubeUiMetrics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = metrics.titleSize
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = metrics.bodySize
            ),
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = actionText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = metrics.bodySize
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun AppHeader(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchOpen: Boolean,
    onSearchOpenChange: (Boolean) -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    centerSearch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val metrics = rememberMpTubeUiMetrics()

    @Composable
    fun Brand() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (metrics.compactLandscape) 16.dp else 24.dp,
                    vertical = if (metrics.compactLandscape) 6.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        horizontal = 5.dp,
                        vertical = 1.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
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
    fun SearchBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                .padding(
                    horizontal = when {
                        metrics.compactLandscape -> 12.dp
                        metrics.largeControls -> 80.dp
                        else -> 34.dp
                    }
                ),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = {
                        Text("Tìm kiếm video...")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            when {
                                metrics.compactLandscape -> 46.dp
                                metrics.largeControls -> 64.dp
                                else -> 56.dp
                            }
                        )
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused && !searchOpen) {
                                onSearchOpenChange(true)
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.trim().isNotEmpty()) {
                                onSearch()
                                onSearchOpenChange(false)
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (searchOpen && searchText.isNotBlank() && suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 3.dp
                    ) {
                        Column {
                            suggestions.take(6).forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            onSuggestionClick(suggestion)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                modifier = Modifier.size(
                    when {
                        metrics.compactLandscape -> 44.dp
                        metrics.largeControls -> 64.dp
                        else -> 48.dp
                    }
                ),
                onClick = {
                    if (searchText.trim().isNotEmpty()) {
                        onSearch()
                        onSearchOpenChange(false)
                        focusManager.clearFocus()
                    } else {
                        onSearchOpenChange(true)
                        focusRequester.requestFocus()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Tìm kiếm",
                    modifier = Modifier.size(
                        when {
                            metrics.compactLandscape -> 22.dp
                            metrics.largeControls -> 30.dp
                            else -> 24.dp
                        }
                    )
                )
            }
        }
    }

    if (centerSearch) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                Brand()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                SearchBar()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Brand()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                SearchBar()
            }
        }
    }
}

@Composable
fun VideoItem(
    video: Video,
    onClick: () -> Unit
) {
    val metrics = rememberMpTubeUiMetrics()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (metrics.compactLandscape) metrics.contentWidthFraction else 1f)
                .padding(
                    horizontal = if (metrics.compactLandscape) 8.dp else if (metrics.largeControls) 24.dp else 16.dp
                )
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Thumbnail: ${video.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = metrics.titleSize
                ),
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = video.channel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = metrics.bodySize
                ),
                maxLines = 1
            )
        }
    }
}
