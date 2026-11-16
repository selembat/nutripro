package com.nutripro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreClimbNudgeTest {
    private val nudge = PreClimbNudge(maxDistanceM = 3000.0, maxEtaSec = 8 * 60L, minDebtFraction = 0.5)

    @Test
    fun `no nudge when no climb is loaded`() {
        assertFalse(nudge.shouldNudge(climbDistanceM = null, climbEtaSec = null, debtGrams = 20.0, servingGrams = 25.0))
    }

    @Test
    fun `no nudge when climb is far away`() {
        assertFalse(nudge.shouldNudge(climbDistanceM = 5000.0, climbEtaSec = 20 * 60L, debtGrams = 20.0, servingGrams = 25.0))
    }

    @Test
    fun `no nudge when climb is near but debt is low`() {
        assertFalse(nudge.shouldNudge(climbDistanceM = 2000.0, climbEtaSec = null, debtGrams = 5.0, servingGrams = 25.0))
    }

    @Test
    fun `nudges when climb is near and debt is at least half a serving`() {
        assertTrue(nudge.shouldNudge(climbDistanceM = 2000.0, climbEtaSec = null, debtGrams = 13.0, servingGrams = 25.0))
    }

    @Test
    fun `nudges on eta alone when within distance window is unknown`() {
        assertTrue(nudge.shouldNudge(climbDistanceM = null, climbEtaSec = 5 * 60L, debtGrams = 20.0, servingGrams = 25.0))
    }
}
