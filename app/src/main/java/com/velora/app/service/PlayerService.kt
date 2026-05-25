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

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            // Loudness normalisation OFF — lets the track play at its native level
            .setSkipSilenceEnabled(false)
            .build()

        // Full volume — ExoPlayer defaults to 1f but be explicit
        player.volume = 1f

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

        mediaSession = MediaSession.Builder(this, player)
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
                            val pos = player.currentPosition + 10_000L
                            player.seekTo(pos.coerceAtMost(player.duration.coerceAtLeast(0L)))
                        }
                        "ACTION_SKIP_BACKWARD" -> {
                            val pos = player.currentPosition - 10_000L
                            player.seekTo(pos.coerceAtLeast(0L))
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()

        // Media3 handles the media notification automatically via MediaSessionService.
        // Calling setMediaNotificationProvider gives us a live notification with
        // album art, play/pause, skip — and powers the lock-screen Now Playing widget.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(1001)
                .setChannelId("velora_playback")
                .setChannelName(androidx.media3.session.R.string.default_notification_channel_name)
                .build()
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            player.stop()
            player.clearMediaItems()
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.run { player.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}
