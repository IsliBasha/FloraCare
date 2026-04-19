package com.floracare.app.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floracare.app.ui.theme.LocalFloraSpacing

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        "Welcome to FloraCare" to "Your plants, remembered — and cared for on time.",
        "Tell us where you live" to "We use local weather to adjust watering.",
        "Permissions" to "Camera for identifying plants, notifications for gentle reminders.",
    )
    val pagerState = rememberPagerState { pages.size }
    val spacing = LocalFloraSpacing.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val (title, body) = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(title, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(16.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            Text(if (pagerState.currentPage == pages.lastIndex) "Enter garden" else "Continue")
        }
    }
}
