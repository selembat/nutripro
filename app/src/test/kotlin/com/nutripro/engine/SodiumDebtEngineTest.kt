package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SodiumDebtEngineTest {
    @Test
    fun `debt accrues from sweat rate times sodium concentration`() {
        val engine = SodiumDebtEngine()
        // 0.75 L/h sweat * 800 mg/L = 600 mg/h -> 1 hour = 600mg
        engine.tick(dtSec = 3600.0, sweatRateLph = 0.75, sodiumMgPerL = 800.0)
        assertEquals(600.0, engine.debtMg, 0.001)
    }

    @Test
    fun `higher sweat rate accrues more sodium debt`() {
        val light = SodiumDebtEngine()
        val heavy = SodiumDebtEngine()
        light.tick(dtSec = 3600.0, sweatRateLph = 0.5, sodiumMgPerL = 800.0)
        heavy.tick(dtSec = 3600.0, sweatRateLph = 1.2, sodiumMgPerL = 800.0)
        assertTrue(heavy.debtMg > light.debtMg)
    }

    @Test
    fun `alert fires once debt reaches threshold`() {
        val engine = SodiumDebtEngine()
        engine.tick(dtSec = 3600.0, sweatRateLph = 0.75, sodiumMgPerL = 800.0) // 600mg
        assertFalse(engine.isAlertDue(thresholdMg = 700.0))
        assertTrue(engine.isAlertDue(thresholdMg = 600.0))
    }

    @Test
    fun `logging sodium reduces debt and can go negative as a credit`() {
        val engine = SodiumDebtEngine()
        engine.tick(dtSec = 3600.0, sweatRateLph = 0.75, sodiumMgPerL = 800.0) // 600mg
        engine.logSodium(1000.0)
        assertEquals(-400.0, engine.debtMg, 0.001)
        assertFalse(engine.isAlertDue(thresholdMg = 600.0))
    }

    @Test
    fun `undo restores exactly what was logged`() {
        val engine = SodiumDebtEngine()
        engine.tick(dtSec = 3600.0, sweatRateLph = 0.75, sodiumMgPerL = 800.0)
        val debtBeforeLog = engine.debtMg
        engine.logSodium(500.0)
        engine.undoSodium(500.0)
        assertEquals(debtBeforeLog, engine.debtMg, 0.001)
    }

    @Test
    fun `seed restores persisted debt`() {
        val engine = SodiumDebtEngine()
        engine.seed(debtMg = 250.0)
        assertEquals(250.0, engine.debtMg, 0.0)
    }

    @Test
    fun `reset clears debt back to zero`() {
        val engine = SodiumDebtEngine()
        engine.tick(dtSec = 3600.0, sweatRateLph = 1.0, sodiumMgPerL = 900.0)
        engine.reset()
        assertEquals(0.0, engine.debtMg, 0.0)
    }
}
