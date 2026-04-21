package com.floracare.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.floracare.app.data.notification.NotificationDispatcher
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val current by deepLink.collectAsState()
            FloraCareTheme {
                FloraCareNavHost(
                    deepLinkPlantId = current?.plantId,
                    deepLinkKey = current?.seq,
                    onDeepLinkConsumed = { deepLink.value = null },
                )
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
