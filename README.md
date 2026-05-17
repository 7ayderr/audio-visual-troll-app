# Weather Monitor

Android dashboard app locked to **landscape orientation**, auto-scaling across phones, tablets, and smartboards.

## Build Status
![Build APK](https://github.com/7ayderr/audio-visual-troll-app/actions/workflows/build.yml/badge.svg)

---

## Project Structure

```
WeatherMonitor/
├── .github/
│   └── workflows/
│       └── build.yml              # CI — builds APK + runs tests on push
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/system/weathermonitor/
│   │   │   │   └── MainActivity.java
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── ic_weather_placeholder.xml
│   │   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   │   └── ic_launcher_foreground.xml
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml          # Phone landscape
│   │   │   │   ├── layout-sw600dp/
│   │   │   │   │   └── activity_main.xml          # Tablet landscape
│   │   │   │   ├── mipmap-anydpi-v26/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   └── ic_launcher_round.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── dimens.xml                 # Phone dimensions
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-sw600dp/
│   │   │   │   │   └── dimens.xml                 # Tablet dimensions (~1.4×)
│   │   │   │   └── values-sw900dp/
│   │   │   │       └── dimens.xml                 # Smartboard dimensions (~2×)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/…                                 # JVM unit tests
│   │   └── androidTest/…                          # Instrumented tests
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── .gitignore
└── README.md
```

---

## Screen Size Scaling

| Qualifier     | Smallest Width | Target Devices          | Scale |
|---------------|---------------|-------------------------|-------|
| *(default)*   | < 600dp       | Phones (landscape)      | 1×    |
| `sw600dp`     | ≥ 600dp       | 7"+ tablets             | ~1.4× |
| `sw900dp`     | ≥ 900dp       | 10"+ tablets, smartboards | ~2× |

Orientation is locked to **landscape** via `android:screenOrientation="landscape"` in the manifest.

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Unit tests
./gradlew test
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Tech Stack

- **Language:** Java
- **Min SDK:** 21 (Android 5.0 Lollipop)
- **Target SDK:** 34 (Android 14)
- **Layout:** ConstraintLayout with percent guidelines
- **Binding:** ViewBinding
- **CI:** GitHub Actions

---

## Roadmap

- **Phase 1** ✅ — Base repo, landscape layout, auto-scaling, CI
- **Phase 2** — Weather API integration, live data, location
- **Phase 3** — Alerts, audio notifications, multi-station support
