package com.floracare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PlantEntity::class,
        SpeciesEntity::class,
        CareTaskEntity::class,
        CareLogEntity::class,
        JournalEntryEntity::class,
        DiagnosisResultEntity::class,
        WeatherSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FloraCareDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun careLogDao(): CareLogDao
    abstract fun journalDao(): JournalDao
    abstract fun diagnosisDao(): DiagnosisDao
    abstract fun weatherDao(): WeatherDao
}
