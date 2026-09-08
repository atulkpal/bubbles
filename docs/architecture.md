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
| `GameModels` | `domain/model/GameModels.kt` | Data classes: `Bubble`, `GameState`, `LevelConfig`, etc.; `starsForTimeFraction` star rating; `generateProceduralLevel()` (levels 21–100) |
| `Repositories` | `domain/repository/` | Interfaces: `ScoreRepository`, `EconomyRepository`, `SettingsRepository` |
| `UseCases` | `domain/usecase/GameUseCases.kt` | Business logic: spawning (`SpawnBubblesUseCase`), spawn gating (`CanSpawnBubbleUseCase`), tap handling, bubble updates, level completion (`CheckLevelCompleteUseCase`) |

### Data Layer (`data/`)

| Component | File | Purpose |
|-----------|------|---------|
| `ScoreRepositoryImpl` | `data/local/ScoreRepositoryImpl.kt` | Persists high scores, zen best, games played, and highest cleared adventure level via DataStore |
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
| **Adventure** | Timed levels 1–100 (curated 1–20, procedural 21–100). Each level has a fixed spawn budget (`maxBubbles`); it completes when every bubble has spawned AND the board is cleared. Boss bubbles on designated levels. Cleared levels unlock the next one (persisted) and can be replayed via level select. |
| **Zen** | Endless mode, no timer, pure relaxation. |
| **Daily** | Same puzzle for everyone each day (seed-based). |

## Economy

- **Coins** earned per pop (scaled by level multiplier) plus milestone rewards at each level-up: `25 + (level-1)×10`
- **Skins** unlockable at coin thresholds (Common → Rare → Epic → Legendary)
- **Themes** unlockable at higher thresholds
- All persisted via DataStore

## Level Completion & Progression

- `CheckLevelCompleteUseCase`: a level completes only when `bubblesSpawnedSoFar >= totalBubblesToSpawn` AND the board is empty. An empty board alone is NOT complete (prevents instant-skip at level start).
- `CanSpawnBubbleUseCase`: Adventure spawns against the fixed level budget (board occupancy must NOT gate spawning, or levels become uncompletable); Zen/Daily refill endlessly up to the board cap.
- Unlock tracking: clearing level N persists `highestLevel = N+1` via `ScoreRepository`. Replays of cleared levels never re-trigger unlocks.
- Star rating: `starsForTimeFraction` — ≥50% time left = ★★★, ≥25% = ★★, else ★.
- Interstitial ads between levels: every 3rd (1–10), odd (11–19), every (21+); gated by Remove Ads IAP and daily cap.
