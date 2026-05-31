package com.velora.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
    val extraMediaList: List<MediaItem> = emptyList(),
    // Non-null only when playing from an actual playlist (not shuffle-all)
    val currentPlaylistId: Long? = null,
    // Non-null when item belongs to >1 playlist and user presses next/prev
    val pendingPlaylistChoice: List<Playlist>? = null,
    // IDs of items hidden from the library
    val hiddenItemIds: Set<Long> = emptySet()
)

enum class FilterTab { ALL, AUDIO, VIDEO, PLAYLISTS }

private const val PREFS_NAME   = "velora_prefs"
private const val KEY_PLAYLISTS  = "playlists"
private const val KEY_EXTRA_MEDIA = "extra_media"

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var toastJob: Job? = null
    private var visualizer: Visualizer? = null

    init { loadPersistedData(); loadMedia(); connectToService() }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun prefs() = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadPersistedData() {
        val p = prefs()
        val playlistsJson = p.getString(KEY_PLAYLISTS, null)
        val playlists = if (playlistsJson != null) {
            try {
                val arr = JSONArray(playlistsJson)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val ids = obj.getJSONArray("itemIds")
                    Playlist(id = obj.getLong("id"), name = obj.getString("name"),
                        isFavourites = obj.optBoolean("isFavourites", false),
                        itemIds = (0 until ids.length()).map { ids.getLong(it) })
                }
            } catch (_: Exception) { listOf(Playlist(id = 0L, name = "Favorites", isFavourites = true)) }
        } else listOf(Playlist(id = 0L, name = "Favorites", isFavourites = true))

        val extraJson = p.getString(KEY_EXTRA_MEDIA, null)
        val extraMedia = if (extraJson != null) {
            try {
                val arr = JSONArray(extraJson)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    val filePath = obj.optString("filePath", "")
                    val file = File(filePath)
                    if (file.exists()) MediaItem(
                        id = obj.getLong("id"), uri = Uri.fromFile(file),
                        title = obj.getString("title"), artist = obj.optString("artist", ""),
                        album = obj.optString("album", ""), duration = obj.optLong("duration", 0L),
                        mimeType = obj.getString("mimeType"),
                        lyricsPath = obj.optString("lyricsPath").takeIf { it.isNotBlank() },
                        albumArtUri = obj.optString("albumArtUri").takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    ) else null
                }
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        val hiddenJson = prefs().getString("hidden_ids", null)
        val hiddenIds = mutableSetOf<Long>()
        if (hiddenJson != null) {
            try { val a = JSONArray(hiddenJson); for (i in 0 until a.length()) hiddenIds.add(a.getLong(i)) }
            catch (_: Exception) {}
        }
        _state.update { it.copy(playlists = playlists, extraMediaList = extraMedia, hiddenItemIds = hiddenIds) }
    }

    private fun persistData() {
        val s = _state.value
        val p = prefs().edit()
        val arr = JSONArray()
        s.playlists.forEach { pl ->
            val obj = JSONObject(); obj.put("id", pl.id); obj.put("name", pl.name)
            obj.put("isFavourites", pl.isFavourites)
            val ids = JSONArray(); pl.itemIds.forEach { ids.put(it) }; obj.put("itemIds", ids); arr.put(obj)
        }
        p.putString(KEY_PLAYLISTS, arr.toString())
        val mediaArr = JSONArray()
        s.extraMediaList.forEach { item ->
            val obj = JSONObject(); obj.put("id", item.id)
            obj.put("filePath", item.uri.path ?: ""); obj.put("title", item.title)
            obj.put("artist", item.artist); obj.put("album", item.album)
            obj.put("duration", item.duration); obj.put("mimeType", item.mimeType)
            obj.put("lyricsPath", item.lyricsPath ?: "")
            obj.put("albumArtUri", item.albumArtUri?.toString() ?: "")
            mediaArr.put(obj)
        }
        p.putString(KEY_EXTRA_MEDIA, mediaArr.toString())
        val hiddenArr = JSONArray(); s.hiddenItemIds.forEach { hiddenArr.put(it) }
        p.putString("hidden_ids", hiddenArr.toString())
        p.apply()
    }

    private fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = MediaRepository.loadAllMedia(getApplication())
            _state.update { it.copy(mediaList = items, isLoading = false) }
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(getApplication(),
            android.content.ComponentName(getApplication(), PlayerService::class.java))
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
            if (!isPlaying) { releaseVisualizer(); _state.update { it.copy(waveformAmplitudes = List(64) { 0.05f }) } }
            else startVisualizer()
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration ?: 0L
            _state.update { it.copy(durationMs = if (dur < 0) 0L else dur) }
            if (playbackState == Player.STATE_ENDED) advanceQueue()
        }
    }

    // ── Visualizer ─────────────────────────────────────────────────────────────

    private fun startVisualizer() {
        if (_state.value.currentItem?.isVideo == true) return
        releaseVisualizer()
        try {
            // Use the ExoPlayer's actual audio session id so the Visualizer
            // doesn't interfere with the audio output level
            val audioSessionId = com.velora.app.service.PlayerService.player?.audioSessionId ?: 0
            val vis = Visualizer(audioSessionId)
            vis.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    val barCount = 64
                    val samplesPerBar = waveform.size / barCount
                    val bars = (0 until barCount).map { b ->
                        val start = b * samplesPerBar
                        val end = (start + samplesPerBar).coerceAtMost(waveform.size)
                        val rms = sqrt(waveform.slice(start until end)
                            .map { s -> val v2 = (s.toInt() and 0xFF) - 128; (v2 * v2).toDouble() }
                            .average()).toFloat()
                        (rms / 80f).coerceIn(0.04f, 1f)
                    }
                    _state.update { it.copy(waveformAmplitudes = bars) }
                }
                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            vis.enabled = true; visualizer = vis
        } catch (_: Exception) { startFallbackWave() }
    }

    private fun startFallbackWave() {
        viewModelScope.launch {
            var phase = 0f
            while (isActive && _state.value.isPlaying) {
                phase += 0.18f
                val bars = List(64) { i ->
                    val base = Math.sin((i * 0.3 + phase).toDouble()).toFloat()
                    val noise = (Math.random() * 0.3 - 0.15).toFloat()
                    ((base + noise + 1f) / 2f).coerceIn(0.04f, 1f)
                }
                _state.update { it.copy(waveformAmplitudes = bars) }
                delay(80)
            }
        }
    }

    private fun releaseVisualizer() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        visualizer = null
    }

    // ── Playback ───────────────────────────────────────────────────────────────

    fun playItem(item: MediaItem) {
        val ctrl = controller ?: return
        val artUri: Uri? = item.artUri
        val meta = MediaMetadata.Builder()
            .setTitle(item.title).setArtist(item.artist)
            .setAlbumTitle(item.album).setArtworkUri(artUri).build()
        val exoItem = ExoMediaItem.Builder().setUri(item.uri).setMediaMetadata(meta).build()
        ctrl.setMediaItem(exoItem); ctrl.prepare(); ctrl.play()
        ctrl.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed))
        // If item doesn't have a lyricsPath yet, try loading from stable lyrics dir
        val resolvedItem = if (item.lyricsPath == null) {
            val ctx = getApplication<Application>()
            val lyricsDir = File(ctx.filesDir, "lyrics")
            val found = lyricsDir.listFiles()?.firstOrNull { it.name.startsWith("lyrics_${item.id}.") }
            if (found != null) item.copy(lyricsPath = found.absolutePath) else item
        } else item
        val lyrics = resolvedItem.lyricsPath?.let { LyricsParser.parse(File(it)) } ?: emptyList()
        // If the item belongs to playlists but no playlist context is active, auto-set
        // the first matching playlist as the queue so Prev/Next work immediately.
        val currentState = _state.value
        val newPlaylistId: Long?
        val newQueue: List<MediaItem>
        val newQueueIndex: Int
        val newQueueMode: Boolean

        if (currentState.currentPlaylistId != null) {
            // Already in a playlist context — keep it, just update position
            val existingQueue = currentState.queue
            val existingIdx = existingQueue.indexOfFirst { it.id == resolvedItem.id }
            newPlaylistId = currentState.currentPlaylistId
            newQueue = existingQueue
            newQueueIndex = if (existingIdx >= 0) existingIdx else 0
            newQueueMode = true
        } else {
            // Not in a playlist — try to find a playlist containing this item
            val allMedia = currentState.mediaList + currentState.extraMediaList
            val matchingPlaylist = currentState.playlists
                .firstOrNull { !it.isFavourites && it.itemIds.contains(resolvedItem.id) }
            if (matchingPlaylist != null) {
                val items = matchingPlaylist.itemIds
                    .mapNotNull { id -> allMedia.firstOrNull { it.id == id } }
                val idx = items.indexOfFirst { it.id == resolvedItem.id }
                newPlaylistId = matchingPlaylist.id
                newQueue = items
                newQueueIndex = if (idx >= 0) idx else 0
                newQueueMode = true
            } else {
                // No named playlist — build an implicit queue from all same-type media
                // so Prev / Next always work from the library list
                val allMedia = currentState.mediaList + currentState.extraMediaList.filter { e ->
                    currentState.mediaList.none { it.id == e.id }
                }
                val sameType = allMedia.filter {
                    it.id !in currentState.hiddenItemIds &&
                    (resolvedItem.isVideo == it.isVideo)
                }
                val idx = sameType.indexOfFirst { it.id == resolvedItem.id }
                newPlaylistId = null
                newQueue = if (sameType.size > 1) sameType else currentState.queue
                newQueueIndex = if (idx >= 0) idx else 0
                newQueueMode = sameType.size > 1
            }
        }

        _state.update { it.copy(
            currentItem = resolvedItem,
            lyrics = lyrics,
            activeLyricIndex = -1,
            currentPlaylistId = newPlaylistId,
            queue = newQueue,
            queueIndex = newQueueIndex,
            isQueueMode = newQueueMode
        ) }
        startPositionTracking()
    }

    fun playUri(uri: Uri, mimeType: String) {
        val ctrl = controller ?: return
        val name = uri.lastPathSegment ?: "Media"
        val exoItem = ExoMediaItem.Builder().setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(name).build()).build()
        ctrl.setMediaItem(exoItem); ctrl.prepare(); ctrl.play()
        ctrl.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed))
        _state.update { it.copy(currentItem = MediaItem(id=-1L, uri=uri, title=name, mimeType=mimeType), lyrics = emptyList()) }
        startPositionTracking()
    }

    fun togglePlayPause() { val ctrl = controller ?: return; if (ctrl.isPlaying) ctrl.pause() else ctrl.play() }
    fun seekForward() {
        val ctrl = controller ?: return
        val newPos = (ctrl.currentPosition + _state.value.skipSeconds * 1000L).coerceAtMost(ctrl.duration.takeIf { it >= 0 } ?: Long.MAX_VALUE)
        ctrl.seekTo(newPos); _state.update { it.copy(positionMs = newPos) }
    }
    fun seekBackward() {
        val ctrl = controller ?: return
        val newPos = (ctrl.currentPosition - _state.value.skipSeconds * 1000L).coerceAtLeast(0L)
        ctrl.seekTo(newPos); _state.update { it.copy(positionMs = newPos) }
    }
    fun seekTo(posMs: Long) { controller?.seekTo(posMs); _state.update { it.copy(positionMs = posMs) } }
    fun setSkipSeconds(secs: Int) { _state.update { it.copy(skipSeconds = secs) } }
    fun setFilter(tab: FilterTab) { _state.update { it.copy(filterTab = tab) } }
    fun setLandscape(landscape: Boolean) { _state.update { it.copy(isLandscape = landscape) } }
    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackParameters(PlaybackParameters(speed))
        _state.update { it.copy(playbackSpeed = speed) }
    }

    // ── Queue / Shuffle ────────────────────────────────────────────────────────

    fun toggleShuffle() { _state.update { it.copy(isShuffle = !it.isShuffle) } }
    fun toggleQueueMode() {
        val s = _state.value
        if (!s.isQueueMode) {
            val list = filteredList(); val shuffled = if (s.isShuffle) list.shuffled() else list
            val idx = shuffled.indexOfFirst { it.id == s.currentItem?.id }.coerceAtLeast(0)
            _state.update { it.copy(isQueueMode = true, queue = shuffled, queueIndex = idx) }
        } else _state.update { it.copy(isQueueMode = false) }
    }
    fun playNext() = advanceQueue(true)
    fun playPrev() = advanceQueue(false)

    /** Called when user picks a playlist from the selector dialog. */
    fun continueInPlaylist(playlist: Playlist, forward: Boolean = true) {
        _state.update { it.copy(pendingPlaylistChoice = null) }
        val s = _state.value; val allMedia = s.mediaList + s.extraMediaList
        val items = playlist.itemIds.mapNotNull { id -> allMedia.firstOrNull { it.id == id } }
        if (items.isEmpty()) return
        val queue = if (s.isShuffle) items.shuffled() else items
        val currentId = s.currentItem?.id
        val startIdx = if (s.isShuffle) {
            queue.indices.filter { queue[it].id != currentId }.let { if (it.isEmpty()) 0 else it.random() }
        } else {
            val idx = queue.indexOfFirst { it.id == currentId }
            if (forward) (idx + 1) % queue.size else if (idx > 0) idx - 1 else queue.size - 1
        }
        _state.update { it.copy(isQueueMode = true, queue = queue, queueIndex = startIdx, currentPlaylistId = playlist.id) }
        playItem(queue[startIdx])
    }

    fun dismissPlaylistChoice() { _state.update { it.copy(pendingPlaylistChoice = null) } }

    private fun advanceQueue(forward: Boolean = true) {
        val s = _state.value
        // Allow navigation as long as there is a queue, even without a named playlist
        if (s.queue.size < 2) return
        // If shuffle, pick a different random track within same playlist
        val nextIdx = if (s.isShuffle) {
            val candidates = s.queue.indices.filter { it != s.queueIndex }
            if (candidates.isEmpty()) s.queueIndex else candidates.random()
        } else {
            if (forward) (s.queueIndex + 1) % s.queue.size
            else if (s.queueIndex > 0) s.queueIndex - 1 else s.queue.size - 1
        }
        val nextItem = s.queue.getOrNull(nextIdx) ?: return
        // Check if next item belongs to multiple playlists: if so, prompt
        val containingPlaylists = s.playlists.filter { !it.isFavourites && it.itemIds.contains(nextItem.id) }
        if (containingPlaylists.size > 1) {
            _state.update { it.copy(pendingPlaylistChoice = containingPlaylists, queueIndex = nextIdx) }
            return
        }
        _state.update { it.copy(queueIndex = nextIdx) }; playItem(nextItem)
    }

    // ── Favorites & Playlists ──────────────────────────────────────────────────

    fun toggleFavourite(item: MediaItem) {
        val s = _state.value; val fav = s.playlists.firstOrNull { it.isFavourites } ?: return
        val isFav = fav.itemIds.contains(item.id)
        val updated = fav.copy(itemIds = if (isFav) fav.itemIds - item.id else fav.itemIds + item.id)
        _state.update { it.copy(playlists = it.playlists.map { p -> if (p.isFavourites) updated else p }) }
        if (!isFav) showFavouriteToast(); persistData()
    }
    fun isFavourite(item: MediaItem) = _state.value.playlists.firstOrNull { it.isFavourites }?.itemIds?.contains(item.id) == true
    private fun showFavouriteToast() {
        toastJob?.cancel(); _state.update { it.copy(showFavouriteToast = true) }
        toastJob = viewModelScope.launch { delay(2500); _state.update { it.copy(showFavouriteToast = false) } }
    }
    fun createPlaylist(name: String) {
        _state.update { it.copy(playlists = it.playlists + Playlist(id = System.currentTimeMillis(), name = name)) }; persistData()
    }
    fun addToPlaylist(playlistId: Long, item: MediaItem) {
        val updated = _state.value.playlists.map { p -> if (p.id == playlistId && !p.itemIds.contains(item.id)) p.copy(itemIds = p.itemIds + item.id) else p }
        _state.update { it.copy(playlists = updated) }; persistData()
    }
    fun removeFromPlaylist(playlistId: Long, itemId: Long) {
        val updated = _state.value.playlists.map { p -> if (p.id == playlistId) p.copy(itemIds = p.itemIds - itemId) else p }
        _state.update { it.copy(playlists = updated) }; persistData()
    }
    fun deletePlaylist(playlistId: Long) {
        _state.update { it.copy(playlists = it.playlists.filter { p -> p.id != playlistId || p.isFavourites }) }; persistData()
    }
    fun playPlaylist(playlist: Playlist) {
        val s = _state.value; val allMedia = s.mediaList + s.extraMediaList
        val items = playlist.itemIds.mapNotNull { id -> allMedia.firstOrNull { it.id == id } }
        if (items.isEmpty()) return
        val shuffled = if (s.isShuffle) items.shuffled() else items
        _state.update { it.copy(isQueueMode = true, queue = shuffled, queueIndex = 0, currentPlaylistId = playlist.id) }
        playItem(shuffled[0])
    }
    /** Adds all items from [playlistId] into the Favorites playlist. */
    fun addPlaylistToFavorites(playlistId: Long) {
        val s = _state.value
        val src = s.playlists.firstOrNull { it.id == playlistId } ?: return
        val fav = s.playlists.firstOrNull { it.isFavourites } ?: return
        val merged = (fav.itemIds + src.itemIds).distinct()
        _state.update { st ->
            st.copy(playlists = st.playlists.map { p -> if (p.isFavourites) p.copy(itemIds = merged) else p })
        }
        persistData()
    }

    fun mergePlaylists(sourceIds: List<Long>, newName: String, keepOriginals: Boolean = false) {
        val s = _state.value
        val mergedIds = sourceIds.flatMap { id -> s.playlists.firstOrNull { it.id == id }?.itemIds ?: emptyList() }.distinct()
        _state.update { state ->
            val merged = Playlist(id = System.currentTimeMillis(), name = newName, itemIds = mergedIds)
            val filtered = if (keepOriginals) state.playlists else state.playlists.filter { p -> p.id !in sourceIds || p.isFavourites }
            state.copy(playlists = filtered + merged)
        }; persistData()
    }
    fun renamePlaylist(playlistId: Long, newName: String) {
        val updated = _state.value.playlists.map { p ->
            if (p.id == playlistId && !p.isFavourites) p.copy(name = newName) else p
        }
        _state.update { it.copy(playlists = updated) }; persistData()
    }

    // ── Remove imported media ──────────────────────────────────────────────────

    fun removeImportedMedia(itemId: Long) {
        val s = _state.value
        // Remove the physical file if it exists
        val item = s.extraMediaList.firstOrNull { it.id == itemId }
        item?.uri?.path?.let { path -> try { File(path).delete() } catch (_: Exception) {} }
        // Remove from all playlists, media lists
        val updatedPlaylists = s.playlists.map { p -> p.copy(itemIds = p.itemIds - itemId) }
        _state.update { it.copy(
            extraMediaList = it.extraMediaList.filter { m -> m.id != itemId },
            mediaList = it.mediaList.filter { m -> m.id != itemId },
            playlists = updatedPlaylists
        ) }
        persistData()
    }

    fun multiDeleteImportedMedia(itemIds: Set<Long>, deleteFiles: Boolean) {
        val s = _state.value
        if (deleteFiles) {
            itemIds.forEach { id ->
                s.extraMediaList.firstOrNull { it.id == id }?.uri?.path
                    ?.let { path -> try { File(path).delete() } catch (_: Exception) {} }
            }
        }
        val updatedPlaylists = s.playlists.map { p -> p.copy(itemIds = p.itemIds.filter { it !in itemIds }) }
        _state.update { it.copy(
            extraMediaList = it.extraMediaList.filter { m -> m.id !in itemIds },
            mediaList = if (deleteFiles) it.mediaList.filter { m -> m.id !in itemIds } else it.mediaList,
            playlists = updatedPlaylists
        ) }; persistData()
    }

    fun multiDeletePlaylists(playlistIds: Set<Long>, deleteFiles: Boolean) {
        val s = _state.value
        if (deleteFiles) {
            val affectedItemIds = s.playlists
                .filter { it.id in playlistIds && !it.isFavourites }
                .flatMap { it.itemIds }.toSet()
            affectedItemIds.forEach { id ->
                s.extraMediaList.firstOrNull { it.id == id }?.uri?.path
                    ?.let { path -> try { File(path).delete() } catch (_: Exception) {} }
            }
            _state.update { it.copy(
                extraMediaList = it.extraMediaList.filter { m -> m.id !in affectedItemIds },
                mediaList = it.mediaList.filter { m -> m.id !in affectedItemIds }
            ) }
        }
        _state.update { it.copy(
            playlists = it.playlists.filter { p -> p.id !in playlistIds || p.isFavourites }
        ) }; persistData()
    }

    // ── ZIP import ─────────────────────────────────────────────────────────────

    fun importZip(
        uri: Uri,
        playlistMode: com.velora.app.model.ZipPlaylistMode = com.velora.app.model.ZipPlaylistMode.NEW,
        existingPlaylistId: Long? = null,
        customPlaylistName: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                // Resolve the real display name from the content resolver if possible
                val zipName = run {
                    val cursor = ctx.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (it.moveToFirst() && idx >= 0) it.getString(idx)
                            ?.substringBeforeLast('.') ?: ""
                        else ""
                    }.takeIf { !it.isNullOrBlank() }
                        ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                        ?: "ZIP Import"
                }
                // Use custom name if provided (from dialog), else fall back to zip filename
                val playlistName = customPlaylistName?.trim()?.ifBlank { null } ?: zipName
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
                                val meta = extractMetadataFull(outFile, name, playlistName, ctx)
                                importedItems.add(MediaItem(
                                    id = itemId, uri = Uri.fromFile(outFile),
                                    title = meta.title, artist = meta.artist, album = playlistName,
                                    duration = meta.durationMs, mimeType = mime,
                                    lyricsPath = lyricsFile?.absolutePath,
                                    albumArtUri = meta.artUri
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

                _state.update { s ->
                    val newPlaylists = when (playlistMode) {
                        com.velora.app.model.ZipPlaylistMode.NEW -> {
                            val pl = Playlist(id = System.currentTimeMillis(), name = playlistName, itemIds = importedItems.map { it.id })
                            s.playlists + pl
                        }
                        com.velora.app.model.ZipPlaylistMode.EXISTING -> {
                            s.playlists.map { pl ->
                                if (pl.id == existingPlaylistId) pl.copy(itemIds = pl.itemIds + importedItems.map { it.id })
                                else pl
                            }
                        }
                        com.velora.app.model.ZipPlaylistMode.NONE -> s.playlists
                    }
                    val msg = when (playlistMode) {
                        com.velora.app.model.ZipPlaylistMode.NEW -> "Imported ${importedItems.size} file(s) → playlist \"$playlistName\""
                        com.velora.app.model.ZipPlaylistMode.EXISTING -> "Imported ${importedItems.size} file(s) into playlist"
                        com.velora.app.model.ZipPlaylistMode.NONE -> "Imported ${importedItems.size} file(s)"
                    }
                    s.copy(
                        mediaList = s.mediaList + importedItems,
                        extraMediaList = s.extraMediaList + importedItems,
                        playlists = newPlaylists,
                        zipImportedItems = importedItems,
                        zipImportMessage = msg
                    )
                }
                persistData()
            } catch (e: Exception) { _state.update { it.copy(zipImportMessage = "Failed to read ZIP: ${e.message}") } }
        }
    }

    fun toggleHideItem(itemId: Long) {
        _state.update { s ->
            val updated = if (itemId in s.hiddenItemIds) s.hiddenItemIds - itemId else s.hiddenItemIds + itemId
            s.copy(hiddenItemIds = updated)
        }
        persistData()
    }

    fun removeLyrics() {
        val item = _state.value.currentItem ?: return
        // Delete the file if it lives in our stable lyrics dir
        item.lyricsPath?.let { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        val updatedItem = item.copy(lyricsPath = null)
        _state.update { s ->
            val updatedMedia = s.mediaList.map { m -> if (m.id == item.id) m.copy(lyricsPath = null) else m }
            val updatedExtra = s.extraMediaList.map { m -> if (m.id == item.id) m.copy(lyricsPath = null) else m }
            s.copy(
                currentItem  = updatedItem,
                lyrics       = emptyList(),
                activeLyricIndex = -1,
                mediaList    = updatedMedia,
                extraMediaList = updatedExtra
            )
        }
        persistData()
    }

    /** Extract title, artist and embedded art from a media file */
    private fun extractMetadata(file: File, fileName: String, fallbackArtist: String, ctx: Context): Triple<String, String, Uri?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.')
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
            // For videos (or any file with no embedded artist), fall back to "Unknown Artist"
            // only use the zip name as fallback for audio files
            val ext = file.extension.lowercase()
            val isVideo = ext in setOf("mp4", "mkv", "avi", "webm")
            val artist = rawArtist ?: if (isVideo) "Unknown Artist" else fallbackArtist.ifBlank { "Unknown Artist" }
            // Extract duration from metadata
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            // Save embedded art to a file next to the media file (audio only – videos have own thumbnails)
            val artUri: Uri? = if (!isVideo) {
                retriever.embeddedPicture?.let { bytes ->
                    val artFile = File(file.parent, "${file.nameWithoutExtension}_art.jpg")
                    artFile.outputStream().use { it.write(bytes) }
                    Uri.fromFile(artFile)
                }
            } else null
            Triple(title, artist, artUri)
        } catch (_: Exception) {
            val isVideo = file.extension.lowercase() in setOf("mp4", "mkv", "avi", "webm")
            Triple(fileName.substringBeforeLast('.'), if (isVideo) "Unknown Artist" else fallbackArtist.ifBlank { "Unknown Artist" }, null)
        } finally {
            retriever.release()
        }
    }

    // Overload that also returns duration
    private fun extractMetadataFull(file: File, fileName: String, fallbackArtist: String, ctx: Context): Quadruple {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.')
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
            val ext = file.extension.lowercase()
            val isVideo = ext in setOf("mp4", "mkv", "avi", "webm")
            val artist = rawArtist ?: if (isVideo) "Unknown Artist" else fallbackArtist.ifBlank { "Unknown Artist" }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val artUri: Uri? = if (!isVideo) {
                retriever.embeddedPicture?.let { bytes ->
                    val artFile = File(file.parent, "${file.nameWithoutExtension}_art.jpg")
                    artFile.outputStream().use { it.write(bytes) }
                    Uri.fromFile(artFile)
                }
            } else null
            Quadruple(title, artist, artUri, durationMs)
        } catch (_: Exception) {
            val isVideo = file.extension.lowercase() in setOf("mp4", "mkv", "avi", "webm")
            Quadruple(fileName.substringBeforeLast('.'), if (isVideo) "Unknown Artist" else fallbackArtist.ifBlank { "Unknown Artist" }, null, 0L)
        } finally {
            retriever.release()
        }
    }

    data class Quadruple(val title: String, val artist: String, val artUri: android.net.Uri?, val durationMs: Long)

    fun clearZipMessage() = _state.update { it.copy(zipImportMessage = null) }

    // ── Lyrics ─────────────────────────────────────────────────────────────────

    fun importLyricsFile(uri: Uri) {
        val item = _state.value.currentItem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val ext = ctx.contentResolver.getType(uri)?.substringAfterLast('/') ?: "lrc"
                // Use a stable filename keyed by media id so lyrics survive navigation
                val lyricsDir = File(ctx.filesDir, "lyrics").also { it.mkdirs() }
                val dest = File(lyricsDir, "lyrics_${item.id}.$ext")
                ctx.contentResolver.openInputStream(uri)?.use { inp -> FileOutputStream(dest).use { inp.copyTo(it) } }
                val lyrics = LyricsParser.parse(dest)
                val updatedItem = item.copy(lyricsPath = dest.absolutePath)
                _state.update { s ->
                    // Persist lyricsPath back into mediaList / extraMediaList so reload survives
                    val updatedMedia = s.mediaList.map { m -> if (m.id == item.id) m.copy(lyricsPath = dest.absolutePath) else m }
                    val updatedExtra = s.extraMediaList.map { m -> if (m.id == item.id) m.copy(lyricsPath = dest.absolutePath) else m }
                    s.copy(
                        currentItem = updatedItem,
                        lyrics = lyrics,
                        activeLyricIndex = -1,
                        mediaList = updatedMedia,
                        extraMediaList = updatedExtra
                    )
                }
                persistData()
            } catch (_: Exception) {}
        }
    }

    // ── Position tracking ──────────────────────────────────────────────────────

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
        val all = (s.mediaList + s.extraMediaList.filter { extra -> s.mediaList.none { it.id == extra.id } })
            .filter { it.id !in s.hiddenItemIds }
        return when (s.filterTab) {
            FilterTab.ALL      -> all
            FilterTab.AUDIO    -> all.filter { it.isAudio }
            FilterTab.VIDEO    -> all.filter { it.isVideo }
            FilterTab.PLAYLISTS -> all
        }
    }

    override fun onCleared() {
        positionJob?.cancel(); toastJob?.cancel(); releaseVisualizer()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        persistData(); super.onCleared()
    }
}
