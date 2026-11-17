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

/** Opens the fluid select screen (presets + any drink mixes + custom amount). */
class AddFluidDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "add-fluid", streamValue = { it.fluidDebtMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val (bg, label) = rememberFieldColors(preferences.themeMode)
        val context = LocalContext.current
        QuickActionButtonView(
            label = context.getString(R.string.dt_add_fluid_short),
            valueText = context.getString(R.string.dt_select),
            unitText = "",
            accentColor = GlanceColors.Water,
            quickLogMode = "water",
            viewConfig = viewConfig,
            backgroundColor = bg,
            labelColor = label,
        )
    }
}
