# Todo — Backlog Completion

Source plan: `tasks/plan.md` · Spec: `LUXURY_DESIGN_PLAN.md` (Settings Polish & Achievements section)

## Phase 1: Achievements
- [x] Task 1: Achievement catalog + pure engine + tests (TDD)
- [x] Task 2: Persist `unlockedAchievements` in EconomyState/EconomyRepositoryImpl
- [x] Task 3: Wire GameViewModel + unlock chip moment
- [x] Task 4: Achievements section in Settings

## Phase 2: Settings polish
- [x] Task 5: Extract SettingsScreen verbatim to `ui/settings/SettingsScreen.kt`
- [x] Task 6: Restyle upgrade rows, selection states, close affordance, spacing

## Checkpoint: build + unit tests green — PASSED

## Phase 3: Verification & hardening
- [x] Task 7: ProGuard/R8 verification (assembleRelease + testReleaseUnitTest + emulator smoke test)
- [x] Task 8: Compose UI tests — 8/8 green on Pixel_10 emulator
- [x] Task 9: Accessibility pass
- [x] Task 10: Pixel emulator performance verification (baseline recorded in docs/changelog.md)

## Checkpoint: all acceptance criteria met, AGENTS.md backlog updated — DONE
