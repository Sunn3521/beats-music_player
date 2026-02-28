package com.sunn3521.vinylos.data.model

sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    object Completed : DownloadState()
    object NotDownloaded : DownloadState()
    object NotStarted : DownloadState() // Added to support previous logic if needed
}
