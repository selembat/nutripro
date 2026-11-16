package com.nutripro.engine

/**
 * Pure Kotlin hold/release gate (spec 3.4). Holds a due alert while conditions are
 * unsafe, releasing it once they ease — but never holds past [maxHoldSec].
 */
class AlertHoldGate {
    private var holdStartSec: Long? = null

    /** Returns true if the alert should fire now. [maxHoldSec] is read fresh on every call. */
    fun evaluate(nowSec: Long, baseAlertDue: Boolean, isUnsafe: Boolean, maxHoldSec: Long): Boolean {
        if (!baseAlertDue) {
            holdStartSec = null
            return false
        }
        if (!isUnsafe) {
            holdStartSec = null
            return true
        }
        val startedHoldingAt = holdStartSec ?: nowSec.also { holdStartSec = it }
        val heldForSec = nowSec - startedHoldingAt
        return heldForSec >= maxHoldSec
    }

    fun reset() {
        holdStartSec = null
    }
}
