package com.floracare.app.domain.model

import kotlin.math.roundToInt

/**
 * Display unit for temperature values. Internally everything is stored in
 * Celsius (matches the OpenWeather and Species data); this enum only affects
 * presentation. Convert and format in one step via [format].
 */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    /**
     * Formats a Celsius reading for display in this unit.
     * Always rounds to the nearest whole degree and appends °C / °F.
     */
    fun format(celsius: Float): String = when (this) {
        CELSIUS -> "${celsius.roundToInt()}°C"
        FAHRENHEIT -> "${(celsius * 9f / 5f + 32f).roundToInt()}°F"
    }
}
