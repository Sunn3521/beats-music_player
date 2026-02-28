package com.sunn3521.vinylos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sunn3521.vinylos.data.model.DownloadState
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.data.repository.MusicRepository
import com.sunn3521.vinylos.player.MusicService

@Composable
fun SongItem(
    song: Song,
    index: Int,
    fullList: List<Song>,
    repository: MusicRepository,
    musicService: MusicService?,
    externalDownloadState: DownloadState? = null,
    onDelete: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable {
                musicService?.setPlaylist(fullList, index)
                musicService?.resume()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White)
                Text(song.artist, color = Color.Gray)
            }

            if (externalDownloadState is DownloadState.Downloading) {
                CircularProgressIndicator(
                    progress = { externalDownloadState.progress },
                    modifier = Modifier.size(24.dp),
                    color = Color.Red
                )
            } else {
                IconButton(onClick = {
                    musicService?.setPlaylist(fullList, index)
                    musicService?.resume()
                }) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
            }

            if (onDelete != null) {
                IconButton(onClick = { onDelete() }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                }
            }
        }
    }
}
