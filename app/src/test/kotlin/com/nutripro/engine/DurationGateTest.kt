package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationGateTest {
    @Test
    fun `under one hour needs no exogenous carbs regardless of intensity`() {
        assertEquals(0.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 30 * 60L), 0.0)
        assertEquals(0.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 59 * 60L), 0.0)
    }

    @Test
    fun `one to two hours caps at 45 even during hard efforts`() {
        assertEquals(45.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 70 * 60L), 0.0)
        // A low intensity target under the cap passes through unchanged.
        assertEquals(30.0, DurationGate.capGph(intensityTargetGph = 30.0, planSec = 90 * 60L), 0.0)
    }

    @Test
    fun `past two hours unlocks the full intensity curve`() {
        assertEquals(90.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 121 * 60L), 0.0)
    }

    @Test
    fun `boundaries are inclusive at the lower edge of the next bracket`() {
        assertEquals(45.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 60 * 60L), 0.0)
        assertEquals(90.0, DurationGate.capGph(intensityTargetGph = 90.0, planSec = 120 * 60L), 0.0)
    }
}
