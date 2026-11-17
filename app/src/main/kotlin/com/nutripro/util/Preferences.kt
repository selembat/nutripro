package com.nutripro.util

import android.content.Context
import android.content.SharedPreferences
import com.nutripro.model.AmountPreset
import com.nutripro.model.FoodTemplate
import com.nutripro.model.RideNutritionState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Typed wrapper over the extension's [SharedPreferences]. Each setting is a
 * small delegated property instead of a hand-written get/set pair per field,
 * so adding a setting is one line rather than a getter/setter/key triple.
 */
class Preferences(val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nutripro_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private fun intPref(key: String, default: Int) = PrefDelegate(prefs, key, default, SharedPreferences::getInt, SharedPreferences.Editor::putInt)
    private fun longPref(key: String, default: Long) = PrefDelegate(prefs, key, default, SharedPreferences::getLong, SharedPreferences.Editor::putLong)
    private fun floatPref(key: String, default: Float) = PrefDelegate(prefs, key, default, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat)
    private fun boolPref(key: String, default: Boolean) = PrefDelegate(prefs, key, default, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean)
    private fun stringPref(key: String, default: String) = PrefDelegate(
        prefs, key, default,
        read = { k, def -> getString(k, def) ?: def },
        write = SharedPreferences.Editor::putString,
    )

    /** FTP override; 0 means "use the Karoo profile value". */
    var ftpOverride: Int by intPref("ftp_override", 0)

    /** Weight override; 0 means "use the Karoo profile value". */
    var weightOverride: Float by floatPref("weight_override", 0f)

    var soundEnabled: Boolean by boolPref("sound_enabled", true)
    var fitExportEnabled: Boolean by boolPref("fit_export", false)

    /** App-wide appearance: "SYSTEM" (follows the device's Android dark-mode setting), "LIGHT" or "DARK". */
    var themeMode: String by stringPref("theme_mode", "SYSTEM")

    /** Per-nutrient in-ride alert toggles - [soundEnabled] still gates whether any of them beep. */
    var carbAlertsEnabled: Boolean by boolPref("carb_alerts_enabled", true)
    var fluidAlertsEnabled: Boolean by boolPref("fluid_alerts_enabled", true)
    var sodiumAlertsEnabled: Boolean by boolPref("sodium_alerts_enabled", true)

    /** Minutes of no intake before the first eat/drink reminder can fire. */
    var reminderStartMinutes: Int by intPref("reminder_start", 30)

    /** Carb serving size that triggers an eat alert (g). */
    var servingGrams: Float by floatPref("serving_grams", 25f)

    /** Fluid sip-block that triggers a drink alert (ml). */
    var sipBlockMl: Float by floatPref("sip_block_ml", 150f)

    /** Amount logged by the one-tap "Quick Sip" button (ml) — separate from the alert threshold. */
    var sipAmountMl: Float by floatPref("sip_amount_ml", 100f)

    var bottleCount: Int by intPref("bottle_count", 2)
    var bottleVolumeMl: Float by floatPref("bottle_volume_ml", 750f)

    /** Manual ambient temperature override (°C) — fallback when weather/BLE aren't available. */
    var manualTempC: Float by floatPref("manual_temp_c", 20f)

    /** Fetch ambient temperature from the weather API (spec 3.3) when GPS location is available. */
    var weatherEnabled: Boolean by boolPref("weather_enabled", true)

    /** Personal sweat-model calibration multiplier (spec 3.7); 1.0 = uncalibrated. */
    var sweatMultiplier: Float by floatPref("sweat_multiplier", 1f)

    /** Sweat sodium concentration (mg/L), used to estimate sodium loss from sweat rate. */
    var sweatSodiumMgPerL: Float by floatPref("sweat_sodium_mg_per_l", 800f)

    /** Amount logged by the one-tap sodium field, and the debt level it's considered "due" at (mg). */
    var sodiumDoseMg: Float by floatPref("sodium_dose_mg", 500f)

    /** Pre/post-ride weigh-in for sweat-rate calibration (spec 3.7), kg; 0 = not entered. */
    var preRideWeightKg: Float by floatPref("pre_ride_weight_kg", 0f)
    var postRideWeightKg: Float by floatPref("post_ride_weight_kg", 0f)

    /** Snapshot of the last completed ride's model output, used to calibrate against the weigh-in. */
    var lastRideSweatLossMl: Float by floatPref("last_ride_sweat_loss_ml", 0f)
    var lastRideFluidDrunkMl: Float by floatPref("last_ride_fluid_drunk_ml", 0f)

    /** Max time an alert can be held while conditions are unsafe (seconds). */
    var alertHoldMaxSec: Long by longPref("alert_hold_max_sec", 5 * 60L)

    /** Re-remind interval if nothing is logged after an alert (seconds). */
    var reRemindAfterSec: Long by longPref("re_remind_after_sec", 10 * 60L)

    /**
     * How long the rider plans to ride today (minutes); 0 means "unknown" - the carb-debt
     * duration gate then falls back to elapsed time, or a loaded route's live ETA if one
     * is available (spec: don't prescribe high carb targets on short rides/efforts).
     */
    var plannedRideMinutes: Int by intPref("planned_ride_minutes", 0)

    /**
     * Ride mode (spec 3.1 extension): one of "CASUAL_Z2", "CASUAL_SPORT", "WORKOUT", "RACE".
     * Sticky across rides - seeds the next ride's default, same as [sweatMultiplier].
     */
    var rideMode: String by stringPref("ride_mode", "CASUAL_Z2")

    /** Testing: feed a fixed simulated power/HR into the engine instead of real sensor data. */
    var simulationModeEnabled: Boolean by boolPref("simulation_mode_enabled", false)
    var simulatedPowerWatts: Float by floatPref("simulated_power_watts", 200f)
    var simulatedHeartRateBpm: Int by intPref("simulated_heart_rate_bpm", 140)

    fun loadFoodTemplates(): List<FoodTemplate> {
        val defaults = FoodTemplate.defaults(context)
        val raw = prefs.getString(KEY_FOOD_TEMPLATES, null) ?: return defaults
        return runCatching { json.decodeFromString<List<FoodTemplate>>(raw) }
            .onFailure { Timber.w(it, "Failed to load food templates") }
            .getOrDefault(defaults)
    }

    fun saveFoodTemplates(templates: List<FoodTemplate>) {
        prefs.edit().putString(KEY_FOOD_TEMPLATES, json.encodeToString(templates)).apply()
    }

    fun getDefaultTemplate(): FoodTemplate =
        loadFoodTemplates().firstOrNull { it.isDefault } ?: FoodTemplate.defaults(context).first()

    fun loadFluidPresets(): List<AmountPreset> = loadPresets(KEY_FLUID_PRESETS, AmountPreset.FLUID_DEFAULTS)

    fun saveFluidPresets(presets: List<AmountPreset>) {
        savePresets(KEY_FLUID_PRESETS, presets)
        presets.firstOrNull { it.isDefault }?.let { sipAmountMl = it.amount.toFloat() }
    }

    fun loadSodiumPresets(): List<AmountPreset> = loadPresets(KEY_SODIUM_PRESETS, AmountPreset.SODIUM_DEFAULTS)

    fun saveSodiumPresets(presets: List<AmountPreset>) {
        savePresets(KEY_SODIUM_PRESETS, presets)
        presets.firstOrNull { it.isDefault }?.let { sodiumDoseMg = it.amount.toFloat() }
    }

    private fun loadPresets(key: String, defaults: List<AmountPreset>): List<AmountPreset> {
        val raw = prefs.getString(key, null) ?: return defaults
        return runCatching { json.decodeFromString<List<AmountPreset>>(raw) }
            .onFailure { Timber.w(it, "Failed to load presets for %s", key) }
            .getOrDefault(defaults)
    }

    private fun savePresets(key: String, presets: List<AmountPreset>) {
        prefs.edit().putString(key, json.encodeToString(presets)).apply()
    }

    fun saveNutritionState(state: RideNutritionState) {
        prefs.edit().putString(KEY_NUTRITION_STATE, json.encodeToString(state)).apply()
    }

    fun loadNutritionState(): RideNutritionState? {
        val raw = prefs.getString(KEY_NUTRITION_STATE, null) ?: return null
        return runCatching { json.decodeFromString<RideNutritionState>(raw) }
            .onFailure { Timber.w(it, "Failed to load nutrition state") }
            .getOrNull()
    }

    fun clearNutritionState() {
        prefs.edit().remove(KEY_NUTRITION_STATE).apply()
    }

    private companion object {
        const val KEY_FOOD_TEMPLATES = "food_templates"
        const val KEY_NUTRITION_STATE = "nutrition_state"
        const val KEY_FLUID_PRESETS = "fluid_presets"
        const val KEY_SODIUM_PRESETS = "sodium_presets"
    }
}

/** A single SharedPreferences entry exposed as a mutable property, for one scalar type [T]. */
private class PrefDelegate<T>(
    private val prefs: SharedPreferences,
    private val key: String,
    private val default: T,
    private val read: SharedPreferences.(String, T) -> T,
    private val write: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = prefs.read(key, default)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        prefs.edit().write(key, value).apply()
    }
}
