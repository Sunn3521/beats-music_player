package com.sunn3521.vinylos.player

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sunn3521.vinylos.data.model.Song
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class AudioManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPosition by mutableStateOf(0)
        private set

    var duration by mutableStateOf(0)
        private set

    fun play(file: File, song: Song) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                val fis = FileInputStream(file)
                setDataSource(fis.fd)
                fis.close()

                setOnPreparedListener { mp ->
                    mp.start()
                    this@AudioManager.isPlaying = true
                    this@AudioManager.duration = mp.duration
                    startProgressUpdater()
                }

                setOnCompletionListener {
                    this@AudioManager.isPlaying = false
                    this@AudioManager.currentPosition = 0
                }

                setOnErrorListener { _, what, extra ->
                    this@AudioManager.isPlaying = false
                    android.util.Log.e("AudioManager", "MediaPlayer error: $what, $extra")
                    true
                }

                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                this@AudioManager.isPlaying = false
            }
        }

        currentSong = song
    }

    private fun startProgressUpdater() {
        Thread {
            while (mediaPlayer != null && isPlaying) {
                try {
                    val pos = mediaPlayer?.currentPosition ?: 0
                    currentPosition = pos
                } catch (e: IllegalStateException) {
                    break
                }
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
            currentPosition = position
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            startProgressUpdater()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
