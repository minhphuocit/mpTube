package com.mptube.app

import com.mptube.app.model.Video
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SerpApiRepository {
    suspend fun getRelatedVideos(videoId: String): List<Video> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            val cached = cache[videoId]
            if (cached != null && now - cached.time < CACHE_DURATION) return cached.videos
        }

        var owner = false
        val deferred = mutex.withLock {
            requests[videoId] ?: CompletableDeferred<List<Video>>().also {
                requests[videoId] = it
                owner = true
            }
        }

        if (!owner) return deferred.await()

        return try {
            val videos = SerpApi.service.getRelatedVideos(videoId)
                .videos
                .asSequence()
                .filter { it.videoId.isNotBlank() && it.videoId != videoId }
                .map {
                    Video(
                        videoId = it.videoId,
                        title = it.title,
                        channel = it.channel,
                        thumbnailUrl = it.thumbnail
                    )
                }
                .distinctBy { it.videoId }
                .take(MAX_RESULTS)
                .toList()

            if (videos.isNotEmpty()) {
                val savedAt = System.currentTimeMillis()
                mutex.withLock {
                    cache[videoId] = CacheEntry(videos, savedAt)
                    trimCache()
                }
            }
            deferred.complete(videos)
            videos
        } catch (error: Exception) {
            deferred.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (requests[videoId] === deferred) requests.remove(videoId)
            }
        }
    }

    companion object {
        private data class CacheEntry(
            val videos: List<Video>,
            val time: Long
        )

        private const val CACHE_DURATION = 24L * 60L * 60L * 1000L
        private const val MAX_CACHE_ENTRIES = 24
        private const val MAX_RESULTS = 20

        private val mutex = Mutex()
        private val cache = LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true)
        private val requests = mutableMapOf<String, CompletableDeferred<List<Video>>>()

        private fun trimCache() {
            while (cache.size > MAX_CACHE_ENTRIES) {
                val first = cache.keys.firstOrNull() ?: return
                cache.remove(first)
            }
        }
    }
}
