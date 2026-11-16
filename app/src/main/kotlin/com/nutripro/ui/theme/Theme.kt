package com.nutripro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App palette, grouped by what the colors are for rather than kept as one flat
 * bag of fields: accent branding, dark-OLED surfaces, text-on-surface tiers,
 * and the food/water/alert semantics the rest of the UI reaches for by name.
 */
private data class AccentPalette(val base: Color, val light: Color, val onAccent: Color)
private data class SurfacePalette(val background: Color, val surface: Color, val variant: Color, val elevated: Color)
private data class TextPalette(val onBackground: Color, val onSurface: Color, val onSurfaceVariant: Color)
private data class SemanticPalette(
    val food: Color,
    val foodDark: Color,
    val water: Color,
    val waterDark: Color,
    val warning: Color,
    val error: Color,
    val errorLight: Color,
)

/** The 5-step green→red ladder the carb-balance field colors itself by. */
data class BalanceLadder(val healthy: Color, val mild: Color, val moderate: Color, val severe: Color, val critical: Color)

private class NutriProPalette(
    val accent: AccentPalette,
    val surfaces: SurfacePalette,
    val text: TextPalette,
    val semantic: SemanticPalette,
    val balance: BalanceLadder,
)

private val defaultPalette = NutriProPalette(
    accent = AccentPalette(base = Color(0xFF5A2073), light = Color(0xFF8B3FB0), onAccent = Color(0xFFFFFFFF)),
    surfaces = SurfacePalette(
        background = Color(0xFF000000),
        surface = Color(0xFF111111),
        variant = Color(0xFF1C1C1E),
        elevated = Color(0xFF2C2C2E),
    ),
    text = TextPalette(
        onBackground = Color(0xFFF4F4F5),
        onSurface = Color(0xFFF4F4F5),
        onSurfaceVariant = Color(0xFF71717A),
    ),
    semantic = SemanticPalette(
        food = Color(0xFF22C55E),
        foodDark = Color(0xFF16A34A),
        water = Color(0xFF3B82F6),
        waterDark = Color(0xFF2563EB),
        warning = Color(0xFFD97706),
        error = Color(0xFFEF4444),
        errorLight = Color(0xFFF87171),
    ),
    balance = BalanceLadder(
        healthy = Color(0xFF22C55E),
        mild = Color(0xFF16A34A),
        moderate = Color(0xFFEAB308),
        severe = Color(0xFFF97316),
        critical = Color(0xFFEF4444),
    ),
)

private val LocalPalette = staticCompositionLocalOf { defaultPalette }

// Flat accessors so call sites can keep saying `Food`, `Water`, `Accent`, etc.
// without caring how the palette is grouped internally.
val Accent: Color @Composable get() = LocalPalette.current.accent.base
val AccentLight: Color @Composable get() = LocalPalette.current.accent.light
val OnAccent: Color @Composable get() = LocalPalette.current.accent.onAccent
val Background: Color @Composable get() = LocalPalette.current.surfaces.background
val Surface: Color @Composable get() = LocalPalette.current.surfaces.surface
val SurfaceVariant: Color @Composable get() = LocalPalette.current.surfaces.variant
val SurfaceElevated: Color @Composable get() = LocalPalette.current.surfaces.elevated
val OnBackground: Color @Composable get() = LocalPalette.current.text.onBackground
val OnSurface: Color @Composable get() = LocalPalette.current.text.onSurface
val OnSurfaceVariant: Color @Composable get() = LocalPalette.current.text.onSurfaceVariant
val Food: Color @Composable get() = LocalPalette.current.semantic.food
val FoodDark: Color @Composable get() = LocalPalette.current.semantic.foodDark
val Water: Color @Composable get() = LocalPalette.current.semantic.water
val WaterDark: Color @Composable get() = LocalPalette.current.semantic.waterDark
val Warning: Color @Composable get() = LocalPalette.current.semantic.warning
val Error: Color @Composable get() = LocalPalette.current.semantic.error

private fun textStyle(weight: FontWeight, size: TextUnit, lineHeight: TextUnit, tracking: TextUnit = 0.sp) =
    TextStyle(fontWeight = weight, fontSize = size, lineHeight = lineHeight, letterSpacing = tracking)

val NutriProTypography = Typography(
    headlineLarge = textStyle(FontWeight.Bold, 32.sp, 40.sp),
    headlineMedium = textStyle(FontWeight.Bold, 24.sp, 32.sp),
    headlineSmall = textStyle(FontWeight.SemiBold, 20.sp, 28.sp),
    titleLarge = textStyle(FontWeight.SemiBold, 18.sp, 24.sp),
    titleMedium = textStyle(FontWeight.SemiBold, 14.sp, 20.sp, 0.15.sp),
    bodyLarge = textStyle(FontWeight.Normal, 14.sp, 20.sp),
    bodyMedium = textStyle(FontWeight.Normal, 12.sp, 16.sp),
    bodySmall = textStyle(FontWeight.Normal, 11.sp, 14.sp),
    labelLarge = textStyle(FontWeight.Medium, 12.sp, 16.sp),
    labelMedium = textStyle(FontWeight.Medium, 11.sp, 14.sp, 0.5.sp),
    labelSmall = textStyle(FontWeight.Medium, 9.sp, 12.sp, 0.5.sp),
)

/** A single hairline along the bottom edge — used to separate stacked list rows. */
fun Modifier.bottomRule(color: Color): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}

@Composable
fun NutriProTheme(content: @Composable () -> Unit) {
    val palette = defaultPalette
    val colorScheme = darkColorScheme(
        primary = palette.accent.base,
        onPrimary = palette.accent.onAccent,
        primaryContainer = palette.accent.light,
        secondary = palette.semantic.food,
        onSecondary = Color.Black,
        tertiary = palette.semantic.water,
        background = palette.surfaces.background,
        onBackground = palette.text.onBackground,
        surface = palette.surfaces.surface,
        onSurface = palette.text.onSurface,
        surfaceVariant = palette.surfaces.variant,
        onSurfaceVariant = palette.text.onSurfaceVariant,
        error = palette.semantic.error,
        onError = Color.White,
    )

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = NutriProTypography, content = content)
    }
}
