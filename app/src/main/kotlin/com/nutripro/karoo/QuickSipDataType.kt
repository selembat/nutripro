package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Dedicated water-logging field: tapping it opens a confirm-or-custom screen for the
 * configured sip amount, separate from the (now status-only) Fluid Balance field.
 */
class QuickSipDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "quick-sip", streamValue = { it.fluidDebtMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        QuickActionButtonView(
            label = "QUICK SIP",
            valueText = preferences.sipAmountMl.toInt().toString(),
            unitText = "ml",
            accentColor = GlanceColors.Water,
            quickLogMode = "confirm-water",
            viewConfig = viewConfig,
            backgroundColor = GlanceColors.Background,
        )
    }
}
