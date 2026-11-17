package com.nutripro.karoo

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import com.nutripro.R
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

/**
 * Fluid balance (spec 3.2): ml behind (-X) or ahead (+X) of the sweat-based drink target,
 * shown as a solid blue/red field in the native Karoo numeric-field style. Tapping it opens
 * Add Fluid's select screen.
 */
class FluidDebtDataType(
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "fluid-debt", streamValue = { it.fluidDebtMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtMl = state?.fluidDebtMl ?: 45.0
        val balance = -debtMl

        QuickActionButtonView(
            label = LocalContext.current.getString(R.string.dt_fluid_short),
            valueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "ml",
            accentColor = GlanceColors.White,
            quickLogMode = "water",
            viewConfig = viewConfig,
            backgroundColor = if (balance >= 0) GlanceColors.Water else GlanceColors.Error,
        )
    }
}
