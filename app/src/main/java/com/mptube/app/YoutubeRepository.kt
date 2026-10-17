package com.mptube.app

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class YoutubeRepository(context: Context) {
    companion object {
        private data class CacheEntry<T>(val value: T, val time: Long)

        private val mutex = Mutex()
        private val searchCache = LinkedHashMap<String, CacheEntry<YoutubeSearchResponse>>()
        private val relatedCache = LinkedHashMap<String, CacheEntry<YoutubeRelatedResponse>>()
        private val musicCache = LinkedHashMap<String, CacheEntry<YoutubeTrendingResponse>>()
        private val categoryCache = LinkedHashMap<String, CacheEntry<String>>()
        private val searchRequests = mutableMapOf<String, CompletableDeferred<YoutubeSearchResponse>>()
        private val relatedRequests = mutableMapOf<String, CompletableDeferred<YoutubeRelatedResponse>>()
        private val musicRequests = mutableMapOf<String, CompletableDeferred<YoutubeTrendingResponse>>()

        private const val SEARCH_CACHE_DURATION = 12L * 60L * 60L * 1000L
        private const val RELATED_CACHE_DURATION = 24L * 60L * 60L * 1000L
        private const val MUSIC_CACHE_DURATION = 15L * 60L * 1000L
        private const val VIDEO_CATEGORY_CACHE_DURATION = 24L * 60L * 60L * 1000L
        private const val DEFAULT_SEARCH_RESULTS = 25
        private const val API_MAX_RESULTS = 50
        private const val MAX_RELATED_RESULTS = 20
        private const val MAX_VIDEO_DETAILS_IDS = 50
        private const val MAX_MEMORY_CACHE_ENTRIES = 24
        private const val MAX_DISK_CACHE_ENTRIES_PER_TYPE = 24
        private const val MUSIC_CATEGORY_ID = "10"
    }

    private val preferences = context.getSharedPreferences("youtube_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun requireApiKey(): String {
        return BuildConfig.YOUTUBE_API_KEY.trim().also { key ->
            check(key.isNotEmpty()) {
                "YouTube API key is missing. Add YOUTUBE_API_KEY to local.properties."
            }
        }
    }

    suspend fun searchVideos(
        query: String,
        videoCategoryId: String? = null,
        pageToken: String? = null,
        maxResults: Int = DEFAULT_SEARCH_RESULTS,
        forceRefresh: Boolean = false
    ): YoutubeSearchResponse {
        val normalizedQuery = query.trim().replace(Regex("\\s+"), " ").lowercase()
        if (normalizedQuery.isEmpty()) return YoutubeSearchResponse(null, emptyList())

        val resultCount = maxResults.coerceIn(1, API_MAX_RESULTS)
        val cacheKey = buildSearchKey(normalizedQuery, videoCategoryId, pageToken, resultCount)
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            mutex.withLock {
                val cached = searchCache[cacheKey]
                if (cached != null && now - cached.time < SEARCH_CACHE_DURATION) return cached.value
            }

            readDiskCache(
                prefix = "search",
                cacheKey = cacheKey,
                now = now,
                duration = SEARCH_CACHE_DURATION,
                clazz = YoutubeSearchResponse::class.java
            )?.let { cached ->
                mutex.withLock {
                    searchCache[cacheKey] = CacheEntry(cached, now)
                    trimMemoryCache(searchCache)
                }
                return cached
            }
        }

        var owner = false
        val deferred = mutex.withLock {
            searchRequests[cacheKey] ?: CompletableDeferred<YoutubeSearchResponse>().also {
                searchRequests[cacheKey] = it
                owner = true
            }
        }

        if (!owner) return deferred.await()

        return try {
            val response = YoutubeApi.service.searchVideos(
                query = normalizedQuery,
                maxResult = resultCount,
                pageToken = pageToken,
                videoCategoryId = videoCategoryId,
                apiKey = requireApiKey()
            )

            val savedAt = System.currentTimeMillis()
            mutex.withLock {
                searchCache[cacheKey] = CacheEntry(response, savedAt)
                trimMemoryCache(searchCache)
            }
            saveDiskCache("search", cacheKey, response, savedAt)
            deferred.complete(response)
            response
        } catch (error: Exception) {
            deferred.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (searchRequests[cacheKey] === deferred) searchRequests.remove(cacheKey)
            }
        }
    }

    suspend fun getRelatedVideos(
        videoId: String,
        videoTitle: String,
        forceRefresh: Boolean = false,
        musicOnly: Boolean = false
    ): YoutubeRelatedResponse {
        val cacheKey = "related|$videoId|music=$musicOnly"
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            mutex.withLock {
                val cached = relatedCache[cacheKey]
                if (cached != null && now - cached.time < RELATED_CACHE_DURATION) return cached.value
            }

            readDiskCache(
                prefix = "related",
                cacheKey = cacheKey,
                now = now,
                duration = RELATED_CACHE_DURATION,
                clazz = YoutubeRelatedResponse::class.java
            )?.let { cached ->
                mutex.withLock {
                    relatedCache[cacheKey] = CacheEntry(cached, now)
                    trimMemoryCache(relatedCache)
                }
                return cached
            }
        }

        var owner = false
        val deferred = mutex.withLock {
            relatedRequests[cacheKey] ?: CompletableDeferred<YoutubeRelatedResponse>().also {
                relatedRequests[cacheKey] = it
                owner = true
            }
        }

        if (!owner) return deferred.await()

        return try {
            val response = YoutubeApi.service.searchVideos(
                query = buildRelatedQuery(videoTitle),
                videoCategoryId = if (musicOnly || looksLikeMusic(videoTitle)) MUSIC_CATEGORY_ID else null,
                maxResult = MAX_RELATED_RESULTS,
                apiKey = requireApiKey()
            )

            val result = YoutubeRelatedResponse(
                nextPageToken = response.nextPageToken,
                items = response.items
                    .asSequence()
                    .filter { it.id.videoId.isNotBlank() && it.id.videoId != videoId }
                    .distinctBy { it.id.videoId }
                    .take(MAX_RELATED_RESULTS)
                    .toList()
            )

            val savedAt = System.currentTimeMillis()
            mutex.withLock {
                relatedCache[cacheKey] = CacheEntry(result, savedAt)
                trimMemoryCache(relatedCache)
            }
            saveDiskCache("related", cacheKey, result, savedAt)
            deferred.complete(result)
            result
        } catch (error: Exception) {
            deferred.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (relatedRequests[cacheKey] === deferred) relatedRequests.remove(cacheKey)
            }
        }
    }

    suspend fun getTrendingMusic(
        pageToken: String? = null,
        maxResults: Int = 25,
        forceRefresh: Boolean = false
    ): YoutubeTrendingResponse {
        val resultCount = maxResults.coerceIn(1, API_MAX_RESULTS)
        val cacheKey = "music|VN|$MUSIC_CATEGORY_ID|${pageToken ?: "first"}|$resultCount"
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            mutex.withLock {
                val cached = musicCache[cacheKey]
                if (cached != null && now - cached.time < MUSIC_CACHE_DURATION) return cached.value
            }

            readDiskCache(
                prefix = "music",
                cacheKey = cacheKey,
                now = now,
                duration = MUSIC_CACHE_DURATION,
                clazz = YoutubeTrendingResponse::class.java
            )?.let { cached ->
                mutex.withLock {
                    musicCache[cacheKey] = CacheEntry(cached, now)
                    trimMemoryCache(musicCache)
                }
                return cached
            }
        }

        var owner = false
        val deferred = mutex.withLock {
            musicRequests[cacheKey] ?: CompletableDeferred<YoutubeTrendingResponse>().also {
                musicRequests[cacheKey] = it
                owner = true
            }
        }

        if (!owner) return deferred.await()

        return try {
            val response = YoutubeApi.service.getTrendingVideos(
                regionCode = "VN",
                videoCategoryId = MUSIC_CATEGORY_ID,
                maxResults = resultCount,
                pageToken = pageToken,
                apiKey = requireApiKey()
            )

            val savedAt = System.currentTimeMillis()
            mutex.withLock {
                musicCache[cacheKey] = CacheEntry(response, savedAt)
                trimMemoryCache(musicCache)
            }
            saveDiskCache("music", cacheKey, response, savedAt)
            deferred.complete(response)
            response
        } catch (error: Exception) {
            deferred.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (musicRequests[cacheKey] === deferred) musicRequests.remove(cacheKey)
            }
        }
    }


    suspend fun getVideoCategoryId(videoId: String): String? {
        return getVideoCategoryIds(listOf(videoId))[videoId]
    }

    suspend fun getVideoCategoryIds(videoIds: List<String>): Map<String, String> {
        val ids = videoIds
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_VIDEO_DETAILS_IDS)
            .toList()

        if (ids.isEmpty()) return emptyMap()

        val now = System.currentTimeMillis()
        val result = LinkedHashMap<String, String>()
        val missing = mutableListOf<String>()

        mutex.withLock {
            ids.forEach { id ->
                val cached = categoryCache[id]
                if (cached != null && now - cached.time < VIDEO_CATEGORY_CACHE_DURATION) {
                    result[id] = cached.value
                } else {
                    missing += id
                }
            }
        }

        if (missing.isNotEmpty()) {
            val response = YoutubeApi.service.getVideoDetails(
                videoIds = missing.joinToString(","),
                apiKey = requireApiKey()
            )
            val savedAt = System.currentTimeMillis()

            mutex.withLock {
                response.items.forEach { item ->
                    if (item.id.isNotBlank() && item.snippet.categoryId.isNotBlank()) {
                        result[item.id] = item.snippet.categoryId
                        categoryCache[item.id] = CacheEntry(item.snippet.categoryId, savedAt)
                    }
                }
                trimMemoryCache(categoryCache)
            }
        }

        return result
    }

    private fun buildRelatedQuery(title: String): String {
        val cleaned = title
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val ignored = setOf(
            "official", "video", "lyrics", "lyric", "audio", "mv", "hd", "4k",
            "vietsub", "karaoke", "full", "music", "nhạc"
        )

        val words = cleaned.split(" ")
            .filter { it.length >= 2 && it.lowercase() !in ignored }
            .take(8)

        return words.joinToString(" ").ifBlank { cleaned.take(100) }
    }

    private fun looksLikeMusic(title: String): Boolean {
        val text = title.lowercase()
        return listOf(
            "music", "nhạc", "lyrics", "lyric", "mv", "audio", "remix", "lofi",
            "cover", "karaoke", "dj", "edm", "ballad", "rap"
        ).any(text::contains)
    }

    private fun buildSearchKey(
        query: String,
        categoryId: String?,
        pageToken: String?,
        maxResults: Int
    ): String {
        return "search|$query|category=${categoryId ?: "all"}|page=${pageToken ?: "first"}|max=$maxResults"
    }

    private fun cachePreferenceKey(prefix: String, cacheKey: String): String {
        val digest = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(cacheKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "${prefix}_$digest"
    }

    private fun <T> trimMemoryCache(cache: LinkedHashMap<String, CacheEntry<T>>) {
        while (cache.size > MAX_MEMORY_CACHE_ENTRIES) {
            val first = cache.keys.firstOrNull() ?: return
            cache.remove(first)
        }
    }

    private suspend fun <T : Any> readDiskCache(
        prefix: String,
        cacheKey: String,
        now: Long,
        duration: Long,
        clazz: Class<T>
    ): T? = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey(prefix, cacheKey)
        val json = preferences.getString(key, null) ?: return@withContext null
        val savedTime = preferences.getLong("${key}_time", 0L)

        if (now - savedTime >= duration) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            return@withContext null
        }

        try {
            gson.fromJson(json, clazz)
        } catch (_: Exception) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            null
        }
    }

    private suspend fun saveDiskCache(
        prefix: String,
        cacheKey: String,
        value: Any,
        time: Long
    ) = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey(prefix, cacheKey)
        preferences.edit {
            putString(key, gson.toJson(value))
            putLong("${key}_time", time)
        }
        pruneDiskCache(prefix)
    }

    private fun pruneDiskCache(prefix: String) {
        val dataKeys = preferences.all.keys
            .filter { it.startsWith("${prefix}_") && !it.endsWith("_time") }

        if (dataKeys.size <= MAX_DISK_CACHE_ENTRIES_PER_TYPE) return

        val removeCount = dataKeys.size - MAX_DISK_CACHE_ENTRIES_PER_TYPE
        val keysToRemove = dataKeys
            .sortedBy { preferences.getLong("${it}_time", 0L) }
            .take(removeCount)

        preferences.edit {
            keysToRemove.forEach { key ->
                remove(key)
                remove("${key}_time")
            }
        }
    }
}
