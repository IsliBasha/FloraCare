package com.floracare.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitTest {

    @Test
    fun `Celsius rounds and appends degree symbol`() {
        assertEquals("20°C", TemperatureUnit.CELSIUS.format(19.5f))
        assertEquals("19°C", TemperatureUnit.CELSIUS.format(19.49f))
        assertEquals("0°C", TemperatureUnit.CELSIUS.format(0.0f))
        assertEquals("100°C", TemperatureUnit.CELSIUS.format(100.0f))
    }

    @Test
    fun `Fahrenheit converts from Celsius and rounds`() {
        // 0°C = 32°F, 100°C = 212°F, 19.5°C = 67.1°F → 67°F
        assertEquals("32°F", TemperatureUnit.FAHRENHEIT.format(0.0f))
        assertEquals("212°F", TemperatureUnit.FAHRENHEIT.format(100.0f))
        assertEquals("67°F", TemperatureUnit.FAHRENHEIT.format(19.5f))
        assertEquals("68°F", TemperatureUnit.FAHRENHEIT.format(20.0f))
    }

    @Test
    fun `formats negative temperatures correctly in both units`() {
        assertEquals("-5°C", TemperatureUnit.CELSIUS.format(-4.6f))
        // -10°C = 14°F
        assertEquals("14°F", TemperatureUnit.FAHRENHEIT.format(-10.0f))
        // -40 is the convergence point
        assertEquals("-40°C", TemperatureUnit.CELSIUS.format(-40.0f))
        assertEquals("-40°F", TemperatureUnit.FAHRENHEIT.format(-40.0f))
    }
}
