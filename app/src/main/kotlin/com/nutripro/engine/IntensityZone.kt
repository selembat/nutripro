package com.nutripro.engine

object IntensityZone {
    /** Race-mode ceiling (spec 3.1 extension): elite practice now runs dual-source carbs well
     * past the 90g/h top band below, so Race mode alone is allowed to exceed it. */
    private const val RACE_TOP_BAND_GPH = 115.0

    fun getCarbFraction(ftpPercent: Double): Double = when {
        ftpPercent < 55 -> 0.40
        ftpPercent < 75 -> 0.55
        ftpPercent < 90 -> 0.70
        ftpPercent < 105 -> 0.85
        else -> 0.95
    }

    fun getRecommendedIntakeGph(ftpPercent: Double, isRaceMode: Boolean = false): Double = when {
        ftpPercent < 55 -> 30.0
        ftpPercent < 75 -> 45.0
        ftpPercent < 90 -> 60.0
        ftpPercent < 105 -> 75.0
        else -> if (isRaceMode) RACE_TOP_BAND_GPH else 90.0
    }
}
