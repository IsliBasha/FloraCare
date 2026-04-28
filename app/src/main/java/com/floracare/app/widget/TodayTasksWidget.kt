package com.floracare.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.floracare.app.MainActivity
import com.floracare.app.data.notification.NotificationDispatcher
import com.floracare.app.domain.model.CareTaskType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

private val Paper = Color(0xFFF5EFE0)
private val PaperDim = Color(0xFFEAE2D0)
private val ForestDeep = Color(0xFF1F3A2E)
private val RowSurface = Color(0xFF2A4A3B)
private val Sage = Color(0xFF5B8C6B)
private val Terracotta = Color(0xFFC46D4F)

class TodayTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rows = loadRows(context)
        provideContent {
            GlanceTheme {
                WidgetBody(context = context, rows = rows)
            }
        }
    }

    private suspend fun loadRows(context: Context): List<WidgetRow> {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val repo = entryPoint.plantRepository()
        val tasks = repo.observeAllOpenTasks().first()
        val plants = repo.observePlants().first()
        return toWidgetRows(now = Clock.System.now(), tasks = tasks, plants = plants)
    }

    @Composable
    private fun WidgetBody(context: Context, rows: List<WidgetRow>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(ForestDeep))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Header(taskCount = rows.size)
            Spacer(GlanceModifier.height(10.dp))
            if (rows.isEmpty()) {
                EmptyState()
            } else {
                rows.forEachIndexed { i, row ->
                    if (i > 0) Spacer(GlanceModifier.height(6.dp))
                    TaskRow(context = context, row = row)
                }
            }
        }
    }

    @Composable
    private fun Header(taskCount: Int) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Today's care",
                style = TextStyle(
                    color = ColorProvider(Paper),
                    fontSize = sp(14),
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                if (taskCount == 0) "—" else "$taskCount due",
                style = TextStyle(
                    color = ColorProvider(Sage),
                    fontSize = sp(11),
                ),
            )
        }
    }

    @Composable
    private fun EmptyState() {
        Box(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "All caught up.",
                style = TextStyle(
                    color = ColorProvider(PaperDim),
                    fontSize = sp(12),
                ),
            )
        }
    }

    @Composable
    private fun TaskRow(context: Context, row: WidgetRow) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(RowSurface))
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(
                    actionStartActivity(
                        activity = MainActivity::class.java,
                        parameters = actionParametersOf(PlantIdKey to row.plantId),
                    ),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.taskType.glyph(),
                style = TextStyle(color = ColorProvider(Sage), fontSize = sp(14)),
            )
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    row.plantNickname,
                    style = TextStyle(
                        color = ColorProvider(Paper),
                        fontSize = sp(13),
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    row.taskType.verb(),
                    style = TextStyle(
                        color = ColorProvider(PaperDim),
                        fontSize = sp(10),
                    ),
                )
            }
            Text(
                row.dueLabel,
                style = TextStyle(
                    color = ColorProvider(if (row.dueLabel.contains("ago")) Terracotta else Paper),
                    fontSize = sp(11),
                ),
            )
        }
    }

    private companion object {
        // Glance ActionParameters keys are forwarded into the launched
        // Activity intent as extras under the same string name.
        val PlantIdKey = ActionParameters.Key<String>(NotificationDispatcher.EXTRA_PLANT_ID)
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}

private fun CareTaskType.verb(): String = when (this) {
    CareTaskType.WATER -> "Water"
    CareTaskType.FERTILIZE -> "Fertilize"
    CareTaskType.MIST -> "Mist"
    CareTaskType.ROTATE -> "Rotate"
    CareTaskType.REPOT -> "Repot"
    CareTaskType.PRUNE -> "Prune"
}

private fun CareTaskType.glyph(): String = when (this) {
    CareTaskType.WATER -> "💧"
    CareTaskType.FERTILIZE -> "🌱"
    CareTaskType.MIST -> "💦"
    CareTaskType.ROTATE -> "🔄"
    CareTaskType.REPOT -> "🪴"
    CareTaskType.PRUNE -> "✂"
}

private fun sp(value: Int): TextUnit = TextUnit(value.toFloat(), TextUnitType.Sp)
