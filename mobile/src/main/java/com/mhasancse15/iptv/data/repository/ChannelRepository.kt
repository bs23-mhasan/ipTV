package com.mhasancse15.iptv.data.repository

import com.mhasancse15.iptv.data.local.FavoritesStore
import com.mhasancse15.iptv.data.remote.RemotePlaylistRepository
import com.mhasancse15.iptv.domain.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class ChannelUiState {
    object Loading : ChannelUiState()
    data class Success(
        val allChannels: List<Channel>,
        val featuredChannels: List<Channel>,
        val categories: List<String>,
        val favoriteChannels: List<Channel>,
        val recentChannels: List<Channel>
    ) : ChannelUiState()
    data class Error(val message: String) : ChannelUiState()
}

@Singleton
class ChannelRepository @Inject constructor(
    private val remoteRepository: RemotePlaylistRepository,
    private val favoritesStore: FavoritesStore
) {
    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState

    private var cachedChannels: List<Channel> = emptyList()

    suspend fun refreshChannels() {
        _uiState.update { ChannelUiState.Loading }
        try {
            val fetched = withContext(Dispatchers.IO) { remoteRepository.fetchChannels() }
            cachedChannels = fetched
            updateUiState()
        } catch (e: Exception) {
            _uiState.update { ChannelUiState.Error("Failed to load channels: ${e.message}") }
        }
    }

    private suspend fun updateUiState() {
        val favoriteIds = favoritesStore.favoriteIds.first()
        val channelsWithFavorites = cachedChannels.map {
            it.copy(isFavorite = favoriteIds.contains(it.id))
        }
        val featured = channelsWithFavorites.take(8)
        val categories = channelsWithFavorites.map { it.category }.distinct().sorted()
        val favorites = channelsWithFavorites.filter { it.isFavorite }
        val recent = channelsWithFavorites
            .sortedByDescending { it.lastWatchedTimestamp }
            .take(10)

        _uiState.update {
            ChannelUiState.Success(
                allChannels = channelsWithFavorites,
                featuredChannels = featured,
                categories = categories,
                favoriteChannels = favorites,
                recentChannels = recent
            )
        }
    }

    suspend fun toggleFavorite(channelId: String) {
        favoritesStore.toggleFavorite(channelId)
        updateUiState()
    }

    fun searchChannels(query: String, channels: List<Channel>): List<Channel> {
        if (query.isBlank()) return channels
        val lower = query.lowercase()
        return channels.filter {
            it.name.lowercase().contains(lower) ||
            it.category.lowercase().contains(lower) ||
            it.country.lowercase().contains(lower)
        }
    }

    fun filterByCategory(channels: List<Channel>, category: String): List<Channel> {
        if (category == "All") return channels
        return channels.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun filterByCountry(channels: List<Channel>, country: String): List<Channel> {
        if (country == "All") return channels
        return channels.filter { it.country.equals(country, ignoreCase = true) }
    }

    fun markAsWatched(channelId: String) {
        val updated = cachedChannels.map {
            if (it.id == channelId) {
                it.copy(lastWatchedTimestamp = System.currentTimeMillis())
            } else it
        }
        cachedChannels = updated
        // Could persist recent watched separately, for now just live update
        // updateUiState() later when needed
    }
}
