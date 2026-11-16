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

/**
 * One-tap "Ride Plan" button (spec 3.1 extension): tap to pick how long you're planning to
 * ride, same picker-menu interaction as Quick Log. Feeds [com.nutripro.engine.DurationGate]
 * when no route is loaded. Re-renders on a timer since the chosen plan isn't part of the
 * live ride state - just a Preferences value.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class RidePlanDataType(
    private val preferences: Preferences,
    extension: String,
) : DataTypeImpl(extension, "ride-plan") {
    private val glance = GlanceRemoteViews()

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
        }
        val viewJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val minutes = preferences.plannedRideMinutes
                val valueText = if (minutes <= 0) {
                    "SET"
                } else if (minutes >= 60) {
                    "${minutes / 60}h${if (minutes % 60 != 0) (minutes % 60).toString() else ""}"
                } else {
                    "${minutes}m"
                }
                val result = glance.compose(context, DpSize.Unspecified) {
                    QuickActionButtonView(
                        label = "RIDE PLAN",
                        valueText = valueText,
                        unitText = "",
                        accentColor = GlanceColors.Accent,
                        quickLogMode = "duration",
                        viewConfig = config,
                    )
                }
                emitter.updateView(result.remoteViews)
                kotlinx.coroutines.delay(5_000L)
            }
        }
        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }
}
