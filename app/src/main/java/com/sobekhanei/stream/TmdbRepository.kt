package com.sobekhanei.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MediaItem(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val mediaType: String
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val year: String?
        get() = releaseDate?.take(4)?.takeIf { it.length == 4 }
}

class TmdbRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun trending(type: String): List<MediaItem> = requestList(
        "trending/$type/day?language=en-US"
    , type)

    suspend fun search(type: String, query: String): List<MediaItem> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return requestList("search/$type?query=$encoded&include_adult=false&language=en-US", type)
    }

    private suspend fun requestList(path: String, type: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val key = BuildConfig.TMDB_API_KEY
            require(key.isNotBlank() && !key.contains("PASTE_")) {
                "TMDB API key missing. Copy local.properties.example to local.properties and add TMDB_API_KEY."
            }
            val url = "https://api.themoviedb.org/3/$path&api_key=$key"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("TMDB request failed: HTTP ${response.code}")
                val body = response.body?.string() ?: error("Empty response from TMDB")
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                buildList {
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val id = obj.optInt("id")
                        if (id <= 0) continue
                        val title = if (type == "movie") obj.optString("title") else obj.optString("name")
                        val date = if (type == "movie") obj.optString("release_date") else obj.optString("first_air_date")
                        add(
                            MediaItem(
                                id = id,
                                title = title.ifBlank { "Untitled" },
                                overview = obj.optString("overview"),
                                posterPath = obj.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                                backdropPath = obj.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
                                releaseDate = date.takeIf { it.isNotBlank() },
                                mediaType = type
                            )
                        )
                    }
                }
            }
        }
}
