package com.mhasancse15.iptv.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mhasancse15.iptv.domain.model.Channel

@Composable
fun PlayerScreen(
    channel: Channel?,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner, channel) {
        channel?.let {
            viewModel.loadChannel(it)
        }
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            viewModel.player.pause()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val uiState = viewModel.uiState
    var controlsVisible by remember(channel) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is PlayerUiState.Loading, is PlayerUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(channel) {
                            detectTapGestures(
                                onLongPress = { controlsVisible = true }
                            )
                        }
                ) {
                    ExoPlayerSurface(
                        exoPlayer = viewModel.player,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (uiState is PlayerUiState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading channel...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = channel?.name ?: "Unknown",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    PlayerOverlay(
                        channel = channel,
                        onBack = onBack,
                        onPrevious = {
                            controlsVisible = false
                            onPrevious()
                        },
                        onNext = {
                            controlsVisible = false
                            onNext()
                        },
                        isPlaying = uiState is PlayerUiState.Success && uiState.isPlaying,
                        onPlayPause = {
                            controlsVisible = false
                            viewModel.togglePlayPause()
                        },
                        visible = controlsVisible
                    )
                }
            }

            PlayerUiState.Idle -> Unit

            is PlayerUiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { channel?.let { viewModel.loadChannel(it) } },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}