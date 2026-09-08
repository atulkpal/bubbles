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

## Current status
- **DONE:** Steps 1–5 complete. All screens redesigned, glass system, fonts, haptics, staggered animations, light-aware bubbles, custom icon.
- **DONE:** Spawn balance + ghost containment + bubble tutorial (2026-09-08). Level completion fixed (spawn budget gate), achievement choreography on level clear, continue + level select.
- **TODO:** Settings screen polish — upgrade buttons restyled to glass pills, but overall settings layout/spacing/visual hierarchy still needs refinement. Skin/theme card selection states could be improved. Close button styling needs work.

## Acceptance criteria
- Zero emojis as structural icons
- LuxuryTokens drives every color/spacing/radius
- Cormorant + Montserrat loaded & used
- Glass surfaces everywhere; gold accent consistent
- Spring animations + haptics on interactions
- Reduced-motion respected; touch targets >= 48dp; contrast >= 4.5:1
- Gameplay logic untouched (phases 1-4 behaviors preserved)
