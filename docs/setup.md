# Development Setup

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 17+ | Required by Kotlin 2.0 and Android Gradle Plugin |
| **Android Studio** | Hedgehog (2023.1.1)+ | Or latest stable with Compose support |
| **Android SDK** | API 36 (compileSdk) | Install via SDK Manager |
| **Android Emulator** | API 26+ (minSdk) | Or physical device with USB debugging |
| **Gradle** | 8.7+ (bundled) | Use wrapper: `./gradlew` |

## Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ashwathai/bubbles.git
cd bubbles
```

### 2. Open in Android Studio

- File → Open → select the `bubbles` folder
- Android Studio will sync Gradle automatically
- Wait for indexing to complete

### 3. Verify JDK

Android Studio bundles JDK 17. Confirm in:
- File → Settings → Build → Gradle → Gradle JDK

Or from terminal:

```bash
java -version
# Should show 17.x or higher
```

### 4. Install SDK Components

In Android Studio: Tools → SDK Manager → SDK Platforms

Ensure these are installed:
- Android API 36 (compileSdk)
- Android API 26 (minSdk, for emulator images)

SDK Tools tab:
- Android SDK Build-Tools 36.0.0
- Android Emulator
- Android SDK Platform-Tools

### 5. Configure Signing (Optional — for Release Builds)

Signing credentials are **never committed**. They load from `keystore.properties`
(repo root, gitignored) or environment variables. For release builds:

1. Generate a keystore (stored **outside** the repo if possible):
   ```bash
   keytool -genkey -v -keystore release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias bubbles
   ```

2. Create `keystore.properties` in the repo root:
   ```properties
   storeFile=release-key.jks
   storePassword=YOUR_PASSWORD
   keyAlias=bubbles
   keyPassword=YOUR_PASSWORD
   ```

3. `app/build.gradle.kts` reads this automatically (env-var fallback:
   `BUBBLES_STORE_FILE`, `BUBBLES_STORE_PASSWORD`, `BUBBLES_KEY_ALIAS`,
   `BUBBLES_KEY_PASSWORD`).

> **Never commit `*.jks`, `*.keystore`, or `keystore.properties`** — they are
> gitignored, but verify with `git check-ignore` before your first push.

### 6. Build and Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

Or use Android Studio's Run button (▶️).

## Project Configuration

| File | Purpose |
|------|---------|
| `gradle.properties` | JVM args (4GB heap), AndroidX, Jetifier |
| `settings.gradle.kts` | Repository config, module includes |
| `app/build.gradle.kts` | App-level build config, dependencies, signing |
| `libs.versions.toml` | Centralized dependency versions |

## Dependencies

### Core
- `androidx.compose:compose-bom` — Compose BOM for version alignment
- `androidx.activity:activity-compose` — Compose activity integration
- `androidx.lifecycle:lifecycle-runtime-compose` — StateFlow collection in Compose

### UI
- `androidx.compose.material3:material3` — Material Design 3
- `androidx.compose.ui:ui-text-google-fonts` — Google Fonts (Cormorant, Montserrat)

### Data
- `androidx.datastore:datastore-preferences` — Key-value persistence

### Ads
- `com.ironsource.sdk:mediationsdk` — ironSource LevelPlay

### DI
- `com.google.dagger:hilt-android` — Dagger Hilt (classpath dependency, but **not used** — DI is manual via `AppModule` factory pattern)

### Serialization
- `org.jetbrains.kotlinx:kotlinx-serialization-json` — JSON serialization

## GitHub Pages Deployment

The website (`website/`) is auto-deployed to GitHub Pages via GitHub Actions.

- **Workflow:** `.github/workflows/deploy-website.yml`
- **Trigger:** Every push to `master`
- **Live URL:** `https://atulkpal.github.io/bubbles/`
- **Settings:** Repo → Settings → Pages → Source → **GitHub Actions**

To deploy manually, trigger the workflow from the Actions tab → "Deploy Website" → "Run workflow".

## Troubleshooting

### Gradle Sync Fails
- Ensure JDK 17+ is configured in Android Studio
- File → Invalidate Caches → Restart
- Delete `.gradle/` and re-sync

### Compose Compiler Error
- Ensure Kotlin 2.0.20 is installed (Settings → Kotlin)
- Compose compiler extension must match: `2.0.0`

### Emulator Performance
- Enable hardware acceleration (HAXM/Hyper-V)
- Allocate 2GB+ RAM to emulator
- Use x86_64 system image
