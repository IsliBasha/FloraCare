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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.DiagnosisResult
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    onDiagnose: () -> Unit,
    onJournal: () -> Unit,
    onEdit: () -> Unit,
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
                actions = {
                    if (state is PlantDetailUiState.Ready) {
                        OverflowMenu(onEdit = onEdit)
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
            val waterTask = state.upcoming.firstOrNull { it.type == CareTaskType.WATER }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
            ) {
                Vital(
                    label = "Water",
                    value = waterTask?.label?.substringBefore(" · ") ?: "None scheduled",
                    accent = accents.terracotta,
                    sub = waterTask?.reasonLabel,
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
                    state.upcoming.forEach { task ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                "·  ${task.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            task.reasonLabel?.let { reason ->
                                Text(
                                    "    $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                            }
                        }
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
        if (state.recentDiagnoses.isNotEmpty()) {
            item {
                DiagnosisHistorySection(diagnoses = state.recentDiagnoses)
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
    }
    val heroImage = plant.coverPhotoUri ?: species?.imageUrl
    val onPhoto = heroImage != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .background(accent.copy(alpha = 0.35f)),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (onPhoto) {
            AsyncImage(
                model = heroImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
            )
        }
        val nicknameColor = if (onPhoto) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onBackground
        }
        val subtitleColor = if (onPhoto) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Column(Modifier.padding(spacing.lg)) {
            Text(
                plant.nickname,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = nicknameColor,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
    }
}

@Composable
private fun Vital(label: String, value: String, accent: Color, sub: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
            sub?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
        }
    }
}

@Composable
private fun DiagnosisHistorySection(diagnoses: List<DiagnosisResult>) {
    val spacing = LocalFloraSpacing.current
    Column(Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)) {
        Text(
            "Recent diagnoses",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(spacing.xs))
        diagnoses.forEach { result ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            result.diagnosisLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatDiagnosisDate(result.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        result.treatmentSuggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun formatDiagnosisDate(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.take(3).let { it[0] + it.drop(1).lowercase() }
    return "${date.dayOfMonth} $month"
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
