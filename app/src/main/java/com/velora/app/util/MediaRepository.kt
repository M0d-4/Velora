package com.velora.app.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.velora.app.model.MediaItem

object MediaRepository {

    fun loadAllMedia(context: Context): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items += loadAudio(context)
        items += loadVideo(context)
        return items.sortedBy { it.title.lowercase() }
    }

    fun loadAudio(context: Context): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val albumId  = cursor.getLong(albumIdCol)
                val uri      = ContentUris.withAppendedId(collection, id)
                val dataPath = cursor.getString(dataCol)
                val lyricsFile = LyricsParser.findLyricsForMedia(dataPath)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId)
                val rawArtist = cursor.getString(artistCol) ?: ""
                items.add(MediaItem(
                    id          = id,
                    uri         = uri,
                    title       = cursor.getString(titleCol) ?: "Unknown",
                    artist      = if (rawArtist.isBlank() || rawArtist == "<unknown>") "Unknown" else rawArtist,
                    album       = cursor.getString(albumCol) ?: "",
                    duration    = cursor.getLong(durCol),
                    mimeType    = cursor.getString(mimeCol) ?: "audio/*",
                    albumArtUri = albumArtUri,
                    lyricsPath  = lyricsFile?.absolutePath
                ))
            }
        }
        return items
    }

    fun loadVideo(context: Context): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA
        )

        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dataCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val uri      = ContentUris.withAppendedId(collection, id)
                val dataPath = cursor.getString(dataCol)
                val lyricsFile = LyricsParser.findLyricsForMedia(dataPath)
                val thumbUri = Uri.parse("content://media/external/video/media/$id/thumbnail")
                items.add(MediaItem(
                    id           = id,
                    uri          = uri,
                    title        = cursor.getString(titleCol) ?: "Unknown",
                    duration     = cursor.getLong(durCol),
                    mimeType     = cursor.getString(mimeCol) ?: "video/*",
                    thumbnailUri = thumbUri,
                    lyricsPath   = lyricsFile?.absolutePath
                ))
            }
        }
        return items
    }

    /** "2:34" → shows as "2:34" • also returns a human unit suffix */
    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Returns "hrs", "mins", or "secs" label for display beside the duration */
    fun durationUnit(ms: Long): String {
        val totalSec = ms / 1000
        return when {
            totalSec >= 3600 -> "hrs"
            totalSec >= 60   -> "mins"
            else             -> "secs"
        }
    }
}
