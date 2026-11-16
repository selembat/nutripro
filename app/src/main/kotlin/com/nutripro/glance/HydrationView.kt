package com.nutripro.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.nutripro.karoo.LayoutSize
import com.nutripro.karoo.getLayoutSize
import io.hammerhead.karooext.models.ViewConfig

@Composable
fun HydrationView(
    totalWaterMl: Double,
    mlPerHour: Double,
    sipCount: Int,
    viewConfig: ViewConfig,
) {
    val totalLabel = "${totalWaterMl.toInt()}ml"
    val rateLabel = "${mlPerHour.toInt()}ml/h"

    FieldSurface {
        when (getLayoutSize(viewConfig)) {
            LayoutSize.SMALL ->
                MetricText(text = "${totalWaterMl.toInt()}", color = GlanceColors.Water, fontSize = 24.sp)

            LayoutSize.SMALL_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "HYDRATION", fontSize = 10.sp)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 24.sp)
            }

            LayoutSize.MEDIUM_WIDE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "HYDRATION", fontSize = 10.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow(
                    stats = listOf(
                        Stat("TOTAL", totalLabel, GlanceColors.Water),
                        Stat("RATE", rateLabel, GlanceColors.Water),
                    ),
                    labelFontSize = 9.sp,
                    valueFontSize = 18.sp,
                )
            }

            LayoutSize.MEDIUM -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "HYDRATION", fontSize = 11.sp)
                MetricText(text = "${totalWaterMl.toInt()}", color = GlanceColors.Water, fontSize = 24.sp)
                CaptionText(text = "ml", fontSize = 12.sp, color = GlanceColors.WaterLight)
            }

            LayoutSize.LARGE -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "HYDRATION", fontSize = 11.sp)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 36.sp)
                Spacer(modifier = GlanceModifier.height(4.dp))
                HorizontalRule()
                Spacer(modifier = GlanceModifier.height(4.dp))
                LabelValueRow(label = "RATE", value = rateLabel, valueColor = GlanceColors.Water, fontSize = 16.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                LabelValueRow(label = "SIPS", value = "$sipCount", valueColor = GlanceColors.Water, fontSize = 16.sp)
            }

            LayoutSize.NARROW -> Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionText(text = "HYDRATION", fontSize = 11.sp)
                MetricText(text = totalLabel, color = GlanceColors.Water, fontSize = 24.sp)
                Spacer(modifier = GlanceModifier.height(2.dp))
                CaptionText(text = rateLabel, fontSize = 13.sp, color = GlanceColors.Water)
            }
        }
    }
}
