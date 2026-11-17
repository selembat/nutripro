package com.nutripro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class IntensityZoneTest {
    @Test
    fun `band transitions match spec table`() {
        assertEquals(30.0, IntensityZone.getRecommendedIntakeGph(0.0), 0.0)
        assertEquals(30.0, IntensityZone.getRecommendedIntakeGph(54.9), 0.0)
        assertEquals(45.0, IntensityZone.getRecommendedIntakeGph(55.0), 0.0)
        assertEquals(45.0, IntensityZone.getRecommendedIntakeGph(74.9), 0.0)
        assertEquals(60.0, IntensityZone.getRecommendedIntakeGph(75.0), 0.0)
        assertEquals(60.0, IntensityZone.getRecommendedIntakeGph(89.9), 0.0)
        assertEquals(75.0, IntensityZone.getRecommendedIntakeGph(90.0), 0.0)
        assertEquals(75.0, IntensityZone.getRecommendedIntakeGph(104.9), 0.0)
        assertEquals(90.0, IntensityZone.getRecommendedIntakeGph(105.0), 0.0)
        assertEquals(90.0, IntensityZone.getRecommendedIntakeGph(200.0), 0.0)
    }

    @Test
    fun `race mode raises only the top band's ceiling`() {
        assertEquals(115.0, IntensityZone.getRecommendedIntakeGph(105.0, isRaceMode = true), 0.0)
        // Lower bands are unaffected by race mode.
        assertEquals(45.0, IntensityZone.getRecommendedIntakeGph(60.0, isRaceMode = true), 0.0)
    }
}
