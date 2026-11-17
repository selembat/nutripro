package com.nutripro.karoo

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import com.nutripro.R
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.glance.rememberFieldColors
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig

/**
 * Dedicated instant-carb field: tapping it opens a confirm-or-custom screen for the
 * default food template's amount, separate from the (status-only) Carb Balance field and
 * from Add Carb's full template/select menu.
 */
class QuickCarbDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "quick-carb", streamValue = { it.carbDebtGrams }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val (bg, label) = rememberFieldColors(preferences.themeMode)
        QuickActionButtonView(
            label = LocalContext.current.getString(R.string.dt_quick_carb_short),
            valueText = preferences.getDefaultTemplate().carbsGrams.toString(),
            unitText = "g",
            accentColor = GlanceColors.Food,
            quickLogMode = "confirm-carb",
            viewConfig = viewConfig,
            backgroundColor = bg,
            labelColor = label,
        )
    }
}
