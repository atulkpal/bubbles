# 01 — Runtime / Smoke-Test QA: Bubbles v1.2 (versionCode 3)

**Date:** 2026-11-24 · **Branch:** `feature/backlog-completion` (HEAD `dc875be` plus a large uncommitted working tree, tested as-is) · **Build:** DEBUG (`assembleDebug`, `testDebugUnitTest`)
**Devices:**
- **Emulator** (working-tree debug build): a temporary clone of the `Pixel_10` AVD, Android 17, 1080x2424, started with `-wipe-data -no-snapshot-load` (brand-new user). Software GPU (`swiftshader_indirect`), because the host-GPU emulator crashed twice. Used for about 5 minutes before the owner asked for it to be shut down (PC low on memory).
- **Real phone** (Play Store build): motorola edge 60 stylus, Android 16, 1220x2712. It runs the **Play Store install of v1.2** (installer `com.android.vending`), with the owner's own progress on it (level 22, about 39,000 coins). This is exactly what users get today.
**Playbook followed:** `C:\Users\Atul\projects\_studio\qa\QA-PLAYBOOK.md` (new user, instant tap feedback, Back on every screen, main journeys, promise-to-code check)
**Screenshots:** `docs/audit-2026-11-24/screenshots/` (`rt-NN-*` = emulator / working tree, `rt-phone-NN-*` = phone / Play v1.2), 34 files

> **Big limitation:** the working-tree debug build was **never run on the phone**. The owner approved uninstalling the Play copy, but the approval came to me through the coordinating agent, and the tool permission layer blocked the uninstall: it does not accept agent-relayed approval. So most of the matrix below is the **Play v1.2 build on the phone**, plus about 3 minutes of the working tree on the emulator. Where a bug is in code that the working tree did not change, I say so: those bugs are still present in the next release.
> **Gradle note:** `./gradlew` worked. Its Gradle 8.11.1 was already cached in `~/.gradle/wrapper/dists`. No repo files were changed. I created a temporary AVD clone (`Bubbles_QA`) because `Pixel_10` was already running another app; I deleted the clone afterwards.
> **Ad click disclosure:** on the phone, a production interstitial appeared at the instant my scripted tap landed on "CONTINUE", so I **accidentally clicked one live ad** (Blinkit, 11:10:50, it opened the Play Store page). No other ad was tapped; I closed the others with their skip/close buttons. No purchases were made.

---

## 1. Build and automated tests

| Item | Result |
|---|---|
| `assembleDebug` | **PASS**: BUILD SUCCESSFUL (whole run 5m 0s, together with the tests), debug APK **29.4 MB** (not minified) |
| `testDebugUnitTest` | **PASS: 65 tests, 0 failures, 0 errors, 0 skipped** |
| Unit test suites | `AchievementEngineTest` 18, `AudioConfigTest` 8, `SpawnBalanceTest` 8, `SettingsRepositoryImplAudioTest` 7, `CanSpawnBubbleUseCaseTest` 6, `CheckLevelCompleteUseCaseTest` 5, `EconomyConfigTest` 4, `EconomyRepositoryImplAchievementsTest` 4, `StarsForTimeFractionTest` 4, `ExampleUnitTest` 1 |
| Instrumented tests | `SettingsScreenUiTest` (8 Compose tests, Settings only). Not run: they need a device, and the emulator was shut down. No UI test covers the start screen, a game, pause, game over, level complete or Back. None of the bugs in §4 would be caught by the current tests. |

## 2. Performance and size

| Metric | Value | Notes |
|---|---|---|
| Cold start, phone, Play v1.2 (`am start -W`, 3 runs) | **511 / 362 / 349 ms** | Fast. |
| Cold start, emulator, debug, first install | 13,316 ms | Software GPU, first-ever run, GMS busy. Not representative; the phone number is the useful one. |
| Frames, phone, ~60 s Adventure L22 with the tap bot running | 4,044 frames (~64 fps), **0.40 % janky**, p50 29 ms, p90 36 ms, p99 44 ms; GPU p50 12 ms / p99 16 ms | The bot takes a screenshot every 1.5 s, which inflates the frame times. |
| Frames, phone, ~50 s Zen, simple taps only | 3,170 frames (~63 fps), **0.28 % janky**, p50 22 ms, p99 32 ms; GPU p50 13 ms | The game loop runs at about 60 fps (`delay(16)`), so on this 120 Hz screen it never uses the higher refresh rate. Smooth enough. |
| Memory, phone (meminfo) | **TOTAL PSS 194–197 MB, RSS 355–359 MB**, Native heap 50 MB, Java heap 28–37 MB | OK, but heavy for a 2D bubble game. Most of it is the ad SDK (WebView). |
| Crashes / ANRs in the app | **0** (`logcat -b crash` has nothing for `com.ashwathai.bubbles`; no `ANR in com.ashwathai`) | Phone ~15 min, emulator ~5 min. The emulator showed GMS ANRs (system, not the app). |
| Play v1.2 APK / AAB | **11.6 MB APK / 15.1 MB AAB** | 5.0 MB of the AAB is the R8 mapping file (`BUNDLE-METADATA`), which is not shipped to phones. In the APK, dex is 6.4 MB, the Unity/ironSource native libs 1.95 MB (4 ABIs), and **`bubbles_logo.png` 1.75 MB** (`res/0l.png`), a raw PNG used for one orb. |
| Working-tree size changes | **+3.2 MB** of music as **uncompressed WAV** (`music_adventure.wav` 1.41 MB = 32 s mono 22 kHz; `music_zen.wav` 1.76 MB = 40 s), **−1.75 MB** logo PNG (deleted), +0.1 MB of new OGG effects | `res/raw` files are stored without compression in the APK. As 96 kbps OGG, the two loops would be about 0.85 MB together, or about 0.4 MB at 64 kbps. Convert them before release. |

## 3. Test matrix

| # | Screen / flow | Build | Result | Evidence |
|---|---|---|---|---|
| 1 | Install on clean emulator, cold start | WT | PASS (slow on the emulator only) | rt-01 |
| 2 | First-run experience / tutorial | WT | **FAIL: none.** A new player lands on "CONTINUE · LEVEL 1" (there is nothing to continue). The only teaching is 3 lines of small grey text at the bottom. Special-bubble cards exist only for the first time a Magnet/Ticking Bomb/Chaos/Ghost bubble pops. | rt-01 |
| 3 | Level select (new user) | WT | **FAIL:** shows "1" and "★ 2". A brand-new player can start level 2 without clearing level 1 (Bug M1) | rt-02 |
| 4 | Level select (owner, level 22) | v1.2 | **FAIL:** CONTINUE says level 22, but the select row offers "★ 23" | rt-phone-27 |
| 5 | Adventure level 1 as a new player | WT | PASS (playable). Popping splits bubbles, so "6 bubbles" grew to **19 on screen** with 20 s left. "Bubbles left" counts what is on screen, not what is left to clear. Bubbles drift on top of the HUD. My bot (1.3 s reaction time) failed the level. | rt-03 to rt-06 |
| 6 | Game over after failing level 1 | WT | PASS (works). Confusing: a first-time player who **failed** sees a gold "NEW HIGH SCORE" with no "time's up" message. With 25 coins, the 50-coin continue is hidden. | rt-07 |
| 7 | Adventure level 22 (15 s timer, specials, boss-style bubbles) | v1.2 | PASS (playable, smooth) | rt-phone-02, 03 |
| 8 | Pause → Resume / Restart / Home | v1.2 | PASS (instant). "RESTART" goes to **level 1** (Bug H3) | rt-phone-04, 16 |
| 9 | Background mid-level, come back | v1.2 | **FAIL:** the game is not paused. On return, a **full-screen interstitial** covers the game while the level timer keeps running, and closing it lands on "TIME'S UP" (Bug C1) | rt-phone-11a, 12, 13 |
| 10 | Open the app (process already warm) | v1.2 | **FAIL (UX):** full-screen interstitial before any play | rt-phone-06 to 09 |
| 11 | Game over → "CONTINUE · 50 COINS" | v1.2 | PASS: coins 39,060→39,010, same level 22, timer 30 s (level time + 15 s). Shows at once. | rt-phone-14 |
| 12 | Game over → "PLAY AGAIN" at level 22 | v1.2 | **FAIL:** starts **level 1** (Bug H3) | rt-phone-15, 16 |
| 13 | Zen: play, "End zen session", Zen complete | v1.2 | PASS. Cosmetic: the level slot shows the text "INF" | rt-phone-17, 18, 26 |
| 14 | Daily challenge: play, finish, reward +1,613 coins | v1.2 | PASS (reward credited, button becomes "DAILY · COMPLETED", "RETRY DAILY · WATCH AD" appears). The HUD shows "LEVEL 0" | rt-phone-19, 20, 22 |
| 15 | Daily complete → "PLAY AGAIN" | v1.2 | **FAIL:** starts **Adventure level 1**, not the daily (Bug H3) | rt-phone-21 |
| 16 | Level complete screen / stars / level-3 boss / between-level ad | — | **NOT RUN.** The bot could not clear a level fast enough before the emulator was shut down, and on the phone I could not risk the owner's level 22 progress for it. | — |
| 17 | Settings: open, Reduced Motion toggle on→off→on | v1.2 | PASS (flips at once; the owner's value was restored) | rt-phone-23, 24 |
| 18 | Settings: skins, themes, stats, prestige, About links | v1.2 | Viewed only (not changed, to keep the owner's setup). The overlay is see-through: start-screen buttons show through the settings text. | rt-phone-25 |
| 19 | Settings: SFX/music sliders, Music toggle, achievements, upgrade pills (working-tree rework) | WT | **NOT RUN** (debug build never got onto the phone; emulator shut down) | — |
| 20 | Remove Ads button | v1.2 | Visible; not tapped (no purchases) | rt-phone-23 |
| 21 | System Back on Settings | v1.2 | **FAIL: closes the app** (Bug H4). No `BackHandler` anywhere in the working tree either | logcat / dumpsys |
| 22 | Rotation | — | Portrait is locked in the manifest (`screenOrientation="portrait"`). Not toggled on the owner's phone. | manifest |
| 23 | Sound on Play v1.2 | v1.2 | **FAIL: completely silent.** All 5 `.ogg` files in v1.2 are 0 bytes (git blob `e69de29…`), and logcat on launch shows `SoundPool::Sound: doLoad: unable to load sound` ×6. v1.2 has no music. | logcat, `git ls-tree dc875be` |
| 24 | Sound / music in the working tree | WT | **NOT VERIFIED by ear.** The files are real now (13 OGG clips, 2 WAV loops). The emulator decoded vorbis at start-up without errors. | logcat |
| 25 | Banner ad | both | No banner showed on the emulator (test units, ~5 min). On the phone, an empty grey bar appears at the bottom of the game-over screen | rt-phone-13 |
| 26 | Crashes / ANR | both | PASS: none | logcat |

---

## 4. Bugs ranked by severity

### CRITICAL

**C1. Leaving the app for a moment costs you the level, and an ad covers the game when you come back.** (Play v1.2. The ad code and the `onPause` code are unchanged in the working tree.)
- `MainActivity.onPause()` only pauses the music. The game loop is a `viewModelScope` coroutine, so it keeps running in the background and the level timer keeps falling.
- `LevelPlayAdManager.foregroundObserver.onStart` shows an interstitial on the first return to the foreground in each app process (`lastForegroundInterstitialAt` is in memory only, so it resets every launch, and then once per 4 h). It does not check whether a game is in progress.
- **Repro (phone):** start level 22 (17 s on the timer, rt-phone-11a) → Home → wait 5 s → reopen from the launcher → full-screen "Splash Buster" interstitial (a rival bubble game) → skip, wait, close → **"TIME'S UP"**, run over (rt-phone-12, 13).
- It also fires when the app is simply reopened: rt-phone-06 to 09, logcat `Interstitial DISPLAYED | unit=interstitial_main … revenue=1.36E-4`.
- Impact: in a stress-buster game, the player is punished for answering a message, by an ad. This alone is a strong reason for uninstalls and 1-star reviews.

### HIGH

**H1. The live Play version has no sound at all.** v1.2's five `res/raw/*.ogg` are empty files (committed as 0 bytes in `dc875be`), and SoundPool fails to load all six sounds at start-up. A "pop" game with no pop sound feels broken. The working tree replaces them with 13 real clips and 2 music loops, but I could not check this by ear on a device.

**H2. Interstitials and rewarded ads are not counted together, and the "N ads remaining today" text is wrong.** After two interstitials, the game-over screen still said "5 ads remaining today" (rt-phone-13). `adsShownToday` is only increased in the rewarded listener. A player can get 2+ forced interstitials and still be offered 5 rewarded ads.

**H3. "PLAY AGAIN" and "RESTART" always restart Adventure level 1.** `GameViewModel.restartGame()` calls `startGame()` with its default `level = 1` (unchanged in the working tree).
- **Repro:** at level 22, pause → RESTART, or fail → PLAY AGAIN → HUD shows LEVEL 1 (rt-phone-16).
- **Daily:** DAILY COMPLETE → PLAY AGAIN → Adventure level 1 (rt-phone-21), because `restartGame()` only knows ZEN or "not ZEN".
- The best score stays, but the player has to find SELECT LEVEL to get back. Most will think their progress was lost.

**H4. System Back closes the app from Settings (and from any screen).** There is no `BackHandler` anywhere (`grep` finds none). **Repro:** home → SETTINGS & UPGRADES → Back → launcher. A mid-game Back also exits without pausing. Playbook rule 2 (Back on every screen) fails.

### MEDIUM

- **M1. Level select lets you skip one uncleared level.** `playableMax = highestLevel + 1`, but `highestLevel` is already the next unplayed level. A new user sees "1, ★2" (rt-02); the owner sees CONTINUE 22 next to "★23" (rt-phone-27).
- **M2. A failed first level is celebrated.** "NEW HIGH SCORE" in gold, with no "TIME'S UP", is shown when the first-ever run fails (rt-07), because the new-high-score title wins over the time-up title. The player is not told that they lost or why.
- **M3. Level 1 is confusing for a beginner.** It starts with one bubble, and new ones come every 2 s. Popping splits bubbles, so the board grows from 6 to 19 or more. "Bubbles left: 19" counts what is on screen, not what is needed to win. Bubbles float over the HUD and the pause button, so taps there are ambiguous (rt-06).
- **M4. Between-level ads use the rewarded unit (code only, not run).** `startNextLevel()` sets `waitingForAd`, and only `onUserEarnedReward` or `onAdFailed` moves the game on. If a player closes the rewarded video early, `onAdClosed` clears the callbacks and nothing advances the game, so the player is left on a screen with no buttons. A forced rewarded video is also a policy problem (rewarded with no reward to choose). Needs a device test.
- **M5. The Settings sheet is see-through.** Start-screen buttons ("BUBBLES", "CONTINUE · LEVEL 22") show through the settings text (rt-phone-23, 25). This hurts the "liquid glass" look; it needs blur or a darker scrim.
- **M6. Empty banner box.** A grey empty bar sits at the bottom of the game-over screen (rt-phone-13). No banner filled on the emulator.

### LOW

- L1. The Daily HUD shows "LEVEL 0"; Zen shows the text "INF" (rt-phone-19, 17).
- L2. Status-bar icons are dark on the dark-navy top on the emulator start screen (rt-01).
- L3. The design-system fonts mostly don't show. Buttons and titles render in the system font (Roboto on Pixel, Motorola's font on the phone). Only floating pop words ("Luminous!") show the Cormorant serif (rt-05).
- L4. The tap at the gap between "SETTINGS & UPGRADES" and "POWER-UP GUARANTEE · WATCH AD" did nothing. When a rewarded ad cannot load or is on cooldown, every rewarded button fails silently (`onAdFailed = {}` everywhere in `MainActivity`).
- L5. The game loop's `delay(16)` caps play at about 60 fps on 90/120 Hz phones.
- L6. `bubbles_logo.png` (1.75 MB) in v1.2 was the largest single resource. It is already deleted in the working tree; keep it out.

---

## 5. User-facing promises vs code (runtime view)

| Promise (where) | Delivered? |
|---|---|
| "POP · BREATHE · REPEAT", "stress buster" (start screen, About) | **No in v1.2:** silent, and ads interrupt mid-level (C1, H1) |
| "Clear each level before time runs out" | Yes, but backgrounding the app runs the clock out (C1) |
| "CONTINUE · 50 COINS" (+15 s) | **Yes** (rt-phone-14) |
| "DAILY CHALLENGE" reward | **Yes** (+1,613 coins credited) |
| "PLAY AGAIN" | Misleading: restarts level 1 / Adventure (H3) |
| "N ads remaining today" | **Wrong:** ignores interstitials (H2) |
| "Remove All Ads — no banners, no interstitials, no rewarded ads. Rewards grant instantly." | Code gates banner, both interstitials and rewarded on `BillingManager.adsRemoved`. Not purchase-tested (no tester account, no real purchases) |
| "SELECT LEVEL" | Unlocks one level too many (M1) |
| "Coins … buy power-up upgrades" | Prices shown (100 each); not bought on the owner's phone |

---

## 6. First 60 seconds as a brand-new player (emulator, working tree)

1. **0 s:** a bright blue-to-teal gradient, a flat gold circle as the logo, "BUBBLES" in plain system bold, a coin pill showing 0, and six big buttons, the first one saying "CONTINUE · LEVEL 1" although there is nothing to continue. One of the six buttons already asks you to watch an ad.
2. **~5 s:** CONTINUE starts the game with **one bubble** in an empty screen and a 60 s ring. There is no "tap to pop" hint and no goal shown.
3. **~10–30 s:** popping works and feels fine, with pop words like "Luminous!" and particle rings. Every pop splits into more bubbles, so the screen fills up, and "Bubbles left" keeps rising. I couldn't tell how close I was to winning.
4. **~60 s:** time runs out, and the screen shouts "NEW HIGH SCORE 537" in gold, with "CONTINUE · WATCH AD" and "+50 COINS · WATCH AD" offered next. I wasn't told that I had lost.
5. On the Play build, the same minute is **completely silent**. If the player switches away and comes back, they get a full-screen ad and a lost level.

**Verdict:** it is a clean, stable, smooth tap game, but **generic, not premium**. The "Midnight Glass" luxury look in the design docs barely shows: no glass blur, system fonts, a flat orb logo, and bright teal casual colours. It looks like a 2016 template, not something people would pick over the big free bubble games. The core is fun for a minute, but there is no hook. There is no tutorial or goal, a fail is celebrated, progress resets on Play Again, the live build has no sound, and ads take over the screen at the worst moment. A player has little reason to stay past the first session.

---

## 7. What I could not test (blockers)

- **The whole working-tree build on real hardware:** music and SFX by ear, the new Settings (sliders, Music toggle, achievements, upgrade pills), the achievement chip, the level-complete "achievement moment" and stars, and the between-level rewarded ad (M4). Cause: uninstalling the Play copy was blocked by the tool permission layer (an agent-relayed approval is not accepted). **To unblock:** the owner runs `adb -s ZD222SCQ94 uninstall com.ashwathai.bubbles` and installs `app/build/outputs/apk/debug/app-debug.apk`, or approves the uninstall directly in the session.
- **Level complete, level-3 boss, and the between-level ad flow:** my screenshot bot is too slow to clear a level, and I did not want to risk the owner's level 22 progress.
- **Instrumented `SettingsScreenUiTest`:** needs a device with the debug build.
- **Remove Ads purchase / restore:** needs a licence-tester account; no real purchases allowed.
- **Rotation:** portrait-locked; not toggled on the owner's phone.
- **Settings skins / themes / prestige / upgrades:** not changed, to keep the owner's live setup.

The phone is left with the **Play v1.2 install untouched** (owner's progress kept; coins went 38,876 → 41,045, mostly from today's daily reward, which is now used; 50 coins were spent on one continue). The emulator is shut down.
