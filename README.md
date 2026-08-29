# 🫧 Bubbles

A premium bubble-popping stress buster Android app with a luxury "Midnight Glass" design system.

## Overview

Bubbles is an Android game where players pop bubbles to relieve stress. It features three game modes, a premium visual design inspired by Apple/Google aesthetics, and a complete economy system with coins, skins, and themes.

## Features

- **Three Game Modes**: Adventure (timed progression), Zen (endless relaxation), Daily Challenge
- **Luxury Design**: Liquid glass UI, gold accents, spring animations, haptic feedback
- **Economy System**: Earn coins, unlock skins and themes
- **Glass Bubble Rendering**: Multi-layer glass effects with refraction, specular highlights, and vector icons
- **Responsive Motion**: Spring physics on all interactions with reduced-motion support
- **Premium Typography**: Cormorant (display) + Montserrat (body) font pairing

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Manual (AppModule factory) |
| Persistence | DataStore Preferences |
| Ads | ironSource LevelPlay |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |

## Quick Start

```bash
# Clone the repo
git clone https://github.com/ashwathai/bubbles.git
cd bubbles

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Project Structure

```
bubbles/
├── app/src/main/java/com/ashwathai/bubbles/
│   ├── BubblesApplication.kt      # Application entry (Hilt)
│   ├── MainActivity.kt            # Single activity host
│   ├── data/
│   │   ├── local/                  # DataStore repository implementations
│   │   └── sound/                  # SoundManager (SoundPool)
│   ├── di/
│   │   └── AppModule.kt            # Hilt dependency injection
│   ├── domain/
│   │   ├── model/                  # GameModels (data classes)
│   │   ├── repository/             # Repository interfaces
│   │   └── usecase/                # GameUseCases
│   └── ui/
│       ├── game/
│       │   └── GameViewModel.kt    # Main game logic + state
│       └── theme/
│           ├── Theme.kt            # Material 3 theme
│           └── luxury/             # Premium design system
│               ├── GlassSurface.kt
│               ├── LuxuryComponents.kt
│               ├── LuxuryIcons.kt
│               ├── LuxuryMotion.kt
│               ├── LuxuryTokens.kt
│               └── LuxuryTypography.kt
├── .github/workflows/             # GitHub Actions (auto-deploy website)
├── docs/                           # Repository documentation
├── scripts/                        # Play Store automation
├── website/                        # GitHub Pages site source
└── play-listing-assets/            # Store listing screenshots
```

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/architecture.md) | Tech stack, design patterns, folder structure |
| [Setup Guide](docs/setup.md) | Development environment setup |
| [Contributing](docs/contributing.md) | Contribution guidelines |
| [Changelog](docs/changelog.md) | Version history |

## Website

The project website is auto-deployed to GitHub Pages via GitHub Actions:

🔗 **[https://atulkpal.github.io/bubbles/](https://atulkpal.github.io/bubbles/)**

Source files live in `website/`. The workflow (`.github/workflows/deploy-website.yml`) deploys on every push to `master`.

## License

Private — All rights reserved.
