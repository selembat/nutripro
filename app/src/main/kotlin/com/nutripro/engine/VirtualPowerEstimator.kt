package com.nutripro.engine

import kotlin.math.pow

/**
 * Pure Kotlin "virtual power" estimate from speed and grade (spec 3.1 extension): when
 * there's no power meter or HR strap at all, speed + grade + rider weight is still enough
 * to estimate roughly how hard the rider is working, which beats a flat assumption.
 *
 * P = (rolling resistance + gravity + aero drag) * speed / drivetrain efficiency.
 * Coasting/descending can yield a negative estimate, which is clamped to 0 - we're
 * estimating pedaling effort, not deceleration.
 */
object VirtualPowerEstimator {
    private const val GRAVITY = 9.80665
    private const val DEFAULT_ROLLING_RESISTANCE = 0.005
    private const val DEFAULT_DRAG_AREA_M2 = 0.32
    private const val DEFAULT_AIR_DENSITY_KG_M3 = 1.225
    private const val DEFAULT_DRIVETRAIN_EFFICIENCY = 0.97

    fun estimateWatts(
        speedMps: Double,
        gradePercent: Double,
        totalWeightKg: Double,
        rollingResistance: Double = DEFAULT_ROLLING_RESISTANCE,
        dragAreaM2: Double = DEFAULT_DRAG_AREA_M2,
        airDensityKgM3: Double = DEFAULT_AIR_DENSITY_KG_M3,
        drivetrainEfficiency: Double = DEFAULT_DRIVETRAIN_EFFICIENCY,
    ): Double {
        if (speedMps <= 0.0 || totalWeightKg <= 0.0) return 0.0
        val gradeFraction = gradePercent / 100.0

        val rollingForce = rollingResistance * totalWeightKg * GRAVITY
        val gravityForce = totalWeightKg * GRAVITY * gradeFraction
        val aeroForce = 0.5 * dragAreaM2 * airDensityKgM3 * speedMps.pow(2)

        val watts = (rollingForce + gravityForce + aeroForce) * speedMps / drivetrainEfficiency
        return watts.coerceAtLeast(0.0)
    }
}
