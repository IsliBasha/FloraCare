package com.floracare.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface PlantDao {
    @Query("SELECT * FROM plant WHERE archived = 0 ORDER BY nickname ASC")
    fun observeActive(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plant WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plant: PlantEntity)

    @Update suspend fun update(plant: PlantEntity)

    @Query("SELECT COUNT(*) FROM plant")
    suspend fun count(): Int
}

@Dao
interface SpeciesDao {
    @Query("SELECT * FROM species WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SpeciesEntity?

    @Query("SELECT * FROM species WHERE lower(trim(scientificName)) = lower(trim(:scientificName)) LIMIT 1")
    suspend fun findByScientificName(scientificName: String): SpeciesEntity?

    @Query("SELECT * FROM species WHERE provider = :provider AND providerSpeciesId = :pid LIMIT 1")
    suspend fun findByProviderId(provider: String, pid: String): SpeciesEntity?

    @Query("SELECT * FROM species ORDER BY commonName ASC")
    fun observeAll(): Flow<List<SpeciesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(species: List<SpeciesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(species: SpeciesEntity)

    @Query("UPDATE species SET fetchedAt = :at WHERE id = :id")
    suspend fun markFetched(id: String, at: Instant)

    @Query("SELECT COUNT(*) FROM species")
    suspend fun count(): Int
}

@Dao
interface CareTaskDao {
    @Query("SELECT * FROM care_task WHERE plantId = :plantId AND completedAt IS NULL ORDER BY scheduledAt ASC")
    fun observeOpenForPlant(plantId: String): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_task WHERE completedAt IS NULL ORDER BY scheduledAt ASC")
    fun observeAllOpen(): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_task WHERE completedAt IS NULL AND scheduledAt <= :until ORDER BY scheduledAt ASC")
    suspend fun findDueBefore(until: Instant): List<CareTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: CareTaskEntity)

    @Query("UPDATE care_task SET completedAt = :at WHERE id = :id")
    suspend fun markCompleted(id: String, at: Instant)

    @Query("UPDATE care_task SET snoozedUntil = :until WHERE id = :id")
    suspend fun snooze(id: String, until: Instant)
}

@Dao
interface CareLogDao {
    @Query("SELECT * FROM care_log WHERE plantId = :plantId AND performedAt >= :since ORDER BY performedAt DESC")
    suspend fun findRecent(plantId: String, since: Instant): List<CareLogEntity>

    @Query("SELECT * FROM care_log WHERE performedAt >= :since ORDER BY performedAt DESC")
    fun observeSince(since: Instant): Flow<List<CareLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CareLogEntity)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entry WHERE plantId = :plantId ORDER BY capturedAt DESC")
    fun observeForPlant(plantId: String): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity)
}

@Dao
interface DiagnosisDao {
    @Query("SELECT * FROM diagnosis_result WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun observeForPlant(plantId: String): Flow<List<DiagnosisResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DiagnosisResultEntity)
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_snapshot WHERE recordedAt >= :since ORDER BY recordedAt DESC")
    suspend fun findRecent(since: Instant): List<WeatherSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: WeatherSnapshotEntity)
}
