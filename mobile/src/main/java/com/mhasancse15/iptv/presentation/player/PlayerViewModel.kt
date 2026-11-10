package com.mhasancse15.iptv.presentation.player

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import com.mhasancse15.iptv.domain.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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

    fun loadChannel(channel: Channel) {
        uiState = PlayerUiState.Loading
        try {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            uiState = PlayerUiState.Success(isPlaying = true)
        } catch (e: Exception) {
            uiState = PlayerUiState.Error(e.message ?: "Unknown playback error")
        }
    }

    fun togglePlayPause() {
        val current = uiState
        if (current is PlayerUiState.Success) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                uiState = current.copy(isPlaying = false)
            } else {
                exoPlayer.play()
                uiState = current.copy(isPlaying = true)
            }
        }
    }

    fun release() {
        try {
            exoPlayer.release()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error releasing player", e)
        }
    }
}