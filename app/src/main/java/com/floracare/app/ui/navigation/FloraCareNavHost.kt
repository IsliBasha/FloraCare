package com.floracare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.floracare.app.ui.feature.addplant.AddPlantManualScreen
import com.floracare.app.ui.feature.addplant.AddPlantScreen
import com.floracare.app.ui.feature.dashboard.DashboardScreen
import com.floracare.app.ui.feature.diagnose.DiagnoseScreen
import com.floracare.app.ui.feature.editplant.EditPlantScreen
import com.floracare.app.ui.feature.identify.IdentifyScreen
import com.floracare.app.ui.feature.journal.JournalScreen
import com.floracare.app.ui.feature.onboarding.OnboardingScreen
import com.floracare.app.ui.feature.plantdetail.PlantDetailScreen
import com.floracare.app.ui.feature.plantlist.PlantListScreen
import com.floracare.app.ui.feature.settings.SettingsScreen

@Composable
fun FloraCareNavHost(
    startDestination: FloraRoute = FloraRoute.PlantList,
    deepLinkPlantId: String? = null,
    deepLinkKey: Int? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkKey, deepLinkPlantId) {
        if (deepLinkPlantId != null && deepLinkKey != null) {
            navController.navigate(FloraRoute.PlantDetail(deepLinkPlantId))
            onDeepLinkConsumed()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<FloraRoute.Onboarding> {
            OnboardingScreen(
                onDone = {
                    navController.navigate(FloraRoute.PlantList) {
                        popUpTo(FloraRoute.Onboarding) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<FloraRoute.PlantList> {
            PlantListScreen(
                onPlantClick = { id -> navController.navigate(FloraRoute.PlantDetail(id)) },
                onAddClick = { navController.navigate(FloraRoute.AddPlant) },
                onDashboardClick = { navController.navigate(FloraRoute.Dashboard) },
                onSettingsClick = { navController.navigate(FloraRoute.Settings) },
            )
        }
        composable<FloraRoute.PlantDetail> { backStackEntry ->
            val route: FloraRoute.PlantDetail = backStackEntry.toRoute()
            PlantDetailScreen(
                plantId = route.plantId,
                onBack = { navController.popBackStack() },
                onDiagnose = { navController.navigate(FloraRoute.Diagnose(route.plantId)) },
                onJournal = { navController.navigate(FloraRoute.Journal(route.plantId)) },
                onEdit = { navController.navigate(FloraRoute.EditPlant(route.plantId)) },
            )
        }
        composable<FloraRoute.EditPlant> {
            EditPlantScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(
                        route = FloraRoute.PlantList,
                        inclusive = false,
                    )
                },
            )
        }
        composable<FloraRoute.AddPlant> {
            AddPlantScreen(
                onIdentifyClick = { navController.navigate(FloraRoute.Identify) },
                onManualClick = { navController.navigate(FloraRoute.AddPlantManual) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<FloraRoute.AddPlantManual> {
            AddPlantManualScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(
                        route = FloraRoute.PlantList,
                        inclusive = false,
                    )
                },
            )
        }
        composable<FloraRoute.Identify> {
            IdentifyScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.popBackStack(
                        route = FloraRoute.PlantList,
                        inclusive = false,
                    )
                },
            )
        }
        composable<FloraRoute.Diagnose> {
            DiagnoseScreen(onBack = { navController.popBackStack() })
        }
        composable<FloraRoute.Journal> { backStackEntry ->
            val route: FloraRoute.Journal = backStackEntry.toRoute()
            JournalScreen(plantId = route.plantId, onBack = { navController.popBackStack() })
        }
        composable<FloraRoute.Dashboard> {
            DashboardScreen(onBack = { navController.popBackStack() })
        }
        composable<FloraRoute.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
