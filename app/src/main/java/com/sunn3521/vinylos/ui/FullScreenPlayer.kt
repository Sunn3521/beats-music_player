package com.sunn3521.vinylos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.player.MusicService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    musicService: MusicService,
    visualizerSettings: VisualizerSettings,
    onBack: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onShowSleepTimer: () -> Unit
) {
    val song = musicService.currentSong ?: return
    
    // We can reuse parts of the dynamic background logic here if needed
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // 🔹 Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                Row {
                    IconButton(onClick = onOpenVisualizer) {
                        Text("V", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                    IconButton(onClick = onShowSleepTimer) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // 🔹 Album Art
            AsyncImage(
                model = song.albumArt,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(32.dp))

            // 🔹 Title
            Text(
                text = song.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // 🔹 Artist
            Text(
                text = song.artist,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // 🔹 Seekbar
            if (musicService.duration > 0) {
                Slider(
                    value = musicService.currentPosition.toFloat(),
                    onValueChange = { musicService.seekTo(it.toLong()) },
                    valueRange = 0f..musicService.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent, // Managed by thumb composable
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(musicService.currentPosition), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(formatTime(musicService.duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // 🔹 Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                IconButton(onClick = { musicService.previous() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(
                    onClick = { if (musicService.isPlaying) musicService.pause() else musicService.resume() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        if (musicService.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { musicService.next() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Optional: You can add the mini visualizer here as requested
            FFTVisualizer(
                magnitudes = musicService.fftMagnitudes,
                color = visualizerSettings.color.copy(alpha = 0.5f),
                modifier = Modifier.height(40.dp).fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}
