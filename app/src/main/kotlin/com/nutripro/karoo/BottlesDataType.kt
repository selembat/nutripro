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

/** Remaining fluid across all bottles, as one total (spec: no per-bottle breakdown). Tap to refill. */
class BottlesDataType(
    private val preferences: Preferences,
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "bottles", streamValue = { it.remainingBottleMl }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val capacityMl = preferences.bottleCount * preferences.bottleVolumeMl
        val remainingMl = state?.remainingBottleMl ?: capacityMl.toDouble()
        val isEmpty = remainingMl <= 0.0
        val (bg, label) = rememberFieldColors(preferences.themeMode)

        QuickActionButtonView(
            label = LocalContext.current.getString(R.string.dt_bottles_short),
            valueText = remainingMl.toInt().toString(),
            unitText = "ml",
            accentColor = if (isEmpty) GlanceColors.Error else GlanceColors.Water,
            quickLogMode = "refill",
            viewConfig = viewConfig,
            backgroundColor = bg,
            labelColor = label,
        )
    }
}
