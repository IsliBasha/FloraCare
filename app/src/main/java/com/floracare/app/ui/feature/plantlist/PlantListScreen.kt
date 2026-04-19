package com.floracare.app.ui.feature.plantlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.ui.theme.LocalFloraAccents
import com.floracare.app.ui.theme.LocalFloraSpacing

data class PlantCardUi(
    val id: String,
    val nickname: String,
    val speciesName: String,
    val nextTaskLabel: String,
    val accent: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    onPlantClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: PlantListViewModel = hiltViewModel(),
) {
    val spacing = LocalFloraSpacing.current
    val state by viewModel.state.collectAsState()
    val plants: List<PlantCardUi> = (state as? PlantListUiState.Success)?.plants.orEmpty()
    val headerSubtitle = when (val s = state) {
        PlantListUiState.Loading -> "Loading your garden…"
        is PlantListUiState.Success -> "${s.plants.size} plants · tap a card to open"
        is PlantListUiState.Error -> "Error: ${s.message}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Your garden",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            headerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onDashboardClick) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Dashboard")
                    }
                    FilledTonalIconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                    Spacer(Modifier.padding(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add plant")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = spacing.md, end = spacing.md,
                top = padding.calculateTopPadding() + spacing.sm,
                bottom = padding.calculateBottomPadding() + spacing.xxl,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(plants, key = { it.id }) { plant ->
                PlantCard(plant = plant, onClick = { onPlantClick(plant.id) })
            }
        }
    }
}

@Composable
private fun PlantCard(plant: PlantCardUi, onClick: () -> Unit) {
    val accents = LocalFloraAccents.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(plant.accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.BottomStart,
        ) {
            Surface(
                color = plant.accent,
                shape = CircleShape,
                modifier = Modifier
                    .padding(12.dp)
                    .height(14.dp),
            ) { Box(Modifier.padding(horizontal = 10.dp, vertical = 1.dp)) }
        }
        Column(Modifier.padding(14.dp)) {
            Text(
                plant.nickname,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                plant.speciesName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                plant.nextTaskLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accents.terracotta,
            )
        }
    }
}
