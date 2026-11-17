package com.nutripro.glance

import androidx.compose.runtime.Composable
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
fun HydrationView(
    totalWaterMl: Double,
    mlPerHour: Double,
    sipCount: Int,
    themeMode: String,
    viewConfig: ViewConfig,
) {
    val context = LocalContext.current
    val hydrationCaption = context.getString(R.string.glance_hydration_caption)
    val totalLabel = "${totalWaterMl.toInt()}ml"
    val rateLabel = "${mlPerHour.toInt()}ml/h"
    val (bg, dim) = rememberFieldColors(themeMode)

    FieldSurface(backgroundColor = bg) {
        when (getLayoutSize(viewConfig)) {
            LayoutSize.SMALL ->
                MetricText(text = "${totalWaterMl.toInt()}", color = GlanceColors.Water, fontSize = 24.sp)

            LayoutSize.SMALL_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = hydrationCaption, fontSize = 10.sp, color = dim)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 24.sp)
            }

            LayoutSize.MEDIUM_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = hydrationCaption, fontSize = 10.sp, color = dim)
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow(
                    stats = listOf(
                        Stat(context.getString(R.string.glance_total), totalLabel, GlanceColors.Water),
                        Stat(context.getString(R.string.glance_rate), rateLabel, GlanceColors.Water),
                    ),
                    labelFontSize = 9.sp,
                    valueFontSize = 18.sp,
                    labelColor = dim,
                )
            }

            LayoutSize.MEDIUM -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = hydrationCaption, fontSize = 11.sp, color = dim)
                MetricText(text = "${totalWaterMl.toInt()}", color = GlanceColors.Water, fontSize = 24.sp)
                CaptionText(text = context.getString(R.string.unit_ml), fontSize = 12.sp, color = GlanceColors.Water)
            }

            LayoutSize.LARGE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = hydrationCaption, fontSize = 11.sp, color = dim)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 36.sp)
                Spacer(modifier = GlanceModifier.height(4.dp))
                HorizontalRule(color = dim)
                Spacer(modifier = GlanceModifier.height(4.dp))
                LabelValueRow(label = context.getString(R.string.glance_rate), value = rateLabel, valueColor = GlanceColors.Water, fontSize = 16.sp, labelColor = dim)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(label = context.getString(R.string.glance_sips), value = "$sipCount", valueColor = GlanceColors.Water, fontSize = 16.sp, labelColor = dim)
            }

            LayoutSize.NARROW -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = hydrationCaption, fontSize = 11.sp, color = dim)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 24.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                CaptionText(text = rateLabel, fontSize = 13.sp, color = GlanceColors.Water)
            }
        }
    }
}
