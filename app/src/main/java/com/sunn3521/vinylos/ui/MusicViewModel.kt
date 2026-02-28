package com.sunn3521.vinylos.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunn3521.vinylos.data.VisualizerPrefs
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.data.repository.MusicRepository
import com.sunn3521.vinylos.player.MusicService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(val repository: MusicRepository, context: Context) : ViewModel() {

    private val visualizerPrefs = VisualizerPrefs(context)
    val visualizerSettings: StateFlow<VisualizerSettings> = visualizerPrefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisualizerSettings())

    var musicService by mutableStateOf<MusicService?>(null)
        private set

    private val _remoteSongs = MutableStateFlow<List<Song>>(emptyList())
    val remoteSongs: StateFlow<List<Song>> = _remoteSongs

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs

    var isLoading by mutableStateOf(true)
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        try {
            context.unbindService(connection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSongs(deviceSongs: List<Song>) {
        viewModelScope.launch {
            isLoading = true
            _remoteSongs.value = repository.getSongs()
            _localSongs.value = deviceSongs
            isLoading = false
        }
    }

    fun updateVisualizerSettings(settings: VisualizerSettings) {
        viewModelScope.launch {
            visualizerPrefs.updateSettings(settings)
        }
    }
}

class MusicViewModelFactory(private val repository: MusicRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
