package com.sunn3521.vinylos.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.sunn3521.vinylos.data.VisualizerPrefs
import com.sunn3521.vinylos.player.MusicService
import kotlinx.coroutines.delay

class LockVisualizerActivity : ComponentActivity() {

    private var musicService by mutableStateOf<MusicService?>(null)
    private var isBound by mutableStateOf(false)
    private lateinit var visualizerPrefs: VisualizerPrefs

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STOP_VISUALIZER") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visualizerPrefs = VisualizerPrefs(this)

        // Always-On style flags
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Initial Dim
        window.attributes = window.attributes.apply {
            screenBrightness = 0.05f
        }

        // Setup lock screen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter("STOP_VISUALIZER"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, IntentFilter("STOP_VISUALIZER"))
        }

        setContent {
            val settings by visualizerPrefs.settings.collectAsState(initial = VisualizerSettings())
            val service = musicService

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.let {
                        it.hide(WindowInsets.Type.systemBars())
                        it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
            }

            // Auto Dim further after 20 seconds
            LaunchedEffect(Unit) {
                delay(20000)
                window.attributes = window.attributes.apply {
                    screenBrightness = 0.03f
                }
            }

            if (isBound && service != null) {
                LockModeScreen(
                    musicService = service,
                    settings = settings,
                    onSettingsChange = { /* Persistent settings updated via ViewModel in app */ },
                    onExit = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {}
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
