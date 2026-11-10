package com.mhasancse15.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mhasancse15.iptv.domain.model.Channel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "iptv_preferences")

@Singleton
class FavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val legacyFavoritesKey = stringPreferencesKey("favorite_channel_ids")
    private val favoritesKey = stringSetPreferencesKey("favorite_channel_ids_set")
    private val cachedChannelsKey = stringPreferencesKey("cached_channels")
    private val recentChannelsKey = stringPreferencesKey("recent_channels")
    private val themePreferenceKey = stringPreferencesKey("theme_preference")
    private val epgCacheFile get() = java.io.File(context.filesDir, "cached_epg.xml")
    private val epgTempFile get() = java.io.File(context.filesDir, "cached_epg.xml.tmp")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[favoritesKey]
            ?: preferences[legacyFavoritesKey]
                ?.split(',')
                ?.filter(String::isNotBlank)
                ?.toSet()
            ?: emptySet()
    }

    val cachedChannels: Flow<List<Channel>> = context.dataStore.data.map { preferences ->
        decodeChannels(preferences[cachedChannelsKey])
    }

    val recentWatched: Flow<Map<String, Long>> = context.dataStore.data.map { preferences ->
        decodeRecent(preferences[recentChannelsKey])
    }
    val themePreference: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[themePreferenceKey] ?: "System"
    }

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { preferences ->
            val favorites = preferences[favoritesKey]?.toMutableSet()
                ?: preferences[legacyFavoritesKey]
                    ?.split(',')
                    ?.filter(String::isNotBlank)
                    ?.toMutableSet()
                ?: mutableSetOf()
            if (!favorites.add(channelId)) favorites.remove(channelId)
            preferences[favoritesKey] = favorites
            preferences.remove(legacyFavoritesKey)
        }
    }

    suspend fun clearFavorites() {
        context.dataStore.edit { preferences ->
            preferences[favoritesKey] = emptySet()
            preferences.remove(legacyFavoritesKey)
        }
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences -> preferences[themePreferenceKey] = theme }
    }

    suspend fun cacheChannels(channels: List<Channel>) {
        context.dataStore.edit { preferences ->
            preferences[cachedChannelsKey] = encodeChannels(channels)
        }
    }

    suspend fun clearCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(cachedChannelsKey)
        }
        withContext(Dispatchers.IO) {
            epgCacheFile.delete()
            epgTempFile.delete()
        }
    }

    suspend fun cacheEpg(xml: String) {
        withContext(Dispatchers.IO) {
            epgTempFile.writeText(xml)
            if (!epgTempFile.renameTo(epgCacheFile)) {
                epgTempFile.delete()
                throw java.io.IOException("Unable to persist the EPG cache")
            }
        }
    }

    suspend fun cachedEpg(): String? = withContext(Dispatchers.IO) {
        epgCacheFile.takeIf { it.isFile }?.readText()
    }

    suspend fun markWatched(channelId: String) {
        context.dataStore.edit { preferences ->
            val recentlyWatched = decodeRecent(preferences[recentChannelsKey]).toMutableMap()
            recentlyWatched[channelId] = System.currentTimeMillis()
            preferences[recentChannelsKey] = encodeRecent(recentlyWatched)
        }
    }

    private fun encodeChannels(channels: List<Channel>): String =
        JSONArray().apply {
            channels.forEach { channel ->
                put(
                    JSONObject()
                        .put("id", channel.id)
                        .put("name", channel.name)
                        .put("streamUrl", channel.streamUrl)
                        .put("logoUrl", channel.logoUrl)
                        .put("category", channel.category)
                        .put("country", channel.country)
                        .put("tvgId", channel.tvgId)
                        .put("tvgName", channel.tvgName)
                )
            }
        }.toString()

    private fun decodeChannels(serialized: String?): List<Channel> {
        if (serialized.isNullOrBlank()) return emptyList()
        val json = JSONArray(serialized)
        return (0 until json.length()).mapNotNull { index ->
            val item = json.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            val name = item.optString("name")
            val url = item.optString("streamUrl")
            if (id.isBlank() || name.isBlank() || url.isBlank()) return@mapNotNull null
            Channel(
                id = id,
                name = name,
                streamUrl = url,
                logoUrl = item.optString("logoUrl"),
                category = item.optString("category"),
                country = item.optString("country"),
                tvgId = item.optString("tvgId"),
                tvgName = item.optString("tvgName")
            )
        }
    }

    private fun encodeRecent(recent: Map<String, Long>): String =
        JSONArray().apply {
            recent.forEach { (id, timestamp) ->
                put(JSONObject().put("id", id).put("timestamp", timestamp))
            }
        }.toString()

    private fun decodeRecent(serialized: String?): Map<String, Long> {
        if (serialized.isNullOrBlank()) return emptyMap()
        val json = JSONArray(serialized)
        return (0 until json.length()).mapNotNull { index ->
            val item = json.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank()) null else id to item.optLong("timestamp")
        }.toMap()
    }
}
