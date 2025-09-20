package com.avas.proteinviewer.di

import com.avas.proteinviewer.data.api.PDBApiService
import com.avas.proteinviewer.data.api.PDBFileService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CORE_BASE_URL = "https://data.rcsb.org/rest/v1/core/"
    private const val FILE_BASE_URL = "https://files.rcsb.org/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    @Named("core")
    fun provideCoreRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(CORE_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePdbApiService(@Named("core") retrofit: Retrofit): PDBApiService =
        retrofit.create(PDBApiService::class.java)

    @Provides
    @Singleton
    @Named("file")
    fun provideFileRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(FILE_BASE_URL)
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun providePdbFileService(@Named("file") retrofit: Retrofit): PDBFileService =
        retrofit.create(PDBFileService::class.java)
}
