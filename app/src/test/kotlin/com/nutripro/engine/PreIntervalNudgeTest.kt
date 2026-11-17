package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreIntervalNudgeTest {
    private val nudge = PreIntervalNudge(recoveryZoneIndex = 2, maxTimeToStepFinishSec = 90L, minDebtFraction = 0.5)
    private val zones = listOf(0..120, 121..160, 161..200, 201..240, 241..300)

    @Test
    fun `classifies a value into its 1-based zone`() {
        assertEquals(1, nudge.classifyZone(100.0, zones))
        assertEquals(2, nudge.classifyZone(140.0, zones))
        assertEquals(5, nudge.classifyZone(260.0, zones))
    }

    @Test
    fun `classifies null or out-of-range values as unknown`() {
        assertNull(nudge.classifyZone(null, zones))
        assertNull(nudge.classifyZone(400.0, zones))
    }

    @Test
    fun `no nudge outside zone 2`() {
        assertFalse(nudge.shouldNudge(currentZone = 3, timeToStepFinishSec = 10L, currentStepIndex = 2, totalSteps = 6, debtGrams = 20.0, servingGrams = 25.0))
    }

    @Test
    fun `no nudge when zone 2 step still has plenty of time left`() {
        assertFalse(nudge.shouldNudge(currentZone = 2, timeToStepFinishSec = 200L, currentStepIndex = 2, totalSteps = 6, debtGrams = 20.0, servingGrams = 25.0))
    }

    @Test
    fun `no nudge on the workout's last step even if it is zone 2 and ending`() {
        assertFalse(nudge.shouldNudge(currentZone = 2, timeToStepFinishSec = 10L, currentStepIndex = 6, totalSteps = 6, debtGrams = 20.0, servingGrams = 25.0))
    }

    @Test
    fun `no nudge when debt is below half a serving`() {
        assertFalse(nudge.shouldNudge(currentZone = 2, timeToStepFinishSec = 10L, currentStepIndex = 2, totalSteps = 6, debtGrams = 5.0, servingGrams = 25.0))
    }

    @Test
    fun `nudges in a zone 2 step ending soon, with debt built up, before the last step`() {
        assertTrue(nudge.shouldNudge(currentZone = 2, timeToStepFinishSec = 45L, currentStepIndex = 2, totalSteps = 6, debtGrams = 13.0, servingGrams = 25.0))
    }
}
