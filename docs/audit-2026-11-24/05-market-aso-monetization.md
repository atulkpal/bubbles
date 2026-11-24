# 05: Market, ASO and Monetization Audit

**Date:** 2026-11-24
**Scope:** Why Bubbles (`com.ashwathai.bubbles`, v1.2 / versionCode 3) has no users on Google Play, and what a zero/low-budget relaunch that earns money should look like.
**Method:** Read-only review of the repo (listing script `scripts/update-play-listing.mjs`, the other Play scripts, `play-listing-assets/`, `website/`, `LevelPlayAdManager.kt`, `AdConfig.kt`, `BillingManager.kt`, `MainActivity.kt`, `GameModels.kt`, `build.gradle.kts`, `libs.versions.toml`, `releases/`), the owner's earlier Jump Droid audit and TradeLab ad research, plus web research. No scripts were run and no credentials were used. **The public Play page did not render for automated fetching (IN and US storefronts)**, so install count, rating, category and Play's "similar apps" are not verified here. The first step of the relaunch is to export them from Play Console (section 8).

---

## 1. Snapshot: what the store listing is made of

| Field | Value (from `scripts/update-play-listing.mjs` and `play-listing-assets/`) | Problem |
|---|---|---|
| Title | **Bubbles** | One of the most crowded single words on Play (bubble shooters, kids' bubble apps, wallpapers). Ranks for nothing. |
| Short description | "A luxurious bubble popping game — 100 levels, star ratings, zen mode" | "Luxurious" is not a search term. The useful words "stress relief", "relaxing", "ASMR", "satisfying", "anxiety" are missing. |
| Full description | Feature list ("Midnight Glass", "spring physics", "prestige progression") | Written for the developer. "Stress" appears **zero** times, although the README calls the game a "stress buster". |
| Video | `video: ""` | No promo video. For a "satisfying" game, the video is the main conversion asset. |
| Icon | `icon-512.png`: cartoon glossy balls, a big "BUBBLES" wordmark and a cartoon hand | Looks like a generic bubble-shooter icon. The text is unreadable at 48 dp. It does not match the in-app "Midnight Glass" style. |
| Screenshots | 4 portrait images + 2 feature graphics, all AI-generated marketing art | See section 2, #3: they show UI that **does not exist** and a **test ad**. |
| Feature graphic | "TAP BUBBLES TO BLAST!", bombs, stars | Fine as art, but again cartoon-arcade, not the real product. |
| Download size | APK 11.6 MB (`releases/bubbles-v1.2-code3.apk`), AAB 15.1 MB | OK. Two WAV music files (3.2 MB) in `res/raw` could become ~0.4 MB as OGG. Low priority. |
| Rating / reviews | Unknown (not fetchable). No `ReviewManager` in code, so there is no in-app review prompt. | Without at least ~10 to 15 ratings, Play shows no star score. |
| Analytics | **None.** No Firebase or other analytics SDK in `build.gradle.kts`. | The owner cannot see retention, funnels or ad views per user. |
| Marketing history | 18 commits in total. The only "marketing" commits (2026-08-28) added listing assets and scripts. | No record of any outreach. |
| Developer account | Same account as a kids' learning app, Jump Droid (action) and TradeLab (finance). | Play's classifier gets mixed audience signals (the Jump Droid audit found Play recommending toddler apps next to it). |

---

## 2. Diagnosis: why there are no users (ranked)

### #1: No distribution ever happened (critical)
Same root cause as Jump Droid. Play does not hand organic installs to a new, unrated, zero-velocity app in a crowded genre. Nothing in the repo records a Reddit post, a reel, a press email or a paid test. **Zero installs is the expected result of zero distribution**, not proof that the game is bad.

### #2: Nobody can find it by search (critical)
- "Bubbles" competes with tens of thousands of listings, including 100M+ bubble shooters (e.g. *Bubble Shooter Rainbow*, 100M+ installs) and kids' apps. A new app with one generic word ranks on page 20+.
- Play indexes the **title (30 chars), short description (80) and full description (4,000)**. None of them contains the words people in this niche search: *stress relief, anti stress, relaxing games, satisfying, ASMR, calm, anxiety relief, pop it, bubble wrap, fidget, tap game, offline game*.
- English only. No Hindi listing, although the owner's home market is India (a translated *listing* is cheap even if the in-game text stays English).

### #3: The store page misrepresents the product (high: conversion, reviews and policy)
I compared the screenshots against the code:
- `phone-02-gameplay.png` shows a **"MOVES" counter**. There is no moves mechanic anywhere in the code (`grep MOVES` finds nothing).
- `phone-02-gameplay.png` and `phone-03-zen.png` both show an **ironSource "Demo App" test banner** at the bottom of the phone mock-up. That is a test ad in a store asset: it looks unprofessional and advertises the ad load.
- The art is bright cartoon-arcade (cartoon hands, explosions, "FAST, FUN & ADDICTIVE!"), but the app is a dark "Midnight Glass" luxury design with gold accents and serif type. Users who install for the cartoon look and get a dark glass UI uninstall; users who want a calm, premium toy never tap the cartoon icon.
- Google Play's metadata policy forbids screenshots that misrepresent the app. Mock-ups that show features which don't exist are a review-rejection and 1-star-review risk. QA-PLAYBOOK rule 4 ("every promise must be real") applies to store art too.
- "Fast, fun & addictive" and "Tap bubbles to blast" position it as an arcade game, while the strongest market for a tap-to-pop game is **stress relief / ASMR**. The page sells neither clearly.

### #4: Positioning is split three ways (high)
The listing tries to be a luxury app, a 100-level arcade game with bosses and prestige, and a zen stress toy all at once. The players who search for "stress relief bubble pop" want **instant, no-fail, satisfying popping with great sound and haptics**. Timers, bosses and Game Over screens are friction for that audience. The players who want levels and bosses already have Bubble Shooter and match-3. The winner in this niche (JindoBlu *Antistress*, 370M+ installs) is a pure toy box with no fail state. Recommendation: lead with **Zen / "pop to relax"** as the first screen and the store promise, and keep Adventure as the secondary "challenge" mode.

### #5: The monetization design will hurt retention and carries policy risk (high, once traffic arrives)
Code-verified details:
- **A rewarded ad is forced as an interstitial between levels.** `MainActivity.kt:356-375` calls `LevelPlayAdManager.loadAndShowRewardedAd(adType = "level_interstitial", ...)` automatically when `waitingForAd` is true (levels 3, 6, 9, then every 2nd from 11, then **every level from 21**, per `EconomyConfig.shouldShowAdBetweenLevels`). Rewarded formats must be opt-in; forcing an unskippable rewarded video is against Unity/LevelPlay rewarded guidelines and the Better Ads standard, and it is the kind of thing networks flag.
- **Possible soft-lock:** in that flow the next level starts only from `onAdFailed` or `onUserEarnedReward`. If the user closes the rewarded ad before the reward (allowed by many creatives), `onAdClosed` clears the callbacks and neither fires, so the player can be left on an empty Level Complete state. Flagged for the runtime/code auditors to confirm.
- The real interstitial `maybeShowLevelCompleteInterstitial()` is **dead code** (never called). The only true interstitial is the **foreground/resume interstitial** (`maybeShowForegroundInterstitial`, on every app start after 4 h). Ads on app open/resume are the most disliked placement and the riskiest under Play's disruptive-ads policy, and they hit the first session of every new user.
- **Ad cap of 5 per day shared by all rewarded placements**, held only in memory (`adsShownToday` is not persisted, so it resets on process death; and it is also counted against forced "level" ads). A player who wants to watch ads for coins runs out; a player who doesn't want ads is forced to watch them.
- **Only one demand source** (Unity Ads adapter, `libs.versions.toml`). Low fill and low eCPM at small scale. Also note from the TradeLab research: the ironSource Ads SDK stops serving on 31 Dec 2026, so Unity Ads is the single bidder from 2027 unless more are added.
- No LevelPlay Ad Quality SDK. TradeLab already saw no-close-button playables and auto-redirect ads from Unity demand; in a "relaxing" game those are review poison.
- **Remove Ads is $1.99 converted** (`create-inapp-product.py`: `to_money("1990000", "USD")` then `convertRegionPrices`). Play's auto-conversion typically puts that around ₹170 to ₹190 for India, which is above the ₹49 to ₹99 impulse band that works in India.
- The "Remove Ads" purchase also auto-grants every rewarded reward (`loadAndShowRewardedAd` grants immediately when `adsRemoved`). That is a generous, sellable perk, but it is not stated anywhere on the listing or in the purchase UI copy. Say it, it is the strongest reason to buy.

### #6: No feedback loop (medium)
No analytics, no in-app review prompt, no share button, no Play Games/leaderboard. The owner cannot tell whether the problem is traffic, listing conversion or D1 retention.

---

## 3. Competitive landscape (tap-pop, pop-it, ASMR, antistress)

| Game (Play) | Scale (approx.) | Title pattern | Look | Monetization | Lesson |
|---|---|---|---|---|---|
| **Antistress - relaxation toys** (JindoBlu) | 370M+ installs, ~980K ratings, 4.5 stars, est. 18M MAU, est. $1M+/month (AppGoblin) | Brand word + "relaxation toys" | Bright, realistic 3D toys; screenshots are real gameplay of each toy | ~68% ads / ~32% IAP (AppGoblin estimate); mediation AppLovin MAX + AdMob + Unity; ~19 ad networks | Keyword-heavy title, many toys, no fail state, heavy ad stack. |
| **Daily Bubble** (Big Cake) | 10M+ installs, 4.9 stars, ~59K reviews | Brand + implied "daily" habit | Soft pastel, calm, collectible postcards | Ads + IAP | Calm bubble popping plus a collection meta works at scale. Closest analogue to Bubbles' Zen mode. |
| **Bubble Shooter Rainbow** (Blackout Lab) | 100M+ installs | "Bubble Shooter" + flavour word | Bright cartoon | Interstitial + rewarded + IAP | The word "Bubble" in titles is owned by shooters. Don't compete on it alone. |
| Pop-it / fidget clones (many: "Pop It Fidget", "Pop It ASMR 3D", "Fidget Toys 3D") | 1M to 50M each (2021 to 2022 wave) | Literal keyword stacks: "Pop it - Fidget Toys ASMR" | 3D silicone toys | Rewarded-heavy, cosmetic toy unlocks via ads | Shows the search vocabulary. Note "POP IT!" is reported to be a Buffalo Games trademark for the fidget toy, so **do not use "Pop It" in the title** [uncertain, verify before use]. |
| Small "Zen Bubble" / "BubbleZen" Android apps | hundreds of installs, 0 ratings | Generic | Pastel | Ads | Same trap as Bubbles: generic name, no marketing. |

What the winners share: (1) a title that is mostly **search keywords**; (2) an icon with **one big, tactile object** (a single glossy bubble or toy, no text); (3) screenshots that are **real gameplay, full-bleed, with 3 to 5 words of caption**; (4) **no fail state** in the core toy; (5) **rewarded-first** monetization plus cosmetic unlocks, with heavy mediation; (6) ASMR sound and haptics as the headline feature.

Bubbles' real advantages: genuinely premium glass rendering, 13 sound clips plus music, distinct haptics, small download (12 MB), offline, no account. These are exactly what the "satisfying/ASMR" audience values, but the listing hides them.

---

## 4. Name, keywords and listing copy

### Name candidates (title field, max 30 chars)
The package name stays `com.ashwathai.bubbles`; only title, icon and brand change. Searches on 2026-11-24 found no Play/App Store app with these exact brand names. This is not a legal clearance; run a free check on the Indian TM registry (ipindiaonline.gov.in) and USPTO before committing.

| Option | Title (chars) | Notes |
|---|---|---|
| **A (recommended)** | **Pop Haven: Bubble Pop & Relax** (29) | Ownable brand "Pop Haven", plus "bubble pop" and "relax" keywords. No app with this name found. |
| B | Bubble Pop Zen: Stress Relief (29) | Highest keyword density, weakest brand. Small apps named "Zen Bubble"/"BubbleZen" exist, but the full phrase differs. |
| C | Glasspop: ASMR Bubble Popping (29) | Fits the "Midnight Glass" look. "ASMR" is a strong niche keyword. |

Avoid: **"Calm"** as a leading word (Calm.com holds CALM marks for wellness apps), **"Pop It"** (fidget-toy trademark, see above), "Popwell" (an iOS app with that name exists), "Headspace", "Bubble Shooter" (wrong genre expectation).

### Keyword set (put the top ones in title/short description; the rest naturally 2 to 4 times in the full description)
Primary: bubble pop, stress relief, relaxing game, anti stress, ASMR, satisfying game.
Secondary: calm, anxiety relief, bubble wrap, popping game, tap game, offline game, zen, relax, mindful, fidget, no wifi game, sleep, breathe.
Hindi listing (hi-IN) terms: बबल पॉप, तनाव मुक्ति (stress relief), रिलैक्स गेम, ऑफलाइन गेम.

### Short description (80 chars max)
"Pop glossy bubbles to relax. ASMR sounds, haptics, offline. Stress relief in 1 tap" (80 chars; trim "in 1 tap" if Play counts differently).

### Full description (outline, first 2 lines matter most)
> Feeling stressed? Pop glass bubbles and breathe. Pop Haven is a satisfying ASMR bubble popping game for stress relief and calm, with real pop sounds and phone haptics on every tap.
>
> - **Zen mode: no timer, no fail.** Endless bubbles to pop whenever you need a break.
> - **ASMR pop sounds and haptics** (13 pop, crack and chime sounds; distinct vibration for each bubble).
> - **Calm music** (or switch it off).
> - **Daily Challenge** for a 2-minute habit.
> - **Adventure: 100 levels** when you want a challenge, with special bubbles (ice, bomb, rainbow, magnet, ghost).
> - Unlock glass skins and themes with coins.
> - **Offline, 12 MB, no account, no wifi needed.**
> - One small purchase removes all ads and gives every bonus for free.

Every line above maps to existing code (Zen mode, SoundManager clips, MusicManager, Daily, 100 levels, skins/themes, Remove Ads auto-grants rewards). Do not add "breathing exercises" or "sleep sounds" claims unless that is built.

---

## 5. Visual assets plan

**Icon:** one large, dark-glass bubble with a gold rim and a crack/pop burst, on the Midnight Glass navy background. No text, no hand. Test 2 or 3 versions with a Play Store Listing Experiment (free, built into Play Console).

**Screenshots (real captures from the app, then framed, 8 slots):**
1. Zen mode mid-pop, burst particles visible, caption "Pop to relax".
2. Close-up of the glass bubble rendering, caption "Satisfying ASMR pops".
3. A pop chain/combo, caption "Feel every pop" (haptics icon).
4. Theme/skin grid, caption "Unlock glass skins".
5. Special bubbles (ice crack, bomb, rainbow), caption "7 special bubbles".
6. Adventure level with stars, caption "100 levels when you want a challenge".
7. Daily Challenge, caption "A 2-minute daily break".
8. "Offline · 12 MB · No account" card.

Rules: **no test ads, no features that don't exist, one style (the real one)**. Remove the current four screenshots and both feature graphics. Re-capture with debug ads hidden.

**Promo video (YouTube, 15 to 30 s):** real screen capture with real audio; first 3 seconds = a satisfying pop chain with sound. The same clip feeds Reels/Shorts.

**Category:** Casual (if it is currently Arcade or Puzzle, move it). Tags: Relaxing, Casual, Offline, Satisfying (pick from Play's tag list).

---

## 6. Monetization: realistic numbers and the model

### eCPM ranges (rough, vendor sources)
| Format | India Android | US / Tier-1 Android |
|---|---|---|
| Rewarded video | $1 to $4 (one report: $2.43) | $10 to $20 |
| Interstitial | $0.5 to $2 | $8 to $15 |
| Banner | $0.03 to $0.10 | $0.3 to $1 |

Mintegral's India report cites a ~22x gap between global and Indian rewarded eCPMs. Sources: [Playio 2026](https://blog.playio.co/mobile-game-ecpm-benchmarks-2026), [vmobify](https://vmobify.com/blog/casual-game-marketing-strategy), [Outlook Respawn / Lumikai](https://respawn.outlookindia.com/amp/story/gaming/gaming-news/lumikai-india-gaming-market-1-5-billion-rmg-ban-2025), [IANS via Investment Guru](https://investmentguruindia.com/newsdetail/india-accounts-for-18-pc-of-global-android-game-downloads179265), and the owner's Jump Droid audit.

### Plausible revenue (honest)
Assumptions for a well-tuned casual relax game: D1 ~25%, D30 ~4%, so DAU ≈ 8 to 12% of cumulative installs after a few months; ARPDAU India $0.003 to $0.008 (rewarded-first), Tier-1 $0.03 to $0.06; IAP conversion 0.3 to 1% of installs.

| Cumulative installs | Steady DAU | Ads/month (India-heavy) | Ads/month (50% Tier-1) | Remove-Ads/supporter IAP (one-off, total) |
|---|---|---|---|---|
| 1,000 | 80 to 120 | **$7 to $30** (₹600 to ₹2,500) | $40 to $120 | 3 to 10 sales ≈ ₹300 to ₹1,000 |
| 10,000 | 800 to 1,200 | **$70 to $300** (₹6k to ₹25k) | $400 to $1,200 | 30 to 100 sales ≈ ₹3k to ₹10k |
| 100,000 | 8k to 12k | **$700 to $3,000** (₹60k to ₹2.5L) | $4k to $12k | 300 to 1,000 sales ≈ ₹30k to ₹1L |

Plain reading: **at 1k installs the game earns pocket money; at 10k it pays for a domain and some ads; it only becomes real income near 100k installs, or with meaningful Tier-1 traffic.** Tier-1 players are worth ~10x Indian players, which is why the English listing, the US-friendly "ASMR/stress relief" positioning and short-video reach outside India matter.

### Recommended model (rewarded-first)
1. **Remove the forced "level_interstitial" rewarded ad now.** Replace it, if wanted, with a real interstitial at natural breaks: never in session 1, never in Zen, at most once every 4 levels and at least 3 to 4 minutes apart, never on app resume.
2. **Remove the foreground/resume interstitial.**
3. **Rewarded placements the player chooses:** "Double coins" on level clear and daily, "+15 s continue", "Free skin trial", "Unlock today's theme", plus a coin chest every 30 minutes in Zen. Raise the daily cap to ~15 to 20 and persist it. Rewarded is the format that does not hurt retention.
4. **Banners:** keep at most one, on the home screen only, and never in Zen. In India they earn cents and they break the "calm" promise.
5. **Mediation:** add bidders before scaling (from the TradeLab research: InMobi, Liftoff/Vungle, Moloco; consider Google AdMob bidding and Mintegral for Asia) and add the LevelPlay **Ad Quality SDK**. Ask Unity to disable hidden-close/auto-redirect, as already drafted for TradeLab.
6. **IAP price points (₹, set per region, not auto-converted):**
   - **Supporter Pack (non-consumable): ₹99** India, $2.99 US. Removes all forced ads, grants all rewarded bonuses automatically (already coded), plus one exclusive gold-glass skin. Run a ₹49 launch price for the first 2 weeks.
   - **Glass Theme Pack (non-consumable): ₹49 each** (3 premium themes per pack), only after D7 ≥ 8% and DAU ≥ 500.
   - **Coin bundles (consumable): ₹19 / ₹49 / ₹99** — only if coins gate cosmetics the player wants. Low priority; coin IAP rarely sells in a no-fail relax game.
   - Optional later: **"Calm Pass" ₹49/month** with a new theme + music track each month. Only once there are thousands of DAU.
7. **Instrument** (Firebase Analytics, free): D1/D7, rewarded views per DAU, opt-in rate, IAP conversion, by country.

---

## 7. Go-to-market plan (zero to low budget)

**Positioning:** "The most satisfying bubble pop on your phone. Pop, breathe, relax: offline, 12 MB."

### Phase 0: fix the funnel (weeks 1 to 2)
- Export Play Console data (section 8).
- New title, icon, real screenshots, promo video, hi-IN listing, Casual category, description rewritten.
- Make Zen the first-run experience (first pop within 3 seconds of launch, no ads in session 1).
- Ship the ad changes in section 6 plus the in-app review prompt (after the 3rd Zen session or 5th level clear) and Firebase Analytics.
- Owner approval needed for the version bump (AGENTS.md rule).

### Phase 1: short-video engine (weeks 2 to 10)
The owner already runs a sheet-driven autopost pipeline (Instagram/Facebook Reels + YouTube Shorts with an approval tick-box). Make a dedicated channel for the game (one more page on the existing shared Meta app login, per the owner's setup), and feed it from a library of real screen captures.

| Content | Length | Cadence | Notes |
|---|---|---|---|
| "Satisfying pop" loops (real audio, no voiceover) | 7 to 15 s | 1/day | The ASMR/satisfying format is native to Reels/Shorts. Loop-able ending. Caption: "Pop the bubbles. Breathe." |
| "Try not to pop" / "Which skin?" polls | 10 s | 2/week | Drives comments and saves. |
| "Level 100 boss" / near-miss clips | 15 to 20 s | 1/week | For the challenge audience. |
| Hindi caption variants | same clips | 2/week | Same video, Hindi on-screen text, for the India feed. |

Link: Play URL with a `referrer` UTM in bio and description, so Play Console shows installs per channel.
Also once each: r/AndroidGaming (self-promo rules), r/playmygame, r/IndianGaming, r/ASMR-adjacent communities only where self-promo is allowed; Product Hunt is not a fit.

### Phase 2: small paid test (week 6 or later, ₹5,000 to ₹10,000)
Google App Campaign (installs) or boosting the best-performing reel, India + one Tier-1 test (e.g. UK or Canada with ₹3,000). Goal is to measure CPI and D7 on cold traffic, not to scale. India casual CPI is often ₹5 to ₹20; Tier-1 is much higher. If D7 < 5%, stop and fix the product. If D7 ≥ 10% and ARPDAU × lifetime beats CPI in Tier-1, scale slowly.

### Install targets
| By | Cumulative installs | Ratings | D1 / D7 | Trigger if missed |
|---|---|---|---|---|
| Week 4 | 300 to 500 | 15+ (star score shows) | ≥ 25% / ≥ 8% | Re-test icon and first 3 seconds of reels |
| Month 3 | 3,000 to 5,000 | 60+ | ≥ 25% / ≥ 8% | If reels get views but not installs, the listing is the problem |
| Month 6 | 10,000 to 20,000 | 150+ | ≥ 30% / ≥ 10% | If missed with a fixed funnel, the core loop is the problem |

One reel going viral is the upside case. The satisfying/ASMR niche has a much better chance of that than most genres.

### Web portals (Poki, CrazyGames, itch.io)
- **The genre suits the web well:** instant, one-tap, no account, short sessions, mobile-friendly. CrazyGames and Poki carry many "satisfying"/idle/clicker games.
- **The tech does not:** the game is Kotlin + Jetpack Compose for Android. A web build means a rewrite (e.g. a small HTML5/Canvas or Phaser port of Zen + Daily only, perhaps 2 to 4 weeks), or Compose Multiplatform for web (Wasm), which is still maturing and heavy for portal size limits (CrazyGames: ≤ 50 MB initial download, ≤ 20 MB for the mobile homepage).
- Terms: CrazyGames pays an ad revenue share through its SDK after a QA'd Full Launch ([docs](https://docs.crazygames.com/requirements/intro/)); Poki is selective, reportedly 50/50 on Poki traffic and asks for web exclusivity [third-party, unverified]. itch.io is free and good for backlinks but brings little casual traffic.
- **Recommendation:** not now. Revisit at month 3 only if the Android relaunch shows good retention; then port Zen mode only as a light HTML5 build and submit to CrazyGames first (open submission), Poki second.

---

## 8. Data the owner should pull before acting
1. Play Console → Statistics: store-listing visitors/week and visitor-to-install conversion. Near-zero visitors = traffic problem; hundreds of visitors with < 15% conversion = listing problem.
2. Acquisition by source (Search, Explore, external) and top search terms.
3. Installs, uninstalls, D1/D7 retention, rating count.
4. Android vitals (crash/ANR rate).
5. LevelPlay dashboard: fill rate, eCPM per format, impressions per DAU by country.
6. Current category, tags and the "similar apps" Play shows on the live page (I could not fetch it).

---

## 9. Priority checklist
| # | Action | Effort | Impact |
|---|---|---|---|
| 1 | Remove the screenshots with the fake "MOVES" UI and the test "Demo App" banner; re-capture real screenshots | S | High (policy + conversion) |
| 2 | Stop forcing a rewarded ad between levels; remove the resume interstitial; check the closed-early soft-lock | S | High (policy + retention) |
| 3 | New title + short/full description with keywords; hi-IN listing | S | High (search) |
| 4 | New icon (single glass bubble, no text) + Store Listing Experiment | S | High (conversion) |
| 5 | 20 s promo video from real capture | S | High |
| 6 | Zen-first first session; in-app review prompt; Firebase Analytics | M | High |
| 7 | Supporter Pack at ₹99 (₹49 launch), regional pricing set by hand | S | Medium |
| 8 | Add 2 to 3 LevelPlay bidders + Ad Quality SDK | M | Medium (revenue from 2027) |
| 9 | Reels/Shorts channel on the existing autopost pipeline, 1 clip/day | M (ongoing) | Critical (traffic) |
| 10 | Paid CPI/D7 test, ₹5k to ₹10k | S | Medium (decision data) |
| 11 | Web port of Zen mode | L | Later, only if retention is good |

### Sources
- AppGoblin, Antistress - relaxation toys: https://appgoblin.info/apps/com.JindoBlu.Antistress
- APKMirror, JindoBlu Antistress: https://www.apkmirror.com/apk/jindoblu/antistress-relaxation-toys
- Play event pages for Daily Bubble / Bubble Shooter Rainbow (via search): https://play.google.com/store/apps/eventdetails/4829985295206597334
- Zen Bubble (Android) stats: https://appgoblin.info/apps/com.soorajV345.zenbubble ; BubbleZen: https://appgoblin.info/apps/com.ddb.bubblezen
- Popwell (iOS): https://apps.apple.com/us/app/-/id6755931457
- eCPM: https://blog.playio.co/mobile-game-ecpm-benchmarks-2026 ; https://vmobify.com/blog/casual-game-marketing-strategy ; https://respawn.outlookindia.com/amp/story/gaming/gaming-news/lumikai-india-gaming-market-1-5-billion-rmg-ban-2025 ; https://investmentguruindia.com/newsdetail/india-accounts-for-18-pc-of-global-android-game-downloads179265
- Web portals: https://docs.crazygames.com/requirements/intro/ ; https://sdk.poki.com/requirements ; https://www.summerengine.com/make-great-games/publish-an-html5-game
- Owner's internal references: `C:\Users\Atul\AndroidStudioProjects\Jump_droid\docs\audit-2026-11-23\05-market-aso-monetization.md`, `C:\Users\Atul\AndroidStudioProjects\TradeLab\docs\ads\AD_NETWORKS_RESEARCH_2026-11-24.md`
