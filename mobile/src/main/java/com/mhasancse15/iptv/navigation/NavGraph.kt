package com.mhasancse15.iptv.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhasancse15.iptv.domain.model.Channel
import com.mhasancse15.iptv.presentation.BdixViewModel
import com.mhasancse15.iptv.presentation.about.AboutScreen
import com.mhasancse15.iptv.presentation.channels.AllChannelsScreen
import com.mhasancse15.iptv.presentation.epg.EpgScreen
import com.mhasancse15.iptv.presentation.favorites.FavoritesScreen
import com.mhasancse15.iptv.presentation.home.HomeScreen
import com.mhasancse15.iptv.presentation.player.PlayerScreen
import com.mhasancse15.iptv.presentation.search.SearchScreen
import com.mhasancse15.iptv.presentation.settings.SettingsScreen

object NavGraph {
    const val HOME = "home"
    const val ALL_CHANNELS = "all_channels"
    const val FAVORITES = "favorites"
    const val SEARCH = "search"
    const val TV_GUIDE = "tv_guide"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PLAYER = "player"

    @Composable
    fun BdixNavigation(
        viewModel: BdixViewModel,
        navController: NavHostController = rememberNavController()
    ) {
        val channelState by viewModel.uiState.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val effectiveSearchQuery by viewModel.effectiveSearchQuery.collectAsState()
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        val selectedCountry by viewModel.selectedCountry.collectAsState()
        val epgData by viewModel.epgData.collectAsState()
        val epgError by viewModel.epgError.collectAsState()
        val themePreference by viewModel.themePreference.collectAsState()
        var selectedChannel by remember { mutableStateOf<Channel?>(null) }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        fun openChannel(channel: Channel) {
            selectedChannel = channel
            viewModel.markAsWatched(channel.id)
            navController.navigate(PLAYER)
        }

        fun changeChannel(step: Int) {
            val channels = (channelState as? com.mhasancse15.iptv.data.repository.ChannelUiState.Success)
                ?.allChannels.orEmpty()
            val index = channels.indexOfFirst { it.id == selectedChannel?.id }
            if (index >= 0 && channels.isNotEmpty()) {
                selectedChannel = channels[(index + step + channels.size) % channels.size]
                selectedChannel?.let { viewModel.markAsWatched(it.id) }
            }
        }

        val destinations = listOf(
            Triple(HOME, "Home", Icons.Default.Home),
            Triple(ALL_CHANNELS, "Channels", Icons.Default.LiveTv),
            Triple(FAVORITES, "Favorites", Icons.Default.Favorite),
            Triple(TV_GUIDE, "Guide", Icons.Default.Tv),
            Triple(SETTINGS, "Settings", Icons.Default.Settings)
        )

        Scaffold(
            bottomBar = {
                if (currentRoute != PLAYER) {
                    NavigationBar {
                        destinations.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                selected = currentRoute == route,
                                onClick = {
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HOME,
                modifier = Modifier.padding(padding)
            ) {
                composable(HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onChannelClick = { id ->
                            val channels = (channelState as? com.mhasancse15.iptv.data.repository.ChannelUiState.Success)
                                ?.allChannels.orEmpty()
                            channels.firstOrNull { it.id == id }?.let(::openChannel)
                        },
                        onFavoriteClick = viewModel::toggleFavorite,
                        onSearchClick = { navController.navigate(SEARCH) },
                        onSettingsClick = { navController.navigate(SETTINGS) },
                        onCategoryClick = { category ->
                            viewModel.selectCategory(category)
                            navController.navigate(ALL_CHANNELS)
                        },
                        onCountryClick = { country ->
                            viewModel.selectCountry(country)
                            navController.navigate(ALL_CHANNELS)
                        }
                    )
                }
                composable(ALL_CHANNELS) {
                    val channels = viewModel.getFilteredChannels(effectiveSearchQuery)
                    AllChannelsScreen(
                        channels = channels,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onChannelClick = ::openChannel,
                        onFavoriteClick = viewModel::toggleFavorite,
                        categories = viewModel.getCategoryList(),
                        countries = viewModel.getCountryList(),
                        selectedCategory = selectedCategory,
                        selectedCountry = selectedCountry,
                        onCategoryChange = viewModel::selectCategory,
                        onCountryChange = viewModel::selectCountry
                    )
                }
                composable(FAVORITES) {
                    val favorites = (channelState as? com.mhasancse15.iptv.data.repository.ChannelUiState.Success)
                        ?.favoriteChannels.orEmpty()
                    FavoritesScreen(
                        favoriteChannels = favorites,
                        onChannelClick = ::openChannel,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onClearAll = viewModel::clearFavorites
                    )
                }
                composable(SEARCH) {
                    SearchScreen(
                        channels = viewModel.getSearchResults(effectiveSearchQuery),
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onChannelClick = ::openChannel,
                        onFavoriteClick = viewModel::toggleFavorite
                    )
                }
                composable(TV_GUIDE) {
                    EpgScreen(
                        channels = (channelState as? com.mhasancse15.iptv.data.repository.ChannelUiState.Success)
                            ?.allChannels.orEmpty(),
                        epgData = epgData,
                        errorMessage = epgError
                    )
                }
                composable(SETTINGS) {
                    SettingsScreen(
                        onRefreshPlaylist = viewModel::refresh,
                        onClearCache = viewModel::clearCache,
                        onEpgRefresh = viewModel::refreshEpg,
                        onThemeChange = viewModel::setThemePreference,
                        themePreference = themePreference,
                        onAboutClick = { navController.navigate(ABOUT) }
                    )
                }
                composable(ABOUT) { AboutScreen() }
                composable(PLAYER) {
                    PlayerScreen(
                        channel = selectedChannel,
                        onBack = { navController.popBackStack() },
                        onPrevious = { changeChannel(-1) },
                        onNext = { changeChannel(1) }
                    )
                }
            }
        }
    }
}
