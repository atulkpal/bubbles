# Bubbles: Full Audit Summary (24 Nov 2026)

**Scope:** the Android app v1.2 (versionCode 3) on Play, plus the large uncommitted working tree on `feature/backlog-completion` (music, 13 sound clips, achievements, Settings rework).
**Method:** five independent reviews, each with its own report in this folder:

| Report | What it covers |
|---|---|
| `01-runtime-smoke.md` | Debug build + 65 unit tests, emulator run of the working tree, Play v1.2 on a real phone (motorola edge 60 stylus). Screenshots in `screenshots/` |
| `02-architecture-engine.md` | Game loop, physics, economy, 27 ranked bugs with file:line, engine verdict |
| `03-security-privacy-policy.md` | Secrets in git, ads, billing, consent, Play policy, privacy policy |
| `04-creative-design.md` | Fun, clarity, art, audio, identity, 3 relaunch concepts |
| `05-market-aso-monetization.md` | Why there are no users, competitors, names, revenue math, go-to-market |

No game code was changed. The only new files are the ones in this folder.

---

## 1. Honest rating: **3 / 10**

| Area | Score | One line |
|---|---|---|
| Stability & speed | 8 | Zero crashes; cold start ~0.35–0.5 s and <0.5% jank on the phone. 11.6 MB download. |
| Engineering foundation | 5 | Level/spawn rules are sound and tested. The rest is a 2,056-line `MainActivity`, pixel-based sizes and a lifecycle that doesn't pause. |
| Correctness of promises | 2 | The live game is **silent**; magnet/ticking-bomb/chaos don't do what the tutorial says; Daily isn't seeded; "Breathe" has no feature. |
| Fun & clarity | 3 | Popping is fine for a minute. No tutorial, a fail shows "NEW HIGH SCORE", Play Again resets you to level 1, ads ruin runs. |
| Look & identity | 2 | Three different looks (cartoon store art, black-gold website, teal candy game). Reads like a 2016 template, not premium. |
| Store presence | 1 | "Bubbles" can't be found; store screenshots are AI posters of features that don't exist, two with a test ad banner; no video; no marketing ever done. |
| Policy & security | 2 | Signing key + passwords public on GitHub; forced rewarded ads; ad on every app return; no consent flow. |

**Why nobody downloads it:** people can't find it, the store page doesn't show the real game, and the people who did install it got a silent game with ads that wreck their run. **The engine is not one of the reasons.**

---

## 2. Fix immediately (money, trust, policy)

1. **CRITICAL: signing key is public.** `app/release-key.jks` and its passwords (literal in `app/build.gradle.kts`) are in commit `4f888ec` on public `origin/master`, and it's the same key used today (blob hash verified). Fix: Play Console → App integrity → request **upload key reset**, make a new keystore kept only in `keystore.properties`, optionally purge history.
2. **v1.2 on Play is silent.** All five `.ogg` files in `dc875be` are 0 bytes (verified). Real sounds exist only in the uncommitted working tree. **Commit the working tree first**; it is a lot of unsaved work.
3. **Forced rewarded ad between levels** (every level from 21, `MainActivity.kt:352-375`). Breaks Play's opt-in rule; closing it early leaves a blank screen with no buttons.
4. **Full-screen ad on every return to the app** (`LevelPlayAdManager.kt:75-79, 293-311`), and the game doesn't pause in background (`MainActivity.kt:186`). Confirmed on the phone: you come back to an ad covering the level, then "TIME'S UP".
5. **No consent flow** (UMP / `LevelPlay.setConsent` / child-directed flag). A "stress buster" game risks Families-policy review.
6. **Store screenshots misrepresent the app** (fake "MOVES" HUD, test-ad banner). Metadata-policy risk; replace with real captures.

## 3. Main bugs (details and file:line in reports 01 and 02)

- Back closes the app from every screen; Play Again / Restart always starts level 1 (also from Daily).
- "Pop!/COMBO" labels use px as dp (`MainActivity.kt:2017`), so they land off-screen.
- Bubbles pop on finger lift, not touch; overlapping taps pop the bottom bubble.
- Sizes and speeds in raw pixels: the smallest bubble is ~23 dp on sharp screens, ~40 dp on 720p.
- Economy:
  - Prestige is free and gives a permanent 2× boost.
  - The Zen combo bonus has no cap.
  - The +50 / Double buttons never hide, so ad-free buyers get unlimited coins.
  - Overlapping coin writes lose data.
  - Achievements can pay twice (working tree only).
- ViewModel created with `by lazy`, not retained: a theme change loses the run and leaks the loop.
- The daily ad cap is kept in memory only, and the "ads remaining" count ignores interstitials.
- Level select unlocks one level too many; Daily shows "LEVEL 0".
- Working tree ships 3.2 MB of WAV music that would be ~0.6 MB as OGG.

**What works:**
- Remove Ads really removes every ad.
- Opt-in rewards are granted only after a full watch.
- Purchases are acknowledged and restored on reinstall.
- The manifest is clean, and `play-account.json` / `keystore.properties` were never committed.
- Performance is very good.

---

## 4. Engine: keep Kotlin + Compose

All reviews agree with the owner: **a bubble popper doesn't need a new engine.** The simulation is about 1,500 lines, and its problems are bugs and structure, not the engine's limits.

| Option | Effort* | Verdict |
|---|---|---|
| A. Fix + polish in Compose | 1.5–2.5 wk | Phase 0 only |
| **B. Clean up Compose: pure-Kotlin engine module, dp-based world, pooled objects, real ViewModel tests** | 2–3 wk incl. A | **Recommended**, done as part of the relaunch |
| C. Godot 4.7 rebuild, same package | 5–8 wk | Only if web portals (Poki/CrazyGames) become a goal. No LevelPlay plugin; ads move to AdMob. |
| D. Web-first (Phaser/Pixi + Capacitor) | 4–6 wk | New stack; revisit at month 3 if a web build is wanted |

\*One developer working with AI agents.

**Carries forward:** level table and generator, spawn weights, chain/scoring rules, economy and achievement data, CC0 audio, package id, Play scripts, website.

---

## 5. The relaunch strategy

### Phase 0: Stop the harm (3–5 days, v1.3 in Compose)
1. Commit the working tree. Reset the upload key.
2. Remove the forced between-level rewarded ad and the app-return interstitial. Pause the game in `onPause`. Add Back handling, and make Restart replay the same level.
3. Add a UMP consent flow, the LevelPlay consent and child flags, a privacy-policy update, and `app-ads.txt`.
4. Fix the px/dp labels, pop on touch-down, topmost-bubble hit test, and the economy exploits (Prestige, Zen bonus, reward buttons, write race).
5. Convert WAV to OGG. Replace store screenshots with real captures. Add search words to the listing (stress relief, relaxing, ASMR, satisfying, anti stress).

*Version bump needs your OK (AGENTS.md §6).*

### Phase 1: Fresh look and fresh start (3–4 weeks, Compose option B)
The recommended concept from report 04 is **"Hushpop"**, a working title that still needs trademark and store checks:
- **Look:** real soap bubbles, see-through with a swirling rainbow film and a soft wobble.
- **Pop feel:** they pop the instant you touch them, with a pitch that rises as bubbles get smaller.
- **The hook:** hold your finger down to blow your own bubble.
- **Breathe mode:** inflate on the in-breath, let the bubble float off on the out-breath, so the tagline finally has a feature behind it.
- **Reason to return:** a "Soap Shelf" where daily play fills a jar that unlocks a new soap. A missed day pauses the streak instead of resetting it.
- **Modes:** Zen becomes the main mode, and Adventure shrinks to about 30 optional calm levels.

**Cut:** levels 21–100, the magnet, ticking-bomb, chaos and ghost bubbles, Prestige, the upgrade tree, the banner during play, and the gold/serif luxury look.

**Money:**
- Opt-in rewarded ads only, with no ads in a player's first 3 sessions.
- At most one menu banner.
- A ₹99 Supporter Pack (Remove Ads + soaps; ₹49 at launch, $2.99 in the US).
- Soap packs once retention is proven.
- 2–3 more ad networks (InMobi / Liftoff / Moloco).

**Store:**
- A new title. Report 05's pick is "Pop Haven: Bubble Pop & Relax".
- An icon showing one glass bubble with no text, and gameplay-first screenshots.
- A 20 s video, a Hindi listing, and an in-app review prompt.
- A/B test the icon and title with Play store listing experiments before committing.

Same package `com.ashwathai.bubbles`, so existing installs and purchases carry over.

### Phase 2: Launch and measure (from week 3, alongside Phase 1)
- **Short videos:** a game channel on the existing sheet-autopost pipeline, posting one satisfying-pop Reel/Short a day.
- **Paid test:** ₹5–10k to measure install cost and D1/D7 retention before spending more.
- **Targets:** 300–500 installs by week 4, 3–5k by month 3, 10–20k by month 6, but only if D1 retention is at least 30%.
- **Realistic ad revenue (mostly India):**

  | Installs | Ad revenue |
  |---|---|
  | 1k | ~$7–30/month |
  | 10k | ~$70–300/month |
  | 100k | ~$700–3k/month |

  Tier-1 players are worth about 10× more, which is why the English listing, the video and the US pricing matter.

### Freeze now
No more features on the current game except Phase 0.

---

## 6. Incidents during this audit
- While the tester was on the owner's phone (Play v1.2), a live interstitial appeared just as a scripted tap landed, so **one production ad (Blinkit) was clicked by accident**. One invalid click is not normally a problem, but don't click your own ads.
- The owner approved removing the Play copy from the phone. It was uninstalled, and the working-tree **debug build is now installed** on the phone (`rt-phone-new-01-start.png`). Reinstall from Play whenever you like.

## 7. Not tested / needs the owner
- The full working-tree matrix on a real phone: the new Settings, music by ear, achievements, the level-3 boss and level complete.
- A real purchase, and closing the forced rewarded ad early (soft-lock found from code only).
- **Play Console data:** installs, store visitors, conversion, retention, the Data safety form and target audience. This is the first input for Phase 2.
- Legal clearance of any new name.
