# Architecture

## Overview

Bubbles follows **Clean Architecture** with **MVVM** pattern, separating concerns into data, domain, and UI layers.

## Layer Breakdown

### UI Layer (`ui/`)

| Component | File | Purpose |
|-----------|------|---------|
| `MainActivity` | `MainActivity.kt` | Single-activity Compose host; renders all screens |
| `GameViewModel` | `ui/game/GameViewModel.kt` | Game state, logic, scoring, coin management |
| `Theme` | `ui/theme/Theme.kt` | Material 3 dynamic color theme |
| `Luxury Design System` | `ui/theme/luxury/` | Premium tokens, glass surfaces, components |

### Domain Layer (`domain/`)

| Component | File | Purpose |
|-----------|------|---------|
| `GameModels` | `domain/model/GameModels.kt` | Data classes: `Bubble`, `GameState`, `GameMode`, etc. |
| `Repositories` | `domain/repository/` | Interfaces: `ScoreRepository`, `EconomyRepository`, `SettingsRepository` |
| `UseCases` | `domain/usecase/GameUseCases.kt` | Business logic: scoring, coin rewards, skin unlocking |

### Data Layer (`data/`)

| Component | File | Purpose |
|-----------|------|---------|
| `ScoreRepositoryImpl` | `data/local/ScoreRepositoryImpl.kt` | Persists high scores via DataStore |
| `EconomyRepositoryImpl` | `data/local/EconomyRepositoryImpl.kt` | Persists coins, owned skins/themes |
| `SettingsRepositoryImpl` | `data/local/SettingsRepositoryImpl.kt` | Persists sound, haptics, difficulty preferences |
| `SoundManager` | `data/sound/SoundManager.kt` | SoundPool-based sound effects |

### DI Layer (`di/`)

| Component | File | Purpose |
|-----------|------|---------|
| `AppModule` | `di/AppModule.kt` | Manual dependency injection (factory pattern, provides repositories, use cases, sound manager) |

## Data Flow

```
User Interaction
       ↓
   Compose UI ← observes → GameViewModel (StateFlow)
       ↓                         ↓
  Recomposition            UseCases
                             ↓
                       Repositories (interfaces)
                             ↓
                    DataStore (implementations)
```

- **State** flows up via `StateFlow<GameUiState>`
- **Events** flow down via ViewModel methods
- **Persistence** is handled through DataStore Preferences

## Luxury Design System

The `ui/theme/luxury/` package implements the "Midnight Glass" design language:

| File | Purpose |
|------|---------|
| `LuxuryTokens.kt` | Color palettes (dark + light), spacing scale, radius scale, glass opacities, semantic colors |
| `LuxuryTypography.kt` | Cormorant (serif display) + Montserrat (sans body) text styles |
| `LuxuryMotion.kt` | Spring presets, duration constants, reduced-motion helpers |
| `GlassSurface.kt` | Reusable glass card component (gradient fill, hairline border, blur) |
| `LuxuryComponents.kt` | Buttons, toggles, stat displays, timer ring, section headers |
| `LuxuryIcons.kt` | Vector Path icons (bomb, prism, ice, crown, sparkle) replacing emoji |

### Glass Surface Rendering

Bubbles are rendered with 6 visual layers:

1. Refraction ring (outer glow)
2. Main glass gradient (radial fill)
3. Lens layer (inner highlight)
4. Specular highlight (white reflection)
5. Secondary highlight (subtle glow)
6. Vector icon (bomb, prism, ice crystal, crown, sparkle)

### Color System

| Role | Light | Dark |
|------|-------|------|
| Background | `#FFFAF9` | `#0C0A09` |
| Surface | `#FFFFFF` | `#1C1917` |
| Gold Accent | `#A16207` | `#F0B429` |
| Gold Highlight | `#CA8A04` | `#FCD34D` |
| Text Primary | `#1C1917` | `#FAFAF9` |

## Game Modes

| Mode | Description |
|------|-------------|
| **Adventure** | Timed mode with boss bubbles every 10 levels. Score multiplied by level. |
| **Zen** | Endless mode, no timer, pure relaxation. |
| **Daily** | Same puzzle for everyone each day (seed-based). |

## Economy

- **Coins** earned per pop (scaled by level multiplier)
- **Skins** unlockable at coin thresholds (Common → Rare → Epic → Legendary)
- **Themes** unlockable at higher thresholds
- All persisted via DataStore
