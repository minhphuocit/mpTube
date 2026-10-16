package com.mptube.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mptube.app.R
import com.mptube.app.ui.util.rememberMpTubeUiMetrics

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isPlayerFullscreen by rememberSaveable { mutableStateOf(false) }
    val metrics = rememberMpTubeUiMetrics()

    BackHandler(enabled = !isPlayerFullscreen && selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        bottomBar = {
            if (!isPlayerFullscreen) {
                if (metrics.largeControls) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(metrics.navigationHeight)
                            .background(Color.Transparent),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LargeNavigationItem(
                            modifier = Modifier.weight(1f),
                            selected = selectedTab == 0,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_home),
                                    contentDescription = "Home",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = "Home",
                            onClick = { selectedTab = 0 }
                        )
                        LargeNavigationItem(
                            modifier = Modifier.weight(1f),
                            selected = selectedTab == 1,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music),
                                    contentDescription = "Âm nhạc",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = "Âm Nhạc",
                            onClick = { selectedTab = 1 }
                        )
                        LargeNavigationItem(
                            modifier = Modifier.weight(1f),
                            selected = selectedTab == 2,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = "Thông tin",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = "Thông Tin",
                            onClick = { selectedTab = 2 }
                        )
                    }
                } else {
                    NavigationBar(containerColor = Color.Transparent) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_home),
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music),
                                    contentDescription = "Âm nhạc"
                                )
                            },
                            label = { Text("Âm Nhạc") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = "Thông tin"
                                )
                            },
                            label = { Text("Thông Tin") }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onPlayerFullscreenChanged = { isPlayerFullscreen = it }
                )

                1 -> MusicScreen(
                    onPlayerFullscreenChanged = { isPlayerFullscreen = it }
                )

                else -> AboutScreen()
            }
        }
    }
}

@Composable
private fun LargeNavigationItem(
    modifier: Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    val metrics = rememberMpTubeUiMetrics()
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.bodySize)
        )
    }
}
