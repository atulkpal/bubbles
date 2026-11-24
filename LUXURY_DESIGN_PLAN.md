# Bubbles — Luxury Redesign Plan

## Objective
Transform Bubbles from an indie-feeling casual game into a premium, high-end app in the style of Apple/Google. Eliminate every "indie developer" tell: emoji-as-icons, hardcoded colors, flat circles, basic Material cards, and cheap motion.

## Guiding skills
design, ui-styling, ui-ux-pro-max (search-backed guidance)

## Design Direction: "Midnight Glass" — Liquid Glass + Premium Black/Gold
- Visual language: Liquid Glass (translucency, lensing, refraction, fluid morph)
- Color system: Premium black (#0C0A09 / #1C1917) + gold accent (#A16207 core, #CA8A04, #F0B429, #FCD34D)
- Typography: Cormorant (luxury serif, display) + Montserrat (geometric sans, body/labels)
- Motion: spring physics on all interactions; reduced-motion respected everywhere
- Haptics: distinct patterns for pop / combo / boss-hit / success / level-complete

## Architecture
New `ui/theme/luxury/` package holding the full token + component system:

1. `LuxuryTokens.kt` — color palettes (light + dark), spacing scale, radius scale, elevation, glass opacities, semantic colors
2. `LuxuryTypography.kt` — full text-style scale (Cormorant display + Montserrat body), with bundled font assets fallback
3. `LuxuryMotion.kt` — duration/easing constants + reduced-motion helpers + spring presets
4. `GlassSurface.kt` — the core reusable glass card (gradient fill, hairline top border, shadow, clip)
5. `LuxuryButton.kt` — Primary (gold), Secondary (glass), press-scale spring, optional icon
6. `LuxuryToggle.kt` — premium switch with gold thumb + spring animation
7. `LuxuryStat.kt` — HUD stat (label + value, gold accents)
8. `LuxuryTimerRing.kt` — circular progress ring for adventure timer
9. `LuxurySection.kt` — collapsible settings section header
10. `LuxuryHaptics.kt` — VibrationEffect patterns (replaces inline vibrate calls)

## Game visual upgrades (in MainActivity draw code)
- Glass bubble rendering: refraction ring → main glass gradient → lens layer → specular highlight → secondary highlight → type icon (vector paths, NOT emoji) → power-up ring
- Vector icons drawn via Path: bomb, prism (rainbow), ice crystal (frozen, with crack state), crown (boss), sparkle
- Particle system: gold-dust specks, caustic rings (expanding circle stroke), prism shards, ice crystals, crown sparkles
- Combo counter: spring scale + gold shimmer; shake stays trauma-based

## Screen redesigns
- StartScreen: animated breathing logo orb, "BUBBLES" in Cormorant with gold gradient, glass mode cards (Adventure / Zen / Daily) with vector icons, coin pill, staggered entry
- GameScreen: continuous glass HUD strip (score / timer ring / best / level / coins), luxury combo, luxury pause button
- PauseOverlay: glass modal with blurred backdrop
- LevelComplete / GameOver: gold celebration, elegant serif typography, premium cards
- SettingsScreen: sectioned glass cards, premium toggles, upgrade rings, skin/theme selectors with vector previews

## Anti-patterns to eliminate
| Current | Luxury replacement |
|---|---|
| Emoji icons (🫧🧘⏸💣🌈🧊👑) | Vector Path icons |
| Color.Black.copy(alpha=0.35f) cards | GlassSurface |
| OutlinedButton + emoji | LuxuryButton |
| Linear progress bar | LuxuryTimerRing |
| Flat drawCircle bubbles | Multi-layer glass render |
| Hardcoded colors | LuxuryTokens |
| FontWeight.ExtraBold | LuxuryTypography scale |
| Inline vibrate() | LuxuryHaptics |

## Execution order
1. Design system (tokens, typography, motion, glass) — new files, no behavior change
2. Core components (button, toggle, stat, ring, section, haptics)
3. Game visuals (bubble renderer, particles, vector icons, combo)
4. Screen redesigns (start, game HUD, settings, overlays)
5. Wire haptics + motion + reduced-motion

## Spawn Balance & Bubble Tutorial (spec — 2026-09-08)

### Problem
Since the 1.0.3 special-types work, `spawnLargeBubble()` has no NORMAL weight in its
roll table — every spawned bubble is a special type. Players see only icon bubbles
from level 1 and cannot learn the base game. Additionally, ghost bubbles split into
3 ghosts at half radius with no depth limit, producing unhittable sub-minimum specks.

### Workstream A — Spawn balance
1. NORMAL becomes the dominant outcome; specials draw from a special budget that
   ramps `0.10 → 0.55` from level 1 → 25, capped at `0.65` after.
2. Each special type unlocks at a level and ramps in over ~4 levels:
   BOMB@3, FROZEN@5, RAINBOW@7, MAGNET@9, TICKING_BOMB@12, CHAOS@15, GHOST@18.
   Pre-unlock, a type's weight is 0.
3. Zen & Daily (level 0) use a fixed gentle mix: ~75% NORMAL, at most BOMB/FROZEN/RAINBOW.

### Workstream B — Ghost containment
1. Split children radius floored at `config.minRadius` (40px) — never unhittable.
2. Split only once: children spawn as `splitGeneration = 1`; ghosts with
   `splitGeneration >= 1` never re-split. Max 3 ghosts per ghost, recursion dead.

### Workstream C — Bubble introduction tutorial
1. **First-appearance label:** the first time a special type ever spawns for the
   player, a small floating tag near the bubble: "NEW — BOMB" (non-blocking, fades).
2. **First-burst detail card:** on popping that type for the first time, a brief
   full pause (~0.6s) with a GlassSurface card: type name, icon, one-line mechanic.
   Tap to dismiss, auto-dismiss after 3s. Timer and bubbles freeze during the card.
   Respects reduced-motion (no spring bounce).
3. **Persistence:** `seenBubbleTypes: Set<String>` in `EconomyState` via DataStore —
   one-time per install, not per level.
4. All UI via LuxuryTokens / GlassSurface / LuxuryTypography / LuxuryMotion.

### Acceptance
- Level 1: majority NORMAL bubbles, at most BOMB+FROZEN slowly ramping.
- No spawn can produce a bubble below `minRadius`.
- Every special type teaches itself exactly once, on first spawn + first burst.

## Level Completion, Stars & Level Select (spec — 2026-09-08)

### Problem
The instant-skip fix (level completes only when all `maxBubbles` spawned AND board clear)
was paired with the old board-occupancy spawn gate (`bubbles.size < maxBubbles`). Every pop
freed a slot, the level refilled itself, and the board could never empty after the budget
was spent — level 3 (first boss level) never ended. Separately, the level-clear moment felt
flat (no transition, no sense of achievement), and players could only ever resume Adventure
from level 1.

### Workstream A — Spawn budget gate
1. New `CanSpawnBubbleUseCase`: Adventure spawns against the fixed level budget
   (`bubblesSpawnedSoFar < maxBubbles`, board cap still applied); Zen/Daily refill
   endlessly up to the board cap.
2. `continueGame()` / `continueGameWithTimeBonus()` reset `bubblesSpawnedThisLevel`
   (a stale counter after Continue would instantly re-complete the level).
3. Board-occupancy spawn gating is a REJECTED pattern (see AGENTS.md).

### Workstream B — Achievement moment (LevelCompleteScreen)
1. Glass card springs in with gentle overshoot (no flat fade).
2. 3-star rating from remaining time: ≥50% ★★★, ≥25% ★★, else ★ — earned stars pop in
   sequentially via `starsForTimeFraction` + keyframe spring.
3. Score counts up from 0 (900ms ease-out).
4. Gold "LEVEL N UNLOCKED" chip (`GlassSurface` pill) when a new level unlocks.
5. Reduced-motion: static equivalent, no springs.

### Workstream C — Continue & level select
1. `ScoreRepository.highestLevel` persisted (DataStore); level 1 always playable.
2. StartScreen: "CONTINUE · LEVEL N" primary button + "SELECT LEVEL" expander with a
   horizontally scrollable chip row — cleared levels + next level (gold ★) playable.
3. `GameViewModel.startGame(level)` replays any cleared level from a clean slate;
   replays never re-trigger unlock celebrations or raise `highestLevel`.

### Acceptance
- Adventure levels always completable; Zen/Daily unchanged.
- Level clear feels earned: motion, stars, count-up, unlock chip.
- Returning players resume at their frontier level or replay any cleared level.

## Settings Screen Polish & Achievements (spec — 2026-09-12)

### Problem
Settings works but the visual hierarchy is uneven: upgrade rows mix a coin-cost pill with plain text details and show current (not next) effect values; skin/theme cards signal selection with a flat background swap only; the close affordance is duplicated (top-right icon chip + bottom CLOSE button); and the whole settings subtree lives inline in a 2,500-line MainActivity. Separately, the economy already tracks lifetime pops, best combo, games played, prestige, skins and themes — but none of it is celebrated. Achievements surface that data as milestone badges.

### Workstream A — Settings screen polish (no behavior change)
1. **Extract first:** move the settings subtree verbatim from `MainActivity.kt` into `ui/settings/SettingsScreen.kt` (with `UpgradeRow`, `SkinCard`, `ThemeCard`, `StatRow`, `AboutLinkChip` co-located). Pure move — zero visual change.
2. **Upgrade rows:** cost pill becomes a `GlassSurface` pill — gold hairline when affordable, dimmed when not, "MAX" in gold outline when maxed. Detail line shows next-level effect: "15s → 17s".
3. **Skin/theme cards:** selected = gold hairline ring + small gold check chip; locked = dimmed + lock overlay; press feedback via `LuxuryMotion` press-scale spring.
4. **Close:** keep the top-right glass icon chip; remove the duplicate bottom CLOSE button.
5. **Rhythm & a11y:** consistent section spacing (`MD` between sections, `SM` inside cards); content descriptions on all interactive icons; touch targets ≥ 48dp.

### Workstream B — Achievements (local milestone badges, no leaderboards)
1. **Catalog:** 12 achievements in 4 families (domain/model, data-only):
   - Pops: 100 / 1,000 / 10,000 lifetime pops
   - Combo: 10x / 25x / 50x best combo
   - Levels: clear level 5 / 20 / 50
   - Collection: own 3 skins · own all themes · reach Prestige 1
2. **State:** `EconomyState.unlockedAchievements: Set<String>` (DataStore, default empty — backward compatible with existing installs).
3. **Engine:** pure `AchievementEngine.evaluate(EconomyState): Set<String>` returns newly unlocked ids given prior state. `GameViewModel` calls it after each economy mutation (pop batch, combo update, level clear, skin/theme purchase, prestige). First unlock only, exactly-once.
4. **Reward & moment:** 25 coins per achievement; gold GlassSurface unlock chip (spring entrance, static fade under reduced-motion); SoundManager level-up sound + success haptic pattern.
5. **Settings section:** "Achievements" above Stats — 12 glass chips; unlocked = gold icon + serif name; locked = dimmed with "?" name (simple, no progress bars).
6. No new screens, no network, no leaderboard. Everything local via DataStore.

### Acceptance
- Settings hierarchy clear, selection states unmistakable, single close affordance, settings code extracted out of MainActivity, no regressions elsewhere.
- Achievement unlock is exactly-once, persists across restarts, works offline.
- Unit tests: `AchievementEngineTest` (all 12 + no-unlock + idempotency), `EconomyRepositoryImpl` persistence of `unlockedAchievements`.
- UI tests: settings opens → toggles sound → closes; achievements section renders an unlocked chip with seeded state.

## Current status
- **DONE (2026-09-12):** Settings polish + Achievements spec implemented on `feature/backlog-completion`. Settings extracted to `ui/settings/SettingsScreen.kt`; achievements (12 badges, `AchievementEngine`, DataStore persistence, unlock chip, Settings section); ProGuard verified (release APK runs on Pixel emulator, zero crashes); 8 Compose UI tests green; a11y pass done. Emulator perf baseline recorded in changelog.
- **DONE:** Steps 1–5 complete. All screens redesigned, glass system, fonts, haptics, staggered animations, light-aware bubbles, custom icon.
- **DONE:** Spawn balance + ghost containment + bubble tutorial (2026-09-08). Level completion fixed (spawn budget gate), achievement choreography on level clear, continue + level select.

## Acceptance criteria
- Zero emojis as structural icons
- LuxuryTokens drives every color/spacing/radius
- Cormorant + Montserrat loaded & used
- Glass surfaces everywhere; gold accent consistent
- Spring animations + haptics on interactions
- Reduced-motion respected; touch targets >= 48dp; contrast >= 4.5:1
- Gameplay logic untouched (phases 1-4 behaviors preserved)

## Audio System (spec - 2026-10-07)

### Problem
All 5 files in `app/src/main/res/raw/` are 0-byte placeholders, so `SoundManager`
loads successfully but plays silence. There is no background music at all.

### Asset sources (all CC0, no attribution required)
- SFX pops/cracks: Kenney Impact Sounds (`impactGlass_light/heavy/medium`)
- Power-up/combo/boss: Kenney Digital Audio (`powerUp3`, `phaserUp2`, `lowDown`)
- Coin: Kenney RPG Audio (`handleCoins`); UI tap: Kenney UI Pack (`tap-a`)
- Win/lose stings: Kenney Music Jingles, selected by spectral-centroid slope
  analysis (strongest ascending = win `NES00`, strongest descending = lose `PIZZI01`)
- BGM: 2 synthesized seamless ambient pad loops (integer-cycle partials, no clicks):
  `music_adventure.wav` (A-major, 32s, menu/Adventure/Daily),
  `music_zen.wav` (D-major, 40s, Zen). Generated by `gen_bgm.py`, owned by the studio.

### Design tokens (`AudioConfig`, domain layer, pure/testable)
- `DEFAULT_SFX_VOLUME = 1.0f`, `DEFAULT_MUSIC_VOLUME = 0.35f`
- `CROSSFADE_MS = 1500L` — every track switch crossfades, never hard-cuts
- `trackForMode(isZen)` — Zen -> zen loop, everything else -> adventure loop
- Volumes coerced to [0f, 1f]

### Architecture
- `SoundManager` (SoundPool, SFX only): distinct raw per event
  (`pop1/2/3`, `bigpop`, `crack`, `split`, `powerup`, `combo`, `coin`, `click`,
  `levelup`, `gameover`, `boss`) + `setSfxVolume` master multiplier.
- `MusicManager` (NEW, dual-MediaPlayer): gapless `setLooping` playback,
  coroutine crossfade between tracks, `play / pause / resume / stop / release`.
- `SettingsRepository` gains `musicEnabled` (default true),
  `sfxVolume` (default 1.0), `musicVolume` (default 0.35), DataStore-persisted.
- `GameViewModel` drives music per game state (menu/adventure/zen, pause/resume);
  `MainActivity` pauses on app background, releases on destroy.
- Settings "AUDIO" section: Sound toggle, Music toggle, SFX slider, Music slider
  (`LuxurySlider`, gold, >= 48dp touch target, content descriptions).

### Acceptance
- Tapping a bubble plays a crisp varied pop; every game event has a distinct sound.
- Ambient music plays per mode and crossfades on mode switch; sliders change
  volumes live; toggles + volumes persist across restarts.
- Unit tests: `AudioConfigTest` (defaults, clamping, track mapping),
  `SettingsRepositoryImplAudioTest` (persistence round-trip).
- `./gradlew assembleDebug` + `testDebugUnitTest` green.
