package com.nutripro.engine

/**
 * Pure Kotlin alert-firing decision (spec 3.4): combines [AlertHoldGate] (hold during unsafe
 * conditions) with [ReRemindTracker] (nag again if ignored) into a single edge-triggered
 * channel. Fires once per "due" occurrence — not on every tick it stays due — then only
 * fires again on the re-remind interval until the underlying condition clears.
 */
class AlertChannel {
    private val holdGate = AlertHoldGate()
    private val reRemind = ReRemindTracker()
    private var active = false

    /** Returns true if the alert should fire now. */
    fun evaluate(nowSec: Long, baseDue: Boolean, isUnsafe: Boolean, maxHoldSec: Long, reRemindAfterSec: Long): Boolean {
        if (!baseDue) {
            active = false
            holdGate.reset()
            return false
        }
        if (!active) {
            val released = holdGate.evaluate(nowSec, baseAlertDue = true, isUnsafe = isUnsafe, maxHoldSec = maxHoldSec)
            if (!released) return false
            active = true
            reRemind.onAlertFired(nowSec)
            return true
        }
        if (!reRemind.isReRemindDue(nowSec, reRemindAfterSec)) return false
        reRemind.onAlertFired(nowSec)
        return true
    }

    fun onIntakeLogged() {
        reRemind.onIntakeLogged()
    }

    fun reset() {
        active = false
        holdGate.reset()
    }
}
