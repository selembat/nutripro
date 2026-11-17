package com.nutripro.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.nutripro.R
import com.nutripro.karoo.LayoutSize
import com.nutripro.karoo.getLayoutSize
import io.hammerhead.karooext.models.ViewConfig

@Composable
fun CarbBalanceView(
    balance: Double,
    burned: Double,
    eaten: Double,
    burnRateGph: Double,
    lastIntakeTimestampMs: Long,
    viewConfig: ViewConfig,
) {
    val palette = BalancePalette(
        background = GlanceColors.balanceBgColor(balance),
        foreground = GlanceColors.balanceTextColor(balance),
        dim = GlanceColors.balanceDimColor(balance),
    )
    val readings = BalanceReadings(balance, burned, eaten, burnRateGph, lastIntakeTimestampMs)

    FieldSurface(backgroundColor = palette.background) {
        when (getLayoutSize(viewConfig)) {
            LayoutSize.SMALL -> JustTheNumber(readings, palette)
            LayoutSize.SMALL_WIDE -> TitledNumber(readings, palette)
            LayoutSize.MEDIUM_WIDE -> NumberWithThreeStats(readings, palette)
            LayoutSize.MEDIUM -> NumberWithInlineSummary(readings, palette)
            LayoutSize.LARGE -> FullBreakdown(readings, palette)
            LayoutSize.NARROW -> StackedBreakdown(readings, palette)
        }
    }
}

private data class BalancePalette(val background: Color, val foreground: Color, val dim: Color)

private data class BalanceReadings(
    val balance: Double,
    val burned: Double,
    val eaten: Double,
    val burnRateGph: Double,
    val lastIntakeTimestampMs: Long,
) {
    val balanceLabel get() = "${balance.toInt()}g"
    val burnedLabel get() = "${burned.toInt()}g"
    val eatenLabel get() = "${eaten.toInt()}g"
    val burnRateLabel get() = "${burnRateGph.toInt()}g/h"
}

@Composable
private fun JustTheNumber(r: BalanceReadings, p: BalancePalette) {
    MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 24.sp)
}

@Composable
private fun TitledNumber(r: BalanceReadings, p: BalancePalette) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptionText(text = context.getString(R.string.glance_balance_caption), fontSize = 10.sp, color = p.dim)
        MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 26.sp)
    }
}

@Composable
private fun NumberWithThreeStats(r: BalanceReadings, p: BalancePalette) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptionText(text = context.getString(R.string.glance_carb_balance_caption), fontSize = 10.sp, color = p.dim)
        MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 28.sp)
        Spacer(modifier = GlanceModifier.height(2.dp))
        StatRow(
            stats = listOf(
                Stat(context.getString(R.string.glance_burned), r.burnedLabel, p.foreground),
                Stat(context.getString(R.string.glance_eaten), r.eatenLabel, p.foreground),
                Stat(context.getString(R.string.glance_rate), r.burnRateLabel, p.foreground),
            ),
            labelFontSize = 9.sp,
            valueFontSize = 14.sp,
            labelColor = p.dim,
        )
    }
}

@Composable
private fun NumberWithInlineSummary(r: BalanceReadings, p: BalancePalette) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptionText(text = context.getString(R.string.glance_balance_caption), fontSize = 11.sp, color = p.dim)
        MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 24.sp)
        Spacer(modifier = GlanceModifier.height(2.dp))
        CaptionText(text = context.getString(R.string.glance_compact_be, r.burnedLabel, r.eatenLabel), fontSize = 12.sp, color = p.dim)
    }
}

@Composable
private fun FullBreakdown(r: BalanceReadings, p: BalancePalette) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptionText(text = context.getString(R.string.glance_carb_balance_caption), fontSize = 11.sp, color = p.dim)
        MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 36.sp)
        Spacer(modifier = GlanceModifier.height(4.dp))
        HorizontalRule(color = p.dim)
        Spacer(modifier = GlanceModifier.height(4.dp))
        val rows = buildList {
            add(context.getString(R.string.glance_burned) to r.burnedLabel)
            add(context.getString(R.string.glance_eaten) to r.eatenLabel)
            add(context.getString(R.string.glance_burn_rate) to r.burnRateLabel)
            if (r.lastIntakeTimestampMs > 0) {
                add(context.getString(R.string.glance_last_eat) to relativeMinutesLabel(context, r.lastIntakeTimestampMs))
            }
        }
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(modifier = GlanceModifier.height(2.dp))
            LabelValueRow(label = label, value = value, valueColor = p.foreground, fontSize = 16.sp, labelColor = p.dim)
        }
    }
}

@Composable
private fun StackedBreakdown(r: BalanceReadings, p: BalancePalette) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptionText(text = context.getString(R.string.glance_balance_caption), fontSize = 11.sp, color = p.dim)
        MetricText(text = r.balanceLabel, color = p.foreground, fontSize = 28.sp)
        Spacer(modifier = GlanceModifier.height(4.dp))
        CaptionText(text = context.getString(R.string.glance_compact_e, r.eatenLabel), fontSize = 13.sp, color = p.dim)
        Spacer(modifier = GlanceModifier.height(2.dp))
        CaptionText(text = context.getString(R.string.glance_compact_b, r.burnedLabel), fontSize = 13.sp, color = p.dim)
    }
}
