package com.velora.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.velora.app.model.LyricLine
import com.velora.app.model.MediaItem
import com.velora.app.service.PlayerService
import com.velora.app.util.LyricsParser
import com.velora.app.util.MediaRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class PlayerState(
    val mediaList: List<MediaItem> = emptyList(),
    val currentItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val skipSeconds: Int = 10,
    val lyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val isLoading: Boolean = true,
    val waveformAmplitudes: List<Float> = List(64) { 0f },
    val filterTab: FilterTab = FilterTab.ALL,
    // ZIP import result
    val zipImportMessage: String? = null,
    val zipImportedItems: List<MediaItem> = emptyList()
)

enum class FilterTab { ALL, AUDIO, VIDEO }

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var waveJob: Job? = null

    init {
        loadMedia()
        connectToService()
    }

    // ── Media loading ─────────────────────────────────────────────────────────

    private fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = MediaRepository.loadAllMedia(getApplication())
            _state.update { it.copy(mediaList = items, isLoading = false) }
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            // Always track position — even when paused, so scrubber stays current
            startPositionTracking()
            if (!isPlaying) {
                // Keep waveform bars at rest amplitude when paused
                _state.update { it.copy(waveformAmplitudes = List(64) { 0.05f }) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration ?: 0L
            _state.update { it.copy(durationMs = if (dur < 0) 0L else dur) }
        }
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun playItem(item: MediaItem) {
        val ctrl = controller ?: return

        // Build ExoMediaItem with metadata so lock screen shows title/artist/art
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setArtist(item.artist)
            .setAlbumTitle(item.album)
            .setArtworkUri(item.artUri)
            .build()

        val exoItem = ExoMediaItem.Builder()
            .setUri(item.uri)
            .setMediaMetadata(metadata)
            .build()

        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()

        val lyrics = item.lyricsPath?.let { LyricsParser.parse(File(it)) } ?: emptyList()
        _state.update { it.copy(currentItem = item, lyrics = lyrics, activeLyricIndex = -1) }

        startPositionTracking()
        if (item.isAudio) startWaveformSimulation()
    }

    fun playUri(uri: Uri, mimeType: String) {
        val ctrl = controller ?: return
        val name = uri.lastPathSegment ?: "Media"

        val metadata = MediaMetadata.Builder().setTitle(name).build()
        val exoItem = ExoMediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()

        val synthetic = MediaItem(
            id = -1L, uri = uri,
            title = name,
            mimeType = mimeType
        )
        _state.update { it.copy(currentItem = synthetic, lyrics = emptyList()) }
        startPositionTracking()
        if (!mimeType.startsWith("video")) startWaveformSimulation()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    // Works whether playing or paused
    fun seekForward() {
        val ctrl = controller ?: return
        val secs = _state.value.skipSeconds.toLong() * 1000
        val newPos = (ctrl.currentPosition + secs).coerceAtMost(
            ctrl.duration.takeIf { it >= 0 } ?: Long.MAX_VALUE
        )
        ctrl.seekTo(newPos)
        _state.update { it.copy(positionMs = newPos) }
    }

    fun seekBackward() {
        val ctrl = controller ?: return
        val secs = _state.value.skipSeconds.toLong() * 1000
        val newPos = (ctrl.currentPosition - secs).coerceAtLeast(0L)
        ctrl.seekTo(newPos)
        _state.update { it.copy(positionMs = newPos) }
    }

    fun seekTo(posMs: Long) {
        controller?.seekTo(posMs)
        _state.update { it.copy(positionMs = posMs) }
    }

    fun setSkipSeconds(secs: Int) {
        _state.update { it.copy(skipSeconds = secs) }
    }

    fun setFilter(tab: FilterTab) {
        _state.update { it.copy(filterTab = tab) }
    }

    // ── Lyrics import ─────────────────────────────────────────────────────────

    /** Called when user picks an .lrc or .srt file from the system picker */
    fun importLyricsFile(uri: Uri) {
        val item = _state.value.currentItem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                // Copy into app cache so we have a stable path
                val ext = ctx.contentResolver.getType(uri)
                    ?.substringAfterLast('/') ?: "lrc"
                val dest = File(ctx.cacheDir, "imported_lyrics.$ext")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                val lyrics = LyricsParser.parse(dest)
                val updatedItem = item.copy(lyricsPath = dest.absolutePath)
                _state.update {
                    it.copy(
                        currentItem = updatedItem,
                        lyrics = lyrics,
                        activeLyricIndex = -1
                    )
                }
            } catch (_: Exception) { }
        }
    }

    // ── ZIP import ────────────────────────────────────────────────────────────

    /** Import a ZIP containing MP3 files. Extracts to cache and adds to playlist. */
    fun importZip(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val outDir = File(ctx.cacheDir, "zip_import").also { it.mkdirs() }
                val importedItems = mutableListOf<MediaItem>()
                var count = 0

                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = File(entry.name).name
                            val ext = name.substringAfterLast('.', "").lowercase()
                            if (!entry.isDirectory && ext in setOf("mp3","flac","ogg","m4a","aac","wav")) {
                                val outFile = File(outDir, name)
                                FileOutputStream(outFile).use { zip.copyTo(it) }
                                val lyricsFile = LyricsParser.findLyricsForMedia(outFile.absolutePath)
                                importedItems.add(
                                    MediaItem(
                                        id = outFile.hashCode().toLong(),
                                        uri = Uri.fromFile(outFile),
                                        title = name.substringBeforeLast('.'),
                                        mimeType = "audio/$ext",
                                        lyricsPath = lyricsFile?.absolutePath
                                    )
                                )
                                count++
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                val msg = if (count > 0) "Imported $count audio file${if (count > 1) "s" else ""} from ZIP"
                          else "No audio files found in ZIP"

                _state.update { s ->
                    s.copy(
                        mediaList = s.mediaList + importedItems,
                        zipImportedItems = importedItems,
                        zipImportMessage = msg
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(zipImportMessage = "Failed to read ZIP: ${e.message}") }
            }
        }
    }

    fun clearZipMessage() = _state.update { it.copy(zipImportMessage = null) }

    // ── Position tracking ─────────────────────────────────────────────────────

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val pos = controller?.currentPosition ?: 0L
                val dur = controller?.duration?.let { if (it < 0) 0L else it } ?: 0L
                val lyrics = _state.value.lyrics
                val activeIdx = if (lyrics.isNotEmpty()) LyricsParser.activeIndex(lyrics, pos) else -1
                _state.update { it.copy(positionMs = pos, durationMs = dur, activeLyricIndex = activeIdx) }
                delay(200)
            }
        }
    }

    private fun startWaveformSimulation() {
        waveJob?.cancel()
        waveJob = viewModelScope.launch {
            var phase = 0f
            while (isActive) {
                if (_state.value.isPlaying) {
                    phase += 0.15f
                    val bars = List(64) { i ->
                        val base = Math.sin((i * 0.3 + phase).toDouble()).toFloat()
                        val noise = (Math.random() * 0.4 - 0.2).toFloat()
                        ((base + noise + 1f) / 2f).coerceIn(0.05f, 1f)
                    }
                    _state.update { it.copy(waveformAmplitudes = bars) }
                }
                delay(80)
            }
        }
    }

    fun filteredList(): List<MediaItem> {
        val all = _state.value.mediaList
        return when (_state.value.filterTab) {
            FilterTab.ALL   -> all
            FilterTab.AUDIO -> all.filter { it.isAudio }
            FilterTab.VIDEO -> all.filter { it.isVideo }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        waveJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
