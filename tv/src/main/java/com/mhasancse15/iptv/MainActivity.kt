package com.mhasancse15.iptv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Date
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
    private var playerChannelKeyHandler: ((Int) -> Boolean)? = null

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
                var selectedIndex by remember { mutableIntStateOf(-1) }
                val selected = channels.getOrNull(selectedIndex)

                fun selectAdjacentChannel(offset: Int) {
                    val current = channels.getOrNull(selectedIndex) ?: return
                    val sectionChannels = channels.filter { it.homeSection() == current.homeSection() }
                    val sectionIndex = sectionChannels.indexOfFirst { it.id == current.id }
                    if (sectionIndex >= 0 && sectionChannels.isNotEmpty()) {
                        val next = sectionChannels[
                            (sectionIndex + offset + sectionChannels.size) % sectionChannels.size
                        ]
                        selectedIndex = channels.indexOfFirst { it.id == next.id }
                    }
                }

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

                SideEffect {
                    playerChannelKeyHandler = if (selectedIndex < 0) null else { keyCode ->
                        when (keyCode) {
                            KeyEvent.KEYCODE_CHANNEL_DOWN,
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                selectAdjacentChannel(-1)
                                true
                            }
                            KeyEvent.KEYCODE_CHANNEL_UP,
                            KeyEvent.KEYCODE_MEDIA_NEXT,
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                selectAdjacentChannel(1)
                                true
                            }
                            else -> false
                        }
                    }
                }

                BackHandler(selectedIndex >= 0) { selectedIndex = -1 }
                TvExperience(
                    channels = channels,
                    favorites = favorites,
                    selectedChannel = selected,
                    loading = loading,
                    error = error,
                    onRefresh = ::refresh,
                    onToggleFavorite = { channel ->
                        favorites = favorites.toMutableSet().apply {
                            if (!add(channel.id)) remove(channel.id)
                        }
                        getSharedPreferences("bdix_tv", MODE_PRIVATE).edit()
                            .putStringSet("favorite_ids", favorites).apply()
                    },
                    onSelectChannel = { channel ->
                        val index = channels.indexOf(channel)
                        if (index >= 0) selectedIndex = index
                    }
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && playerChannelKeyHandler?.invoke(event.keyCode) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        playerChannelKeyHandler = null
        super.onDestroy()
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
    onToggleFavorite: (TvChannel) -> Unit,
    onSelectChannel: (TvChannel) -> Unit
) {
    if (selectedChannel != null) {
        TvPlayer(
            channel = selectedChannel
        )
        return
    }

    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = android.text.format.DateFormat.getTimeFormat(context).format(Date())
            delay(60_000)
        }
    }
    val filtered = channels.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E131F))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 56.dp, top = 28.dp, end = 56.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(38.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "BDIX TV",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFDEE2F3),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "LIVE CHANNELS",
                            color = Color(0xFF4EDEA3),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search channels") },
                        singleLine = true,
                        modifier = Modifier.width(300.dp)
                    )
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh playlists",
                            tint = if (loading) Color(0xFF70788A) else Color(0xFFDEE2F3)
                        )
                    }
                    Text(
                        currentTime,
                        color = Color(0xFFDEE2F3),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            if (error != null) {
                item {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            if (loading && channels.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFADC6FF))
                    }
                }
            }
            val favoriteChannels = filtered.filter { it.id in favorites }
            if (favoriteChannels.isNotEmpty()) {
                item {
                    TvChannelRow("Favorites", favoriteChannels, favorites, onSelectChannel, onToggleFavorite)
                }
            }

            TvChannelSection.entries.forEach { section ->
                val sectionChannels = filtered.filter { it.homeSection() == section }
                if (sectionChannels.isNotEmpty()) {
                    item(key = section.title) {
                        TvChannelRow(section.title, sectionChannels, favorites, onSelectChannel, onToggleFavorite)
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
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp, bottom = 20.dp)
                .clip(CircleShape)
                .background(Color(0xE6161B28))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("▲ ▼ ◀ ▶  Navigate", color = Color(0xFFC2C6D6), style = MaterialTheme.typography.labelLarge)
            Text("•", color = Color(0xFF70788A))
            Text("OK  Select", color = Color(0xFFC2C6D6), style = MaterialTheme.typography.labelLarge)
            Text("•", color = Color(0xFF70788A))
            Text("BACK  Back", color = Color(0xFFC2C6D6), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private enum class TvChannelSection(val title: String) {
    BANGLADESH("Bangla TV Channels"),
    INDIAN_BANGLA("Indian Bangla TV Channels"),
    SPORTS("Sports & Global Networks"),
    OTHER("Other Channels")
}

private fun TvChannel.homeSection(): TvChannelSection {
    val searchable = "${category.lowercase()} ${name.lowercase()}"
    val indianBanglaBrands = listOf(
        "zee bangla", "star jalsha", "jalsha movies", "colors bangla", "sun bangla",
        "abp ananda", "aakash aath", "ruposhi bangla", "sangeet bangla", "kolkata tv",
        "khushboo bangla", "sananda tv", "r plus", "news18 bangla"
    )
    val sportsTerms = listOf(
        "sport", "cricket", "football", "soccer", "tennis", "olympic",
        "espn", "star sports", "sony sports", "ten sports", "willow"
    )
    val hasBanglaLabel = listOf("bangla", "bengali", "বাংলা").any(searchable::contains)
    val hasIndianLabel = listOf("india", "indian", "kolkata", "west bengal").any(searchable::contains)
    return when {
        indianBanglaBrands.any(searchable::contains) ||
            (hasBanglaLabel && hasIndianLabel) -> TvChannelSection.INDIAN_BANGLA
        hasBanglaLabel || searchable.contains("bangladesh") || searchable.contains("bangladeshi") ->
            TvChannelSection.BANGLADESH
        sportsTerms.any(searchable::contains) -> TvChannelSection.SPORTS
        else -> TvChannelSection.OTHER
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDEE2F3)
            )
            Text(
                "${channels.size} Live",
                modifier = Modifier
                    .padding(start = 14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252A37))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFADC6FF)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Scroll Right",
                color = Color(0xFFC2C6D6),
                style = MaterialTheme.typography.labelLarge
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFC2C6D6),
                modifier = Modifier.padding(start = 4.dp).size(18.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
    val accentColors = listOf(
        Color(0xFF123D4A), Color(0xFF3B263A), Color(0xFF213B35),
        Color(0xFF3D3122), Color(0xFF29354B), Color(0xFF3A2630)
    )
    val accent = accentColors[Math.floorMod(channel.name.hashCode(), accentColors.size)]
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(320.dp)
            .height(180.dp)
            .graphicsLayer {
                val scale = if (focused) 1.04f else 1f
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) Color(0xFFADC6FF) else Color(0xFF303542)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, focusedElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B28))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.32f),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.38f),
                            Color(0xB30E131F),
                            Color(0xF2090E1A)
                        )
                    )
                )
            )
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 136.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    channel.name.take(3).uppercase(),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFFADC6FF),
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFB4AB))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        "● LIVE",
                        color = Color(0xFF690005),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xB3090E1A))
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) Color(0xFFFFB4AB) else Color(0xFFDEE2F3)
                )
            }
            Text(
                text = channel.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xF2090E1A))
                        )
                    )
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp, top = 30.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (focused) Color.White else Color(0xFFDEE2F3)
            )
        }
    }
}

@Composable
private fun TvPlayer(
    channel: TvChannel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    var playbackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(channel.id) {
        playbackError = null
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
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
        Text(
            channel.name,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleLarge
        )
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
