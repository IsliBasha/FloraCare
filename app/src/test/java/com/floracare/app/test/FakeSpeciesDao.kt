package com.floracare.app.test

import com.floracare.app.data.local.SpeciesDao
import com.floracare.app.data.local.SpeciesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant

/**
 * In-memory [SpeciesDao] for JVM tests. No Room dependency, no coroutines
 * dispatcher hop — the semantics that matter for the repository are just
 * read/write + case-insensitive lookup.
 */
class FakeSpeciesDao : SpeciesDao {
    private val rows = MutableStateFlow<List<SpeciesEntity>>(emptyList())

    override suspend fun findById(id: String): SpeciesEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun findByScientificName(scientificName: String): SpeciesEntity? {
        val key = scientificName.trim().lowercase()
        return rows.value.firstOrNull { it.scientificName.trim().lowercase() == key }
    }

    override suspend fun findByProviderId(provider: String, pid: String): SpeciesEntity? =
        rows.value.firstOrNull { it.provider == provider && it.providerSpeciesId == pid }

    override fun observeAll(): Flow<List<SpeciesEntity>> = rows.asStateFlow()

    override suspend fun upsertAll(species: List<SpeciesEntity>) {
        species.forEach { upsert(it) }
    }

    override suspend fun upsert(species: SpeciesEntity) {
        rows.value = rows.value.filterNot { it.id == species.id } + species
    }

    override suspend fun markFetched(id: String, at: Instant) {
        rows.value = rows.value.map { if (it.id == id) it.copy(fetchedAt = at) else it }
    }

    override suspend fun count(): Int = rows.value.size
}
