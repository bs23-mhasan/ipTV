package com.mhasancse15.iptv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavHostController
import com.mhasancse15.iptv.domain.model.Channel
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
    fun NavGraph(
        navController: NavHostController,
        modifier: Modifier = Modifier,
        onChannelClick: (Channel) -> Unit,
        onFavoriteClick: (String) -> Unit,
        onPreviousChannel: (Channel) -> Unit,
        onNextChannel: (Channel) -> Unit
    ) {
        NavHost(
            navController = navController,
            startDestination = HOME,
            modifier = modifier
        ) {
            composable(HOME) {
                HomeScreen(
                    onChannelClick = { id ->
                        // Find channel and navigate to player
                        onChannelClick(Channel(id = id, name = "", streamUrl = ""))
                    },
                    onFavoriteClick = onFavoriteClick,
                    onSearchClick = { navController.navigate(SEARCH) },
                    onSettingsClick = { navController.navigate(SETTINGS) },
                    onCategoryClick = { navController.navigate(ALL_CHANNELS) },
                    onCountryClick = { navController.navigate(ALL_CHANNELS) }
                )
            }
            composable(ALL_CHANNELS) {
                AllChannelsScreen(
                    channels = emptyList(),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onChannelClick = { channel -> onChannelClick(channel) },
                    onFavoriteClick = onFavoriteClick
                )
            }
            composable(FAVORITES) {
                FavoritesScreen(
                    favoriteChannels = emptyList(),
                    onChannelClick = { channel -> onChannelClick(channel) },
                    onFavoriteClick = onFavoriteClick,
                    onClearAll = {}
                )
            }
            composable(SEARCH) {
                SearchScreen(
                    channels = emptyList(),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onChannelClick = { channel -> onChannelClick(channel) },
                    onFavoriteClick = onFavoriteClick
                )
            }
            composable(TV_GUIDE) {
                EpgScreen(
                    channels = emptyList(),
                    epgData = emptyList()
                )
            }
            composable(SETTINGS) {
                SettingsScreen(
                    onRefreshPlaylist = {},
                    onClearCache = {},
                    onEpgRefresh = {},
                    onThemeChange = {}
                )
            }
            composable(ABOUT) {
                AboutScreen()
            }
            composable(PLAYER) {
                PlayerScreen(
                    channel = null,
                    onBack = { navController.popBackStack() },
                    onPrevious = { onPreviousChannel(Channel(id = "", name = "", streamUrl = "")) },
                    onNext = { onNextChannel(Channel(id = "", name = "", streamUrl = "")) }
                )
            }
        }
    }
}