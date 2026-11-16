package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualPowerEstimatorTest {
    @Test
    fun `stationary rider produces zero estimated power`() {
        val watts = VirtualPowerEstimator.estimateWatts(speedMps = 0.0, gradePercent = 0.0, totalWeightKg = 85.0)
        assertEquals(0.0, watts, 0.0)
    }

    @Test
    fun `faster flat-ground speed estimates more power`() {
        val slow = VirtualPowerEstimator.estimateWatts(speedMps = 5.0, gradePercent = 0.0, totalWeightKg = 85.0)
        val fast = VirtualPowerEstimator.estimateWatts(speedMps = 10.0, gradePercent = 0.0, totalWeightKg = 85.0)
        assertTrue(fast > slow)
    }

    @Test
    fun `climbing at the same speed estimates more power than flat ground`() {
        val flat = VirtualPowerEstimator.estimateWatts(speedMps = 5.0, gradePercent = 0.0, totalWeightKg = 85.0)
        val climbing = VirtualPowerEstimator.estimateWatts(speedMps = 5.0, gradePercent = 8.0, totalWeightKg = 85.0)
        assertTrue(climbing > flat)
    }

    @Test
    fun `steep descent never goes negative`() {
        val watts = VirtualPowerEstimator.estimateWatts(speedMps = 15.0, gradePercent = -10.0, totalWeightKg = 85.0)
        assertTrue(watts >= 0.0)
    }

    @Test
    fun `heavier rider plus bike estimates more power at the same speed and grade`() {
        val light = VirtualPowerEstimator.estimateWatts(speedMps = 6.0, gradePercent = 3.0, totalWeightKg = 70.0)
        val heavy = VirtualPowerEstimator.estimateWatts(speedMps = 6.0, gradePercent = 3.0, totalWeightKg = 95.0)
        assertTrue(heavy > light)
    }

    @Test
    fun `rough magnitude check against a plausible real-world value`() {
        // ~90kg rider+bike at ~7 m/s (25 km/h) on a flat road is a moderate ~150-250W effort.
        val watts = VirtualPowerEstimator.estimateWatts(speedMps = 7.0, gradePercent = 0.0, totalWeightKg = 90.0)
        assertTrue("estimated $watts W out of plausible range", watts in 80.0..300.0)
    }
}
