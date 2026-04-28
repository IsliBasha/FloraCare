package com.floracare.app.widget

import com.floracare.app.domain.repository.PlantRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt accessor for the Glance widget. `GlanceAppWidget` is constructed by
 * the platform — not by Hilt — so we reach into the singleton graph at
 * provideGlance time via [dagger.hilt.android.EntryPointAccessors].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun plantRepository(): PlantRepository
}
