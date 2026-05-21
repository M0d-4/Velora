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
    val albumArtUri: Uri? = null,      // embedded cover art for audio files
    val lyricsPath: String? = null     // path to .lrc/.srt file if found
) {
    val isVideo: Boolean get() = mimeType.startsWith("video")
    val isAudio: Boolean get() = mimeType.startsWith("audio")

    /** Best available art — album art for audio, thumbnail for video */
    val artUri: Uri? get() = albumArtUri ?: thumbnailUri
}

/** Parsed lyric line (works for both LRC and SRT) */
data class LyricLine(
    val timeMs: Long,
    val text: String
)
