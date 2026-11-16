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

/** One-tap button (spec 3.5): instantly logs the default food template, no menu. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class QuickFoodLogDataType(
    private val preferences: Preferences,
    extension: String,
) : DataTypeImpl(extension, "quick-food-log") {
    private val glance = GlanceRemoteViews()

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
        }
        val viewJob = CoroutineScope(Dispatchers.IO).launch {
            val template = preferences.getDefaultTemplate()
            val result = glance.compose(context, DpSize.Unspecified) {
                QuickActionButtonView(
                    context = context,
                    label = "QUICK LOG",
                    valueText = "${template.emoji.ifEmpty { "⚡" }} ${template.carbsGrams}",
                    unitText = "g",
                    accentColor = GlanceColors.Food,
                    quickLogMode = "quick-food",
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
