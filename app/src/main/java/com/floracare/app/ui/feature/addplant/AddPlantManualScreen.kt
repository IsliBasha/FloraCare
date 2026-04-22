package com.floracare.app.ui.feature.addplant

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Species
import com.floracare.app.ui.theme.LocalFloraSpacing
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantManualScreen(
    onBack: () -> Unit,
    onSaved: (plantId: String) -> Unit,
    viewModel: AddPlantManualViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedPlantId) {
        state.savedPlantId?.let(onSaved)
    }

    val spacing = LocalFloraSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New plant",
                        style = MaterialTheme.typography.headlineSmall,
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
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            NicknameField(state, viewModel::onNicknameChange)
            SpeciesField(state, viewModel)
            LocationSelector(state.locationTag, viewModel::onLocationTagChange)
            AcquiredDateField(state.acquiredAt, viewModel::onAcquiredAtChange)
            NotesField(state.notes, viewModel::onNotesChange)

            state.errorMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.onSubmit() },
                enabled = !state.saving && state.savedPlantId == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Saving…" else "Save plant")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NicknameField(
    state: AddPlantManualUiState,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.nickname,
        onValueChange = onChange,
        label = { Text("Nickname") },
        placeholder = { Text("Mona the monstera") },
        singleLine = true,
        isError = state.nicknameError != null,
        supportingText = state.nicknameError?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SpeciesField(
    state: AddPlantManualUiState,
    viewModel: AddPlantManualViewModel,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.speciesQuery,
            onValueChange = viewModel::onSpeciesQueryChange,
            label = { Text("Species") },
            placeholder = { Text("Search or type a new species") },
            singleLine = true,
            trailingIcon = {
                if (state.speciesQuery.isNotEmpty()) {
                    IconButton(onClick = viewModel::onSpeciesCleared) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
            },
            supportingText = {
                if (state.selectedSpecies == null && state.speciesQuery.isNotBlank()) {
                    Text("No match? We'll create it as a new species.")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.speciesMatches.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.heightIn(max = 260.dp)) {
                    state.speciesMatches.forEachIndexed { i, species ->
                        SpeciesRow(species = species, onClick = { viewModel.onSpeciesSelected(species) })
                        if (i < state.speciesMatches.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeciesRow(species: Species, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                species.commonName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                species.scientificName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelector(
    selected: LocationTag,
    onChange: (LocationTag) -> Unit,
) {
    Column {
        Text(
            "Where does it live?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LocationTag.entries.forEach { tag ->
                FilterChip(
                    selected = tag == selected,
                    onClick = { onChange(tag) },
                    label = { Text(tag.displayLabel()) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquiredDateField(
    acquiredAt: Instant?,
    onChange: (Instant) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = acquiredAt?.let { humanDate(it) } ?: "Select a date"

    Column {
        Text(
            "Acquired",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (showPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = acquiredAt?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onChange(Instant.fromEpochMilliseconds(it)) }
                    showPicker = false
                }) { Text("Use date") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesField(notes: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = notes,
        onValueChange = onChange,
        label = { Text("Notes (optional)") },
        placeholder = { Text("Where it sits, how it reacted to its last repot…") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun LocationTag.displayLabel(): String = when (this) {
    LocationTag.INDOOR -> "Indoor"
    LocationTag.OUTDOOR -> "Outdoor"
    LocationTag.GREENHOUSE -> "Greenhouse"
}

private fun humanDate(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d".format(local.year, local.monthNumber, local.dayOfMonth)
}
