package com.floracare.app.data.repository

import com.floracare.app.data.local.DiagnosisDao
import com.floracare.app.data.local.DiagnosisResultEntity
import com.floracare.app.domain.model.DiagnosisResult
import com.floracare.app.domain.repository.DiagnosisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosisRepositoryImpl @Inject constructor(
    private val dao: DiagnosisDao,
) : DiagnosisRepository {

    override fun observeForPlant(plantId: String): Flow<List<DiagnosisResult>> =
        dao.observeForPlant(plantId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(result: DiagnosisResult) =
        dao.insert(result.toEntity())
}

private fun DiagnosisResultEntity.toDomain() = DiagnosisResult(
    id = id,
    plantId = plantId,
    photoUri = photoUri,
    diagnosisLabel = diagnosisLabel,
    confidence = confidence,
    treatmentSuggestion = treatmentSuggestion,
    createdAt = createdAt,
)

private fun DiagnosisResult.toEntity() = DiagnosisResultEntity(
    id = id,
    plantId = plantId,
    photoUri = photoUri,
    diagnosisLabel = diagnosisLabel,
    confidence = confidence,
    treatmentSuggestion = treatmentSuggestion,
    createdAt = createdAt,
)
