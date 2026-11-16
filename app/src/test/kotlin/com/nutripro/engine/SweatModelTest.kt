package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SweatModelTest {
    @Test
    fun `matches spec worked examples`() {
        assertEquals(0.75, SweatModel.sweatRateLph(powerWatts = 200.0, tempC = 20.0), 0.01)
        assertEquals(0.98, SweatModel.sweatRateLph(powerWatts = 200.0, tempC = 30.0), 0.01)
        assertEquals(0.53, SweatModel.sweatRateLph(powerWatts = 200.0, tempC = 10.0), 0.01)
    }

    @Test
    fun `temp factor is clamped at the extremes`() {
        assertEquals(0.5, SweatModel.tempFactor(tempC = -50.0), 0.0)
        assertEquals(1.8, SweatModel.tempFactor(tempC = 100.0), 0.0)
    }

    @Test
    fun `personal multiplier scales the estimate linearly`() {
        val base = SweatModel.sweatRateLph(powerWatts = 200.0, tempC = 20.0)
        val scaled = SweatModel.sweatRateLph(powerWatts = 200.0, tempC = 20.0, personalMultiplier = 1.2)
        assertEquals(base * 1.2, scaled, 0.0001)
    }
}
