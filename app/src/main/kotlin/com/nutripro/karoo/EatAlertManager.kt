package com.nutripro.karoo

import com.nutripro.R
import com.nutripro.engine.AlertChannel
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.engine.PreClimbNudge
import com.nutripro.engine.TemperatureFallback
import com.nutripro.engine.UnsafeConditionDetector
import com.nutripro.engine.VirtualPowerEstimator
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
    private val karooSystem: KarooSystemService,
    private val tracker: CarbBalanceTracker,
    private val preferences: Preferences,
) {
    companion object {
        private const val WEATHER_REFRESH_MS = 15 * 60_000L
        private const val WEATHER_MAX_AGE_MS = 20 * 60_000L
        private const val SENSOR_FRESH_WINDOW_MS = 5_000L
        private const val ASSUMED_BIKE_WEIGHT_KG = 9.0
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

    // Pre-climb nudge (spec 3.4) — only available while navigating a loaded route, since that's
    // the only mode exposing a fixed route distance to measure climb positions against.
    @Volatile private var routeClimbs: List<OnNavigationState.NavigationState.Climb> = emptyList()
    @Volatile private var routeDistanceM: Double = 0.0
    @Volatile private var hasRoute: Boolean = false
    @Volatile private var distanceToDestinationM: Double = 0.0
    private var lastNudgedClimbStartDistance: Double? = null

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
                        carbChannel.reset()
                        fluidChannel.reset()
                        hasRoute = false
                        routeClimbs = emptyList()
                        lastNudgedClimbStartDistance = null
                        hasLocation = false
                        weatherTempC = null
                        weatherTempAtMs = null
                        deviceSensorTempC = null
                    }
                }
            }
        }
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

                tracker.tickDebt(dtSec, resolveIntensityPercent(now), resolvePowerWatts(now), currentTempC())
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
        val maxHoldSec = preferences.alertHoldMaxSec
        val reRemindAfterSec = preferences.reRemindAfterSec

        if (carbChannel.evaluate(elapsedSec, tracker.isCarbAlertDue(), isUnsafe, maxHoldSec, reRemindAfterSec)) {
            dispatchEatAlert(state.carbDebtGrams)
        }

        if (fluidChannel.evaluate(elapsedSec, tracker.isFluidAlertDue(), isUnsafe, maxHoldSec, reRemindAfterSec)) {
            dispatchDrinkAlert()
        }

        if (!bottleWarningFired && tracker.isBottleNearlyEmpty()) {
            bottleWarningFired = true
            dispatchBottleWarning()
        }

        checkPreClimbNudge(state.carbDebtGrams)
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

        dispatchAlert(
            idPrefix = "eat-reminder",
            icon = R.drawable.ic_food,
            title = "Time to eat!",
            detail = "Recommended: ${defaultTemplate.carbsGrams}g carbs. Debt: ${carbDebtGrams.toInt()}g",
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
                "Time to eat! Recommended: ${defaultTemplate.carbsGrams}g carbs. Debt: ${carbDebtGrams.toInt()}g",
                action = "Log ${defaultTemplate.name} ${defaultTemplate.carbsGrams}g",
                actionIntent = "com.nutripro.QUICK_LOG",
            ),
        )
    }

    private fun dispatchDrinkAlert() {
        Timber.d("Drink alert")
        dispatchAlert(
            idPrefix = "drink-reminder",
            icon = R.drawable.ic_water,
            title = "Drink water!",
            detail = "Stay hydrated — take a sip",
            backgroundColor = R.color.alert_drink,
            autoDismissMs = 8_000,
            tones = listOf(
                PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
                PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
            ),
        )
    }

    private fun dispatchClimbAheadAlert() {
        Timber.d("Climb ahead nudge")
        dispatchAlert(
            idPrefix = "climb-ahead",
            icon = R.drawable.ic_food,
            title = "Climb ahead – eat now",
            detail = "Top up before the climb starts",
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
                "Bottle nearly empty — plan a refill",
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
