package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureFallbackTest {
    @Test
    fun `uses weather when fresh`() {
        val (temp, source) = TemperatureFallback.resolve(
            weatherTempC = 22.0,
            weatherAgeMs = 5 * 60_000L,
            maxWeatherAgeMs = 20 * 60_000L,
            sensorTempC = 18.0,
            manualTempC = 15.0,
        )
        assertEquals(22.0, temp, 0.0)
        assertEquals(TemperatureSource.WEATHER, source)
    }

    @Test
    fun `falls back to the device sensor when weather is stale`() {
        val (temp, source) = TemperatureFallback.resolve(
            weatherTempC = 22.0,
            weatherAgeMs = 21 * 60_000L,
            maxWeatherAgeMs = 20 * 60_000L,
            sensorTempC = 18.0,
            manualTempC = 15.0,
        )
        assertEquals(18.0, temp, 0.0)
        assertEquals(TemperatureSource.DEVICE_SENSOR, source)
    }

    @Test
    fun `falls back to the device sensor when weather is missing`() {
        val (temp, source) = TemperatureFallback.resolve(
            weatherTempC = null,
            weatherAgeMs = null,
            maxWeatherAgeMs = 20 * 60_000L,
            sensorTempC = 18.0,
            manualTempC = 15.0,
        )
        assertEquals(18.0, temp, 0.0)
        assertEquals(TemperatureSource.DEVICE_SENSOR, source)
    }

    @Test
    fun `falls back to manual when nothing else is available`() {
        val (temp, source) = TemperatureFallback.resolve(
            weatherTempC = null,
            weatherAgeMs = null,
            maxWeatherAgeMs = 20 * 60_000L,
            sensorTempC = null,
            manualTempC = 15.0,
        )
        assertEquals(15.0, temp, 0.0)
        assertEquals(TemperatureSource.MANUAL, source)
    }

    @Test
    fun `negative weather age is treated as invalid`() {
        val (temp, source) = TemperatureFallback.resolve(
            weatherTempC = 22.0,
            weatherAgeMs = -1L,
            maxWeatherAgeMs = 20 * 60_000L,
            sensorTempC = null,
            manualTempC = 15.0,
        )
        assertEquals(15.0, temp, 0.0)
        assertEquals(TemperatureSource.MANUAL, source)
    }
}
