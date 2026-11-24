# 02 — Architecture & Engine Review (Bubbles v1.2 + working tree)

**Date:** 2026-11-24 · **Mode:** read-only. I did not build, edit, stash or check anything out; this file is the only thing written.
**Baseline:** shipped **v1.2** = commit `dc875be` (versionCode 3). Branch `feature/backlog-completion` holds a large **uncommitted** working tree (21 modified files, about 20 new ones: audio system, achievements, Settings extraction, tests). The review covers the working tree as it stands, and every finding is tagged **[shipped]** (in `dc875be`) or **[WT]** (working tree only).
**Read first:** `_studio/qa/QA-PLAYBOOK.md`, `AGENTS.md`, `docs/architecture.md`, `LUXURY_DESIGN_PLAN.md`. There is no `graphify-out/` in this repo, so graphify was not used.

---

## 1. Verdict (short)

- **Engine: Bubbles does not need a new engine to fix what is wrong with it.** The whole simulation (spawn, move, bounce, chain, split, boss) is about **1,500 lines**. Almost every serious defect is a plain bug, and none of them is a limit of Compose: sounds were silent, text was positioned in the wrong units, the game kept running in the background, an ad could soft-lock the player, and coin writes raced each other. "No takers" is not an engine problem.
- **What actually shipped in v1.2 is worse than the code suggests.** All five SFX files in `dc875be` are **0-byte OGGs** (`git ls-tree -l dc875be app/src/main/res/raw`), so the v1.2 "stress-buster bubble popper" has **no pop sound at all**. Every floating "Pop!/CHAIN/COMBO" label is drawn at `px` values read as `dp`, which on a 2.6–3.5× density phone puts it off-screen. The first impression is a silent popper with no feedback text.
- **Difficulty depends on the device.** All world units are raw pixels: bubble radius 40–130 px, speeds 300–1,400 px/s, bomb blast 180 px. A smallest bubble is a **~23 dp target on a 1440p phone and ~40 dp on a 720p phone**, and physical speed differs by about 1.75× between them.
- **The economy is broken in three independent ways.** Prestige is free at any time and gives permanent 2× coins. Combo coin bonus is unbounded, so Zen is an infinite faucet. Coin writes race and can lose or resurrect coins. The only coin sinks are 16 upgrades (1,600 coins) and Continue. Skins and themes are free, so coins stop mattering within minutes.
- **Recommendation:** (1) **Fix the P0/P1 list in Compose now** (about 5–7 dev-days) and ship v1.3, because that is the cheapest way to learn whether the game itself retains anyone. (2) Then decide the engine by **distribution**, not by tech. For **Android only**, refactor Compose (about 2–3 weeks; section 9, Option B). If you **want web portals (Poki/CrazyGames) as well**, rebuild in **Godot 4.7** (about 5–8 weeks with agents) behind a 1-week spike with hard go/no-go gates (section 9.3). A web-first TypeScript build (Phaser/Pixi + Capacitor) is the lightest option for portals, but it gives up the owner's existing Godot/Beckett pipeline and moves billing and ads onto Capacitor plugins.

---

## 2. Architecture map (actual, not as documented)

```
MainActivity.kt (2,056 L)  ── builds repos + GameViewModel "by lazy" (NOT a ViewModelProvider)
 ├─ BubbleScreen()         ── one big when(gameState); a root pointerInput sends EVERY tap into the VM channel
 │   ├─ StartScreen / LevelChip / LogoOrb
 │   ├─ GameScreen()       ── one Canvas (background + particles + bubbles), HUD composables,
 │   │                        PopMessageView = one Text composable per message (px used as dp)
 │   ├─ PauseOverlay / LevelCompleteScreen / GameOverScreen / AchievementUnlockChip / BubbleTutorialCard
 │   ├─ ad wiring for 7 rewarded placements, inline in composition
 │   └─ drawBubble() + 7 icon painters + drawParticle()         (lines 1709–2056)
 ├─ ui/settings/SettingsScreen.kt (733 L) [WT: extracted from MainActivity]
GameViewModel.kt (1,073 L) ── game loop, timer, combo, power-ups, economy calls, ads state, achievements [WT],
                              skin preview, daily, continue ×2 (duplicated), settings proxies
domain/usecase/GameUseCases.kt (940 L) ── Spawn / UpdateBubbles / HandleTap (chain BFS + particles + messages) /
                              UpdateMessages / UpdateParticles / CanSpawn / CheckLevelComplete
                              + 2 dead use cases (ActivatePowerUp, CheckPowerUpExpiration)
domain/model/GameModels.kt ── Bubble/Particle/PopMessage (data classes with var fields), LevelConfig table 1–20,
                              generateProceduralLevel 21–100, EconomyConfig, skins/themes, EconomyState
data/local/*RepositoryImpl ── DataStore; each repo keeps a MutableStateFlow mirror, loaded once at init
LevelPlayAdManager / BillingManager / LevelPlayBanner ── singletons (objects)
```

**Layering is nominal.** The domain layer imports `androidx.compose.ui.graphics.Color` and `ui.theme.luxury.LuxuryColors` (`GameUseCases.kt:10-11`, `GameModels.kt:3`). Use cases build UI artefacts directly: pop-message text, font sizes and particle colours. `docs/architecture.md` describes `StateFlow<GameUiState>`, but that does not exist. State is about 30 `mutableStateOf` fields plus 4 `mutableStateListOf` on the VM. The docs also say the glass has "blur", but `GlassSurface` is a gradient and a border (no blur, which is fine for performance).

**God objects:** `MainActivity.kt` (all screens, all ad wiring, all drawing) and `GameViewModel` (loop + rules + economy + ads + achievements + settings). `HandleTapUseCase` mixes rules, scoring, VFX and copy in one 540-line class.

---

## 3. Game loop, timing, lifecycle

| Aspect | What the code does | Assessment |
|---|---|---|
| Driver | `viewModelScope.launch { while(isActive){ updateGame(dt); delay(16) } }` on **Main** (`GameViewModel.kt:378-391`) | Not vsync-locked. `delay(16)` plus the work gives about 55–60 updates/s that beat against 60/90/120 Hz panels, which shows as visible judder on high-refresh phones. Use `withFrameNanos` (Compose frame clock) or `Choreographer`. |
| Delta time | Real `nanoTime` delta, clamped to 50 ms | Correct. Below 20 fps the game slows down instead of skipping, which is acceptable. |
| Fixed step | None. Variable dt with Euler integration and edge-bounce | Fine for a bouncer. Tunnelling cannot happen because bubbles only collide with walls. |
| Timers | Level timer uses dt. **Power-ups (slow-mo, freeze, multi-pop) and combo expiry use `System.currentTimeMillis()`** (`:404-405, :420, :643-675`) | Power-ups keep **running out while paused, during the tutorial card and during hit-stop**. A 5 s slow-mo collected just before Pause is gone on resume. |
| Start/stop | Loop cancelled on pause, level complete, game over and home; restarted by `startGameLoop()` | OK. |
| **Lifecycle** | `MainActivity.onPause` only pauses music (`MainActivity.kt:186-189`). Nothing calls `pauseGame()` | **The loop keeps running in the background.** The level timer drains while the user is on a call or in another app, then Game Over. Zen burns CPU at about 60 Hz in the background. **[shipped]** |
| ViewModel lifetime | `GameViewModel` is created with `by lazy { GameViewModel(...) }` inside the Activity (`MainActivity.kt:158`), not through `ViewModelProvider` | Any recreation the manifest does not absorb (dark-mode/uiMode, locale, font scale, density) builds a **new VM and new repos**. The old VM's `viewModelScope` is never cancelled because `onCleared` never runs, so its loop and collectors leak and the run in progress is lost. `configChanges` covers orientation only. **[shipped]** |
| Surface size | `LaunchedEffect(Unit) { onSurfaceSizeChanged(w,h) }` (`MainActivity.kt:243-245`) | Runs only once. Orientation is not locked and rotation is absorbed by `configChanges`, so after a rotate, split-screen or fold the world keeps its old bounds. Bubbles bounce inside an invisible portrait box. |
| Input | A root `pointerInput` sends every tap into an `UNLIMITED` channel (`MainActivity.kt:232-236`); the loop drains at most 3 per frame | Taps made on the Pause dim layer, the Start screen background, etc. are **queued and replayed** against live bubbles on the next resume or start. Drain the channel in `beginSession`/`resumeGame`, and drop taps unless the state is `Playing`. |
| Back | No `BackHandler` anywhere | System Back in game or in Settings **closes the app** (Playbook rule 2). **[shipped]** |

## 4. Physics, hit-testing, spawn and level logic

- **Movement** (`GameUseCases.kt:220-268`): position += v·dt, plus wind and gravity added to velocity, and a 0.8 restitution bounce off the screen edges. Bubbles never collide with each other. The off-screen filter (`:267`) is dead because positions are clamped first.
- **Spawn** (`:105-217`): every bubble spawns at **one of four exact corners** (`x=-r,y=-r` …). The first update clamps it to `(r,r)`, so bubbles visibly **teleport into a corner**. There is no entry from the edges, which makes the spawn look monotonous.
- **Boss re-spawns** (`:63`): `hasBoss && none{BOSS}` is checked on *every* spawn tick. Killing the boss early makes another one spawn on the next tick, until the budget is spent. A boss level can contain 3–4 bosses (each with 5 HP and 2 minions per hit). This is probably unintended. **[shipped]**
- **Hit test** (`HandleTapUseCase :293-295`): it uses `indexOfFirst` with exact radius and no touch slop. The list order is also the draw order, so with overlapping bubbles **the one underneath pops**, not the one drawn on top. Iterate from the end and add about 8 dp of slop.
- **Chain BFS** (`:335-364`): O(n²) per tap with `sqrt` and `Set<Bubble>` membership on data-class equality, which hashes all 14 fields. That is fine at n ≤ 60. **Split children spawn at the same point and in the same colour**, so tapping one often chain-pops its sibling in the same tap.
- **CHAOS effect is a no-op** (`:521`): `remaining.toMutableList()[i] = …` writes into a throw-away copy. **[shipped]**
- **MAGNET "pulls nearby bubbles"** and **TICKING_BOMB "explodes on its own in 1.5 s"** (tutorial copy, `GameViewModel.kt:571-572`) are **not implemented anywhere**. `Bubble` has no fuse field, and `UpdateBubbles` has no attraction. These are broken promises (Playbook rule 4). **[shipped]**
- **PRISM power-up** (`GameViewModel.kt:658-660`) is added to `activePowerUps`, has **no effect**, and **never expires** (`checkPowerUpExpiration` ignores it), so its HUD chip stays for the rest of the session.
- **Message placement**: "CHAIN"/random pop text and "COMBO" are placed at `bubbles.first()` (the oldest bubble on the board), not at the tap (`GameUseCases.kt:611-612, 622-623`, `GameViewModel.kt:618-619`).
- **Level budget/completion** (`CanSpawnBubbleUseCase`, `CheckLevelCompleteUseCase`): the logic is correct, and it is the one well-tested area. The HUD "Bubbles left: N" shows `bubbles.size` (on board), so it reads 0 at level start and goes **up** as bubbles split. It means nothing to the player.

### Density / screen-size dependence (yes, difficulty varies)
All gameplay constants are pixels (`GameConfig.minRadius = 40f, radiusStep = 30f`, `baseSpeed` 300→1,400, `BOMB_BLAST_RADIUS = 180f`, `multiPopRadius 150f+`):

| Device class | density | smallest bubble (r=40 px) | level-20 speed 950 px/s |
|---|---|---|---|
| 720×1600, 320 dpi | 2.0 | ~40 dp diameter | ~3.0 in/s, crosses the width in 0.76 s |
| 1080×2400, 420 dpi | 2.625 | ~30 dp | ~2.3 in/s, 1.1 s |
| 1440×3200, 560 dpi | 3.5 | **~23 dp** (below the 48 dp touch guidance) | ~1.7 in/s, 1.5 s |

**Fix:** define a world in dp (or a fixed virtual width such as 400 units, scaled to the screen) and convert once at render time.

### Procedural levels 21–100 (`GameModels.kt:129-175`)
- **Discontinuity at 21:** level 20 has wind 105 and gravity 95. Level 21 computes `stage = 2`, so `windBase = 0` and gravity = 0 (gravity only starts at stage ≥ 3, and from 0). Wind and gravity therefore **drop to zero at level 21** and do not get back to level-20 values until about level 60. The `difficulty` blend only affects `powerUpChance`.
- Time limits shrink to about 10 s by level 100. Each popping tap adds 0.5 s (capped at the limit), so the timer is really a **sustained "≥2 pops/s" tap-rate test**. Accuracy on tiny 1,000+ px/s targets is what limits a player, and that depends on screen density (above). Whether 50+ is winnable on a 1440p phone is unverified. A headless bot simulation of the use cases would settle it in an afternoon.
- The `LEVEL_50` achievement and levels much past 25 are probably unreached content.

## 5. State and per-frame cost

**Per frame (`updateGame`)**, with about 50 bubbles, 150 particles and 10 messages:
- `bubbles.toList()` ×1–3, then `map{copy}` + `filter` in UpdateBubbles; the same for messages and particles. That is about **200+ data-class copies and about 8–10 new lists per frame**, roughly 10–15 KB/frame or about 0.7 MB/s of garbage. It is fine on mid-range phones and causes GC stutter on cheap ones.
- `bubbles.clear(); bubbles.addAll(...)` on **three `SnapshotStateList`s every frame** creates new persistent lists and snapshot write records. Any composable that reads them is invalidated.
- **Recomposition scope is the whole screen:** `BubbleScreen` reads `bubbles.size` (for `bubblesLeft`), `levelTimeRemaining`, `shakeTrauma` and `combo` *in composition*. So `BubbleScreen` → `GameScreen` → HUD row → timer ring recompose **every frame**, plus one `Text` composable per pop message (`PopMessageView`, with shadow blur).
- **Coins and stats hit DataStore on every popping tap:** `addCoins` + `recordPops` + `recordMaxCombo` = **up to 3 file-writing `DataStore.edit` calls per tap** (`GameViewModel.kt:594-632`). At 4 taps/s that is about 12 disk writes per second, and it also triggers the race in B5.

**Rendering (`drawBubble`, `MainActivity.kt:1709-1914`):** 6 circles per bubble, 2 of them with a `Brush.radialGradient` **allocated per bubble per frame** (a new `RadialGradient` shader each time), plus a new `Path` for each icon. Particles take 3 circles each, and rings take 2 stroked circles. At 50 bubbles and 150 particles that is about 750 draw ops/frame and about 100 shader allocations/frame. Draw-op count is not the problem for the GPU (hardware canvas handles it easily). Allocation churn and full-screen recomposition are the problem. Fixes: cache brushes per (colour, radius bucket) or draw one pre-rendered bubble `ImageBitmap` per colour and scale it, reuse `Path`s, and read state only inside the `Canvas` draw lambda.

**Verdict on performance:** expect 60 fps on mid-range, with GC hitches and high-refresh judder. None of this needs a new engine. It needs about 3–4 days: a frame-clock loop, mutable pooled entities in a plain array plus one `mutableIntStateOf` "frame tick" that the Canvas reads, cached brushes, and DataStore writes batched at level end or every 2 s.

---

## 6. Economy and progression correctness

| System | Rule in code | Problem |
|---|---|---|
| Coins per pop | `pops × 1 × coinMultiplier + (combo-1) × 2` per tap (`GameViewModel.kt:593`) | The combo bonus grows **without bound**. Combo continues for as long as taps land within 700 ms, so in **Zen (no timer, 18 bubbles)** a steady tapper reaches combo 50 and gets about 100 coins per tap. This is an infinite faucet. Cap it, or pay the combo bonus once when the combo ends. |
| Score | `scoreGain × (1 + (combo-1) × 0.25)`, also unbounded | High scores are mostly a function of combo length. |
| Upgrades | 4 tracks × 4 steps × 100 = **1,600 coins total** | Read-then-async: a double tap spends 200 for one level (`:859-874`). Once maxed, coins have no use. |
| Skins/themes | All free except Gold (prestige-gated) | `docs/architecture.md` says "unlockable at coin thresholds". That is false. |
| **Prestige** | Settings button shown whenever `prestigeLevel < 1` (`SettingsScreen.kt:371-380`), with no requirement or confirmation; it resets coins to 0, sets the Gold skin and **2× coins forever** | Any new player can prestige at 0 coins straight away. Max prestige is 1, so this is not a loop. **[shipped]** |
| Daily | `dailyReward = 100 + score` coins (`:804`), "double daily" via ad, and **"retry daily" via ad pays again** | The coin reward is about 10–50× a normal session. There is **no seed anywhere** (`grep -i seed` → 0 hits), so "same puzzle for everyone" (`docs/architecture.md`) is false. "PLAY AGAIN" after a Daily starts **Adventure level 1** (`restartGame`, `:843-847`). |
| Continue (ad) | `continueGame()` restarts the level with a **full** timer, then `addTimeBonus(15f)` is **clamped to the limit**, so the promised +15 s does nothing (`MainActivity.kt:435-436`, `GameViewModel.kt:984-988`) | Both continues also reset the board and spawn budget, so Continue is really "retry this level, keep the score". |
| Continue (coins) | Checks coins, fires `spendCoins` async **ignoring the result**, then continues with `timeLimit + 15` (`:991-1010`) | The timer starts above 100% of the ring. There is also a duplicate of `continueGame` (Playbook rule 5c). |
| Restart | `restartGame()` → `startGame()` = **level 1** | "RESTART" in Pause at level 15 sends the player to level 1 with score 0. |
| High score | Saved only in `gameOver()` | Quitting via Home from Level Complete or Pause **discards the run's high score** and the games-played count. |
| Stars | `starsForTimeFraction` on time remaining | Correct and tested. Because each pop adds 0.5 s, skilled play tends to end with a full timer, so stars hardly vary. |
| Milestone coins | `25 + (level-1) × 10` on **every** clear, including replays | It can be farmed via level select (level 20 = 215 coins per replay). Minor. |
| Achievements [WT] | Re-evaluated on every economy emission; unlock + reward run in a launched coroutine | **Double-award race:** `unlockAchievements` suspends in `dataStore.edit` before updating state. The next emission from the same tap (`addCoins`/`recordMaxCombo`) re-evaluates, sees the achievement still locked, and **grants the 25 coins and plays the chip/sound again**. `GAMES_10` counts `gamesPlayed`, which is incremented in **two** repositories (Score and Economy). |
| Ads gating | Between-level "interstitial" uses the **rewarded** placement (`MainActivity.kt:358-376`) | If the player closes it before the reward, `onAdClosed` drops the callbacks and nothing calls `startNextLevelConfirmed()`. The state stays `LevelComplete(waitingForAd=true)`, which renders **nothing but the background**: a soft-lock until the app is killed. It is also a forced rewarded ad (policy; see the security report). |

**Race in the repositories (root cause of several symptoms):** `addCoins`, `spendCoins`, `recordPops` etc. read `_state.value`, `suspend` in `dataStore.edit`, and only then write `_state` (`EconomyRepositoryImpl.kt:71-86, 119-136`). Two overlapping calls lose an update. A prestige racing a pending `addCoins` can bring coins back. `load()` runs asynchronously on IO at construction, so a write that lands before the load finishes overwrites saved coins with `0 + amount`. **Fix:** do every mutation inside one `dataStore.edit { prefs -> … }` that reads from `prefs` (atomic), and derive `state` from `dataStore.data.map{}` instead of a hand-kept mirror.

---

## 7. Ranked bug list

| # | Sev | Bug | Evidence | Fix | Tag |
|---|---|---|---|---|---|
| B1 | **P0** | **No SFX at all in v1.2.** Every `res/raw/*.ogg` in the release commit is 0 bytes, plus an unused 1.75 MB `bubbles_logo.png` | `git ls-tree -l dc875be app/src/main/res/raw` | Already fixed in WT (13 CC0 clips). Ship it. Convert `music_*.wav` (3.2 MB) to OGG (~0.4 MB). | shipped (fixed in WT) |
| B2 | **P0** | Pop/chain/combo/"NEW" labels use **pixel coordinates as dp**, so they land off-screen or far from the tap on every modern phone | `MainActivity.kt:2017` `.offset(x=(msg.x-50).dp, y=msg.y.dp)` | Convert with `LocalDensity` (`toDp()`), or draw text in the Canvas with `TextMeasurer` | shipped |
| B3 | **P0** | **Game runs in the background**: the timer drains, Game Over happens off-screen, CPU burns | `MainActivity.kt:186-189`; no `pauseGame()` caller besides the HUD | Call `pauseGame()` in `onPause`, or observe `Lifecycle.Event.ON_STOP` in the VM | shipped |
| B4 | **P0** | **Soft-lock after the between-level ad** if it is closed before the reward (blank screen, no buttons) | `MainActivity.kt:356-376`, `LevelPlayAdManager.kt:229-235` | Use a real interstitial with an `onClosed` → continue path. Always render `LevelCompleteScreen` underneath. | shipped |
| B5 | **P1** | Coin/stat **lost-update race**, and a load-vs-write race at startup | `EconomyRepositoryImpl.kt:71-136`, `ScoreRepositoryImpl.kt:77-105` | Atomic `edit{}` reading from `prefs`; derive state from `data` | shipped |
| B6 | **P1** | **Free prestige** (2× coins forever, Gold skin) for any player, with no confirmation | `SettingsScreen.kt:371-380`, `EconomyRepositoryImpl.kt:138-146` | Gate on clearing level 20 (or 100) and add a confirm dialog, or remove prestige | shipped |
| B7 | **P1** | Unbounded combo coin bonus turns Zen into an infinite coin faucet | `GameViewModel.kt:593` | Cap at combo 10, or pay once on combo end; give Zen 0 coins or a cap | shipped |
| B8 | **P1** | Achievement **double reward / double chip** race | `GameViewModel.kt:216-226` | Mark unlocked in VM memory synchronously before launching; make the repository unlock+reward atomic | WT |
| B9 | **P1** | VM not owned by a `ViewModelStore`: recreation leaks the loop and loses the run | `MainActivity.kt:158` | `by viewModels { factory }` | shipped |
| B10 | **P1** | MAGNET pull and TICKING_BOMB self-detonation are promised in the tutorial but not implemented; CHAOS fling is a no-op | `GameViewModel.kt:571-573`, `GameUseCases.kt:511-526` | Implement (about 40 lines each in `UpdateBubbles`) or change the copy | shipped |
| B11 | **P1** | Difficulty and touch-target size scale with screen density (px units) | `GameModels.kt:178-179`, `GameUseCases.kt:32` | dp/virtual-unit world | shipped |
| B12 | **P1** | Restart (Pause and Game Over) always goes to **level 1**; "PLAY AGAIN" after a Daily starts Adventure | `GameViewModel.kt:843-847` | `restartGame()` replays the current mode and level | shipped |
| B13 | **P1** | No `BackHandler`: Back in game or Settings exits the app | no hits for `BackHandler` | Back → pause / close Settings | shipped |
| B14 | **P2** | Ad Continue's "+15 s" is clamped away; coin Continue starts above 100% and ignores the `spendCoins` result | `MainActivity.kt:435-436`, `GameViewModel.kt:984-1010` | One `continueRun(bonus)` that raises `levelTimeLimit` too and checks `ok` | shipped |
| B15 | **P2** | Power-up and combo timers use wall clock, so they expire during pause, tutorial and hit-stop | `GameViewModel.kt:404-405, 643-675` | Count down in game-time with dt | shipped |
| B16 | **P2** | Stale taps queued from non-game screens are replayed on resume/start | `MainActivity.kt:232-236`, `GameViewModel.kt:161, 465-470` | Drain the channel on start/resume; ignore unless `Playing` | shipped |
| B17 | **P2** | Boss respawns after being killed on boss levels | `GameUseCases.kt:63` | Track `bossSpawnedThisLevel` | shipped |
| B18 | **P2** | Upgrade double-tap pays twice | `GameViewModel.kt:859-874` | Atomic `buyUpgrade(key)` in the repository | shipped |
| B19 | **P2** | High score and games played not saved when leaving via Home from Level Complete or Pause | `GameViewModel.kt:677-732, 849-857` | Save the best score on every level clear and on `goHome` | shipped |
| B20 | **P2** | Surface size captured once: rotation, split-screen and foldables keep the old bounds | `MainActivity.kt:243-245` | `LaunchedEffect(width, height)` or `onSizeChanged`; or lock portrait | shipped |
| B21 | **P2** | Wind and gravity drop to 0 at level 21 (procedural curve discontinuity) | `GameModels.kt:155-159` | Start the procedural bands from level-20 values | shipped |
| B22 | **P2** | Daily has no seed (not "same for everyone"); "retry daily" pays the reward again; reward = 100 + score | `GameViewModel.kt:304-312, 804`; no `seed` in code | Seed `Random(yyyymmdd)` into spawn; pay the reward once per date | shipped |
| B23 | **P3** | PRISM power-up has no effect and its HUD chip never clears | `GameViewModel.kt:658-660, 664-675` | Implement (e.g. +rainbow odds for 6 s) or remove it from `PowerUpType` | shipped |
| B24 | **P3** | Hit test pops the bubble *underneath*; no touch slop | `GameUseCases.kt:293-295` | `indexOfLast`, `r + 8dp` | shipped |
| B25 | **P3** | Labels anchored at `bubbles.first()` instead of the tap | `GameUseCases.kt:611, 622`; `GameViewModel.kt:618` | Pass the tap point | shipped |
| B26 | **P3** | "Bubbles left" shows bubbles on board, not remaining budget | `MainActivity.kt:304, 990` | `maxBubbles − spawned + onBoard` or a progress bar | shipped |
| B27 | **P3** | Bubbles spawn by teleporting into the 4 corners | `GameUseCases.kt:118-126` + clamp at `:250-264` | Spawn at a random edge point, moving inwards, and skip the clamp until inside | shipped |

---

## 8. Code health: dead code, unreachable features, tests

**Dead or unreachable**
- `ActivatePowerUpUseCase`, `CheckPowerUpExpirationUseCase` (`GameUseCases.kt:896-940`) and their `AppModule` providers: unused duplicates of VM logic.
- `SoundManager.playSplit/playBoss/playCrack`: never called, so `boss.ogg`, `crack.ogg` and `split.ogg` load but never play **[WT]**. The boss and frozen-crack moments are silent.
- `LevelPlayAdManager.maybeShowLevelCompleteInterstitial`, `hasAdsRemaining`, and `todayString()` in MainActivity: unused.
- `ScoreRepository.gamesPlayed` duplicates `EconomyState.gamesPlayed`.
- Probably unreached by players: levels ≫ 25 and `LEVEL_50`; skin "Emoji" (it draws a sparkle, not emoji); 4 of 7 specials work only partly (B10).

**Tests (9 unit files, 1 UI file)**
- Meaningful: `CheckLevelCompleteUseCaseTest` and `CanSpawnBubbleUseCaseTest` (guard the two regressions in AGENTS.md §8). `SpawnBalanceTest` is seeded and statistical, and includes ghost-split containment. `AchievementEngineTest` is pure. The repository tests run against real DataStore files.
- Low value: `AudioConfigTest` and `EconomyConfigTest` mostly assert constants. `ExampleUnitTest` and `ExampleInstrumentedTest` are template code.
- **Gaps where the bugs are:** nothing tests `GameViewModel` (1,073 lines: continue, restart, daily, combo, power-ups, pause). Chain rules other than ghost are untested, and so are the CHAOS no-op, concurrent repository writes, procedural-curve monotonicity and winnability. The only UI test file covers Settings (`SettingsScreenUiTest`, 11 tests **[WT]**). No test starts a game, pops a bubble or clears a level (Playbook rule 6). The repository tests would not catch B5 because they never run writes concurrently.

---

## 9. Engine verdict

### 9.1 What a bubble popper actually needs
A few hundred sprites at most, lots of short-lived particles, screen shake, hit-stop and tweens ("juice"), a glass/refraction look (ideally a shader), low-latency audio with pitch variation, 60 fps on a ₹8k phone, tiny download, instant start. No physics engine, pathfinding, tilemaps or 3D.

**Compose Canvas can do all of this** except shaders on older Android versions: AGSL `RuntimeShader` needs API 33+, and minSdk is 26. The glass look can be faked with pre-rendered bitmaps, as it already is with gradients. The current jank sources (allocation churn, full recomposition, `delay(16)`) can all be fixed in Compose.

### 9.2 Options

| Option | What it means | Effort (1 dev + AI agents) | Pros | Cons / risks |
|---|---|---|---|---|
| **A. Keep Compose, fix + polish** | Fix B1–B22, batch DataStore, frame-clock loop, dp world, cached brushes, BackHandler, auto-pause; fresh look via new art/palette in the existing Canvas | **5–7 dev-days** (bugs) + 3–5 days (visual refresh) | Cheapest; LevelPlay + Billing 8 already integrated and working; ships in about 2 weeks; tests stay valid | Android only. Monolithic files stay. No shader pipeline below API 33. |
| **B. Refactor Compose** | A + split into `engine/` (pure Kotlin: `World`, entity arrays, `step(dt)`, seeded RNG, no Compose types), a `render/` Canvas layer reading one frame tick, a screen-per-file UI with Navigation and BackHandler, atomic repositories, VM tests + a headless bot sim | **2–3 weeks** total (includes A) | Testable engine; per-frame allocations removed; the pure-Kotlin engine is a direct reference for any later port | Still Android only. Compose stays an unusual game host (no particle editor, no shader tooling). |
| **C. Godot 4.7 rebuild (same package)** | New Godot project, Compatibility renderer; port the sim to GDScript; `GPUParticles2D`/`CPUParticles2D`, canvas_item glass shader, Tweens; Android export + **Web export** for portals | **5–8 weeks** (≈26–42 dev-days, see 9.4) | One codebase for Android + web portals (+ iOS/desktop templates already installed); real shaders on every API level; the owner already has Godot 4.7.1, export templates and the Beckett MCP play-test loop (`C:\Users\Atul\projects\2d`) | **No LevelPlay plugin for Godot** (found in the Jump Droid research): switch to AdMob (Poing Studios plugin) or write a custom plugin. Billing needs the GodotGooglePlayBilling plugin (check the bundled Billing version ≥ 7/8). Godot adds about 20–25 MB per ABI on Android. The Android export pipeline has never been run end-to-end on this PC. DataStore protobuf progress needs a small importer, or accept a reset given the tiny user base. Web build starts around 8–10 MB compressed (wasm engine), heavier than portals like for mobile web; check each portal's current size and SDK rules. |
| **D. Web-first (TypeScript + Phaser/Pixi), Android via Capacitor/TWA** | Rebuild as a web game; ship to Poki/CrazyGames with their JS SDKs; wrap for Play with Capacitor (AdMob + billing plugins) | **4–6 weeks** | Smallest, fastest-loading portal build (~1–2 MB); portals are a real discovery channel for casual poppers; the web is the native platform for portal SDKs | New stack for the owner (no Beckett loop). Android feel and haptics are weaker in a WebView. Play Billing and ads go through third-party Capacitor plugins. Two ad systems (portal SDK vs AdMob). |
| E. Do nothing / freeze | Leave v1.2 | 0 | — | Shipped build is silent, and labels are off-screen. Do at least B1–B4. |

### 9.3 Recommendation
1. **Now: Option A** (about 1 week of bug work) and release v1.3. The v1.2 player never heard a pop, never saw a label, could be soft-locked by an ad and lost runs when the app went to the background. Retention data from v1.2 tells you nothing about the game design. Fix it first and measure D1/D7.
2. **Then choose by channel:**
   - **Android only → Option B.** Rebuilding a 1,500-line simulation in another engine gives no gain that justifies 5+ weeks.
   - **Android + web portals → Option C (Godot)**, *only after a 1-week spike* that must pass: (a) Web export ≤ ~10 MB compressed and first frame ≤ 3 s on mid-range mobile Chrome; (b) 60 fps with 60 bubbles + 300 particles + glass shader on the cheapest test phone; (c) an AdMob (or custom LevelPlay) rewarded ad and a Play Billing test purchase working in a signed AAB; (d) 16 KB page-size check passes on the 4.7.1 AAB. If (a) fails, portals favour **Option D**.
3. **Honest bottom line on the owner's suspicion:** you are right that a simple game does not *need* a new engine. Godot only pays off if you want **web portals** or a **shader-heavy fresh look** and plan to make more small games in it, where the pipeline cost is shared. A fresh look by itself is art and UI work in any engine.

### 9.4 Godot effort breakdown (Option C)
| Work item | Dev-days |
|---|---|
| Sim port (spawn, move, chain BFS, split, boss, specials incl. real MAGNET/TICKING/CHAOS), seeded daily, dp-free virtual resolution | 3–4 |
| Rendering: bubble glass shader, particles, shake/hit-stop/tweens, pop labels | 4–6 |
| UI: start, level select, HUD, pause, level complete (stars), game over, settings, achievements, tutorial cards | 6–9 |
| Save system + optional DataStore importer | 1–3 |
| Audio (13 SFX + 2 loops, buses, pitch variation, crossfade) | 1–2 |
| Ads: AdMob via Poing (3–4) **or** custom LevelPlay plugin (6–10) | 3–10 |
| Billing (remove ads) via GodotGooglePlayBilling | 2–4 |
| Web portal SDK (Poki/CrazyGames ad breaks via `JavaScriptBridge` or a community addon) | 2–3 |
| Export presets, signing (same keystore/package), on-device QA per playbook | 4–6 |
| **Total** | **≈ 26–42 dev-days (5–8 weeks)** |

### 9.5 What carries forward (any option)
| Asset | Reuse |
|---|---|
| Level table 1–20 + `generateProceduralLevel` (fix B21), spawn-balance weights, unlock levels, chain rules, scoring, economy constants, achievement list | 100% as data or logic; port mechanically |
| `CanSpawn`/`CheckLevelComplete` rules + their tests | Port the rules and re-express the tests (GUT in Godot, Vitest in TS) |
| 13 CC0 OGG SFX + 2 music loops (convert WAV→OGG) | 100%, drop-in |
| Palette/skins/themes, Midnight Glass tokens (`LuxuryTokens.kt`) | 100% as colour data |
| Icon painters (`drawBombIcon` …) | Logic maps 1:1 to Godot `_draw()` or SVG |
| Package id, keystore, Play listing, `scripts/*.mjs` upload automation, website | 100% |
| ironSource/LevelPlay integration, `BillingManager` | Reusable only in Options A/B, or as the body of a custom Godot Android plugin |
| UI screens (Compose) | Rewrite in C/D |

---

## 10. Suggested order of work (Option A, about 1–1.5 weeks)
1. **Day 1:** B1 (ship WT audio, WAV→OGG), B2, B3, B4, B13. These are first-impression and soft-lock bugs.
2. **Days 2–3:** B5 + B8 + B18 (atomic repositories), B6, B7, B22 (economy), B12, B14, B19.
3. **Days 4–5:** frame-clock loop, pooled entities, single Canvas read, cached brushes, dp world (B11), B15–B17, B20, B21. Then add a headless bot sim test over levels 1–100 on 3 screen sizes.
4. **Day 6:** B10 (implement MAGNET/TICKING/CHAOS or change the copy), B23–B27. Add a Compose UI test that starts a level, pops bubbles and clears it, run the QA playbook pass, and log any escapes in the playbook's Lessons log.

Do not bump `versionCode` without the owner's approval (AGENTS.md §6.6).
