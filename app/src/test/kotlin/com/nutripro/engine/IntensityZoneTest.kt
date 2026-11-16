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
}
