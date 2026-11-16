package com.nutripro.karoo

import io.hammerhead.karooext.models.ViewConfig

const val NO_DATA = "-"

private const val FULL_WIDTH_GRID_UNITS = 50

/**
 * Buckets a field's [ViewConfig] into one of the six layout classes the
 * Glance views render against. Full-width fields are classified by a
 * tall/medium/short height ladder; half-width fields by a separate
 * narrow/medium/small ladder, since the same pixel height reads very
 * differently at half width.
 */
enum class LayoutSize { SMALL, SMALL_WIDE, MEDIUM_WIDE, MEDIUM, LARGE, NARROW }

private val fullWidthLadder = listOf(250 to LayoutSize.LARGE, 160 to LayoutSize.MEDIUM_WIDE)
private val halfWidthLadder = listOf(600 to LayoutSize.NARROW, 200 to LayoutSize.MEDIUM)

fun getLayoutSize(config: ViewConfig): LayoutSize {
    val height = config.viewSize.second
    val isFullWidth = config.gridSize.first >= FULL_WIDTH_GRID_UNITS
    val ladder = if (isFullWidth) fullWidthLadder else halfWidthLadder
    val fallback = if (isFullWidth) LayoutSize.SMALL_WIDE else LayoutSize.SMALL
    return ladder.firstOrNull { (minHeight, _) -> height >= minHeight }?.second ?: fallback
}
