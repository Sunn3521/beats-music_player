@file:OptIn(ExperimentalMaterial3Api::class)
package com.sunn3521.vinylos.ui

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sunn3521.vinylos.data.model.DownloadState
import com.sunn3521.vinylos.data.model.RemoteSong
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.data.repository.MusicRepository
import com.sunn3521.vinylos.player.MusicService
import com.sunn3521.vinylos.ui.components.SleepTimerDialog
import com.sunn3521.vinylos.viewmodel.MusicViewModel
import com.sunn3521.vinylos.viewmodel.MusicViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var musicViewModel: MusicViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val repository = MusicRepository(this)
        requestPermission()

        setContent {
            val viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory(repository, this))
            musicViewModel = viewModel
            
            LaunchedEffect(Unit) {
                viewModel.bindService(this@MainActivity)
            }

            VinylOSApp(viewModel, ::getDeviceSongs)
        }
    }

    private fun requestPermission() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            requestPermissions(toRequest.toTypedArray(), 100)
        }
    }

    private fun getDeviceSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = contentResolver.query(collection, projection, selection, null, null)

        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            val sArtworkUri = "content://media/external/audio/albumart".toUri()

            while (it.moveToNext()) {
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val path = it.getString(dataColumn)
                val albumId = it.getLong(albumIdColumn)
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                if (artist != null && artist != "<unknown>" && artist != "Unknown") {
                    songs.add(Song(title = title, artist = artist, file = path, albumArt = albumArtUri))
                }
            }
        }
        return songs
    }

    override fun onDestroy() {
        super.onDestroy()
        musicViewModel?.unbindService(this)
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuroraProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val auroraBrush = Brush.linearGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.7f),
            Color.White.copy(alpha = 0.5f),
            color.copy(alpha = 0.7f),
            color
        ),
        start = androidx.compose.ui.geometry.Offset(offset, 0f),
        end = androidx.compose.ui.geometry.Offset(offset + 300f, 300f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
        )
        
        // Progress and Thumb
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(auroraBrush, RoundedCornerShape(2.dp))
            )
            
            // White circle with red circle in center
            Box(
                modifier = Modifier
                    .offset(x = 6.dp)
                    .size(12.dp)
                    .background(Color.White, CircleShape)
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}

@Composable
fun VinylOSApp(viewModel: MusicViewModel, onScanDevice: () -> List<Song>) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val musicService = viewModel.musicService

    val remoteSongs by viewModel.remoteSongs.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val downloadStates = remember { mutableStateMapOf<String, DownloadState>() }
    var downloadJob: Job? by remember { mutableStateOf(null) }
    var isDownloadingAll by remember { mutableStateOf(false) }

    var isLockMode by remember { mutableStateOf(false) }
    var isShowingVisualizerSettings by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showSleepTimerPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visualizerSettings by viewModel.visualizerSettings.collectAsState()

    LaunchedEffect(isLockMode) {
        val activity = context.findActivity()
        activity?.window?.let { window ->
            if (isLockMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.hide(WindowInsets.Type.systemBars())
                    window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSongs(onScanDevice())
    }

    BackHandler(showSheet || isShowingVisualizerSettings) {
        if (showSheet) {
            showSheet = false
        } else if (isShowingVisualizerSettings) {
            isShowingVisualizerSettings = false
        }
    }

    if (isShowingVisualizerSettings) {
        VisualizerSettingsScreen(
            settings = visualizerSettings,
            onSettingsChange = { viewModel.updateVisualizerSettings(it) },
            onBack = { isShowingVisualizerSettings = false },
            onActivateLockMode = {
                isShowingVisualizerSettings = false
                isLockMode = true
            }
        )
    } else if (isLockMode && musicService != null) {
        LockModeScreen(
            musicService = musicService, 
            settings = visualizerSettings,
            onSettingsChange = { viewModel.updateVisualizerSettings(it) },
            onExit = { isLockMode = false }
        )
    } else {
        Scaffold(
            containerColor = Color(0xFF121212),
            topBar = {
                TopAppBar(
                    title = { Text("Beats!", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF121212),
                        titleContentColor = Color.White
                    ),
                    actions = {
                        Box(
                            modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .clickable { isShowingVisualizerSettings = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        }
                        IconButton(onClick = {
                            val remoteAsSongs = remoteSongs.map { it.toSong() }
                            val combined = (remoteAsSongs.filter { viewModel.repository.isSongDownloaded(it) } + localSongs).shuffled()
                            if (combined.isNotEmpty()) {
                                musicService?.setPlaylist(combined, 0)
                                if (selectedTab != 2) selectedTab = 2
                            }
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Random Playlist", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->

            Box(Modifier.fillMaxSize().padding(padding).background(Color(0xFF121212))) {
                Column(Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF121212),
                        contentColor = Color.Red,
                        divider = {}
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Git", color = if(selectedTab == 0) Color.Red else Color.Gray) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Device", color = if(selectedTab == 1) Color.Red else Color.Gray) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Playlist", color = if(selectedTab == 2) Color.Red else Color.Gray) })
                    }

                    if (viewModel.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Red)
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> { // Git Songs
                                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        Button(
                                            onClick = {
                                                downloadJob = scope.launch {
                                                    isDownloadingAll = true
                                                    try {
                                                        remoteSongs.forEach { remote ->
                                                            val song = remote.toSong()
                                                            if (isActive && !viewModel.repository.isSongDownloaded(song)) {
                                                                viewModel.repository.getOrDownloadSong(song) { progress ->
                                                                    downloadStates[song.file] = DownloadState.Downloading(progress)
                                                                }
                                                                downloadStates[song.file] = DownloadState.Completed
                                                            }
                                                        }
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        isDownloadingAll = false
                                                        viewModel.loadSongs(onScanDevice())
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isDownloadingAll,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            if (isDownloadingAll) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Downloading...")
                                            } else {
                                                Icon(Icons.Default.Download, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Download All")
                                            }
                                        }
                                        
                                        if (isDownloadingAll) {
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    downloadJob?.cancel()
                                                    isDownloadingAll = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Cancel")
                                            }
                                        }
                                    }

                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        itemsIndexed(remoteSongs) { _, remote ->
                                            val song = remote.toSong()
                                            SongItem(
                                                song = song,
                                                fullList = remoteSongs.map { it.toSong() },
                                                repository = viewModel.repository,
                                                musicService = musicService,
                                                externalDownloadState = downloadStates[song.file],
                                                onDelete = {
                                                    downloadStates.remove(song.file)
                                                    viewModel.loadSongs(onScanDevice())
                                                },
                                                onRefresh = { viewModel.loadSongs(onScanDevice()) }
                                            )
                                        }
                                    }
                                }
                                1 -> { // Device Songs
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        itemsIndexed(localSongs) { _, song ->
                                            SongItem(
                                                song = song,
                                                fullList = localSongs,
                                                repository = viewModel.repository,
                                                musicService = musicService,
                                                onRefresh = { viewModel.loadSongs(onScanDevice()) }
                                            )
                                        }
                                    }
                                }
                                2 -> { // Playlist
                                    val currentPlaylist = musicService?.songList ?: emptyList()
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        itemsIndexed(currentPlaylist) { index, song ->
                                            val isCurrent = musicService?.currentSong?.file == song.file
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable {
                                                    musicService?.setPlaylist(currentPlaylist, index)
                                                }.border(0.5.dp, if(isCurrent) Color.White else Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color.Red.copy(alpha = 0.8f) else Color(0xFF1E1E1E))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Card(
                                                        modifier = Modifier.size(40.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        if (song.albumArt != null) {
                                                            AsyncImage(
                                                                model = song.albumArt,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Icon(Icons.Default.Album, null, modifier = Modifier.fillMaxSize().padding(4.dp), tint = Color.Gray)
                                                        }
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(song.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                                        Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mini Player
                    val playingSong = musicService?.currentSong
                    if (playingSong != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { showSheet = true },
                                            onDoubleTap = { showSheet = true }
                                        )
                                    },
                                color = Color.Black.copy(alpha = 0.75f),
                                shadowElevation = 10.dp
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 12.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Card(
                                            modifier = Modifier.size(48.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                                        ) {
                                            if (playingSong.albumArt != null) {
                                                AsyncImage(
                                                    model = playingSong.albumArt,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Image(
                                                    imageVector = Icons.Default.Album,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = ColorFilter.tint(Color.Gray)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(playingSong.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(playingSong.artist, color = Color(0xFFB3B3B3), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        IconButton(onClick = { musicService.previous() }) {
                                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
                                        }

                                        IconButton(onClick = { if (musicService.isPlaying) musicService.pause() else musicService.resume() }) {
                                            Icon(if (musicService.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                                        }

                                        IconButton(onClick = { musicService.next() }) {
                                            Icon(Icons.Default.SkipNext, null, tint = Color.White)
                                        }
                                    }

                                    if (musicService.duration > 0) {
                                        val progress = musicService.currentPosition.toFloat() / musicService.duration.toFloat()
                                        AuroraProgressBar(
                                            progress = progress,
                                            color = visualizerSettings.color,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(16.dp)
                                                .align(Alignment.BottomStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSheet && musicService?.currentSong != null) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.Transparent,
                    dragHandle = null
                ) {
                    FullScreenPlayer(
                        musicService = musicService,
                        visualizerSettings = visualizerSettings,
                        onBack = { showSheet = false },
                        onOpenVisualizer = {
                            isShowingVisualizerSettings = true
                            showSheet = false
                        },
                        onShowSleepTimer = { showSleepTimerPicker = true }
                    )
                }
            }
            
            if (showSleepTimerPicker && musicService != null) {
                SleepTimerDialog(
                    onDismiss = { showSleepTimerPicker = false },
                    onStart = { millis, finishCurrentSong ->
                        musicService.startSleepTimer(millis, finishCurrentSong)
                        showSleepTimerPicker = false
                        Toast.makeText(context, "Timer started", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// Extension to bridge RemoteSong to local Song model
fun RemoteSong.toSong(): Song {
    // We use the downloadUrl's last path segment as the filename for the local Song
    val fileName = downloadUrl.substringAfterLast("/")
    return Song(
        title = title,
        artist = artist,
        file = fileName,
        albumArt = albumArt,
        downloadUrl = downloadUrl
    )
}

fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
