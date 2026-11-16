package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.HydrationView
import com.nutripro.model.IntakeEntry
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.models.ViewConfig

class HydrationDataType(tracker: CarbBalanceTracker, extension: String) :
    GraphicalDataType(tracker, extension, "hydration", streamValue = { it.totalWaterMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        if (state == null) {
            HydrationView(totalWaterMl = 750.0, mlPerHour = 500.0, sipCount = 3, viewConfig = viewConfig)
            return
        }
        HydrationView(
            totalWaterMl = state.totalWaterMl,
            mlPerHour = sippingRateMlPerHour(state),
            sipCount = state.intakeLog.count { it.type == IntakeEntry.IntakeType.WATER },
            viewConfig = viewConfig,
        )
    }

    private fun sippingRateMlPerHour(state: RideNutritionState): Double {
        val firstSipAt = state.intakeLog
            .filter { it.type == IntakeEntry.IntakeType.WATER }
            .minOfOrNull { it.timestampMs } ?: return 0.0
        if (state.totalWaterMl <= 0) return 0.0

        val elapsedMs = System.currentTimeMillis() - firstSipAt
        if (elapsedMs < 60_000) return 0.0
        return state.totalWaterMl / (elapsedMs / 3_600_000.0)
    }
}
