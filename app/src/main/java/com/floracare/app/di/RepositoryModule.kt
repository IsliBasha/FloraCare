package com.floracare.app.di

import com.floracare.app.data.ml.DiseaseClassifier
import com.floracare.app.data.ml.SpeciesClassifier
import com.floracare.app.data.ml.TfliteDiseaseClassifier
import com.floracare.app.data.ml.TfliteSpeciesClassifier
import com.floracare.app.data.repository.PlantRepositoryImpl
import com.floracare.app.data.repository.WeatherRepositoryImpl
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {
    @Binds abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository
    @Binds abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
    @Binds abstract fun bindSpeciesClassifier(impl: TfliteSpeciesClassifier): SpeciesClassifier
    @Binds abstract fun bindDiseaseClassifier(impl: TfliteDiseaseClassifier): DiseaseClassifier
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides @Singleton
    fun provideComputeNextCareTaskUseCase(): ComputeNextCareTaskUseCase = ComputeNextCareTaskUseCase()
}
