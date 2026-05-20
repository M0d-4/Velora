package com.velora.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
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

data class PlayerState(
    val mediaList: List<MediaItem> = emptyList(),
    val currentItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val skipSeconds: Int = 10,          // 5 or 10
    val lyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val isLoading: Boolean = true,
    val waveformAmplitudes: List<Float> = List(64) { 0f },
    val filterTab: FilterTab = FilterTab.ALL
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
            if (isPlaying) startPositionTracking() else positionJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration ?: 0L
            _state.update { it.copy(durationMs = if (dur < 0) 0L else dur) }
        }
    }

    fun playItem(item: MediaItem) {
        val ctrl = controller ?: return
        val exoItem = ExoMediaItem.fromUri(item.uri)
        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()

        val lyrics = item.lyricsPath?.let { LyricsParser.parseLrc(File(it)) } ?: emptyList()
        _state.update { it.copy(currentItem = item, lyrics = lyrics, activeLyricIndex = -1) }

        startPositionTracking()
        if (item.isAudio) startWaveformSimulation()
    }

    fun playUri(uri: Uri, mimeType: String) {
        val ctrl = controller ?: return
        val exoItem = ExoMediaItem.fromUri(uri)
        ctrl.setMediaItem(exoItem)
        ctrl.prepare()
        ctrl.play()

        val isVideo = mimeType.startsWith("video")
        val synthetic = MediaItem(
            id = -1L, uri = uri,
            title = uri.lastPathSegment ?: "Media",
            mimeType = mimeType
        )
        _state.update { it.copy(currentItem = synthetic, lyrics = emptyList()) }
        startPositionTracking()
        if (!isVideo) startWaveformSimulation()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekForward() {
        val secs = _state.value.skipSeconds.toLong() * 1000
        controller?.seekTo((controller!!.currentPosition + secs).coerceAtMost(
            controller!!.duration.coerceAtLeast(0L)
        ))
    }

    fun seekBackward() {
        val secs = _state.value.skipSeconds.toLong() * 1000
        controller?.seekTo((controller!!.currentPosition - secs).coerceAtLeast(0L))
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

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val pos = controller?.currentPosition ?: 0L
                val dur = controller?.duration?.let { if (it < 0) 0L else it } ?: 0L
                val lyrics = _state.value.lyrics
                val activeIdx = if (lyrics.isNotEmpty())
                    LyricsParser.activeIndex(lyrics, pos) else -1
                _state.update { it.copy(positionMs = pos, durationMs = dur, activeLyricIndex = activeIdx) }
                delay(200)
            }
        }
    }

    private fun startWaveformSimulation() {
        waveJob?.cancel()
        waveJob = viewModelScope.launch {
            // Simulate audio waveform — in a real app you'd read PCM amplitudes from the decoder
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
                } else {
                    val bars = List(64) { 0.05f }
                    _state.update { it.copy(waveformAmplitudes = bars) }
                }
                delay(80)
            }
        }
    }

    fun filteredList(): List<MediaItem> {
        val all = _state.value.mediaList
        return when (_state.value.filterTab) {
            FilterTab.ALL -> all
            FilterTab.AUDIO -> all.filter { it.isAudio }
            FilterTab.VIDEO -> all.filter { it.isVideo }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        waveJob?.cancel()
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture!!)
        super.onCleared()
    }
}
