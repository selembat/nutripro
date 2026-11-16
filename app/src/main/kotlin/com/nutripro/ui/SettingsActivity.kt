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

@Composable
fun SettingsScreen(
    preferences: Preferences,
    onNavigateBack: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp

    var reminderStart by remember { mutableIntStateOf(preferences.reminderStartMinutes) }
    var soundEnabled by remember { mutableStateOf(preferences.soundEnabled) }
    var fitExport by remember { mutableStateOf(preferences.fitExportEnabled) }
    var weatherEnabled by remember { mutableStateOf(preferences.weatherEnabled) }
    var ftpOverride by remember { mutableIntStateOf(preferences.ftpOverride) }
    var weightOverride by remember { mutableStateOf(preferences.weightOverride) }

    var servingGrams by remember { mutableIntStateOf(preferences.servingGrams.toInt()) }
    var sipBlockMl by remember { mutableIntStateOf(preferences.sipBlockMl.toInt()) }
    var sipAmountMl by remember { mutableIntStateOf(preferences.sipAmountMl.toInt()) }
    var bottleCount by remember { mutableIntStateOf(preferences.bottleCount) }
    var bottleVolumeMl by remember { mutableIntStateOf(preferences.bottleVolumeMl.toInt()) }
    var manualTempC by remember { mutableIntStateOf(preferences.manualTempC.toInt()) }

    var simulationModeEnabled by remember { mutableStateOf(preferences.simulationModeEnabled) }
    var simulatedPowerWatts by remember { mutableIntStateOf(preferences.simulatedPowerWatts.toInt()) }
    var simulatedHeartRateBpm by remember { mutableIntStateOf(preferences.simulatedHeartRateBpm) }

    var preRideWeight by remember { mutableStateOf(preferences.preRideWeightKg) }
    var postRideWeight by remember { mutableStateOf(preferences.postRideWeightKg) }
    var sweatMultiplier by remember { mutableStateOf(preferences.sweatMultiplier) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }

    val templates = remember { mutableStateListOf(*preferences.loadFoodTemplates().toTypedArray()) }
    var showAddTemplate by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    var newTemplateCarbs by remember { mutableStateOf("") }
    var newTemplateEmoji by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Accent)
                .padding(horizontal = 4.dp, vertical = if (isCompact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp),
            ) {
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

        // Scrollable settings
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
        ) {
            // === PROFILE ===
            SettingsSection(title = "PROFILE") {
                FtpRow(
                    value = ftpOverride,
                    onUpdate = {
                        ftpOverride = it
                        preferences.ftpOverride = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                WeightRow(
                    value = weightOverride,
                    onUpdate = {
                        weightOverride = it
                        preferences.weightOverride = it
                    },
                    isCompact = isCompact,
                )
            }

            // === SWEAT CALIBRATION ===
            SettingsSection(title = "SWEAT CALIBRATION") {
                WeighInRow(
                    label = "Pre-ride weight",
                    value = preRideWeight,
                    onUpdate = {
                        preRideWeight = it
                        preferences.preRideWeightKg = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                WeighInRow(
                    label = "Post-ride weight",
                    value = postRideWeight,
                    onUpdate = {
                        postRideWeight = it
                        preferences.postRideWeightKg = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                SettingsRow(
                    label = "Current multiplier",
                    trailingText = "%.2f×".format(sweatMultiplier),
                    isCompact = isCompact,
                )
                CardDivider()
                CalibrateRow(
                    enabled = preRideWeight > 0f && postRideWeight > 0f && preferences.lastRideSweatLossMl > 0f,
                    message = calibrationMessage,
                    isCompact = isCompact,
                    onCalibrate = {
                        val actualMl = SweatCalibrator.computeActualSweatLossMl(
                            preWeightKg = preRideWeight.toDouble(),
                            postWeightKg = postRideWeight.toDouble(),
                            fluidDrunkMl = preferences.lastRideFluidDrunkMl.toDouble(),
                        )
                        val updated = SweatCalibrator.updateMultiplier(
                            currentMultiplier = preferences.sweatMultiplier.toDouble(),
                            modeledSweatLossMl = preferences.lastRideSweatLossMl.toDouble(),
                            actualSweatLossMl = actualMl,
                        )
                        preferences.sweatMultiplier = updated.toFloat()
                        sweatMultiplier = updated.toFloat()
                        preRideWeight = 0f
                        postRideWeight = 0f
                        preferences.preRideWeightKg = 0f
                        preferences.postRideWeightKg = 0f
                        calibrationMessage = "Updated to %.2f× (measured %.0fml lost)".format(updated, actualMl)
                    },
                )
            }

            // === FOOD TEMPLATES ===
            SettingsSection(title = "FOOD TEMPLATES") {
                templates.forEachIndexed { index, template ->
                    if (index > 0) CardDivider()
                    TemplateRow(
                        template = template,
                        onSetDefault = {
                            val updated = templates.mapIndexed { i, t ->
                                t.copy(isDefault = i == index)
                            }
                            templates.clear()
                            templates.addAll(updated)
                            preferences.saveFoodTemplates(updated)
                        },
                        onDelete = {
                            templates.removeAt(index)
                            preferences.saveFoodTemplates(templates.toList())
                        },
                        isCompact = isCompact,
                    )
                }

                if (templates.isNotEmpty()) CardDivider()

                if (showAddTemplate) {
                    AddTemplateRow(
                        name = newTemplateName,
                        carbs = newTemplateCarbs,
                        emoji = newTemplateEmoji,
                        onNameChange = { newTemplateName = it },
                        onCarbsChange = { newTemplateCarbs = it.filter { c -> c.isDigit() } },
                        onEmojiChange = { newTemplateEmoji = it },
                        onAdd = {
                            val carbs = newTemplateCarbs.toIntOrNull()
                            if (newTemplateName.isNotBlank() && carbs != null && carbs > 0) {
                                val newTemplate = FoodTemplate(
                                    name = newTemplateName.trim(),
                                    carbsGrams = carbs,
                                    emoji = newTemplateEmoji.trim(),
                                )
                                templates.add(newTemplate)
                                preferences.saveFoodTemplates(templates.toList())
                                newTemplateName = ""
                                newTemplateCarbs = ""
                                newTemplateEmoji = ""
                                showAddTemplate = false
                            }
                        },
                        onCancel = {
                            showAddTemplate = false
                            newTemplateName = ""
                            newTemplateCarbs = ""
                            newTemplateEmoji = ""
                        },
                        isCompact = isCompact,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Food.copy(alpha = 0.1f))
                            .clickable { showAddTemplate = true }
                            .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Food,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Template",
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Food,
                        )
                    }
                }
            }

            // === FUELING ===
            SettingsSection(title = "FUELING") {
                CompactValueRow(
                    label = "Serving size",
                    value = servingGrams,
                    unit = "g",
                    color = Food,
                    onIncrease = {
                        servingGrams = (servingGrams + 5).coerceAtMost(60)
                        preferences.servingGrams = servingGrams.toFloat()
                    },
                    onDecrease = {
                        servingGrams = (servingGrams - 5).coerceAtLeast(10)
                        preferences.servingGrams = servingGrams.toFloat()
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Sip size",
                    value = sipBlockMl,
                    unit = "ml",
                    color = Water,
                    onIncrease = {
                        sipBlockMl = (sipBlockMl + 25).coerceAtMost(300)
                        preferences.sipBlockMl = sipBlockMl.toFloat()
                    },
                    onDecrease = {
                        sipBlockMl = (sipBlockMl - 25).coerceAtLeast(50)
                        preferences.sipBlockMl = sipBlockMl.toFloat()
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Quick Sip amount",
                    value = sipAmountMl,
                    unit = "ml",
                    color = Water,
                    onIncrease = {
                        sipAmountMl = (sipAmountMl + 25).coerceAtMost(500)
                        preferences.sipAmountMl = sipAmountMl.toFloat()
                    },
                    onDecrease = {
                        sipAmountMl = (sipAmountMl - 25).coerceAtLeast(25)
                        preferences.sipAmountMl = sipAmountMl.toFloat()
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Bottle count",
                    value = bottleCount,
                    unit = "x",
                    color = Water,
                    onIncrease = {
                        bottleCount = (bottleCount + 1).coerceAtMost(4)
                        preferences.bottleCount = bottleCount
                    },
                    onDecrease = {
                        bottleCount = (bottleCount - 1).coerceAtLeast(1)
                        preferences.bottleCount = bottleCount
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Bottle size",
                    value = bottleVolumeMl,
                    unit = "ml",
                    color = Water,
                    onIncrease = {
                        bottleVolumeMl = (bottleVolumeMl + 250).coerceAtMost(1500)
                        preferences.bottleVolumeMl = bottleVolumeMl.toFloat()
                    },
                    onDecrease = {
                        bottleVolumeMl = (bottleVolumeMl - 250).coerceAtLeast(250)
                        preferences.bottleVolumeMl = bottleVolumeMl.toFloat()
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "First alert after",
                    value = reminderStart,
                    unit = "min",
                    color = OnSurfaceVariant,
                    onIncrease = {
                        reminderStart = (reminderStart + 5).coerceAtMost(60)
                        preferences.reminderStartMinutes = reminderStart
                    },
                    onDecrease = {
                        reminderStart = (reminderStart - 5).coerceAtLeast(10)
                        preferences.reminderStartMinutes = reminderStart
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                ToggleRow(
                    label = "Weather temperature",
                    checked = weatherEnabled,
                    activeColor = Water,
                    onToggle = {
                        weatherEnabled = it
                        preferences.weatherEnabled = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = if (weatherEnabled) "Manual temp (fallback)" else "Manual temperature",
                    value = manualTempC,
                    unit = "°C",
                    color = OnSurfaceVariant,
                    onIncrease = {
                        manualTempC = (manualTempC + 1).coerceAtMost(45)
                        preferences.manualTempC = manualTempC.toFloat()
                    },
                    onDecrease = {
                        manualTempC = (manualTempC - 1).coerceAtLeast(-10)
                        preferences.manualTempC = manualTempC.toFloat()
                    },
                    isCompact = isCompact,
                )
            }

            // === ALERTS & DATA ===
            SettingsSection(title = "OPTIONS") {
                ToggleRow(
                    label = "Sound alerts",
                    checked = soundEnabled,
                    activeColor = Accent,
                    onToggle = {
                        soundEnabled = it
                        preferences.soundEnabled = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                ToggleRow(
                    label = "FIT export",
                    checked = fitExport,
                    activeColor = Accent,
                    onToggle = {
                        fitExport = it
                        preferences.fitExportEnabled = it
                    },
                    isCompact = isCompact,
                )
            }

            // === TESTING ===
            SettingsSection(title = "TESTING") {
                ToggleRow(
                    label = "Simulation mode",
                    checked = simulationModeEnabled,
                    activeColor = Error,
                    onToggle = {
                        simulationModeEnabled = it
                        preferences.simulationModeEnabled = it
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Simulated power",
                    value = simulatedPowerWatts,
                    unit = "W",
                    color = Accent,
                    onIncrease = {
                        simulatedPowerWatts = (simulatedPowerWatts + 10).coerceAtMost(500)
                        preferences.simulatedPowerWatts = simulatedPowerWatts.toFloat()
                    },
                    onDecrease = {
                        simulatedPowerWatts = (simulatedPowerWatts - 10).coerceAtLeast(0)
                        preferences.simulatedPowerWatts = simulatedPowerWatts.toFloat()
                    },
                    isCompact = isCompact,
                )
                CardDivider()
                CompactValueRow(
                    label = "Simulated heart rate",
                    value = simulatedHeartRateBpm,
                    unit = "bpm",
                    color = Accent,
                    onIncrease = {
                        simulatedHeartRateBpm = (simulatedHeartRateBpm + 5).coerceAtMost(220)
                        preferences.simulatedHeartRateBpm = simulatedHeartRateBpm
                    },
                    onDecrease = {
                        simulatedHeartRateBpm = (simulatedHeartRateBpm - 5).coerceAtLeast(0)
                        preferences.simulatedHeartRateBpm = simulatedHeartRateBpm
                    },
                    isCompact = isCompact,
                )
                if (simulationModeEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
                    ) {
                        Text(
                            text = "Using simulated power/HR instead of real sensors — turn off before a real ride.",
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            color = Error,
                        )
                    }
                }
            }

            // === ABOUT ===
            AboutSection(isCompact = isCompact)

            Spacer(modifier = Modifier.height(8.dp))
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
