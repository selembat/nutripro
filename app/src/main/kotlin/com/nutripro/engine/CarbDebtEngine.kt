package com.nutripro.engine

/**
 * Pure Kotlin carb-debt engine (spec 3.1). No Android dependencies.
 *
 * Debt accrues at the target *intake* rate for the current intensity (not a 1:1
 * burn-replacement rate — the gut caps absorption well below burn rate at high
 * intensity). Logging an intake reduces the debt directly, and can go negative
 * (a credit) if the intake overshoots what's currently owed — that credit is real
 * and gets absorbed as debt accrues again, rather than being silently discarded.
 */
class CarbDebtEngine {
    var debtGrams: Double = 0.0
        private set
    var elapsedSec: Long = 0L
        private set

    /**
     * Advances the engine by [dtSec] seconds at the given [intensityPercent] (%FTP or %maxHR).
     * [planSec] is the best available ride-duration estimate (route ETA, a manually chosen
     * plan, or elapsed time as a last resort) - short/unplanned rides don't need exogenous
     * carbs at all, so [DurationGate] caps the intensity-based target until the ride is
     * actually (or knowingly going to be) long enough to need them, unless [modeConfirmed]
     * (the rider has explicitly confirmed a ride mode this ride) overrides that inference.
     * [isRaceMode] raises the top intensity band's ceiling (spec 3.1 extension).
     */
    fun tick(dtSec: Double, intensityPercent: Double, planSec: Long, modeConfirmed: Boolean = false, isRaceMode: Boolean = false) {
        require(dtSec >= 0.0)
        elapsedSec += dtSec.toLong()
        val intensityTargetGph = IntensityZone.getRecommendedIntakeGph(intensityPercent, isRaceMode)
        val targetGph = DurationGate.capGph(intensityTargetGph, planSec, modeConfirmed)
        debtGrams += targetGph / 3600.0 * dtSec
    }

    /** Reduces debt by a logged intake; may go negative if it overshoots what's owed. */
    fun logIntake(grams: Double) {
        require(grams >= 0.0)
        debtGrams -= grams
    }

    /** Reverses a previously logged intake, e.g. on undo. */
    fun undoIntake(grams: Double) {
        require(grams >= 0.0)
        debtGrams += grams
    }

    /** True once past the start delay and debt has reached at least one serving. [servingGrams]/[startDelaySec] are read fresh on every call. */
    fun isAlertDue(servingGrams: Double, startDelaySec: Long): Boolean =
        elapsedSec >= startDelaySec && debtGrams >= servingGrams

    fun reset() {
        debtGrams = 0.0
        elapsedSec = 0L
    }

    /** Restores previously-persisted state, e.g. after the extension process restarts mid-ride. */
    fun seed(debtGrams: Double, elapsedSec: Long) {
        this.debtGrams = debtGrams
        this.elapsedSec = elapsedSec
    }
}
