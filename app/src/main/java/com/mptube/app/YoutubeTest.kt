package com.mptube.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun testYoutubeApi(
    context: Context
): String {
    return withContext(Dispatchers.IO) {
        try {
            val response =
                YoutubeRepository(context)
                    .searchVideos("Goku")

            "Kết quả cho ${response.items.size} video"

        } catch (e: Exception) {
            "Lỗi API: ${e.message}"
        }
    }
}