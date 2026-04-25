package com.floracare.app.di

import com.floracare.app.BuildConfig
import com.floracare.app.data.remote.WeatherApi
import com.floracare.app.data.remote.perenual.PerenualApi
import com.floracare.app.data.remote.perenual.PerenualAuthInterceptor
import com.floracare.app.data.remote.perenual.RedactingHttpLogger
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton
    @Named("perenualClient")
    fun providePerenualClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(PerenualAuthInterceptor(BuildConfig.PERENUAL_KEY))
        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor(RedactingHttpLogger())
                .apply { level = HttpLoggingInterceptor.Level.BASIC }
            builder.addInterceptor(logger)
        }
        return builder.build()
    }

    @Provides @Singleton
    @Named("perenualRetrofit")
    fun providePerenualRetrofit(
        @Named("perenualClient") client: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://perenual.com/api/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton
    fun providePerenualApi(@Named("perenualRetrofit") retrofit: Retrofit): PerenualApi =
        retrofit.create(PerenualApi::class.java)

    @Provides @Singleton
    fun provideWeatherApi(@Named("weatherRetrofit") retrofit: Retrofit): WeatherApi =
        retrofit.create(WeatherApi::class.java)
}
