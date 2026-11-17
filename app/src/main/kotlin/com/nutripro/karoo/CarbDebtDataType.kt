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
 * Carb balance (spec 3.1): how far behind (-X) or ahead (+X) of the target intake rate the
 * rider currently is, shown as a solid green/red field in the native Karoo numeric-field
 * style. Tapping it opens Add Carb's select screen.
 */
class CarbDebtDataType(
    tracker: CarbBalanceTracker,
    extension: String,
) : GraphicalDataType(tracker, extension, "carb-debt", streamValue = { it.carbDebtGrams }) {

    @Composable
    override fun Content(state: RideNutritionState?, viewConfig: ViewConfig) {
        val debtGrams = state?.carbDebtGrams ?: 12.0
        val balance = -debtGrams

        QuickActionButtonView(
            label = LocalContext.current.getString(R.string.dt_carb_short),
            valueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "g",
            accentColor = GlanceColors.White,
            quickLogMode = "food",
            viewConfig = viewConfig,
            backgroundColor = if (balance >= 0) GlanceColors.Food else GlanceColors.Error,
        )
    }
}
