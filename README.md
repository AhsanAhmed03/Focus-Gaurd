# Focus Guard 🧘

**Digital Wellbeing Guardian for Android**

MindGate shows a customisable mindfulness pause screen every time you unlock your phone or open a selected app — creating a moment of intentional reflection before you dive into social media or any distracting app.

---

## Compatibility

| Tool | Version |
|---|---|
| Android Studio | Meerkat 2024.3.1 / Narwhal 2025.1.1+ |
| AGP (Android Gradle Plugin) | **8.9.0** |
| Kotlin | **2.1.0** |
| Gradle | **8.11.1** |
| compileSdk / targetSdk | **36 (Android 16)** |
| minSdk | 26 (Android 8.0) |
| Java | **17** |
| Annotation Processor | **KSP** (kapt removed) |

---

## Features

- 🔒 **Lock Screen Trigger** — Overlay appears every time you unlock your phone
- 📱 **App Launch Trigger** — Intercepts any apps you choose before they open
- ✍️ **Fully Customisable Screen** — Set your own title, motivational quote, and background image
- ⏱️ **Auto-close Timer** — Screen closes after 3–30 seconds with an animated progress bar
- 🚨 **Emergency Exit** — One-tap dismissal, shown only on the lock screen trigger
- 📋 **App Selection** — Searchable list of all installed apps with per-app toggles
- 🌙 **Dark / Light / System Theme** — Full Material 3 dynamic theming
- 🔁 **Auto-start on Boot** — Services restart automatically after device reboot

---


## First-Time Setup (on device)

The app opens the permission wizard automatically on first launch. Grant in order:

1. **Usage Access** → Settings → Special App Access → Usage Access → MindGate → Enable
2. **Display Over Other Apps** → Settings → Special App Access → Display Over Other Apps → MindGate → Allow
3. **Notifications** → Allow (required on Android 13+)
4. **Battery Optimization** → Don't optimize (keeps services alive)

Then go back to MindGate and flip the main toggle **ON**.

---

## How It Works

### Lock Screen Detection
`LockScreenService` is a foreground service that registers a `BroadcastReceiver` for `ACTION_USER_PRESENT` (fired when the user unlocks). It uses `RECEIVER_NOT_EXPORTED` on Android 13+ and has a 5-second cooldown to prevent double-firing.

### App Launch Detection
`AppMonitorService` polls `UsageStatsManager.queryUsageStats()` every second. When a selected app appears as the foreground app, it launches `MindGateOverlayActivity`. A 30-second per-app cooldown prevents the screen repeating immediately.

### Overlay Screen
`MindGateOverlayActivity` uses `setShowWhenLocked(true)` + `setTurnScreenOn(true)` (API 27+). An `ObjectAnimator` drives the progress bar. Back press is intercepted with `OnBackPressedDispatcher` (modern API). The emergency button only appears when triggered from the lock screen.

### Foreground Services (Android 14+)
Both services use `ServiceCompat.startForeground()` with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` to comply with Android 14's stricter foreground service requirements.

---

## Customisation

| What | Where |
|---|---|
| Default quotes | `models/Models.kt` → `DefaultQuotes.quotes` |
| Lock screen cooldown | `LockScreenService.kt` → `unlockCooldown` (default 5s) |
| App monitor cooldown | `AppMonitorService.kt` → hardcoded `30_000` ms |
| Overlay gradient colours | `drawable/bg_overlay_gradient.xml` |
| Progress bar colours | `drawable/progress_bar_drawable.xml` |

---

## Known Limitations

1. **~1s detection lag** — UsageStats polling has inherent latency; the overlay may appear just after the target app's splash screen. This is an Android platform constraint.
2. **Aggressive OEMs** — Xiaomi (MIUI), Huawei (EMUI), and some Samsung devices kill background services. Users should enable "Autostart" in device settings and whitelist MindGate from battery saver.
3. **Emulator testing** — UsageStats and lock screen detection work best on physical devices.


---

## License
MIT — free to use, modify, and distribute.
