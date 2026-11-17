package com.nutripro.engine

import android.content.Context
import com.nutripro.R
import com.nutripro.karoo.consumerFlow
import com.nutripro.karoo.streamDataFlow
import com.nutripro.util.Preferences
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.SystemNotification
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

private const val NO_FTP_PERCENT_ESTIMATE = 65.0
private const val NO_FTP_CARB_FRACTION = 0.55
private const val MAX_PLAUSIBLE_KCAL_DELTA = 50.0

/** Turns the live calorie counter into carbs burned, using %FTP to split kcal between carbs and fat. */
class CarbBurnEngine(
    private val context: Context,
    private val karooSystem: KarooSystemService,
    private val tracker: CarbBalanceTracker,
    private val preferences: Preferences,
) {
    private var job: Job? = null
    private var lastCumulativeKcal = -1.0
    var hasFtp: Boolean = false
        private set

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            resolveFtp()
            combine(calorieFlow(), carbFractionInputFlow(), karooSystem.consumerFlow<RideState>()) { kcal, ftpPercent, rideState ->
                Triple(kcal, ftpPercent, rideState)
            }.collect { (kcal, ftpPercent, rideState) -> onTick(kcal, ftpPercent, rideState) }
        }
    }

    private suspend fun resolveFtp() {
        val userProfile = karooSystem.consumerFlow<UserProfile>().first()
        val ftp = preferences.ftpOverride.takeIf { it > 0 } ?: userProfile.ftp
        hasFtp = ftp > 0
        Timber.d("User FTP: %d (override: %d), Weight: %.1f", userProfile.ftp, preferences.ftpOverride, userProfile.weight)

        if (!hasFtp) {
            karooSystem.dispatch(
                SystemNotification(
                    "nutripro-no-ftp",
                    context.getString(R.string.notif_set_ftp),
                    action = context.getString(R.string.action_open_settings),
                    actionIntent = "com.nutripro.SETTINGS",
                ),
            )
        }
    }

    private fun calorieFlow(): Flow<Double> =
        karooSystem.streamDataFlow(DataType.Type.CALORIES).mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }

    /** %FTP when we have one to stream, otherwise a flat estimate used for the whole ride. */
    private fun carbFractionInputFlow(): Flow<Double> = if (hasFtp) {
        karooSystem.streamDataFlow(DataType.Type.PERCENT_MAX_FTP).mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
    } else {
        flow { emit(NO_FTP_PERCENT_ESTIMATE) }
    }

    private fun onTick(cumulativeKcal: Double, ftpPercent: Double, rideState: RideState) {
        when (rideState) {
            is RideState.Recording -> onRecordingTick(cumulativeKcal, ftpPercent)
            is RideState.Idle -> lastCumulativeKcal = -1.0
            else -> lastCumulativeKcal = cumulativeKcal // paused: resync so resuming doesn't spike
        }
    }

    private fun onRecordingTick(cumulativeKcal: Double, ftpPercent: Double) {
        if (lastCumulativeKcal < 0) {
            lastCumulativeKcal = cumulativeKcal
            return
        }
        val deltaKcal = cumulativeKcal - lastCumulativeKcal
        lastCumulativeKcal = cumulativeKcal
        if (deltaKcal <= 0 || deltaKcal >= MAX_PLAUSIBLE_KCAL_DELTA) return

        val carbFraction = if (hasFtp) IntensityZone.getCarbFraction(ftpPercent) else NO_FTP_CARB_FRACTION
        tracker.addBurned(deltaKcal * carbFraction / 4.0)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
