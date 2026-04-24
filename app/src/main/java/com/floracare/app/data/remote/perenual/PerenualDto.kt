package com.floracare.app.data.remote.perenual

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpeciesSearchResponse(
    @Json(name = "data") val data: List<SpeciesSearchItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SpeciesSearchItem(
    @Json(name = "id") val id: Long,
    @Json(name = "common_name") val commonName: String?,
    @Json(name = "scientific_name") val scientificName: List<String> = emptyList(),
    @Json(name = "watering") val watering: String?,
    @Json(name = "sunlight") val sunlight: List<String> = emptyList(),
    @Json(name = "default_image") val image: PerenualImage?,
)

@JsonClass(generateAdapter = true)
data class SpeciesDetailsResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "common_name") val commonName: String?,
    @Json(name = "scientific_name") val scientificName: List<String> = emptyList(),
    @Json(name = "family") val family: String?,
    @Json(name = "genus") val genus: String?,
    @Json(name = "watering") val watering: String?,
    @Json(name = "sunlight") val sunlight: List<String> = emptyList(),
    @Json(name = "hardiness") val hardiness: Hardiness?,
    @Json(name = "poisonous_to_humans") val poisonousToHumans: Int?,
    @Json(name = "poisonous_to_pets") val poisonousToPets: Int?,
    @Json(name = "care_level") val careLevel: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "default_image") val image: PerenualImage?,
)

@JsonClass(generateAdapter = true)
data class Hardiness(
    @Json(name = "min") val min: String?,
    @Json(name = "max") val max: String?,
)

@JsonClass(generateAdapter = true)
data class PerenualImage(
    @Json(name = "thumbnail") val thumbnail: String?,
    @Json(name = "medium_url") val mediumUrl: String?,
)
