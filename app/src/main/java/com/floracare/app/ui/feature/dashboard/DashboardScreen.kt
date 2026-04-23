package com.floracare.app.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing

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
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
        StreakHeroCard(
            streakDays = snapshot.currentStreakDays,
            totalWatersLast30d = snapshot.totalWatersLast30d,
        )
        WateringTrendCard(dailyCounts = snapshot.dailyWaterCounts)
        PlantOfTheMonthCard(potm = snapshot.plantOfTheMonth)
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
                    if (streakDays == 1) "day watering streak" else "day watering streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$totalWatersLast30d watering${if (totalWatersLast30d == 1) "" else "s"} in the last 30 days",
                    style = MaterialTheme.typography.labelMedium,
                    color = accents.terracotta,
                )
            }
        }
    }
}

@Composable
private fun WateringTrendCard(dailyCounts: List<DailyCount>) {
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
                strokeColor = accents.sage,
                fillColor = accents.sage.copy(alpha = 0.14f),
                gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
            )
            Spacer(Modifier.height(spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "30d ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Sparkline(
    counts: List<Int>,
    strokeColor: Color,
    fillColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = counts.size
        if (n == 0) return@Canvas

        // faint baseline grid — quartiles
        for (q in 1..3) {
            val y = h * q / 4f
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(w, y),
                strokeWidth = 1f,
            )
        }

        val stepX = if (n > 1) w / (n - 1) else w
        val points = counts.mapIndexed { i, c ->
            val x = i * stepX
            val yNorm = c.toFloat() / maxCount
            val y = h - (yNorm * h * 0.9f) - h * 0.05f
            androidx.compose.ui.geometry.Offset(x, y)
        }

        // filled polygon under the line
        val fillPath = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(path = fillPath, color = fillColor)

        // polyline
        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = strokePath,
            color = strokeColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // dot on the last day (today)
        val last = points.last()
        drawCircle(
            color = strokeColor,
            radius = 5f,
            center = last,
        )
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
