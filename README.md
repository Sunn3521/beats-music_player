# 🎵 Beats — Music Player

A modern Android music player built with **Kotlin** and **Jetpack Compose**, featuring seamless playback of both local device audio and remote songs streamed from a GitHub API. Beats offers an intuitive Material 3 UI with advanced visualizations and playback controls.

## ✨ Features

### Playback & Control
- **Play, Pause, Skip** — Full playback control with previous/next navigation
- **Shuffle Mode** — Randomize playlist order
- **Repeat Modes** — Repeat all songs or repeat single song
- **Sleep Timer** — Auto-pause playback with optional finish-current-song behavior
- **Volume Control** — Adjust audio levels with system integration

### Audio Management
- **Local Playback** — Stream audio files from device storage
- **Remote Streaming** — Download and play songs from GitHub API
- **Download Manager** — Batch download remote songs with progress tracking
- **Album Art Display** — Show album artwork from device metadata
- **Audio Visualization** — Real-time FFT and waveform visualizations

### UI & UX
- **Material 3 Design** — Modern dark theme with red accent colors
- **Three-Tab Navigation**:
  - **Git Tab** — Browse and download remote songs
  - **Device Tab** — Browse local device audio library
  - **Playlist Tab** — View and manage active playback queue
- **Mini Player** — Quick access playback controls with aurora progress bar
- **Full Screen Player** — Immersive playback view with visualizer settings
- **Lock Screen Visualizer** — Always-on display with audio-reactive visuals
- **Dynamic Wallpaper** — Aurora-animated progress bar synchronized to music

### System Integration
- **Foreground Service** — Continuous playback with media notifications
- **Media Notifications** — System notifications with playback controls
- **Always-On Display (AOD)** — Lock screen music visualizer
- **Runtime Permissions** — Handle audio and storage access (API 24–35)
- **MediaStore Integration** — Discover device audio files

## 🏗️ Architecture

```
com.sunn3521.vinylos/
├── ui/                          # Jetpack Compose UI layer
│   ├── MainActivity.kt           # Main app activity
│   ├── FullScreenPlayer.kt       # Full player screen
│   ├── LockVisualizerMode.kt     # Lock screen visualizer
│   ├── LockVisualizerActivity.kt # Lock screen activity
│   ├── SongItem.kt               # Song list item component
│   ├── VisualizerSettingsScreen.kt # Visualizer customization
│   ├── AodScreen.kt              # Always-on display screen
│   ├── FFTVisualizer.kt          # FFT visualization component
│   ├── WaveformVisualizer.kt     # Waveform visualization
│   └── SleepTimerDialog.kt       # Sleep timer dialog
├── player/                       # Audio playback layer
│   ├── MusicService.kt           # Background music service
│   ├── AudioManager.kt           # Audio session management
│   └── Song.kt                   # Song data model
├── data/                         # Data & repository layer
│   ├── VisualizerPrefs.kt        # Visualizer settings storage
│   ├── model/                    # Data models
│   │   ├── Song.kt               # Song model
│   │   ├── RemoteSong.kt         # Remote song model
│   │   ├── DownloadState.kt      # Download state enum
│   │   └── SongResponse.kt       # API response model
│   └── repository/               # Data access layer
│       └── MusicRepository.kt    # API & local data management
├── viewmodel/                    # MVVM ViewModel layer
│   └── MusicViewModel.kt         # Shared state management
└── aod/                          # Always-on display
    └── AodActivity.kt            # AOD activity
```

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM with ViewModel
- **Audio Playback**: ExoPlayer (Media3)
- **Networking**: OkHttp + Kotlin Serialization
- **Image Loading**: Coil Compose
- **Color Extraction**: Androidx Palette
- **Local Storage**: DataStore Preferences
- **Audio Visualization**: Android Visualizer API
- **Minimum API**: 24 (Android 7.0)
- **Target API**: 35 (Android 15)

## 📋 Requirements

- Android 7.0 (API 24) or higher
- Kotlin 1.9+
- Gradle 8.0+

### Permissions Required
- `READ_MEDIA_AUDIO` — Access device audio files
- `RECORD_AUDIO` — Capture audio for visualization
- `MODIFY_AUDIO_SETTINGS` — Control volume
- `INTERNET` — Download remote songs
- `POST_NOTIFICATIONS` — Media playback notifications
- `FOREGROUND_SERVICE` — Background playback

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/Sunn3521/beats-music_player.git
cd beats-music_player
```

### 2. Open in Android Studio
- Open the project in Android Studio Koala or later
- Let Gradle sync dependencies

### 3. Configure API Settings
Update `Constants.BASE_URL` and `MUSIC_FOLDER` in your constants file to point to your remote song API:
```kotlin
const val BASE_URL = "https://your-api-endpoint.com/"
const val MUSIC_FOLDER = "music"  // Optional folder path
```

### 4. Build & Run
```bash
# Build the app
./gradlew build

# Run on emulator/device
./gradlew installDebug
```

## 📦 Dependencies

Key libraries used in this project:

| Library | Version | Purpose |
|---------|---------|---------|
| ExoPlayer (Media3) | 1.3.1 | Audio playback engine |
| Jetpack Compose | Latest | UI framework |
| Coil | 2.6.0 | Image loading |
| OkHttp | 4.12.0 | Networking |
| Kotlin Serialization | 1.6.0 | JSON parsing |
| DataStore | Latest | Preferences storage |
| Androidx Palette | 1.0.0 | Color extraction |

## 🎮 Usage

### Playing Local Songs
1. Navigate to the **Device** tab
2. Tap a song to start playback
3. Use controls to play/pause/skip

### Playing Remote Songs
1. Navigate to the **Git** tab
2. Tap "Download All" to batch download songs
3. Or tap individual songs to download and play

### Managing Playlist
1. Navigate to the **Playlist** tab
2. View the current queue
3. Tap a song to jump to it
4. Use mini player controls for quick actions

### Customizing Visualizer
1. Tap the **V** button in the top-right
2. Customize visualizer colors, styles, and effects
3. Tap "Activate Lock Mode" for lock screen visualizer

### Sleep Timer
1. Tap the mini player to expand full screen
2. Tap the timer icon
3. Set duration and completion behavior
4. Timer will auto-pause music when time expires

## 📱 Screenshots

*(Add screenshots of your app here)*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👤 Author

**Sunn3521** - [GitHub Profile](https://github.com/Sunn3521)

## 🙏 Acknowledgments

- ExoPlayer team for the robust audio playback library
- Jetpack Compose team for the modern UI framework
- Material Design team for Material 3 guidelines
