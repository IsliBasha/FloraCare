package com.floracare.app.domain.repository

import com.floracare.app.domain.model.DiagnosisResult
import kotlinx.coroutines.flow.Flow

interface DiagnosisRepository {
    fun observeForPlant(plantId: String): Flow<List<DiagnosisResult>>
    suspend fun insert(result: DiagnosisResult)
}
