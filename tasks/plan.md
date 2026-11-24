# Implementation Plan: Backlog Completion (Settings Polish + Achievements + Verification)

## Overview

Complete the Bubbles backlog with the scope agreed on 2026-09-12: Settings screen polish,
a local Achievements feature (leaderboards cut — keep it simple), ProGuard/R8 verification,
Compose UI tests, an accessibility pass, and performance verification on a Pixel emulator.
Localization was explicitly skipped (English hardcoded strings stay).

Spec: `LUXURY_DESIGN_PLAN.md` → "Settings Screen Polish & Achievements (spec — 2026-09-12)".

## Architecture Decisions

- **Achievements are domain data, not a new layer.** A pure `AchievementEngine` in
  `domain/model/` evaluates `EconomyState` → newly unlocked achievement ids. No network,
  no new repository interface beyond persisting `unlockedAchievements: Set<String>` in
  `EconomyState` via the existing `EconomyRepositoryImpl` DataStore.
- **Settings extraction is a pure move first.** Copy the settings composables verbatim from
  `MainActivity.kt` into `ui/settings/SettingsScreen.kt`, verify build + tests, then restyle.
  Two small diffs are far easier to review and bisect than one giant one.
- **Design-system compliance everywhere.** All new UI uses `LuxuryTokens`, `GlassSurface`,
  `LuxuryTypography`, `LuxuryMotion`, `LuxuryIcons`; haptics via `SoundManager`.
- **Rewards and unlock moment reuse existing patterns.** Achievement coin reward uses the
  existing `addCoins` path; the unlock chip reuses the glass-chip + spring entrance pattern
  from LevelCompleteScreen; reduced-motion gets a static fade.

## Task List

### Phase 1: Achievements (foundation → feature)
- **Task 1 (S): Achievement catalog + pure engine + tests**
  - `Achievements.kt` in `domain/model/`: 12 ids across 4 families
    (pops 100/1k/10k, combo 10/25/50, levels 5/20/50, collection skins-3/all-themes/prestige-1)
    with `id`, `title`, `description`, `rewardCoins = 25`, icon reference.
  - `AchievementEngine.evaluate(economy: EconomyState): Set<String>` — pure, returns only
    *newly* unlocked ids given `economy.unlockedAchievements`.
  - Unit tests `AchievementEngineTest`: every achievement individually, no-unlock case,
    idempotency (already-unlocked never re-returned).
  - Files: `GameModels.kt` (or new file), new test file.
- **Task 2 (S): Persist `unlockedAchievements`**
  - Add field to `EconomyState` (default `emptySet()`), key in `EconomyRepositoryImpl`,
    `unlockAchievements(ids)` suspend on interface + impl.
  - Unit test: persistence round-trip.
  - Files: `GameModels.kt`, `EconomyRepository.kt`, `EconomyRepositoryImpl.kt`.
- **Task 3 (M): Wire GameViewModel + unlock moment**
  - Evaluate after each economy mutation (pop batch, combo, level clear, purchase, prestige).
  - Grant 25 coins/achievement; emit one-shot unlock event; gold glass chip with spring
    entrance (static fade under reduced-motion); SoundManager level-up sound + haptic.
  - Files: `GameViewModel.kt`, `MainActivity.kt` (chip composable), SoundManager if a new
    pattern is needed.
- **Task 4 (S): Achievements section in Settings**
  - Section above Stats; 12 glass chips — unlocked = gold icon + serif name, locked = dimmed
    with "?" name.
  - Files: settings subtree (see Task 5 location).

### Phase 2: Settings polish
- **Task 5 (S): Extract settings subtree verbatim**
  - Move `SettingsScreen`, `UpgradeRow`, `SkinCard`, `ThemeCard`, `StatRow`, `AboutLinkChip`
    into `ui/settings/SettingsScreen.kt`. Zero visual change; build + existing tests pass.
- **Task 6 (M): Restyle within extracted file**
  - Upgrade cost pill → glass pill (gold hairline affordable / dimmed / gold-outline MAX);
    detail shows next-level effect ("15s → 17s"); skin/theme selection = gold hairline ring +
    check chip + press-scale spring; remove bottom CLOSE button; spacing rhythm MD/SM;
    content descriptions; ≥48dp targets.
- **Checkpoint:** build + unit tests green; manual emulator pass on Settings.

### Phase 3: Verification & hardening
- **Task 7 (M): ProGuard/R8 verification**
  - Review keep rules vs actual reflectively-accessed classes; run `assembleRelease` +
    `testReleaseUnitTest`; document result in `docs/changelog.md`.
- **Task 8 (M): Compose UI tests**
  - `androidTest`: settings open → toggle sound → close; achievements section renders
    seeded unlocked chip. Requires emulator (Task 10 setup).
- **Task 9 (S): Accessibility pass**
  - Replace remaining `contentDescription = null` on interactive icons; audit contrast on
    dimmed text; touch targets ≥ 48dp.
- **Task 10 (M): Emulator performance verification**
  - Boot Pixel emulator (AVD on this machine), install debug build, record macrobenchmark-ish
    manual check: cold start, gameplay frame stability (systrace/gfxinfo), memory over a
    2-minute Zen session; fix hot-path findings (per-frame allocations, recomposition traps)
    if any surface.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| R8 breaks DataStore/Compose at runtime, not compile time | High | Keep rules already broad; verify on release build + emulator smoke test, not just build success |
| Achievement evaluation storms on rapid economy mutations | Low | Pure function on immutable state; evaluate once per mutation batch in ViewModel |
| MainActivity extraction regressions | Med | Verbatim move with compile-check before any restyle; UI tests cover settings flows |
| Emulator unavailable/slow on this machine | Med | Fall back to build + unit tests + static perf audit; ask user to run device check |

## Open Questions

- None — scope locked by user on 2026-09-12 (achievements in, leaderboards/time-travel/KMP out,
  localization skipped, performance via local Pixel emulator).
