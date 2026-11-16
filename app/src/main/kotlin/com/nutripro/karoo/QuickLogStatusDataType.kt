package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.QuickLogStatusView
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.models.ViewConfig

private val previewState = RideNutritionState(
    totalEaten = 35.0,
    totalBurned = 82.0,
    burnRateGph = 45.0,
    lastIntakeName = "Energy Gel",
    lastIntakeEmoji = "⚡",
    lastIntakeTimestampMs = System.currentTimeMillis() - 8 * 60_000,
)

class QuickLogStatusDataType(tracker: CarbBalanceTracker, extension: String) :
    GraphicalDataType(tracker, extension, "quick-log-status", streamValue = { it.totalEaten }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val s = state ?: previewState
        QuickLogStatusView(
            lastIntakeName = s.lastIntakeName,
            lastIntakeEmoji = s.lastIntakeEmoji,
            lastIntakeTimestampMs = s.lastIntakeTimestampMs,
            balance = s.balance,
            burnRateGph = s.burnRateGph,
            viewConfig = viewConfig,
        )
    }
}
