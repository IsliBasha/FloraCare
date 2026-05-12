package com.floracare.app.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.domain.model.TemperatureUnit
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing
import kotlinx.datetime.Clock
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val spacing = LocalFloraSpacing.current
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val s = state) {
            DashboardUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Reading your herbarium…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is DashboardUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            is DashboardUiState.Success -> DashboardContent(
                snapshot = s.snapshot,
                temperatureUnit = s.temperatureUnit,
                contentPadding = PaddingValues(
                    start = spacing.md,
                    end = spacing.md,
                    top = padding.calculateTopPadding() + spacing.sm,
                    bottom = padding.calculateBottomPadding() + spacing.xxl,
                ),
            )
        }
    }
}

@Composable
private fun DashboardContent(
    snapshot: DashboardSnapshot,
    temperatureUnit: TemperatureUnit,
    contentPadding: PaddingValues,
) {
    val spacing = LocalFloraSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        WeatherCard(weather = snapshot.currentWeather, temperatureUnit = temperatureUnit)
        StreakHeroCard(
            streakDays = snapshot.currentStreakDays,
            totalWatersLast30d = snapshot.totalWatersLast30d,
        )
        CareRingCard(completion = snapshot.weekCareCompletion)
        HealthTrendCard(summary = snapshot.weeklyCareSummary)
        WateringTrendCard(
            dailyCounts = snapshot.dailyWaterCounts,
            weekLabels = snapshot.sparklineWeekLabels,
        )
        PlantOfTheMonthCard(potm = snapshot.plantOfTheMonth)
    }
}

@Composable
private fun WeatherCard(weather: WeatherSnapshot?, temperatureUnit: TemperatureUnit) {
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        ) {
            Text(
                "Current weather",
                style = MaterialTheme.typography.labelLarge,
                color = accents.terracotta,
            )
            Spacer(Modifier.height(spacing.sm))
            if (weather == null) {
                Text(
                    "No recent reading. The daily run will fetch the next one — make sure location is granted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = accents.sage.copy(alpha = 0.22f),
                        shape = CircleShape,
                        modifier = Modifier.height(64.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(64.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Thermostat,
                                contentDescription = null,
                                tint = accents.sage,
                            )
                        }
                    }
                    Spacer(Modifier.padding(horizontal = spacing.sm))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            temperatureUnit.format(weather.tempC),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${weather.humidityPct.roundToInt()}% humidity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (weather.rainMm > 0f) {
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Outlined.Umbrella,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${"%.1f".format(weather.rainMm)} mm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Updated ${formatWeatherAge(Clock.System.now(), weather.recordedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakHeroCard(
    streakDays: Int,
    totalWatersLast30d: Int,
) {
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accents.sage.copy(alpha = 0.22f),
                shape = CircleShape,
                modifier = Modifier.height(72.dp),
            ) {
                Box(
                    modifier = Modifier
                        .height(72.dp)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = accents.sage,
                    )
                }
            }
            Spacer(Modifier.padding(horizontal = spacing.sm))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    streakDays.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (streakDays == 1) "day watering streak" else "days watering streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$totalWatersLast30d watering${if (totalWatersLast30d == 1) "" else "s"} in the last 30 days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CareRingCard(completion: CareCompletion) {
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    val ringSize = 88.dp
    val strokeWidth = 10.dp
    val fraction = if (completion.totalActivePlants == 0) 0f
    else completion.plantsWateredCount.toFloat() / completion.totalActivePlants

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(ringSize)) {
                    val inset = strokePx / 2f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val topLeft = Offset(inset, inset)
                    // track
                    drawArc(
                        color = accents.sage.copy(alpha = 0.15f),
                        startAngle = -210f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    // progress
                    if (fraction > 0f) {
                        drawArc(
                            color = accents.sage,
                            startAngle = -210f,
                            sweepAngle = 240f * fraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    text = if (completion.totalActivePlants == 0) "–"
                    else "${completion.plantsWateredCount}/${completion.totalActivePlants}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(spacing.lg))
            Column {
                Text(
                    "This week",
                    style = MaterialTheme.typography.labelLarge,
                    color = accents.terracotta,
                )
                Spacer(Modifier.height(4.dp))
                val body = when {
                    completion.totalActivePlants == 0 -> "No plants registered yet."
                    completion.plantsWateredCount == completion.totalActivePlants ->
                        "All ${completion.totalActivePlants} plants watered."
                    completion.plantsWateredCount == 0 ->
                        "None of ${completion.totalActivePlants} plants watered yet."
                    else ->
                        "${completion.plantsWateredCount} of ${completion.totalActivePlants} plants watered."
                }
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun WateringTrendCard(dailyCounts: List<DailyCount>, weekLabels: List<String?>) {
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        ) {
            Text(
                "Watering consistency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Last ${dailyCounts.size} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
            Sparkline(
                counts = dailyCounts.map { it.count },
                weekLabels = weekLabels,
                strokeColor = accents.sage,
                fillColor = accents.sage.copy(alpha = 0.14f),
                gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
            )
        }
    }
}

@Composable
private fun Sparkline(
    counts: List<Int>,
    weekLabels: List<String?>,
    strokeColor: Color,
    fillColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val labelHeightDp = 16.dp
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier) {
        val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val labelHeightPx = with(LocalDensity.current) { labelHeightDp.toPx() }
        val chartHeightPx = totalHeightPx - labelHeightPx

        // Chart canvas fills the top portion
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight - labelHeightDp),
        ) {
            val w = size.width
            val h = size.height
            val n = counts.size
            if (n == 0) return@Canvas

            for (q in 1..3) {
                val y = h * q / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                )
            }

            val stepX = if (n > 1) w / (n - 1) else w
            val points = counts.mapIndexed { i, c ->
                val x = i * stepX
                val yNorm = c.toFloat() / maxCount
                val y = h - (yNorm * h * 0.9f) - h * 0.05f
                Offset(x, y)
            }

            val fillPath = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(path = fillPath, color = fillColor)

            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(path = strokePath, color = strokeColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            drawCircle(color = strokeColor, radius = 5f, center = points.last())

            // tick marks at Monday boundaries
            val n2 = counts.size
            val step2 = if (n2 > 1) w / (n2 - 1) else w
            weekLabels.forEachIndexed { i, label ->
                if (label != null) {
                    val x = i * step2
                    drawLine(
                        color = gridColor,
                        start = Offset(x, h - 4f),
                        end = Offset(x, h + 4f),
                        strokeWidth = 1.5f,
                    )
                }
            }
        }

        // Label row pinned at the bottom — drawn using Text composables
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(labelHeightDp)
                .align(Alignment.BottomStart),
        ) {
            val n = counts.size
            val step = if (n > 1) 1f / (n - 1) else 1f
            weekLabels.forEachIndexed { i, label ->
                if (label != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .offset(x = (-12).dp),
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = labelColor,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HealthTrendCard(summary: List<WeeklyCareSummary>) {
    if (summary.isEmpty()) return
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        ) {
            Text(
                "Care over time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Last ${summary.size} weeks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                LegendDot(color = accents.sage, label = "Water")
                LegendDot(color = accents.terracotta.copy(alpha = 0.75f), label = "Other care")
            }
            Spacer(Modifier.height(spacing.sm))
            CareBarChart(
                summary = summary,
                waterColor = accents.sage,
                otherColor = accents.terracotta,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CareBarChart(
    summary: List<WeeklyCareSummary>,
    waterColor: Color,
    otherColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val labelHeightDp = 20.dp
    val maxCount = summary.maxOf { maxOf(it.waterCount, it.otherCount) }.coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight - labelHeightDp),
        ) {
            val w = size.width
            val h = size.height
            val n = summary.size
            val groupWidth = w / n
            val barWidth = groupWidth * 0.28f
            val barGap = groupWidth * 0.06f

            summary.forEachIndexed { i, week ->
                val groupCenter = (i + 0.5f) * groupWidth
                val leftX = groupCenter - barWidth - barGap / 2f
                val rightX = groupCenter + barGap / 2f

                val waterH = (week.waterCount.toFloat() / maxCount) * h * 0.85f
                val otherH = (week.otherCount.toFloat() / maxCount) * h * 0.85f

                if (waterH > 0f) drawRect(
                    color = waterColor,
                    topLeft = Offset(leftX, h - waterH),
                    size = Size(barWidth, waterH),
                )
                if (otherH > 0f) drawRect(
                    color = otherColor.copy(alpha = 0.72f),
                    topLeft = Offset(rightX, h - otherH),
                    size = Size(barWidth, otherH),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(labelHeightDp)
                .align(Alignment.BottomStart),
        ) {
            summary.forEach { week ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = week.weekLabel,
                        fontSize = 9.sp,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantOfTheMonthCard(potm: PlantOfTheMonth?) {
    val accents = LocalFloraAccents.current
    val spacing = LocalFloraSpacing.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        ) {
            Text(
                "Plant of the month",
                style = MaterialTheme.typography.labelLarge,
                color = accents.terracotta,
            )
            Spacer(Modifier.height(6.dp))
            if (potm == null) {
                Text(
                    "No watering logged yet — care for a plant and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    potm.nickname,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!potm.speciesName.isNullOrBlank()) {
                    Text(
                        potm.speciesName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(spacing.sm))
                val verb = if (potm.waterCount == 1) "watering" else "waterings"
                Text(
                    "${potm.waterCount} $verb in the last 30 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
