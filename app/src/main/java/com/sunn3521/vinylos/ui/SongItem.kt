package com.sunn3521.vinylos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sunn3521.vinylos.data.model.DownloadState
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.data.repository.MusicRepository
import com.sunn3521.vinylos.player.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SongItem(
    song: Song,
    fullList: List<Song>,
    repository: MusicRepository,
    musicService: MusicService?,
    externalDownloadState: DownloadState? = null,
    onDelete: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val isCurrentSong = musicService?.currentSong?.file == song.file
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrentSong) Color.Red.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(300),
        label = "cardBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentSong) Color.Red.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(300),
        label = "cardBorder"
    )

    // Performance: Properly keyed state
    var internalDownloadState by remember(song.file) { mutableStateOf<DownloadState>(DownloadState.NotStarted) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "itemScale"
    )

    val effectiveDownloadState = remember(externalDownloadState, internalDownloadState) {
        externalDownloadState ?: internalDownloadState
    }
    
    val localFile = remember(song.file) { File(song.file) }
    val isDeviceSong = remember(localFile) { localFile.isAbsolute && localFile.exists() }
    val isDownloaded = remember(song.file, effectiveDownloadState) { repository.isSongDownloaded(song) }

    val imageRequest = remember(song.albumArt, context) {
        if (song.albumArt != null) {
            ImageRequest.Builder(context)
                .data(song.albumArt)
                .crossfade(250)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (effectiveDownloadState is DownloadState.NotStarted) {
                        downloadJob = scope.launch {
                            if (isDeviceSong) {
                                musicService?.startSmartPlaylist(song, fullList)
                            } else if (repository.isSongDownloaded(song)) {
                                val file = repository.getLocalFile(song)
                                val updatedSong = song.copy(file = file.absolutePath)
                                musicService?.startSmartPlaylist(updatedSong, fullList.map { 
                                    if (it.file == song.file) updatedSong else it 
                                })
                            } else {
                                try {
                                    val file = repository.getOrDownloadSong(song) { progress ->
                                        internalDownloadState = DownloadState.Downloading(progress)
                                    }
                                    internalDownloadState = DownloadState.Completed
                                    val updatedSong = song.copy(file = file.absolutePath)
                                    musicService?.startSmartPlaylist(updatedSong, fullList.map { 
                                        if (it.file == song.file) updatedSong else it 
                                    })
                                } catch (_: Exception) {
                                    internalDownloadState = DownloadState.NotStarted
                                }
                            }
                            onRefresh()
                        }
                    } else if (isDownloaded || isDeviceSong) {
                        scope.launch {
                            val path = if (isDeviceSong) song.file else repository.getLocalFile(song).absolutePath
                            val updatedSong = song.copy(file = path)
                            musicService?.startSmartPlaylist(updatedSong, fullList.map { 
                                if (it.file == song.file) updatedSong else it 
                                })
                        }
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Gray)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val isDownloading = effectiveDownloadState is DownloadState.Downloading
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val progressLambda = remember(effectiveDownloadState) {
                        { (effectiveDownloadState as? DownloadState.Downloading)?.progress?.div(100f) ?: 0f }
                    }
                    Column {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progressLambda,
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            color = Color.Red,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                AnimatedVisibility(visible = isDownloaded && !isDeviceSong && effectiveDownloadState !is DownloadState.Downloading) {
                    Text(
                        text = "Downloaded ✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (effectiveDownloadState is DownloadState.Downloading) {
                IconButton(onClick = {
                    downloadJob?.cancel()
                    internalDownloadState = DownloadState.NotStarted
                    onRefresh()
                }) {
                    Icon(Icons.Default.Close, "Cancel", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else if (!isDeviceSong && (effectiveDownloadState == DownloadState.Completed || isDownloaded)) {
                IconButton(onClick = {
                    scope.launch {
                        internalDownloadState = DownloadState.NotStarted
                        repository.deleteSong(song)
                        onDelete()
                        onRefresh()
                    }
                }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
