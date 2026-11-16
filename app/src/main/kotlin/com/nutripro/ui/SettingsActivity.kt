package com.nutripro.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutripro.BuildConfig
import com.nutripro.engine.SweatCalibrator
import com.nutripro.model.FoodTemplate
import com.nutripro.ui.theme.Accent
import com.nutripro.ui.theme.AccentLight
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
import com.nutripro.util.Preferences

class SettingsActivity : ComponentActivity() {
    private lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = Preferences(applicationContext)

        setContent {
            NutriProTheme {
                SettingsScreen(
                    preferences = preferences,
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}

/**
 * Thin wrapper around one preference value: reads the stored value once into
 * Compose state, and every write goes through both the mutable state (so the
 * UI recomposes) and back out to [Preferences]. Each settings section below
 * builds the handful of these it needs rather than threading loose `var`s
 * and bespoke `onUpdate` lambdas through the composable tree.
 */
private class PrefField<T>(initial: T, private val persist: (T) -> Unit) {
    var value: T by mutableStateOf(initial)
        private set

    fun set(newValue: T) {
        value = newValue
        persist(newValue)
    }
}

@Composable
private fun <T> rememberPrefField(initial: T, persist: (T) -> Unit): PrefField<T> =
    remember { PrefField(initial, persist) }

@Composable
fun SettingsScreen(
    preferences: Preferences,
    onNavigateBack: () -> Unit,
) {
    val isCompact = LocalConfiguration.current.screenHeightDp.dp < 420.dp

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        SettingsHeader(onNavigateBack = onNavigateBack, isCompact = isCompact)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
        ) {
            ProfileSection(preferences, isCompact)
            SweatCalibrationSection(preferences, isCompact)
            FoodTemplatesSection(preferences, isCompact)
            FuelingSection(preferences, isCompact)
            OptionsSection(preferences, isCompact)
            TestingSection(preferences, isCompact)
            AboutSection(isCompact)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit, isCompact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Accent)
            .padding(horizontal = 4.dp, vertical = if (isCompact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = OnAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = "NUTRIPRO SETTINGS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = OnAccent,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun ProfileSection(preferences: Preferences, isCompact: Boolean) {
    val ftp = rememberPrefField(preferences.ftpOverride) { preferences.ftpOverride = it }
    val weight = rememberPrefField(preferences.weightOverride) { preferences.weightOverride = it }

    SettingsSection(title = "PROFILE") {
        FtpRow(value = ftp.value, onUpdate = ftp::set, isCompact = isCompact)
        CardDivider()
        WeightRow(value = weight.value, onUpdate = weight::set, isCompact = isCompact)
    }
}

@Composable
private fun SweatCalibrationSection(preferences: Preferences, isCompact: Boolean) {
    val preRideWeight = rememberPrefField(preferences.preRideWeightKg) { preferences.preRideWeightKg = it }
    val postRideWeight = rememberPrefField(preferences.postRideWeightKg) { preferences.postRideWeightKg = it }
    var sweatMultiplier by remember { mutableStateOf(preferences.sweatMultiplier) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }

    val readyToCalibrate = preRideWeight.value > 0f &&
        postRideWeight.value > 0f &&
        preferences.lastRideSweatLossMl > 0f

    SettingsSection(title = "SWEAT CALIBRATION") {
        WeighInRow(label = "Pre-ride weight", value = preRideWeight.value, onUpdate = preRideWeight::set, isCompact = isCompact)
        CardDivider()
        WeighInRow(label = "Post-ride weight", value = postRideWeight.value, onUpdate = postRideWeight::set, isCompact = isCompact)
        CardDivider()
        SettingsRow(label = "Current multiplier", trailingText = "%.2f×".format(sweatMultiplier), isCompact = isCompact)
        CardDivider()
        CalibrateRow(
            enabled = readyToCalibrate,
            message = calibrationMessage,
            isCompact = isCompact,
            onCalibrate = {
                val actualLossMl = SweatCalibrator.computeActualSweatLossMl(
                    preWeightKg = preRideWeight.value.toDouble(),
                    postWeightKg = postRideWeight.value.toDouble(),
                    fluidDrunkMl = preferences.lastRideFluidDrunkMl.toDouble(),
                )
                val newMultiplier = SweatCalibrator.updateMultiplier(
                    currentMultiplier = preferences.sweatMultiplier.toDouble(),
                    modeledSweatLossMl = preferences.lastRideSweatLossMl.toDouble(),
                    actualSweatLossMl = actualLossMl,
                ).toFloat()

                preferences.sweatMultiplier = newMultiplier
                sweatMultiplier = newMultiplier
                preRideWeight.set(0f)
                postRideWeight.set(0f)
                calibrationMessage = "Updated to %.2f× (measured %.0fml lost)".format(newMultiplier, actualLossMl)
            },
        )
    }
}

@Composable
private fun FoodTemplatesSection(preferences: Preferences, isCompact: Boolean) {
    val templates = remember { mutableStateListOf(*preferences.loadFoodTemplates().toTypedArray()) }
    var isAdding by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    var draftCarbs by remember { mutableStateOf("") }
    var draftEmoji by remember { mutableStateOf("") }

    fun persist() = preferences.saveFoodTemplates(templates.toList())
    fun resetDraft() {
        isAdding = false
        draftName = ""
        draftCarbs = ""
        draftEmoji = ""
    }

    SettingsSection(title = "FOOD TEMPLATES") {
        templates.forEachIndexed { index, template ->
            if (index > 0) CardDivider()
            TemplateRow(
                template = template,
                onSetDefault = {
                    templates.indices.forEach { i -> templates[i] = templates[i].copy(isDefault = i == index) }
                    persist()
                },
                onDelete = {
                    templates.removeAt(index)
                    persist()
                },
                isCompact = isCompact,
            )
        }

        if (templates.isNotEmpty()) CardDivider()

        if (isAdding) {
            AddTemplateRow(
                name = draftName,
                carbs = draftCarbs,
                emoji = draftEmoji,
                onNameChange = { draftName = it },
                onCarbsChange = { draftCarbs = it.filter(Char::isDigit) },
                onEmojiChange = { draftEmoji = it },
                onAdd = {
                    val carbsGrams = draftCarbs.toIntOrNull()
                    if (draftName.isNotBlank() && carbsGrams != null && carbsGrams > 0) {
                        templates.add(FoodTemplate(name = draftName.trim(), carbsGrams = carbsGrams, emoji = draftEmoji.trim()))
                        persist()
                        resetDraft()
                    }
                },
                onCancel = ::resetDraft,
                isCompact = isCompact,
            )
        } else {
            AddTemplateButton(onClick = { isAdding = true }, isCompact = isCompact)
        }
    }
}

@Composable
private fun AddTemplateButton(onClick: () -> Unit, isCompact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Food.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Food, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Add Template", fontSize = if (isCompact) 11.sp else 12.sp, fontWeight = FontWeight.Bold, color = Food)
    }
}

private data class SteppedSetting(
    val label: String,
    val value: Int,
    val unit: String,
    val color: Color,
    val step: Int,
    val range: IntRange,
    val persist: (Int) -> Unit,
)

@Composable
private fun FuelingSection(preferences: Preferences, isCompact: Boolean) {
    val servingGrams = rememberPrefField(preferences.servingGrams.toInt()) { preferences.servingGrams = it.toFloat() }
    val sipBlockMl = rememberPrefField(preferences.sipBlockMl.toInt()) { preferences.sipBlockMl = it.toFloat() }
    val sipAmountMl = rememberPrefField(preferences.sipAmountMl.toInt()) { preferences.sipAmountMl = it.toFloat() }
    val bottleCount = rememberPrefField(preferences.bottleCount) { preferences.bottleCount = it }
    val bottleVolumeMl = rememberPrefField(preferences.bottleVolumeMl.toInt()) { preferences.bottleVolumeMl = it.toFloat() }
    val reminderStart = rememberPrefField(preferences.reminderStartMinutes) { preferences.reminderStartMinutes = it }
    val manualTempC = rememberPrefField(preferences.manualTempC.toInt()) { preferences.manualTempC = it.toFloat() }
    val weatherEnabled = rememberPrefField(preferences.weatherEnabled) { preferences.weatherEnabled = it }

    val steppedSettings = listOf(
        SteppedSetting("Serving size", servingGrams.value, "g", Food, 5, 10..60, servingGrams::set),
        SteppedSetting("Sip size", sipBlockMl.value, "ml", Water, 25, 50..300, sipBlockMl::set),
        SteppedSetting("Quick Sip amount", sipAmountMl.value, "ml", Water, 25, 25..500, sipAmountMl::set),
        SteppedSetting("Bottle count", bottleCount.value, "x", Water, 1, 1..4, bottleCount::set),
        SteppedSetting("Bottle size", bottleVolumeMl.value, "ml", Water, 250, 250..1500, bottleVolumeMl::set),
        SteppedSetting("First alert after", reminderStart.value, "min", OnSurfaceVariant, 5, 10..60, reminderStart::set),
    )

    SettingsSection(title = "FUELING") {
        steppedSettings.forEachIndexed { index, setting ->
            if (index > 0) CardDivider()
            CompactValueRow(
                label = setting.label,
                value = setting.value,
                unit = setting.unit,
                color = setting.color,
                onIncrease = { setting.persist((setting.value + setting.step).coerceAtMost(setting.range.last)) },
                onDecrease = { setting.persist((setting.value - setting.step).coerceAtLeast(setting.range.first)) },
                isCompact = isCompact,
            )
        }
        CardDivider()
        ToggleRow(label = "Weather temperature", checked = weatherEnabled.value, activeColor = Water, onToggle = weatherEnabled::set, isCompact = isCompact)
        CardDivider()
        CompactValueRow(
            label = if (weatherEnabled.value) "Manual temp (fallback)" else "Manual temperature",
            value = manualTempC.value,
            unit = "°C",
            color = OnSurfaceVariant,
            onIncrease = { manualTempC.set((manualTempC.value + 1).coerceAtMost(45)) },
            onDecrease = { manualTempC.set((manualTempC.value - 1).coerceAtLeast(-10)) },
            isCompact = isCompact,
        )
    }
}

@Composable
private fun OptionsSection(preferences: Preferences, isCompact: Boolean) {
    val soundEnabled = rememberPrefField(preferences.soundEnabled) { preferences.soundEnabled = it }
    val fitExport = rememberPrefField(preferences.fitExportEnabled) { preferences.fitExportEnabled = it }

    SettingsSection(title = "OPTIONS") {
        ToggleRow(label = "Sound alerts", checked = soundEnabled.value, activeColor = Accent, onToggle = soundEnabled::set, isCompact = isCompact)
        CardDivider()
        ToggleRow(label = "FIT export", checked = fitExport.value, activeColor = Accent, onToggle = fitExport::set, isCompact = isCompact)
    }
}

@Composable
private fun TestingSection(preferences: Preferences, isCompact: Boolean) {
    val simulationEnabled = rememberPrefField(preferences.simulationModeEnabled) { preferences.simulationModeEnabled = it }
    val simulatedPower = rememberPrefField(preferences.simulatedPowerWatts.toInt()) { preferences.simulatedPowerWatts = it.toFloat() }
    val simulatedHeartRate = rememberPrefField(preferences.simulatedHeartRateBpm) { preferences.simulatedHeartRateBpm = it }

    SettingsSection(title = "TESTING") {
        ToggleRow(label = "Simulation mode", checked = simulationEnabled.value, activeColor = Error, onToggle = simulationEnabled::set, isCompact = isCompact)
        CardDivider()
        CompactValueRow(
            label = "Simulated power",
            value = simulatedPower.value,
            unit = "W",
            color = Accent,
            onIncrease = { simulatedPower.set((simulatedPower.value + 10).coerceAtMost(500)) },
            onDecrease = { simulatedPower.set((simulatedPower.value - 10).coerceAtLeast(0)) },
            isCompact = isCompact,
        )
        CardDivider()
        CompactValueRow(
            label = "Simulated heart rate",
            value = simulatedHeartRate.value,
            unit = "bpm",
            color = Accent,
            onIncrease = { simulatedHeartRate.set((simulatedHeartRate.value + 5).coerceAtMost(220)) },
            onDecrease = { simulatedHeartRate.set((simulatedHeartRate.value - 5).coerceAtLeast(0)) },
            isCompact = isCompact,
        )
        if (simulationEnabled.value) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp)) {
                Text(
                    text = "Using simulated power/HR instead of real sensors — turn off before a real ride.",
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    color = Error,
                )
            }
        }
    }
}

// =============================================================================
// SECTION CARD — wraps content in a rounded card with header
// =============================================================================

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface)) {
            content()
        }
    }
}

// TODO: point these at your own GitHub repo/contact once you have one \u2014 these
// are placeholders left over from detaching this project from its origin.
private const val PROJECT_REPO_LABEL = "yourusername/nutripro"
private const val PROJECT_REPO_URL = "https://github.com/yourusername/nutripro"
private const val CONTACT_EMAIL = "nutripro@selemba.sk"
private const val PRIVACY_POLICY_URL = "https://nutripro-landing.ykan.workers.dev/privacy"

@Composable
private fun AboutSection(isCompact: Boolean) {
    val context = LocalContext.current

    SettingsSection(title = "ABOUT") {
        val rows = listOf<@Composable () -> Unit>(
            { SettingsRow(label = "Version", trailingText = BuildConfig.VERSION_NAME, isCompact = isCompact) },
            {
                SettingsRow(
                    label = "GitHub",
                    trailingText = PROJECT_REPO_LABEL,
                    isCompact = isCompact,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_REPO_URL))) },
                )
            },
            {
                SettingsRow(
                    label = "Contact",
                    trailingText = CONTACT_EMAIL,
                    isCompact = isCompact,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$CONTACT_EMAIL") })
                    },
                )
            },
            {
                SettingsRow(
                    label = "Privacy Policy",
                    isCompact = isCompact,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) },
                )
            },
            { SettingsRow(label = "License", trailingText = "MIT", isCompact = isCompact) },
        )
        rows.forEachIndexed { index, row ->
            if (index > 0) CardDivider()
            row()
        }
    }
}

/**
 * One settings row: a label on the left, either a plain trailing value or a
 * clickable link (shown with an external-link glyph and an accent-colored
 * value). Replaces having a separate static-row and link-row composable for
 * what is the same row shape with an optional click target.
 */
@Composable
private fun SettingsRow(
    label: String,
    trailingText: String? = null,
    isCompact: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val isLink = onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = if (isCompact) 12.sp else 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = if (isCompact) (if (isLink) 10.sp else 11.sp) else (if (isLink) 11.sp else 12.sp),
                    color = if (isLink) Accent else OnSurfaceVariant,
                )
                if (isLink) Spacer(modifier = Modifier.width(4.dp))
            }
            if (isLink) Text(text = "\u2197", fontSize = 14.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun CardDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(SurfaceVariant))
}

// =============================================================================
// NUMERIC INPUT DIALOG
// =============================================================================

@Composable
private fun NumericInputDialog(
    title: String,
    currentValue: String,
    unit: String,
    accentColor: Color,
    minValue: Int,
    maxValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var textValue by remember { mutableStateOf(currentValue) }
    val isValid = textValue.toIntOrNull()?.let { it in minValue..maxValue } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            BasicTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    textValue = newValue.filter { it.isDigit() }.take(4)
                                },
                                modifier = Modifier.widthIn(min = 60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor,
                                    textAlign = TextAlign.Center,
                                ),
                                cursorBrush = SolidColor(accentColor),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (textValue.isEmpty()) {
                                            Text(
                                                text = "---",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                color = OnSurfaceVariant,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = unit,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.8f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Range: $minValue - $maxValue $unit",
                    fontSize = 12.sp,
                    color = if (isValid) OnSurfaceVariant else Error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { textValue.toIntOrNull()?.let { onConfirm(it) } },
                enabled = isValid,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isValid) accentColor else SurfaceVariant)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = "SAVE",
                    color = if (isValid) Color.White else OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}

// =============================================================================
// ROW COMPONENTS — all inside rounded cards now
// =============================================================================

@Composable
private fun FtpRow(
    value: Int,
    onUpdate: (Int) -> Unit,
    isCompact: Boolean,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = if (isCompact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "FTP",
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
            )
            if (value == 0) {
                Text(
                    text = "From Karoo profile",
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value > 0) "$value" else "---",
                fontSize = if (isCompact) 20.sp else 24.sp,
                fontWeight = FontWeight.Black,
                color = Accent,
            )
            Text(
                text = " W",
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = Accent.copy(alpha = 0.6f),
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(14.dp),
            )
        }
    }

    if (showDialog) {
        NumericInputDialog(
            title = "FTP",
            currentValue = if (value > 0) value.toString() else "",
            unit = "W",
            accentColor = Accent,
            minValue = 50,
            maxValue = 999,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onUpdate(newValue)
                showDialog = false
            },
        )
    }
}

@Composable
private fun WeightRow(
    value: Float,
    onUpdate: (Float) -> Unit,
    isCompact: Boolean,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = if (isCompact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Weight",
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
            )
            if (value == 0f) {
                Text(
                    text = "From Karoo profile",
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value > 0f) "${value.toInt()}" else "---",
                fontSize = if (isCompact) 18.sp else 22.sp,
                fontWeight = FontWeight.Black,
                color = AccentLight,
            )
            Text(
                text = " kg",
                fontSize = if (isCompact) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = AccentLight.copy(alpha = 0.6f),
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(14.dp),
            )
        }
    }

    if (showDialog) {
        NumericInputDialog(
            title = "Weight",
            currentValue = if (value > 0f) value.toInt().toString() else "",
            unit = "kg",
            accentColor = AccentLight,
            minValue = 30,
            maxValue = 200,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onUpdate(newValue.toFloat())
                showDialog = false
            },
        )
    }
}

@Composable
private fun WeighInRow(
    label: String,
    value: Float,
    onUpdate: (Float) -> Unit,
    isCompact: Boolean,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = if (isCompact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value > 0f) "%.1f".format(value) else "---",
                fontSize = if (isCompact) 18.sp else 22.sp,
                fontWeight = FontWeight.Black,
                color = Water,
            )
            Text(
                text = " kg",
                fontSize = if (isCompact) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = Water.copy(alpha = 0.6f),
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(14.dp),
            )
        }
    }

    if (showDialog) {
        NumericInputDialog(
            title = label,
            currentValue = if (value > 0f) value.toInt().toString() else "",
            unit = "kg",
            accentColor = Water,
            minValue = 30,
            maxValue = 200,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onUpdate(newValue.toFloat())
                showDialog = false
            },
        )
    }
}

@Composable
private fun CalibrateRow(
    enabled: Boolean,
    message: String?,
    isCompact: Boolean,
    onCalibrate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (enabled) Water.copy(alpha = 0.15f) else SurfaceVariant)
                .clickable(enabled = enabled, onClick = onCalibrate)
                .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Calibrate from this ride",
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Water else OnSurfaceVariant,
            )
        }
        if (!enabled) {
            Text(
                text = "Enter both weigh-ins after a ride to calibrate",
                fontSize = if (isCompact) 9.sp else 10.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (message != null) {
            Text(
                text = message,
                fontSize = if (isCompact) 10.sp else 11.sp,
                color = Water,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun CompactValueRow(
    label: String,
    value: Int,
    unit: String,
    color: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    isCompact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2212",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                )
            }

            Row(
                modifier = Modifier.width(if (isCompact) 52.dp else 58.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$value",
                    fontSize = if (isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                )
                Text(
                    text = unit,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.6f),
                )
            }

            Box(
                modifier = Modifier
                    .size(if (isCompact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    activeColor: Color,
    onToggle: (Boolean) -> Unit,
    isCompact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) activeColor else SurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (checked) "ON" else "OFF",
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Black,
                color = if (checked) Color.White else OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TemplateRow(
    template: FoodTemplate,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    isCompact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (template.isDefault) Icons.Default.Star else Icons.Outlined.Star,
            contentDescription = null,
            tint = if (template.isDefault) Food else SurfaceVariant,
            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = template.displayName,
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = if (template.isDefault) FontWeight.Bold else FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
            )
            Text(
                text = "${template.carbsGrams}g${if (template.isDefault) " · Quick Gel default" else ""}",
                fontSize = if (isCompact) 10.sp else 11.sp,
                color = if (template.isDefault) Food else OnSurfaceVariant,
            )
        }

        if (!template.isDefault) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = "Set as Quick Gel default",
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .size(if (isCompact) 22.dp else 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onSetDefault)
                    .padding(2.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .size(if (isCompact) 28.dp else 32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Error.copy(alpha = 0.15f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Error,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AddTemplateRow(
    name: String,
    carbs: String,
    emoji: String,
    onNameChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    isCompact: Boolean,
) {
    val fontSize = if (isCompact) 12.sp else 13.sp
    val fieldPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(if (isCompact) 40.dp else 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .padding(fieldPadding),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = emoji,
                    onValueChange = { v -> onEmojiChange(v.take(2)) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = if (isCompact) 16.sp else 18.sp,
                        color = OnSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(Food),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (emoji.isEmpty()) {
                                Text(
                                    text = "😀",
                                    fontSize = if (isCompact) 16.sp else 18.sp,
                                    color = OnSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .padding(fieldPadding),
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = fontSize,
                        color = OnSurface,
                    ),
                    cursorBrush = SolidColor(Food),
                    decorationBox = { innerTextField ->
                        Box {
                            if (name.isEmpty()) {
                                Text("Name", fontSize = fontSize, color = OnSurfaceVariant)
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .width(if (isCompact) 64.dp else 72.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .padding(fieldPadding),
            ) {
                BasicTextField(
                    value = carbs,
                    onValueChange = onCarbsChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = fontSize,
                        color = OnSurface,
                    ),
                    cursorBrush = SolidColor(Food),
                    decorationBox = { innerTextField ->
                        Box {
                            if (carbs.isEmpty()) {
                                Text("Carbs g", fontSize = fontSize, color = OnSurfaceVariant)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .height(if (isCompact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancel",
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                )
            }
            val canAdd = name.isNotBlank() && (carbs.toIntOrNull() ?: 0) > 0
            Box(
                modifier = Modifier
                    .height(if (isCompact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (canAdd) Food else SurfaceVariant)
                    .clickable(enabled = canAdd, onClick = onAdd)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Add",
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAdd) Color.Black else OnSurfaceVariant,
                )
            }
        }
    }
}
