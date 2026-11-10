package com.mhasancse15.iptv.presentation.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mhasancse15.iptv.domain.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Loading : PlayerUiState()
    data class Success(val isPlaying: Boolean) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoPlayer: ExoPlayer
) : ViewModel() {
    val player: ExoPlayer
        get() = exoPlayer

    var uiState by mutableStateOf<PlayerUiState>(PlayerUiState.Idle)
        private set
    private var currentChannel: Channel? = null
    private var retryCount = 0
    private var retryJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> uiState = PlayerUiState.Loading
                Player.STATE_READY -> uiState = PlayerUiState.Success(exoPlayer.isPlaying)
                Player.STATE_IDLE -> if (uiState !is PlayerUiState.Error) uiState = PlayerUiState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (uiState is PlayerUiState.Success) uiState = PlayerUiState.Success(isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            if (retryCount < MAX_AUTOMATIC_RETRIES && currentChannel != null) {
                retryCount++
                uiState = PlayerUiState.Loading
                retryJob?.cancel()
                retryJob = viewModelScope.launch {
                    delay(RETRY_DELAY_MILLIS * retryCount)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            } else {
                uiState = PlayerUiState.Error("Unable to play this stream (${error.errorCodeName})")
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    fun loadChannel(channel: Channel) {
        retryJob?.cancel()
        retryCount = 0
        currentChannel = channel
        uiState = PlayerUiState.Loading
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (error: RuntimeException) {
            uiState = PlayerUiState.Error(error.message ?: "Unable to start this stream")
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    override fun onCleared() {
        retryJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val MAX_AUTOMATIC_RETRIES = 2
        const val RETRY_DELAY_MILLIS = 1_000L
    }
}
