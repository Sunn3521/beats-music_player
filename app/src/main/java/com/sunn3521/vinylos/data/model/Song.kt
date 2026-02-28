package com.sunn3521.vinylos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String? = null,
    val title: String,
    val artist: String,
    val file: String,
    val albumArt: String? = null,
    val downloadUrl: String? = null
)
