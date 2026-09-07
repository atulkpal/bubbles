# Changelog

All notable changes to Bubbles are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

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
