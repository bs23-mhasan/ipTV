package com.mhasancse15.iptv.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mhasancse15.iptv.presentation.BdixViewModel
import com.mhasancse15.iptv.presentation.home.components.*
import com.mhasancse15.iptv.ui.theme.Color

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    viewModel: BdixViewModel = hiltViewModel(),
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCountryClick: (String) -> Unit
) {
    val uiState = viewModel.uiState.value

    LaunchedEffect(Unit) {
        if (uiState is ChannelUiState.Loading) {
            viewModel.refresh()
        }
    }

    when (uiState) {
        is ChannelUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading channels...", style = MaterialTheme.typography.bodyLarge)
            }
        }

        is ChannelUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Error: ${uiState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        is ChannelUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    HomeHeader(
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick
                    )
                }

                // Featured Channel
                item {
                    FeaturedChannelCard(
                        channel = uiState.featuredChannels.firstOrNull(),
                        onClick = { onChannelClick(it.id) }
                    )
                }

                // Categories
                if (uiState.categories.isNotEmpty()) {
                    item {
                        CategoriesSection(
                            categories = uiState.categories,
                            onCategoryClick = onCategoryClick
                        )
                    }
                }

                // Popular Channels
                item {
                    ChannelsSection(
                        title = "Popular Channels",
                        channels = uiState.allChannels.take(20),
                        onChannelClick = onChannelClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }

                // Favorites
                if (uiState.favoriteChannels.isNotEmpty()) {
                    item {
                        ChannelsSection(
                            title = "Favorites",
                            channels = uiState.favoriteChannels,
                            onChannelClick = onChannelClick,
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }

                // Recent Channels
                if (uiState.recentChannels.isNotEmpty()) {
                    item {
                        ChannelsSection(
                            title = "Recently Watched",
                            channels = uiState.recentChannels,
                            onChannelClick = onChannelClick,
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }

                // Bottom padding
                item {
                    Box(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
