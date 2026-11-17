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
import com.nutripro.karoo.LayoutSize
import com.nutripro.karoo.getLayoutSize
import com.nutripro.ui.QuickLogActivity
import io.hammerhead.karooext.models.ViewConfig

/**
 * A tappable data field that instantly logs a fixed intake when tapped - no menu, same
 * effect as a mapped hardware BonusAction, for riders who only have the Karoo screen
 * (spec 3.5). [backgroundColor] can be driven by the current debt level so the field's
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CaptionText(text = label, fontSize = if (layoutSize == LayoutSize.SMALL) 9.sp else 11.sp)
            MetricText(
                text = "$valueText$unitText",
                color = accentColor,
                fontSize = if (layoutSize == LayoutSize.LARGE) 26.sp else 18.sp,
            )
        }
    }
}
