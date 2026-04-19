package com.floracare.app.data.local

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot

fun SpeciesEntity.toDomain(): Species = Species(
    id = id,
    scientificName = scientificName,
    commonName = commonName,
    waterFrequencyDays = waterFrequencyDays,
    lightNeed = lightNeed,
    humidityNeed = humidityNeed,
    temperatureRangeC = tempMinC..tempMaxC,
    toxicity = toxicity,
    careNotes = careNotes,
)

fun Species.toEntity(): SpeciesEntity = SpeciesEntity(
    id = id,
    scientificName = scientificName,
    commonName = commonName,
    waterFrequencyDays = waterFrequencyDays,
    lightNeed = lightNeed,
    humidityNeed = humidityNeed,
    tempMinC = temperatureRangeC.start,
    tempMaxC = temperatureRangeC.endInclusive,
    toxicity = toxicity,
    careNotes = careNotes,
)

fun PlantEntity.toDomain(): Plant = Plant(
    id = id,
    nickname = nickname,
    speciesId = speciesId,
    locationTag = locationTag,
    acquiredAt = acquiredAt,
    coverPhotoUri = coverPhotoUri,
    notes = notes,
    archived = archived,
)

fun Plant.toEntity(): PlantEntity = PlantEntity(
    id = id,
    nickname = nickname,
    speciesId = speciesId,
    locationTag = locationTag,
    acquiredAt = acquiredAt,
    coverPhotoUri = coverPhotoUri,
    notes = notes,
    archived = archived,
)

fun CareTaskEntity.toDomain(): CareTask = CareTask(
    id = id,
    plantId = plantId,
    type = type,
    scheduledAt = scheduledAt,
    completedAt = completedAt,
    snoozedUntil = snoozedUntil,
    source = source,
)

fun CareLogEntity.toDomain(): CareLog = CareLog(
    id = id,
    plantId = plantId,
    taskType = taskType,
    performedAt = performedAt,
    soilMoistureNote = soilMoistureNote,
    userNote = userNote,
)

fun CareLog.toEntity(): CareLogEntity = CareLogEntity(
    id = id,
    plantId = plantId,
    taskType = taskType,
    performedAt = performedAt,
    soilMoistureNote = soilMoistureNote,
    userNote = userNote,
)

fun WeatherSnapshotEntity.toDomain(): WeatherSnapshot = WeatherSnapshot(
    id = id, lat = lat, lon = lon, recordedAt = recordedAt,
    tempC = tempC, humidityPct = humidityPct, rainMm = rainMm, uvIndex = uvIndex,
)

fun WeatherSnapshot.toEntity(): WeatherSnapshotEntity = WeatherSnapshotEntity(
    id = id, lat = lat, lon = lon, recordedAt = recordedAt,
    tempC = tempC, humidityPct = humidityPct, rainMm = rainMm, uvIndex = uvIndex,
)
