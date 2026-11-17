package com.nutripro.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.nutripro.R
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
    themeMode: String,
    viewConfig: ViewConfig,
) {
    val context = LocalContext.current
    val isDark = GlanceColors.isDarkTheme(themeMode, context)
    val bg = GlanceColors.fieldBackground(isDark)
    val dim = GlanceColors.fieldLabelColor(isDark)
    val onTile = if (isDark) GlanceColors.White else GlanceColors.OnLightTile
    val display = LastIntakeDisplay.from(context, lastIntakeName, lastIntakeEmoji, lastIntakeTimestampMs, onTile, dim)
    val balanceLabel = "${balance.toInt()}g"
    val lastFoodCaption = context.getString(R.string.glance_last_food_caption)

    FieldSurface(
        backgroundColor = bg,
        modifier = GlanceModifier.clickable(actionStartActivity<QuickLogActivity>()),
    ) {
        when (getLayoutSize(viewConfig)) {
            LayoutSize.SMALL ->
                MetricText(text = display.shortText, color = display.nameColor, fontSize = 14.sp)

            LayoutSize.SMALL_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = lastFoodCaption, fontSize = 10.sp, color = dim)
                MetricText(text = display.shortText, color = display.foodColor, fontSize = 18.sp)
            }

            LayoutSize.MEDIUM_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = lastFoodCaption, fontSize = 10.sp, color = dim)
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow(
                    stats = listOf(
                        Stat(context.getString(R.string.glance_name), display.shortText, display.foodColor),
                        Stat(context.getString(R.string.glance_ago), display.timeAgoText, display.timeAgoColor),
                        Stat(context.getString(R.string.glance_bal), balanceLabel, onTile),
                    ),
                    labelFontSize = 9.sp,
                    valueFontSize = 14.sp,
                    labelColor = dim,
                )
            }

            LayoutSize.MEDIUM -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = lastFoodCaption, fontSize = 11.sp, color = dim)
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
                CaptionText(text = lastFoodCaption, fontSize = 11.sp, color = dim)
                MetricText(text = display.fullText, color = display.foodColor, fontSize = 28.sp)
                Spacer(modifier = GlanceModifier.height(4.dp))
                HorizontalRule(color = dim)
                Spacer(modifier = GlanceModifier.height(4.dp))
                LabelValueRow(label = context.getString(R.string.glance_time), value = display.timeAgoText, valueColor = display.timeAgoColor, fontSize = 16.sp, labelColor = dim)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(label = context.getString(R.string.glance_balance), value = balanceLabel, valueColor = onTile, fontSize = 16.sp, labelColor = dim)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(
                    label = context.getString(R.string.glance_burn_rate),
                    value = "${burnRateGph.toInt()}g/h",
                    valueColor = GlanceColors.burnRateColor(burnRateGph),
                    fontSize = 16.sp,
                    labelColor = dim,
                )
            }

            LayoutSize.NARROW -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = lastFoodCaption, fontSize = 11.sp, color = dim)
                MetricText(text = display.fullText, color = display.foodColor, fontSize = 16.sp)
                if (display.hasTimestamp) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    CaptionText(text = display.timeAgoText, fontSize = 13.sp, color = display.timeAgoColor)
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                CaptionText(text = context.getString(R.string.glance_compact_bal, balanceLabel), fontSize = 13.sp, color = dim)
            }
        }
    }
}

/** Precomputes every text/color choice the layouts above need from the raw last-intake fields. */
private class LastIntakeDisplay(
    context: android.content.Context,
    name: String?,
    emoji: String?,
    val hasTimestamp: Boolean,
    minutesAgo: Int,
    onTileColor: Color,
    dimColor: Color,
) {
    private val logged = name != null
    val shortText = emoji?.takeIf { it.isNotEmpty() } ?: name ?: NO_DATA
    val fullText = if (!emoji.isNullOrEmpty() && name != null) "$emoji $name" else name ?: NO_DATA
    val timeAgoText = if (hasTimestamp) minutesAgoLabel(context, minutesAgo) else NO_DATA
    val nameColor = if (logged) onTileColor else dimColor
    val foodColor = if (logged) GlanceColors.Food else dimColor
    val timeAgoColor = if (hasTimestamp) GlanceColors.timeSinceColor(minutesAgo) else dimColor

    companion object {
        fun from(context: android.content.Context, name: String?, emoji: String?, timestampMs: Long, onTileColor: Color, dimColor: Color): LastIntakeDisplay {
            val hasTimestamp = timestampMs > 0
            val minutesAgo = if (hasTimestamp) ((System.currentTimeMillis() - timestampMs) / 60_000).toInt() else 0
            return LastIntakeDisplay(context, name, emoji, hasTimestamp, minutesAgo, onTileColor, dimColor)
        }
    }
}
