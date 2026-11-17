# NutriPro

[![License](https://img.shields.io/badge/License-MIT-0d1117?style=flat-square&logo=opensourceinitiative&logoColor=white)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Karoo%202%2F3-0d1117?style=flat-square&logo=android&logoColor=white)](https://www.hammerhead.io/)
[![Release](https://img.shields.io/github/v/release/yourusername/nutripro?style=flat-square&color=0d1117&logo=github&logoColor=white)](https://github.com/yourusername/nutripro/releases/latest)

**A fueling and hydration coach that lives on your Hammerhead Karoo bike computer.**

NutriPro watches your power, heart rate, elapsed time and the weather, and tells you — in grams and milliliters, not vague reminders — how far ahead or behind your fueling plan you are *right now*. No more guessing whether "one gel an hour" is enough on a hot day, or too much on an easy one.

---

## TL;DR (short description)

NutriPro is an open-source Karoo data-field extension that turns sports-nutrition science (carbohydrate oxidation curves, sweat-rate modeling, sodium loss) into three live "debt" meters — **Carbs**, **Fluid**, and **Sodium** — plus one-tap logging and smart eat/drink alerts that go quiet when you're hammering a climb or bombing a descent. It runs entirely on-device, needs no cloud account, and exports everything to your `.fit` file for analysis afterward.

---

## The long version

### The problem

Most riders fuel on vibes: "I'll eat something at the top of the climb," or "I've had two bottles, I'm probably fine." On rides under 90 minutes that's harmless. Past that, it's how people bonk, cramp, or end up nauseated from choking down sports food reactively instead of steadily.

The sports-science answer — carbohydrate intake scaled to exercise intensity, fluid intake scaled to sweat rate, sodium replaced in proportion to sweat loss — is well established, but it's math nobody wants to do mid-ride. NutriPro does that math every second and shows you the one number that matters: *are you ahead or behind, and by how much?*

### How it works, in plain terms

Think of each of the three resources — carbs, fluid, sodium — as a **running tab**, like a bar bill that's constantly ticking upward while you ride:

1. **The engine totals up what your body is asking for.** Every second, based on your current effort and conditions, it adds a small amount to what you "owe" — more during a hard effort or a hot day, less when you're soft-pedaling.
2. **Logging food or drink pays down the tab.** Tap the Quick Gel action, log a bottle refill, or log sodium tabs, and that amount comes straight off the balance.
3. **The field shows the running difference.** A positive number means you're ahead of the target (a credit); a negative number means you're behind and the field shifts color — green through amber to red — so you can see urgency at a glance without reading digits.

This is deliberately framed as *debt*, not just "intake vs. burn," because what matters on the bike isn't the total calories you've burned — it's whether you're currently behind on what you need *right now*, which is exactly what a debt/credit ledger captures.

### The science behind each engine

#### Carbohydrate debt
Exogenous carb intake targets are tiered by exercise intensity (adapted from the standard sports-nutrition "intake curve" used for multi-hour endurance events):

| Intensity (%FTP) | Target intake | Energy source split |
|---|---|---|
| < 55% | 30 g/h | Mostly fat |
| 55–75% | 45 g/h | Mixed |
| 75–90% | 60 g/h | Carb-dominant |
| 90–105% | 75 g/h | Carb-dominant |
| > 105% (Race mode: higher) | 90 g/h (115 g/h) | Carb only — glycogen-limited |

Two refinements matter in practice:
- **The first hour is "free."** Muscle glycogen stores alone comfortably cover roughly the first 60 minutes of riding, so the carb target only ramps in once your planned or elapsed ride length shows you're actually going to need exogenous carbs — short rides don't get nagged to eat. See [`DurationGate.kt`](app/src/main/kotlin/com/nutripro/engine/DurationGate.kt).
- **Gut absorption, not burn rate, is the real ceiling.** You can burn carbs faster than your gut can absorb them at high intensity, so the target is capped at a realistic intake rate rather than trying to match calorie burn 1:1. See [`IntensityZone.kt`](app/src/main/kotlin/com/nutripro/engine/IntensityZone.kt) and [`CarbDebtEngine.kt`](app/src/main/kotlin/com/nutripro/engine/CarbDebtEngine.kt).

Carb debt can also go *negative* (a credit) if you eat ahead of schedule — that credit carries forward and gets absorbed naturally as debt accrues again, instead of being wasted.

#### Fluid debt
Fluid needs are modeled from sweat loss, not a flat "bottle an hour" rule:

```
sweat rate (L/h) = (0.25 + 0.0025 × power_watts) × temperature_factor × personal_multiplier
drink target (L/h) = min(sweat_rate × 0.70, 1.0 L/h)
```

- Power drives sweat rate up (harder effort → more heat to dump).
- A temperature factor scales sweat rate relative to a 20 °C baseline, clamped between 0.5× and 1.8× so an extreme reading can't produce an absurd target.
- The drink target replaces **~70% of sweat loss**, not 100% — fully replacing sweat loss in real time is neither necessary nor achievable, and the field is capped at 1 L/h because the gut can only absorb fluid so fast regardless of how much you're sweating.
- A `personalMultiplier` lets individual riders calibrate the model up or down once they know their own sweat rate (see [`SweatCalibrator.kt`](app/src/main/kotlin/com/nutripro/engine/SweatCalibrator.kt)).

See [`SweatModel.kt`](app/src/main/kotlin/com/nutripro/engine/SweatModel.kt) and [`FluidDebtEngine.kt`](app/src/main/kotlin/com/nutripro/engine/FluidDebtEngine.kt). The field also tracks remaining bottle volume, so it can flag a near-empty bottle before you're caught out.

#### Sodium debt
Sodium leaves the body with sweat regardless of how much fluid you replace, so sodium loss is tied to the *actual* sweat rate, not the (smaller) fluid-replacement target:

```
sodium loss (mg/h) = sweat_rate (L/h) × sweat sodium concentration (mg/L)
```

This produces a running sodium balance alongside the carb and fluid ones, so cramping risk on hot or salty-sweat days is visible rather than discovered the hard way. See [`SodiumDebtEngine.kt`](app/src/main/kotlin/com/nutripro/engine/SodiumDebtEngine.kt).

#### Smart alerts that respect the moment
A reminder that fires mid-sprint or mid-descent is useless at best and dangerous at worst, so alerts are gated by context, not just a timer:
- **Unsafe-moment hold** — an alert due during a hard effort (well above FTP) or a fast, steep descent is *held*, not dropped, and released the moment conditions ease, with a hard ceiling so it can never be withheld indefinitely. See [`UnsafeConditionDetector.kt`](app/src/main/kotlin/com/nutripro/engine/UnsafeConditionDetector.kt) and [`AlertHoldGate.kt`](app/src/main/kotlin/com/nutripro/engine/AlertHoldGate.kt).
- **Pre-climb / pre-interval nudges** — a heads-up to top up *before* a known hard effort starts, when eating is still easy. See [`PreClimbNudge.kt`](app/src/main/kotlin/com/nutripro/engine/PreClimbNudge.kt) / [`PreIntervalNudge.kt`](app/src/main/kotlin/com/nutripro/engine/PreIntervalNudge.kt).
- **Re-remind backoff** — a dismissed alert doesn't nag again immediately. See [`ReRemindTracker.kt`](app/src/main/kotlin/com/nutripro/engine/ReRemindTracker.kt).

### What you set once

A **Ride Plan** field lets you confirm a ride mode — Casual Z2 / Sport / Workout / Race — and a planned duration before or during a ride. Confirming a mode is treated as your explicit intent: it overrides the duration-based ramp-up entirely, so a deliberately hard 45-minute workout still gets the full intensity-based carb target instead of being capped by the clock.

---

## Features

- **Carb / Fluid / Sodium Debt** — three live "ahead/owed" balances driven by real sports-nutrition models, not flat timers
- **Duration-aware carb targets** — no nagging to eat on short rides; full intensity curve once the ride is long enough to need it
- **Ride Plan picker** — confirm Casual Z2 / Sport / Workout / Race and a planned duration; overrides duration inference when you know better
- **Context-aware alerts** — held during hard efforts and fast descents, released once it's safe; pre-climb/pre-interval heads-up nudges
- **One-tap logging** — Quick Gel / Quick Sip / Quick Sodium bonus actions, plus full food/water logging screens
- **Undo** — reverse the last logged entry within 60 seconds
- **FIT Export** — carb/fluid/sodium data written to your `.fit` file as developer fields for post-ride analysis
- **Food Templates** — customizable presets (gels, bars, bananas, etc.) for one-tap logging
- **Adaptive layouts** — graphical fields render differently across all six Karoo field sizes (SMALL → NARROW)
- **Light/dark aware** — plain numeric fields follow the Karoo Appearance setting (or your device's system theme)

## Data Fields

NutriPro ships ~20 data fields for Karoo ride screens; the graphical ones (Carb Debt, Fluid Debt, Sodium Debt, Ride Plan, Quick Log, Hydration, Quick Sip/Carb/Sodium, Bottles) adapt their content to whichever of the six Karoo layout sizes you drop them into.

| Size | Grid | Resolution | Content |
|------|------|-----------|---------|
| SMALL | Half-width, short | 235×195px | Value only |
| SMALL_WIDE | Full-width, short | 470×130-156px | Label + value |
| MEDIUM_WIDE | Full-width, medium | 470×195px | Label + value + metrics row |
| MEDIUM | Half-width, tall | 235×390px | Label + value + secondary info |
| LARGE | Full-width, tall | 470×250+px | Full detail with dividers and metric rows |
| NARROW | Half-width, very tall | 235×600+px | Stacked vertical layout |

## Bonus Actions

- **Log Food** — open food logging screen
- **Log Water** — log water intake
- **Quick Gel** — instant-log default food template
- **Undo Last** — undo last intake entry (within 60s)

## Installation

### Sideload

1. Download `nutripro.apk` from [Releases](https://github.com/yourusername/nutripro/releases/latest)
2. Install via ADB:
   ```bash
   adb install nutripro.apk
   ```
3. The extension appears automatically in Karoo's extension list

## Build from Source

```bash
git clone https://github.com/yourusername/nutripro.git
cd nutripro
JAVA_HOME="/path/to/jdk" ANDROID_HOME="/path/to/android-sdk" ./gradlew app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/nutripro.apk`

### Requirements

- JDK 21 (Android Studio JBR recommended)
- Android SDK with compileSdk 35
- Gradle 8.7
- Kotlin 2.0.0

## Configuration

Tap the extension in Karoo's extension list to open settings:

- **FTP / Weight** — override Karoo profile values or use defaults
- **Food Templates** — customize quick-log food items (name, carbs per serving)
- **Eat / Drink / Sodium Reminder Intervals** — how often to be reminded
- **Sound Alerts** — enable/disable audible reminders
- **FIT Export** — toggle carb/fluid/sodium data in FIT files as DeveloperFields

## Architecture

- **Pure Kotlin engines** (`app/src/main/kotlin/com/nutripro/engine/`) — no Android dependencies, fully unit-testable: `CarbDebtEngine`, `FluidDebtEngine`, `SodiumDebtEngine`, `IntensityZone`, `DurationGate`, `SweatModel`, `UnsafeConditionDetector`, `AlertHoldGate`, and the pre-climb/interval nudges
- **`CarbBalanceTracker`** — core state engine with `StateFlow<RideNutritionState>`, coordinating the debt engines, intake logging, and auto-save every 30s
- **DataTypes** (`app/src/main/kotlin/com/nutripro/karoo/`) — each extends `DataTypeImpl` from the karoo-ext SDK, streaming values and rendering Glance views
- **Glance Components** (`app/src/main/kotlin/com/nutripro/glance/`) — reusable composables (`DataFieldContainer`, `ColoredFieldContainer`, `ValueText`, `LabelText`, `MetricValueRow`) with a centralized `GlanceColors` palette
- **FIT Integration** — `DeveloperField` entries written during Recording/Paused ride states

## Contributing

Issues and pull requests are welcome. The engine classes are pure Kotlin with no Android dependencies, so most of the fueling logic can be read, tested, and improved without touching the Karoo SDK at all — see `app/src/test/kotlin/com/nutripro/engine/` for the existing test suite.

## License

MIT — see [LICENSE](LICENSE).
