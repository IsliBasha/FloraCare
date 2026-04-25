package com.floracare.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.floracare.app.domain.model.Coordinates
import com.floracare.app.domain.repository.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's last-known location via the platform [LocationManager].
 *
 * No new dependency: avoids `play-services-location` to keep the APK small
 * — the daily care scheduler runs at most once per day, so a recent passive
 * fix is good enough. Callers must handle `null` as "skip weather refresh".
 *
 * Permission gating: returns `null` if neither COARSE nor FINE has been
 * granted, even though the manifest declares both — a paranoid runtime
 * check protects against malformed install state.
 */
@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    override suspend fun current(): Coordinates? {
        if (!hasAnyLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val candidates = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )

        val best: Location? = candidates
            .mapNotNull { provider -> safeLastKnown(manager, provider) }
            .maxByOrNull { it.time }

        return best?.let { Coordinates(lat = it.latitude, lon = it.longitude) }
    }

    private fun hasAnyLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    private fun safeLastKnown(manager: LocationManager, provider: String): Location? = try {
        manager.getLastKnownLocation(provider)
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
