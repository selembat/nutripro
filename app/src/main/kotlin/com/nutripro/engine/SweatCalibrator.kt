package com.nutripro.engine

/**
 * Pure Kotlin sweat-rate calibration (spec 3.7): turns a pre/post-ride weigh-in into a
 * correction factor for the sweat model, blended into the rider's personal multiplier as a
 * rolling average so one noisy ride doesn't swing it too far.
 */
object SweatCalibrator {
    private const val MIN_MULTIPLIER = 0.5
    private const val MAX_MULTIPLIER = 2.0

    /**
     * Actual fluid lost during the ride: body-mass lost (1 kg ~= 1000 ml of water) plus
     * whatever was drunk to offset it.
     */
    fun computeActualSweatLossMl(preWeightKg: Double, postWeightKg: Double, fluidDrunkMl: Double): Double =
        ((preWeightKg - postWeightKg) * 1000.0 + fluidDrunkMl).coerceAtLeast(0.0)

    /**
     * Blends a new observed correction into [currentMultiplier] as a rolling average, weighted
     * by [smoothing] (0 = ignore this ride, 1 = replace the multiplier outright).
     */
    fun updateMultiplier(
        currentMultiplier: Double,
        modeledSweatLossMl: Double,
        actualSweatLossMl: Double,
        smoothing: Double = 0.3,
    ): Double {
        if (modeledSweatLossMl <= 0.0) return currentMultiplier
        val observedMultiplier = currentMultiplier * (actualSweatLossMl / modeledSweatLossMl)
        val blended = currentMultiplier * (1.0 - smoothing) + observedMultiplier * smoothing
        return blended.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
    }
}
