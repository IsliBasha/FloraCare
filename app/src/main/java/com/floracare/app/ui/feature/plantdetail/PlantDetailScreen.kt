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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    onDiagnose: () -> Unit,
    onJournal: () -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when (val s = state) {
                PlantDetailUiState.Loading -> CenteredSpinner()
                PlantDetailUiState.NotFound -> CenteredMessage("We couldn't find that plant.")
                is PlantDetailUiState.Error -> CenteredMessage(s.message)
                is PlantDetailUiState.Ready -> ReadyContent(
                    state = s,
                    onDiagnose = onDiagnose,
                    onJournal = onJournal,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: PlantDetailUiState.Ready,
    onDiagnose: () -> Unit,
    onJournal: () -> Unit,
) {
    val spacing = LocalFloraSpacing.current
    val accents = LocalFloraAccents.current
    val plant = state.plant
    val species = state.species

    LazyColumn(
        contentPadding = PaddingValues(bottom = spacing.xxl),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Hero(plant = plant, species = species, accent = accents.sage)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
            ) {
                Vital(
                    label = "Water",
                    value = state.upcoming
                        .firstOrNull { it.type == CareTaskType.WATER }
                        ?.label
                        ?.substringBefore(" · ")
                        ?: "None scheduled",
                    accent = accents.terracotta,
                )
                Vital(
                    label = "Light",
                    value = species?.lightNeed?.display() ?: "—",
                    accent = MaterialTheme.colorScheme.primary,
                )
                Vital(
                    label = "Humidity",
                    value = species?.humidityNeed?.display() ?: "—",
                    accent = accents.sage,
                )
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
                if (state.upcoming.isEmpty()) {
                    Text(
                        "·  Nothing scheduled right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    state.upcoming.forEach {
                        Text(
                            "·  ${it.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
        if (plant.notes.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = spacing.md, vertical = spacing.md)) {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        plant.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (species?.careNotes?.isNotBlank() == true) {
            item {
                Column(Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)) {
                    Text(
                        "Care tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        species.careNotes,
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
                    .padding(horizontal = spacing.md, vertical = spacing.md),
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

@Composable
private fun Hero(plant: Plant, species: Species?, accent: Color) {
    val spacing = LocalFloraSpacing.current
    val subtitle = buildString {
        if (species != null) {
            append(species.scientificName)
            append(" · ")
        }
        append(plant.locationTag.display())
        if (plant.notes.isNotBlank()) {
            append(" · ")
            append(plant.notes)
        }
    }
    // Prefer the user's own photo; fall back to Perenual's species image when
    // available; otherwise keep the solid accent block. The scrim below keeps
    // the subtitle legible against any busy photo.
    val heroImage = plant.coverPhotoUri ?: species?.imageUrl
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .background(accent.copy(alpha = 0.35f)),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (heroImage != null) {
            AsyncImage(
                model = heroImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
        }
        Column(Modifier.padding(spacing.lg)) {
            Text(
                plant.nickname,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
