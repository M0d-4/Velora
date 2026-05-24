package com.velora.app

import android.app.Application
import android.content.Context
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.velora.app.model.LyricLine
import com.velora.app.model.MediaItem
import com.velora.app.model.Playlist
import com.velora.app.service.PlayerService
import com.velora.app.util.LyricsParser
import com.velora.app.util.MediaRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.sqrt

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
    val zipImportMessage: String? = null,
    val zipImportedItems: List<MediaItem> = emptyList(),
    val playlists: List<Playlist> = listOf(
        Playlist(id = 0L, name = "Favorites", isFavourites = true)
    ),
    val isShuffle: Boolean = false,
    val isQueueMode: Boolean = false,
    val queue: List<MediaItem> = emptyList(),
    val queueIndex: Int = 0,
    val showFavouriteToast: Boolean = false,
    val isLandscape: Boolean = false,
    val playbackSpeed: Float = 1f,
    val showMergePlaylistDialog: Boolean = false,
    // Persisted extra media (zip imports)
    val extraMediaList: List<MediaItem> = emptyList()
)

enum class FilterTab { ALL, AUDIO, VIDEO, PLAYLISTS }

private const val PREFS_NAME = "velora_prefs"
private const val KEY_PLAYLISTS = "playlists"
private const val KEY_EXTRA_MEDIA = "extra_media"

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var toastJob: Job? = null
    private var visualizer: Visualizer? = null

    init {
        loadPersistedData()
        loadMedia()
        connectToService()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun prefs() = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadPersistedData() {
        val p = prefs()

        // Load playlists
        val playlistsJson = p.getString(KEY_PLAYLISTS, null)
        val playlists = if (playlistsJson != null) {
            try {
                val arr = JSONArray(playlistsJson)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val ids = obj.getJSONArray("itemIds")
                    Playlist(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        isFavourites = obj.optBoolean("isFavourites", false),
                        itemIds = (0 until ids.length()).map { ids.getLong(it) }
                    )
                }
            } catch (_: Exception) {
                listOf(Playlist(id = 0L, name = "Favorites", isFavourites = true))
            }
        } else {
            listOf(Playlist(id = 0L, name = "Favorites", isFavourites = true))
        }

        // Load persisted extra media (zip imports)
        val extraJson = p.getString(KEY_EXTRA_MEDIA, null)
        val extraMedia = if (extraJson != null) {
            try {
                val arr = JSONArray(extraJson)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    val filePath = obj.optString("filePath", "")
                    val file = File(filePath)
                    if (file.exists()) {
                        MediaItem(
                            id = obj.getLong("id"),
                            uri = Uri.fromFile(file),
                            title = obj.getString("title"),
                            artist = obj.optString("artist", "Unknown"),
                            album = obj.optString("album", ""),
                            duration = obj.optLong("duration", 0L),
                            mimeType = obj.getString("mimeType"),
                            lyricsPath = obj.optString("lyricsPath").takeIf { it.isNotBlank() }
                        )
                    } else null
                }
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        _state.update { it.copy(playlists = playlists, extraMediaList = extraMedia) }
    }

    private fun persistData() {
        val s = _state.value
        val p = prefs().edit()

        // Save playlists
        val arr = JSONArray()
        s.playlists.forEach { pl ->
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("isFavourites", pl.isFavourites)
            val ids = JSONArray()
            pl.itemIds.forEach { ids.put(it) }
            obj.put("itemIds", ids)
            arr.put(obj)
        }
        p.putString(KEY_PLAYLISTS, arr.toString())

        // Save extra media
        val mediaArr = JSONArray()
        s.extraMediaList.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("filePath", item.uri.path ?: "")
            obj.put("title", item.title)
            obj.put("artist", item.artist)
            obj.put("album", item.album)
            obj.put("duration", item.duration)
            obj.put("mimeType", item.mimeType)
            obj.put("lyricsPath", item.lyricsPath ?: "")
            mediaArr.put(obj)
        }
        p.putString(KEY_EXTRA_MEDIA, mediaArr.toString())
        p.apply()
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
            android.content.ComponentName(getApplication(), PlayerService::class.java)
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
            startPositionTracking()
            if (!isPlaying) {
                releaseVisualizer()
                _state.update { it.copy(waveformAmplitudes = List(64) { 0.05f }) }
            } else {
                startVisualizer()
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration ?: 0L
            _state.update { it.copy(durationMs = if (dur < 0) 0L else dur) }
            if (playbackState == Player.STATE_ENDED) advanceQueue()
        }
    }

    // ── Audio Visualizer (real waveform) ──────────────────────────────────────

    private fun startVisualizer() {
        if (_state.value.currentItem?.isVideo == true) return
        releaseVisualizer()
        try {
            // Use session ID 0 = global audio mix output.
            // MediaController does not expose audioSessionId; 0 captures the final mix.
            val vis = Visualizer(0)
            vis.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    val barCount = 64
                    val samplesPerBar = waveform.size / barCount
                    val bars = (0 until barCount).map { b ->
                        val start = b * samplesPerBar
                        val end = (start + samplesPerBar).coerceAtMost(waveform.size)
                        val rms = sqrt(waveform.slice(start until end)
                            .map { s -> val v = (s.toInt() and 0xFF) - 128; (v * v).toDouble() }
                            .average()).toFloat()
                        (rms / 80f).coerceIn(0.04f, 1f)
                    }
                    _state.update { it.copy(waveformAmplitudes = bars) }
                }
                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            vis.enabled = true
            visualizer = vis
        } catch (_: Exception) {
            // Fallback: energy-based simulation from position ticks
            startFallbackWave()
        }
    }

    private fun startFallbackWave() {
        // No random phase walk — amplitude pulses up/down based on position
        viewModelScope.launch {
            var lastPos = 0L
            var energy = 0f
            while (isActive && _state.value.isPlaying) {
                val pos = _state.value.positionMs
                val delta = abs(pos - lastPos).toFloat()
                lastPos = pos
                energy = (energy * 0.7f + (delta / 200f).coerceIn(0f, 1f) * 0.3f).coerceIn(0.04f, 0.9f)
                val bars = List(64) { i ->
                    val envelope = energy * (0.5f + 0.5f * kotlin.math.sin(i * 0.25f))
                    envelope.coerceIn(0.04f, 1f)
                }
                _state.update { it.copy(waveformAmplitudes = bars) }
                delay(50)
            }
        }
    }

    private fun releaseVisualizer() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        visualizer = null
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun playItem(item: MediaItem) {
        val ctrl = controller ?: return
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title).setArtist(item.artist)
            .setAlbumTitle(item.album).setArtworkUri(item.artUri).build()
        val exoItem = ExoMediaItem.Builder().setUri(item.uri).setMediaMetadata(metadata).build()
        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()
        ctrl.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed))
        val lyrics = item.lyricsPath?.let { LyricsParser.parse(File(it)) } ?: emptyList()
        _state.update { it.copy(currentItem = item, lyrics = lyrics, activeLyricIndex = -1) }
        startPositionTracking()
    }

    fun playUri(uri: Uri, mimeType: String) {
        val ctrl = controller ?: return
        val name = uri.lastPathSegment ?: "Media"
        val metadata = MediaMetadata.Builder().setTitle(name).build()
        val exoItem = ExoMediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build()
        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()
        ctrl.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed))
        val synthetic = MediaItem(id = -1L, uri = uri, title = name, mimeType = mimeType)
        _state.update { it.copy(currentItem = synthetic, lyrics = emptyList()) }
        startPositionTracking()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekForward() {
        val ctrl = controller ?: return
        val secs = _state.value.skipSeconds.toLong() * 1000
        val newPos = (ctrl.currentPosition + secs).coerceAtMost(ctrl.duration.takeIf { it >= 0 } ?: Long.MAX_VALUE)
        ctrl.seekTo(newPos); _state.update { it.copy(positionMs = newPos) }
    }

    fun seekBackward() {
        val ctrl = controller ?: return
        val secs = _state.value.skipSeconds.toLong() * 1000
        val newPos = (ctrl.currentPosition - secs).coerceAtLeast(0L)
        ctrl.seekTo(newPos); _state.update { it.copy(positionMs = newPos) }
    }

    fun seekTo(posMs: Long) {
        controller?.seekTo(posMs); _state.update { it.copy(positionMs = posMs) }
    }

    fun setSkipSeconds(secs: Int) { _state.update { it.copy(skipSeconds = secs) } }
    fun setFilter(tab: FilterTab) { _state.update { it.copy(filterTab = tab) } }
    fun setLandscape(landscape: Boolean) { _state.update { it.copy(isLandscape = landscape) } }
    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackParameters(PlaybackParameters(speed))
        _state.update { it.copy(playbackSpeed = speed) }
    }

    // ── Shuffle & Queue ────────────────────────────────────────────────────────

    fun toggleShuffle() { _state.update { it.copy(isShuffle = !it.isShuffle) } }

    fun toggleQueueMode() {
        val s = _state.value
        if (!s.isQueueMode) {
            val list = filteredList()
            val shuffled = if (s.isShuffle) list.shuffled() else list
            val idx = shuffled.indexOfFirst { it.id == s.currentItem?.id }.coerceAtLeast(0)
            _state.update { it.copy(isQueueMode = true, queue = shuffled, queueIndex = idx) }
        } else {
            _state.update { it.copy(isQueueMode = false) }
        }
    }

    fun playNext() = advanceQueue(forward = true)
    fun playPrev() = advanceQueue(forward = false)

    private fun advanceQueue(forward: Boolean = true) {
        val s = _state.value
        if (!s.isQueueMode || s.queue.isEmpty()) return
        val nextIdx = if (forward) {
            if (s.isShuffle) s.queue.indices.random() else (s.queueIndex + 1) % s.queue.size
        } else {
            if (s.queueIndex > 0) s.queueIndex - 1 else s.queue.size - 1
        }
        val nextItem = s.queue.getOrNull(nextIdx) ?: return
        _state.update { it.copy(queueIndex = nextIdx) }
        playItem(nextItem)
    }

    // ── Favorites & Playlists ─────────────────────────────────────────────────

    fun toggleFavourite(item: MediaItem) {
        val s = _state.value
        val favPlaylist = s.playlists.firstOrNull { it.isFavourites } ?: return
        val isFav = favPlaylist.itemIds.contains(item.id)
        val updatedFav = if (isFav) favPlaylist.copy(itemIds = favPlaylist.itemIds - item.id)
                         else favPlaylist.copy(itemIds = favPlaylist.itemIds + item.id)
        val updatedPlaylists = s.playlists.map { if (it.isFavourites) updatedFav else it }
        _state.update { it.copy(playlists = updatedPlaylists) }
        if (!isFav) showFavouriteToast()
        persistData()
    }

    fun isFavourite(item: MediaItem): Boolean {
        val fav = _state.value.playlists.firstOrNull { it.isFavourites } ?: return false
        return fav.itemIds.contains(item.id)
    }

    private fun showFavouriteToast() {
        toastJob?.cancel()
        _state.update { it.copy(showFavouriteToast = true) }
        toastJob = viewModelScope.launch {
            delay(2500); _state.update { it.copy(showFavouriteToast = false) }
        }
    }

    fun createPlaylist(name: String) {
        val newId = System.currentTimeMillis()
        _state.update { it.copy(playlists = it.playlists + Playlist(id = newId, name = name)) }
        persistData()
    }

    fun addToPlaylist(playlistId: Long, item: MediaItem) {
        val updated = _state.value.playlists.map { p ->
            if (p.id == playlistId && !p.itemIds.contains(item.id))
                p.copy(itemIds = p.itemIds + item.id) else p
        }
        _state.update { it.copy(playlists = updated) }
        persistData()
    }

    fun removeFromPlaylist(playlistId: Long, itemId: Long) {
        val updated = _state.value.playlists.map { p ->
            if (p.id == playlistId) p.copy(itemIds = p.itemIds - itemId) else p
        }
        _state.update { it.copy(playlists = updated) }
        persistData()
    }

    fun deletePlaylist(playlistId: Long) {
        _state.update { it.copy(playlists = it.playlists.filter { p -> p.id != playlistId || p.isFavourites }) }
        persistData()
    }

    fun playPlaylist(playlist: Playlist) {
        val s = _state.value
        val allMedia = s.mediaList + s.extraMediaList
        val items = playlist.itemIds.mapNotNull { id -> allMedia.firstOrNull { it.id == id } }
        if (items.isEmpty()) return
        val shuffled = if (s.isShuffle) items.shuffled() else items
        _state.update { it.copy(isQueueMode = true, queue = shuffled, queueIndex = 0) }
        playItem(shuffled[0])
    }

    fun mergePlaylists(sourceIds: List<Long>, newName: String) {
        val s = _state.value
        val mergedIds = sourceIds
            .flatMap { id -> s.playlists.firstOrNull { it.id == id }?.itemIds ?: emptyList() }
            .distinct()
        val newPlaylist = Playlist(id = System.currentTimeMillis(), name = newName, itemIds = mergedIds)
        _state.update { it.copy(playlists = s.playlists + newPlaylist) }
        persistData()
    }

    // ── ZIP import ────────────────────────────────────────────────────────────

    fun importZip(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                // Use files dir (not cache) so files persist across restarts
                val zipName = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringBeforeLast('.')
                    ?: "ZIP Import"
                val outDir = File(ctx.filesDir, "zip_${zipName.replace("[^a-zA-Z0-9]".toRegex(), "_")}").also { it.mkdirs() }
                val importedItems = mutableListOf<MediaItem>()

                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = File(entry.name).name
                            val ext = name.substringAfterLast('.', "").lowercase()
                            if (!entry.isDirectory && ext in setOf("mp3","flac","ogg","m4a","aac","wav","mp4","mkv","avi","webm")) {
                                val outFile = File(outDir, name)
                                FileOutputStream(outFile).use { zip.copyTo(it) }
                                val isVideo = ext in setOf("mp4","mkv","avi","webm")
                                val mime = if (isVideo) "video/$ext" else "audio/$ext"
                                val lyricsFile = LyricsParser.findLyricsForMedia(outFile.absolutePath)
                                val itemId = outFile.absolutePath.hashCode().toLong()
                                importedItems.add(MediaItem(
                                    id = itemId,
                                    uri = Uri.fromFile(outFile),
                                    title = name.substringBeforeLast('.'),
                                    artist = zipName,   // zip name as artist
                                    album = zipName,
                                    duration = 0L,
                                    mimeType = mime,
                                    lyricsPath = lyricsFile?.absolutePath
                                ))
                            }
                            zip.closeEntry(); entry = zip.nextEntry
                        }
                    }
                }

                if (importedItems.isEmpty()) {
                    _state.update { it.copy(zipImportMessage = "No media files found in ZIP") }
                    return@launch
                }

                // Create a playlist named after the ZIP
                val playlistId = System.currentTimeMillis()
                val playlist = Playlist(
                    id = playlistId,
                    name = zipName,
                    itemIds = importedItems.map { it.id }
                )

                _state.update { s ->
                    s.copy(
                        mediaList = s.mediaList + importedItems,
                        extraMediaList = s.extraMediaList + importedItems,
                        playlists = s.playlists + playlist,
                        zipImportedItems = importedItems,
                        zipImportMessage = "Imported ${importedItems.size} file(s) → playlist \"$zipName\""
                    )
                }
                persistData()
            } catch (e: Exception) {
                _state.update { it.copy(zipImportMessage = "Failed to read ZIP: ${e.message}") }
            }
        }
    }

    fun clearZipMessage() = _state.update { it.copy(zipImportMessage = null) }

    // ── Lyrics ────────────────────────────────────────────────────────────────

    fun importLyricsFile(uri: Uri) {
        val item = _state.value.currentItem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val ext = ctx.contentResolver.getType(uri)?.substringAfterLast('/') ?: "lrc"
                val dest = File(ctx.cacheDir, "imported_lyrics.$ext")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                val lyrics = LyricsParser.parse(dest)
                val updatedItem = item.copy(lyricsPath = dest.absolutePath)
                _state.update { it.copy(currentItem = updatedItem, lyrics = lyrics, activeLyricIndex = -1) }
            } catch (_: Exception) {}
        }
    }

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

    fun filteredList(): List<MediaItem> {
        val s = _state.value
        val all = s.mediaList + s.extraMediaList.filter { extra -> s.mediaList.none { it.id == extra.id } }
        return when (s.filterTab) {
            FilterTab.ALL      -> all
            FilterTab.AUDIO    -> all.filter { it.isAudio }
            FilterTab.VIDEO    -> all.filter { it.isVideo }
            FilterTab.PLAYLISTS -> all
        }
    }

    override fun onCleared() {
        positionJob?.cancel(); toastJob?.cancel()
        releaseVisualizer()
        controller?.removeListener(playerListener)
        // Do NOT release the player — let the service handle it so audio stops properly
        controllerFuture?.let { MediaController.releaseFuture(it) }
        persistData()
        super.onCleared()
    }
}
