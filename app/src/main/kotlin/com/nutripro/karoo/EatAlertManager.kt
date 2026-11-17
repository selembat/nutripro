package com.nutripro.karoo

import android.content.Context
import com.nutripro.R
import com.nutripro.engine.AlertChannel
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.engine.PreClimbNudge
import com.nutripro.engine.PreIntervalNudge
import com.nutripro.engine.TemperatureFallback
import com.nutripro.engine.UnsafeConditionDetector
import com.nutripro.engine.VirtualPowerEstimator
import com.nutripro.model.RideMode
import com.nutripro.util.Preferences
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.OnLocationChanged
import io.hammerhead.karooext.models.OnNavigationState
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.SystemNotification
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Drives the smart-fueling engine (spec 3.1-3.4): ticks the carb/fluid debt engines once a
 * second from live power/temperature, decides when a due alert is safe to fire (hold during
 * hard efforts/descents, release once safe, re-remind if nothing is logged), and dispatches
 * the resulting [InRideAlert]s.
 */
class EatAlertManager(
    private val context: Context,
    private val karooSystem: KarooSystemService,
    private val tracker: CarbBalanceTracker,
    private val preferences: Preferences,
) {
    companion object {
        private const val WEATHER_REFRESH_MS = 15 * 60_000L
        private const val WEATHER_MAX_AGE_MS = 20 * 60_000L
        private const val SENSOR_FRESH_WINDOW_MS = 5_000L
        private const val ASSUMED_BIKE_WEIGHT_KG = 9.0
        private const val DUAL_SOURCE_THRESHOLD_GPH = 60.0
        private const val RACE_DUAL_SOURCE_THRESHOLD_GPH = 50.0
        private const val RACE_MAX_HOLD_SEC = 2 * 60L
        private const val RACE_RE_REMIND_SEC = 6 * 60L
    }

    private var rideStateJob: Job? = null
    private var sensorJob: Job? = null
    private var tickJob: Job? = null
    private var recordingStartTimeMs = 0L
    private var pausedAtMs = 0L

    @Volatile private var powerWatts: Double = 0.0
    @Volatile private var lastRealPowerAtMs: Long = 0L
    @Volatile private var power30sWatts: Double = 0.0
    @Volatile private var speedMps: Double = 0.0
    @Volatile private var gradePercent: Double = 0.0
    @Volatile private var ftpPercentLive: Double = 0.0
    @Volatile private var lastFtpPercentAtMs: Long = 0L
    @Volatile private var hrPercentLive: Double = 0.0
    @Volatile private var lastHrPercentAtMs: Long = 0L
    @Volatile private var ftpWatts: Double = 0.0
    @Volatile private var maxHrBpm: Int = 0
    @Volatile private var totalWeightKg: Double = 0.0
    @Volatile private var bottleWarningFired: Boolean = false
    @Volatile private var bottlesEmptyFired: Boolean = false

    // Pre-climb nudge (spec 3.4) — only available while navigating a loaded route, since that's
    // the only mode exposing a fixed route distance to measure climb positions against.
    @Volatile private var routeClimbs: List<OnNavigationState.NavigationState.Climb> = emptyList()
    @Volatile private var routeDistanceM: Double = 0.0
    @Volatile private var hasRoute: Boolean = false
    @Volatile private var distanceToDestinationM: Double = 0.0
    @Volatile private var timeToDestinationSec: Long = 0L
    private var lastNudgedClimbStartDistance: Double? = null
    @Volatile private var durationNoticeFired: Boolean = false

    // Ride mode (spec 3.1 extension): confirmed only once the rider has explicitly picked a
    // mode this ride (manually, or by accepting the workout-detected suggestion) - a sticky
    // Preferences default is NOT by itself "confirmed", see DurationGate's modeConfirmed param.
    @Volatile private var rideModeConfirmedThisRide: Boolean = false
    @Volatile private var racePreloadFired: Boolean = false

    // Pre-interval nudge (spec 3.1 extension, Workout mode) - only available while a
    // structured workout is loaded, same current-step-only limitation as the Karoo SDK.
    @Volatile private var zoneRanges: List<IntRange> = emptyList()
    @Volatile private var workoutActive: Boolean = false
    @Volatile private var workoutTargetValue: Double? = null
    @Volatile private var workoutTimeToStepFinishSec: Long? = null
    @Volatile private var workoutCurrentStep: Int = 0
    @Volatile private var workoutTotalSteps: Int = 0
    private var lastNudgedWorkoutStep: Int? = null
    private var workoutSuggestionFired: Boolean = false
    private val preIntervalNudge = PreIntervalNudge()

    // Temperature fallback chain (spec 3.3): weather API -> BLE sensor (not yet implemented) -> manual.
    @Volatile private var hasLocation: Boolean = false
    @Volatile private var currentLat: Double = 0.0
    @Volatile private var currentLng: Double = 0.0
    @Volatile private var weatherTempC: Double? = null
    @Volatile private var weatherTempAtMs: Long? = null
    @Volatile private var deviceSensorTempC: Double? = null
    private val weatherService = WeatherService(karooSystem)

    private val unsafeDetector = UnsafeConditionDetector()
    private val preClimbNudge = PreClimbNudge()
    private val carbChannel = AlertChannel()
    private val fluidChannel = AlertChannel()
    private val sodiumChannel = AlertChannel()
    private var prevCarbDebtGrams = 0.0
    private var prevFluidDebtMl = 0.0

    fun start(scope: CoroutineScope) {
        rideStateJob = scope.launch {
            karooSystem.consumerFlow<RideState>().collect { rideState ->
                when (rideState) {
                    is RideState.Recording -> {
                        val now = System.currentTimeMillis()
                        if (recordingStartTimeMs == 0L) {
                            recordingStartTimeMs = now
                        } else if (pausedAtMs != 0L) {
                            // Shift the clock forward by the pause duration so elapsedSec
                            // reflects real ride time, not wall-clock time including the pause.
                            recordingStartTimeMs += now - pausedAtMs
                            pausedAtMs = 0L
                        }
                        startSensorStreams(scope)
                        startTick(scope)
                        checkRacePreload()
                    }
                    is RideState.Paused -> {
                        if (pausedAtMs == 0L) {
                            pausedAtMs = System.currentTimeMillis()
                        }
                        stopTick()
                    }
                    is RideState.Idle -> {
                        stopSensorStreams()
                        stopTick()
                        recordingStartTimeMs = 0L
                        pausedAtMs = 0L
                        bottleWarningFired = false
                        bottlesEmptyFired = false
                        carbChannel.reset()
                        fluidChannel.reset()
                        sodiumChannel.reset()
                        hasRoute = false
                        routeClimbs = emptyList()
                        lastNudgedClimbStartDistance = null
                        durationNoticeFired = false
                        preferences.plannedRideMinutes = 0
                        hasLocation = false
                        weatherTempC = null
                        weatherTempAtMs = null
                        deviceSensorTempC = null
                        rideModeConfirmedThisRide = false
                        racePreloadFired = false
                        workoutActive = false
                        workoutTargetValue = null
                        workoutTimeToStepFinishSec = null
                        workoutCurrentStep = 0
                        workoutTotalSteps = 0
                        lastNudgedWorkoutStep = null
                        workoutSuggestionFired = false
                    }
                }
            }
        }
    }

    /**
     * Rider explicitly picked (or accepted a suggestion for) a ride mode this ride (spec 3.1
     * extension) - sticky in [Preferences] for next ride's default, and marks the duration
     * gate as overridden for the rest of *this* ride via [rideModeConfirmedThisRide].
     */
    fun confirmRideMode(mode: String) {
        preferences.rideMode = mode
        rideModeConfirmedThisRide = true
        Timber.d("Ride mode confirmed: %s", mode)
        // Covers picking Race moments into the ride, not just when it was already the sticky
        // default at RideState.Recording - checkRacePreload() is itself idempotent.
        checkRacePreload()
    }

    private fun startSensorStreams(scope: CoroutineScope) {
        if (sensorJob?.isActive == true) return
        sensorJob = scope.launch {
            val profile = karooSystem.consumerFlow<UserProfile>().first()
            val ftp = if (preferences.ftpOverride > 0) preferences.ftpOverride else profile.ftp
            ftpWatts = ftp.toDouble()
            val hasFtp = ftp > 0
            val hasMaxHr = profile.maxHr > 0
            maxHrBpm = profile.maxHr
            val riderWeightKg = if (preferences.weightOverride > 0f) preferences.weightOverride.toDouble() else profile.weight.toDouble()
            totalWeightKg = riderWeightKg + ASSUMED_BIKE_WEIGHT_KG

            // Rider's own configured zones (spec 3.1 extension), whichever target type we'll
            // actually have live data for - same hasFtp/hasMaxHr split as the intensity source.
            zoneRanges = when {
                hasFtp -> profile.powerZones.map { it.min..it.max }
                hasMaxHr -> profile.heartRateZones.map { it.min..it.max }
                else -> emptyList()
            }

            launch {
                karooSystem.streamDataFlow(DataType.Type.POWER)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect {
                        powerWatts = it
                        lastRealPowerAtMs = System.currentTimeMillis()
                    }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.SMOOTHED_30S_AVERAGE_POWER)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { power30sWatts = it }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.SPEED)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { speedMps = it }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.ELEVATION_GRADE)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { gradePercent = it }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.DISTANCE_TO_DESTINATION)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { distanceToDestinationM = it }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.TIME_TO_DESTINATION)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { timeToDestinationSec = it.toLong() }
            }
            launch {
                karooSystem.streamDataFlow(DataType.Type.TEMPERATURE)
                    .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                    .collect { deviceSensorTempC = it }
            }
            launch {
                karooSystem.consumerFlow<OnLocationChanged>().collect { location ->
                    currentLat = location.lat
                    currentLng = location.lng
                    hasLocation = true
                }
            }
            if (preferences.weatherEnabled) {
                launch { pollWeather() }
            }
            launch {
                karooSystem.consumerFlow<OnNavigationState>().collect { event ->
                    when (val navState = event.state) {
                        is OnNavigationState.NavigationState.NavigatingRoute -> {
                            hasRoute = true
                            routeDistanceM = navState.routeDistance
                            routeClimbs = navState.climbs
                        }
                        else -> {
                            // Point-to-point navigation and idle don't expose a fixed route
                            // distance to measure climb positions against — no nudge then.
                            hasRoute = false
                            routeClimbs = emptyList()
                        }
                    }
                }
            }
            if (hasFtp) {
                launch {
                    karooSystem.streamDataFlow(DataType.Type.PERCENT_MAX_FTP)
                        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                        .collect {
                            ftpPercentLive = it
                            lastFtpPercentAtMs = System.currentTimeMillis()
                        }
                }
            }
            if (hasMaxHr) {
                launch {
                    karooSystem.streamDataFlow(DataType.Type.PERCENT_MAX_HR)
                        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                        .collect {
                            hrPercentLive = it
                            lastHrPercentAtMs = System.currentTimeMillis()
                        }
                }
            }
            if (hasFtp || hasMaxHr) {
                launch {
                    karooSystem.streamDataFlow(DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION)
                        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.values }
                        .collect { values ->
                            // WORKOUT_STATE's exact value scheme isn't documented by the SDK;
                            // treat present-and-positive as "a workout is active" and absent as not.
                            workoutActive = (values[DataType.Field.WORKOUT_STATE] ?: 0.0) > 0.0
                            workoutTimeToStepFinishSec = values[DataType.Field.WORKOUT_TIME_TO_STEP_FINISH]?.toLong()
                        }
                }
                launch {
                    karooSystem.streamDataFlow(DataType.Type.WORKOUT_INTERVAL_COUNT)
                        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.values }
                        .collect { values ->
                            workoutCurrentStep = values[DataType.Field.WORKOUT_CURRENT_STEP]?.toInt() ?: 0
                            workoutTotalSteps = values[DataType.Field.WORKOUT_STEP_COUNT]?.toInt() ?: 0
                        }
                }
                launch {
                    val workoutTargetType = if (hasFtp) DataType.Type.WORKOUT_POWER_TARGET else DataType.Type.WORKOUT_HEART_RATE_TARGET
                    karooSystem.streamDataFlow(workoutTargetType)
                        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.values }
                        .collect { values -> workoutTargetValue = values[DataType.Field.WORKOUT_TARGET_VALUE] }
                }
            }
        }
    }

    /**
     * Picks the best available intensity source, live each tick rather than fixed for the
     * ride: a real power meter's %FTP, else a HR strap's %maxHR, else a "virtual power"
     * estimate from speed/grade/weight converted to %FTP (if FTP is known), else a flat
     * moderate-intensity fallback so the carb-debt engine still runs on a time-based schedule.
     */
    private fun resolveIntensityPercent(nowMs: Long): Double {
        if (preferences.simulationModeEnabled) {
            return simulatedIntensityPercent()
        }
        val ftpPercentFresh = (nowMs - lastFtpPercentAtMs) <= SENSOR_FRESH_WINDOW_MS
        val hrPercentFresh = (nowMs - lastHrPercentAtMs) <= SENSOR_FRESH_WINDOW_MS
        return when {
            ftpPercentFresh -> ftpPercentLive
            hrPercentFresh -> hrPercentLive
            ftpWatts > 0 -> {
                val estimatedWatts = VirtualPowerEstimator.estimateWatts(speedMps, gradePercent, totalWeightKg)
                estimatedWatts / ftpWatts * 100.0
            }
            else -> 65.0
        }
    }

    /** Simulated power's %FTP if FTP is known, else simulated HR's %maxHR, else a flat fallback. */
    private fun simulatedIntensityPercent(): Double {
        val simulatedPower = preferences.simulatedPowerWatts.toDouble()
        val simulatedHr = preferences.simulatedHeartRateBpm
        return when {
            ftpWatts > 0 -> simulatedPower / ftpWatts * 100.0
            maxHrBpm > 0 -> simulatedHr.toDouble() / maxHrBpm * 100.0
            else -> 65.0
        }
    }

    /** Real power when fresh, else the same virtual-power estimate, for the sweat model. */
    private fun resolvePowerWatts(nowMs: Long): Double {
        if (preferences.simulationModeEnabled) {
            return preferences.simulatedPowerWatts.toDouble()
        }
        val realPowerFresh = (nowMs - lastRealPowerAtMs) <= SENSOR_FRESH_WINDOW_MS
        return if (realPowerFresh) {
            powerWatts
        } else {
            VirtualPowerEstimator.estimateWatts(speedMps, gradePercent, totalWeightKg)
        }
    }

    /**
     * Best available ride-duration estimate for [DurationGate] (spec 3.1 extension): a
     * loaded route's live elapsed+ETA, else a manually chosen ride plan, else elapsed time
     * so far - which lets the carb target ramp up naturally the longer an unplanned ride runs.
     */
    private fun resolvePlanSec(elapsedSec: Long): Long {
        val plannedMinutes = preferences.plannedRideMinutes
        return when {
            hasRoute -> elapsedSec + timeToDestinationSec
            plannedMinutes > 0 -> plannedMinutes * 60L
            else -> elapsedSec
        }
    }

    private fun stopSensorStreams() {
        sensorJob?.cancel()
        sensorJob = null
    }

    private suspend fun pollWeather() {
        while (true) {
            if (hasLocation) {
                val temp = weatherService.fetchTemperatureC(currentLat, currentLng)
                if (temp != null) {
                    weatherTempC = temp
                    weatherTempAtMs = System.currentTimeMillis()
                    Timber.d("Weather temperature: %.1f°C", temp)
                }
            }
            delay(WEATHER_REFRESH_MS)
        }
    }

    private fun currentTempC(): Double {
        val now = System.currentTimeMillis()
        val weatherAgeMs = weatherTempAtMs?.let { now - it }
        val (tempC, _) = TemperatureFallback.resolve(
            weatherTempC = weatherTempC,
            weatherAgeMs = weatherAgeMs,
            maxWeatherAgeMs = WEATHER_MAX_AGE_MS,
            sensorTempC = deviceSensorTempC,
            manualTempC = preferences.manualTempC.toDouble(),
        )
        return tempC
    }

    private fun startTick(scope: CoroutineScope) {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            var lastTickMs = System.currentTimeMillis()
            while (true) {
                delay(1_000L)
                val now = System.currentTimeMillis()
                val dtSec = (now - lastTickMs) / 1000.0
                lastTickMs = now
                val elapsedSec = (now - recordingStartTimeMs) / 1000L
                val planSec = resolvePlanSec(elapsedSec)

                tracker.tickDebt(
                    dtSec,
                    resolveIntensityPercent(now),
                    resolvePowerWatts(now),
                    currentTempC(),
                    planSec,
                    modeConfirmed = rideModeConfirmedThisRide,
                    isRaceMode = preferences.rideMode == RideMode.RACE,
                )
                checkDurationNotice()
                checkWorkoutModeSuggestion()
                onTick(elapsedSec)
            }
        }
    }

    private fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun onTick(elapsedSec: Long) {
        val state = tracker.stateFlow.value
        if (state.carbDebtGrams < prevCarbDebtGrams) carbChannel.onIntakeLogged()
        if (state.fluidDebtMl < prevFluidDebtMl) fluidChannel.onIntakeLogged()
        prevCarbDebtGrams = state.carbDebtGrams
        prevFluidDebtMl = state.fluidDebtMl

        val isUnsafe = unsafeDetector.isUnsafe(power30sWatts, ftpWatts, speedMps, gradePercent)
        val isRaceMode = preferences.rideMode == RideMode.RACE
        // Race mode only ever tightens the rider's own hold/re-remind settings, never loosens them.
        val maxHoldSec = if (isRaceMode) preferences.alertHoldMaxSec.coerceAtMost(RACE_MAX_HOLD_SEC) else preferences.alertHoldMaxSec
        val reRemindAfterSec = if (isRaceMode) preferences.reRemindAfterSec.coerceAtMost(RACE_RE_REMIND_SEC) else preferences.reRemindAfterSec

        if (preferences.carbAlertsEnabled &&
            carbChannel.evaluate(elapsedSec, tracker.isCarbAlertDue(), isUnsafe, maxHoldSec, reRemindAfterSec)
        ) {
            dispatchEatAlert(state.carbDebtGrams)
        }

        if (preferences.fluidAlertsEnabled &&
            fluidChannel.evaluate(elapsedSec, tracker.isFluidAlertDue(), isUnsafe, maxHoldSec, reRemindAfterSec)
        ) {
            dispatchDrinkAlert()
        }

        if (preferences.sodiumAlertsEnabled &&
            sodiumChannel.evaluate(elapsedSec, tracker.isSodiumAlertDue(), isUnsafe, maxHoldSec, reRemindAfterSec)
        ) {
            dispatchSodiumAlert(state.sodiumDebtMg)
        }

        if (tracker.isBottleNearlyEmpty()) {
            if (!bottleWarningFired) {
                bottleWarningFired = true
                dispatchBottleWarning()
            }
        } else {
            bottleWarningFired = false
        }

        if (tracker.isBottleEmpty()) {
            if (!bottlesEmptyFired) {
                bottlesEmptyFired = true
                dispatchBottlesEmpty()
            }
        } else {
            bottlesEmptyFired = false
        }

        checkPreClimbNudge(state.carbDebtGrams)
        checkPreIntervalNudge(state.carbDebtGrams)
    }

    /** One-time heads-up (spec 3.1 extension) that a structured workout is active, offering to
     * switch to Workout mode - only while the rider hasn't already confirmed a mode this ride. */
    private fun checkWorkoutModeSuggestion() {
        if (workoutSuggestionFired || rideModeConfirmedThisRide || !workoutActive) return
        workoutSuggestionFired = true
        karooSystem.dispatch(
            SystemNotification(
                "nutripro-workout-detected",
                context.getString(R.string.alert_workout_detected_title),
                action = context.getString(R.string.action_switch_workout_mode),
                actionIntent = "com.nutripro.SET_RIDE_MODE_WORKOUT",
            ),
        )
    }

    /** One-time pre-ride-start reminder for Race mode (spec 3.1 extension): race fueling is
     * front-loaded, unlike the purely reactive alerts the rest of the engine runs on. */
    private fun checkRacePreload() {
        if (racePreloadFired || preferences.rideMode != RideMode.RACE) return
        racePreloadFired = true
        dispatchAlert(
            idPrefix = "race-preload",
            icon = R.drawable.ic_food,
            title = context.getString(R.string.alert_race_preload_title),
            detail = context.getString(R.string.alert_race_preload_detail),
            backgroundColor = R.color.alert_eat,
            autoDismissMs = 10_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
            ),
        )
    }

    /** "Interval ending soon - eat now" (spec 3.1 extension, Workout mode): fires while the
     * current step is a Z2 recovery that's about to end and isn't the workout's last step. */
    private fun checkPreIntervalNudge(carbDebtGrams: Double) {
        if (!preferences.carbAlertsEnabled || !workoutActive) return
        if (lastNudgedWorkoutStep == workoutCurrentStep) return

        val zone = preIntervalNudge.classifyZone(workoutTargetValue, zoneRanges)
        if (preIntervalNudge.shouldNudge(
                currentZone = zone,
                timeToStepFinishSec = workoutTimeToStepFinishSec,
                currentStepIndex = workoutCurrentStep,
                totalSteps = workoutTotalSteps,
                debtGrams = carbDebtGrams,
                servingGrams = preferences.servingGrams.toDouble(),
            )
        ) {
            lastNudgedWorkoutStep = workoutCurrentStep
            dispatchIntervalAheadAlert()
        }
    }

    /** One-time heads-up (spec 3.1 extension): explains the first-hour gate when we have no
     * better duration estimate than "wait and see", so the lack of early alerts isn't a mystery. */
    private fun checkDurationNotice() {
        if (durationNoticeFired) return
        if (hasRoute || preferences.plannedRideMinutes > 0) return
        durationNoticeFired = true
        dispatchAlert(
            idPrefix = "duration-notice",
            icon = R.drawable.ic_food,
            title = context.getString(R.string.alert_no_route_title),
            detail = context.getString(R.string.alert_no_route_detail),
            backgroundColor = R.color.alert_eat,
            autoDismissMs = 8_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 900, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 900, durationMs = 150),
            ),
        )
    }

    private fun checkPreClimbNudge(carbDebtGrams: Double) {
        if (!hasRoute || routeClimbs.isEmpty()) return
        val distanceAlongRouteM = (routeDistanceM - distanceToDestinationM).coerceAtLeast(0.0)
        val nextClimb = routeClimbs
            .filter { it.startDistance > distanceAlongRouteM }
            .minByOrNull { it.startDistance } ?: return

        // Once a climb is behind us, allow it to be nudged again if somehow re-approached
        // (e.g. a rejoin), but never re-fire for the same upcoming climb every tick.
        if (nextClimb.startDistance == lastNudgedClimbStartDistance) return

        val climbDistanceM = nextClimb.startDistance - distanceAlongRouteM
        val climbEtaSec = if (speedMps > 1.0) (climbDistanceM / speedMps).toLong() else null

        if (preClimbNudge.shouldNudge(climbDistanceM, climbEtaSec, carbDebtGrams, preferences.servingGrams.toDouble())) {
            lastNudgedClimbStartDistance = nextClimb.startDistance
            dispatchClimbAheadAlert()
        }
    }

    private fun dispatchAlert(
        idPrefix: String,
        icon: Int,
        title: String,
        detail: String,
        backgroundColor: Int,
        autoDismissMs: Long,
        tones: List<PlayBeepPattern.Tone>,
    ) {
        karooSystem.dispatch(
            InRideAlert(
                id = "$idPrefix-${System.currentTimeMillis()}",
                icon = icon,
                title = title,
                detail = detail,
                autoDismissMs = autoDismissMs,
                backgroundColor = backgroundColor,
                textColor = R.color.white,
            ),
        )
        if (preferences.soundEnabled) {
            karooSystem.dispatch(PlayBeepPattern(tones))
        }
    }

    private fun dispatchEatAlert(carbDebtGrams: Double) {
        val defaultTemplate = preferences.getDefaultTemplate()
        Timber.d("Eat alert: debt=%.0fg, recommend=%dg", carbDebtGrams, defaultTemplate.carbsGrams)

        // Above ~60g/h a single sugar source (glucose/maltodextrin alone) saturates the gut's
        // SGLT1 transporter - a 2:1 or 1:0.8 glucose:fructose blend is needed to actually
        // absorb it without GI distress. Race mode warns earlier since sustained high rates
        // are more likely there.
        val carbTargetGph = tracker.stateFlow.value.carbTargetGph
        val dualSourceThresholdGph = if (preferences.rideMode == RideMode.RACE) RACE_DUAL_SOURCE_THRESHOLD_GPH else DUAL_SOURCE_THRESHOLD_GPH
        val compositionNote = if (carbTargetGph > dualSourceThresholdGph) {
            context.getString(R.string.composition_note)
        } else {
            ""
        }
        val eatDetail = context.getString(R.string.alert_eat_detail_full, defaultTemplate.carbsGrams, carbDebtGrams.toInt(), compositionNote)

        dispatchAlert(
            idPrefix = "eat-reminder",
            icon = R.drawable.ic_food,
            title = context.getString(R.string.alert_eat_title),
            detail = eatDetail,
            backgroundColor = R.color.alert_eat,
            autoDismissMs = 10_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 200),
                PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                PlayBeepPattern.Tone(frequency = 1200, durationMs = 200),
            ),
        )

        karooSystem.dispatch(
            SystemNotification(
                "eat-notification",
                context.getString(R.string.notif_eat_text, eatDetail),
                action = context.getString(R.string.action_log_food_grams, defaultTemplate.name, defaultTemplate.carbsGrams),
                actionIntent = "com.nutripro.QUICK_LOG",
            ),
        )
    }

    private fun dispatchDrinkAlert() {
        Timber.d("Drink alert")
        dispatchAlert(
            idPrefix = "drink-reminder",
            icon = R.drawable.ic_water,
            title = context.getString(R.string.alert_drink_title),
            detail = context.getString(R.string.alert_drink_detail),
            backgroundColor = R.color.alert_drink,
            autoDismissMs = 8_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
            ),
        )

        val sipMl = preferences.sipAmountMl.toInt()
        karooSystem.dispatch(
            SystemNotification(
                "drink-notification",
                context.getString(R.string.notif_drink_text),
                action = context.getString(R.string.action_log_sip, sipMl),
                actionIntent = "com.nutripro.QUICK_LOG_WATER",
            ),
        )
    }

    private fun dispatchSodiumAlert(sodiumDebtMg: Double) {
        Timber.d("Sodium alert: debt=%.0fmg", sodiumDebtMg)
        dispatchAlert(
            idPrefix = "sodium-reminder",
            icon = R.drawable.ic_sodium,
            title = context.getString(R.string.alert_low_sodium_title),
            detail = context.getString(R.string.alert_low_sodium_detail, sodiumDebtMg.toInt()),
            backgroundColor = R.color.alert_sodium,
            autoDismissMs = 8_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 700, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                PlayBeepPattern.Tone(frequency = 700, durationMs = 150),
            ),
        )

        val doseMg = preferences.sodiumDoseMg.toInt()
        karooSystem.dispatch(
            SystemNotification(
                "sodium-notification",
                context.getString(R.string.notif_low_sodium_text, sodiumDebtMg.toInt()),
                action = context.getString(R.string.action_log_sodium_dose, doseMg),
                actionIntent = "com.nutripro.QUICK_LOG_SODIUM",
            ),
        )
    }

    private fun dispatchClimbAheadAlert() {
        Timber.d("Climb ahead nudge")
        dispatchAlert(
            idPrefix = "climb-ahead",
            icon = R.drawable.ic_food,
            title = context.getString(R.string.alert_climb_title),
            detail = context.getString(R.string.alert_climb_detail),
            backgroundColor = R.color.alert_eat,
            autoDismissMs = 10_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 1200, durationMs = 200),
            ),
        )
    }

    private fun dispatchIntervalAheadAlert() {
        Timber.d("Interval-ending nudge (step %d/%d)", workoutCurrentStep, workoutTotalSteps)
        dispatchAlert(
            idPrefix = "interval-ahead",
            icon = R.drawable.ic_food,
            title = context.getString(R.string.alert_interval_title),
            detail = context.getString(R.string.alert_interval_detail),
            backgroundColor = R.color.alert_eat,
            autoDismissMs = 10_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 80),
                PlayBeepPattern.Tone(frequency = 1200, durationMs = 200),
            ),
        )
    }

    private fun dispatchBottleWarning() {
        Timber.d("Bottle nearly empty")
        karooSystem.dispatch(
            SystemNotification(
                "bottle-warning",
                context.getString(R.string.notif_bottle_low),
            ),
        )
    }

    private fun dispatchBottlesEmpty() {
        Timber.d("Bottles empty")
        val capacityMl = (preferences.bottleCount * preferences.bottleVolumeMl).toInt()
        karooSystem.dispatch(
            SystemNotification(
                "bottles-empty",
                context.getString(R.string.notif_bottles_empty, capacityMl),
            ),
        )
    }

    fun stop() {
        stopSensorStreams()
        stopTick()
        rideStateJob?.cancel()
        rideStateJob = null
        recordingStartTimeMs = 0L
    }
}
