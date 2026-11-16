package com.nutripro.engine

/** Pure Kotlin detector for conditions where firing an alert is unsafe/unrealistic (spec 3.4). */
class UnsafeConditionDetector(
    private val hardEffortFtpThreshold: Double = 1.05,
    private val descentSpeedThresholdMps: Double = 12.0,
    private val descentGradeThreshold: Double = -3.0,
) {
    /**
     * [power30sAvgWatts]/[ftpWatts]: a hard climb or sprint.
     * [speedMps]/[gradePercent] (negative = downhill): a fast descent.
     */
    fun isUnsafe(power30sAvgWatts: Double, ftpWatts: Double, speedMps: Double, gradePercent: Double): Boolean {
        val hardEffort = ftpWatts > 0 && power30sAvgWatts > hardEffortFtpThreshold * ftpWatts
        val fastDescent = speedMps >= descentSpeedThresholdMps && gradePercent <= descentGradeThreshold
        return hardEffort || fastDescent
    }
}
