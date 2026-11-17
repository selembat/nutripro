package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Dedicated sodium-logging field: tapping it opens a confirm-or-custom screen for the
 * configured sodium dose, separate from the (now status-only) Sodium Balance field.
 */
class QuickSodiumLogDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "quick-sodium", streamValue = { it.sodiumDebtMg }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        QuickActionButtonView(
            label = "QUICK SODIUM",
            valueText = preferences.sodiumDoseMg.toInt().toString(),
            unitText = "mg",
            accentColor = GlanceColors.White,
            quickLogMode = "confirm-sodium",
            viewConfig = viewConfig,
            backgroundColor = GlanceColors.Background,
        )
    }
}
