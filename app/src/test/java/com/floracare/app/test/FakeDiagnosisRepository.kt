package com.floracare.app.test

import com.floracare.app.domain.model.DiagnosisResult
import com.floracare.app.domain.repository.DiagnosisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [DiagnosisRepository]. Instance-isolated so each test
 * gets a clean slate.
 */
class FakeDiagnosisRepository : DiagnosisRepository {

    val insertedResults = mutableListOf<DiagnosisResult>()
    private val results = MutableStateFlow<List<DiagnosisResult>>(emptyList())

    var throwOnInsert: Throwable? = null

    override fun observeForPlant(plantId: String): Flow<List<DiagnosisResult>> =
        results.map { list -> list.filter { it.plantId == plantId } }

    override suspend fun insert(result: DiagnosisResult) {
        throwOnInsert?.let { throw it }
        insertedResults += result
        results.value = results.value + result
    }
}
