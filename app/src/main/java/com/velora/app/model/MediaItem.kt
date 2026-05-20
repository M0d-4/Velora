package com.velora.app.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val duration: Long = 0L,          // ms
    val mimeType: String = "",
    val thumbnailUri: Uri? = null,
    val lyricsPath: String? = null    // path to .lrc file if found
) {
    val isVideo: Boolean get() = mimeType.startsWith("video")
    val isAudio: Boolean get() = mimeType.startsWith("audio")
}

/** Parsed LRC lyric line */
data class LyricLine(
    val timeMs: Long,
    val text: String
)
