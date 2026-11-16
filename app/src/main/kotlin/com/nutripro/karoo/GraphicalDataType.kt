package com.nutripro.karoo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base for data fields that have both a plain numeric stream (for the ride
 * summary / FIT export) and a custom Glance graphic. The stream wiring and
 * the preview-vs-live rendering branch are identical for every such field,
 * so they live here once; subclasses only decide which number to stream and
 * how to draw [Content] given either a live state or a null (preview) one.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class GraphicalDataType(
    private val tracker: CarbBalanceTracker,
    extension: String,
    typeId: String,
    private val streamValue: (RideNutritionState) -> Double,
) : DataTypeImpl(extension, typeId) {

    private val glanceRenderer = GlanceRemoteViews()

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            tracker.stateFlow.collect { state ->
                val point = DataPoint(dataTypeId, values = mapOf(DataType.Field.SINGLE to streamValue(state)))
                emitter.onNext(StreamState.Streaming(point))
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    /** Draws the field. [state] is null only for the layout-editor preview. */
    @Composable
    protected abstract fun Content(state: RideNutritionState?, viewConfig: ViewConfig)

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val headerJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
        }
        val renderJob = CoroutineScope(Dispatchers.IO).launch {
            suspend fun render(state: RideNutritionState?) {
                val composed = glanceRenderer.compose(context, DpSize.Unspecified) { Content(state, config) }
                emitter.updateView(composed.remoteViews)
            }
            if (config.preview) {
                render(null)
            } else {
                tracker.stateFlow.collect { render(it) }
            }
        }
        emitter.setCancellable {
            headerJob.cancel()
            renderJob.cancel()
        }
    }
}
