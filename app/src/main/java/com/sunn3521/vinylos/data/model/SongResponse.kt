package com.sunn3521.vinylos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SongResponse(val songs: List<Song>)
