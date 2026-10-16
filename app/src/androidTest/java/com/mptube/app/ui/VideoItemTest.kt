package com.mptube.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mptube.app.model.Video
import com.mptube.app.ui.components.VideoItem
import org.junit.Rule
import org.junit.Test

class VideoItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysVideoTitleAndChannel() {
        val video = Video(
            videoId = "video-id",
            title = "Video test",
            channel = "Kênh test",
            thumbnailUrl = ""
        )

        composeRule.setContent {
            MaterialTheme {
                VideoItem(video = video, onClick = {})
            }
        }

        composeRule.onNodeWithText("Video test").assertIsDisplayed()
        composeRule.onNodeWithText("Kênh test").assertIsDisplayed()
    }
}
