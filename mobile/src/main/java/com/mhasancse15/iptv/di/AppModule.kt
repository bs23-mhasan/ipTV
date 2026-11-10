package com.mhasancse15.iptv.di

import android.content.Context
import com.mhasancse15.iptv.data.local.FavoritesStore
import com.mhasancse15.iptv.data.remote.RemotePlaylistRepository
import com.mhasancse15.iptv.data.repository.ChannelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30L, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30L, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30L, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRemotePlaylistRepository(client: OkHttpClient): RemotePlaylistRepository {
        return RemotePlaylistRepository(client)
    }

    @Provides
    @Singleton
    fun provideFavoritesStore(@ApplicationContext context: Context): FavoritesStore {
        return FavoritesStore(context)
    }

    @Provides
    @Singleton
    fun provideChannelRepository(
        remote: RemotePlaylistRepository,
        favorites: FavoritesStore
    ): ChannelRepository {
        return ChannelRepository(remote, favorites)
    }
}
