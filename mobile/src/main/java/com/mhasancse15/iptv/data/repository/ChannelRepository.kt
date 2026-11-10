package com.mhasancse15.iptv.data.repository

import com.mhasancse15.iptv.data.local.FavoritesStore
import com.mhasancse15.iptv.data.remote.RemotePlaylistRepository
import com.mhasancse15.iptv.domain.model.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

sealed class ChannelUiState {
    object Loading : ChannelUiState()
    data class Success(
        val allChannels: List<Channel>,
        val featuredChannels: List<Channel>,
        val categories: List<String>,
        val favoriteChannels: List<Channel>,
        val recentChannels: List<Channel>,
        val warning: String? = null
    ) : ChannelUiState()
    data class Error(val message: String) : ChannelUiState()
}

@Singleton
class ChannelRepository @Inject constructor(
    private val remoteRepository: RemotePlaylistRepository,
    private val favoritesStore: FavoritesStore
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(true)

    val uiState: StateFlow<ChannelUiState> = combine(
        favoritesStore.cachedChannels,
        favoritesStore.favoriteIds,
        favoritesStore.recentWatched,
        loadError,
        isRefreshing
    ) { cached, favoriteIds, recentWatched, error, refreshing ->
        val channels = cached.map { channel ->
            channel.copy(
                isFavorite = channel.id in favoriteIds,
                lastWatchedTimestamp = recentWatched[channel.id] ?: 0L
            )
        }
        when {
            refreshing && channels.isEmpty() -> ChannelUiState.Loading
            channels.isEmpty() && error != null -> ChannelUiState.Error(error)
            else -> ChannelUiState.Success(
                allChannels = channels,
                featuredChannels = channels.take(8),
                categories = channels.map { it.category }.filter(String::isNotBlank).distinct().sorted(),
                favoriteChannels = channels.filter { it.isFavorite },
                recentChannels = channels
                    .filter { it.lastWatchedTimestamp > 0L }
                    .sortedByDescending { it.lastWatchedTimestamp }
                    .take(10),
                warning = error
            )
        }
    }.stateIn(repositoryScope, SharingStarted.Eagerly, ChannelUiState.Loading)
    val themePreference = favoritesStore.themePreference

    suspend fun refreshChannels() {
        isRefreshing.value = true
        try {
            val fetched = remoteRepository.fetchChannels()
            if (fetched.isEmpty()) throw IllegalStateException("The playlist sources contained no channels")
            favoritesStore.cacheChannels(fetched)
            loadError.value = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loadError.value = "Refresh failed: ${error.message ?: "network error"}"
        } finally {
            isRefreshing.value = false
        }
    }

    suspend fun toggleFavorite(channelId: String) {
        favoritesStore.toggleFavorite(channelId)
    }

    suspend fun clearFavorites() {
        favoritesStore.clearFavorites()
    }

    suspend fun setThemePreference(theme: String) {
        favoritesStore.setThemePreference(theme)
    }

    suspend fun clearCache() {
        favoritesStore.clearCache()
        loadError.value = null
        isRefreshing.value = true
    }

    suspend fun markAsWatched(channelId: String) {
        favoritesStore.markWatched(channelId)
    }

    fun searchChannels(query: String, channels: List<Channel>): List<Channel> {
        if (query.isBlank()) return channels
        return channels.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true)
        }
    }

    fun filterByCategory(channels: List<Channel>, category: String): List<Channel> =
        if (category == "All") channels
        else channels.filter { it.category.equals(category, ignoreCase = true) }

    fun filterByCountry(channels: List<Channel>, country: String): List<Channel> =
        if (country == "All") channels
        else channels.filter { it.country.equals(country, ignoreCase = true) }
}
