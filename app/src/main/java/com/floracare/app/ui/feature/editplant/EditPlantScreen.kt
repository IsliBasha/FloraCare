package com.floracare.app.ui.feature.editplant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.ui.theme.LocalFloraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditPlantViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var pendingArchive by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        val s = state
        if (s !is EditPlantUiState.Saved) return@LaunchedEffect
        if (pendingArchive) {
            // Show "Archived" snackbar with Undo for ~4s. Either path eventually
            // pops back — Undo just decides whether the archived flag stays flipped.
            val result = snackbarHost.showSnackbar(
                message = "Plant deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(EditPlantEvent.UnarchiveLast)
            }
            pendingArchive = false
        }
        onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit plant",
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
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                EditPlantUiState.Loading -> CenteredSpinner()
                EditPlantUiState.NotFound -> CenteredMessage("We couldn't find that plant.")
                EditPlantUiState.Saved -> CenteredSpinner()
                is EditPlantUiState.Ready -> ReadyForm(
                    state = s,
                    onEvent = viewModel::onEvent,
                    onArchiveConfirmed = {
                        pendingArchive = true
                        viewModel.onEvent(EditPlantEvent.Archive)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyForm(
    state: EditPlantUiState.Ready,
    onEvent: (EditPlantEvent) -> Unit,
    onArchiveConfirmed: () -> Unit,
) {
    val spacing = LocalFloraSpacing.current
    var showArchiveDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { onEvent(EditPlantEvent.SetCoverPhotoUri(it.toString())) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        OutlinedTextField(
            value = state.nickname,
            onValueChange = { onEvent(EditPlantEvent.SetNickname(it)) },
            label = { Text("Nickname") },
            singleLine = true,
            isError = state.nicknameError != null,
            supportingText = state.nicknameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        LocationSelector(
            selected = state.locationTag,
            onChange = { onEvent(EditPlantEvent.SetLocation(it)) },
        )

        CoverPhotoTile(
            uri = state.coverPhotoUri,
            onPick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onClear = { onEvent(EditPlantEvent.SetCoverPhotoUri(null)) },
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = { onEvent(EditPlantEvent.SetNotes(it)) },
            label = { Text("Notes (optional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { onEvent(EditPlantEvent.Save) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.saving) "Saving…" else "Save changes")
        }
        OutlinedButton(
            onClick = { showArchiveDialog = true },
            enabled = !state.saving,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Delete plant")
        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Delete this plant?") },
            text = {
                Text(
                    "It will disappear from your list. You can undo right after.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveDialog = false
                    onArchiveConfirmed()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Cancel") }
            },
        )
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

@Composable
private fun CoverPhotoTile(
    uri: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val spacing = LocalFloraSpacing.current
    Column {
        Text(
            "Cover photo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clickable(onClick = onPick),
        ) {
            if (uri.isNullOrBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Pick a photo")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = uri.toUri(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f)),
                    )
                }
            }
        }
        if (!uri.isNullOrBlank()) {
            Spacer(Modifier.height(spacing.xs))
            TextButton(onClick = onClear) { Text("Remove photo") }
        }
    }
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

private fun LocationTag.displayLabel(): String = when (this) {
    LocationTag.INDOOR -> "Indoor"
    LocationTag.OUTDOOR -> "Outdoor"
    LocationTag.GREENHOUSE -> "Greenhouse"
}
