# Changelog

All notable changes to Bubbles are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Repository documentation (architecture, setup, contributing, changelog)
- GitHub Pages website with privacy policy, about, and data deletion pages

### Changed
- Documentation moved from `docs/` to `website/` for GitHub Pages

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

---

## [0.1.0] — 2026-08-20

### Added
- Initial project setup
- Basic bubble-popping gameplay
- Jetpack Compose UI
- Hilt dependency injection
