package com.nutripro.glance

import androidx.compose.ui.graphics.Color

/**
 * Shared palette for the Glance widgets. Thresholds below are expressed as
 * ordered (lowerBound, color) bands rather than if/else chains so new bands
 * can be inserted without restructuring the branching logic.
 */
object GlanceColors {
    val White = Color(0xFFF4F4F5)
    val Background = Color(0xFF000000)
    val Frame = Color(0xFF1C1C1E)
    val Label = Color(0xFFAAAAAA)
    val Divider = Color(0xFF555555)
    val Food = Color(0xFF22C55E)
    val Water = Color(0xFF3B82F6)
    val WaterLight = Color(0xFFBFDBFE)
    val Accent = Color(0xFF5A2073)
    val Warning = Color(0xFFD97706)
    val Error = Color(0xFFEF4444)

    private data class Band(val floor: Double, val background: Color, val foreground: Color)

    private val balanceBands = listOf(
        Band(0.0, Color(0xFF22C55E), Color.Black),
        Band(-50.0, Color(0xFF16A34A), Color.White),
        Band(-100.0, Color(0xFFEAB308), Color.Black),
        Band(-150.0, Color(0xFFF97316), Color.White),
        Band(Double.NEGATIVE_INFINITY, Color(0xFFEF4444), Color.White),
    )

    private fun bandFor(balance: Double): Band =
        balanceBands.first { balance > it.floor || it.floor == Double.NEGATIVE_INFINITY }

    fun balanceBgColor(balance: Double): Color = bandFor(balance).background

    fun balanceTextColor(balance: Double): Color = bandFor(balance).foreground

    fun balanceDimColor(balance: Double): Color = balanceTextColor(balance).copy(alpha = 0.7f)

    fun burnRateColor(gph: Double): Color = thresholdColor(gph, 40.0 to Food, 60.0 to Warning, fallback = Error)

    fun timeSinceColor(minutesAgo: Int): Color =
        thresholdColor(minutesAgo.toDouble(), 20.0 to Food, 40.0 to Warning, fallback = Error)

    /** Background urgency band for a debt field, by its debt relative to [dueAt] (e.g. one dose/serving). */
    fun debtBgColor(debtAmount: Double, dueAt: Double): Color =
        thresholdColor(debtAmount, dueAt * 0.5 to Background, dueAt to Warning, fallback = Error)

    private fun thresholdColor(value: Double, vararg steps: Pair<Double, Color>, fallback: Color): Color =
        steps.firstOrNull { (ceiling, _) -> value < ceiling }?.second ?: fallback
}
