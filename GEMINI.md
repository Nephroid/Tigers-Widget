# Android & Tigers-Widget Project Guidelines

This file defines the engineering guidelines, architecture patterns, and best practices for the Detroit Tigers Android App & Home Screen Widget (`Tigers-Widget`). All agents and developers working in this repository should adhere to these practices.

---

## 1. Project Architecture & Stack

- **UI Layer**: Jetpack Compose with Material 3 and dynamic color (Material You) theming.
- **Widget Layer**: Android AppWidget framework using `RemoteViews` and `AppWidgetProvider`.
- **Presentation / State**: MVVM architecture (`GameViewModel` exposing `StateFlow`).
- **Data Layer**:
  - `GameRepository` coordinating remote and local sources.
  - Remote: MLB Stats REST API via Retrofit + OkHttp.
  - Local: Room Database (`AppDatabase`, `GameDao`) for offline caching and fast startup.
- **AI Service**: `GeminiSearchService` for Tigers context and search augmentation.
- **Testing**: JUnit 4, Robolectric for Android-aware unit tests, Roborazzi for visual regression/screenshot tests.
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`), version catalog (`libs.versions.toml`), and CalVer (`version.properties`).

---

## 2. RemoteViews & AppWidget Best Practices

Because Android `RemoteViews` operate across process boundaries with strict system limitations, follow these rules:

1. **RemoteViews XML Constraints**:
   - **Never** use `<View>` directly in widget XML layouts. Android's RemoteViews whitelist strictly rejects generic `<View>` elements and throws an inflation crash at runtime.
   - Use `<FrameLayout>` or `<LinearLayout>` with a background drawable instead of a bare `<View>` for dividers and spacers.
2. **Proportional Scaling & Foldable Support**:
   - Use `android:layout_height="0dp"` with `android:layout_weight` for all vertical child cards/sections inside `LinearLayout` containers. This guarantees the widget host's allocated space is fully consumed without clipping or dead space across both cover screens and large inner foldable screens (e.g., Pixel Fold / Pixel 9 Pro Fold).
   - Scale typography dynamically in `applyResponsiveLayout()` using proportional formulas based on measured widget height (`minHeight`) rather than rigid breakpoint step functions.
3. **Battery & Alarm Efficiency**:
   - Avoid aggressive polling when no games are live or during the offseason.
   - Schedule widget refreshes via `AlarmManager` with inexact alarms or `WorkManager` when appropriate.
4. **Color Palettes & Dynamic Theming**:
   - Maintain compatibility across API levels: provide baseline colors in `res/values/colors_widget.xml`, and dynamic Material You colors in `res/values-v31/` and `res/values-night-v31/`.

---

## 3. Jetpack Compose Guidelines

1. **State Hoisting**:
   - Composables must be stateless wherever possible. Accept UI state as parameters and emit events through lambda callbacks (`onRefreshClick = { ... }`).
2. **Recomposition & Performance**:
   - Avoid creating heavy objects or executing side-effects directly in composable bodies.
   - Use `remember` for expensive computations and `derivedStateOf` for derived values.
   - Do not pass mutable states deep into the composable tree; pass read-only state or explicit data classes.
3. **Edge-to-Edge & System Insets**:
   - Always respect system bars using `WindowInsets.safeDrawing`, `WindowInsets.systemBars`, or Scaffold inner padding.
   - Maintain dark/light theme contrast parity following Material 3 guidelines.

---

## 4. Coroutines, Threading & Data Handling

1. **Dispatcher Discipline**:
   - Always offload network (MLB API, Gemini) and disk operations (Room database) to `Dispatchers.IO`.
   - Never run blocking network or DB calls on `Dispatchers.Main`.
2. **Lifecycle Scoping**:
   - Never use `GlobalScope`.
   - Use `viewModelScope` in ViewModels and `lifecycleScope` in activities.
3. **Room Database Single Source of Truth**:
   - Expose queries as reactive `Flow<T>` from DAOs.
   - Use `OnConflictStrategy.REPLACE` for game data upserts to preserve cache freshness.
4. **Graceful Degradation**:
   - If the device is offline or the MLB API fails, load cached data from Room and display non-intrusive status indicators without crashing.

---

## 5. Testing & Verification

1. **Unit & Widget Tests**:
   - Any layout or responsive sizing adjustments in `DetroitTigersWidgetProvider` must be validated in `DetroitTigersWidgetProviderTest` using Robolectric.
2. **Visual Screenshot Tests**:
   - Roborazzi tests (`GreetingScreenshotTest`) verify UI layout consistency across screen variations.
   - Before submitting changes, run:
     ```bash
     ./gradlew testDebugUnitTest
     ```
3. **Build Health**:
   - Verify that `./gradlew assembleDebug` compiles cleanly with zero unhandled warnings or deprecation errors.

---

## 6. Versioning, Git & CI/CD Workflow

1. **CalVer Versioning**:
   - Versions follow `YYYY.M.D.BUILD` (e.g. `v26.9.3.4`), defined in `version.properties`:
     - `VERSION_MAJOR`: 2-digit year (e.g., 26 for 2026).
     - `VERSION_MINOR`: Month (e.g., 9).
     - `VERSION_PATCH`: Day (e.g., 3).
     - `VERSION_BUILD`: Build increment for the day.
   - When introducing significant fixes or features, increment `VERSION_BUILD` or use `./gradlew bumpBuild`.
2. **Git Commit Conventions**:
   - Use conventional commit messages: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`.
   - Mention the version in the commit title when bumping version (e.g., `fix(widget): improve foldable padding (v26.9.3.5)`).
3. **Automated CI/CD Release**:
   - Pushes to `main` and version tags trigger [.github/workflows/build-apk.yml](.github/workflows/build-apk.yml) to automatically compile, package, sign with `release.keystore`, and publish GitHub Releases with standalone APK downloads.
