package com.velora.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.velora.app.MainActivity

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
            // Allow video rendering on all tracks including MP4
            .build()

        // Pending intent to return to app when tapping the notification
        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom skip-forward command for the notification
        val skipForwardCommand = SessionCommand("ACTION_SKIP_FORWARD", android.os.Bundle.EMPTY)
        val skipBackwardCommand = SessionCommand("ACTION_SKIP_BACKWARD", android.os.Bundle.EMPTY)

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
                    args: android.os.Bundle
                ): androidx.media3.session.SessionResult {
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
                    return androidx.media3.session.SessionResult(
                        androidx.media3.session.SessionResult.RESULT_SUCCESS
                    )
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
