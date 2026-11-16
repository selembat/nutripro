package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarbDebtEngineTest {
    // A planSec long enough that DurationGate never caps the intensity curve - these tests
    // are about the intensity curve/alerting/logging, not the duration gate (see
    // DurationGateTest and the dedicated gate-integration tests below for that).
    private val longRideSec = 3 * 3600L

    @Test
    fun `no alert before start delay even if debt exceeds serving`() {
        val engine = CarbDebtEngine()
        // 90 g/h for just under 20 minutes = ~29.5g debt, well past one serving.
        engine.tick(dtSec = 20 * 60.0 - 1.0, intensityPercent = 110.0, planSec = longRideSec)
        assertTrue(engine.debtGrams >= 25.0)
        assertFalse(
            "alert should be suppressed before the start delay elapses",
            engine.isAlertDue(servingGrams = 25.0, startDelaySec = 20 * 60L),
        )
    }

    @Test
    fun `alert fires once debt reaches one serving after start delay`() {
        val engine = CarbDebtEngine()
        // 45 g/h band (55-74% FTP) for 30 min = 22.5g -- not yet a serving.
        engine.tick(dtSec = 30 * 60.0, intensityPercent = 65.0, planSec = longRideSec)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
        // 10 more minutes pushes debt past 25g.
        engine.tick(dtSec = 10 * 60.0, intensityPercent = 65.0, planSec = longRideSec)
        assertTrue(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `logging intake reduces debt and clears alert`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 3600.0, intensityPercent = 65.0, planSec = longRideSec) // 45g debt
        assertTrue(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))

        engine.logIntake(25.0)
        assertEquals(20.0, engine.debtGrams, 0.001)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `over-logging leaves a credit instead of discarding the overshoot`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 60.0, intensityPercent = 65.0, planSec = longRideSec) // 0.75g debt
        engine.logIntake(25.0)
        assertEquals(0.75 - 25.0, engine.debtGrams, 0.001)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `undo restores exactly what was logged, even over a credit`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 60.0, intensityPercent = 65.0, planSec = longRideSec) // 0.75g debt
        val debtBeforeLog = engine.debtGrams
        engine.logIntake(25.0)
        engine.undoIntake(25.0)
        assertEquals(debtBeforeLog, engine.debtGrams, 0.001)
    }

    @Test
    fun `higher intensity accrues debt faster than lower intensity`() {
        val easy = CarbDebtEngine()
        val hard = CarbDebtEngine()
        easy.tick(dtSec = 3600.0, intensityPercent = 40.0, planSec = longRideSec) // < 55% band -> 30 g/h
        hard.tick(dtSec = 3600.0, intensityPercent = 110.0, planSec = longRideSec) // >= 105% band -> 90 g/h
        assertTrue(hard.debtGrams > easy.debtGrams)
    }

    @Test
    fun `short planned ride accrues no debt even at high intensity`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 45 * 60.0, intensityPercent = 110.0, planSec = 45 * 60L)
        assertEquals(0.0, engine.debtGrams, 0.0)
    }

    @Test
    fun `medium planned ride caps accrual even during a hard effort`() {
        val engine = CarbDebtEngine()
        // 90 g/h target capped at 45 g/h for 1 hour = 45g, not 90g.
        engine.tick(dtSec = 3600.0, intensityPercent = 110.0, planSec = 90 * 60L)
        assertEquals(45.0, engine.debtGrams, 0.001)
    }

    @Test
    fun `a known long route unlocks the full curve immediately, not just after elapsed time`() {
        val engine = CarbDebtEngine()
        // 5 minutes into a route whose live ETA already says 4 hours total: full curve now.
        engine.tick(dtSec = 5 * 60.0, intensityPercent = 110.0, planSec = 4 * 3600L)
        assertEquals(90.0 / 12.0, engine.debtGrams, 0.001)
    }
}
