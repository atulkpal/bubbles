# Bubbles relaunch: design spec (draft for owner approval)

**Date:** 24 Nov 2026 · **Status:** APPROVED by owner 24 Nov 2026 (title: Hushpop; Challenge keeps levels 1–20)
**Based on:** `docs/audit-2026-11-24/` (reports 04 creative and 05 market) · **Ships after:** v1.3 (`fix/v1.3-stop-the-harm`)
**Budget:** ₹0. Growth is organic only, with no paid ads.

---

## 1. One line

**A calm soap-bubble toy.** Real-looking, wobbly, rainbow soap bubbles that pop the instant you touch them, with soft wet sounds. Hold your finger down to blow your own. An optional Breathe mode finally delivers the "Pop · Breathe · Repeat" promise.

## 2. What stays the same
- **Package:** `com.ashwathai.bubbles`, so existing installs, the Remove Ads purchase and the store listing history carry over.
- **Technology:** Kotlin + Jetpack Compose, no new engine.
- **Carried forward:**
  - Spawning, splitting and chains.
  - Particles, `SoundManager`, `MusicManager` and DataStore.
  - Achievements.
  - LevelPlay ads and Play Billing.
- **Privacy:** zero cloud, no accounts, works offline.

## 3. New name and look

| Item | Proposal |
|---|---|
| Store title (≤30) | **Hushpop: Bubble Pop & Relax** (27 chars). Backup: **Pop Haven: Bubble Pop & Relax**. Free trademark check (ipindiaonline.gov.in + USPTO) before switching. |
| Icon | One iridescent soap bubble on deep violet. No text. |
| Palette | Night room: background `#0E1022 → #1C1B3A`, one warm lamp glow, soft-white text. **No gold, no serif.** |
| Type | One rounded sans (Nunito or Quicksand, bundled offline). |
| UI | Minimal: a small "pops today" pill at the top and the jar in a corner. Everything else is bubbles. |
| Sound | Wet "plip" pops pitched by bubble size, chained pops forming a pentatonic phrase, and a rain-on-glass or night ambience bed. |

The "Midnight Glass" luxury design system (`ui/theme/luxury/*`) is replaced by a new **"Soap"** token set (`ui/theme/soap/*`). The AGENTS.md design-system rule is updated to point to it.

## 4. Modes

| Mode | What it is |
|---|---|
| **Float** (default, opens straight in) | Endless bubbles. No timer, no fail, no score shouting. This is today's Zen, retuned: fewer, slower, larger bubbles rising from the bottom. |
| **Breathe** (optional toggle) | Guided rhythm: a bubble inflates for 4 s (inhale), drifts up (exhale), then you pop it. Sessions of 1, 3 or 5 minutes. |
| **Challenge** (optional, small icon) | Today's Adventure levels 1–20, kept as-is, with no timer under 30 s. Levels 21–100 are cut. |
| **Soap of the day** | Replaces the Daily Challenge. A soap picked by a date seed, so it really is the same for everyone. |

**Cut:**
- Levels 21–100.
- The magnet, ticking-bomb, chaos and ghost bubbles (the tutorial promised features that don't exist).
- Prestige and the power-up upgrade tree.
- The "Power-up guarantee" ad button.
- The banner during play.

## 5. The two touch actions
1. **Tap to pop:**
   - The bubble pops as the finger touches down, in the same frame.
   - It hits the topmost bubble, with a little extra touch area.
   - Nothing smaller than 56 dp.
2. **Hold to blow:**
   - Hold anywhere and a bubble inflates under your finger with a breathy rising tone.
   - Let go and it floats away.
   - Its size depends on how long you held.

## 6. Feel checklist (must pass before release)
- [ ] Bubbles grow in with a slight spring. While idle they wobble (radius ±3%), and the film sheen slowly rotates.
- [ ] A popping bubble squashes for 40 ms toward the finger, the film tears from the touch point, and droplets, curling film shards and a faint ring fly out.
- [ ] 4–6 pop sound variations, pitched by size, plus a pentatonic note that rises on quick pops and resets after 1.5 s.
- [ ] Haptics:
  - A light tick for small pops, a click for big ones and a soft double for chains.
  - They respect the system haptics setting.
- [ ] No screen shake in Float or Breathe, and reduced-motion is respected.
- [ ] All sizes and speeds in dp, so the game is the same on every phone.
- [ ] 60 fps on a low-end phone (emulator gfxinfo + the cheapest real device available). The soap-film shader has a simpler fallback.
- [ ] Sessions end kindly ("Nice. 84 pops today."), with no fail sound in calm modes.

## 7. Reason to come back: the Soap Shelf
- Popping fills a **jar**. A full jar unlocks a new **soap**: a film colour, a pop sound and a background together.
- **Starter soaps:**
  - Lavender Dusk
  - Monsoon Glass
  - Chai Steam
  - Firefly Night
  - Sea Glass
  - Marigold
- Coins become **soap drops**. Existing coin balances convert, so no player loses progress.
- **Kind streak:** a missed day pauses the streak and never resets it.
- Achievements are kept and renamed to fit.

## 8. Money (no forced ads)
- **No ads in a player's first 3 sessions.** No banner during play, and never an ad on app open or return.
- **Rewarded ads, opt-in only:**
  - "Fill the jar faster".
  - "Try a locked soap for today".
  - A Challenge continue.
- **Interstitials:** Challenge mode only, with the v1.3 pacing.
- **One-time purchases (set per region, not auto-converted):**
  - **Supporter Pack** (₹99 India / $2.99 US, ₹49 for the first 2 weeks): removes ads, adds one exclusive soap, and grants rewarded bonuses automatically. Existing "Remove Ads" buyers get it free.
  - **Soap packs** (₹49) only once retention is proven (D7 ≥ 8%).

## 9. Store page and free launch
- New title and icon. Run the **free Play store listing experiment** (old vs new icon) first.
- 8 screenshots taken from the real app, plus a 20 s video of a real pop recorded on the emulator.
- A Hindi listing.
- The short description and full description use the search words: stress relief, relaxing, ASMR, satisfying, anti stress, bubble pop, offline.
- Every claim must map to code (QA playbook rule 4).
- An **in-app review prompt** after the 3rd jar fill.
- **Short video:** one satisfying-pop Reel or Short a day through the existing sheet-autopost pipeline (free), plus posts on r/AndroidGaming and r/ASMR, where self-promotion rules allow.
- **Targets:** 300–500 installs by week 4, 3–5k by month 3. Count only installs where the player comes back the next day (D1 ≥ 30%).

## 10. Build order (each step builds, tests and is reviewed before the next)

| # | Step | Approx. |
|---|---|---|
| 1 | dp-based world plus a pure-Kotlin engine module, pop on touch-down, topmost hit test (engine review option B) | 3–4 days |
| 2 | Soap bubble renderer (film sheen, wobble, tear-pop, droplets) and the "Soap" design tokens | 4–5 days |
| 3 | Float as the default mode, hold-to-blow, new pop sounds and ambience | 3–4 days |
| 4 | Soap Shelf, jar, streak, soap of the day, coin conversion | 3–4 days |
| 5 | Breathe mode | 2 days |
| 6 | Cut the old features; Challenge = levels 1–20; ad and purchase changes; first-3-sessions rule | 2–3 days |
| 7 | Full QA playbook pass on the emulator, store assets, video, listing text | 2–3 days |

**Total: about 3–4 weeks.** Each step gets unit tests (the AGENTS.md rule) and, for screens, a Compose UI test.

## 11. Open questions for the owner
1. Title: **Hushpop** or **Pop Haven**?
2. Keep the Challenge track (levels 1–20) in the relaunch, or go calm-only?
3. Is there a cheap real phone to test the 60 fps target, or emulator only?
