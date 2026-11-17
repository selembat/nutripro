package com.nutripro.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutripro.R
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.karoo.EatAlertManager
import com.nutripro.model.AmountPreset
import com.nutripro.model.FoodTemplate
import com.nutripro.model.IntakeEntry
import com.nutripro.model.RideMode
import com.nutripro.ui.theme.Accent
import com.nutripro.ui.theme.Background
import com.nutripro.ui.theme.Error
import com.nutripro.ui.theme.Food
import com.nutripro.ui.theme.NutriProTheme
import com.nutripro.ui.theme.OnAccent
import com.nutripro.ui.theme.OnSurface
import com.nutripro.ui.theme.OnSurfaceVariant
import com.nutripro.ui.theme.Surface
import com.nutripro.ui.theme.SurfaceElevated
import com.nutripro.ui.theme.SurfaceVariant
import com.nutripro.ui.theme.Water
import com.nutripro.ui.theme.rememberIsDarkTheme
import com.nutripro.util.Preferences

/** Header bar uses the elevated surface tone so it reads correctly in both light and dark theme. */
private val HeaderBg: Color @Composable get() = SurfaceElevated

/** Full-screen activity launched from a bonus action or a tap on a quick-log field. */
class QuickLogActivity : ComponentActivity() {
    private lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = NutriProExtensionHolder.preferences ?: Preferences(applicationContext)

        if (intent?.action == "com.nutripro.SET_RIDE_MODE_WORKOUT") {
            NutriProExtensionHolder.alertManager?.confirmRideMode(RideMode.WORKOUT)
            Toast.makeText(this, getString(R.string.toast_switched_workout), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mode = intent?.getStringExtra("mode") ?: when (intent?.action) {
            "com.nutripro.QUICK_LOG_WATER" -> "confirm-water"
            "com.nutripro.QUICK_LOG_SODIUM" -> "confirm-sodium"
            else -> null
        }
        val lastEntry = NutriProExtensionHolder.tracker?.stateFlow?.value?.intakeLog?.lastOrNull()
        val allTemplates = preferences.loadFoodTemplates()
        val drinkMixes = allTemplates.filter { it.isDrinkMix }

        setContent {
            NutriProTheme(darkTheme = rememberIsDarkTheme(preferences.themeMode)) {
                when (mode) {
                    "water" -> WaterLogScreen(
                        presets = preferences.loadFluidPresets(),
                        drinkMixes = drinkMixes,
                        onLog = ::logWater,
                        onLogMix = ::logFood,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onCancel = ::finish,
                    )
                    "sodium" -> SodiumLogScreen(
                        presets = preferences.loadSodiumPresets(),
                        drinkMixes = drinkMixes,
                        onLog = ::logSodium,
                        onLogMix = ::logFood,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onCancel = ::finish,
                    )
                    "ride-setup" -> RideSetupScreen(
                        currentMode = preferences.rideMode,
                        onDone = ::setRideModeAndPlan,
                        onCancel = ::finish,
                    )
                    "refill" -> RefillBottlesScreen(
                        capacityMl = (preferences.bottleCount * preferences.bottleVolumeMl).toInt(),
                        onRefill = ::refillBottles,
                        onCancel = ::finish,
                    )
                    "confirm-carb" -> ConfirmDoseScreen(
                        title = getString(R.string.dialog_quick_carb_title),
                        barColor = Food,
                        confirmLabel = getString(
                            R.string.confirm_ate_food,
                            preferences.getDefaultTemplate().name,
                            preferences.getDefaultTemplate().carbsGrams,
                        ),
                        confirmAmount = preferences.getDefaultTemplate().carbsGrams,
                        unit = "g",
                        step = 5,
                        range = 5..200,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onConfirm = ::logQuickCarb,
                        onCancel = ::finish,
                    )
                    "confirm-water" -> ConfirmDoseScreen(
                        title = getString(R.string.dialog_quick_sip_title),
                        barColor = Water,
                        confirmLabel = getString(R.string.confirm_drank_sip, preferences.sipAmountMl.toInt()),
                        confirmAmount = preferences.sipAmountMl.toInt(),
                        unit = "ml",
                        step = 25,
                        range = 25..1000,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onConfirm = ::logWater,
                        onCancel = ::finish,
                    )
                    "confirm-sodium" -> ConfirmDoseScreen(
                        title = getString(R.string.dialog_quick_sodium_title),
                        barColor = Accent,
                        confirmLabel = getString(R.string.confirm_took_sodium, preferences.sodiumDoseMg.toInt()),
                        confirmAmount = preferences.sodiumDoseMg.toInt(),
                        unit = "mg",
                        step = 100,
                        range = 100..2000,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onConfirm = ::logSodium,
                        onCancel = ::finish,
                    )
                    else -> FoodLogScreen(
                        templates = allTemplates,
                        onLog = ::logFood,
                        onLogCustom = ::logCustom,
                        lastEntry = lastEntry,
                        onUndo = ::undoLast,
                        onCancel = ::finish,
                    )
                }
            }
        }
    }

    private fun withRunningTracker(block: (CarbBalanceTracker) -> Unit) {
        val tracker = NutriProExtensionHolder.tracker
        if (tracker == null) {
            Toast.makeText(this, getString(R.string.toast_not_running), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        block(tracker)
    }

    private fun logFood(template: FoodTemplate) = withRunningTracker { tracker ->
        if (template.isDrinkMix) {
            val (carbsGrams, waterMl, sodiumMg) = template.sipAmounts()
            tracker.logCompound(template.name, carbsGrams, waterMl, sodiumMg)
            Toast.makeText(
                this,
                getString(R.string.toast_logged_full, template.name, carbsGrams.toInt(), waterMl.toInt(), sodiumMg.toInt()),
                Toast.LENGTH_SHORT,
            ).show()
            finish()
            return@withRunningTracker
        }

        tracker.logIntake(
            IntakeEntry(
                timestampMs = System.currentTimeMillis(),
                carbsGrams = template.carbsGrams.toDouble(),
                type = IntakeEntry.IntakeType.FOOD,
                templateName = template.name,
                templateEmoji = template.emoji.ifEmpty { null },
            ),
        )
        Toast.makeText(this, getString(R.string.toast_logged_food, template.name, template.carbsGrams), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logQuickCarb(grams: Int) {
        val defaultTemplate = preferences.getDefaultTemplate()
        if (grams == defaultTemplate.carbsGrams) logFood(defaultTemplate) else logCustom(grams)
    }

    private fun logCustom(grams: Int) = withRunningTracker { tracker ->
        tracker.logIntake(
            IntakeEntry(
                timestampMs = System.currentTimeMillis(),
                carbsGrams = grams.toDouble(),
                type = IntakeEntry.IntakeType.FOOD,
                templateName = getString(R.string.template_custom_grams, grams),
            ),
        )
        Toast.makeText(this, getString(R.string.toast_logged_carbs, grams), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logWater(ml: Int) = withRunningTracker { tracker ->
        tracker.logWater(ml.toDouble())
        Toast.makeText(this, getString(R.string.toast_logged_water, ml), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logSodium(mg: Int) = withRunningTracker { tracker ->
        tracker.logSodium(mg.toDouble())
        Toast.makeText(this, getString(R.string.toast_logged_sodium, mg), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun undoLast() = withRunningTracker { tracker ->
        val ok = tracker.undoLast()
        Toast.makeText(this, if (ok) getString(R.string.toast_undone) else getString(R.string.toast_nothing_to_undo), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun refillBottles(ml: Int) = withRunningTracker { tracker ->
        tracker.refillBottles(ml.toDouble())
        Toast.makeText(this, getString(R.string.toast_refilled, ml), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setRideModeAndPlan(mode: String, minutes: Int) {
        NutriProExtensionHolder.alertManager?.confirmRideMode(mode)
        preferences.plannedRideMinutes = minutes
        Toast.makeText(this, getString(R.string.toast_ride_mode_set, rideModeLabel(this, mode)), Toast.LENGTH_SHORT).show()
        finish()
    }
}

object NutriProExtensionHolder {
    var tracker: CarbBalanceTracker? = null
    var preferences: Preferences? = null
    var alertManager: EatAlertManager? = null
}

/** True below this height, where the quick-log sheet trims its own padding to still fit. */
@Composable
private fun isCompactScreen(): Boolean = LocalConfiguration.current.screenHeightDp.dp < 420.dp

@Composable
private fun ScreenHeader(title: String, barColor: Color, contentColor: Color, onBack: () -> Unit, compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor)
            .padding(horizontal = 4.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = contentColor, letterSpacing = 1.sp)
    }
}

/** A tappable rounded rect used for both the template/water-amount rows and the bottom action buttons. */
@Composable
private fun Pill(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun ActionButton(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Pill(modifier = modifier.height(height), backgroundColor = backgroundColor, onClick = onClick) {
        Text(text = label, fontSize = if (height < 50.dp) 13.sp else 14.sp, fontWeight = FontWeight.Black, color = textColor)
    }
}

private fun lastEntrySummary(context: android.content.Context, entry: IntakeEntry): String {
    val minutesAgo = ((System.currentTimeMillis() - entry.timestampMs) / 60_000L).toInt()
    val timeText = if (minutesAgo < 1) {
        context.getString(R.string.time_just_now)
    } else {
        context.getString(R.string.time_minutes_ago, minutesAgo)
    }
    val name = entry.templateName ?: context.getString(R.string.label_generic_entry)
    return context.getString(R.string.label_logged_summary, name, timeText)
}

/** No time limit - the most recent intake log entry can always be undone, atomically even for a compound entry. */
@Composable
private fun UndoLastRow(lastEntry: IntakeEntry?, onUndo: () -> Unit, compact: Boolean) {
    if (lastEntry == null) return
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable(onClick = onUndo)
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "↺", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_undo_last_button), fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold, color = Error)
            }
            Text(
                text = lastEntrySummary(context, lastEntry),
                fontSize = if (compact) 10.sp else 11.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}

@Composable
fun FoodLogScreen(
    templates: List<FoodTemplate>,
    onLog: (FoodTemplate) -> Unit,
    onLogCustom: (Int) -> Unit,
    lastEntry: IntakeEntry? = null,
    onUndo: () -> Unit = {},
    onCancel: () -> Unit,
) {
    val compact = isCompactScreen()
    val buttonHeight = if (compact) 48.dp else 56.dp
    var showCustom by remember { mutableStateOf(false) }
    var customGrams by remember { mutableIntStateOf(25) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (showCustom) stringResource(R.string.dialog_custom_carbs_title) else stringResource(R.string.dialog_log_food_title),
            barColor = HeaderBg,
            contentColor = OnSurface,
            onBack = if (showCustom) ({ showCustom = false }) else onCancel,
            compact = compact,
        )

        if (showCustom) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = stringResource(R.string.label_carbs), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customGrams = (customGrams - 5).coerceAtLeast(5) },
                    ) {
                        Text(text = "−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }

                    Column(modifier = Modifier.width(if (compact) 100.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$customGrams",
                            fontSize = if (compact) 42.sp else 56.sp,
                            fontWeight = FontWeight.Black,
                            color = Food,
                            textAlign = TextAlign.Center,
                        )
                        Text(text = stringResource(R.string.unit_grams_label), fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, color = Food.copy(alpha = 0.6f))
                    }

                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customGrams = (customGrams + 5).coerceAtMost(200) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_plus5), tint = OnSurface, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(stringResource(R.string.action_back), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f)) { showCustom = false }
                    ActionButton(stringResource(R.string.action_log_grams, customGrams), Food, Color.Black, buttonHeight, Modifier.weight(1f)) { onLogCustom(customGrams) }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UndoLastRow(lastEntry = lastEntry, onUndo = onUndo, compact = compact)
                templates.forEach { template ->
                    Pill(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Surface,
                        onClick = { onLog(template) },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 14.dp else 18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        fontSize = if (compact) 14.sp else 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface,
                                    )
                                    if (template.isDrinkMix) {
                                        Text(
                                            text = stringResource(R.string.hint_adds_all),
                                            fontSize = if (compact) 10.sp else 11.sp,
                                            color = OnSurfaceVariant,
                                        )
                                    }
                                }
                                if (template.isDrinkMix) {
                                    val (carbsGrams, waterMl, sodiumMg) = template.sipAmounts()
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row {
                                            Text(text = "~${carbsGrams.toInt()}", fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Black, color = Food)
                                            Text(text = "g", fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold, color = Food.copy(alpha = 0.6f))
                                        }
                                        Text(
                                            text = stringResource(R.string.stat_fluid_sodium, waterMl.toInt(), sodiumMg.toInt()),
                                            fontSize = if (compact) 10.sp else 11.sp,
                                            color = OnSurfaceVariant,
                                        )
                                    }
                                } else {
                                    Text(text = "${template.carbsGrams}", fontSize = if (compact) 22.sp else 26.sp, fontWeight = FontWeight.Black, color = Food)
                                    Text(text = "g", fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold, color = Food.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_custom), Accent, OnAccent, buttonHeight, Modifier.weight(1f)) { showCustom = true }
                ActionButton(stringResource(R.string.action_cancel_caps), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f), onCancel)
            }
        }
    }
}

@Composable
fun WaterLogScreen(
    presets: List<AmountPreset>,
    drinkMixes: List<FoodTemplate>,
    onLog: (Int) -> Unit,
    onLogMix: (FoodTemplate) -> Unit,
    lastEntry: IntakeEntry? = null,
    onUndo: () -> Unit = {},
    onCancel: () -> Unit,
) = AmountSelectScreen(
    title = stringResource(R.string.dialog_log_water_title),
    unit = "ml",
    color = Water,
    presets = presets,
    drinkMixes = drinkMixes,
    step = 25,
    range = 25..2000,
    onLog = onLog,
    onLogMix = onLogMix,
    lastEntry = lastEntry,
    onUndo = onUndo,
    onCancel = onCancel,
)

@Composable
fun SodiumLogScreen(
    presets: List<AmountPreset>,
    drinkMixes: List<FoodTemplate>,
    onLog: (Int) -> Unit,
    onLogMix: (FoodTemplate) -> Unit,
    lastEntry: IntakeEntry? = null,
    onUndo: () -> Unit = {},
    onCancel: () -> Unit,
) = AmountSelectScreen(
    title = stringResource(R.string.dialog_log_sodium_title),
    unit = "mg",
    color = Accent,
    presets = presets,
    drinkMixes = drinkMixes,
    step = 50,
    range = 50..3000,
    onLog = onLog,
    onLogMix = onLogMix,
    lastEntry = lastEntry,
    onUndo = onUndo,
    onCancel = onCancel,
)

/**
 * Shared implementation for [WaterLogScreen] and [SodiumLogScreen]: a preset/drink-mix list
 * plus a Custom stepper fallback, matching [FoodLogScreen]'s shape so every Add screen offers
 * the same three ways in - preset, drink mix, or a fully custom amount.
 */
@Composable
private fun AmountSelectScreen(
    title: String,
    unit: String,
    color: Color,
    presets: List<AmountPreset>,
    drinkMixes: List<FoodTemplate>,
    step: Int,
    range: IntRange,
    onLog: (Int) -> Unit,
    onLogMix: (FoodTemplate) -> Unit,
    lastEntry: IntakeEntry?,
    onUndo: () -> Unit,
    onCancel: () -> Unit,
) {
    val compact = isCompactScreen()
    val buttonHeight = if (compact) 48.dp else 56.dp
    var showCustom by remember { mutableStateOf(false) }
    var customAmount by remember {
        mutableIntStateOf(presets.firstOrNull { it.isDefault }?.amount ?: presets.firstOrNull()?.amount ?: range.first)
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (showCustom) stringResource(R.string.dialog_custom_amount_title) else title,
            barColor = HeaderBg,
            contentColor = OnSurface,
            onBack = if (showCustom) ({ showCustom = false }) else onCancel,
            compact = compact,
        )

        if (showCustom) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customAmount = (customAmount - step).coerceAtLeast(range.first) },
                    ) {
                        Text(text = "−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }

                    Column(modifier = Modifier.width(if (compact) 100.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$customAmount", fontSize = if (compact) 42.sp else 56.sp, fontWeight = FontWeight.Black, color = color, textAlign = TextAlign.Center)
                        Text(text = unit, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.6f))
                    }

                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customAmount = (customAmount + step).coerceAtMost(range.last) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_plus_step, step.toString()), tint = OnSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_back), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f)) { showCustom = false }
                ActionButton(stringResource(R.string.action_log_amount, customAmount.toString(), unit), color, Color.Black, buttonHeight, Modifier.weight(1f)) { onLog(customAmount) }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UndoLastRow(lastEntry = lastEntry, onUndo = onUndo, compact = compact)
                presets.forEach { preset ->
                    Pill(
                        modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 14.dp else 18.dp),
                        backgroundColor = Surface,
                        onClick = { onLog(preset.amount) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${preset.amount}", fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Black, color = color)
                            Text(text = " $unit", fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.6f))
                        }
                    }
                }
                drinkMixes.forEach { mix -> DrinkMixRow(mix = mix, onClick = { onLogMix(mix) }, compact = compact) }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_custom), Accent, OnAccent, buttonHeight, Modifier.weight(1f)) { showCustom = true }
                ActionButton(stringResource(R.string.action_cancel_caps), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f), onCancel)
            }
        }
    }
}

/** A compound drink-mix row shared by Add Carb/Fluid/Sodium - same item, same per-tap amounts, wherever it's reached from. */
@Composable
private fun DrinkMixRow(mix: FoodTemplate, onClick: () -> Unit, compact: Boolean) {
    val (carbsGrams, waterMl, sodiumMg) = mix.sipAmounts()
    Pill(modifier = Modifier.fillMaxWidth(), backgroundColor = Surface, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 14.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mix.name, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(text = stringResource(R.string.hint_adds_all), fontSize = if (compact) 10.sp else 11.sp, color = OnSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${waterMl.toInt()}ml", fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black, color = Water)
                Text(text = "${carbsGrams.toInt()}g · ${sodiumMg.toInt()}mg", fontSize = if (compact) 10.sp else 11.sp, color = OnSurfaceVariant)
            }
        }
    }
}

/**
 * A quick, single-dose confirm step for Quick Sip / Quick Sodium: "did you just take the
 * configured amount?" with one tap to confirm, or a fallback to a custom amount stepper -
 * same two-screen shape as [FoodLogScreen]'s custom-carbs flow.
 */
@Composable
fun ConfirmDoseScreen(
    title: String,
    barColor: Color,
    confirmLabel: String,
    confirmAmount: Int,
    unit: String,
    step: Int,
    range: IntRange,
    lastEntry: IntakeEntry? = null,
    onUndo: () -> Unit = {},
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val compact = isCompactScreen()
    val buttonHeight = if (compact) 48.dp else 56.dp
    var showCustom by remember { mutableStateOf(false) }
    var customAmount by remember { mutableIntStateOf(confirmAmount) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (showCustom) stringResource(R.string.dialog_custom_amount_title) else title,
            barColor = HeaderBg,
            contentColor = OnSurface,
            onBack = if (showCustom) ({ showCustom = false }) else onCancel,
            compact = compact,
        )

        if (showCustom) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customAmount = (customAmount - step).coerceAtLeast(range.first) },
                    ) {
                        Text(text = "−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }

                    Column(modifier = Modifier.width(if (compact) 100.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$customAmount", fontSize = if (compact) 42.sp else 56.sp, fontWeight = FontWeight.Black, color = barColor, textAlign = TextAlign.Center)
                        Text(text = unit, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, color = barColor.copy(alpha = 0.6f))
                    }

                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customAmount = (customAmount + step).coerceAtMost(range.last) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_plus_step, step.toString()), tint = OnSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_back), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f)) { showCustom = false }
                ActionButton(stringResource(R.string.action_log_amount, customAmount.toString(), unit), barColor, Color.Black, buttonHeight, Modifier.weight(1f)) { onConfirm(customAmount) }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp)) {
                UndoLastRow(lastEntry = lastEntry, onUndo = onUndo, compact = compact)
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = confirmLabel, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, textAlign = TextAlign.Center)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_confirm), barColor, Color.Black, buttonHeight, Modifier.weight(1f)) { onConfirm(confirmAmount) }
                ActionButton(stringResource(R.string.action_custom), Accent, OnAccent, buttonHeight, Modifier.weight(1f)) { showCustom = true }
            }
            ActionButton(
                label = stringResource(R.string.action_cancel_caps),
                backgroundColor = Surface,
                textColor = OnSurfaceVariant,
                height = buttonHeight,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                onClick = onCancel,
            )
        }
    }
}

/** Tap-to-refill screen reached from the Bottles field: full refill in one tap, or a custom partial amount. */
@Composable
fun RefillBottlesScreen(capacityMl: Int, onRefill: (Int) -> Unit, onCancel: () -> Unit) {
    val compact = isCompactScreen()
    val buttonHeight = if (compact) 48.dp else 56.dp
    var showCustom by remember { mutableStateOf(false) }
    var customMl by remember { mutableIntStateOf(capacityMl) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (showCustom) stringResource(R.string.dialog_custom_refill_title) else stringResource(R.string.dialog_refill_bottles_title),
            barColor = HeaderBg,
            contentColor = OnSurface,
            onBack = if (showCustom) ({ showCustom = false }) else onCancel,
            compact = compact,
        )

        if (showCustom) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customMl = (customMl - 50).coerceAtLeast(50) },
                    ) {
                        Text(text = "−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }

                    Column(modifier = Modifier.width(if (compact) 100.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$customMl", fontSize = if (compact) 42.sp else 56.sp, fontWeight = FontWeight.Black, color = Water, textAlign = TextAlign.Center)
                        Text(text = "ml", fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, color = Water.copy(alpha = 0.6f))
                    }

                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customMl = (customMl + 50).coerceAtMost(capacityMl) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_plus50), tint = OnSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_back), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f)) { showCustom = false }
                ActionButton(stringResource(R.string.action_refill_amount, customMl), Water, Color.Black, buttonHeight, Modifier.weight(1f)) { onRefill(customMl) }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.confirm_refill_both),
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.action_refill_full, capacityMl), Water, Color.Black, buttonHeight, Modifier.fillMaxWidth()) { onRefill(capacityMl) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(stringResource(R.string.action_custom), Accent, OnAccent, buttonHeight, Modifier.weight(1f)) { showCustom = true }
                    ActionButton(stringResource(R.string.action_cancel_caps), Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f), onCancel)
                }
            }
        }
    }
}

/** Display label for a [RideMode] value, shared by the setup screen and its confirm toast. */
fun rideModeLabel(context: android.content.Context, mode: String): String = when (mode) {
    RideMode.CASUAL_Z2 -> context.getString(R.string.ridemode_casual_z2)
    RideMode.CASUAL_SPORT -> context.getString(R.string.ridemode_casual_sport)
    RideMode.WORKOUT -> context.getString(R.string.ridemode_workout)
    RideMode.RACE -> context.getString(R.string.ridemode_race)
    else -> mode
}

/**
 * Combined ride mode + duration setup (spec 3.1 extension): picking a mode advances straight
 * to the duration step (same one-tap-commits feel as every other screen here, just chained
 * once) rather than requiring a separate "save" step. "Using loaded route" clears the manual
 * plan so [com.nutripro.engine.DurationGate]'s route-ETA fallback applies instead.
 */
@Composable
fun RideSetupScreen(currentMode: String, onDone: (mode: String, minutes: Int) -> Unit, onCancel: () -> Unit) {
    val compact = isCompactScreen()
    var step by remember { mutableIntStateOf(1) }
    var selectedMode by remember { mutableStateOf(currentMode) }

    val modes = listOf(
        Triple(RideMode.CASUAL_Z2, stringResource(R.string.ridemode_casual_z2_title), stringResource(R.string.ridemode_casual_z2_subtitle)),
        Triple(RideMode.CASUAL_SPORT, stringResource(R.string.ridemode_casual_sport_title), stringResource(R.string.ridemode_casual_sport_subtitle)),
        Triple(RideMode.WORKOUT, stringResource(R.string.ridemode_workout), stringResource(R.string.ridemode_workout_subtitle)),
        Triple(RideMode.RACE, stringResource(R.string.ridemode_race), stringResource(R.string.ridemode_race_subtitle)),
    )
    val durations = listOf(
        stringResource(R.string.duration_using_route) to 0,
        stringResource(R.string.duration_lt_1h) to 45,
        stringResource(R.string.duration_1_2h) to 90,
        stringResource(R.string.duration_2_3h) to 150,
        stringResource(R.string.duration_3_4h) to 210,
        stringResource(R.string.duration_4h_plus) to 300,
    )

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (step == 1) stringResource(R.string.step_title_ride_mode) else stringResource(R.string.step_title_ride_plan),
            barColor = HeaderBg,
            contentColor = OnSurface,
            onBack = if (step == 1) onCancel else ({ step = 1 }),
            compact = compact,
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (step == 1) {
                modes.forEach { (value, label, subtitle) ->
                    val isCurrent = value == selectedMode
                    Pill(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isCurrent) SurfaceElevated else Surface,
                        onClick = { selectedMode = value; step = 2 },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 16.dp),
                        ) {
                            Text(text = label, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Black, color = Accent)
                            Text(text = subtitle, fontSize = if (compact) 10.sp else 11.sp, color = OnSurfaceVariant)
                        }
                    }
                }
            } else {
                durations.forEach { (label, minutes) ->
                    Pill(
                        modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 12.dp else 16.dp),
                        backgroundColor = Surface,
                        onClick = { onDone(selectedMode, minutes) },
                    ) {
                        Text(text = label, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black, color = Accent)
                    }
                }
            }
        }

        ActionButton(
            label = stringResource(R.string.action_cancel_caps),
            backgroundColor = Surface,
            textColor = OnSurfaceVariant,
            height = if (compact) 48.dp else 56.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            onClick = onCancel,
        )
    }
}

