package com.nutripro.karoo

import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.model.RideNutritionState
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Base for data fields that are just one rounded number pulled straight off
 * [RideNutritionState] with no graphical rendering of their own. Subclasses
 * supply the type id and the field they stream; the collect/emit/cancel
 * wiring lives here once instead of being repeated per field.
 */
abstract class SingleValueDataType(
    private val tracker: CarbBalanceTracker,
    extension: String,
    typeId: String,
    private val select: (RideNutritionState) -> Double,
) : DataTypeImpl(extension, typeId) {

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            tracker.stateFlow.collect { state ->
                val point = DataPoint(dataTypeId, values = mapOf(DataType.Field.SINGLE to round(select(state))))
                emitter.onNext(StreamState.Streaming(point))
            }
        }
        emitter.setCancellable { job.cancel() }
    }
}

class BurnRateDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "burn-rate", select = { it.burnRateGph })

class CarbsBurnedDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "carbs-burned", select = { it.totalBurned })

class CarbsEatenDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "carbs-eaten", select = { it.totalEaten })

class FluidBurnedDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "fluid-burned", select = { it.sweatLossMl })

class FluidEatenDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "fluid-eaten", select = { it.totalWaterMl })

class FluidRateDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "fluid-rate", select = { it.sweatRateLph * 1000.0 })

class SodiumBurnedDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "sodium-burned", select = { it.sodiumLossMg })

class SodiumEatenDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "sodium-eaten", select = { it.totalSodiumMg })

/** Average sodium loss rate so far this ride (mg/h) - no live instantaneous rate is tracked. */
class SodiumRateDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(
        tracker,
        extension,
        "sodium-rate",
        select = { if (it.elapsedRideSec > 0) it.sodiumLossMg / (it.elapsedRideSec / 3600.0) else 0.0 },
    )
