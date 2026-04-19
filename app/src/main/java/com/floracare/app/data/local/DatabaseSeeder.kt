package com.floracare.app.data.local

import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/**
 * Seeds 3 sample species + 3 plants + a handful of scheduled tasks on first launch.
 * Safe to call multiple times — no-ops if data already present.
 */
class DatabaseSeeder(private val db: FloraCareDatabase) {
    suspend fun seedIfEmpty() {
        if (db.plantDao().count() > 0) return

        val now = Clock.System.now()
        val species = listOf(
            SpeciesEntity(
                id = "sp-monstera",
                scientificName = "Monstera deliciosa",
                commonName = "Swiss Cheese Plant",
                waterFrequencyDays = 7,
                lightNeed = LightNeed.MEDIUM,
                humidityNeed = HumidityNeed.MEDIUM,
                tempMinC = 18f, tempMaxC = 28f,
                toxicity = Toxicity.MILD,
                careNotes = "Let the top 2cm of soil dry between waterings.",
            ),
            SpeciesEntity(
                id = "sp-ficus-lyrata",
                scientificName = "Ficus lyrata",
                commonName = "Fiddle Leaf Fig",
                waterFrequencyDays = 8,
                lightNeed = LightNeed.HIGH,
                humidityNeed = HumidityNeed.MEDIUM,
                tempMinC = 16f, tempMaxC = 27f,
                toxicity = Toxicity.MILD,
                careNotes = "Bright indirect light. Hates cold drafts.",
            ),
            SpeciesEntity(
                id = "sp-sansevieria",
                scientificName = "Sansevieria trifasciata",
                commonName = "Snake Plant",
                waterFrequencyDays = 14,
                lightNeed = LightNeed.LOW,
                humidityNeed = HumidityNeed.LOW,
                tempMinC = 15f, tempMaxC = 30f,
                toxicity = Toxicity.MILD,
                careNotes = "Drought-tolerant. Overwatering is the main killer.",
            ),
        )
        db.speciesDao().upsertAll(species)

        val plants = listOf(
            PlantEntity("pl-mona", "Mona", "sp-monstera", LocationTag.INDOOR, now - 120.days, null, "East window.", false),
            PlantEntity("pl-finn", "Finn", "sp-ficus-lyrata", LocationTag.INDOOR, now - 60.days, null, "Living room corner.", false),
            PlantEntity("pl-sage", "Sage", "sp-sansevieria", LocationTag.INDOOR, now - 210.days, null, "Bathroom shelf.", false),
        )
        plants.forEach { db.plantDao().upsert(it) }

        val tasks = listOf(
            CareTaskEntity("ct-1", "pl-mona", CareTaskType.WATER, now + 2.days, null, null, CareTaskSource.ADAPTIVE),
            CareTaskEntity("ct-2", "pl-finn", CareTaskType.WATER, now + 1.days, null, null, CareTaskSource.ADAPTIVE),
            CareTaskEntity("ct-3", "pl-sage", CareTaskType.WATER, now + 6.days, null, null, CareTaskSource.ADAPTIVE),
        )
        tasks.forEach { db.careTaskDao().upsert(it) }
    }
}
