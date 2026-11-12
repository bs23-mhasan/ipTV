package com.mhasancse15.iptv

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mhasancse15.iptv.ui.theme.IPTVTheme
import com.mhasancse15.iptv.channels.PremiumChannelRepository
import com.mhasancse15.iptv.update.AppUpdateInfo
import com.mhasancse15.iptv.update.AppDownloadStatus
import com.mhasancse15.iptv.update.AppUpdateDownloader
import com.mhasancse15.iptv.update.AppUpdateRepository
import com.mhasancse15.iptv.update.UpdateDecision
import com.mhasancse15.iptv.update.evaluateUpdate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
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
    val category: String,
    val resolution: Int? = null
)

private fun com.mhasancse15.iptv.channels.PremiumChannel.toTvChannel() = TvChannel(
    id = id,
    name = name,
    url = url,
    logo = logo,
    category = category,
    resolution = resolution
)

private sealed interface TvUpdateDialogState {
    data class Available(
        val info: AppUpdateInfo,
        val installedVersionCode: Long,
        val mandatory: Boolean
    ) : TvUpdateDialogState

    data class UpToDate(val version: String) : TvUpdateDialogState
    data class Error(val message: String) : TvUpdateDialogState
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
        val preferences = getSharedPreferences("bdix_tv", MODE_PRIVATE)
        val cachedM3u = preferences.getString("playlist", null)
        val cachedPremiumJson = preferences.getString("premium_channels_json", null)
        setContent {
            IPTVTheme {
                var channels by remember { mutableStateOf(cachedM3u?.let(::parsePlaylist).orEmpty()) }
                var premiumChannels by remember {
                    mutableStateOf(
                        cachedPremiumJson?.let { cached ->
                            runCatching {
                                PremiumChannelRepository.parseChannels(cached).map { it.toTvChannel() }
                            }.getOrDefault(emptyList())
                        }.orEmpty()
                    )
                }
                var premiumError by remember { mutableStateOf<String?>(null) }
                var favorites by remember { mutableStateOf(savedFavorites) }
                var loading by remember {
                    mutableStateOf(channels.isEmpty() && premiumChannels.isEmpty())
                }
                var error by remember { mutableStateOf<String?>(null) }
                var selectedIndex by remember { mutableIntStateOf(-1) }
                var selectedChannelList by remember { mutableStateOf(emptyList<TvChannel>()) }
                var selectedChannelRow by remember { mutableStateOf<String?>(null) }
                val allChannels = channels + premiumChannels
                val selected = allChannels.getOrNull(selectedIndex)
                val premiumRepository = remember { PremiumChannelRepository(client) }

                fun selectAdjacentChannel(offset: Int) {
                    val sectionIndex = selectedChannelList.indexOfFirst { it.id == selected?.id }
                    if (sectionIndex >= 0 && selectedChannelList.isNotEmpty()) {
                        val next = selectedChannelList[
                            (sectionIndex + offset + selectedChannelList.size) % selectedChannelList.size
                        ]
                        selectedIndex = allChannels.indexOfFirst { it.id == next.id }
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
                            error = getString(
                                R.string.refresh_failed,
                                failure.message ?: getString(R.string.network_error)
                            )
                        } finally {
                            loading = false
                        }
                    }
                }

                fun refreshPremiumChannels() {
                    lifecycleScope.launch {
                        premiumError = null
                        try {
                            val json = premiumRepository.fetchChannelsJson()
                            val parsed = PremiumChannelRepository.parseChannels(json).map { it.toTvChannel() }
                            if (parsed.isEmpty()) throw IOException("JSON contained no valid premium channels")
                            premiumChannels = parsed
                            preferences.edit().putString("premium_channels_json", json).apply()
                        } catch (failure: Exception) {
                            premiumError = getString(
                                R.string.premium_refresh_failed,
                                failure.message ?: getString(R.string.network_error)
                            )
                        }
                    }
                }

                fun refreshAllChannels() {
                    refresh()
                    refreshPremiumChannels()
                }

                LaunchedEffect(Unit) {
                    refreshAllChannels()
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
                    channels = allChannels,
                    premiumChannels = premiumChannels,
                    premiumError = premiumError,
                    favorites = favorites,
                    selectedChannel = selected,
                    loading = loading,
                    error = error,
                    onRefresh = ::refreshAllChannels,
                    httpClient = client,
                    selectedChannelId = selected?.id,
                    selectedChannelRow = selectedChannelRow,
                    onSelectChannel = { channel, orderedChannels, rowKey ->
                        val index = allChannels.indexOfFirst { it.id == channel.id }
                        if (index >= 0) {
                            selectedChannelList = orderedChannels
                            selectedChannelRow = rowKey
                            selectedIndex = index
                        }
                    },
                    onToggleFavorite = { channel ->
                        favorites = favorites.toMutableSet().apply {
                            if (!add(channel.id)) remove(channel.id)
                        }
                        getSharedPreferences("bdix_tv", MODE_PRIVATE).edit()
                            .putStringSet("favorite_ids", favorites).apply()
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
    premiumChannels: List<TvChannel>,
    premiumError: String?,
    favorites: Set<String>,
    selectedChannel: TvChannel?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    httpClient: OkHttpClient,
    selectedChannelId: String?,
    selectedChannelRow: String?,
    onSelectChannel: (TvChannel, List<TvChannel>, String) -> Unit,
    onToggleFavorite: (TvChannel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchButtonFocused by remember { mutableStateOf(false) }
    var languageButtonFocused by remember { mutableStateOf(false) }
    var refreshButtonFocused by remember { mutableStateOf(false) }
    var restoreLanguageFocus by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val languageFocusRequester = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(Unit) {
        if (restoreLanguageFocus) {
            languageFocusRequester.requestFocus()
            restoreLanguageFocus = false
        }
    }
    BackHandler(enabled = searchActive && selectedChannel == null) {
        searchActive = false
    }
    val context = LocalContext.current
    val activeLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
        ?: context.resources.configuration.locales[0]?.language
    val updateScope = rememberCoroutineScope()
    val updateRepository = remember(httpClient) { AppUpdateRepository(httpClient) }
    val updateDownloader = remember(context) { AppUpdateDownloader(context) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var updateDialog by remember { mutableStateOf<TvUpdateDialogState?>(null) }
    var requiredUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var latestAvailableInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var latestInstalledVersionCode by remember { mutableStateOf<Long?>(null) }
    var activeDownloadVersionCode by remember { mutableStateOf<Long?>(null) }
    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }
    var completedDownload by remember { mutableStateOf<Pair<Long, Uri>?>(null) }
    val isDownloadingUpdate = downloadId != null
    val mandatoryUpdate = requiredUpdateInfo != null

    fun openInstaller(uri: Uri) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
            updateStatus = context.getString(R.string.opening_installer)
        } catch (failure: ActivityNotFoundException) {
            updateStatus = context.getString(R.string.installer_unavailable)
            updateDialog = TvUpdateDialogState.Error(updateStatus!!)
        } catch (failure: Exception) {
            updateStatus = context.getString(
                R.string.installer_open_failed,
                failure.message ?: context.getString(R.string.unknown_error)
            )
            updateDialog = TvUpdateDialogState.Error(updateStatus!!)
        }
    }

    val installSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val uri = pendingInstallUri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls() &&
            uri != null
        ) {
            pendingInstallUri = null
            openInstaller(uri)
        } else {
            updateStatus = context.getString(R.string.allow_installs_retry)
            if (!mandatoryUpdate) updateDialog = TvUpdateDialogState.Error(updateStatus!!)
        }
    }

    fun requestInstall(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallUri = uri
            updateStatus = context.getString(R.string.allow_installs_next)
            try {
                installSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } catch (failure: Exception) {
                updateStatus = context.getString(
                    R.string.install_settings_failed,
                    failure.message ?: context.getString(R.string.unknown_error)
                )
                updateDialog = TvUpdateDialogState.Error(updateStatus!!)
            }
        } else {
            openInstaller(uri)
        }
    }

    fun startApkDownload(info: AppUpdateInfo) {
        if (isDownloadingUpdate) return
        try {
            activeDownloadVersionCode = info.latestVersionCode
            downloadId = updateDownloader.enqueue(info)
            updateProgress = 0f
            updateStatus = context.getString(R.string.downloading_version, info.latestVersion)
            if (!mandatoryUpdate) updateDialog = null
        } catch (failure: Exception) {
            updateStatus = context.getString(
                R.string.download_start_failed,
                failure.message ?: context.getString(R.string.unknown_error)
            )
            updateDialog = TvUpdateDialogState.Error(updateStatus!!)
        }
    }

    fun checkForUpdates(showUpToDateDialog: Boolean = true) {
        if (checkingUpdate || isDownloadingUpdate) return
        checkingUpdate = true
        updateStatus = context.getString(R.string.checking_updates)
        updateDialog = null
        latestAvailableInfo = null
        updateScope.launch {
            try {
                val info = updateRepository.fetchLatest()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                updateDialog = when (val decision = evaluateUpdate(installedVersionCode, info)) {
                    is UpdateDecision.UpToDate -> {
                        requiredUpdateInfo = null
                        latestAvailableInfo = null
                        latestInstalledVersionCode = null
                        val installedVersion = packageInfo.versionName ?: "installed"
                        updateStatus = context.getString(R.string.latest_version_status, installedVersion)
                        if (showUpToDateDialog) TvUpdateDialogState.UpToDate(installedVersion) else null
                    }
                    is UpdateDecision.Available -> {
                        requiredUpdateInfo = decision.info.takeIf { decision.mandatory }
                        latestAvailableInfo = decision.info
                        latestInstalledVersionCode = installedVersionCode
                        updateStatus = context.getString(
                            R.string.version_available_status,
                            decision.info.latestVersion
                        )
                        val availableDialog = TvUpdateDialogState.Available(
                            info = decision.info,
                            installedVersionCode = installedVersionCode,
                            mandatory = decision.mandatory
                        )
                        if (showUpToDateDialog || decision.mandatory) availableDialog else null
                    }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                val message = context.getString(
                    R.string.update_check_failed,
                    failure.message ?: context.getString(R.string.network_error)
                )
                updateStatus = message
                updateDialog = TvUpdateDialogState.Error(message)
            } finally {
                checkingUpdate = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates(showUpToDateDialog = false)
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        while (true) {
            val completed = try {
                when (val status = updateDownloader.status(id)) {
                    AppDownloadStatus.Missing -> {
                        updateStatus = context.getString(R.string.download_not_found)
                        updateDialog = TvUpdateDialogState.Error(updateStatus!!)
                        downloadId = null
                        activeDownloadVersionCode = null
                        true
                    }
                    AppDownloadStatus.Complete -> {
                        val uri = updateDownloader.downloadedUri(id)
                        if (uri == null) {
                            updateStatus = context.getString(R.string.download_open_failed)
                            updateDialog = TvUpdateDialogState.Error(updateStatus!!)
                        } else {
                            val versionCode = activeDownloadVersionCode
                            if (versionCode == null) {
                                updateStatus = context.getString(R.string.download_version_unverified)
                                updateDialog = TvUpdateDialogState.Error(updateStatus!!)
                            } else {
                                completedDownload = versionCode to uri
                                updateStatus = context.getString(R.string.download_complete)
                                requestInstall(uri)
                            }
                        }
                        activeDownloadVersionCode = null
                        downloadId = null
                        true
                    }
                    is AppDownloadStatus.Failed -> {
                        updateStatus = context.getString(R.string.apk_download_failed, status.reason)
                        updateDialog = TvUpdateDialogState.Error(updateStatus!!)
                        downloadId = null
                        activeDownloadVersionCode = null
                        true
                    }
                    is AppDownloadStatus.Running -> {
                        if (status.progress != null) {
                            updateProgress = status.progress
                            updateStatus = context.getString(
                                R.string.downloading_percent,
                                (updateProgress * 100).toInt()
                            )
                        } else {
                            updateStatus = context.getString(R.string.downloading_version_unknown)
                        }
                        false
                    }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                updateStatus = context.getString(
                    R.string.download_progress_failed,
                    failure.message ?: context.getString(R.string.unknown_error)
                )
                updateDialog = TvUpdateDialogState.Error(updateStatus!!)
                downloadId = null
                activeDownloadVersionCode = null
                return@LaunchedEffect
            }
            if (completed) return@LaunchedEffect
            delay(500)
        }
    }

    BackHandler(enabled = mandatoryUpdate) {}

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
    val filteredPremiumChannels = premiumChannels.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0E131F))
                .alpha(if (selectedChannel == null) 1f else 0f)
        ) {
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
                            stringResource(R.string.live_channels),
                            color = Color(0xFF4EDEA3),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (searchActive) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search_channels)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { searchActive = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close_search)
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .width(340.dp)
                                .focusRequester(searchFocusRequester)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .width(300.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1F2C))
                                .border(
                                    width = if (searchButtonFocused) 2.dp else 1.dp,
                                    color = if (searchButtonFocused) Color(0xFFADC6FF) else Color(0xFF424754),
                                    shape = CircleShape
                                )
                                .onFocusChanged { searchButtonFocused = it.isFocused }
                                .clickable { searchActive = true }
                                .padding(horizontal = 18.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFADC6FF)
                            )
                            Text(
                                if (query.isBlank()) stringResource(R.string.search_channels) else query,
                                color = if (query.isBlank()) Color(0xFFC2C6D6) else Color(0xFFDEE2F3),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .focusRequester(languageFocusRequester)
                            .onFocusChanged { languageButtonFocused = it.isFocused }
                            .border(
                                width = if (languageButtonFocused) 2.dp else 1.dp,
                                color = if (languageButtonFocused) {
                                    Color(0xFFADC6FF)
                                } else {
                                    Color(0xFF424754)
                                },
                                shape = RoundedCornerShape(50)
                            ),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFADC6FF)
                        ),
                        onClick = {
                            val targetLanguage = if (activeLanguage == "bn") "en" else "bn"
                            restoreLanguageFocus = true
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(targetLanguage)
                            )
                        }
                    ) {
                        Text(stringResource(R.string.language_switch))
                    }
                    IconButton(
                        onClick = { if (!loading) onRefresh() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (refreshButtonFocused) Color(0xFF252A37) else Color.Transparent
                            )
                            .border(
                                width = if (refreshButtonFocused) 2.dp else 1.dp,
                                color = if (refreshButtonFocused) {
                                    Color(0xFFADC6FF)
                                } else {
                                    Color(0xFF424754)
                                },
                                shape = CircleShape
                            )
                            .onFocusChanged { refreshButtonFocused = it.isFocused }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_playlists),
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
                if (premiumError != null) {
                    item {
                        Text(
                            premiumError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            if (latestAvailableInfo != null) {
                item(key = "tv_update_available") {
                    TvUpdateCard(
                        version = latestAvailableInfo!!.latestVersion,
                        downloading = isDownloadingUpdate,
                        progress = updateProgress,
                        status = updateStatus,
                        onClick = {
                            val info = latestAvailableInfo
                            val installedVersionCode = latestInstalledVersionCode
                            if (info != null && installedVersionCode != null) {
                                updateDialog = TvUpdateDialogState.Available(
                                    info = info,
                                    installedVersionCode = installedVersionCode,
                                    mandatory = requiredUpdateInfo != null
                                )
                            }
                        }
                    )
                }
            }
            val favoriteChannels = filtered.filter { it.id in favorites }
            if (favoriteChannels.isNotEmpty()) {
                item {
                    TvChannelRow(
                        title = stringResource(R.string.favorites),
                        channels = favoriteChannels,
                        favorites = favorites,
                        selectedChannelId = selectedChannelId,
                        selectedChannelRow = selectedChannelRow,
                        onSelectChannel = onSelectChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
            if (filteredPremiumChannels.isNotEmpty()) {
                item(key = "premium_channel_section") {
                    TvChannelRow(
                        title = stringResource(R.string.premium_channels),
                        channels = filteredPremiumChannels,
                        favorites = favorites,
                        selectedChannelId = selectedChannelId,
                        selectedChannelRow = selectedChannelRow,
                        onSelectChannel = onSelectChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
            val premiumIds = premiumChannels.mapTo(mutableSetOf()) { it.id }
            TvChannelSection.entries.forEach { section ->
                val sectionChannels = filtered.filter {
                    it.id !in premiumIds &&
                        it.homeSection() == section &&
                        (section !in TvChannelSection.hdOnlySections ||
                            (it.resolution ?: 0) >= 720)
                }
                if (sectionChannels.isNotEmpty()) {
                    item(key = section.name) {
                        TvChannelRow(
                            title = stringResource(section.titleRes),
                            channels = sectionChannels,
                            favorites = favorites,
                            selectedChannelId = selectedChannelId,
                            selectedChannelRow = selectedChannelRow,
                            onSelectChannel = onSelectChannel,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
            if (!loading && filtered.isEmpty() && filteredPremiumChannels.isEmpty()) {
                item {
                    Text(
                        if (channels.isEmpty() && premiumChannels.isEmpty()) {
                            stringResource(R.string.no_channels_available)
                        }
                        else stringResource(R.string.no_search_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        if (loading && channels.isEmpty()) {
            LoadingGif(Modifier.fillMaxSize())
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
            Text(
                "▲ ▼ ◀ ▶  ${stringResource(R.string.navigate)}",
                color = Color(0xFFC2C6D6),
                style = MaterialTheme.typography.labelLarge
            )
            Text("•", color = Color(0xFF70788A))
            Text(
                stringResource(R.string.remote_select, stringResource(R.string.select)),
                color = Color(0xFFC2C6D6),
                style = MaterialTheme.typography.labelLarge
            )
            Text("•", color = Color(0xFF70788A))
            Text(
                stringResource(R.string.remote_back, stringResource(R.string.back)),
                color = Color(0xFFC2C6D6),
                style = MaterialTheme.typography.labelLarge
            )
        }
        }
        updateDialog?.let { dialog ->
            AlertDialog(
                onDismissRequest = {
                    if (!mandatoryUpdate && !isDownloadingUpdate) updateDialog = null
                },
                title = {
                    Text(
                        when (dialog) {
                            is TvUpdateDialogState.Available ->
                                stringResource(
                                    if (dialog.mandatory) R.string.required_update
                                    else R.string.update_available
                                )
                            is TvUpdateDialogState.UpToDate -> stringResource(R.string.app_up_to_date)
                            is TvUpdateDialogState.Error -> stringResource(R.string.update_problem)
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (dialog) {
                            is TvUpdateDialogState.Available -> {
                                Text(
                                    stringResource(
                                        R.string.update_version_details,
                                        dialog.info.latestVersion,
                                        dialog.installedVersionCode
                                    )
                                )
                                if (dialog.info.releaseNotes.isNotEmpty()) {
                                    Text(
                                        stringResource(
                                            R.string.whats_new,
                                            dialog.info.releaseNotes.joinToString("\n") { "• $it" }
                                        )
                                    )
                                }
                                if (dialog.mandatory) {
                                    Text(stringResource(R.string.required_update_message))
                                }
                                if (isDownloadingUpdate) {
                                    Text(updateStatus ?: stringResource(R.string.downloading_update))
                                    CircularProgressIndicator(
                                        progress = { updateProgress },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is TvUpdateDialogState.UpToDate ->
                                Text(stringResource(R.string.up_to_date_version, dialog.version))
                            is TvUpdateDialogState.Error -> {
                                Text(
                                    if (mandatoryUpdate && isDownloadingUpdate) {
                                        updateStatus ?: stringResource(R.string.retrying_required_update)
                                    } else {
                                        dialog.message
                                    }
                                )
                                if (mandatoryUpdate && isDownloadingUpdate) {
                                    CircularProgressIndicator(
                                        progress = { updateProgress },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    when (dialog) {
                        is TvUpdateDialogState.Available -> {
                            if (!isDownloadingUpdate) {
                                TextButton(onClick = {
                                    val downloadedUpdate = completedDownload
                                    if (downloadedUpdate?.first == dialog.info.latestVersionCode) {
                                        requestInstall(downloadedUpdate.second)
                                    }
                                    else startApkDownload(dialog.info)
                                }) {
                                    Text(stringResource(R.string.update_now))
                                }
                            }
                        }
                        is TvUpdateDialogState.UpToDate -> {
                            TextButton(onClick = { updateDialog = null }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                        is TvUpdateDialogState.Error -> {
                            if (!isDownloadingUpdate) {
                                TextButton(onClick = {
                                    val requiredInfo = requiredUpdateInfo
                                    val downloadedUpdate = completedDownload
                                    val latestInfo = latestAvailableInfo
                                    if (downloadedUpdate != null &&
                                        latestInfo?.latestVersionCode == downloadedUpdate.first
                                    ) {
                                        requestInstall(downloadedUpdate.second)
                                    } else if (requiredInfo != null) {
                                        startApkDownload(requiredInfo)
                                    } else {
                                        updateDialog = null
                                        checkForUpdates()
                                    }
                                }) {
                                    Text(
                                        stringResource(
                                            if (mandatoryUpdate) R.string.retry_update else R.string.retry
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    when (dialog) {
                        is TvUpdateDialogState.Available -> {
                            if (!dialog.mandatory && !isDownloadingUpdate) {
                                TextButton(onClick = { updateDialog = null }) {
                                    Text(stringResource(R.string.later))
                                }
                            }
                        }
                        is TvUpdateDialogState.Error -> {
                            if (!mandatoryUpdate && !isDownloadingUpdate) {
                                TextButton(onClick = { updateDialog = null }) {
                                    Text(stringResource(R.string.close))
                                }
                            }
                        }
                        is TvUpdateDialogState.UpToDate -> Unit
                    }
                }
            )
        }
        selectedChannel?.let { TvPlayer(channel = it) }
    }
}

@Composable
private fun TvUpdateCard(
    version: String,
    downloading: Boolean,
    progress: Float,
    status: String?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val pulse by rememberInfiniteTransition(label = "update-button-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "update-button-scale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (focused) 1.04f else pulse
                scaleY = if (focused) 1.04f else pulse
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = !downloading, onClick = onClick)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color(0xFFADC6FF) else Color(0xFF424754),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFFADC6FF),
                    trackColor = Color(0xFF424754)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.update_available_heading),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDEE2F3)
                )
                Text(
                    status ?: stringResource(R.string.version_ready, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC2C6D6)
                )
            }
            if (downloading) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFADC6FF)
                )
            }
        }
    }
}

private enum class TvChannelSection(@param:StringRes val titleRes: Int) {
    BANGLADESH(R.string.section_bangladesh),
    INDIAN_BANGLA(R.string.section_indian_bangla),
    INDIAN_MOVIES(R.string.section_indian_movies),
    INDIAN_MUSIC(R.string.section_indian_music),
    INDIAN_ENTERTAINMENT(R.string.section_indian_entertainment),
    PAKISTANI(R.string.section_pakistani),
    INTERNATIONAL(R.string.section_international),
    DOCUMENTARY_WILDLIFE(R.string.section_documentary_wildlife),
    KIDS(R.string.section_kids),
    RELIGIOUS(R.string.section_religious),
    SPORTS(R.string.section_sports),
    OTHER(R.string.section_other);

    companion object {
        val hdOnlySections = setOf(BANGLADESH, INDIAN_BANGLA, INDIAN_MOVIES)
    }
}

private fun TvChannel.homeSection(): TvChannelSection {
    val searchable = "${category.lowercase()} ${name.lowercase()}"
    val normalizedCategory = category.trim().lowercase()
        .replace("&", "and")
        .replace(Regex("\\s+"), " ")
    when (normalizedCategory) {
        "bangladesh", "bangladeshi" -> return TvChannelSection.BANGLADESH
        "indian bangla", "indian bengali" -> return TvChannelSection.INDIAN_BANGLA
        "indian movies", "indian movie" -> return TvChannelSection.INDIAN_MOVIES
        "indian music" -> return TvChannelSection.INDIAN_MUSIC
        "indian entertainment" -> return TvChannelSection.INDIAN_ENTERTAINMENT
        "pakistani", "pakistan" -> return TvChannelSection.PAKISTANI
        "international" -> return TvChannelSection.INTERNATIONAL
        "documentary and wildlife", "documentary", "wildlife" ->
            return TvChannelSection.DOCUMENTARY_WILDLIFE
        "kids", "children" -> return TvChannelSection.KIDS
        "religious", "religion" -> return TvChannelSection.RELIGIOUS
        "sports", "sport" -> return TvChannelSection.SPORTS
    }
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
        searchable.contains("pakistan") || searchable.contains("pakistani") ->
            TvChannelSection.PAKISTANI
        else -> TvChannelSection.OTHER
    }
}

@Composable
private fun TvChannelRow(
    title: String,
    channels: List<TvChannel>,
    favorites: Set<String>,
    selectedChannelId: String?,
    selectedChannelRow: String?,
    onSelectChannel: (TvChannel, List<TvChannel>, String) -> Unit,
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
                stringResource(R.string.live_count, channels.size),
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
                stringResource(R.string.scroll_right),
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
                    restoreFocus = selectedChannelId == channel.id && selectedChannelRow == title,
                    onClick = { onSelectChannel(channel, channels, title) },
                    onFavoriteClick = { onToggleFavorite(channel) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TvChannelCard(
    channel: TvChannel,
    isFavorite: Boolean,
    restoreFocus: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var favoriteMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember(channel.id) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) focusRequester.requestFocus()
    }
    val accentColors = listOf(
        Color(0xFF123D4A), Color(0xFF3B263A), Color(0xFF213B35),
        Color(0xFF3D3122), Color(0xFF29354B), Color(0xFF3A2630)
    )
    val accent = accentColors[Math.floorMod(channel.name.hashCode(), accentColors.size)]
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(180.dp)
            .graphicsLayer {
                val scale = if (focused) 1.04f else 1f
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) Color(0xFFADC6FF) else Color(0xFF303542)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, focusedElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B28))
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { favoriteMenuExpanded = true }
                    )
            ) {
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
                            stringResource(R.string.live),
                            color = Color(0xFF690005),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
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
            DropdownMenu(
                expanded = favoriteMenuExpanded,
                onDismissRequest = { favoriteMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isFavorite) R.string.remove_favorite else R.string.add_favorite
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        favoriteMenuExpanded = false
                        onFavoriteClick()
                    }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun TvPlayer(
    channel: TvChannel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                false
            )
            .build()
    }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var hasPlaybackStarted by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(channel.id) {
        playerView?.hideController()
        hasPlaybackStarted = false
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
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackStarted = true
                }
            }

            override fun onRenderedFirstFrame() {
                hasPlaybackStarted = true
            }

            override fun onPlayerError(error: PlaybackException) {
                hasPlaybackStarted = false
                playbackError = context.getString(R.string.stream_play_error, error.errorCodeName)
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
        val activity = context as? ComponentActivity
        val window = activity?.window
        val insetsController = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.release()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = true
                    setControllerAutoShow(false)
                    isLongClickable = true
                    setOnLongClickListener {
                        showController()
                        true
                    }
                    hideController()
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!hasPlaybackStarted && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                CenteredChannelLoader(
                    channelName = channel.name,
                    logoUrl = channel.logo
                )
            }
        }
        playbackError?.let { message ->
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(message, color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    playbackError = null
                    hasPlaybackStarted = false
                    exoPlayer.prepare()
                    exoPlayer.play()
                }) { Text(stringResource(R.string.retry)) }
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
                val resolution = Regex("""(?<!\d)(\d{3,4})\s*p\b""", RegexOption.IGNORE_CASE)
                    .find(name)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                channels += TvChannel(
                    id = id,
                    name = name,
                    url = line,
                    logo = attribute("tvg-logo"),
                    category = category,
                    resolution = resolution
                )
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
