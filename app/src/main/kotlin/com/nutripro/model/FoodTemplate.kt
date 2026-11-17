package com.nutripro.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodTemplate(
    val name: String,
    val carbsGrams: Int,
    val emoji: String = "",
    val isDefault: Boolean = false,
) {
    val displayName: String get() = if (emoji.isNotEmpty()) "$emoji $name" else name
    val shortName: String get() = emoji.ifEmpty { name }

    companion object {
        val DEFAULTS = listOf(
            FoodTemplate("Energy Bar", 40, emoji = "🍫"),
            FoodTemplate("Banana", 27, emoji = "🍌"),
            FoodTemplate("Energy Gel", 40, emoji = "⚡", isDefault = true),
        )
    }
}
