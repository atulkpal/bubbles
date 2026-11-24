# Hushpop relaunch: QA report (24 Nov 2026)

**Build:** `feature/relaunch-hushpop`, v1.3 (versionCode 4). Debug build on the emulator for every step. The signed release build (`releases/hushpop-v1.3-code4.apk`, R8-minified) was smoke-tested too.
**Device:** Pixel_10 emulator (1080×2424, 420 dpi, Android 17), fresh install as a brand-new user (`uninstall` → `install`). The owner's phone was not used.
**Playbook:** `C:\Users\Atul\projects\_studio\qa\QA-PLAYBOOK.md`. There are no sign-in types, so rule 1 means a fresh install and data.
**Screenshots:** `docs/relaunch-qa/s*-*.png` (per step) and `docs/relaunch-qa/final/q*.png` (final pass).

## Automated
- **Unit tests:** 101+ pass, including these new suites:
  - Concurrency
  - Tap hit test & world scale
  - Float mode
  - Blow mechanics
  - Soap pop pitch
  - Soap shelf
  - Breath cycle
  - Updated achievements and spawn balance tests
- **Checked that the tests can fail:** with the coin-write lock removed, the race test fails.
- **Compose UI tests** (`SettingsScreenUiTest`): updated and compiling. Not run in this pass.

## Main journeys (emulator)

| # | Journey | Result |
|---|---|---|
| 1 | First launch: dark splash, opens straight into Float with bubbles already on screen | PASS (`final/q01`, `s3-06`) |
| 2 | Tap pops on touch-down; "pops today" counts; per-day reset | PASS (unit test for the reset) |
| 3 | Hold to blow → bubble grows above the finger → floats away; hint shown once | PASS (`s3-07`) |
| 4 | Breathe 1 min: phases, tap pop in the pop window, auto-pop, End, summary chip | PASS (`s5-*`) |
| 5 | Jar → Soap Shelf → select today's soap ("In use") → close (✕ and Back) | PASS |
| 6 | Jar fills → next soap unlocks, auto-selected, "New soap" chip | PASS (`s4-04`) |
| 7 | Rewarded "+50 drops" (test ad): reward arrives on close, jar 8 → 58 | PASS |
| 8 | Menu: Float / Challenge / Soap Shelf / Settings; Back returns to Float | PASS (`s6-01`) |
| 9 | Settings: toggles switch at once; cut sections gone; Supporter Pack shown; opaque | PASS (`final/q03`, `s6-02`) |
| 10 | Challenge: level plays, time's up → Continue (coins) resumes the same level (+15 s), Restart replays the same level | PASS (`final/q07`) |
| 11 | Leave the app mid-Float → return: carries on, no pause menu | PASS after fix |
| 12 | Leave the app mid-Challenge → return: paused with Resume/Restart/Home | PASS |
| 13 | Back on every screen: Float exits; menu, shelf and Settings close; Challenge pauses/resumes | PASS |
| 14 | Release (R8) build: launch 1.4 s, pops, shelf, Settings, menu, no crash | PASS |

## Bugs found and fixed in this pass
1. Returning to the app during Float showed the Challenge pause menu (RESUME / RESTART / HOME). Float now resumes quietly.
2. The jar button and the shelf ✕ had no TalkBack label. Labels added.
3. A brand-new player couldn't see the Breathe button until they had blown a bubble. Both now show.
4. The shelf title overlapped the status bar when opened from the menu. Inset added.
5. Settings was see-through over the menu. It is now opaque.
6. The do-nothing Prism power-up could still spawn in Challenge. It no longer spawns, and the website claim was removed.

## Promise check (rule 4)
Every claim in the store listing maps to code: see `play-listing-assets/hushpop/listing.md`. These in-app texts were also checked:
- "Hold anywhere to blow your own bubble"
- "Free for everyone today"
- "it never resets"
- "Each pop adds a drop"
- "+50 drops · watch a short video"
- "Removes every ad for good, unlocks the Moonlit Pearl soap, and bonus videos pay out instantly"
- "Challenge has 20 timed levels"

## Not tested / needs the owner
- **On a real device:**
  - The sound levels by ear (listening page: `docs/relaunch-qa/sounds/listen.html`).
  - 60 fps on a low-end phone.
- **Supporter Pack purchase:** a real purchase needs a licensed tester account on a device.
- **Clearing a Challenge level by hand:** automated taps are too slow. The completion logic is unit-tested, and a run reached a best score of 1584.
- **Interstitial pacing on live ads:** debug test ads returned "no fill" for interstitials.
- **TalkBack:** a full walk-through with the screen reader.
