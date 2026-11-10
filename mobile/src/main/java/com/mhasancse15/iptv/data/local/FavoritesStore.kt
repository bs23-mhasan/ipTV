package com.mhasancse15.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "iptv_preferences")

@Singleton
class FavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val FAVORITES_KEY = stringPreferencesKey("favorite_channel_ids")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val serialized = preferences[FAVORITES_KEY] ?: ""
            if (serialized.isEmpty()) emptySet()
            else serialized.split(",").toSet()
        }

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split(",").toMutableSet()
            if (set.contains(channelId)) {
                set.remove(channelId)
            } else {
                set.add(channelId)
            }
            preferences[FAVORITES_KEY] = set.joinToString(",")
        }
    }

    suspend fun setFavorite(channelId: String, isFavorite: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split(",").toMutableSet()
            if (isFavorite) {
                set.add(channelId)
            } else {
                set.remove(channelId)
            }
            preferences[FAVORITES_KEY] = set.joinToString(",")
        }
    }
}
