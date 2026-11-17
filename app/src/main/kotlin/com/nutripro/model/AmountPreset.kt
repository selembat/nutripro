package com.nutripro.model

import kotlinx.serialization.Serializable

/** A single configurable amount in a Fluid or Sodium preset list (Settings), e.g. "500 ml". */
@Serializable
data class AmountPreset(
    val amount: Int,
    val isDefault: Boolean = false,
) {
    companion object {
        val FLUID_DEFAULTS = listOf(
            AmountPreset(150),
            AmountPreset(250),
            AmountPreset(500, isDefault = true),
            AmountPreset(750),
        )

        val SODIUM_DEFAULTS = listOf(
            AmountPreset(250),
            AmountPreset(500, isDefault = true),
            AmountPreset(750),
            AmountPreset(1000),
        )
    }
}
