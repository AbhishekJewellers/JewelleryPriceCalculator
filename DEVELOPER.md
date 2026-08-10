# Developer Guide

Setup, build, and development reference for the **Jewellery Price Calculator** Android app.
For the mandatory test plan run on every change, see **[AGENTS.md](./AGENTS.md)**.

---

## 1. Overview

- **Type**: Native Android app (single module `app`), 100% Kotlin.
- **Architecture**: `MainActivity` hosts a `ViewPager2` of `CalculatorFragment` tabs; state is
  persisted locally via `SharedPreferences`. No backend, no network.
- **UI**: Android Views + Material Components + ViewBinding.

## 2. Prerequisites

| Tool | Version / Notes |
|------|-----------------|
| **JDK** | 17 (source/target compatibility is Java 17) |
| **Android Studio** | Latest stable (Ladybug or newer recommended) |
| **Android SDK Platform** | API 36 (`compileSdk = 36`) |
| **Android SDK Build-Tools** | Compatible with AGP 8.9.1 |
| **Min / Target SDK** | `minSdk = 23` (Android 6.0) / `targetSdk = 35` |
| **Emulator or device** | Android 6.0+ (an API 34/35/36 emulator image is fine) |

Toolchain (managed by Gradle, no manual install needed):

- **Android Gradle Plugin (AGP)**: 8.9.1
- **Kotlin**: 2.2.10
- **Gradle**: 9.4.1 (via the Gradle wrapper)

## 3. Getting started

```bash
# 1. Clone
git clone <repo-url>
cd JewelleryPriceCalculator

# 2. Point Gradle at your Android SDK (choose ONE):
#    a) Open the project in Android Studio — it creates local.properties automatically, OR
#    b) Create local.properties manually (NOT committed):
#       sdk.dir=/absolute/path/to/Android/Sdk        (Windows: C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk)

# 3. Build
./gradlew :app:assembleDebug        # Windows: .\gradlew.bat :app:assembleDebug
```

> `local.properties` is intentionally git-ignored. Never commit it — it contains a
> machine-specific SDK path.

## 4. Common commands

```bash
./gradlew :app:compileDebugKotlin   # fast compile / sanity check
./gradlew :app:assembleDebug        # build debug APK
./gradlew :app:installDebug         # build + install on a running emulator/device
./gradlew test                      # JVM unit tests
./gradlew connectedAndroidTest      # instrumented tests (needs a device/emulator)
./gradlew clean                     # clean build outputs
```

Launch after install:

```bash
adb shell monkey -p abhishek.jewellers.jewellerypricecalculator -c android.intent.category.LAUNCHER 1
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## 5. Project structure

```
app/
  build.gradle                     # module config (SDK levels, deps, versionName/Code)
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/abhishek/jewellers/jewellerypricecalculator/
      MainActivity.kt              # tabs (ViewPager2) + theme + persistence of tab list
      CalculatorFragment.kt        # per-tab calculator: inputs, sync, validation, submit
      EditTextUtils.kt             # input formatting helpers (Indian currency, decimals, signs)
    res/
      layout/                      # activity_main.xml, fragment_calculator.xml
      values/                      # strings.xml, arrays.xml (material types), colors, styles
      values-night/                # dark theme colors
  src/test/                        # JVM unit tests
  src/androidTest/                 # instrumented tests
build.gradle                       # root plugins/versions
settings.gradle                    # modules + repositories
gradle/wrapper/                    # Gradle wrapper
```

## 6. Key dependencies

- `androidx.core:core-ktx`, `androidx.appcompat:appcompat`, `androidx.activity:activity-ktx`
- `com.google.android.material:material`
- `androidx.constraintlayout:constraintlayout`
- `androidx.viewpager2` (via AndroidX)
- Tests: `junit`, `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`

## 7. Coding conventions

- Kotlin official code style (`kotlin.code.style=official`).
- ViewBinding is enabled (`buildFeatures.viewBinding = true`).
- Keep calculation, sync, and validation logic in `CalculatorFragment`; reusable input/formatting
  behavior in `EditTextUtils`.
- Only comment non-obvious logic.

## 8. Testing

There is no full automated suite yet; testing is primarily manual/UI-driven on an emulator.

- **Before marking any change done, run the applicable sections of [AGENTS.md](./AGENTS.md).**
- Prefer adding JUnit/Espresso coverage for new logic and wiring it into Gradle over time.

## 9. Versioning & release

- Bump `versionName` and `versionCode` in `app/build.gradle` for each release.
- Record notable changes in the **Changelog** section of [README.md](./README.md).
- **Cutting a release**: push a git tag (e.g. `git tag v9.0 && git push origin v9.0`). The
  **Release APK** workflow (see §10) builds the APK and uploads it as an artifact named
  `JewelleryPriceCalculator_<tag>.apk`.

## 10. Continuous Integration

The repo ships a single GitHub Actions workflow:
[`.github/workflows/release-apk.yml`](./.github/workflows/release-apk.yml).

- **Trigger**: any pushed tag (`on: push: tags: ['*']`).
- **Runner**: `ubuntu-latest` with JDK 17 (Temurin) and Gradle 9.4.1 via
  `gradle/actions/setup-gradle`.
- **Wrapper JAR**: `gradle/wrapper/gradle-wrapper.jar` is git-ignored (`*.jar`), so the workflow
  regenerates it with `gradle wrapper --gradle-version 9.4.1` before invoking `./gradlew`.
- **Build**: `./gradlew :app:assembleDebug` (the debug APK is signed with the debug key and is
  installable; there is no release signing config/keystore in the repo).
- **Output**: `app/build/outputs/apk/debug/app-debug.apk` is renamed to
  `JewelleryPriceCalculator_<tag>.apk` and uploaded via `actions/upload-artifact`. Download it
  from the run's **Artifacts** section under the **Actions** tab.

To release a signed **release** build later, add a `signingConfig` (keystore supplied via
repository **Secrets**, never committed) and switch the build step to `assembleRelease`.

## 11. Troubleshooting

- **`./gradlew` fails / wrapper JAR missing on a fresh clone**: `gradle/wrapper/gradle-wrapper.jar`
  is not tracked (the repo ignores `*.jar`). If it is absent, regenerate it with a system Gradle
  (`gradle wrapper --gradle-version 9.4.1`) or open the project once in Android Studio.
- **SDK location not found**: create `local.properties` with `sdk.dir=...` (see §3), or open in
  Android Studio.
- **JDK mismatch**: ensure the Gradle JDK is 17 (Android Studio → Settings → Build Tools → Gradle).
