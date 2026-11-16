package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.CarbBalanceView
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.models.ViewConfig

private val previewState = RideNutritionState(
    totalBurned = 82.0,
    totalEaten = 35.0,
    burnRateGph = 45.0,
    lastIntakeTimestampMs = System.currentTimeMillis() - 12 * 60_000,
)

class CarbBalanceDataType(tracker: CarbBalanceTracker, extension: String) :
    GraphicalDataType(tracker, extension, "carb-balance", streamValue = { it.balance }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val s = state ?: previewState
        CarbBalanceView(
            balance = s.balance,
            burned = s.totalBurned,
            eaten = s.totalEaten,
            burnRateGph = s.burnRateGph,
            lastIntakeTimestampMs = s.lastIntakeTimestampMs,
            viewConfig = viewConfig,
        )
    }
}
