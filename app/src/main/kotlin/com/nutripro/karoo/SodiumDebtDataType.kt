package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Sodium debt (spec 3.7 extension): background color shows urgency relative to one dose,
 * and tapping the field instantly logs that dose - no menu, same pattern as the Carb/Fluid
 * debt fields.
 */
class SodiumDebtDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "sodium-debt", streamValue = { it.sodiumDebtMg }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtMg = state?.sodiumDebtMg ?: 320.0
        val doseMg = preferences.sodiumDoseMg.toDouble()

        QuickActionButtonView(
            label = "SODIUM",
            valueText = debtMg.toInt().toString(),
            unitText = "mg",
            accentColor = GlanceColors.White,
            quickLogMode = "quick-sodium",
            viewConfig = viewConfig,
            backgroundColor = GlanceColors.debtBgColor(debtMg, doseMg),
            fillFraction = (debtMg / doseMg).toFloat(),
        )
    }
}
