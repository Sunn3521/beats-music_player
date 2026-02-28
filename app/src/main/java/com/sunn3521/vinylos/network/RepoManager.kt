package com.sunn3521.vinylos.network

import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.data.model.SongResponse
import com.sunn3521.vinylos.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object RepoManager {

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun fetchSongs(): List<Song> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(Constants.BASE_URL + "songs.json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<SongResponse>(body)
                parsed.songs
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
