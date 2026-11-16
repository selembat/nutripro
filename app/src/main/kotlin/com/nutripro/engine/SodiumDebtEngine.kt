package com.nutripro.engine

/**
 * Pure Kotlin sodium-debt engine. No Android dependencies.
 *
 * Sodium loss tracks actual sweat loss (not the fraction targeted for drinking back), since
 * sodium leaves the body with sweat regardless of how much fluid is replaced:
 * sodium debt (mg/hr) = sweat rate (L/hr) x sweat sodium concentration (mg/L).
 *
 * Debt can go negative (a credit) if logged intake overshoots what's currently owed, rather
 * than discarding the overshoot.
 */
class SodiumDebtEngine {
    var debtMg: Double = 0.0
        private set

    /** Advances the engine by [dtSec] seconds given the current [sweatRateLph] and [sodiumMgPerL]. */
    fun tick(dtSec: Double, sweatRateLph: Double, sodiumMgPerL: Double) {
        require(dtSec >= 0.0)
        val sodiumMgPerHour = sweatRateLph * sodiumMgPerL
        debtMg += sodiumMgPerHour / 3600.0 * dtSec
    }

    /** Reduces debt by a logged sodium intake; may go negative if it overshoots what's owed. */
    fun logSodium(mg: Double) {
        require(mg >= 0.0)
        debtMg -= mg
    }

    /** Reverses a previously logged sodium intake, e.g. on undo. */
    fun undoSodium(mg: Double) {
        require(mg >= 0.0)
        debtMg += mg
    }

    /** [thresholdMg] is read fresh on every call. */
    fun isAlertDue(thresholdMg: Double): Boolean = debtMg >= thresholdMg

    fun reset() {
        debtMg = 0.0
    }

    /** Restores previously-persisted state, e.g. after the extension process restarts mid-ride. */
    fun seed(debtMg: Double) {
        this.debtMg = debtMg
    }
}
