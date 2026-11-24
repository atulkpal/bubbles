# 04: Creative Direction and Game Design Audit

**Date:** 2026-11-24
**Reviewer role:** Creative Director / Game Designer
**Branch read:** `feature/backlog-completion` (working tree, uncommitted changes included)
**Scope:** gameplay code (`domain/usecase/GameUseCases.kt`, `domain/model/GameModels.kt`, `ui/game/GameViewModel.kt`), drawing and UI (`MainActivity.kt`, `ui/theme/luxury/*`, `ui/settings/SettingsScreen.kt`), audio (`data/sound/*`, `res/raw/*`), store assets (`play-listing-assets/*.png`, `bubbles_ico-logo.png`, launcher icon), store copy (`scripts/update-play-listing.mjs`), `website/`, `LUXURY_DESIGN_PLAN.md`, `README.md`, `AGENTS.md`.
**Method:** I read the code and docs and looked at every store image and the launcher icon. I measured the audio files with ffprobe. I did **not** play a build. The runtime agent's screenshots (`docs/audit-2026-11-24/screenshots/rt-*.png`) did not exist when I finished, so every gameplay-feel finding below comes from code and constants. Each needs a 10-minute playtest to confirm before anyone acts on it. QA-Playbook rule 4 (every promise must be real) is applied in §8.

---

## 0. Verdict in one paragraph

Bubbles has a solid little engine (splitting bubbles, colour chains, bosses, power-ups, a decent particle and haptic layer) wearing **three identities that contradict each other**. The store sells a loud candy cartoon ("TAP BUBBLES TO BLAST!", a cartoon glove, a HUD with "MOVES" that the game doesn't have). The website and spec sell black-and-gold "luxury". The game itself shows opaque candy balls on a bright ocean gradient under a black-glass HUD with a serif font. And the tagline, "Pop · Breathe · Repeat", sells calm, while the Adventure mode is a stopwatch that ends with 8-second timers, bubbles crossing the screen in under a second, wind, gravity, bosses and an ad after every level. The name "Bubbles" can't be found in search. **The problem isn't missing features. The game has no single answer to "what does this feel like?"** The relaunch should choose calm and tactile, make the pop itself the product, and cut the stopwatch from the default experience.

## 1. Creative score: **3.1 / 10**

| Dimension | Score | Why |
|---|---|---|
| **Fun** (moment-to-moment) | 4 | Splitting (big, then 2 medium, then 4 small, then 8 tiny) is a good, readable loop. Same-colour chain pops, bomb blasts, a hit-stop of 80 ms on big pops, trauma shake, 3 randomised pop clips with ±12% pitch, 15 ms haptic ticks. But the pop fires on **finger-up** (`detectTapGestures`, MainActivity.kt:233), the bubble vanishes without any squash or tear, the "Pop!/CHAIN" text is placed at the wrong bubble and in the wrong units (§2.3), and 3 of the 13 sounds are never played. |
| **Clarity** | 3 | The goal ("pop every bubble before the timer ends") is told in a footnote on the start screen. In play, the HUD says **"Bubbles left: 0"** in accent colour at the start of every level (it counts bubbles on the board, not the bubbles still to come). The +0.5 s per hit timer refill is never explained. Bubble size and speed are in raw pixels, so the game is a different game on every phone (§2.4). |
| **Art** | 3 | The 6-layer bubble renderer is careful work, but it draws **opaque glossy balls, not bubbles** (body alpha 0.68 to 1.0, no transparency, no iridescence). Store art is AI-painted cartoon in a third style. The icon is a busy poster with the word "BUBBLES" in it, baked black corners and a white frame (§3). |
| **Audio** | 4 | Kenney CC0 SFX are clean and the pop variation is a good instinct. But the pops come from "impactGlass" (glass clinks, not wet bubble pops); `playCrack`, `playSplit` and `playBoss` are never called; combo pitch never rises; the music is two 32 s and 40 s synthesized pad loops at 22 kHz mono, mixed at -24 dB mean and played at 35% volume, so it's nearly inaudible and repeats about 10 times in a 5-minute session. |
| **Originality** | 2 | Tap-the-moving-bubble is one of the most crowded shapes on Play. Nothing in the store page or the first minute says why this one, and not the hundred "bubble pop / antistress" toys. |
| **Retention design** | 3 | There's plenty of content (100 levels, stars, achievements, daily), but the reasons to come back are thin. The Daily Challenge is not seeded (it's a random 60 s run, once a day), skins and themes are all free from day one (only Gold is locked, behind a Prestige button that has no requirement), and coins buy only 16 upgrade steps and continues. There's no streak, no collection and no "tomorrow there's something new". |
| **Identity fit** | 2 | "Luxury Midnight Glass" is the wrong identity for the stress-relief audience (§3.2), and it isn't even what the store shows. |
| **Overall** | **3.1** | Unweighted mean. A competent toy with no point of view. |

---

## 2. Core loop and moment-to-moment feel

### 2.1 What the loop actually is (from code)
- **Spawn:** every bubble enters from one of the four **corners**, off-screen, and flies toward a random point in the middle 60% of the screen at `baseSpeed × 0.7–1.3` px/s, then bounces off the walls, losing 20% speed per bounce (`GameUseCases.kt:105-218`, `220-268`). Bubbles pass through each other.
- **Split:** a normal bubble is level 2 (radius 100 px) or 3 (130 px). Each pop splits it into two children one size down until radius 40 px. **A level-2 bubble takes 7 taps; a level-3 takes 15** (average about 11). Special bubbles never split.
- **Chain:** a tap pops every overlapping bubble of the same colour (and rainbow chains any colour, bombs blast a radius). With a 9-colour palette, same-colour overlaps are rare, so chains mostly come from siblings that just split and haven't separated yet. This is a nice, hidden skill: tap the sibling fast and you get the chain.
- **Timer:** every successful tap adds **+0.5 s**, capped at the level's limit (`GameViewModel.kt:590`). So Adventure is really a "keep up at least 2 hits per second" rate check, with the timer as a buffer.
- **Level end:** every bubble in the level's spawn budget has spawned and the board is empty.

### 2.2 Difficulty curve (code finding, confirm in playtest)

| Level | Spawns | Approx. taps needed | Timer | Speed (px/s) | Notes |
|---|---|---|---|---|---|
| 1 | 6 | ~66 | 60 s | 300 | Gentle. Fine. |
| 3 | 8 + boss | ~90 + boss (5 hits, 2 minions per hit) | 50 s | 360 | **First boss on level 3**, before the player has met a single special bubble. |
| 5 | 10 + boss | ~110 | 40 s | 440 | Gravity and boss together. |
| 10 | 15 + boss | ~165 | 30 s | 650 | Wind + gravity + boss. |
| 20 | 25 | ~275 | 19 s | 950 | Needs a sustained 2+ hits/s for about 2 minutes on bubbles crossing a 1080-px screen in about 1 s. |
| 21+ | 26–55 | ~290–600 | 19 s → 8 s | 980 → 1400 | Ad after **every** level (`shouldShowAdBetweenLevels`). |
| 100 | 55 | ~600 | 8 s | 1400 | A reflex test. Not a stress buster. |

The curve rises fast in the curated levels and keeps rising. **Session length** at level 1 is about 45 to 60 s per level, and late levels are long tap marathons. For a game sold as "Pop · Breathe · Repeat", the default mode gets more frantic the longer you play. That's the opposite of the promise.

### 2.3 Feel bugs that undercut the juice (code findings)
1. **Pop on release, not on touch.** `detectTapGestures { onTap }` fires on finger-up, and the tap then waits for the next game-loop frame (max 3 taps per frame). A bubble game lives or dies on "I touched it and it burst". Use `awaitPointerEventScope`/`onPress` and pop on **down**, the same frame.
2. **Pop text lands in the wrong place.** The "Pop!/CHAIN xN/COMBO" text uses `bubbles.first().x/y` (the first bubble in the list, not the one you tapped) (`GameUseCases.kt:611, 623`; `GameViewModel.kt:618`), and `PopMessageView` puts pixel coordinates into `.dp` (`MainActivity.kt:2017`), which multiplies the position by screen density (2.6× to 3.5×). On most phones the text appears far from the tap and often off-screen. The game's main reward text is effectively invisible.
3. **Overlapping taps pop the bottom bubble.** The hit test uses `indexOfFirst` (`GameUseCases.kt:293`), but the bubble drawn last is on top. The player taps the front bubble and the one behind it pops.
4. **Screen shake is diagonal only.** X and Y use the same random value (`MainActivity.kt:892-893`), so the shake moves along one diagonal. It also runs in Zen.
5. **No squash, wobble, or membrane.** Bubbles are rigid circles. Spawn has no grow-in, the pop has no anticipation or tear, and idle bubbles don't breathe or wobble. Particles are 12 coloured dots plus one ring.
6. **Silent events.** `playCrack`, `playSplit`, `playBoss` and `vibrateBossHit` are defined but never called. Hitting a boss, cracking ice and splitting sound the same as a normal pop.
7. **Zen ends on the game-over sting.** Tapping the tick to end a calm Zen session plays `gameover.ogg` (`GameViewModel.kt:825-826`).

### 2.4 Pixels, not dp
`GameConfig` radii (40/70/100/130 px) and speeds (300 to 1400 px/s) are never scaled by screen density. The smallest bubble is a 80-px circle: about 30 dp on a 2.6× phone and **23 dp on a 3.5× phone**, below the 48 dp touch-target rule the project sets itself (AGENTS.md acceptance criteria). On a 720p budget phone (common in India), everything is larger and relatively slower. Difficulty and comfort change with the device.

### 2.5 The first 10 seconds today
App opens on a black-glass start screen. A breathing gold orb, "BUBBLES" in serif, "POP · BREATHE · REPEAT", a coin pill, CONTINUE · LEVEL 1, SELECT LEVEL, ZEN MODE, DAILY CHALLENGE, SETTINGS & UPGRADES, **POWER-UP GUARANTEE · WATCH AD**, a three-line rules paragraph, and a banner ad. The player taps Continue. The screen turns into a bright blue-to-cyan gradient with a glass HUD, a timer ring, and "Bubbles left: 0". Nothing happens for 2 seconds (the first spawn waits for `spawnInterval` 2.0 s), then a large glossy ball slides in from a corner. **The goal is never shown on the play screen, and the first thing it says is wrong.** The first touch is fine once a bubble arrives: it splits, which is satisfying.

---

## 3. Identity

### 3.1 Inventory

| Element | Current state | Assessment |
|---|---|---|
| **Name** | "Bubbles" | Generic; can't be owned, searched, or trademarked. Play search for "bubbles" returns thousands of shooters and pop toys. |
| **Tagline** | "Pop · Breathe · Repeat" | The best line in the project, and the game doesn't keep it. Nothing in play involves breathing; the default mode is a timer. |
| **Store screenshots** | AI-painted marketing posters with a fake phone. The "gameplay" shot shows a HUD with **"MOVES 0"**, hourglass-icon bubbles and the line "Bubble left: 6 - clear them all!", none of which exist in the app. Two shots show an ironSource **"Demo App · INSTALL"** test ad inside the phone. | Doesn't show the real game. That's a conversion problem (the installer meets a different game) and a Play policy risk (screenshots must show the actual app). The "Demo App" banner looks broken. |
| **Feature graphic** | `feature-graphic-wide.png` is 1795×876 (Play wants 1024×500). `feature-graphic-marketing.png` is portrait 941×1672, so it isn't a feature graphic. | Re-export at the right size. |
| **Icon** | `icon-512.png` is 1254×1254 RGB (not 512×512 with alpha) with **black corners and a white frame baked in**. A poster: 6 balls, a cartoon glove, and the word "BUBBLES". The adaptive launcher foreground uses the same poster shrunk to about 55% of the canvas on a `#0C0A09` background. | At 48 dp on a home screen the word is unreadable and the poster turns into noise. Icons shouldn't contain the app name. The baked corners will show as a black square inside the Play mask. |
| **In-game art** | Opaque glossy candy balls (Classic palette is red/purple/indigo/blue/teal/green/yellow/orange/pink) on a bright Ocean gradient (`#1A2980 → #26D0CE`) under a black-glass gold HUD, with Cormorant serif and system-serif pop text. | Three palettes in one frame. The balls read as billiard balls or gumballs. Nothing says "soap bubble". |
| **Website** | Black and gold luxury, "A Luxury Bubble Popping Game", "10 Adventure Levels". | A fourth look, and out of date (100 levels in code). |
| **UI voice** | "Splendid!", "Glorious!", "Luminous!", "Serene!", plus "BOSS DOWN!", "CHAOS!", "PHANTOM!", "TICK!" | Spa words and arcade shouts mixed together. |

### 3.2 Is "luxury midnight glass" the right identity?
**No.** Who downloads a bubble-pop stress game: broad casual players, mostly 13 to 40, many of them women, many of them on mid- and low-end Android phones in India and South-East Asia, looking for something to calm down with at night or in a queue. They search "stress relief", "antistress", "pop it", "ASMR", "relaxing games". What sells to them:
- **Soft and tactile:** rounded, squishy, pastel or dreamy-dark, things that look nice to touch.
- **Sound-first:** satisfying pops, gentle music, sleep-friendly.
- **No failure states** in the main mode.

Black and gold with a didone-style serif says "watch brand, banking app, premium perfume". It signals exclusive and expensive, which reads as cold for a free calm game. It's also not what the store shows, so it can't be doing the work it was meant for. **Keep two good ideas from it**: dark backgrounds (good for night play and OLED, calm) and glass and refraction as a material. Drop gold-as-luxury, the serif display face and the word "premium".

---

## 4. Feature sprawl versus a simple game

| System | Verdict | Why |
|---|---|---|
| Tap to pop, split into smaller bubbles | **Keep, make it the hero** | The most satisfying thing in the game. |
| Same-colour chains | Keep | A quiet skill layer for players who want one. |
| Zen (endless, no timer) | **Promote to the default mode** | It's what the audience came for. |
| Adventure, 100 levels, wind, gravity, 8 s timers | Shrink and reframe | Keep a short, optional "Challenge" track (around 30 hand-tuned levels, no timer under 30 s). Cut procedural 21 to 100. |
| Bosses (5 hits, minions) | Cut from calm mode; keep 1 per 10 challenge levels | A boss on level 3 is too early and off-tone. |
| 7 special bubbles | Cut to 3 (bomb, rainbow, ice) | Magnet, ticking bomb and chaos don't do what the tutorial cards say (§8). Ghost adds noise. |
| 4 power-ups with 5 upgrade levels each | Cut to 0 to 2 | Upgrades are the main coin sink, but "+1.5 s slow-mo" isn't a reason to return. |
| Coins, prestige (no requirement, resets coins, 2× coins) | Replace | Prestige is a free button for new players; it isn't a system. Replace it with a collection (§7). |
| 6 skins, 6 themes (all free except Gold) | Turn into the collection | Something to earn and show is the strongest return reason this genre has. |
| Achievements (12) | Keep, fold into the collection | Good, local, no network. |
| Daily Challenge | Rebuild | Make it truly seeded and give it a streak and a reward people want. |
| Ads: banner on every screen, rewarded ad as the gate after every level from 21, "Power-up guarantee · Watch ad" on the start screen | Cut hard in calm mode | A calm mode can't have a banner moving under it and an ad between rounds. |

**Rule of thumb for the relaunch:** the first session shows one bubble, one verb and one sound. Everything else appears only after the player has popped for a minute and come back once.

---

## 5. Retention and monetisation feel (player's view)
- **Day-1 hook:** none. Daily is random, not seeded; nothing waits for tomorrow; there's no streak.
- **Collection:** nothing to collect (skins and themes are free on day one).
- **Session end:** Adventure ends on a fail sting and a choice of three ad-flavoured buttons (Continue for ad, 50 coins for +15 s, Coin bonus for ad). Zen ends on the same fail sting.
- **Ads:** a banner sits at the bottom of every screen, including the play field (it overlaps the area where bubbles bounce). From level 21, a rewarded ad runs as a gate after **every** level. The "Remove Ads" IAP exists, which is good, but the pitch for paying is "make it stop" and not "get something nice".
- **Ethics:** fair in intent (no pay-to-win). Noisy and off-tone in execution for a calm game.

---

## 6. Comparables: what the genre nails (lessons, not things to copy)

| Genre reference | What it nails | Bubbles gap |
|---|---|---|
| **Antistress / fidget toy apps** (pop-it sheets, bubble wrap, slime) | Instant tactility: touch → sound → haptic in under 16 ms. No goal needed. Each toy feels like a material. | Pop on release; balls look like plastic; no material sound. |
| **Calm / meditation apps with play** | A breathing rhythm, sleep-friendly audio, dark palettes, no failure, a streak that feels kind. | "Breathe" is only in the tagline. |
| **Cozy collection games** | Small daily progress you can see (a garden, a room, an aquarium), things to collect and name. | Nothing persists that a player can look at. |
| **Physics ASMR toys** | Soft-body wobble, slow motion, close-up macro shots that sell on short-form video. | Rigid circles; no hero shot for a store video. |
| **Daily puzzle games** | One shared seed per day and a spoiler-free share card. | Daily isn't seeded; no share. |

---

## 7. Re-launch concepts (diverge, then converge)

### 7.1 Problem statement
**How might we turn the existing pop-and-split engine into a calm, tactile game that a store browser understands in 2 seconds, that feels good from the very first touch, and that gives players a gentle reason to come back tomorrow, without servers or a big art budget?**

Constraints: solo developer, Kotlin + Compose Canvas engine, zero-cloud (no leaderboards, no accounts), ethical monetisation, India-first low-budget launch, low-end phones. All names below are working titles; check each on Play search and in trademark databases before choosing.

### 7.2 Three concepts

#### Concept A: **"Hushpop"** (working title; calm soap-bubble ASMR toy with breathing)
- **Pitch:** *"Real soap bubbles. Soft pops. Breathe slower."*
- **Core twist:** bubbles are **real-looking soap bubbles**: transparent film, a thin-film rainbow sheen that swirls, a wobbling soft body, and a slow upward float like real bubbles. Two verbs: **tap to pop** (on touch-down) and **hold to blow** (hold anywhere and a new bubble inflates under your finger; release and it floats away). A **Breathe** mode syncs blowing to a breathing rhythm: inhale while a bubble inflates for 4 s, exhale while it drifts up, then pop it. Splits stay: big bubbles burst into a few smaller ones, in a gentle cascade. No timer, no fail, no score in the default mode; a quiet "pops today" count only.
- **Meta:** the **Soap Shelf**. Each day of play fills a jar; full jars unlock a new soap (film colour pattern + pop sound + background), e.g. Lavender Dusk, Monsoon Glass, Chai Steam, Firefly Night, Sea Glass, Marigold. A kind streak (misses don't reset to zero; they pause it). Weekly "soap of the week" from a local seed.
- **Art direction / mood board in words:** night bathroom window with rain outside; a bubble wand in a jar of soapy water under a warm lamp; macro photos of soap film (oil-slick magenta-teal-gold bands); bokeh lights; deep blue-violet backgrounds (`#0E1022 → #1C1B3A`), a single warm lamp light; rounded sans (Nunito / Quicksand-style) in soft white; no gold, no serif, no shouty arcade text. UI is minimal: a tiny pill at the top, everything else is bubbles.
- **Audio:** layered pop = soft wet "plip" + a tiny airy tail, pitch from bubble size (big = low, small = high), and quiet chained notes that form a pentatonic phrase. Rain or night ambience beds (rain on glass, crickets, distant sea) with sparse piano or kalimba over them. Sleep timer.
- **Why it stands out:** most bubble apps on Play are shooters or flat pop-it sheets. A believable, wobbly, iridescent soap bubble you can **blow** yourself, plus a real breathing rhythm, is a fresh hook that shows in a 3-second video and keeps the "Pop · Breathe · Repeat" promise literally.
- **Weakness:** toys have weak long-term retention; the Soap Shelf has to carry it. The soap-film look must be good on low-end GPUs.

#### Concept B: **"Pop Pond"** (working title; cozy pop-to-grow collection)
- **Pitch:** *"Every bubble you pop brings your pond to life."*
- **Core twist:** you pop bubbles rising from a little pond; each pop releases a drop of "pond energy". Energy grows lily pads, reeds and lanterns, and attracts small creatures (frogs, koi, fireflies, a heron) that you collect and name. The pond changes with the real time of day. A short daily session (2 to 3 min) gives visible progress; the pond is the save file you look at.
- **Art direction:** soft painterly 2D, pastel greens and dusk pinks, round chunky shapes, creatures with simple faces.
- **Why it stands out:** cozy plus collection is the strongest retention shape in casual, and a pond you built is a great screenshot.
- **Weakness:** needs a lot of new art (creatures, pond states) and a real economy design. Biggest build of the three, and furthest from the current code.

#### Concept C: **"Chainfizz"** (working title; skill arcade with a daily puzzle)
- **Pitch:** *"One tap. How big can the chain go?"*
- **Core twist:** keep and sharpen the arcade side. Each round starts with a still board of packed bubbles; one tap starts a chain (same colour, bombs, rainbow), and splits keep it going. Score = chain size. A **seeded daily board** (everyone in the world gets the same), with an offline emoji share card. The 100 levels become 60 hand-made puzzle boards.
- **Art direction:** bright neon-on-dark, chunky flat bubbles, big numbers.
- **Why it stands out:** a daily share card is free marketing without servers. Chain-reaction physics videos travel well.
- **Weakness:** gives up the stress-relief audience, which is where the current tagline and search terms point; the puzzle genre is crowded and level design is slow, hand work.

### 7.3 Stress test

| | A: Hushpop | B: Pop Pond | C: Chainfizz |
|---|---|---|---|
| **User value** | High in-session (feel), medium long-term (Soap Shelf) | High long-term, medium in-session | Medium-high for puzzle fans |
| **Feasibility** | **High**: reuse spawn/split/particles/sound/haptics; new work is the bubble shader-look, blow gesture, breathe mode, shelf | Low-medium: big art and economy lift | Medium: level design is the cost |
| **Differentiation** | **High**: soap-film realism + blowing + breathing is rare | Medium-high | Medium |
| **Fits zero-cloud, ethical monetisation** | Yes | Yes | Yes |
| **Fits current audience and search terms** | **Yes** ("stress relief", "ASMR", "relaxing") | Yes | Partly |

**Hidden assumptions and kill risks**
- *A bets that* a convincing soap bubble can be drawn in Compose Canvas at 60 fps on a budget phone. **Test:** 1-day spike: transparent body + sweep-gradient film + wobbling 24-point path, 20 bubbles on screen, on the cheapest test phone. *Kill risk:* it looks like a flat gradient circle. Mitigation: pre-render 8 film textures as small bitmaps and rotate them.
- *A bets that* "blow your own bubble" is discovered and enjoyed. **Test:** 5 strangers, no instructions, watch whether they find hold-to-blow within 30 s with a single ghost-finger hint.
- *A bets that* the Soap Shelf brings people back. **Test:** D1/D7 in a closed test before the public relaunch.
- *B bets that* a solo developer can ship 20+ creatures and pond states at quality. History of the studio says art volume is the bottleneck.
- *C bets that* hand-made puzzle boards are fun enough to beat the many chain puzzle games already out there.
- *All bet that* the install problem is identity and store page, not discoverability alone. **Test first:** run a Play Store Listing Experiment on a new icon and first screenshot before rebuilding the game (05-market should confirm the funnel numbers).

### 7.4 Recommended direction: **Concept A, "Hushpop"**
It's the only concept that keeps the promise already in the tagline, fits the audience and search terms that the store already points at, and reuses almost the whole engine (spawn, split, chain, particles, sound, haptics, DataStore, Zen). It borrows B's best idea (something visible that grows: the Soap Shelf) and C's best idea (a local seeded "soap of the day" and an optional chain-score challenge track) without their build costs.

### 7.5 What it reuses and what it cuts

| Existing asset | Role in Hushpop |
|---|---|
| `SpawnBubblesUseCase`, split logic, colour chains | Kept. Spawn from the bottom and float up; fewer, slower, larger bubbles; split into 2 to 3, max 2 generations. |
| `drawBubble()` 6-layer renderer | Rewritten for transparency: thin rim, film sheen, highlight, faint reflection of the lamp. Keep the light-source maths. |
| Particles, rings, hit-stop, haptics | Kept and retuned (droplets + film shards, softer haptics, no shake in calm modes). |
| `SoundManager` (SoundPool) + `MusicManager` (crossfade) | Kept. Replace glass-clink pops with wet pops; size-to-pitch; add ambient beds. |
| Zen mode | Becomes the default "Float" mode. |
| Adventure | Becomes an optional "Challenge" track of about 30 levels with chain targets, no sub-30 s timers. |
| Achievements, DataStore economy | Kept; coins become "soap drops" that fill the jar. |
| Skins/themes | Become soaps (film + sound + background bundles) on the Soap Shelf. |
| **Cut** | Procedural levels 21 to 100, magnet, ticking bomb, chaos, ghost, prestige, power-up upgrade tree, the rewarded-ad gate between levels, the banner in play, the "Power-up guarantee" ad button, the serif/gold luxury identity. |

### 7.6 First 60 seconds (Hushpop)

| Time | What the player sees and hears | Design intent |
|---|---|---|
| 0–2 s | No menu. Dark violet room, a rain-on-glass bed fades in. A single big soap bubble drifts up from the bottom, wobbling, its film swirling. A ghost finger hovers over it. | One object, one verb. The icon and store shot show exactly this. |
| 2–5 s | Player touches it. **Pop on touch-down**: a 40 ms squash toward the finger, the film tears from the touch point, 10 to 14 droplets and 3 thin film arcs fly out, a soft wet "plip", a 10 ms light haptic. | The first touch has to feel better than any other app on the phone. |
| 5–15 s | It splits into 3 smaller bubbles that float apart slowly. Each pop is a higher note of the same pentatonic phrase. A tiny "12 pops" pill fades in at the top, nothing else. | The cascade teaches splitting and makes music. No numbers shouting. |
| 15–25 s | More bubbles drift up, 3 to 6 on screen. A near pair of the same tint pops together (a quiet chain shimmer, two notes in a chord). | Chains discovered, not taught. |
| 25–35 s | Ghost-finger hint: "Hold to blow your own". Player holds; a bubble inflates under the finger with a breathy rising tone; release → it floats off. | Second verb, the hook nobody else has. |
| 35–50 s | A soft line: "Want to breathe with it? Inhale while it grows…" with a Breathe toggle. If accepted: 3 guided breaths (inflate 4 s, hold, exhale as it drifts, pop). | Keeps the tagline's promise. Optional, never blocking. |
| 50–60 s | The jar at the corner fills a third of the way. "Fill the jar to unlock your next soap: **Firefly Night**." A glimpse of the locked soap (gold-green film, firefly pops). Settings and Challenge show as small icons for the first time. | A reason to come back tomorrow, shown, not told. No ad in the first session. |

### 7.7 Juice checklist (for any concept; all achievable in Compose Canvas)
- [ ] Pop on **pointer down**, in the same frame; no wait for the game loop.
- [ ] Hit test picks the **topmost** bubble (last drawn).
- [ ] Spawn: grow from 0 to full in 200 ms with a slight overshoot (spring).
- [ ] Idle: each bubble wobbles (radius ±3% on 2 to 3 sine modes, random phase) and its film sheen rotates slowly.
- [ ] Pop anticipation: 30 to 50 ms squash toward the touch point (scale 1.06 along the touch axis, 0.94 across).
- [ ] Pop: the film tears from the touch point (an expanding arc gap), droplets with gravity and fade, 2 to 4 thin film shards that curl, and a faint expanding ring.
- [ ] Sound layer 1: a wet pop, picked from 4 to 6 variations, pitch from bubble size; layer 2: a tiny airy tail; layer 3: a pentatonic note that rises with each chained or quick pop and resets after 1.5 s of quiet.
- [ ] Chains: a short 60 ms hit-stop and a chord, not a shout.
- [ ] Haptics: light `EFFECT_TICK` for small pops, `EFFECT_CLICK` for big ones, a soft double for chains; never on every frame; respect the system haptic setting.
- [ ] Screen shake: only in Challenge, only on bombs, 2D (separate X and Y noise), off with reduced motion.
- [ ] Pop text (if any) at the **tapped** bubble, in correct dp, 1 word max, and rare.
- [ ] Bubble size and speed in **dp**, smallest bubble at least 56 dp across.
- [ ] Ambient bed and music under the pops at around -18 LUFS, loops of at least 2 to 3 minutes, 44.1 kHz; duck music 2 dB during dense chains.
- [ ] Session end is kind: a soft chime and "Nice. 84 pops today." Never a fail sting in calm modes.
- [ ] Every special bubble has its own sound and haptic, and the sounds the code loads are actually played.

### 7.8 Not doing (and why)
- **Leaderboards, accounts, cloud sync:** zero-cloud is right; a local seeded "soap of the day" covers sharing.
- **More levels or more special bubbles:** the problem is feel and identity, not quantity.
- **Keeping "Bubbles" as the name:** unfindable.
- **Paid UA as the main channel:** a calm ASMR toy grows through short-form video of the pop and search terms; spend on the store page first.

### 7.9 Monetisation fit for Hushpop
- **Free:** the full Float mode, Breathe mode, Challenge track, a soap every few days.
- **IAP (one-time, low price for India):** Remove Ads; Soap packs (e.g. "Monsoon Collection": 4 soaps with sounds and ambience); a Supporter bundle (all current soaps + remove ads).
- **Rewarded ads (opt-in only):** "fill the jar faster" or "try a locked soap for today". No banner in play, no forced ad between rounds, no ad in the first 3 sessions.

### 7.10 Open questions for the owner
- Rename: is a new title acceptable now? Candidate directions: one soft, ownable word plus a subtitle that carries search terms ("Hushpop: Bubble ASMR & Calm"). Check Play search and trademarks for each.
- Is the Challenge track worth keeping at all, or should version 1 of the relaunch be calm-only?
- What is the cheapest real Android phone available for testing the soap-film look at 60 fps?

---

## 8. Promise audit (QA-Playbook rule 4)

| Claim (where) | Reality in code | Action |
|---|---|---|
| Magnet: "Pulls nearby bubbles toward it. Doesn't chain." (tutorial card, `GameViewModel.kt:571`; store copy lists "magnet") | No pull code anywhere. A magnet is a non-chaining, non-splitting bubble. | Build it or cut it (recommend cut). |
| Ticking bomb: "Explodes on its own in 1.5s — pop it first!" (tutorial card) | No fuse or timer code; it only explodes when tapped, with a bigger blast. | Build it or cut it. |
| Chaos: "Flings every bubble around it in random directions." | The fling writes into a throwaway copy (`remaining.toMutableList()[i] = …`, `GameUseCases.kt:521`), so nothing moves. | Fix or cut. |
| Daily "seed-based" (AGENTS.md), "A fresh seeded run every day" (website) | No seed; `spawnBubblesUseCase` uses `Random.Default`. Daily is a random 60 s run. | Seed from the date, or reword. |
| "every game event has a distinct sound" (spec acceptance), "13 distinct clips" (AGENTS.md) | `crack`, `split`, `boss` and `vibrateBossHit` are loaded or defined but never called. | Wire them up. |
| "build your collection of upgrades, skins, and themes" (store copy); "unlock skins/themes" (README) | All skins and themes are free from install; only Gold is locked, behind a Prestige button with no requirement. | Make them earnable (Soap Shelf) or reword. |
| "Prestige progression" (store copy) | One free button: coins reset to 0, Gold skin and 2× coins forever. No condition. | Gate it or remove it. |
| "10 Adventure Levels … handcrafted" (website) | 20 curated + 80 procedural = 100. | Fix website copy. |
| "Pop · Breathe · Repeat" (start screen, store, website) | No breathing feature. | Build Breathe mode (Concept A) or drop the word. |
| "Every interaction uses spring physics" (store copy) | UI buttons do; bubbles (the main interaction) are rigid. | Add bubble springs or reword. |
| Store screenshots: HUD with "MOVES", hourglass bubbles, "Bubble left: 6" | None of these exist in the app. | Replace with real captures. |
| Store screenshots 2 and 3 show "Demo App · INSTALL" | ironSource test ad captured in marketing art. | Replace. |
| "Clear every bubble before the timer runs out" | True, but the timer refills +0.5 s per hit (never stated), and the HUD "Bubbles left" counts only bubbles on the board. | Show "remaining to spawn + on board", or explain the refill. |
| Icon `icon-512.png` | 1254×1254 RGB with baked black corners. | Re-export 512×512 32-bit PNG, full-bleed, no text. |

---

## 9. Priority list (creative)

1. **Store truth pass (1 day):** retake real screenshots (no "Demo App" ads, no invented HUD), re-export the icon at 512×512 without baked corners or text, fix the feature graphic size, fix website "10 levels". This is Play-policy hygiene and doesn't depend on the relaunch.
2. **Fix the feel bugs (1 to 2 days):** pop on touch-down; topmost-bubble hit test; pop text at the tapped bubble in correct units; dp-scaled bubble sizes and speeds; wire up crack/split/boss sounds; no game-over sting in Zen; 2D shake.
3. **Fix or cut the broken specials (half a day):** magnet, ticking bomb, chaos. Seed the Daily from the date.
4. **Make Zen the default and the first screen (1 day):** no menu on first launch; bubble first. Move the banner off the play field; no ads in the first 3 sessions.
5. **Then** prototype Concept A's load-bearing bets (soap-film look at 60 fps on a cheap phone; hold-to-blow discovered by strangers) and run a Store Listing Experiment on a soap-bubble icon + first screenshot before committing to the rename.

---

*Evidence files: `app/src/main/java/com/ashwathai/bubbles/{MainActivity,ui/game/GameViewModel,domain/usecase/GameUseCases,domain/model/GameModels,data/sound/SoundManager,data/sound/MusicManager,ui/settings/SettingsScreen,ui/theme/luxury/LuxuryMotion}.kt`, `app/src/main/res/raw/*`, `app/src/main/res/drawable/ic_launcher_foreground.png`, `play-listing-assets/*.png`, `bubbles_ico-logo.png`, `scripts/update-play-listing.mjs`, `website/index.html`, `LUXURY_DESIGN_PLAN.md`. Audio measured with ffprobe/volumedetect: music_adventure.wav 32.0 s, 22,050 Hz mono, mean -24.3 dB; music_zen.wav 40.0 s, 22,050 Hz mono, mean -26.6 dB; pops about 0.21 s, peak -1.6 dB.*
