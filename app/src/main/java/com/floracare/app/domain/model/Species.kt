package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class Species(
    val id: String,
    val scientificName: String,
    val commonName: String,
    val waterFrequencyDays: Int,
    val lightNeed: LightNeed,
    val humidityNeed: HumidityNeed,
    val temperatureRangeC: ClosedFloatingPointRange<Float>,
    val toxicity: Toxicity,
    val careNotes: String,
    val provider: String = PROVIDER_LOCAL,
    val providerSpeciesId: String? = null,
    val fetchedAt: Instant? = null,
    val family: String? = null,
    val genus: String? = null,
    val imageUrl: String? = null,
) {
    companion object {
        const val PROVIDER_LOCAL = "local"
        const val PROVIDER_PERENUAL = "perenual"
    }
}
