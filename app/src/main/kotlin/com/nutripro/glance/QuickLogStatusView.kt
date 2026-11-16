package com.nutripro.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.nutripro.karoo.LayoutSize
import com.nutripro.karoo.NO_DATA
import com.nutripro.karoo.getLayoutSize
import com.nutripro.ui.QuickLogActivity
import io.hammerhead.karooext.models.ViewConfig

@Composable
fun QuickLogStatusView(
    lastIntakeName: String?,
    lastIntakeEmoji: String?,
    lastIntakeTimestampMs: Long,
    balance: Double,
    burnRateGph: Double,
    viewConfig: ViewConfig,
) {
    val display = LastIntakeDisplay.from(lastIntakeName, lastIntakeEmoji, lastIntakeTimestampMs)
    val balanceLabel = "${balance.toInt()}g"

    FieldSurface(modifier = GlanceModifier.clickable(actionStartActivity<QuickLogActivity>())) {
        when (getLayoutSize(viewConfig)) {
            LayoutSize.SMALL ->
                MetricText(text = display.shortText, color = display.nameColor, fontSize = 14.sp)

            LayoutSize.SMALL_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "LAST FOOD", fontSize = 10.sp)
                MetricText(text = display.shortText, color = display.foodColor, fontSize = 18.sp)
            }

            LayoutSize.MEDIUM_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "LAST FOOD", fontSize = 10.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow(
                    stats = listOf(
                        Stat("NAME", display.shortText, display.foodColor),
                        Stat("AGO", display.timeAgoText, display.timeAgoColor),
                        Stat("BAL", balanceLabel, GlanceColors.White),
                    ),
                    labelFontSize = 9.sp,
                    valueFontSize = 14.sp,
                )
            }

            LayoutSize.MEDIUM -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "LAST FOOD", fontSize = 11.sp)
                MetricText(text = display.fullText, color = display.foodColor, fontSize = 18.sp)
                if (display.hasTimestamp) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    CaptionText(text = display.timeAgoText, fontSize = 12.sp, color = display.timeAgoColor)
                }
            }

            LayoutSize.LARGE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "LAST FOOD", fontSize = 11.sp)
                MetricText(text = display.fullText, color = display.foodColor, fontSize = 28.sp)
                Spacer(modifier = GlanceModifier.height(4.dp))
                HorizontalRule()
                Spacer(modifier = GlanceModifier.height(4.dp))
                LabelValueRow(label = "TIME", value = display.timeAgoText, valueColor = display.timeAgoColor, fontSize = 16.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(label = "BALANCE", value = balanceLabel, valueColor = GlanceColors.White, fontSize = 16.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(
                    label = "BURN RATE",
                    value = "${burnRateGph.toInt()}g/h",
                    valueColor = GlanceColors.burnRateColor(burnRateGph),
                    fontSize = 16.sp,
                )
            }

            LayoutSize.NARROW -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "LAST FOOD", fontSize = 11.sp)
                MetricText(text = display.fullText, color = display.foodColor, fontSize = 16.sp)
                if (display.hasTimestamp) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    CaptionText(text = display.timeAgoText, fontSize = 13.sp, color = display.timeAgoColor)
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                CaptionText(text = "Bal: $balanceLabel", fontSize = 13.sp)
            }
        }
    }
}

/** Precomputes every text/color choice the layouts above need from the raw last-intake fields. */
private class LastIntakeDisplay(
    name: String?,
    emoji: String?,
    val hasTimestamp: Boolean,
    minutesAgo: Int,
) {
    private val logged = name != null
    val shortText = emoji?.takeIf { it.isNotEmpty() } ?: name ?: NO_DATA
    val fullText = if (!emoji.isNullOrEmpty() && name != null) "$emoji $name" else name ?: NO_DATA
    val timeAgoText = if (hasTimestamp) minutesAgoLabel(minutesAgo) else NO_DATA
    val nameColor = if (logged) GlanceColors.White else GlanceColors.Label
    val foodColor = if (logged) GlanceColors.Food else GlanceColors.Label
    val timeAgoColor = if (hasTimestamp) GlanceColors.timeSinceColor(minutesAgo) else GlanceColors.Label

    companion object {
        fun from(name: String?, emoji: String?, timestampMs: Long): LastIntakeDisplay {
            val hasTimestamp = timestampMs > 0
            val minutesAgo = if (hasTimestamp) ((System.currentTimeMillis() - timestampMs) / 60_000).toInt() else 0
            return LastIntakeDisplay(name, emoji, hasTimestamp, minutesAgo)
        }
    }
}
