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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutripro.engine.CarbBalanceTracker
import com.nutripro.model.FoodTemplate
import com.nutripro.model.IntakeEntry
import com.nutripro.ui.theme.Accent
import com.nutripro.ui.theme.Background
import com.nutripro.ui.theme.Food
import com.nutripro.ui.theme.NutriProTheme
import com.nutripro.ui.theme.OnAccent
import com.nutripro.ui.theme.OnSurface
import com.nutripro.ui.theme.OnSurfaceVariant
import com.nutripro.ui.theme.Surface
import com.nutripro.ui.theme.SurfaceVariant
import com.nutripro.ui.theme.Water
import com.nutripro.ui.theme.WaterDark
import com.nutripro.util.Preferences

/** Full-screen activity launched from a bonus action or a tap on a quick-log field. */
class QuickLogActivity : ComponentActivity() {
    private lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = NutriProExtensionHolder.preferences ?: Preferences(applicationContext)

        when (intent?.getStringExtra("mode")) {
            "quick-food" -> return logFood(preferences.getDefaultTemplate())
            "quick-water" -> return logWater(preferences.sipAmountMl.toInt())
            "quick-sodium" -> return logSodium(preferences.sodiumDoseMg.toInt())
        }

        setContent {
            NutriProTheme {
                when (intent?.getStringExtra("mode")) {
                    "water" -> WaterLogScreen(onLog = ::logWater, onCancel = ::finish)
                    "duration" -> RidePlanScreen(onPick = ::setPlannedMinutes, onCancel = ::finish)
                    else -> FoodLogScreen(
                        templates = preferences.loadFoodTemplates(),
                        onLog = ::logFood,
                        onLogCustom = ::logCustom,
                        onCancel = ::finish,
                    )
                }
            }
        }
    }

    private fun withRunningTracker(block: (CarbBalanceTracker) -> Unit) {
        val tracker = NutriProExtensionHolder.tracker
        if (tracker == null) {
            Toast.makeText(this, "NutriPro not running", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        block(tracker)
    }

    private fun logFood(template: FoodTemplate) = withRunningTracker { tracker ->
        tracker.logIntake(
            IntakeEntry(
                timestampMs = System.currentTimeMillis(),
                carbsGrams = template.carbsGrams.toDouble(),
                type = IntakeEntry.IntakeType.FOOD,
                templateName = template.name,
                templateEmoji = template.emoji.ifEmpty { null },
            ),
        )
        Toast.makeText(this, "Logged ${template.name} (${template.carbsGrams}g)", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logCustom(grams: Int) = withRunningTracker { tracker ->
        tracker.logIntake(
            IntakeEntry(
                timestampMs = System.currentTimeMillis(),
                carbsGrams = grams.toDouble(),
                type = IntakeEntry.IntakeType.FOOD,
                templateName = "Custom ${grams}g",
            ),
        )
        Toast.makeText(this, "Logged ${grams}g carbs", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logWater(ml: Int) = withRunningTracker { tracker ->
        tracker.logWater(ml.toDouble())
        Toast.makeText(this, "Logged ${ml}ml water", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun logSodium(mg: Int) = withRunningTracker { tracker ->
        tracker.logSodium(mg.toDouble())
        Toast.makeText(this, "Logged ${mg}mg sodium", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setPlannedMinutes(minutes: Int) {
        preferences.plannedRideMinutes = minutes
        val label = if (minutes >= 60) "${minutes / 60}h${if (minutes % 60 != 0) "${minutes % 60}m" else ""}" else "${minutes}m"
        Toast.makeText(this, "Ride plan set to $label", Toast.LENGTH_SHORT).show()
        finish()
    }
}

object NutriProExtensionHolder {
    var tracker: CarbBalanceTracker? = null
    var preferences: Preferences? = null
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
                contentDescription = "Back",
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

@Composable
fun FoodLogScreen(
    templates: List<FoodTemplate>,
    onLog: (FoodTemplate) -> Unit,
    onLogCustom: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val compact = isCompactScreen()
    val buttonHeight = if (compact) 48.dp else 56.dp
    var showCustom by remember { mutableStateOf(false) }
    var customGrams by remember { mutableIntStateOf(25) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(
            title = if (showCustom) "CUSTOM CARBS" else "LOG FOOD",
            barColor = Food,
            contentColor = Color.Black,
            onBack = if (showCustom) ({ showCustom = false }) else onCancel,
            compact = compact,
        )

        if (showCustom) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "CARBS", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant, letterSpacing = 1.sp)
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
                        Text(text = "grams", fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, color = Food.copy(alpha = 0.6f))
                    }

                    Pill(
                        modifier = Modifier.size(if (compact) 52.dp else 60.dp),
                        backgroundColor = Surface,
                        onClick = { customGrams = (customGrams + 5).coerceAtMost(200) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+5", tint = OnSurface, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("BACK", Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f)) { showCustom = false }
                    ActionButton("LOG ${customGrams}g", Food, Color.Black, buttonHeight, Modifier.weight(1f)) { onLogCustom(customGrams) }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                templates.forEach { template ->
                    Pill(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Surface,
                        onClick = { onLog(template) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 14.dp else 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = template.name,
                                fontSize = if (compact) 14.sp else 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(text = "${template.carbsGrams}", fontSize = if (compact) 22.sp else 26.sp, fontWeight = FontWeight.Black, color = Food)
                            Text(text = "g", fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold, color = Food.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("CUSTOM", Accent, OnAccent, buttonHeight, Modifier.weight(1f)) { showCustom = true }
                ActionButton("CANCEL", Surface, OnSurfaceVariant, buttonHeight, Modifier.weight(1f), onCancel)
            }
        }
    }
}

@Composable
fun WaterLogScreen(onLog: (Int) -> Unit, onCancel: () -> Unit) {
    val compact = isCompactScreen()
    val waterOptionsMl = listOf(150, 200, 250, 500, 750)

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(title = "LOG WATER", barColor = WaterDark, contentColor = Color.White, onBack = onCancel, compact = compact)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            waterOptionsMl.forEach { ml ->
                Pill(
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 14.dp else 18.dp),
                    backgroundColor = Surface,
                    onClick = { onLog(ml) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$ml", fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Black, color = Water)
                        Text(text = " ml", fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Bold, color = Water.copy(alpha = 0.6f))
                    }
                }
            }
        }

        ActionButton(
            label = "CANCEL",
            backgroundColor = Surface,
            textColor = OnSurfaceVariant,
            height = if (compact) 48.dp else 56.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            onClick = onCancel,
        )
    }
}

/** Preset ride-length buckets for the carb-debt duration gate (spec 3.1 extension). */
@Composable
fun RidePlanScreen(onPick: (Int) -> Unit, onCancel: () -> Unit) {
    val compact = isCompactScreen()
    val options = listOf(
        "< 1 hour" to 45,
        "1 - 2 hours" to 90,
        "2 - 3 hours" to 150,
        "3 - 4 hours" to 210,
        "4+ hours" to 300,
    )

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(title = "RIDE PLAN", barColor = Accent, contentColor = OnAccent, onBack = onCancel, compact = compact)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (label, minutes) ->
                Pill(
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 14.dp else 18.dp),
                    backgroundColor = Surface,
                    onClick = { onPick(minutes) },
                ) {
                    Text(text = label, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Black, color = Accent)
                }
            }
        }

        ActionButton(
            label = "CANCEL",
            backgroundColor = Surface,
            textColor = OnSurfaceVariant,
            height = if (compact) 48.dp else 56.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            onClick = onCancel,
        )
    }
}
