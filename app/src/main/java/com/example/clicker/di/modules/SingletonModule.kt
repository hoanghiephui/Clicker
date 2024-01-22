package com.example.clicker.di.modules

import android.content.Context
import com.example.clicker.data.TokenDataStore
import com.example.clicker.domain.TwitchDataStore
import com.example.clicker.network.clients.TwitchAuthenticationClient
import com.example.clicker.network.clients.TwitchClient
import com.example.clicker.network.domain.TwitchAuthentication
import com.example.clicker.network.domain.TwitchRepo
import com.example.clicker.network.domain.TwitchStream
import com.example.clicker.network.repository.TwitchRepoImpl
import com.example.clicker.network.websockets.ParsingEngine
import com.example.clicker.network.websockets.TwitchWebSocket
import com.example.clicker.network.domain.TwitchSocket
import com.example.clicker.network.interceptors.LiveNetworkMonitor
import com.example.clicker.network.interceptors.LoggingInterceptor
import com.example.clicker.network.interceptors.NetworkMonitor
import com.example.clicker.network.interceptors.NetworkMonitorInterceptor
import com.example.clicker.network.repository.TwitchAuthenticationImpl
import com.example.clicker.network.repository.TwitchStreamImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {

    @Provides
    fun provideNetworkMonitor(
        @ApplicationContext appContext: Context
    ): NetworkMonitor{
        return LiveNetworkMonitor(appContext)
    }

    @Singleton //scope binding
    @Provides
    fun providesTwitchClient(
        liveNetworkMonitor: NetworkMonitor
    ): TwitchClient {
         val monitorClient = OkHttpClient.Builder()
            .addInterceptor(NetworkMonitorInterceptor(liveNetworkMonitor))
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.twitch.tv/helix/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(monitorClient)
            .build().create(TwitchClient::class.java)
    }

    @Singleton //scope binding
    @Provides
    fun providesTwitchAuthenticationClient(
        liveNetworkMonitor: NetworkMonitor
    ): TwitchAuthenticationClient {
        val monitorClient = OkHttpClient.Builder()
            .addInterceptor(NetworkMonitorInterceptor(liveNetworkMonitor))
            .build()
        return Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/oauth2/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(monitorClient)
            .build().create(TwitchAuthenticationClient::class.java)
    }

    @Singleton
    @Provides
    fun providesTokenDataStore(
        @ApplicationContext appContext: Context
    ): TwitchDataStore {
        return TokenDataStore(appContext)
    }

    @Provides
    fun provideTwitchRepo(twitchRepoImpl: TwitchRepoImpl): TwitchRepo {
        return twitchRepoImpl
    }

    @Provides
    fun provideTwitchAuthRepo(twitchAuthenticationImpl: TwitchAuthenticationImpl): TwitchAuthentication {
        return twitchAuthenticationImpl
    }

    @Provides
    fun provideTwitchStreamRepo(twitchStreamImpl: TwitchStreamImpl): TwitchStream {
        return twitchStreamImpl
    }

    @Provides
    fun provideCoroutineDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
    @Provides
    fun provideTwitchWebSocket(
        tokenDataStore: TwitchDataStore,
        twitchParsingEngine: ParsingEngine
    ): TwitchSocket {
        return TwitchWebSocket(tokenDataStore,twitchParsingEngine)
    }


}