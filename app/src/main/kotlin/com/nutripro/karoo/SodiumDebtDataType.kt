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
 * Sodium balance status (spec 3.7 extension): mg behind (-X) or ahead (+X) of estimated
 * sweat loss, shown as a balance bar (right = ahead, left = owed) scaled to one dose.
 * Pure status - no tap; logging happens via the separate Quick Sodium field.
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
        val balance = -debtMg
        val color = if (balance >= 0) GlanceColors.Accent else GlanceColors.Error

        BalanceBarView(
            label = "SODIUM",
            signedValueText = if (balance >= 0) "+${balance.roundToInt()}" else "${balance.roundToInt()}",
            unitText = "mg",
            ratio = (balance / doseMg).toFloat(),
            color = color,
        )
    }
}
