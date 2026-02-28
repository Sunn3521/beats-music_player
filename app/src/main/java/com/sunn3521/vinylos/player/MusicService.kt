@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.sunn3521.vinylos.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sunn3521.vinylos.aod.AodActivity
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.ui.MainActivity
import com.sunn3521.vinylos.util.Constants
import java.io.File

class MusicService : Service() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.sunn3521.vinylos.PLAY_PAUSE"
        const val ACTION_NEXT = "com.sunn3521.vinylos.NEXT"
        const val ACTION_PREVIOUS = "com.sunn3521.vinylos.PREVIOUS"
        const val ACTION_STOP = "com.sunn3521.vinylos.STOP"
        const val ACTION_CLOSE_AOD = "com.sunn3521.vinylos.CLOSE_AOD"
    }

    private val binder = MusicBinder()
    lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSessionCompat? = null
    private var visualizer: Visualizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "vinylos_playback"

    var currentSong by mutableStateOf<Song?>(null)
    var songList: List<Song> by mutableStateOf(emptyList())

    var isPlaying by mutableStateOf(false)
    var isShuffle by mutableStateOf(false)
    var isRepeat by mutableStateOf(false)
    var isRepeatOne by mutableStateOf(false)

    var duration by mutableStateOf(0L)
    var currentPosition by mutableStateOf(0L)

    var fftMagnitudes by mutableStateOf(List(64) { 0f })

    private var pendingStopOnSongEnd = false

    private val updateProgressAction = object : Runnable {
        override fun run() {
            if (::exoPlayer.isInitialized) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                val d = exoPlayer.duration
                if (d > 0 && d != C.TIME_UNSET) {
                    duration = d
                }
                isPlaying = exoPlayer.isPlaying
            }
            handler.postDelayed(this, 500)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MusicService", "Received screen action: ${intent?.action}, isPlaying: $isPlaying")
            if ((intent?.action == Intent.ACTION_SCREEN_OFF || intent?.action == Intent.ACTION_SCREEN_ON) && isPlaying) {
                val aodIntent = Intent(this@MusicService, AodActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(aodIntent)
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )

        mediaSession = MediaSessionCompat(this, "VinylOS").apply {
            isActive = true
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    setupVisualizer()
                    startForegroundPlayback()
                } else {
                    releaseVisualizer()
                    updateNotification()
                    sendBroadcast(Intent(ACTION_CLOSE_AOD))
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                if (index in songList.indices) {
                    currentSong = songList[index]
                    duration = 0L
                    updateNotification()
                }
                
                if (pendingStopOnSongEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    pause()
                    pendingStopOnSongEnd = false
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0 && d != C.TIME_UNSET) {
                        duration = d
                    }
                } else if (state == Player.STATE_ENDED) {
                    if (isRepeatOne) {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    } else if (isRepeat || exoPlayer.hasNextMediaItem()) {
                        exoPlayer.seekToNext()
                        exoPlayer.play()
                    } else {
                        sendBroadcast(Intent(ACTION_CLOSE_AOD))
                    }
                    
                    if (pendingStopOnSongEnd) {
                        pause()
                        pendingStopOnSongEnd = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicService", "ExoPlayer Error: ${error.errorCodeName} - ${error.message}")
            }
        })
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        
        handler.post(updateProgressAction)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (exoPlayer.isPlaying) pause() else resume()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP -> {
                pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundPlayback() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Playback"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        
        val playPauseAction = NotificationCompat.Action(
            playPauseIcon, if (isPlaying) "Pause" else "Play",
            PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE)
        )

        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Next",
            PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE)
        )

        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Previous",
            PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.sunn3521.vinylos.R.drawable.ic_launcher_foreground)
            .setContentTitle(currentSong?.title ?: "VinylOS")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setOngoing(isPlaying)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun setupVisualizer() {
        try {
            if (visualizer != null) return
            val sessionId = exoPlayer.audioSessionId
            if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
            
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveForm: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) {
                            val magnitudes = FloatArray(64)
                            for (i in 0 until 64) {
                                val r = fft[i * 2].toInt()
                                val im = fft[i * 2 + 1].toInt()
                                magnitudes[i] = Math.hypot(r.toDouble(), im.toDouble()).toFloat()
                            }
                            fftMagnitudes = magnitudes.toList()
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Visualizer error: ${e.message}")
        }
    }

    private fun releaseVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }

    private fun songToMediaItem(song: Song): MediaItem {
        val uri = when {
            song.file.startsWith("http") || song.file.startsWith("content") -> Uri.parse(song.file)
            File(song.file).isAbsolute -> Uri.fromFile(File(song.file))
            else -> {
                val internalFile = File(filesDir, song.file)
                if (internalFile.exists()) {
                    Uri.fromFile(internalFile)
                } else {
                    Uri.parse(Constants.BASE_URL + (if (Constants.MUSIC_FOLDER.isNotEmpty()) Constants.MUSIC_FOLDER + "/" else "") + song.file)
                }
            }
        }
        
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.albumArt?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.file)
            .setMediaMetadata(metadata)
            .build()
    }

    fun setPlaylist(list: List<Song>, index: Int) {
        songList = list
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        list.forEach { song ->
            exoPlayer.addMediaItem(songToMediaItem(song))
        }
        exoPlayer.seekTo(index, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        currentSong = list.getOrNull(index)
    }

    fun startSmartPlaylist(song: Song, fullList: List<Song>) {
        songList = fullList
        val index = fullList.indexOfFirst { it.file == song.file }.coerceAtLeast(0)
        setPlaylist(fullList, index)
    }

    fun resume() { exoPlayer.play() }
    fun pause() { exoPlayer.pause() }
    fun next() { if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext() }
    fun previous() { if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() }
    
    fun seekTo(pos: Long) {
        exoPlayer.seekTo(pos)
        currentPosition = pos
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        exoPlayer.shuffleModeEnabled = isShuffle
    }

    fun toggleRepeat() {
        isRepeat = !isRepeat
        exoPlayer.repeatMode = if (isRepeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        if (isRepeat) isRepeatOne = false
    }

    fun toggleRepeatOne() {
        isRepeatOne = !isRepeatOne
        exoPlayer.repeatMode = if (isRepeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (isRepeatOne) isRepeat = false
    }

    fun startSleepTimer(ms: Long, finishCurrentSong: Boolean) {
        handler.removeCallbacksAndMessages("timer")
        handler.postDelayed({
            if (finishCurrentSong) {
                pendingStopOnSongEnd = true
            } else {
                pause()
            }
        }, ms)
    }

    fun cancelSleepTimer() {
        handler.removeCallbacksAndMessages("timer")
        pendingStopOnSongEnd = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        handler.removeCallbacksAndMessages(null)
        releaseVisualizer()
        mediaSession?.release()
        exoPlayer.release()
        sendBroadcast(Intent(ACTION_CLOSE_AOD))
    }
}
