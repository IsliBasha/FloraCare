package com.floracare.app.domain.model

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
)
