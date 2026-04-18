# MindGate 🧘

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
- ⏭️ **Skip Button** — Fades in after 3 seconds for genuinely urgent moments
- 📋 **App Selection** — Searchable list of all installed apps with per-app toggles
- 🌙 **Dark / Light / System Theme** — Full Material 3 dynamic theming
- 🔁 **Auto-start on Boot** — Services restart automatically after device reboot

---

## Project Structure

```
MindGate/
├── app/src/main/
│   ├── java/com/mindgate/app/
│   │   ├── activities/
│   │   │   ├── MainActivity.kt              ← Main dashboard
│   │   │   ├── MindGateOverlayActivity.kt   ← The pause screen
│   │   │   ├── CustomizeScreenActivity.kt   ← Edit title/quote/image/duration
│   │   │   ├── AppSelectionActivity.kt      ← Choose which apps to monitor
│   │   │   └── PermissionSetupActivity.kt   ← Permission wizard
│   │   ├── services/
│   │   │   ├── AppMonitorService.kt         ← Polls UsageStatsManager every 1s
│   │   │   └── LockScreenService.kt         ← Listens for ACTION_USER_PRESENT
│   │   ├── receivers/
│   │   │   ├── BootReceiver.kt              ← Restarts services after reboot
│   │   │   └── ScreenStateReceiver.kt       ← Screen on/off events
│   │   ├── adapters/
│   │   │   └── AppListAdapter.kt            ← RecyclerView adapter
│   │   ├── models/
│   │   │   └── Models.kt                    ← ScreenContent, AppInfo, DefaultQuotes
│   │   └── utils/
│   │       └── AppPreferences.kt            ← SharedPreferences wrapper
│   ├── res/
│   │   ├── layout/                          ← 6 XML layouts
│   │   ├── drawable/                        ← 22 vector icons + shape drawables
│   │   ├── drawable-night/                  ← Dark mode drawable overrides
│   │   ├── values/                          ← colors, strings, themes (light)
│   │   └── values-night/                    ← Dark mode overrides
│   └── AndroidManifest.xml
├── build.gradle                             ← AGP 8.9.0, Kotlin 2.1.0, KSP
├── settings.gradle
├── gradle.properties                        ← Config cache, parallel builds enabled
└── gradle/wrapper/gradle-wrapper.properties ← Gradle 8.11.1
```

---

## How to Open

1. **Extract** the zip
2. Open **Android Studio** (Meerkat 2024.3.1 or Narwhal 2025.1.1+)
3. **File → Open** → select the `MindGate/` folder
4. Wait for **Gradle sync** to complete (~1–2 min on first run, downloads dependencies)
5. Connect a **physical Android device** (recommended — permissions behave differently on emulators)
6. Press ▶ **Run**

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

## Dependencies

| Library | Version |
|---|---|
| Material Components | 1.12.0 |
| AndroidX Core KTX | 1.15.0 |
| AndroidX AppCompat | 1.7.0 |
| AndroidX Lifecycle | 2.8.7 |
| AndroidX Activity KTX | 1.10.1 |
| Glide | 4.16.0 |
| WorkManager | 2.10.0 |

---

## License
MIT — free to use, modify, and distribute.
