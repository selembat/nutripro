package com.nutripro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsafeConditionDetectorTest {
    private val detector = UnsafeConditionDetector()

    @Test
    fun `flat moderate effort is safe`() {
        assertFalse(detector.isUnsafe(power30sAvgWatts = 180.0, ftpWatts = 250.0, speedMps = 8.0, gradePercent = 0.0))
    }

    @Test
    fun `hard climb over ftp threshold is unsafe`() {
        assertTrue(detector.isUnsafe(power30sAvgWatts = 270.0, ftpWatts = 250.0, speedMps = 6.0, gradePercent = 8.0))
    }

    @Test
    fun `fast descent is unsafe even at low power`() {
        assertTrue(detector.isUnsafe(power30sAvgWatts = 50.0, ftpWatts = 250.0, speedMps = 15.0, gradePercent = -5.0))
    }

    @Test
    fun `fast speed on flat ground is safe`() {
        assertFalse(detector.isUnsafe(power30sAvgWatts = 50.0, ftpWatts = 250.0, speedMps = 15.0, gradePercent = 0.0))
    }
}
