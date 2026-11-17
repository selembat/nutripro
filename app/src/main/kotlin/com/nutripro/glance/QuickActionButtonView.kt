package com.nutripro.glance

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import com.nutripro.karoo.LayoutSize
import com.nutripro.karoo.getLayoutSize
import com.nutripro.ui.QuickLogActivity
import io.hammerhead.karooext.models.ViewConfig

/**
 * A tappable data field - opens a log/select/confirm screen when tapped, same effect as a
 * mapped hardware BonusAction, for riders who only have the Karoo screen (spec 3.5). Laid
 * out like a native Karoo numeric field: caps label top-left, value bottom-right, rather
 * than centered. [backgroundColor] can be driven by the current debt level so the field's
 * urgency is visible at a glance, not just its number.
 */
@Composable
fun QuickActionButtonView(
    label: String,
    valueText: String,
    unitText: String,
    accentColor: Color,
    quickLogMode: String,
    viewConfig: ViewConfig,
    backgroundColor: Color = GlanceColors.Background,
    labelColor: Color = GlanceColors.Label,
) {
    val layoutSize = getLayoutSize(viewConfig)
    val context = LocalContext.current
    val intent = Intent(context, QuickLogActivity::class.java).apply {
        putExtra("mode", quickLogMode)
    }

    FieldSurface(
        backgroundColor = backgroundColor,
        modifier = GlanceModifier.clickable(actionStartActivity(intent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                CaptionText(text = label, fontSize = if (layoutSize == LayoutSize.SMALL) 9.sp else 11.sp, color = labelColor)
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                MetricText(
                    text = "$valueText$unitText",
                    color = accentColor,
                    fontSize = if (layoutSize == LayoutSize.LARGE) 26.sp else 18.sp,
                )
            }
        }
    }
}

/**
 * Same tappable-field shape as [QuickActionButtonView], but with a smaller secondary value
 * line under the primary one (spec 3.1 extension: Ride Setup shows mode + planned duration
 * in one tile rather than two separate fields).
 */
@Composable
fun RideSetupFieldView(
    label: String,
    primaryValueText: String,
    secondaryValueText: String,
    accentColor: Color,
    quickLogMode: String,
    viewConfig: ViewConfig,
    backgroundColor: Color = GlanceColors.Background,
    labelColor: Color = GlanceColors.Label,
) {
    val layoutSize = getLayoutSize(viewConfig)
    val context = LocalContext.current
    val intent = Intent(context, QuickLogActivity::class.java).apply {
        putExtra("mode", quickLogMode)
    }

    FieldSurface(
        backgroundColor = backgroundColor,
        modifier = GlanceModifier.clickable(actionStartActivity(intent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                CaptionText(text = label, fontSize = if (layoutSize == LayoutSize.SMALL) 9.sp else 11.sp, color = labelColor)
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                MetricText(
                    text = primaryValueText,
                    color = accentColor,
                    fontSize = if (layoutSize == LayoutSize.LARGE) 22.sp else 16.sp,
                )
            }
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                CaptionText(text = secondaryValueText, fontSize = if (layoutSize == LayoutSize.SMALL) 8.sp else 10.sp, color = labelColor)
            }
        }
    }
}
