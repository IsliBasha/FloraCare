package com.floracare.app.domain.repository

import com.floracare.app.domain.model.Coordinates

/**
 * Resolves the device's last-known approximate location for adaptive care.
 *
 * Returns `null` when:
 *  - no location permission has been granted,
 *  - no provider has produced a fix yet,
 *  - location services are disabled.
 *
 * Callers must treat `null` as "skip remote refresh" — never as an error.
 */
interface LocationProvider {
    suspend fun current(): Coordinates?
}
