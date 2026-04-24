package com.floracare.app.di

import android.content.Context
import androidx.room.Room
import com.floracare.app.data.local.CareLogDao
import com.floracare.app.data.local.CareTaskDao
import com.floracare.app.data.local.DiagnosisDao
import com.floracare.app.data.local.FloraCareDatabase
import com.floracare.app.data.local.JournalDao
import com.floracare.app.data.local.PlantDao
import com.floracare.app.data.local.SpeciesDao
import com.floracare.app.data.local.WeatherDao
import com.floracare.app.data.local.migrations.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): FloraCareDatabase =
        Room.databaseBuilder(ctx, FloraCareDatabase::class.java, "floracare.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides @Singleton
    fun provideClock(): Clock = Clock.System

    @Provides fun providePlantDao(db: FloraCareDatabase): PlantDao = db.plantDao()
    @Provides fun provideSpeciesDao(db: FloraCareDatabase): SpeciesDao = db.speciesDao()
    @Provides fun provideCareTaskDao(db: FloraCareDatabase): CareTaskDao = db.careTaskDao()
    @Provides fun provideCareLogDao(db: FloraCareDatabase): CareLogDao = db.careLogDao()
    @Provides fun provideJournalDao(db: FloraCareDatabase): JournalDao = db.journalDao()
    @Provides fun provideDiagnosisDao(db: FloraCareDatabase): DiagnosisDao = db.diagnosisDao()
    @Provides fun provideWeatherDao(db: FloraCareDatabase): WeatherDao = db.weatherDao()
}
