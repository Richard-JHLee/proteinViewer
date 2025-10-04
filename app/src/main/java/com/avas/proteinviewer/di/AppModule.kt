package com.avas.proteinviewer.di

import com.avas.proteinviewer.data.api.PDBAPIService
import com.avas.proteinviewer.data.repository.ProteinDatabase
import com.avas.proteinviewer.data.repository.ProteinRepositoryImpl
import com.avas.proteinviewer.domain.repository.ProteinRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindProteinRepository(
        proteinRepositoryImpl: ProteinRepositoryImpl
    ): ProteinRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideProteinDatabase(
            apiService: PDBAPIService
        ): ProteinDatabase {
            return ProteinDatabase(apiService)
        }
    }
}
