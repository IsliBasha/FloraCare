package com.floracare.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.SoilMoistureNote
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant

@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,
    val scientificName: String,
    val commonName: String,
    val waterFrequencyDays: Int,
    val lightNeed: LightNeed,
    val humidityNeed: HumidityNeed,
    val tempMinC: Float,
    val tempMaxC: Float,
    val toxicity: Toxicity,
    val careNotes: String,
)

@Entity(
    tableName = "plant",
    foreignKeys = [
        ForeignKey(
            entity = SpeciesEntity::class,
            parentColumns = ["id"],
            childColumns = ["speciesId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("speciesId")],
)
data class PlantEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val speciesId: String?,
    val locationTag: LocationTag,
    val acquiredAt: Instant,
    val coverPhotoUri: String?,
    val notes: String,
    val archived: Boolean,
)

@Entity(
    tableName = "care_task",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId"), Index("scheduledAt")],
)
data class CareTaskEntity(
    @PrimaryKey val id: String,
    val plantId: String,
    val type: CareTaskType,
    val scheduledAt: Instant,
    val completedAt: Instant?,
    val snoozedUntil: Instant?,
    val source: CareTaskSource,
)

@Entity(
    tableName = "care_log",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId"), Index("performedAt")],
)
data class CareLogEntity(
    @PrimaryKey val id: String,
    val plantId: String,
    val taskType: CareTaskType,
    val performedAt: Instant,
    val soilMoistureNote: SoilMoistureNote?,
    val userNote: String?,
)

@Entity(
    tableName = "journal_entry",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId"), Index("capturedAt")],
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val plantId: String,
    val photoUri: String,
    val capturedAt: Instant,
    val note: String,
    val heightCm: Float?,
)

@Entity(tableName = "diagnosis_result")
data class DiagnosisResultEntity(
    @PrimaryKey val id: String,
    val plantId: String?,
    val photoUri: String,
    val diagnosisLabel: String,
    val confidence: Float,
    val treatmentSuggestion: String,
    val createdAt: Instant,
)

@Entity(tableName = "weather_snapshot", indices = [Index("recordedAt")])
data class WeatherSnapshotEntity(
    @PrimaryKey val id: String,
    val lat: Double,
    val lon: Double,
    val recordedAt: Instant,
    val tempC: Float,
    val humidityPct: Float,
    val rainMm: Float,
    val uvIndex: Float,
)
