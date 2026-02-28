package com.sunn3521.vinylos.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunn3521.vinylos.player.MusicService
import com.sunn3521.vinylos.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AodScreen(
    viewModel: MusicViewModel,
    onExit: () -> Unit
) {
    val musicService = viewModel.musicService
    val settings by viewModel.visualizerSettings.collectAsState()
    
    val time by produceState(initialValue = "") {
        while (true) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            value = formatter.format(Date())
            delay(1000)
        }
    }

    val currentSong = musicService?.currentSong

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.isAmoled) Color.Black else Color(0xFF121212))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onExit() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (musicService != null) {
            LockVisualizer(
                fftData = musicService.fftMagnitudes,
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Clock
            if (settings.showClock) {
                Column(
                    modifier = Modifier.padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = time,
                        fontSize = settings.clockSize.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Thin
                    )
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }

            // Bottom section: Song Title
            if (settings.showSongTitle && currentSong != null) {
                Column(
                    modifier = Modifier.padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong.title,
                        fontSize = settings.titleSize.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong.artist,
                        fontSize = (settings.titleSize * 0.7f).sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}
