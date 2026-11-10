package com.mhasancse15.iptv.data.remote

import com.mhasancse15.iptv.data.local.FavoritesStore
import com.mhasancse15.iptv.data.parser.XmltvParser
import com.mhasancse15.iptv.domain.model.EpgProgrammeWithChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEpgRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val favoritesStore: FavoritesStore
) {
    private val epgUrl = "https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/epg/epg.xml"

    suspend fun refresh(): List<EpgProgrammeWithChannel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(epgUrl).build()
        val xml = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("EPG request failed (${response.code})")
            response.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("EPG response was empty")
        }
        val programmes = parse(xml)
        if (programmes.isEmpty()) throw IOException("EPG contained no valid programmes")
        favoritesStore.cacheEpg(xml)
        programmes
    }

    suspend fun cached(): List<EpgProgrammeWithChannel> = withContext(Dispatchers.IO) {
        val xml = favoritesStore.cachedEpg()
        if (xml.isNullOrBlank()) emptyList() else parse(xml)
    }

    private fun parse(xml: String): List<EpgProgrammeWithChannel> {
        val (channels, programmes) = XmltvParser.parse(xml)
        val channelsById = channels.associateBy { it.id }
        return programmes
            .filter { it.start > 0L && it.stop > it.start }
            .map { programme ->
                EpgProgrammeWithChannel(
                    programme = programme,
                    channel = channelsById[programme.channelId]
                )
            }
    }
}
