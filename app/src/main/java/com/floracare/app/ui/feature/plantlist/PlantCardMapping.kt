package com.floracare.app.ui.feature.plantlist

import androidx.compose.ui.graphics.Color
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.ui.theme.ForestDeep
import com.floracare.app.ui.theme.SageDeep
import com.floracare.app.ui.theme.SageMuted
import com.floracare.app.ui.theme.Terracotta
import com.floracare.app.ui.theme.WarningAmber
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Pure rollup: plants + species + open tasks -> plant list UI model. */
fun toPlantCards(
    plants: List<Plant>,
    species: List<Species>,
    openTasks: List<CareTask>,
    now: Instant,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): PlantCardsResult {
    val speciesById = species.associateBy { it.id }
    val tasksByPlant = openTasks
        .filter { it.completedAt == null && !it.isSnoozedAt(now) }
        .groupBy { it.plantId }

    val today = now.toLocalDateTime(tz).date
    var dues = 0

    val cards = plants.map { plant ->
        val speciesFor = plant.speciesId?.let { speciesById[it] }
        val nextTask = tasksByPlant[plant.id]?.minByOrNull { it.scheduledAt }
        if (nextTask != null && nextTask.scheduledAt.toLocalDateTime(tz).date == today) {
            dues += 1
        }
        buildPlantCard(plant, speciesFor, nextTask, now, tz)
    }

    return PlantCardsResult(cards = cards, duesToday = dues)
}

data class PlantCardsResult(
    val cards: List<PlantCardUi>,
    val duesToday: Int,
)

private fun buildPlantCard(
    plant: Plant,
    species: Species?,
    nextTask: CareTask?,
    now: Instant,
    tz: TimeZone,
): PlantCardUi = PlantCardUi(
    id = plant.id,
    nickname = plant.nickname,
    speciesName = species?.commonName?.takeIf { it.isNotBlank() } ?: "Unknown species",
    nextTaskLabel = nextTaskLabel(nextTask, now, tz),
    accent = accentFor(plant.speciesId),
)

internal fun nextTaskLabel(task: CareTask?, now: Instant, tz: TimeZone): String {
    if (task == null) return "No scheduled care"
    val verb = task.type.displayVerb()
    val today = now.toLocalDateTime(tz).date
    val scheduledDay = task.scheduledAt.toLocalDateTime(tz).date
    val deltaDays = scheduledDay.toEpochDays() - today.toEpochDays()
    return when {
        deltaDays == 0 -> "$verb today"
        deltaDays == 1 -> "$verb tomorrow"
        deltaDays == -1 -> "$verb 1d ago"
        deltaDays < 0 -> "$verb ${-deltaDays}d ago"
        else -> "$verb in ${deltaDays}d"
    }
}

internal fun accentFor(speciesId: String?): Color {
    val palette = ACCENT_PALETTE
    val key = speciesId ?: return palette[0]
    // `rem` of a negative int stays negative, and Int.MIN_VALUE.absoluteValue is
    // still negative due to overflow — normalise to a non-negative index.
    val index = ((key.hashCode() % palette.size) + palette.size) % palette.size
    return palette[index]
}

internal fun CareTask.isSnoozedAt(now: Instant): Boolean =
    snoozedUntil?.let { it > now } == true

private fun CareTaskType.displayVerb(): String = when (this) {
    CareTaskType.WATER -> "Water"
    CareTaskType.FERTILIZE -> "Fertilize"
    CareTaskType.MIST -> "Mist"
    CareTaskType.ROTATE -> "Rotate"
    CareTaskType.REPOT -> "Repot"
    CareTaskType.PRUNE -> "Prune"
}

private val ACCENT_PALETTE = listOf(
    SageMuted,
    Terracotta,
    SageDeep,
    WarningAmber,
    ForestDeep,
)
