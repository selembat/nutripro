package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Carb debt (spec 3.1): how far behind the target intake rate the rider currently is.
 * Background color shows urgency relative to one serving, and tapping the field instantly
 * logs the default food template - no menu, same pattern as the Fluid/Sodium debt fields.
 */
class CarbDebtDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "carb-debt", streamValue = { it.carbDebtGrams }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtGrams = state?.carbDebtGrams ?: 12.0
        val servingGrams = preferences.servingGrams.toDouble()

        QuickActionButtonView(
            label = "CARB DEBT",
            valueText = debtGrams.toInt().toString(),
            unitText = "g",
            accentColor = GlanceColors.White,
            quickLogMode = "quick-food",
            viewConfig = viewConfig,
            backgroundColor = GlanceColors.debtBgColor(debtGrams, servingGrams),
            fillFraction = (debtGrams / servingGrams).toFloat(),
        )
    }
}
