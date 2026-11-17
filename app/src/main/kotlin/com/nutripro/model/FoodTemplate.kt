package com.nutripro.model

import android.content.Context
import com.nutripro.R
import kotlinx.serialization.Serializable

@Serializable
data class FoodTemplate(
    val name: String,
    val carbsGrams: Int,
    val emoji: String = "",
    val isDefault: Boolean = false,
    /** Total fluid volume (ml) of a drink mix. 0 = plain food, not a drink mix. */
    val fluidMl: Int = 0,
    /** Total sodium (mg) in the full drink mix. Only meaningful when [fluidMl] > 0. */
    val sodiumMg: Int = 0,
    /** Default sip size (ml) logged per tap. Only meaningful when [fluidMl] > 0. */
    val sipMl: Int = 0,
) {
    val isDrinkMix: Boolean get() = fluidMl > 0

    val displayName: String get() = if (emoji.isNotEmpty()) "$emoji $name" else name
    val shortName: String get() = emoji.ifEmpty { name }

    /** For a drink mix, the carbs/fluid/sodium actually logged per tap (one [sipMl] sip). */
    fun sipAmounts(): Triple<Double, Double, Double> {
        if (!isDrinkMix || sipMl <= 0) return Triple(carbsGrams.toDouble(), 0.0, 0.0)
        val fraction = sipMl.toDouble() / fluidMl
        return Triple(carbsGrams * fraction, sipMl.toDouble(), sodiumMg * fraction)
    }

    companion object {
        /** English fallback, used only where no [Context] is available (e.g. serialization defaults). */
        val DEFAULTS = listOf(
            FoodTemplate("Energy Bar", 40, emoji = "🍫"),
            FoodTemplate("Banana", 27, emoji = "🍌"),
            FoodTemplate("Energy Gel", 40, emoji = "⚡", isDefault = true),
        )

        fun defaults(context: Context): List<FoodTemplate> = listOf(
            FoodTemplate(context.getString(R.string.food_energy_bar), 40, emoji = "🍫"),
            FoodTemplate(context.getString(R.string.food_banana), 27, emoji = "🍌"),
            FoodTemplate(context.getString(R.string.food_energy_gel), 40, emoji = "⚡", isDefault = true),
        )
    }
}
