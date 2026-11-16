package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SweatCalibratorTest {
    @Test
    fun `actual sweat loss combines weight lost and fluid drunk`() {
        // Lost 0.8kg, drank 500ml -> total fluid lost = 800 + 500 = 1300ml
        val actual = SweatCalibrator.computeActualSweatLossMl(
            preWeightKg = 70.0,
            postWeightKg = 69.2,
            fluidDrunkMl = 500.0,
        )
        assertEquals(1300.0, actual, 0.001)
    }

    @Test
    fun `actual sweat loss never goes negative`() {
        val actual = SweatCalibrator.computeActualSweatLossMl(
            preWeightKg = 70.0,
            postWeightKg = 70.5, // gained weight somehow
            fluidDrunkMl = 0.0,
        )
        assertEquals(0.0, actual, 0.0)
    }

    @Test
    fun `multiplier increases when actual loss exceeds modeled loss`() {
        val updated = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.0,
            modeledSweatLossMl = 1000.0,
            actualSweatLossMl = 1300.0,
            smoothing = 1.0, // no smoothing, so this isolates the raw correction
        )
        assertEquals(1.3, updated, 0.001)
    }

    @Test
    fun `multiplier decreases when actual loss is below modeled loss`() {
        val updated = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.0,
            modeledSweatLossMl = 1000.0,
            actualSweatLossMl = 700.0,
            smoothing = 1.0,
        )
        assertEquals(0.7, updated, 0.001)
    }

    @Test
    fun `smoothing blends the observed correction as a rolling average`() {
        val updated = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.0,
            modeledSweatLossMl = 1000.0,
            actualSweatLossMl = 1300.0,
            smoothing = 0.3,
        )
        // observed=1.3, blended = 1.0*0.7 + 1.3*0.3 = 1.09
        assertEquals(1.09, updated, 0.001)
    }

    @Test
    fun `multiplier is clamped to sane bounds`() {
        val tooHigh = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.0,
            modeledSweatLossMl = 100.0,
            actualSweatLossMl = 10_000.0,
            smoothing = 1.0,
        )
        assertEquals(2.0, tooHigh, 0.0)

        val tooLow = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.0,
            modeledSweatLossMl = 10_000.0,
            actualSweatLossMl = 1.0,
            smoothing = 1.0,
        )
        assertEquals(0.5, tooLow, 0.0)
    }

    @Test
    fun `zero modeled loss leaves multiplier unchanged to avoid divide-by-zero`() {
        val updated = SweatCalibrator.updateMultiplier(
            currentMultiplier = 1.2,
            modeledSweatLossMl = 0.0,
            actualSweatLossMl = 500.0,
        )
        assertEquals(1.2, updated, 0.0)
    }
}
