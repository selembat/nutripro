package com.nutripro.karoo

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.nutripro.glance.GlanceColors
import com.nutripro.glance.RideSetupFieldView
import com.nutripro.glance.rememberFieldColors
import com.nutripro.model.RideMode
import com.nutripro.util.Preferences
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * One-tap "Ride Setup" field (spec 3.1 extension): shows the current ride mode plus the
 * planned ride length in one tile, tap opens [com.nutripro.ui.RideSetupScreen]'s combined
 * mode-then-duration picker. Keeps the "ride-plan" typeId so existing Karoo screen layouts
 * that already placed this field don't need to be re-added. Re-renders on a timer
 * since neither value is part of the live ride state - just [Preferences] values.
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
                val durationText = when {
                    minutes <= 0 -> "using route"
                    minutes >= 60 -> "${minutes / 60}h${if (minutes % 60 != 0) (minutes % 60).toString() else ""} plan"
                    else -> "${minutes}m plan"
                }
                val mode = preferences.rideMode
                val modeText = when (mode) {
                    RideMode.CASUAL_Z2 -> "CASUAL Z2"
                    RideMode.CASUAL_SPORT -> "SPORT"
                    RideMode.WORKOUT -> "WORKOUT"
                    RideMode.RACE -> "RACE"
                    else -> mode
                }
                val result = glance.compose(context, DpSize.Unspecified) {
                    val (bg, label) = rememberFieldColors(preferences.themeMode)
                    RideSetupFieldView(
                        label = "RIDE SETUP",
                        primaryValueText = modeText,
                        secondaryValueText = durationText,
                        accentColor = GlanceColors.rideModeAccent(mode),
                        quickLogMode = "ride-setup",
                        viewConfig = config,
                        backgroundColor = bg,
                        labelColor = label,
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
