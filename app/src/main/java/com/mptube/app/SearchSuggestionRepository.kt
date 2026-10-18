package com.mptube.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import java.util.LinkedHashMap

class SearchSuggestionRepository
{
    private data class CacheEntry(
        val createdAt: Long,
        val suggestions: List<String>
    )

    private val client = NetworkClients.suggestions

    private val cache = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true)
    {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean
        {
            return size > MAX_CACHE_ENTRIES
        }
    }

    suspend fun getSuggestions(rawQuery: String): List<String> = withContext(Dispatchers.IO)
    {
        val query = rawQuery.trim().replace(WHITESPACE_REGEX, " ")
        if (query.isBlank()) return@withContext emptyList()
        val cacheKey = query.lowercase()

        synchronized(cache)
        {
            val cached = cache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.createdAt <= CACHE_TTL_MS)
            {
                return@withContext cached.suggestions
            }
        }

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client", "firefox")
            .addQueryParameter("ds", "yt")
            .addQueryParameter("hl", "vi")
            .addQueryParameter("q", query)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        val suggestions = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList()

            val root = JSONArray(response.body.string())
            if (root.length() < 2) return@use emptyList()

            val items = root.optJSONArray(1) ?: return@use emptyList()
            buildList {
                for (i in 0 until items.length())
                {
                    val value = items.optString(i).trim()
                    if (value.isNotBlank() && !contains(value)) add(value)
                    if (size >= MAX_SUGGESTIONS) break
                }
            }
        }

        synchronized(cache)
        {
            cache[cacheKey] = CacheEntry(System.currentTimeMillis(), suggestions)
        }

        suggestions
    }

    companion object
    {
        private const val BASE_URL = "https://suggestqueries.google.com/complete/search"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 32
        private const val MAX_SUGGESTIONS = 6
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
