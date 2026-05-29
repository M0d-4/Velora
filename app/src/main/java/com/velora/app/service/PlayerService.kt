package com.velora.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.velora.app.MainActivity
import com.velora.app.R

@UnstableApi
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        /**
         * Direct reference to the underlying ExoPlayer.
         * Set after onCreate(), cleared in onDestroy().
         * VideoPlayerScreen binds a PlayerView to this instance.
         */
        @Volatile var player: ExoPlayer? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(false)
            .build()
        exo.volume = 1f
        player = exo

        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipForwardCommand  = SessionCommand("ACTION_SKIP_FORWARD",  Bundle.EMPTY)
        val skipBackwardCommand = SessionCommand("ACTION_SKIP_BACKWARD", Bundle.EMPTY)

        val skipForwardButton = CommandButton.Builder()
            .setDisplayName("Skip Forward")
            .setIconResId(androidx.media3.ui.R.drawable.exo_icon_next)
            .setSessionCommand(skipForwardCommand)
            .build()

        val skipBackwardButton = CommandButton.Builder()
            .setDisplayName("Skip Backward")
            .setIconResId(androidx.media3.ui.R.drawable.exo_icon_previous)
            .setSessionCommand(skipBackwardCommand)
            .build()

        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(activityIntent)
            .setCustomLayout(ImmutableList.of(skipBackwardButton, skipForwardButton))
            .setCallback(object : MediaSession.Callback {
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        "ACTION_SKIP_FORWARD" -> {
                            val pos = exo.currentPosition + 10_000L
                            exo.seekTo(pos.coerceAtMost(exo.duration.coerceAtLeast(0L)))
                        }
                        "ACTION_SKIP_BACKWARD" -> {
                            val pos = exo.currentPosition - 10_000L
                            exo.seekTo(pos.coerceAtLeast(0L))
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(1001)
                .setChannelId("velora_playback")
                .setChannelName(androidx.media3.session.R.string.default_notification_channel_name)
                .build()
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        player?.stop()
        player?.clearMediaItems()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        player = null
        mediaSession?.run { player?.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}
