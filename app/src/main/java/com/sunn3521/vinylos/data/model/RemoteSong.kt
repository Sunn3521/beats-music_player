package com.sunn3521.vinylos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteSong(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String?,
    val downloadUrl: String
)
