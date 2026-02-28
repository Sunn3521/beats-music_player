package com.sunn3521.vinylos.data.repository

import android.content.Context
import android.util.Log
import com.sunn3521.vinylos.data.model.RemoteSong
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.network.RepoManager
import com.sunn3521.vinylos.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MusicRepository(private val context: Context) {

    fun isSongDownloaded(song: Song): Boolean {
        val file = getLocalFile(song)
        return file.exists() && file.length() > 0
    }

    fun getLocalFile(song: Song): File {
        return File(context.filesDir, song.file.substringAfterLast("/"))
    }

    suspend fun deleteSong(song: Song) {
        val file = getLocalFile(song)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun getSongs(): List<Song> {
        return RepoManager.fetchSongs()
    }

    suspend fun fetchRemoteSongs(): List<RemoteSong> = withContext(Dispatchers.IO) {
        val url = URL(Constants.BASE_URL + "songs.json")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("songs")
            val list = mutableListOf<RemoteSong>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val fileName = obj.getString("file")
                list.add(
                    RemoteSong(
                        id = i.toString(),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        albumArt = obj.optString("albumArt", null),
                        downloadUrl = Constants.BASE_URL + fileName
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error fetching remote songs", e)
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Download logic for RemoteSong objects
     */
    suspend fun getOrDownloadSong(
        remoteSong: RemoteSong,
        onProgress: (Float) -> Unit
    ): File = getOrDownloadSong(
        Song(
            title = remoteSong.title,
            artist = remoteSong.artist,
            file = remoteSong.downloadUrl.substringAfterLast("/"),
            albumArt = remoteSong.albumArt,
            downloadUrl = remoteSong.downloadUrl
        ),
        onProgress
    )

    suspend fun getOrDownloadSong(
        song: Song,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val file = getLocalFile(song)
        
        if (file.exists() && file.length() > 1024) return@withContext file

        // Prioritize song.downloadUrl, then song.file if it looks like a URL, then fallback to BASE_URL construction
        val url = song.downloadUrl ?: if (song.file.startsWith("http")) song.file else Constants.BASE_URL + song.file
        
        Log.d("MusicRepository", "Downloading: $url")
        val request = Request.Builder().url(url).build()

        try {
            val response = RepoManager.client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw IOException("Unexpected code $code for $url")
            }
            
            val body = response.body
            if (body == null) {
                response.close()
                throw IOException("Response body is null")
            }
            
            val contentLength = body.contentLength()
            
            // Ensure parent directories exist
            file.parentFile?.mkdirs()

            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            onProgress((totalRead.toFloat() / contentLength) * 100f)
                        }
                    }
                }
            }
            response.close()
        } catch (e: Exception) {
            Log.e("MusicRepository", "Download failed", e)
            if (file.exists()) file.delete()
            throw e
        }

        return@withContext file
    }
}
