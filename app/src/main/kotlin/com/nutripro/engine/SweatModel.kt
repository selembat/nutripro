package com.nutripro.engine

/** Pure sweat-rate model (spec 3.2), to be calibrated per rider. */
object SweatModel {
    private const val BASE_L_PER_H = 0.25
    private const val POWER_COEFFICIENT = 0.0025
    private const val REFERENCE_TEMP_C = 20.0
    private const val TEMP_COEFFICIENT = 0.03
    private const val TEMP_FACTOR_MIN = 0.5
    private const val TEMP_FACTOR_MAX = 1.8

    fun tempFactor(tempC: Double): Double =
        (1.0 + TEMP_COEFFICIENT * (tempC - REFERENCE_TEMP_C)).coerceIn(TEMP_FACTOR_MIN, TEMP_FACTOR_MAX)

    /** Estimated sweat loss in L/h, optionally scaled by a rider's personal calibration multiplier. */
    fun sweatRateLph(powerWatts: Double, tempC: Double, personalMultiplier: Double = 1.0): Double =
        (BASE_L_PER_H + POWER_COEFFICIENT * powerWatts) * tempFactor(tempC) * personalMultiplier
}
