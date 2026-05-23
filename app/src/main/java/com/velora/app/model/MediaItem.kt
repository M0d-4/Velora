package com.velora.app.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val duration: Long = 0L,
    val mimeType: String = "",
    val thumbnailUri: Uri? = null,
    val albumArtUri: Uri? = null,
    val lyricsPath: String? = null
) {
    val isVideo: Boolean get() = mimeType.startsWith("video")
    val isAudio: Boolean get() = mimeType.startsWith("audio")
    val artUri: Uri? get() = albumArtUri ?: thumbnailUri
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Playlist(
    val id: Long,
    val name: String,
    val itemIds: List<Long> = emptyList(),
    val isFavourites: Boolean = false
)
