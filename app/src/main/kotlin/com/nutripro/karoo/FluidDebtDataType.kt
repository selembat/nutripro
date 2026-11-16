package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Fluid debt (spec 3.2): estimated ml behind the sweat-based drink target. Background color
 * shows urgency relative to one sip-block, and tapping the field instantly logs the
 * configured Quick Sip amount - no menu, same pattern as the Carb/Sodium debt fields.
 */
class FluidDebtDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "fluid-debt", streamValue = { it.fluidDebtMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtMl = state?.fluidDebtMl ?: 45.0
        val sipBlockMl = preferences.sipBlockMl.toDouble()

        QuickActionButtonView(
            label = "FLUID DEBT",
            valueText = debtMl.toInt().toString(),
            unitText = "ml",
            accentColor = GlanceColors.White,
            quickLogMode = "quick-water",
            viewConfig = viewConfig,
            backgroundColor = GlanceColors.debtBgColor(debtMl, sipBlockMl),
            fillFraction = (debtMl / sipBlockMl).toFloat(),
        )
    }
}
