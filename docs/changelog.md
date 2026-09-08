# Changelog

All notable changes to Bubbles are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

## [1.2] — 2026-09-08

### Changed
- **Google Play Billing 7.1.1 → 8.0.0** — required by Play Console for new uploads (deadline Aug 31, 2026). `queryProductDetailsAsync` callback migrated to the PBL 8 `QueryProductDetailsResult` signature; `enablePendingPurchases(PendingPurchasesParams)` usage already compliant.

### Added
- **About section in Settings** — app version, "© Ashwath AI" owner credit, and link chips to the website (About / Privacy / Data Deletion).

### Fixed

#### Critical: Level 3 never completed (spawn budget not enforced)
- The new completion rule (all assigned bubbles spawned AND board clear) was paired with the OLD spawn gate (`bubbles.size < maxBubbles`): every pop freed a slot, the level refilled itself, and the board could never be empty after the budget was spent — levels 1–2 only completed by luck, level 3 (boss + 8 spawns) never did.
- Extracted the gate into `CanSpawnBubbleUseCase`: Adventure mode spawns against the fixed level budget (`bubblesSpawnedThisLevel < maxBubbles`) with the board cap still applied; Zen/Daily keep endless refill up to the board cap. Wired via `AppModule`.
- `continueGame()` / `continueGameWithTimeBonus()` now reset `bubblesSpawnedThisLevel` (stale counter previously survived a continue — a cleared board would trigger instant level-complete, reopening the skip bug through another door).
- Unit tests: `CanSpawnBubbleUseCaseTest` (6 cases — adventure budget/cap/overshoot, endless cap, regression case for empty-board refill).

#### Critical: Instant level-skip loop
- **Levels auto-completed back-to-back with no gameplay** — `CheckLevelCompleteUseCase` declared a level complete whenever the board was empty, but every level *starts* with an empty board (bubbles spawn on a timer). The first game-loop frame of each level triggered instant "CLEAR!", chaining endlessly through levels.
- `CheckLevelCompleteUseCase.invoke()` now takes `totalBubblesToSpawn` + `bubblesSpawnedSoFar` and only completes a level once every assigned bubble has spawned AND the board is clear.
- `GameViewModel` tracks `bubblesSpawnedThisLevel`, incremented per spawn batch and reset in `beginSession()` / `startNextLevelConfirmed()`.

#### Continue-after-game-over lost level progress
- `GameState.GameOver` now carries `currentLevel` (recorded in `gameOver()`).
- `continueGame()` / `continueGameWithTimeBonus()` resume from the level the player died on instead of hardcoded level 1.

#### Ad pacing math (levels 11–20 and 21+)
- `EconomyConfig.shouldShowAdBetweenLevels()`: `(completedLevel - 10) % 2 == 1` was true for *every* level 11–20, and `(completedLevel - 20) % 1 == 1` was never true — so no ads fired at all past level 21. Now: every 3rd level (1–10), odd levels 11–19, every level 21+.

#### Daily Challenge showed dead Continue buttons
- Game-over screen showed "CONTINUE · WATCH AD" / "CONTINUE · 50 COINS" for daily runs, but the ViewModel silently rejects continues for daily games. Both buttons (and the coin check) now gate on `!daily`.

#### Interstitial ad fired during composition
- The `level_interstitial` rewarded-ad call ran directly in the `GameState.LevelComplete` branch, re-firing on every recomposition. Now wrapped in `LaunchedEffect(completedLevel)`; daily ad cap reached skips straight to the next level.

#### Duplicate banners
- `PauseOverlay` and `LevelCompleteScreen` each rendered their own `LevelPlayBanner` stacked on the root one in `BubbleScreen`. Both removed — the root banner covers all states.

#### Deprecations
- Replaced 2 deprecated `quadraticBezierTo` calls with `quadraticTo` (ghost icon path).

### Added
- **Achievement moment on level clear** — `LevelCompleteScreen` rebuilt: the glass card springs in with a gentle overshoot, a 3-star rating derived from remaining time (≥50% ★★★, ≥25% ★★, else ★) pops in star-by-star, the score counts up from 0, and a gold "LEVEL N UNLOCKED" chip lands when a new level unlocks. Reduced-motion setting shows the static version.
- **Continue & level select** — the highest cleared level now persists (`ScoreRepository.highestLevel`, DataStore). The start screen shows "CONTINUE · LEVEL N" and a "SELECT LEVEL" expander with a horizontally scrollable chip row: every cleared level plus the next one (marked gold with a star) are playable; anything beyond stays locked.
- `GameViewModel.startGame(level)` starts Adventure at any previously cleared level; `GameViewModel` exposes `highestLevel`, `levelUnlockedThisRun` and `lastLevelTimeFraction` for the UI.
- Unit tests: `StarsForTimeFractionTest` (4 cases — star thresholds, clamping).
- Unit tests: `CheckLevelCompleteUseCaseTest` (5 cases — empty board at level start, partial spawn, fully spawned + cleared, bubbles remain, spawn overshoot)
- Unit tests: `EconomyConfigTest` (4 cases — ad gating bands 1–10 / 11–20 / 21+, milestone reward scaling)

- **In-App Purchase: Remove Ads** — one-time purchase to disable all ads (banner, interstitial, rewarded). Rewarded ad rewards grant instantly without watching.
- Google Play Billing Library 7.1.1 integration (`BillingManager` singleton)
- `adsRemoved` preference persisted via DataStore
- "Remove Ads" section in Settings screen with premium styling
- Tagged `ad-supported-v1.1` release point before IAP work

### Changed
- `LevelPlayAdManager`: gates all ad displays on `BillingManager.adsRemoved`
- `LevelPlayBanner`: hides entirely when ads are removed
- `LevelPlayAdManager.loadAndShowRewardedAd`: grants reward immediately when ads removed (no ad shown)
- `SettingsRepository`: added `adsRemoved` / `setAdsRemoved()`
- `BubblesApplication`: initializes `BillingManager` on startup

### Added
- Repository documentation (architecture, setup, contributing, changelog)
- GitHub Pages website with privacy policy, about, and data deletion pages
- GitHub Actions workflow for automatic website deployment to GitHub Pages
- `.nojekyll` file to prevent Jekyll processing on GitHub Pages

### Changed
- Website source lives in `website/` directory, deployed via GitHub Actions (not branch-based)
- Floating bubbles moved from hero-only to full-page across all website pages
- Floating bubbles use `position: fixed` with radial-gradient edge fade mask

---

## [1.0.3] — 2026-08-29

### Added

#### Special Bubble Types
- **Magnet** — pulls nearby bubbles toward it on tap; no chain propagation
- **Ticking Bomb** — auto-explodes after 1.5s (180px blast radius); can be popped early
- **Chaos** — reverses + amplifies velocity of all bubbles within 3× radius on pop
- **Ghost** — 1-health, phaser visual, chains exactly 1 bubble then stops (no propagation)

#### Procedural Level Generator
- `generateProceduralLevel(level)` scales difficulty from level 21 to 100 with:
  - Logarithmic + linear blended difficulty curve
  - Max bubbles: 26 → 55 over the range
  - Spawn interval: 0.25s → 0.08s
  - Base speed: 980 → 1400
  - Power-up chance: 15% → 35%
  - Wind: appears at level 21+, grows with stage bands, oscillates by level parity
  - Gravity: appears at level 31+, grows with stage bands
  - Boss every 3rd level starting at 21
  - Time limit: 19s → 8s

#### Burst Particles (on bubble pop)
- Armored star, snowflake, diamond, hexagon, diamond-star variants in addition to existing circular particles
- Type-specific burst: colorful sparkles for rainbow, explosion particles for bomb, ghost phaser particles for ghost

#### HUD / Visuals
- 24-particle gold burst at screen center on level clear (`completeLevel()`)
- "N levels cleared" on level-complete screen (no cap — procedural generation means infinite progression)
- Start screen copy updated to "100 levels, 4 special bubble types, 5 particle variants"

#### Economy / Monetization
- Milestone rewards: `25 + (level - 1) × 10` coins at each level-up (e.g., level 3 = 45, level 10 = 115)
- Interstitial ad schedule: every 3rd level in 1–10, every 2nd in 11–20, every level from 21+
- Coin-based continue on Game Over: 50 coins → +15s time bonus (no ad required)
- LevelCompleteScreen shows "spend on upgrades" hint for milestone coins

#### Progression Copy
- StartScreen: "Clear each level before time runs out. Difficulty grows with every level — speed, count, wind, gravity. Coins from pops & milestones buy power-up upgrades."

### Changed

#### Gameplay Mechanics
- Spawner now respects `spawnInterval` — spawns 1 bubble per tick (not 2), stops at `maxBubbles`
- Level ends when screen is empty (original bubbles + any child bubbles from chain pops)
- Chain logic excludes new special types (MAGNET, TICKING_BOMB, CHAOS, GHOST) from chain expansion; RAINBOW still chains with them
- Only NORMAL bubbles (level 2+) split into 2 child bubbles on pop; special types do not
- Score: special bubbles worth `(level + 1) × 15` instead of `(level + 1) × 10`

#### Bug Fixes
- Levels now properly progress (spawner no longer refills faster than pop rate)
- HUD "Bubbles left" no longer goes up from spawner — only goes up from chain children, which is intentional and visible
- LevelCompleteScreen no longer says "Level mastered!" (hard cap removed)

---

## [1.0.2] — 2026-08-29

### Added
- **In-App Purchase: Remove Ads** — one-time purchase to disable all ads (banner, interstitial, rewarded). Rewarded ad rewards grant instantly without watching.
- Google Play Billing Library 7.1.1 integration (`BillingManager` singleton)
- `adsRemoved` preference persisted via DataStore
- "Remove Ads" section in Settings screen with premium styling
- Tagged `ad-supported-v1.1` release point before IAP work

### Changed
- `LevelPlayAdManager`: gates all ad displays on `BillingManager.adsRemoved`
- `LevelPlayBanner`: hides entirely when ads are removed
- `LevelPlayAdManager.loadAndShowRewardedAd`: grants reward immediately when ads removed (no ad shown)
- `SettingsRepository`: added `adsRemoved` / `setAdsRemoved()`
- `BubblesApplication`: initializes `BillingManager` on startup

---

## [1.0.1] — 2026-08-29

### Added
- Repository documentation (architecture, setup, contributing, changelog)
- GitHub Pages website with privacy policy, about, and data deletion pages
- GitHub Actions workflow for automatic website deployment to GitHub Pages
- `.nojekyll` file to prevent Jekyll processing on GitHub Pages

### Changed
- Website source lives in `website/` directory, deployed via GitHub Actions (not branch-based)
- Floating bubbles moved from hero-only to full-page across all website pages
- Floating bubbles use `position: fixed` with radial-gradient edge fade mask

---

## [1.0.0] — 2026-08-28

### Added

#### Core App
- Bubble-popping stress buster game
- Three game modes: Adventure, Zen, Daily Challenge
- Coin economy with earn/spend mechanics
- Sound effects via SoundPool
- Haptic feedback for pop, combo, and success events

#### Luxury Design System
- "Midnight Glass" visual language
- Glass surface component with gradient, blur, and refraction
- Premium typography: Cormorant (display) + Montserrat (body)
- Spring-based motion system with reduced-motion support
- Vector icons replacing emoji (bomb, prism, ice, crown, sparkle)
- Multi-layer glass bubble rendering (6 visual layers)
- Gold accent color system with light/dark palettes
- Staggered entry animations for start screen

#### Game Features
- Boss bubbles every 10 levels (Adventure mode)
- Combo counter with spring scale animation
- Level progression with increasing difficulty
- Daily challenge with seed-based puzzles
- Pause overlay with glass modal

#### Skins & Themes
- Unlockable bubble skins (Common → Legendary)
- Unlockable background themes
- Coin-based economy for purchases

#### Data & Persistence
- DataStore-based preferences for settings
- Score persistence (high scores per mode)
- Economy persistence (coins, owned items)

#### Play Store
- Automated upload scripts (internal testing)
- Screenshot upload automation
- Release notes management
- Closed testing promotion scripts

#### Website
- GitHub Pages landing page
- Privacy policy page
- About page
- Data deletion instructions
- Floating decorative bubbles across all pages with smooth edge fade-out

---

## [0.1.0] — 2026-08-20

### Added
- Initial project setup
- Basic bubble-popping gameplay
- Jetpack Compose UI
- Hilt dependency injection
