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
6. Build, install on emulator, verify no crash, iterate

## Current status
- **DONE:** Steps 1–5 complete. All screens redesigned, glass system, fonts, haptics, staggered animations, light-aware bubbles, custom icon.
- **TODO:** Settings screen polish — upgrade buttons restyled to glass pills, but overall settings layout/spacing/visual hierarchy still needs refinement. Skin/theme card selection states could be improved. Close button styling needs work.

## Acceptance criteria
- Zero emojis as structural icons
- LuxuryTokens drives every color/spacing/radius
- Cormorant + Montserrat loaded & used
- Glass surfaces everywhere; gold accent consistent
- Spring animations + haptics on interactions
- Reduced-motion respected; touch targets >= 48dp; contrast >= 4.5:1
- Gameplay logic untouched (phases 1-4 behaviors preserved)
