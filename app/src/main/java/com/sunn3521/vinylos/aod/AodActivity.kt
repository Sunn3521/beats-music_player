package com.sunn3521.vinylos.aod

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.sunn3521.vinylos.data.repository.MusicRepository
import com.sunn3521.vinylos.player.MusicService
import com.sunn3521.vinylos.ui.AodScreen
import com.sunn3521.vinylos.viewmodel.MusicViewModel
import com.sunn3521.vinylos.viewmodel.MusicViewModelFactory

class AodActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        MusicViewModelFactory(MusicRepository(this), this)
    }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.ACTION_CLOSE_AOD) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.bindService(this)

        // Show over lock screen
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

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Dim screen for AOD feel
        window.attributes = window.attributes.apply {
            screenBrightness = 0.05f
        }

        // Make immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val filter = IntentFilter(MusicService.ACTION_CLOSE_AOD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        setContent {
            AodScreen(viewModel = viewModel, onExit = { exitAod() })
        }
    }

    private fun exitAod() {
        // Request dismissal of keyguard to show the lockscreen (or the app/homescreen if unlocked)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(this, null)
        }
        finish()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {}
        viewModel.unbindService(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
