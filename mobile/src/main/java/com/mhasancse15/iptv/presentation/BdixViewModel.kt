package com.mhasancse15.iptv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhasancse15.iptv.data.remote.RemoteEpgRepository
import com.mhasancse15.iptv.data.repository.ChannelRepository
import com.mhasancse15.iptv.data.repository.ChannelUiState
import com.mhasancse15.iptv.domain.model.Channel
import com.mhasancse15.iptv.domain.model.EpgProgrammeWithChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class BdixViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val epgRepository: RemoteEpgRepository
) : ViewModel() {

    val uiState: StateFlow<ChannelUiState> = repository.uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _effectiveSearchQuery = MutableStateFlow("")
    val effectiveSearchQuery: StateFlow<String> = _effectiveSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow("All")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()
    val themePreference: StateFlow<String> = repository.themePreference.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "System"
    )

    private val _epgData = MutableStateFlow<List<EpgProgrammeWithChannel>>(emptyList())
    val epgData: StateFlow<List<EpgProgrammeWithChannel>> = _epgData.asStateFlow()
    private val _epgError = MutableStateFlow<String?>(null)
    val epgError: StateFlow<String?> = _epgError.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery.debounce(250).collect { _effectiveSearchQuery.value = it }
        }
        refresh()
        refreshEpg()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectCountry(country: String) {
        _selectedCountry.value = country
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { repository.toggleFavorite(channelId) }
    }

    fun clearFavorites() {
        viewModelScope.launch { repository.clearFavorites() }
    }

    fun setThemePreference(theme: String) {
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun getFilteredChannels(query: String = _effectiveSearchQuery.value): List<Channel> {
        val state = uiState.value
        if (state !is ChannelUiState.Success) return emptyList()
        var channels = repository.searchChannels(query, state.allChannels)
        channels = repository.filterByCategory(channels, _selectedCategory.value)
        return repository.filterByCountry(channels, _selectedCountry.value)
    }

    fun getSearchResults(query: String = _effectiveSearchQuery.value): List<Channel> {
        val state = uiState.value
        return if (state is ChannelUiState.Success) {
            repository.searchChannels(query, state.allChannels)
        } else emptyList()
    }

    fun getCategoryList(): List<String> {
        val state = uiState.value
        return if (state is ChannelUiState.Success) listOf("All") + state.categories else emptyList()
    }

    fun getCountryList(): List<String> {
        val state = uiState.value
        return if (state is ChannelUiState.Success) {
            listOf("All") + state.allChannels.map { it.country }.filter(String::isNotBlank).distinct().sorted()
        } else emptyList()
    }

    fun markAsWatched(channelId: String) {
        viewModelScope.launch { repository.markAsWatched(channelId) }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshChannels() }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            repository.refreshChannels()
            refreshEpg()
        }
    }

    fun refreshEpg() {
        viewModelScope.launch {
            try {
                _epgData.value = epgRepository.refresh()
                _epgError.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _epgData.value = epgRepository.cached()
                _epgError.value = "EPG refresh failed: ${error.message ?: "network error"}"
            }
        }
    }
}
