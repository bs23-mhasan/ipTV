package com.mhasancse15.iptv.data.remote

import com.mhasancse15.iptv.data.parser.M3uParser
import com.mhasancse15.iptv.domain.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

@Singleton
class RemotePlaylistRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    val playlistUrls = listOf(
        "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u",
        "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u"
    )

    suspend fun fetchChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val results = playlistUrls.map { url ->
            async {
                try {
                    Result.success(fetchFromUrl(url))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Result.failure<List<Channel>>(error)
                }
            }
        }.awaitAll()
        val channels = results.filter { it.isSuccess }.flatMap { it.getOrThrow() }
        if (channels.isEmpty()) {
            val errors = results.mapNotNull { it.exceptionOrNull()?.message }
            throw IOException("Unable to load any playlist source: ${errors.joinToString("; ")}")
        }
        channels.distinctBy { it.id }
    }

    private suspend fun fetchFromUrl(url: String): List<Channel> {
        val request = Request.Builder().url(url).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Playlist request failed (${response.code})")
            }
            val content = response.body?.string()
                ?: throw IOException("Playlist response was empty")
            val channels = M3uParser.parse(content)
            if (channels.isEmpty()) throw IOException("Playlist contained no valid channels")
            channels
        }
    }
}
