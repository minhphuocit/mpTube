package com.mptube.app

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class YoutubeRepository(
    context: Context
) {

    companion object {

        private val searchCache =
            mutableMapOf<String, YoutubeSearchResponse>()

        private val relatedCache =
            mutableMapOf<String, YoutubeRelatedResponse>()

        private val searchTimeCache =
            mutableMapOf<String, Long>()

        private val relatedTimeCache =
            mutableMapOf<String, Long>()

        private val searchRequests =
            mutableMapOf<
                    String,
                    CompletableDeferred<YoutubeSearchResponse>
                    >()

        private val relatedRequests =
            mutableMapOf<
                    String,
                    CompletableDeferred<YoutubeRelatedResponse>
                    >()

        private val mutex =
            Mutex()

        private const val CACHE_DURATION =
            24L * 60L * 60L * 1000L

        private const val MAX_SEARCH_RESULTS =
            50

        private const val MAX_RELATED_RESULTS =
            15
        private const val MAX_MEMORY_CACHE_ENTRIES =
            20
    }

    private val preferences =
        context.getSharedPreferences(
            "youtube_cache",
            Context.MODE_PRIVATE
        )

    private val gson =
        Gson()

    private fun requireApiKey(): String {
        return BuildConfig.YOUTUBE_API_KEY.trim().also { key ->
            check(key.isNotEmpty()) {
                "YouTube API key is missing. Add YOUTUBE_API_KEY to local.properties."
            }
        }
    }

    private fun cachePreferenceKey(prefix: String, cacheKey: String): String {
        val digest = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(cacheKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "${prefix}_$digest"
    }

    private fun trimSearchMemoryCache() {
        while (searchCache.size > MAX_MEMORY_CACHE_ENTRIES) {
            val key = searchCache.keys.firstOrNull() ?: break
            searchCache.remove(key)
            searchTimeCache.remove(key)
        }
    }

    private fun trimRelatedMemoryCache() {
        while (relatedCache.size > MAX_MEMORY_CACHE_ENTRIES) {
            val key = relatedCache.keys.firstOrNull() ?: break
            relatedCache.remove(key)
            relatedTimeCache.remove(key)
        }
    }

    // =========================================================
    // SEARCH VIDEOS
    // =========================================================

    suspend fun searchVideos(
        query: String,
        videoCategoryId: String? = null,
        pageToken: String? = null,
        maxResults: Int = MAX_SEARCH_RESULTS
    ): YoutubeSearchResponse {

        val normalizedQuery =
            query.trim().lowercase()

        if (normalizedQuery.isEmpty()) {
            return YoutubeSearchResponse(
                nextPageToken = null,
                items = emptyList()
            )
        }

        val cacheKey =
            buildSearchKey(
                normalizedQuery,
                videoCategoryId,
                pageToken
            )

        val now =
            System.currentTimeMillis()

        // -----------------------------------------------------
        // 1. RAM CACHE
        // -----------------------------------------------------

        mutex.withLock {

            val cached =
                searchCache[cacheKey]

            val cachedTime =
                searchTimeCache[cacheKey]
                    ?: 0L

            if (
                cached != null &&
                now - cachedTime < CACHE_DURATION
            ) {
                return cached
            }
        }

        // -----------------------------------------------------
        // 2. DISK CACHE
        // -----------------------------------------------------

        val diskResult =
            readSearchCache(
                cacheKey,
                now
            )

        if (diskResult != null) {

            mutex.withLock {

                searchCache[cacheKey] =
                    diskResult

                searchTimeCache[cacheKey] =
                    now
            }

            return diskResult
        }

        // -----------------------------------------------------
        // 3. REQUEST DEDUP
        // -----------------------------------------------------

        var isOwner = false

        val deferred =
            mutex.withLock {

                searchRequests[cacheKey]
                    ?: CompletableDeferred<YoutubeSearchResponse>()
                        .also {

                            searchRequests[cacheKey] =
                                it

                            isOwner = true
                        }
            }

        // Request đã tồn tại -> chỉ chờ request đang chạy
        if (!isOwner) {
            return deferred.await()
        }

        // -----------------------------------------------------
        // 4. API
        // -----------------------------------------------------

        return try {

            val response =
                YoutubeApi.service.searchVideos(

                    query =
                        normalizedQuery,

                    maxResult =
                        maxResults.coerceIn(
                            1,
                            MAX_SEARCH_RESULTS
                        ),

                    pageToken =
                        pageToken,

                    apiKey =
                        requireApiKey()
                )

            // -------------------------------------------------
            // 5. SAVE RAM CACHE
            // -------------------------------------------------

            mutex.withLock {

                searchCache[cacheKey] =
                    response

                searchTimeCache[cacheKey] =
                    System.currentTimeMillis()
                trimSearchMemoryCache()
            }

            // -------------------------------------------------
            // 6. SAVE DISK CACHE
            // -------------------------------------------------

            saveSearchCache(
                cacheKey,
                response,
                System.currentTimeMillis()
            )

            // -------------------------------------------------
            // 7. WAKE WAITING REQUESTS
            // -------------------------------------------------

            deferred.complete(
                response
            )

            response

        } catch (e: Exception) {

            deferred.completeExceptionally(
                e
            )

            throw e

        } finally {

            mutex.withLock {

                if (
                    searchRequests[cacheKey] ===
                    deferred
                ) {
                    searchRequests.remove(
                        cacheKey
                    )
                }
            }
        }
    }

    // =========================================================
    // RELATED VIDEOS
    // =========================================================

    suspend fun getRelatedVideos(
        videoId: String,
        videoTitle: String,
        pageToken: String? = null
    ): YoutubeRelatedResponse {

        val cacheKey =
            buildRelatedKey(
                videoId,
                pageToken
            )

        val now =
            System.currentTimeMillis()

        // -----------------------------------------------------
        // 1. RAM CACHE
        // -----------------------------------------------------

        mutex.withLock {

            val cached =
                relatedCache[cacheKey]

            val cachedTime =
                relatedTimeCache[cacheKey]
                    ?: 0L

            if (
                cached != null &&
                now - cachedTime < CACHE_DURATION
            ) {
                return cached
            }
        }

        // -----------------------------------------------------
        // 2. DISK CACHE
        // -----------------------------------------------------

        val diskResult =
            readRelatedCache(
                cacheKey,
                now
            )

        if (diskResult != null) {

            mutex.withLock {

                relatedCache[cacheKey] =
                    diskResult

                relatedTimeCache[cacheKey] =
                    now
            }

            return diskResult
        }

        // -----------------------------------------------------
        // 3. REQUEST DEDUP
        // -----------------------------------------------------

        var isOwner = false

        val deferred =
            mutex.withLock {

                relatedRequests[cacheKey]
                    ?: CompletableDeferred<YoutubeRelatedResponse>()
                        .also {

                            relatedRequests[cacheKey] =
                                it

                            isOwner = true
                        }
            }

        if (!isOwner) {
            return deferred.await()
        }

        // -----------------------------------------------------
        // 4. API
        // -----------------------------------------------------

        return try {

            val response =
                YoutubeApi.service.searchVideos(

                    query =
                        videoTitle
                            .trim()
                            .replace(Regex("\\s+"), " ")
                            .take(100),

                    maxResult =
                        MAX_RELATED_RESULTS,

                    pageToken =
                        pageToken,

                    apiKey =
                        BuildConfig.YOUTUBE_API_KEY
                )

            val filteredItems =
                response.items
                    .filter { item ->
                        item.id.videoId != videoId
                    }
                    .distinctBy { item ->
                        item.id.videoId
                    }
                    .take(15)

            val result =
                YoutubeRelatedResponse(

                    nextPageToken =
                        response.nextPageToken,

                    items =
                        filteredItems
                )

            // -------------------------------------------------
            // 5. RAM CACHE
            // -------------------------------------------------

            mutex.withLock {

                relatedCache[cacheKey] =
                    result

                relatedTimeCache[cacheKey] =
                    System.currentTimeMillis()

                trimRelatedMemoryCache()
            }

            // -------------------------------------------------
            // 6. DISK CACHE
            // -------------------------------------------------

            saveRelatedCache(
                cacheKey,
                result,
                System.currentTimeMillis()
            )

            // -------------------------------------------------
            // 7. COMPLETE
            // -------------------------------------------------

            deferred.complete(
                result
            )

            result

        } catch (e: Exception) {

            deferred.completeExceptionally(
                e
            )

            throw e

        } finally {

            mutex.withLock {

                if (
                    relatedRequests[cacheKey] ===
                    deferred
                ) {
                    relatedRequests.remove(
                        cacheKey
                    )
                }
            }
        }
    }

    // =========================================================
// AUTOPLAY VIDEO
// =========================================================

    suspend fun getAutoplayVideo(
        currentVideoId: String,
        currentVideoTitle: String
    ): Video? {

        val profile =
            buildAutoplayProfile(
                currentVideoTitle
            )

        val response =
            searchVideos(

                query =
                    profile.query,

                videoCategoryId =
                    profile.categoryId,

                maxResults =
                    1
            )

        return response.items
            .asSequence()
            .filter {
                it.id.videoId.isNotBlank()
            }
            .filter {
                it.id.videoId != currentVideoId
            }
            .map {
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
            .firstOrNull()
    }
    private data class AutoplayProfile(
        val query: String,
        val categoryId: String?
    )
    private fun buildAutoplayProfile(
        title: String
    ): AutoplayProfile {

        val text =
            title
                .lowercase()
                .replace(
                    Regex("[^\\p{L}\\p{N}\\s]"),
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        return when {

            text.contains("bolero") -> {

                AutoplayProfile(
                    query =
                        "bolero Việt Nam",
                    categoryId =
                        "10"
                )
            }

            text.contains("remix") ||
                    text.contains("vinahouse") ||
                    text.contains("dj") ||
                    text.contains("edm") -> {

                AutoplayProfile(
                    query =
                        "nhạc remix Việt Nam trending",
                    categoryId =
                        "10"
                )
            }

            text.contains("lofi") -> {

                AutoplayProfile(
                    query =
                        "lofi Việt Nam",
                    categoryId =
                        "10"
                )
            }

            text.contains("rap") ||
                    text.contains("hip hop") ||
                    text.contains("hiphop") -> {

                AutoplayProfile(
                    query =
                        "rap Việt trending",
                    categoryId =
                        "10"
                )
            }

            text.contains("nhạc") ||
                    text.contains("music") ||
                    text.contains("lyrics") ||
                    text.contains("lyric") ||
                    text.contains("karaoke") ||
                    text.contains("ballad") ||
                    text.contains("cover") ||
                    text.contains("mv") -> {

                AutoplayProfile(
                    query =
                        "nhạc Việt trending",
                    categoryId =
                        "10"
                )
            }

            else -> {

                val words =
                    text
                        .split(" ")
                        .filter {
                            it.length >= 3
                        }
                        .take(5)

                AutoplayProfile(
                    query =
                        words.joinToString(" "),
                    categoryId =
                        null
                )
            }
        }
    }
    suspend fun getTrendingVideos(): List<Video> {

        val response =
            YoutubeApi.service.getTrendingVideos(
                apiKey = requireApiKey()
            )

        return response.items.mapNotNull { item ->

            val thumbnail =
                item.snippet.thumbnails.high.url

            if (item.id.isBlank()) {
                null
            } else {
                Video(
                    videoId = item.id,
                    title = item.snippet.title,
                    channel = item.snippet.channelTitle,
                    thumbnailUrl = thumbnail
                )
            }
        }
    }
    // =========================================================
    // SEARCH CACHE KEY
    // =========================================================

    private fun buildSearchKey(
        query: String,
        categoryId: String?,
        pageToken: String?
    ): String {

        return buildString {

            append("search|")
            append(query)

            append("|category=")
            append(
                categoryId ?: "all"
            )

            append("|page=")
            append(
                pageToken ?: "first"
            )
        }
    }

    // =========================================================
    // RELATED CACHE KEY
    // =========================================================

    private fun buildRelatedKey(
        videoId: String,
        pageToken: String?
    ): String {

        return buildString {

            append("related|")
            append(videoId)

            append("|page=")
            append(
                pageToken ?: "first"
            )
        }
    }

    // =========================================================
    // DISK CACHE
    // =========================================================

    private suspend fun readSearchCache(
        cacheKey: String,
        now: Long
    ): YoutubeSearchResponse? = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey("search", cacheKey)
        val json = preferences.getString(key, null) ?: return@withContext null
        val savedTime = preferences.getLong("${key}_time", 0L)

        if (now - savedTime >= CACHE_DURATION) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            return@withContext null
        }

        try {
            gson.fromJson(json, YoutubeSearchResponse::class.java)
        } catch (_: Exception) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            null
        }
    }

    private suspend fun saveSearchCache(
        cacheKey: String,
        response: YoutubeSearchResponse,
        time: Long
    ) = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey("search", cacheKey)
        preferences.edit {
            putString(key, gson.toJson(response))
            putLong("${key}_time", time)
        }
    }

    private suspend fun readRelatedCache(
        cacheKey: String,
        now: Long
    ): YoutubeRelatedResponse? = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey("related", cacheKey)
        val json = preferences.getString(key, null) ?: return@withContext null
        val savedTime = preferences.getLong("${key}_time", 0L)

        if (now - savedTime >= CACHE_DURATION) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            return@withContext null
        }

        try {
            gson.fromJson(json, YoutubeRelatedResponse::class.java)
        } catch (_: Exception) {
            preferences.edit {
                remove(key)
                remove("${key}_time")
            }
            null
        }
    }

    private suspend fun saveRelatedCache(
        cacheKey: String,
        response: YoutubeRelatedResponse,
        time: Long
    ) = withContext(Dispatchers.IO) {
        val key = cachePreferenceKey("related", cacheKey)
        preferences.edit {
            putString(key, gson.toJson(response))
            putLong("${key}_time", time)
        }
    }

}
