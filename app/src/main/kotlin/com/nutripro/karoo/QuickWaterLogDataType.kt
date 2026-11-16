package com.nutripro.karoo

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.QuickActionButtonView
import com.nutripro.util.Preferences
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** One-tap button (spec 3.5): instantly logs the configured Quick Sip amount, no menu. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class QuickWaterLogDataType(
    private val preferences: Preferences,
    extension: String,
) : DataTypeImpl(extension, "quick-water-log") {
    private val glance = GlanceRemoteViews()

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
        }
        val viewJob = CoroutineScope(Dispatchers.IO).launch {
            val result = glance.compose(context, DpSize.Unspecified) {
                QuickActionButtonView(
                    context = context,
                    label = "QUICK SIP",
                    valueText = "💧 ${preferences.sipAmountMl.toInt()}",
                    unitText = "ml",
                    accentColor = GlanceColors.Water,
                    quickLogMode = "quick-water",
                    viewConfig = config,
                )
            }
            emitter.updateView(result.remoteViews)
        }
        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }
}
