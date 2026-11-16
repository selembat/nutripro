package com.nutripro.engine

/**
 * Pure Kotlin fluid-debt engine (spec 3.2). No Android dependencies.
 *
 * Drink target is a fraction of estimated sweat loss, capped at a max rate.
 * Tracks bottle volume so it can warn when a refill is needed. Fluid debt can go
 * negative (a credit) if a drink overshoots what's currently owed, rather than
 * silently discarding the overshoot.
 */
class FluidDebtEngine(
    private val drinkTargetFraction: Double = 0.70,
    private val maxDrinkTargetLph: Double = 1.0,
    bottleCount: Int = 2,
    bottleVolumeMl: Double = 750.0,
) {
    companion object {
        const val DEFAULT_LOW_BOTTLE_THRESHOLD_ML = 150.0
    }

    var debtMl: Double = 0.0
        private set
    var remainingBottleMl: Double = bottleCount * bottleVolumeMl
        private set

    /** Advances the engine by [dtSec] seconds given current [powerWatts] and [tempC]. */
    fun tick(dtSec: Double, powerWatts: Double, tempC: Double, personalMultiplier: Double = 1.0) {
        require(dtSec >= 0.0)
        val sweatLph = SweatModel.sweatRateLph(powerWatts, tempC, personalMultiplier)
        val drinkTargetLph = (sweatLph * drinkTargetFraction).coerceAtMost(maxDrinkTargetLph)
        debtMl += drinkTargetLph * 1000.0 / 3600.0 * dtSec
    }

    /** Reduces debt by a logged water intake; may go negative if it overshoots what's owed. */
    fun logWater(ml: Double) {
        require(ml >= 0.0)
        debtMl -= ml
        remainingBottleMl = (remainingBottleMl - ml).coerceAtLeast(0.0)
    }

    /** Reverses a previously logged water intake, e.g. on undo. */
    fun undoWater(ml: Double) {
        require(ml >= 0.0)
        debtMl += ml
        remainingBottleMl += ml
    }

    /** Restores previously-persisted state, e.g. after the extension process restarts mid-ride. */
    fun seed(debtMl: Double, remainingBottleMl: Double) {
        this.debtMl = debtMl
        this.remainingBottleMl = remainingBottleMl
    }

    /** [sipBlockMl] is read fresh on every call. */
    fun isAlertDue(sipBlockMl: Double): Boolean = debtMl >= sipBlockMl

    /** [lowBottleThresholdMl] is read fresh on every call. */
    fun isBottleNearlyEmpty(lowBottleThresholdMl: Double = DEFAULT_LOW_BOTTLE_THRESHOLD_ML): Boolean =
        remainingBottleMl <= lowBottleThresholdMl

    fun reset(bottleCount: Int, bottleVolumeMl: Double) {
        debtMl = 0.0
        remainingBottleMl = bottleCount * bottleVolumeMl
    }
}
