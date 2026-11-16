package com.nutripro.karoo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.nutripro.BuildConfig
import com.nutripro.R
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.engine.CarbBurnEngine
import com.nutripro.model.IntakeEntry
import com.nutripro.model.RideNutritionState
import com.nutripro.ui.NutriProExtensionHolder
import com.nutripro.ui.QuickLogActivity
import com.nutripro.util.Preferences
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.DeveloperField
import io.hammerhead.karooext.models.FieldValue
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.SystemNotification
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

/** One FIT developer field: how to read it off [RideNutritionState] plus its FIT metadata. */
private class RideField(number: Int, val name: String, val units: String, val read: (RideNutritionState) -> Double) {
    val definition = DeveloperField(fieldDefinitionNumber = number.toShort(), fitBaseTypeId = 136, fieldName = name, units = units)
}

class NutriProExtension : KarooExtension("nutripro", BuildConfig.VERSION_NAME) {
    private companion object {
        const val FOREGROUND_CHANNEL_ID = "nutripro_foreground"
        const val FOREGROUND_NOTIFICATION_ID = 1
    }

    private lateinit var karooSystem: KarooSystemService
    private lateinit var preferences: Preferences
    private lateinit var tracker: CarbBalanceTracker
    private lateinit var burnEngine: CarbBurnEngine
    private lateinit var alertManager: EatAlertManager

    private val lifecycleScope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private var isRecordingRide = false

    override val types by lazy {
        listOf(
            CarbBalanceDataType(tracker, extension),
            CarbsBurnedDataType(tracker, extension),
            CarbsEatenDataType(tracker, extension),
            BurnRateDataType(tracker, extension),
            QuickLogStatusDataType(tracker, extension),
            HydrationDataType(tracker, extension),
            CarbDebtDataType(tracker, extension),
            FluidDebtDataType(tracker, extension),
            QuickFoodLogDataType(preferences, extension),
            QuickWaterLogDataType(preferences, extension),
        )
    }

    private val rideFields = listOf(
        RideField(0, "carbs_burned", "g") { it.totalBurned },
        RideField(1, "carbs_eaten", "g") { it.totalEaten },
        RideField(2, "carb_balance", "g") { it.balance },
        RideField(3, "fluid_drunk", "ml") { it.totalWaterMl },
        RideField(4, "carb_target", "g/h") { it.carbTargetGph },
        RideField(5, "sweat_loss", "ml") { it.sweatLossMl },
        RideField(6, "temperature", "C") { it.currentTempC },
    )

    override fun onCreate() {
        super.onCreate()
        promoteToForegroundService()

        karooSystem = KarooSystemService(this)
        preferences = Preferences(applicationContext)
        tracker = CarbBalanceTracker(preferences)
        burnEngine = CarbBurnEngine(karooSystem, tracker, preferences)
        alertManager = EatAlertManager(karooSystem, tracker, preferences)
        NutriProExtensionHolder.tracker = tracker
        NutriProExtensionHolder.preferences = preferences

        connectionJob = lifecycleScope.launch {
            karooSystem.connect { connected ->
                if (!connected) return@connect
                Timber.d("NutriPro connected to Karoo System")
                tracker.init()
                burnEngine.start(lifecycleScope)
                alertManager.start(lifecycleScope)
                watchRideLifecycle()
            }
        }
    }

    private fun watchRideLifecycle() {
        lifecycleScope.launch {
            karooSystem.consumerFlow<RideState>().collect { rideState ->
                when (rideState) {
                    is RideState.Recording -> markRideRecording()
                    is RideState.Idle -> markRideFinished()
                    is RideState.Paused -> Unit // still counts as the same ride
                }
            }
        }
    }

    private fun markRideRecording() {
        if (isRecordingRide) return
        isRecordingRide = true
        tracker.onRideStart()
    }

    private fun markRideFinished() {
        if (!isRecordingRide) return
        isRecordingRide = false
        tracker.onRideEnd()
        announceRideSummary(tracker.stateFlow.value)
        tracker.onNewRide()
    }

    private fun announceRideSummary(state: RideNutritionState) {
        preferences.lastRideSweatLossMl = state.sweatLossMl.toFloat()
        preferences.lastRideFluidDrunkMl = state.totalWaterMl.toFloat()
        Timber.d(
            "Ride summary: eaten=%.0fg burned=%.0fg water=%.0fml sweatLoss=%.0fml",
            state.totalEaten,
            state.totalBurned,
            state.totalWaterMl,
            state.sweatLossMl,
        )
        val sweatLossLiters = "%.1f".format(state.sweatLossMl / 1000.0)
        karooSystem.dispatch(
            SystemNotification(
                "nutripro-ride-summary",
                "Ride summary: ${state.totalEaten.toInt()}g carbs, ${state.totalWaterMl.toInt()}ml water " +
                    "(est. sweat loss ${sweatLossLiters}L)",
            ),
        )
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        if (!preferences.fitExportEnabled) {
            emitter.setCancellable { }
            return
        }

        val job = lifecycleScope.launch {
            karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                .combine(karooSystem.consumerFlow<RideState>()) { _, rideState -> rideState }
                .combine(tracker.stateFlow) { rideState, state -> rideState to state }
                .collect { (rideState, state) -> writeFitFields(emitter, rideState, state) }
        }
        emitter.setCancellable { job.cancel() }
    }

    private fun writeFitFields(emitter: Emitter<FitEffect>, rideState: RideState, state: RideNutritionState) {
        val values = rideFields.map { FieldValue(it.definition, it.read(state)) }
        when (rideState) {
            is RideState.Recording -> values.forEach { emitter.onNext(WriteToRecordMesg(it)) }
            is RideState.Paused -> values.forEach { emitter.onNext(WriteToSessionMesg(it)) }
            is RideState.Idle -> Unit
        }
    }

    override fun onBonusAction(actionId: String) {
        when (actionId) {
            "log-food" -> launchQuickLog(mode = null)
            "log-water" -> launchQuickLog(mode = "water")
            "quick-gel" -> logDefaultTemplate()
            "undo-last" -> tracker.undoLast()
            else -> Timber.w("Unknown action: %s", actionId)
        }
    }

    private fun launchQuickLog(mode: String?) {
        val intent = Intent(this, QuickLogActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (mode != null) putExtra("mode", mode)
        }
        startActivity(intent)
    }

    private fun logDefaultTemplate() {
        val template = preferences.getDefaultTemplate()
        tracker.logIntake(
            IntakeEntry(
                timestampMs = System.currentTimeMillis(),
                carbsGrams = template.carbsGrams.toDouble(),
                type = IntakeEntry.IntakeType.FOOD,
                templateName = template.name,
                templateEmoji = template.emoji.ifEmpty { null },
            ),
        )
    }

    private fun promoteToForegroundService() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = getString(R.string.notification_channel_description) },
            )
        }

        val notification = Notification.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_nutripro)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        tracker.forceSave()
        burnEngine.stop()
        alertManager.stop()
        connectionJob?.cancel()
        connectionJob = null
        karooSystem.disconnect()
        NutriProExtensionHolder.tracker = null
        NutriProExtensionHolder.preferences = null
        super.onDestroy()
    }
}
