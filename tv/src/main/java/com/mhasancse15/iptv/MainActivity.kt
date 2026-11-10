package com.mhasancse15.iptv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mhasancse15.iptv.ui.theme.IPTVTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

private data class TvChannel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String,
    val category: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedFavorites = getSharedPreferences("bdix_tv", MODE_PRIVATE)
            .getStringSet("favorite_ids", emptySet()).orEmpty().toSet()
        val cachedM3u = getSharedPreferences("bdix_tv", MODE_PRIVATE).getString("playlist", null)
        setContent {
            IPTVTheme {
                var channels by remember { mutableStateOf(cachedM3u?.let(::parsePlaylist).orEmpty()) }
                var favorites by remember { mutableStateOf(savedFavorites) }
                var loading by remember { mutableStateOf(channels.isEmpty()) }
                var error by remember { mutableStateOf<String?>(null) }
                var selected by remember { mutableStateOf<TvChannel?>(null) }

                fun refresh() {
                    lifecycleScope.launch {
                        loading = true
                        error = null
                        try {
                            val playlist = withContext(Dispatchers.IO) {
                                listOf(
                                    "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u",
                                    "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u"
                                ).map { url ->
                                    val request = Request.Builder().url(url).build()
                                    client.newCall(request).execute().use { response ->
                                        if (!response.isSuccessful) throw IOException("Playlist request failed (${response.code})")
                                        response.body?.string().orEmpty()
                                    }
                                }.joinToString("\n")
                            }
                            val parsed = parsePlaylist(playlist)
                            if (parsed.isEmpty()) throw IOException("The playlists contained no valid channels")
                            channels = parsed
                            getSharedPreferences("bdix_tv", MODE_PRIVATE).edit()
                                .putString("playlist", playlist).apply()
                        } catch (failure: Exception) {
                            error = "Refresh failed: ${failure.message ?: "network error"}"
                        } finally {
                            loading = false
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    refresh()
                }

                BackHandler(selected != null) { selected = null }
                TvExperience(
                    channels = channels,
                    favorites = favorites,
                    selectedChannel = selected,
                    loading = loading,
                    error = error,
                    onRefresh = ::refresh,
                    onBackFromPlayer = { selected = null },
                    onToggleFavorite = { channel ->
                        favorites = favorites.toMutableSet().apply {
                            if (!add(channel.id)) remove(channel.id)
                        }
                        getSharedPreferences("bdix_tv", MODE_PRIVATE).edit()
                            .putStringSet("favorite_ids", favorites).apply()
                    },
                    onSelectChannel = { channel -> selected = channel }
                )
            }
        }
    }
}

@Composable
private fun TvExperience(
    channels: List<TvChannel>,
    favorites: Set<String>,
    selectedChannel: TvChannel?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onBackFromPlayer: () -> Unit,
    onToggleFavorite: (TvChannel) -> Unit,
    onSelectChannel: (TvChannel) -> Unit
) {
    if (selectedChannel != null) {
        TvPlayer(
            channel = selectedChannel,
            onBack = onBackFromPlayer,
            onPrevious = {
                val current = channels.indexOfFirst { it.id == selectedChannel.id }
                if (current > 0) onSelectChannel(channels[current - 1])
            },
            onNext = {
                val current = channels.indexOfFirst { it.id == selectedChannel.id }
                if (current in 0 until channels.lastIndex) onSelectChannel(channels[current + 1])
            }
        )
        return
    }

    var query by remember { mutableStateOf("") }
    val filtered = channels.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F)),
        contentPadding = PaddingValues(start = 52.dp, top = 36.dp, end = 52.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BDIX TV",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Live television for your screen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search channels") },
                    singleLine = true,
                    modifier = Modifier.width(330.dp)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh playlists")
                }
            }
        }
        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
        if (loading && channels.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            } }
        }
        val favoriteChannels = filtered.filter { it.id in favorites }
        if (favoriteChannels.isNotEmpty()) {
            item {
                TvChannelRow("Favorites", favoriteChannels, favorites, onSelectChannel, onToggleFavorite)
            }
        }
        val categories = filtered.map { it.category }.filter(String::isNotBlank).distinct().sorted()
        if (categories.isEmpty() && filtered.isNotEmpty()) {
            item { TvChannelRow("Channels", filtered, favorites, onSelectChannel, onToggleFavorite) }
        } else {
            categories.forEach { category ->
                item(key = category) {
                    TvChannelRow(
                        category,
                        filtered.filter { it.category == category },
                        favorites,
                        onSelectChannel,
                        onToggleFavorite
                    )
                }
            }
        }
        if (!loading && filtered.isEmpty()) {
            item {
                Text(
                    if (channels.isEmpty()) "No channels are available. Refresh to try again."
                    else "No channels match your search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun TvChannelRow(
    title: String,
    channels: List<TvChannel>,
    favorites: Set<String>,
    onSelectChannel: (TvChannel) -> Unit,
    onToggleFavorite: (TvChannel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(channels, key = { it.id }) { channel ->
                TvChannelCard(
                    channel = channel,
                    isFavorite = channel.id in favorites,
                    onClick = { onSelectChannel(channel) },
                    onFavoriteClick = { onToggleFavorite(channel) }
                )
            }
        }
    }
}

@Composable
private fun TvChannelCard(
    channel: TvChannel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(190.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        border = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181F))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize().padding(24.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        channel.name.take(3).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                channel.name,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Color(0xCC101014)).padding(horizontal = 12.dp, vertical = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TvPlayer(
    channel: TvChannel,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    var playbackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(channel.id) {
        playbackError = null
        exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Unable to play this stream: ${error.errorCodeName}"
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> Unit
            }
        }
        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        (context as? ComponentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            (context as? ComponentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.release()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Text(channel.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = onPrevious) { Text("Previous") }
            Button(onClick = onNext) { Text("Next") }
        }
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        playbackError?.let { message ->
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(12.dp))
                Button(onClick = {
                    playbackError = null
                    exoPlayer.prepare()
                    exoPlayer.play()
                }) { Text("Retry") }
            }
        }
    }
}

private fun parsePlaylist(content: String): List<TvChannel> {
    val channels = mutableListOf<TvChannel>()
    var metadata = ""
    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.startsWith("#EXTINF:", ignoreCase = true)) {
            metadata = line
        } else if (line.isNotEmpty() && !line.startsWith("#")) {
            val nameSeparator = metadata.indexOfDisplayNameSeparator()
            val name = if (nameSeparator >= 0) metadata.substring(nameSeparator + 1).trim() else ""
            val validUrl = try {
                val uri = java.net.URI(line)
                uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
            } catch (_: java.net.URISyntaxException) {
                false
            }
            if (name.isNotBlank() && validUrl) {
                fun attribute(key: String) =
                    Regex("""(?:^|\s)$key\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
                        .find(metadata)?.groupValues?.get(1).orEmpty()
                val tvgId = attribute("tvg-id")
                val category = attribute("group-title")
                val id = tvgId.ifBlank {
                    UUID.nameUUIDFromBytes("$name\n$line".toByteArray(Charsets.UTF_8)).toString()
                }
                channels += TvChannel(id, name, line, attribute("tvg-logo"), category)
            }
            metadata = ""
        }
    }
    return channels.distinctBy { it.id }
}

private fun String.indexOfDisplayNameSeparator(): Int {
    var insideQuotes = false
    forEachIndexed { index, char ->
        when (char) {
            '"' -> insideQuotes = !insideQuotes
            ',' -> if (!insideQuotes) return index
        }
    }
    return -1
}
