package com.nutripro.engine

/** Pure Kotlin pre-climb nudge (spec 3.4): "Climb ahead – eat now" when debt is already building. */
class PreClimbNudge(
    private val maxDistanceM: Double = 3000.0,
    private val maxEtaSec: Long = 8 * 60L,
    private val minDebtFraction: Double = 0.5,
) {
    fun shouldNudge(
        climbDistanceM: Double?,
        climbEtaSec: Long?,
        debtGrams: Double,
        servingGrams: Double,
    ): Boolean {
        val withinDistance = climbDistanceM != null && climbDistanceM <= maxDistanceM
        val withinEta = climbEtaSec != null && climbEtaSec <= maxEtaSec
        if (!withinDistance && !withinEta) return false
        return debtGrams >= servingGrams * minDebtFraction
    }
}
