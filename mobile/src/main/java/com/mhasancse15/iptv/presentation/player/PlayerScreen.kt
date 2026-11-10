package com.mhasancse15.iptv.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mhasancse15.iptv.domain.model.Channel
import com.mhasancse15.iptv.ui.theme.Color

@Composable
fun PlayerScreen(
    channel: Channel?,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, channel) {
        channel?.let {
            viewModel.loadChannel(it)
        }
        onDispose {
            viewModel.release()
        }
    }

    val uiState = viewModel.uiState.value

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is PlayerUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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

            is PlayerUiState.Error -> {
                Box(
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

            is PlayerUiState.Success -> {
                // Video Player surface
                ExoPlayerSurface(
                    exoPlayer = viewModel.exoPlayer,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay controls
                PlayerOverlay(
                    channel = channel,
                    onBack = onBack,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    isPlaying = uiState.isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() }
                )
            }
        }
    }
}