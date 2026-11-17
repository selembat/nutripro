package com.nutripro.engine

/**
 * Pure Kotlin duration gate for carb targets: muscle glycogen alone covers roughly the
 * first hour of riding, so a high intensity-based carb target is only appropriate once the
 * ride (planned or elapsed) is actually long enough to need exogenous carbs at all.
 *
 * [planSec] is whichever duration estimate is best available: a loaded route's live
 * elapsed+ETA, a manually entered planned ride length, or — lacking either — the ride's
 * own elapsed time so far (which lets the target ramp up naturally the longer a ride runs).
 *
 * [modeConfirmed] - true once the rider has explicitly confirmed a ride mode this ride
 * (spec 3.1 extension). A confirmed mode *is* the rider's stated intent, so it overrides
 * the duration-based inference entirely rather than being layered on top of it - a 45-minute
 * confirmed Workout still gets the full intensity-based target, not zeroed by the clock.
 */
object DurationGate {
    private const val NO_CARBS_NEEDED_SEC = 60 * 60L
    private const val FULL_CURVE_UNLOCKS_SEC = 120 * 60L
    private const val MODERATE_CAP_GPH = 45.0

    fun capGph(intensityTargetGph: Double, planSec: Long, modeConfirmed: Boolean = false): Double {
        if (modeConfirmed) return intensityTargetGph
        return when {
            planSec < NO_CARBS_NEEDED_SEC -> 0.0
            planSec < FULL_CURVE_UNLOCKS_SEC -> intensityTargetGph.coerceAtMost(MODERATE_CAP_GPH)
            else -> intensityTargetGph
        }
    }
}
