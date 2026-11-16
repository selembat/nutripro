package com.nutripro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertHoldGateTest {
    @Test
    fun `fires immediately when safe`() {
        val gate = AlertHoldGate()
        assertTrue(gate.evaluate(nowSec = 0L, baseAlertDue = true, isUnsafe = false, maxHoldSec = 5 * 60L))
    }

    @Test
    fun `does not fire when no alert is due regardless of safety`() {
        val gate = AlertHoldGate()
        assertFalse(gate.evaluate(nowSec = 0L, baseAlertDue = false, isUnsafe = true, maxHoldSec = 5 * 60L))
        assertFalse(gate.evaluate(nowSec = 0L, baseAlertDue = false, isUnsafe = false, maxHoldSec = 5 * 60L))
    }

    @Test
    fun `holds while unsafe and releases once it eases`() {
        val gate = AlertHoldGate()
        assertFalse(gate.evaluate(nowSec = 0L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
        assertFalse(gate.evaluate(nowSec = 60L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
        assertTrue(gate.evaluate(nowSec = 90L, baseAlertDue = true, isUnsafe = false, maxHoldSec = 5 * 60L))
    }

    @Test
    fun `fires anyway once max hold elapses`() {
        val gate = AlertHoldGate()
        assertFalse(gate.evaluate(nowSec = 0L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
        assertFalse(gate.evaluate(nowSec = 299L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
        assertTrue(gate.evaluate(nowSec = 300L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
    }

    @Test
    fun `max hold can change live between calls`() {
        val gate = AlertHoldGate()
        assertFalse(gate.evaluate(nowSec = 0L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 5 * 60L))
        // Rider shortens the hold max in settings mid-hold; it should take effect immediately.
        assertTrue(gate.evaluate(nowSec = 60L, baseAlertDue = true, isUnsafe = true, maxHoldSec = 30L))
    }
}
