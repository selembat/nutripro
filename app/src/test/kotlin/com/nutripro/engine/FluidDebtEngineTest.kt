package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FluidDebtEngineTest {
    @Test
    fun `alert fires once debt reaches one sip block`() {
        val engine = FluidDebtEngine()
        // sweat ~0.75 L/h @ 200W/20C -> drink target 0.525 L/h capped at 1.0 -> 525ml/h.
        // 10 minutes -> ~87.5ml, not yet a sip block.
        engine.tick(dtSec = 10 * 60.0, powerWatts = 200.0, tempC = 20.0)
        assertFalse(engine.isAlertDue(sipBlockMl = 150.0))
        engine.tick(dtSec = 10 * 60.0, powerWatts = 200.0, tempC = 20.0)
        assertTrue(engine.isAlertDue(sipBlockMl = 150.0))
    }

    @Test
    fun `hot ride accrues more fluid debt than cold ride`() {
        val hot = FluidDebtEngine()
        val cold = FluidDebtEngine()
        hot.tick(dtSec = 3600.0, powerWatts = 200.0, tempC = 30.0)
        cold.tick(dtSec = 3600.0, powerWatts = 200.0, tempC = 10.0)
        assertTrue(hot.debtMl > cold.debtMl)
    }

    @Test
    fun `logging water reduces debt and bottle remaining`() {
        val engine = FluidDebtEngine(bottleCount = 2, bottleVolumeMl = 750.0)
        engine.tick(dtSec = 3600.0, powerWatts = 200.0, tempC = 20.0)
        val debtBefore = engine.debtMl
        engine.logWater(150.0)
        assertTrue(engine.debtMl < debtBefore)
        assertTrue(engine.remainingBottleMl < 1500.0)
    }

    @Test
    fun `over-logging leaves a credit instead of discarding the overshoot`() {
        val engine = FluidDebtEngine()
        engine.tick(dtSec = 60.0, powerWatts = 200.0, tempC = 20.0)
        engine.logWater(500.0)
        assertTrue(engine.debtMl < 0.0)
        assertFalse(engine.isAlertDue(sipBlockMl = 150.0))
    }

    @Test
    fun `undo restores exactly what was logged, even over a credit`() {
        val engine = FluidDebtEngine()
        engine.tick(dtSec = 60.0, powerWatts = 200.0, tempC = 20.0)
        val debtBeforeLog = engine.debtMl
        engine.logWater(500.0)
        engine.undoWater(500.0)
        assertEquals(debtBeforeLog, engine.debtMl, 0.001)
    }

    @Test
    fun `bottle nearly empty warning triggers below threshold`() {
        val engine = FluidDebtEngine(bottleCount = 1, bottleVolumeMl = 200.0)
        assertFalse(engine.isBottleNearlyEmpty(lowBottleThresholdMl = 150.0))
        engine.logWater(100.0)
        assertTrue(engine.isBottleNearlyEmpty(lowBottleThresholdMl = 150.0))
    }
}
