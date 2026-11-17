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
 * Carb balance status (spec 3.1): how far behind (-X) or ahead (+X) of the target intake
 * rate the rider currently is, shown as a balance bar (right = ahead, left = owed) scaled
 * to one serving. Pure status - no tap; logging happens via the separate Quick Log field.
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
        val balance = -debtGrams
        val color = if (balance >= 0) GlanceColors.Food else GlanceColors.Error

        BalanceBarView(
            label = "CARB",
            signedValueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "g",
            ratio = (balance / servingGrams).toFloat(),
            color = color,
        )
    }
}
