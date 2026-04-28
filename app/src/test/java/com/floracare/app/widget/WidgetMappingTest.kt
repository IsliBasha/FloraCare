package com.floracare.app.widget

import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class WidgetMappingTest {

    private val tz = TimeZone.UTC
    private val now = Instant.parse("2026-04-28T12:00:00Z")

    private fun plant(id: String, nick: String = id) = Plant(
        id = id,
        nickname = nick,
        speciesId = null,
        locationTag = LocationTag.INDOOR,
        acquiredAt = now - 30.days,
        coverPhotoUri = null,
        notes = "",
    )

    private fun task(
        id: String,
        plantId: String,
        scheduledAt: Instant,
        type: CareTaskType = CareTaskType.WATER,
        completedAt: Instant? = null,
        snoozedUntil: Instant? = null,
    ) = CareTask(
        id = id,
        plantId = plantId,
        type = type,
        scheduledAt = scheduledAt,
        source = CareTaskSource.ADAPTIVE,
        completedAt = completedAt,
        snoozedUntil = snoozedUntil,
    )

    @Test
    fun `empty inputs yield empty rows`() {
        assertEquals(emptyList<WidgetRow>(), toWidgetRows(now, emptyList(), emptyList(), tz))
    }

    @Test
    fun `completed tasks are excluded`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t1", "p1", now + 1.hours, completedAt = now - 1.hours),
                task("t2", "p1", now + 2.hours),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(1, out.size)
        assertEquals("t2", out.single().taskId)
    }

    @Test
    fun `snoozed tasks (until in the future) are excluded`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t1", "p1", now + 1.hours, snoozedUntil = now + 2.days),
                task("t2", "p1", now + 2.hours),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(1, out.size)
        assertEquals("t2", out.single().taskId)
    }

    @Test
    fun `expired snooze does not exclude the task`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t1", "p1", now + 1.hours, snoozedUntil = now - 1.hours),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(1, out.size)
    }

    @Test
    fun `rows are sorted by scheduledAt ascending`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t-late", "p1", now + 5.days),
                task("t-now", "p1", now),
                task("t-mid", "p1", now + 2.days),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(listOf("t-now", "t-mid", "t-late"), out.map { it.taskId })
    }

    @Test
    fun `rows cap at WIDGET_MAX_ROWS`() {
        val out = toWidgetRows(
            now,
            tasks = (1..10).map {
                task("t$it", "p1", now + it.hours)
            },
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(WIDGET_MAX_ROWS, out.size)
    }

    @Test
    fun `plant lookup populates nickname and missing plant falls back to a generic label`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t1", "p1", now + 1.hours),
                task("t2", "ghost", now + 2.hours),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals("Mona", out[0].plantNickname)
        assertEquals("Plant", out[1].plantNickname)
    }

    @Test
    fun `due labels match for today, tomorrow, future, and overdue`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(
                task("t-today", "p1", now + 4.hours),
                task("t-tom", "p1", now + 1.days),
                task("t-future", "p1", now + 3.days),
            ),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals("Today", out[0].dueLabel)
        assertEquals("Tomorrow", out[1].dueLabel)
        assertEquals("in 3d", out[2].dueLabel)
    }

    @Test
    fun `overdue tasks surface with a past-tense label`() {
        val out = toWidgetRows(
            now,
            tasks = listOf(task("t-late", "p1", now - 2.days)),
            plants = listOf(plant("p1", "Mona")),
            tz = tz,
        )
        assertEquals(1, out.size)
        assertTrue(
            "expected overdue label, got ${out.single().dueLabel}",
            out.single().dueLabel.contains("ago"),
        )
    }
}
