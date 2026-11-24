# Hushpop: Play Store listing (draft for owner approval)

Package stays `com.ashwathai.bubbles`. Nothing here is live until you approve and it is uploaded.

## Title (30 max)
**Hushpop: Bubble Pop & Relax** (27)

## Short description (80 max)
EN: `Pop soap bubbles to relax. Soft ASMR pops, breathe mode, offline. No timer.` (77)
HI: `साबुन के बुलबुले फोड़ें और रिलैक्स करें। ASMR आवाज़ें, ब्रीद मोड, ऑफलाइन।`

## Full description (EN)

Need a quiet minute? Pop real-looking soap bubbles and let your shoulders drop. Hushpop is a calm, satisfying bubble pop game for stress relief, with soft ASMR pop sounds and gentle haptics on every touch.

• **Float: no timer, no fail.** Soap bubbles rise through a quiet night room and drift away. Pop them whenever you like.
• **Hold to blow your own.** Hold anywhere and a bubble grows under your finger. Let go and watch it float off.
• **Breathe mode.** 1, 3 or 5 minutes of guided breathing: breathe in as the bubble grows, out as it rises, then pop it.
• **Soft, satisfying sound.** Wet pops pitched by bubble size that climb a little melody, over rain on the window.
• **Soap Shelf.** Every pop fills a jar. A full jar unlocks a new soap with its own colours, room and sound: Lavender Dusk, Monsoon Glass, Chai Steam, Firefly Night, Sea Glass and Marigold.
• **Soap of the day.** A different soap each day, free for everyone.
• **Kind streak.** Missed a day? Your count never resets.
• **Challenge.** 20 timed levels for when you want a goal, never under 30 seconds.
• **Offline, no account, about 11 MB.**
• **Supporter Pack (optional, one-time):** removes every ad and adds the Moonlit Pearl soap.

Stress relief, anti stress, relaxing game, satisfying ASMR bubble popping, calm, zen, mindful breathing, bubble wrap feel. Free, works without wifi.

## Every claim → code (QA playbook rule 4)
| Claim | Where it is real |
|---|---|
| No timer / no fail in Float | `GameViewModel.updateGame` (timer only when `mode != ZEN`), `FloatMotion` |
| Hold to blow | `BlowMechanics`, `GameViewModel.onPointerDown/updateBlowing/releaseBlownBubble` |
| Breathe 1/3/5 min, in/out/pop | `BreathCycle`, `BreatheChoice`, `GameViewModel.startBreathe` |
| Pops pitched by size, melody | `SoapPopPitch`, `SoundManager.playSoapPop` |
| Rain on the window | `res/raw/music_zen.ogg` (`tools/sound/make_soap_sounds.py`) |
| Haptics on every pop | `SoundManager.vibratePop` in `applyCombo` |
| 6 unlockable soaps + names | `Soaps.ALL`, `SoapShelf.addDrops` |
| Soap of the day, free for everyone | `SoapShelf.soapOfTheDay`, `effectiveSoap` |
| Streak never resets | `SoapShelf.recordPlayDay` |
| 20 levels, never under 30 s | `GameConfig.levels` + `CHALLENGE_MIN_TIME_SEC`, `SpawnBalanceTest` |
| Offline, no account | no network code besides ads/billing; no sign-in |
| ~11 MB | release APK 11.2 MB (app-release.apk, 24 Nov 2026) |
| Supporter Pack removes every ad + Moonlit Pearl | `LevelPlayAdManager.nonRewardedAllowed`, `LevelPlayBanner`, `BillingManager.adsRemoved`, `Soaps.MOONLIT_PEARL` |

## Graphics
- Icon: `icon-512.png`
- Feature graphic: `feature-graphic.png` (1024×500)
- Phone screenshots: `phone-01.png` … `phone-08.png` (1080×1920, real captures, captions added)
- Raw gameplay clip for Reels/Shorts/promo: `gameplay-raw.mp4` (needs a YouTube upload to be used as the Play video)

## Category & settings to check in Play Console
- Category: **Casual** (or Arcade). Tags: Relaxing, Casual, Offline.
- Target audience: 13+ (matches `is_child_directed=false` sent to the ad SDK and the privacy policy).
- In-app product `remove_ads`: rename display name to **Supporter Pack**, price ₹99 India / $2.99 US (₹49 launch price for 2 weeks).
- Data safety: unchanged data types (Advertising ID + device info via LevelPlay; purchase via Google Play). No data collected by the developer.
