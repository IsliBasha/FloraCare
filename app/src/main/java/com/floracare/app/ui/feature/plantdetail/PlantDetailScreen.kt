package com.floracare.app.ui.feature.plantdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    onDiagnose: () -> Unit,
    onJournal: () -> Unit,
) {
    val spacing = LocalFloraSpacing.current
    val accents = LocalFloraAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = spacing.xxl),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .background(accents.sage.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Column(Modifier.padding(spacing.lg)) {
                        Text(
                            "Mona",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "Monstera deliciosa · indoor · East window",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                ) {
                    Vital("Water", "in 2d", accents.terracotta)
                    Vital("Light", "medium", MaterialTheme.colorScheme.primary)
                    Vital("Humidity", "moderate", accents.sage)
                }
            }
            item {
                Column(Modifier.padding(horizontal = spacing.md)) {
                    Text(
                        "Upcoming care",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(spacing.sm))
                    listOf(
                        "Tomorrow · 07:00 · Water",
                        "Saturday · Fertilize (monthly)",
                        "Next week · Rotate a quarter turn",
                    ).forEach {
                        Text(
                            "·  $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = spacing.md, vertical = spacing.lg)) {
                    Text(
                        "Journal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(spacing.sm))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(listOf("j1", "j2", "j3", "j4")) {
                            Box(
                                modifier = Modifier
                                    .height(96.dp)
                                    .aspectRatio(3f / 4f)
                                    .background(
                                        accents.paperGrain,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md),
                ) {
                    FilledTonalButton(onClick = onDiagnose, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Diagnose")
                    }
                    OutlinedButton(onClick = onJournal, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("New photo")
                    }
                }
            }
        }
    }
}

@Composable
private fun Vital(label: String, value: String, accent: Color) {
    AssistChip(
        onClick = {},
        label = {
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

