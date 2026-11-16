package com.nutripro.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Rounded, padded backdrop every field renders inside of. */
@Composable
fun FieldSurface(
    backgroundColor: Color = GlanceColors.Background,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(backgroundColor)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun CaptionText(
    text: String,
    fontSize: TextUnit = 12.sp,
    color: Color = GlanceColors.Label,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        maxLines = maxLines,
        style = TextStyle(color = ColorProvider(color), fontSize = fontSize, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun MetricText(
    text: String,
    color: Color = GlanceColors.White,
    fontSize: TextUnit = 18.sp,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        maxLines = maxLines,
        style = TextStyle(
            color = ColorProvider(color),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
    )
}

/** A labelled value with the label on the left and the number flush right. */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    valueColor: Color = GlanceColors.White,
    fontSize: TextUnit = 18.sp,
    labelColor: Color = GlanceColors.Label,
) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, maxLines = 1, style = TextStyle(color = ColorProvider(labelColor), fontSize = fontSize))
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = value,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(valueColor), fontSize = fontSize, fontWeight = FontWeight.Bold),
        )
    }
}

/** One column of a [StatRow]: a caption over a value, in its own color. */
data class Stat(val label: String, val value: String, val valueColor: Color = GlanceColors.White)

/**
 * Lays out 2-3 [Stat]s side by side, each in its own flexible column,
 * separated by a thin vertical rule. Replaces having a separate
 * two-column and three-column composable for what is the same layout rule
 * applied a different number of times.
 */
@Composable
fun StatRow(
    stats: List<Stat>,
    labelFontSize: TextUnit = 10.sp,
    valueFontSize: TextUnit = 18.sp,
    labelColor: Color = GlanceColors.Label,
) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) VerticalRule()
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = stat.label, fontSize = labelFontSize, color = labelColor)
                MetricText(text = stat.value, color = stat.valueColor, fontSize = valueFontSize)
            }
        }
    }
}

/**
 * A segmented fill bar: Glance's Row weighting only supports equal-weight children (no
 * arbitrary fractions), so the fraction is rendered as N/[segments] colored segments rather
 * than one continuously-sized block - reads the same at a glance, just stepped.
 */
@Composable
fun FillBar(
    fraction: Float,
    fillColor: Color,
    trackColor: Color = GlanceColors.Frame,
    segments: Int = 10,
    height: Dp = 5.dp,
) {
    val filledCount = (fraction.coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)
    Row(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        repeat(segments) { index ->
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .cornerRadius(2.dp)
                    .background(if (index < filledCount) fillColor else trackColor),
            ) {}
        }
    }
}

@Composable
fun HorizontalRule(color: Color = GlanceColors.Divider, thickness: Dp = 1.dp) {
    Box(modifier = GlanceModifier.fillMaxWidth().height(thickness).background(color)) {}
}

@Composable
fun VerticalRule(color: Color = GlanceColors.Divider, thickness: Dp = 1.dp, height: Dp = 28.dp) {
    Box(modifier = GlanceModifier.width(thickness).height(height).background(color)) {}
}

/** Renders "just now" / "<n>min" / "<h>h<m>m" for how long ago [timestampMs] was, or "-" if unset. */
fun relativeMinutesLabel(timestampMs: Long): String {
    if (timestampMs <= 0) return "-"
    return minutesAgoLabel(((System.currentTimeMillis() - timestampMs) / 60_000).toInt())
}

/** Same rendering as [relativeMinutesLabel] but from an already-computed minute count. */
fun minutesAgoLabel(minutesAgo: Int): String {
    if (minutesAgo < 1) return "just now"
    if (minutesAgo < 60) return "${minutesAgo}min"
    val hours = minutesAgo / 60
    val remainder = minutesAgo % 60
    return if (remainder == 0) "${hours}h" else "${hours}h${remainder}m"
}
