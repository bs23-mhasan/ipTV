package com.mhasancse15.iptv.data.remote

import com.mhasancse15.iptv.data.parser.M3uParser
import com.mhasancse15.iptv.domain.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.util.concurrent.TimeUnit

@Singleton
class RemotePlaylistRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val mainPlaylistUrl = "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u"
    private val internationalPlaylistUrl = "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u"

    suspend fun fetchChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val mainChannels = fetchFromUrl(mainPlaylistUrl)
            val intlChannels = fetchFromUrl(internationalPlaylistUrl)
            (mainChannels + intlChannels).distinctBy { it.id }
        } catch (e: Exception) {
            emptyList<Channel>()
        }
    }

    private suspend fun fetchFromUrl(url: String): List<Channel> {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to fetch playlist: ${response.message}")
        }
        val content = response.body?.string() ?: ""
        return M3uParser.parse(content)
    }
}
