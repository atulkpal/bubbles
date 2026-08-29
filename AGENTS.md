# Authoritative Agent Context & Handover (`AGENTS.md`)

Welcome, Agent! This document is your single source of truth for understanding the **Bubbles** project from Ashwath AI. Reading this file gives you full context of the project architecture, features, current progress, and next steps without needing to sift through thousands of lines of code.

---

## 1. Project High-Level Overview

**Bubbles** is an Android bubble-popping stress buster game with a premium "Midnight Glass" design system — liquid glass UI, gold accents, spring animations, and haptic feedback.

*   **Key Philosophy:** A relaxing yet engaging bubble-popper with realistic game mechanics, a complete economy system (coins, skins, themes), and a luxury aesthetic inspired by Apple/Google design language.
*   **Company & Brand:** Developed by **Ashwath AI** (Package ID: `com.ashwathai.bubbles`).
*   **Aesthetic Theme:** Premium **"Midnight Glass"** — liquid glass surfaces, gold accents (#A16207 / #F0B429), Cormorant serif + Montserrat sans typography, spring physics on all interactions.

---

## 2. Directory Structure & Key Code Entry Points

```
├── AGENTS.md                                # This authoritative instruction file
├── README.md                                # Project overview and quick start
├── LUXURY_DESIGN_PLAN.md                    # Detailed design system specification
├── docs/                                    # Repository documentation
│   ├── architecture.md                      # Tech stack, design patterns, data flow
│   ├── setup.md                             # Development environment setup guide
│   ├── contributing.md                      # Contribution guidelines
│   └── changelog.md                         # Version history
├── .github/workflows/
│   └── deploy-website.yml               # GitHub Actions: auto-deploy website to Pages
├── website/                                 # GitHub Pages site source (privacy, about, data deletion)
│   ├── index.html
│   ├── about.html
│   ├── privacy.html
│   ├── data-deletion.html
│   └── style.css
├── scripts/                                 # Play Store automation
│   ├── upload-to-internal-testing.mjs       # AAB upload to internal testing
│   ├── upload-screenshots.mjs               # Listing screenshot upload
│   ├── promote-to-closed-testing.mjs        # Promote from internal to closed testing
│   ├── update-play-listing.mjs              # Update store listing
│   └── update-release-notes.mjs             # Update release notes
├── play-listing-assets/                     # Phone mockup screenshots for Play Store
├── skills/                                  # Agent skills (reusable instruction sets)
│   ├── spec-driven-development/
│   ├── incremental-implementation/
│   ├── test-driven-development/
│   ├── debugging-and-error-recovery/
│   ├── code-review-and-quality/
│   ├── frontend-ui-engineering/
│   └── ... (24 skills total)
├── app/                                     # Android Application Module
│   ├── src/main/java/com/ashwathai/bubbles/
│   │   ├── BubblesApplication.kt           # Application entry (LevelPlay SDK init)
│   │   ├── MainActivity.kt                 # Single activity host (all Compose screens)
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── ScoreRepositoryImpl.kt   # High scores via DataStore
│   │   │   │   ├── EconomyRepositoryImpl.kt # Coins, skins, themes persistence
│   │   │   │   └── SettingsRepositoryImpl.kt# Sound, haptics, reduced-motion prefs
│   │   │   └── sound/
│   │   │       └── SoundManager.kt          # SoundPool-based sound effects + haptics
│   │   ├── di/
│   │   │   └── AppModule.kt                 # Manual dependency injection (factory pattern)
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── GameModels.kt            # All data classes (Bubble, GameState, etc.)
│   │   │   ├── repository/
│   │   │   │   ├── ScoreRepository.kt       # Score persistence interface
│   │   │   │   ├── EconomyRepository.kt     # Economy persistence interface
│   │   │   │   └── SettingsRepository.kt    # Settings persistence interface
│   │   │   └── usecase/
│   │   │       └── GameUseCases.kt          # All use cases (spawn, update, tap, etc.)
│   │   └── ui/
│   │       ├── game/
│   │       │   └── GameViewModel.kt         # Main game logic + state management
│   │       └── theme/
│   │           ├── Theme.kt                 # Material 3 theme
│   │           └── luxury/                  # Premium design system
│   │               ├── LuxuryTokens.kt      # Colors, spacing, radius, glass opacities
│   │               ├── LuxuryTypography.kt  # Cormorant + Montserrat text styles
│   │               ├── LuxuryMotion.kt      # Spring presets, duration constants
│   │               ├── LuxuryIcons.kt       # Vector Path icons (bomb, prism, ice, crown)
│   │               ├── LuxuryComponents.kt  # Buttons, toggles, stats, timer ring
│   │               └── GlassSurface.kt      # Core glass card component
```

---

## 3. What Has Been Done

### Core App (v1.0)
1. **Bubble-Popping Game Engine:** Full game loop with spawn, update, tap detection, collision, and pop mechanics.
2. **Three Game Modes:** Adventure (timed, level-based), Zen (endless), Daily Challenge (seed-based).
3. **Economy System:** Coin earning, skin/theme unlocking, power-up upgrades, prestige system.
4. **Sound & Haptics:** SoundPool-based effects (pop, big pop, combo, coin, level up, game over) + distinct vibration patterns.
5. **Persistence:** DataStore-based for scores, economy, and settings.

### Luxury Design System
6. **"Midnight Glass" Visual Language:** Glass surfaces with gradient fills, hairline borders, blur effects.
7. **Typography:** Cormorant (serif display) + Montserrat (sans body) loaded from Google Fonts.
8. **Motion:** Spring physics on all interactions, staggered entry animations, reduced-motion support.
9. **Vector Icons:** Bomb, prism, ice crystal, crown, sparkle drawn via Path in `drawBubble()`. UI uses `LuxuryIcons` ImageVectors (Coin, Pause, Lotus, Timer, etc.).
10. **Multi-Layer Bubble Rendering:** 6-layer glass effect (refraction, gradient, lens, specular, highlight, icon).
11. **Gold Accent System:** Consistent gold palette across light/dark themes.

### Screens
12. **Start Screen:** Animated breathing logo, glass mode cards, coin pill, staggered entry.
13. **Game Screen:** Continuous glass HUD strip (score, timer ring, best, level, coins), luxury combo counter.
14. **Pause Overlay:** Glass modal with blurred backdrop.
15. **Level Complete / Game Over:** Gold celebration, elegant serif typography.
16. **Settings Screen:** Sectioned glass cards, premium toggles, skin/theme selectors.

### Play Store & Web
17. **Play Store Scripts:** Automated upload, screenshot management, closed testing promotion.
18. **GitHub Pages Website:** Landing page, privacy policy, about, data deletion — auto-deployed via GitHub Actions workflow (`.github/workflows/deploy-website.yml`). Site live at `https://atulkpal.github.io/bubbles/`.
19. **Repository Documentation:** Architecture, setup guide, contributing guidelines, changelog.

---

## 4. What Needs To Be Done (Backlog)

### High Priority
- [ ] **Settings Screen Polish:** Upgrade buttons restyled to glass pills; overall layout/spacing/visual hierarchy refinement; skin/theme card selection states; close button styling.
- [x] **Release Signing:** Generate proper release keystore (current `release-key.jks` is a placeholder).
- [ ] **ProGuard Rules:** Verify R8/obfuscation doesn't break DataStore or Compose.
- [x] **Ad Integration:** ironSource LevelPlay banner ads are live. Add rewarded + interstitial ads for monetization.
- [x] **Play Store Listing:** Finalize store description, screenshots, and feature graphic.

### Medium Priority
- [ ] **Unit Tests:** Cover GameUseCases, repositories, and GameViewModel logic.
- [ ] **UI Tests:** Critical user flows (start game → pop → level complete → game over).
- [ ] **Performance Profiling:** Frame drops on low-end devices, memory leaks.
- [ ] **Accessibility:** Screen reader support, content descriptions, contrast audit.
- [ ] **Localization:** Hindi + English string resources.

### Low Priority / Future
- [ ] **New Bubble Types:** Additional special bubbles with unique mechanics.
- [ ] **Leaderboards:** Global and friend-based score competition.
- [ ] **Achievements:** Milestone rewards and badges.
- [ ] **KMP Migration:** Kotlin Multiplatform for iOS support.
- [ ] **Time-Travel Mode:** Historical replay of daily challenges.

---

## 5. Architectural & Implementation Strategy

### Clean Architecture + MVVM

```
User Interaction
       ↓
   Compose UI ← observes → GameViewModel (mutableStateOf)
       ↓                         ↓
  Recomposition            UseCases (SpawnBubbles, HandleTap, etc.)
                             ↓
                       Repositories (interfaces in domain/)
                             ↓
                    DataStore (implementations in data/local/)
```

- **State** flows up via `mutableStateOf` in ViewModel (observed by Compose)
- **Events** flow down via ViewModel methods
- **Persistence** via DataStore Preferences
- **DI** via manual `AppModule` object (factory pattern, NOT Hilt)
- **Ads** via ironSource LevelPlay (banner ads only, wired in `BubblesApplication`)

### Design System Architecture

All UI components MUST use the luxury design system:

| Concern | Source |
|---------|--------|
| Colors | `LuxuryTokens.kt` — never hardcode |
| Typography | `LuxuryTypography.kt` — Cormorant + Montserrat |
| Spacing/Radius | `LuxuryTokens.kt` — spacing scale, radius scale |
| Motion | `LuxuryMotion.kt` — spring presets, durations |
| Cards | `GlassSurface.kt` — not plain `Surface` |
| Icons | `LuxuryIcons.kt` — vector paths, not emoji |
| Haptics | `SoundManager.kt` — distinct patterns |

---

## 6. Authoritative Document Iteration Process

> **Agent Workflow:** Before starting any task, agents MUST read [`agent-skills.md`](./agent-skills.md) and invoke the appropriate skill from [`skills/`](./skills/).

To maintain continuous alignment and prevent code-spec drift, all agents and developers must adhere to the following iteration loop:

```
  ┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
  │  Step 1: Select Branch  │ ───> │  Step 2: Update Spec    │ ───> │  Step 3: User Approval  │ ───> │  Step 4: Implement Code │
  │ Checkout feature/fix    │      │ Edit LUXURY_DESIGN_PLAN │      │ Confirm plan in chat    │      │ Write Kotlin & Tests    │
  └─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
```

0.  **Branch Discipline:** You MUST work on the correct branch based on the task type:
    *   **`master` branch**: Stable production code. All PRs merge here.
    *   **`feature/*` branches**: New features (e.g., `feature/leaderboards`).
    *   **`fix/*` branches**: Bug fixes (e.g., `fix/combo-reset`).
    *   **NEVER** implement features directly on `master` without a branch.
1.  **Spec-First Modification:** Before implementing a new feature or modifying existing interfaces, update `LUXURY_DESIGN_PLAN.md` or `docs/architecture.md` with the proposed design.
2.  **Review & Handshake:** Present the spec changes to the user for feedback.
3.  **Surgical Execution:** Once agreed, implement the feature, write corresponding unit/integration tests, and mark the task as complete (`[x]`) in `AGENTS.md`.
4.  **Mandatory Testing Rule:** Always add or update corresponding unit tests whenever new functionality is implemented or modified.
5.  **Background Tasks Rule:** Infinite loops or periodic background tasks must NEVER be placed in a `ViewModel`'s `init` block. Move these to explicit functions called by the Activity.
6.  **Versioning & Release Policy:** NEVER automatically bump version codes or numbers in `app/build.gradle.kts`. Always ask the user for explicit approval before performing a version bump.
7.  **No Dead-Ends:** Never add non-functional UI placeholders. Every visual affordance must connect to an active feature or remain omitted.
8.  **Design System Compliance:** Every new UI component MUST use `LuxuryTokens`, `GlassSurface`, `LuxuryTypography`, and `LuxuryMotion`. No hardcoded colors, fonts, or animations.

---

## 7. Build & Release Policy

1.  **Build Command:** `./gradlew assembleDebug` (debug) or `./gradlew assembleRelease` (release).
2.  **Signing:** Release builds require `app/release-key.jks` with proper credentials in `app/build.gradle.kts`.
3.  **ProGuard:** Enabled for release (`isMinifyEnabled = true`, `isShrinkResources = true`).
4.  **Play Store Upload:** Use `scripts/upload-to-internal-testing.mjs` for AAB upload.
5.  **Versioning Discipline:** Never automatically bump version codes without explicit user approval.
6.  **Archive Builds:** Move artifacts from `app/build/outputs/` to root `releases/` folder before new builds.

---

## 8. Rejected Implementations (Historical Reference)

> **DO NOT USE** — The following implementations were attempted but rejected. Preserved for reference only.

*None yet — this section will be populated as rejected approaches are documented.*

---

## 9. Key Configuration Values

| Config | Value | Location |
|--------|-------|----------|
| Package ID | `com.ashwathai.bubbles` | `app/build.gradle.kts` |
| Min SDK | 26 (Android 8.0) | `app/build.gradle.kts` |
| Target SDK | 36 | `app/build.gradle.kts` |
| Compile SDK | 36 | `app/build.gradle.kts` |
| Kotlin | 2.0.20 | `settings.gradle.kts` |
| Compose BOM | Managed | `gradle/libs.versions.toml` |
| Hilt | 2.52 (classpath only, not used — manual DI via AppModule) | `build.gradle.kts` |
| Ad SDK | ironSource LevelPlay (banners only) | `gradle/libs.versions.toml` |
