package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarbDebtEngineTest {
    @Test
    fun `no alert before start delay even if debt exceeds serving`() {
        val engine = CarbDebtEngine()
        // 90 g/h for just under 20 minutes = ~29.5g debt, well past one serving.
        engine.tick(dtSec = 20 * 60.0 - 1.0, intensityPercent = 110.0)
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
        engine.tick(dtSec = 30 * 60.0, intensityPercent = 65.0)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
        // 10 more minutes pushes debt past 25g.
        engine.tick(dtSec = 10 * 60.0, intensityPercent = 65.0)
        assertTrue(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `logging intake reduces debt and clears alert`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 3600.0, intensityPercent = 65.0) // 45g debt
        assertTrue(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))

        engine.logIntake(25.0)
        assertEquals(20.0, engine.debtGrams, 0.001)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `over-logging leaves a credit instead of discarding the overshoot`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 60.0, intensityPercent = 65.0) // 0.75g debt
        engine.logIntake(25.0)
        assertEquals(0.75 - 25.0, engine.debtGrams, 0.001)
        assertFalse(engine.isAlertDue(servingGrams = 25.0, startDelaySec = 0L))
    }

    @Test
    fun `undo restores exactly what was logged, even over a credit`() {
        val engine = CarbDebtEngine()
        engine.tick(dtSec = 60.0, intensityPercent = 65.0) // 0.75g debt
        val debtBeforeLog = engine.debtGrams
        engine.logIntake(25.0)
        engine.undoIntake(25.0)
        assertEquals(debtBeforeLog, engine.debtGrams, 0.001)
    }

    @Test
    fun `higher intensity accrues debt faster than lower intensity`() {
        val easy = CarbDebtEngine()
        val hard = CarbDebtEngine()
        easy.tick(dtSec = 3600.0, intensityPercent = 40.0) // < 55% band -> 30 g/h
        hard.tick(dtSec = 3600.0, intensityPercent = 110.0) // >= 105% band -> 90 g/h
        assertTrue(hard.debtGrams > easy.debtGrams)
    }
}
