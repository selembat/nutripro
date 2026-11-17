package com.nutripro.engine

/**
 * Pure Kotlin pre-interval nudge (spec 3.1 extension, Workout mode): "Interval ending soon -
 * eat now" while the rider is in a Z2 recovery/active-recovery step that's about to end -
 * workouts always begin and end in Z2 and never use Z1, so Z2-ending is the honest signal
 * the Karoo workout stream actually supports for "something harder is likely next" (there is
 * no API to read the *next* step's target). Mirrors the shape of the route-based [PreClimbNudge].
 */
class PreIntervalNudge(
    private val recoveryZoneIndex: Int = 2,
    private val maxTimeToStepFinishSec: Long = 90L,
    private val minDebtFraction: Double = 0.5,
) {
    /** 1-based zone index (1 = Z1) for [targetValue] within [zones], or null if unknown/out of range. */
    fun classifyZone(targetValue: Double?, zones: List<IntRange>): Int? {
        if (targetValue == null) return null
        zones.forEachIndexed { index, range -> if (targetValue.toInt() in range) return index + 1 }
        return null
    }

    /**
     * [currentStepIndex]/[totalSteps] are 1-based (Karoo's WORKOUT_CURRENT_STEP/WORKOUT_STEP_COUNT);
     * the last step of the workout (the closing Z2 cooldown) never nudges, since nothing harder
     * follows it.
     */
    fun shouldNudge(
        currentZone: Int?,
        timeToStepFinishSec: Long?,
        currentStepIndex: Int,
        totalSteps: Int,
        debtGrams: Double,
        servingGrams: Double,
    ): Boolean {
        if (currentZone != recoveryZoneIndex) return false
        if (timeToStepFinishSec == null || timeToStepFinishSec > maxTimeToStepFinishSec) return false
        if (totalSteps <= 0 || currentStepIndex >= totalSteps) return false
        return debtGrams >= servingGrams * minDebtFraction
    }
}
