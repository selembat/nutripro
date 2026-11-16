package com.nutripro.engine

enum class TemperatureSource { WEATHER, DEVICE_SENSOR, MANUAL }

/**
 * Pure Kotlin temperature fallback chain (spec 3.3): weather API -> live local sensor
 * (the Karoo's own ambient sensor, or a BLE one) -> manual entry.
 */
object TemperatureFallback {
    fun resolve(
        weatherTempC: Double?,
        weatherAgeMs: Long?,
        maxWeatherAgeMs: Long,
        sensorTempC: Double?,
        manualTempC: Double,
    ): Pair<Double, TemperatureSource> {
        if (weatherTempC != null && weatherAgeMs != null && weatherAgeMs in 0..maxWeatherAgeMs) {
            return weatherTempC to TemperatureSource.WEATHER
        }
        if (sensorTempC != null) {
            return sensorTempC to TemperatureSource.DEVICE_SENSOR
        }
        return manualTempC to TemperatureSource.MANUAL
    }
}
