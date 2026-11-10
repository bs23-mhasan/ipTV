package com.mhasancse15.iptv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhasancse15.iptv.data.repository.ChannelRepository
import com.mhasancse15.iptv.data.repository.ChannelUiState
import com.mhasancse15.iptv.domain.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BdixViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    val uiState: StateFlow<ChannelUiState> = repository.uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String>("All")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    init {
        viewModelScope.launch {
            repository.refreshChannels()
        }
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
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
        }
    }

    fun getFilteredChannels(): List<Channel> {
        val state = uiState.value
        if (state !is ChannelUiState.Success) return emptyList()
        var channels = state.allChannels
        val query = _searchQuery.value
        val category = _selectedCategory.value
        val country = _selectedCountry.value

        channels = repository.searchChannels(query, channels)
        channels = repository.filterByCategory(channels, category)
        channels = repository.filterByCountry(channels, country)
        return channels
    }

    fun getCategoryList(): List<String> {
        val state = uiState.value
        return if (state is ChannelUiState.Success) {
            listOf("All") + state.categories
        } else emptyList()
    }

    fun getCountryList(): List<String> {
        val state = uiState.value
        return if (state is ChannelUiState.Success) {
            listOf("All") + state.allChannels.map { it.country }.distinct().sorted()
        } else emptyList()
    }

    fun markAsWatched(channelId: String) {
        repository.markAsWatched(channelId)
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshChannels()
        }
    }
}
