package com.floracare.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.floracare.app.data.notification.NotificationDispatcher
import com.floracare.app.ui.AppStartViewModel
import com.floracare.app.ui.navigation.FloraCareNavHost
import com.floracare.app.ui.theme.FloraCareTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Carries the most recent deep-link plant id. Wrapped in a lightweight
     * envelope so two taps on the *same* plant still emit distinct values and
     * re-trigger navigation.
     */
    private val deepLink = MutableStateFlow<DeepLink?>(null)

    private val appStart: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val current by deepLink.collectAsState()
            val startDestination by appStart.startDestination.collectAsState()
            FloraCareTheme {
                // Keep a blank themed surface on screen until the VM resolves
                // the route so we never flash Plant list before Onboarding.
                if (startDestination == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                } else {
                    FloraCareNavHost(
                        startDestination = startDestination!!,
                        deepLinkPlantId = current?.plantId,
                        deepLinkKey = current?.seq,
                        onDeepLinkConsumed = { deepLink.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val plantId = intent?.getStringExtra(NotificationDispatcher.EXTRA_PLANT_ID) ?: return
        val previousSeq = deepLink.value?.seq ?: 0
        deepLink.value = DeepLink(plantId = plantId, seq = previousSeq + 1)
    }

    private data class DeepLink(val plantId: String, val seq: Int)
}
