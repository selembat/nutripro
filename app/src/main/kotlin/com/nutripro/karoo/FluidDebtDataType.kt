package com.nutripro.karoo

import androidx.compose.runtime.Composable
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.glance.BalanceBarView
import com.nutripro.glance.GlanceColors
import com.nutripro.model.RideNutritionState
import com.nutripro.util.Preferences
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

/**
 * Fluid balance status (spec 3.2): ml behind (-X) or ahead (+X) of the sweat-based drink
 * target, shown as a balance bar (right = ahead, left = owed) scaled to one sip-block.
 * Pure status - no tap; logging happens via the separate Quick Sip field.
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
        val balance = -debtMl
        val color = if (balance >= 0) GlanceColors.Water else GlanceColors.Error

        BalanceBarView(
            label = "FLUID",
            signedValueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "ml",
            ratio = (balance / sipBlockMl).toFloat(),
            color = color,
        )
    }
}
