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
 * Sodium balance (spec 3.7 extension): mg behind (-X) or ahead (+X) of estimated sweat
 * loss, shown as a solid purple/red field in the native Karoo numeric-field style. Tapping
 * it opens Add Sodium's select screen.
 */
class SodiumDebtDataType(
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "sodium-debt", streamValue = { it.sodiumDebtMg }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtMg = state?.sodiumDebtMg ?: 320.0
        val balance = -debtMg

        QuickActionButtonView(
            label = LocalContext.current.getString(R.string.dt_sodium_short),
            valueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "mg",
            accentColor = GlanceColors.White,
            quickLogMode = "sodium",
            viewConfig = viewConfig,
            backgroundColor = if (balance >= 0) GlanceColors.Accent else GlanceColors.Error,
        )
    }
}
