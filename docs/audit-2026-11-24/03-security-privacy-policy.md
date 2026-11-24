# 03 — Security, Privacy, Play Policy & Monetization Wiring Audit (Bubbles v1.2)

- **Date:** 2026-11-24
- **Branch audited:** `feature/backlog-completion` working tree (on top of `dc875be`, Release v1.2). Release binary inspected: `releases/bubbles-v1.2-code3.apk` (versionCode 3, versionName 1.2).
- **Scope:** Android app (`app/`), build config, git hygiene and history, merged release manifest, ad wiring (LevelPlay/Unity), Play Billing, consent, `website/` (privacy, data deletion, app-ads.txt), Play policy (disruptive ads, rewarded ads, Families, Data safety), dependencies.
- **Method:** Read-only. Nothing was built, edited, committed or stashed. **No secret values were printed.** Secret checks compared values in-process and reported only "same/different". Release manifest read with `aapt2 dump`. Inputs read first: `C:\Users\Atul\projects\_studio\qa\QA-PLAYBOOK.md`, repo `AGENTS.md`. Format follows `Jump_droid/docs/audit-2026-11-23/03-security-privacy-policy.md`.
- **Repo visibility:** **PUBLIC** (`gh repo view atulkpal/bubbles` → `visibility: PUBLIC`). Every tracked file and all git history should be treated as published.

---

## 0. Threat model (short)

| Trust boundary | Assets | Main threats |
|---|---|---|
| Public git repo ↔ world | Upload keystore and its passwords, Play service-account key | Information disclosure (secret leak), supply-chain / malicious update |
| App ↔ Play Billing | "Remove Ads" entitlement | Tampering (backup restore, root), refund abuse |
| App ↔ device storage (DataStore, Auto Backup) | `adsRemoved`, coins, progress | Cloud-restore of entitlement to another account |
| App ↔ LevelPlay / Unity Ads | Ad ID, App Set ID, IP, consent | No consent (GDPR / US states), disruptive/forced ad formats → Play suspension |
| Store listing / website ↔ users | Privacy policy, Data safety, app-ads.txt | Inaccurate disclosure, lost ad demand |

Abuse cases tried in code review: (1) clone the repo and look for signing material in history; (2) buy Remove Ads, then check every ad path; (3) refund, then check whether ads come back; (4) close a rewarded ad early; (5) ad-free user taps "+50 coins" repeatedly; (6) background → foreground the app; (7) restore a cloud backup on another account.

---

## Summary table

| ID | Severity | Title |
|---|---|---|
| C-1 | **Critical** | Release/upload keystore **and both passwords** are in public git history, and the current keystore and passwords are the exact same ones |
| H-1 | **High** | Rewarded ad unit used as a **forced, unsolicited interstitial** between levels (every level from 21 on); closing it early leaves a blank, stuck screen |
| H-2 | **High** | "Foreground" interstitial fires on return-to-app (app-open style), with no gameplay/context check, including right after the user tapped an ad or an About link |
| H-3 | **High** | No consent management at all (no UMP / no `LevelPlay.setConsent` / no CCPA `do_not_sell` / no child flag); SDK starts in `Application.onCreate` before any choice |
| H-4 | **High** | Ad-free (paid) users get **unlimited free coins**: rewarded buttons stay visible and grant instantly with no cap, no cooldown, no one-shot |
| M-1 | Medium | Billing: once `adsRemoved` is persisted, billing is never initialised again → refunds/revocations never detected; with `allowBackup=true` the flag also restores onto other accounts/devices |
| M-2 | Medium | Privacy policy incomplete/inaccurate vs. what the SDKs do (no consent/rights section, no CCPA, no Play Billing, no IP/approximate location, no Privacy Sandbox/App Set ID, claims "analytics" the app doesn't have) |
| M-3 | Medium | No `app-ads.txt`, and the website lives at a sub-path (`/bubbles/`) where an app-ads.txt cannot work |
| M-4 | Medium | Late-loading rewarded ad auto-shows whenever it finishes loading (possibly after the user has moved on); all `onAdFailed` callbacks are `{}` (tap does nothing, no message) |
| M-5 | Medium | Daily ad cap and level-interstitial pacing are in memory only (reset on every process restart); the forced between-level ad eats the user's 5 opt-in rewarded ads |
| M-6 | Medium | Families / target-audience risk: "stress buster" bubble game with cartoon bubbles can be judged child-appealing; no child-directed signal; policy says "not for under 13" with no age screen |
| L-1 | Low | Billing robustness: no reconnect after disconnect; setup failure leaves a "REMOVE ADS" button that silently does nothing; no pending-purchase message; callback wired after `init()` (race) |
| L-2 | Low | Client-only purchase trust (no server/RTDN verification, no signature check) |
| L-3 | Low | Ad-free users still see "WATCH AD" labels and "N ads remaining today", contradicting the "no rewarded ads" promise; LevelPlay SDK still initialises (and collects) for them |
| L-4 | Low | `Log.d/i` with ad revenue, unit IDs and network names ship in release (no R8 `-assumenosideeffects`) |
| L-5 | Low | Static `Activity` reference in `LevelPlayAdManager.currentActivity` (leak / stale activity) |
| L-6 | Low | Dependency age: Compose BOM 2024.10.00, unused kotlinx-serialization and Hilt classpath, over-broad R8 keep rules |
| L-7 | Low | Data-deletion page calls the Ad ID "anonymous" and gives no route to Unity's own deletion portal; website still says "10 levels" while the listing says 100 |
| I-x | Info / OK | Items checked and found fine (Section 5) |

### Jump Droid pattern checklist (from the 2026-11-23 audit)

| Jump Droid issue | Bubbles result |
|---|---|
| Remove Ads didn't remove ads | **OK.** Banner (`LevelPlayBanner.kt:44`), foreground interstitial (`LevelPlayAdManager.kt:294`), level interstitial (`:319`) and rewarded (`:133`) are all gated on `BillingManager.adsRemoved`. But see H-4 and L-3. |
| Auto-played / unsolicited rewarded ads | **FOUND** — H-1 (forced between-level rewarded) and M-4 (late auto-show). |
| Reward granted on load/fail instead of on earned | **Mostly OK.** Opt-in rewards fire only from `onAdRewarded` (`LevelPlayAdManager.kt:212-216`). Exception: the forced level ad advances on *failure* too (by design), and ad-free users get rewards with no ad (H-4). |
| Daily caps never saved | **FOUND** — M-5 (in-memory only). |
| No UMP / consent | **FOUND** — H-3. |
| Wrong privacy policy | **Partly.** It names the right SDK (Unity LevelPlay), unlike Jump Droid, but is incomplete — M-2. |
| Purchases not verified / acknowledged | Acknowledged: **OK** (`BillingManager.kt:197-213`). Verified: client-only (L-2). Refund handling: **missing** (M-1). |
| Broken `.gitignore` leaking releases | `.gitignore` is fine today; `releases/`, `*.jks`, `keystore.properties`, `play-account.json`, `local.properties` are all ignored and none is tracked. **But the keystore was committed before the ignore rule existed** — C-1. |

---

## 1. Critical

### C-1 — Signing keystore and passwords are in public git history (and still in use)
- **Where:**
  - Commit `4f888ec` ("Add Play Store release pipeline", 2026-08-28) **added `app/release-key.jks`** (2,746 bytes) and hard-coded `storePassword`, `keyAlias` and `keyPassword` as string literals in `app/build.gradle.kts` (`signingConfigs.release`).
  - Commit `dc875be` (Release v1.2) deleted the `.jks` from the tip and moved the passwords to `keystore.properties`. **Both commits are on `origin/master`** of the public repo, so the old blob and the old literals are still downloadable (`git show 4f888ec:app/release-key.jks`).
  - Comparison (values not printed): the working-tree `app/release-key.jks` has **the same git blob hash** as the one in history (`9e579b1…`), and the `storePassword` and `keyPassword` in today's `keystore.properties` are **identical** to the literals in history. In practice the production signing credentials are public.
  - Today's state is otherwise correct: `.gitignore:18-22` ignores `*.jks`, `*.keystore`, `keystore.properties`, `release-key.*`; `play-account.json` (`:15`) and `/local.properties` (`:4`) are ignored, and `git log --all` shows **neither `play-account.json` nor `keystore.properties` nor `local.properties` was ever committed**. A history scan found no private keys, `"private_key"` fields, Google API keys or GitHub tokens.
- **Impact:** Anyone can sign an APK/AAB as "you". Because uploads go in as AABs, the app is almost certainly on **Play App Signing**, so this is the **upload key**: an attacker cannot push to Play without also getting Console or service-account access. But together with any later leak of `play-account.json` (which sits in the same folder, used by `scripts/*.mjs`), they could publish a malicious update to every user. Sideloaded APKs signed with this key (and the `releases/*.apk` built locally) would also pass as genuine.
- **Fix (not applied):**
  1. **Play Console → Test and release → App integrity → App signing → "Request upload key reset".** Generate a new upload keystore with new, strong passwords. Keep it outside the repo folder (for example `C:\Users\Atul\.secrets\bubbles\`) and point `keystore.properties` → `storeFile` at it.
  2. Treat the old key as burned: never sign with it again, and delete the local copy once the reset is confirmed.
  3. Optionally purge the history (`git filter-repo --path app/release-key.jks --invert-paths` plus a replace-text pass for the two passwords), force-push, and ask GitHub support to drop cached views. This **does not replace** step 1, because forks and clones may already exist.
  4. Rotate the Play service-account key in `play-account.json` as a precaution if it has ever been in any other folder, zip or chat. It was never committed here.
  5. Add a pre-commit secret scanner (gitleaks) so a key can't get into git again.

---

## 2. High

### H-1 — Rewarded ad used as a forced interstitial between levels; early close soft-locks the game
- **Where:**
  - `domain/model/GameModels.kt:296-316`: `shouldShowAdBetweenLevels()` → levels 3, 6, 9; 11, 13, …, 19; **every level from 21** (procedural levels up to 100).
  - `ui/game/GameViewModel.kt:734-752`: `startNextLevel()` sets `LevelComplete(waitingForAd = true)` when the user taps **NEXT LEVEL**.
  - `MainActivity.kt:352-375`: on `waitingForAd` it calls `LevelPlayAdManager.loadAndShowRewardedAd(adType = "level_interstitial", …)` straight away. This is the **rewarded** unit (`AdConfig.REWARDED_AD_UNIT_ID`), shown without any offer. `onUserEarnedReward` → next level; `onAdFailed` → next level.
  - While `waitingForAd` is true **no screen is drawn** (`MainActivity.kt:355-376`; the `else` branch is the only one that draws `LevelCompleteScreen`).
  - `LevelPlayAdManager.kt:229-235`: `onAdClosed` clears the callbacks **without calling either of them**.
  - The real interstitial path, `maybeShowLevelCompleteInterstitial()` (`LevelPlayAdManager.kt:318-337`), is **never called** (dead code), so the doc comment at `:36` ("every 3rd") and `AGENTS.md` ("Interstitial Ads Between Levels") describe something the code doesn't do.
- **Impact:**
  1. **Play policy / network ToS:** Play's Ads policy and the Unity/LevelPlay rewarded guidelines say a rewarded ad must be **opt-in**, with a clear offer the user accepts. Showing a rewarded unit as an unrequested, usually unskippable break is "forced rewarded", a common reason Unity limits fill and Play rejects apps (Disruptive Ads / Better Ads). From level 21, one unskippable ad after **every** short timed level is heavy enough to read as disruptive.
  2. **Soft-lock:** if a network lets the user close the ad before the reward (several do after N seconds), `onAdClosed` drops the callbacks. Neither path runs, `waitingForAd` stays true, and the player sees a blank background with only the banner. The only way out is killing the app. This is a QA-playbook "every tap must show its result" failure.
  3. It also uses up the 5-per-day rewarded budget (see M-5), so the user's own opt-in offers (Continue, Double reward) disappear after about 5 levels.
- **Fix:** Use the **interstitial** unit (`maybeShowLevelCompleteInterstitial`, with its 3-minute minimum gap kept) at the natural break, or make it an opt-in rewarded offer. Never show a rewarded ad unprompted. In `LevelPlayAdManager`, route `onAdClosed` without a reward to a "dismissed" callback, and in `MainActivity` always call `startNextLevelConfirmed()` from close/fail/timeout (add a 5–8 s load timeout). Reduce the 21+ frequency (for example at most one interstitial every 2–3 levels and every 3+ minutes). Add a UI test: "level ad closes without reward → next level starts".

### H-2 — App-open-style interstitial on every return to the app
- **Where:** `LevelPlayAdManager.kt:75-79` (`ProcessLifecycleOwner` `onStart` observer), registered by `trackActivity()` from `MainActivity.kt:177`; `maybeShowForegroundInterstitial()` at `:293-311`. `lastForegroundInterstitialAt` starts at `0` and lives only in memory (`:60`), with a 4-hour window (`:61`).
- **Impact:** After the SDK is ready, the **first time in each process that the user leaves the app and comes back**, a full-screen interstitial appears at once, whatever the game state (mid-level, the pause screen, Settings). Common triggers are: coming back from an **ad click** (the ad opens the Play Store or a browser, so the user returns to a second ad), coming back from the Settings → About/Privacy/Delete Data links (`SettingsScreen.kt:419-421` open the browser), or answering a message. The window resets on every cold start. Play's Disruptive Ads policy specifically names interstitials that "appear unexpectedly" and ads shown when the user returns to an app that are not a declared App-Open format. LevelPlay has no App-Open format, and the code comment says this is a substitute for one.
- **Fix:** Remove the foreground interstitial, or limit it to a real cold start with a loading screen (not on resume), never within N seconds of an ad click or external link, never while `GameState.Playing/Paused`, and persist the 4-hour timestamp in DataStore.

### H-3 — No consent management (GDPR/UK, US state laws), no signals to LevelPlay
- **Where:** `BubblesApplication.kt:15` → `LevelPlayAdManager.init()` (`LevelPlayAdManager.kt:84-106`) runs `LevelPlay.init` at process start. A search for `setConsent`, `setMetaData`, `do_not_sell`, `is_child_directed`, `UserMessagingPlatform` and `consent` in `app/src/main` finds **nothing**. No UMP dependency in `gradle/libs.versions.toml`. The release manifest requests `AD_ID`, `ACCESS_ADSERVICES_TOPICS` and `ACCESS_ADSERVICES_ATTRIBUTION`, and `play-services-appset` is bundled.
- **Impact:** EEA/UK users get personalised-ad processing (Ad ID, device storage) with no lawful consent (GDPR/ePrivacy). CA/VA/CO/CT/TX users get no "Do Not Sell/Share" option (CPRA counts ad-ID sharing for cross-context ads as "sharing"). Unity and ironSource contracts require the publisher to collect and pass consent. Without it, Unity applies its own defaults (limited, low-fill, or treated as non-consented depending on region), so this is both a legal risk and a revenue loss. Play's User Data policy requires the privacy policy and in-app disclosure to match.
- **Fix:** Add **Google UMP** (`com.google.android.ump:user-messaging-platform`) with a GDPR message plus a US-states message (AdMob/Funding Choices can be used for UMP even without AdMob demand), or a certified CMP. Before `LevelPlay.init`, call `LevelPlay.setConsent(granted)` and `LevelPlay.setMetaData("do_not_sell", "true"/"false")`, and for Unity `MetaData(context).set("gdpr.consent", …)` / `"privacy.consent"`. Set `LevelPlay.setMetaData("is_child_directed", "false")` explicitly (see M-6). Move `LevelPlay.init` to after the consent flow. Add a **"Privacy choices"** row in Settings → About that reopens the form (required for UMP's US-states and GDPR revocation).

### H-4 — Ad-free users can mint unlimited coins
- **Where:** `LevelPlayAdManager.kt:132-137`: if `adsRemoved`, call `onAdLoaded(); onUserEarnedReward()` at once, **before** the daily-cap (`:140-145`) and cooldown (`:153-159`) checks. The buttons never hide after use: `MainActivity.kt:1473` (LevelComplete "+50 COINS"), `:1655` (Double daily), `:1665` (GameOver "+50 COINS"). `adsRemaining` is a plain value, not state, and is always ≥1 for ad-free users, so the buttons stay.
- **Impact:** A buyer can tap "+50 COINS · WATCH AD" as often as they like, with each tap granting 50 coins at once. "Double reward" can likewise be repeated without limit. This breaks the economy (skins, themes, upgrades, prestige, the 50-coin Continue), makes "Achievements" trivial, and is an exploit that will show up in reviews and videos. For ad-supported users the same buttons are re-tappable up to the cap after each 60 s cooldown (Double daily can be claimed up to 5×).
- **Fix:** Make each offer **one-shot per screen** (state flag `bonusClaimed`, hide or disable after the reward). For ad-free users either keep the caps (count "instant rewards" against the same daily limit) or replace the offer with a fixed bonus on the result. Add a ViewModel test plus a Compose UI test: "tap bonus twice → coins +50 once".

---

## 3. Medium

### M-1 — Refunds never revoke; cloud backup restores the entitlement anywhere
- **Where:** `BillingManager.kt:56-61`: when `adsRemovedPersisted` is true, `init()` **returns before creating the BillingClient**, so `queryPurchasesAsync` never runs again on that install. `BubblesApplication.kt:20-28` reads the flag from DataStore. `AndroidManifest.xml:13-15`: `allowBackup="true"` with the template `backup_rules.xml` / `data_extraction_rules.xml` (nothing excluded), so the DataStore file goes to Google cloud backup and device-to-device transfer.
- **Impact:** (a) A user who buys, then refunds via Play (48-hour self-refund or support), keeps ad-free forever. (b) A backup restored onto a new phone signed in with a **different Google account**, or a family member's device restored from the same backup, gets ad-free without a purchase. Low revenue per case, but it is easy and well known.
- **Fix:** Always connect billing at start. Treat the persisted flag only as a fast cache, then reconcile with `queryPurchasesAsync(INAPP)`: if there is no `PURCHASED` `remove_ads`, set `adsRemoved=false`. Also handle `Purchase.PurchaseState` changes. Exclude the entitlement key from backup (put `adsRemoved` in its own DataStore file and `<exclude domain="file" path="datastore/entitlements.preferences_pb"/>` in both XMLs), or rely on query-on-start alone. For stronger protection see L-2 (RTDN voided purchases).

### M-2 — Privacy policy does not match the shipped SDKs and laws
- **Where:** `website/privacy.html` (last updated 2026-08-29, `:39`).
- **Gaps:**
  1. **No consent / rights section:** nothing on GDPR legal basis, access/erasure/objection rights, withdrawing consent, the CCPA/CPRA "Do Not Sell or Share" right, or how to opt out inside the app (there is no in-app control yet, see H-3).
  2. **No Google Play Billing mention.** `:57` says "we do not collect … financial or payment information". That is true for the developer, but the policy should say that purchases are processed by Google Play and that the app receives a purchase token/state.
  3. **IP address / approximate location** collected by ad networks is not disclosed. Only "precise geolocation" is denied.
  4. **App Set ID** (`play-services-appset`) and **Android Privacy Sandbox Topics/Attribution** (`ACCESS_ADSERVICES_*` in the merged manifest) are not mentioned.
  5. It claims the app collects "usage data (gameplay statistics, session duration…)" and "analyze usage patterns". The app has **no analytics SDK**: game data stays in local DataStore. Usage data is collected by the ad SDKs, not by "we". Over-disclosure is safer than under-disclosure, but it should match the Data safety form.
  6. **Children:** "not intended for children under 13" (`:88-92`) with no age screen; see M-6.
  7. Only Unity's policy is linked (`:72`); the ironSource privacy policy and Unity's data-request page should also be linked. The developer entity is "AshwathAI" here and "Ashwath AI" in the app and listing (use one legal name).
- **Data safety consistency (cannot be read from the repo; must be checked in Console):** the form should declare **Device or other IDs** (collected + shared, advertising), **App interactions** and **Diagnostics** (Unity SDK), **Approximate location** (IP-derived, per Unity's Data safety guidance), and "**Purchase history**" handled by Google Play. "Data is encrypted in transit: yes". "Users can request deletion: yes (email)". The answers must agree with the fixed policy.
- **Fix:** Rewrite the policy to cover the points above, add a "Your privacy choices" section that matches the UMP form, update the date, and re-check the Play Data safety form against Unity's and ironSource's published Data-safety guidance.

### M-3 — No app-ads.txt (and the site can't host one where it counts)
- **Where:** `website/` has no `app-ads.txt`. Pages serve from `https://atulkpal.github.io/bubbles/`. IAB app-ads.txt must sit at the **root of the developer website domain** in the Play listing, i.e. `https://atulkpal.github.io/app-ads.txt`, which needs the separate `atulkpal/atulkpal.github.io` user-site repo, or a custom domain.
- **Impact:** Unity/ironSource and their programmatic buyers reduce or block bids for unauthorised inventory. That means fewer ads and lower eCPM. It isn't a policy violation, but it's a direct revenue loss.
- **Fix:** Create the root site (or a custom domain such as `ashwathai.com`), publish the `app-ads.txt` lines from the LevelPlay dashboard (Unity Ads and ironSource entries), and set the Play listing "Website" to that domain. Keep the `/bubbles/` pages as they are.

### M-4 — Rewarded ad auto-shows on late load; failures are silent
- **Where:** `LevelPlayAdManager.kt:178-183` (calls `loadAd()` if not ready) and `:190-200` (`onAdLoaded` **shows the ad immediately** whenever callbacks are pending, with no time limit). Every opt-in caller passes `onAdFailed = {}` (`MainActivity.kt:264-284, 390-403, 421-454, 510-520`). The cooldown message ("try again in Ns"), the daily-cap message and "SDK still initializing" are therefore never shown.
- **Impact:** A user taps "CONTINUE · WATCH AD", nothing happens, they tap HOME or Restart, and 10 s later a full-screen rewarded ad appears on an unrelated screen. If it completes, the stale callback runs (for example `continueGame()` from the Start screen). Silent no-ops break the QA rule "every tap shows its result at once", and an ad appearing after the user has moved on is an "unexpected ad".
- **Fix:** Add a load timeout (for example 6 s). After it, or when the screen changes, cancel the pending request (`rewardedCallbacks = null`, so it never shows). Show a loading indicator while loading, and send every `onAdFailed` to a snackbar/toast with the message.

### M-5 — Daily cap and pacing are not persisted
- **Where:** `LevelPlayAdManager.kt:64-72` (`levelCompletionCount`, `lastLevelInterstitialAt`, `adsShownToday`, `lastAdDate` are plain vars). `checkDailyCap()` / `updateDailyCount()` at `:341-355`. The UI promises "N ads remaining today" (`MainActivity.kt:1675-1683`).
- **Impact:** The cap resets on every process restart (swipe the app away and you get 5 more), so the "remaining today" promise isn't real (QA rule 4). Meanwhile the forced level ad (H-1) uses up the budget, hiding the user's own opt-in offers. The cap counts only rewarded displays; the foreground interstitial (H-2) isn't capped.
- **Fix:** Persist the date and count (and the interstitial timestamps) in DataStore. Count forced or interstitial ads separately from opt-in rewarded. Make `adsRemaining` observable state so the label updates after each ad.

### M-6 — Families / child-appeal risk
- **Where:** Listing text (`scripts/update-play-listing.mjs:75-96`) and the site sell a "stress buster" with cartoon bubbles, skins and coins, which can easily read as child-appealing. The policy says not for under-13s (`privacy.html:88-92`), but there is no neutral age screen and no `is_child_directed` / COPPA flag to LevelPlay.
- **Impact:** If Play's reviewers (or the Console target-audience answer) class the app as appealing to children, it falls under the **Families policy**: only Families-self-certified ad SDKs, no personalised ads for children, no Ad ID from child users, and a neutral age screen if the audience is mixed. A wrong target-audience answer is a common rejection or removal reason.
- **Fix:** In Play Console → App content → Target audience, choose **13+ (ideally 16+/18+ only)** and make sure the store graphics don't target children. If you want under-13 players, add a neutral age screen and, for under-13s, `LevelPlay.setMetaData("is_child_directed","true")` plus no Ad ID. Unity Ads and ironSource are on Play's Families self-certified list, but they must be configured for it. Otherwise set `is_child_directed=false` explicitly and keep the "not for children" text consistent across the policy and the listing.

---

## 4. Low

- **L-1 Billing robustness.** `BillingManager.kt:86-89`: `onBillingServiceDisconnected` only sets `isReady=false` and never reconnects. `launchRemoveAdsFlow` (`:149-160`) silently returns when the client isn't ready or product details aren't cached. `isReady` is not read by the UI, and `MainActivity.kt:523-525` always shows "REMOVE ADS" when not ad-free, so on devices where setup failed (`:83`) the button does nothing (an `AGENTS.md` §6.7 "No Dead-Ends" breach). PENDING purchases (`:177-193`) give no "payment pending" feedback. `onPurchaseAcknowledged` is assigned **after** `init()` (`BubblesApplication.kt:23-28`), which is a small race. Without the callback, the flag isn't persisted that session; it recovers on the next start via query. **Fix:** reconnect with backoff, gate or disable the button on `isReady && details != null` with the price (`formattedPrice`), show "Purchase pending" for `PENDING`, and set the callback before `init()`.
- **L-2 Client-only trust.** No server verification or Real-time Developer Notifications (voided purchases). That is acceptable for a single-player, low-price unlock, but pair it with the M-1 query reconcile.
- **L-3 Ad-free UX/claims.** Ad-free users still see "… · WATCH AD" and "N ads remaining today" (`MainActivity.kt:1473-1683`), against the Settings promise "No banners, no interstitials, no rewarded ads" (`SettingsScreen.kt:206`). LevelPlay is still initialised for them (`BubblesApplication.kt:15`), so the SDK keeps collecting device data. **Fix:** relabel the buttons for ad-free users ("CLAIM +50") and skip `LevelPlay.init` when `adsRemoved` is true.
- **L-4 Release logging.** `Log.d/i/e` at `LevelPlayAdManager.kt:94-332` logs revenue, unit IDs and networks, and `BillingManager.kt` logs purchase flow. No PII, but add `-assumenosideeffects class android.util.Log { d(...); v(...); i(...); }` to `app/proguard-rules.pro`.
- **L-5 Static Activity reference.** `LevelPlayAdManager.kt:47, 108-114` holds `MainActivity` in a singleton. Use a `WeakReference`, or pass the activity per call (the callers already pass `activity`, but `onAdLoaded` uses the stored one).
- **L-6 Dependencies.** `targetSdk/compileSdk 36` (meets Play's 2026 requirement), Billing **8.0.0** (current major), LevelPlay 9.5.0 / Unity adapter 5.12.0 / Unity Ads 4.20.0 (check the adapter–SDK pairing in the LevelPlay "Integration Manager" before each release). Compose BOM `2024.10.00` and lifecycle 2.8.7 are about two years old. `kotlinx-serialization` (plugin + lib) is unused, and the Hilt classpath is unused. `app/proguard-rules.pro` keeps all of `com.ashwathai.bubbles.**`, `androidx.compose.**`, `kotlinx.coroutines.**` and `* extends Activity`, which defeats most shrinking and obfuscation. Trim these when you next touch the build.
- **L-7 Website text.** `data-deletion.html:58` calls ad data "anonymous" (the Ad ID is pseudonymous personal data under GDPR). Add a link to Unity's data-privacy request page. `index.html:65` and `about.html:49` still say "10 levels", while the listing says "100 levels" (the promise mismatch is for the creative/market reviewers).

---

## 5. Checked and OK (Info)

- **Release manifest** (`aapt2 dump` of `releases/bubbles-v1.2-code3.apk`): not debuggable, no `testOnly`, `targetSdkVersion 36`, `minSdk 26`. No `usesCleartextTraffic`, so the platform default (cleartext blocked) applies. Exported components are only the launcher `MainActivity` and library defaults that are protected (`SystemJobService` with `BIND_JOB_SERVICE`, `DiagnosticsReceiver` with `DUMP`, `ProfileInstallReceiver`). Everything else (ironSource activities/providers, billing proxies, WorkManager, datatransport) is `exported=false`. Permissions: INTERNET, ACCESS_NETWORK_STATE, VIBRATE, AD_ID, ADSERVICES_TOPICS/ATTRIBUTION, BILLING, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE (the last three come from WorkManager via the ad SDK), plus the signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. There are no dangerous runtime permissions.
- **Test vs live ad units:** `AdConfig.kt:11` switches on `BuildConfig.DEBUG`; release builds use the production key and units. LevelPlay app keys and unit IDs in a public repo are not secrets.
- **Reward-on-earned** for opt-in rewarded ads is correct (`onAdRewarded` only), and callbacks are cleared on close to stop double grants (`LevelPlayAdManager.kt:229-233`).
- **Acknowledgement:** non-acknowledged purchases are acknowledged before granting, and the grant waits for ack OK (`BillingManager.kt:197-213`). Restore on reinstall works through `queryPurchasesAsync` on connect (`:124-145`) when the flag isn't persisted. `enablePendingPurchases(enableOneTimeProducts())` is set (PBL 8 requirement). The product is non-consumable and never consumed.
- **Git hygiene today:** the `.gitignore` covers APK/AAB, keystores, `keystore.properties`, `play-account.json`, `local.properties` and `app.zip`; none of these is tracked, and only `app/release-key.jks` ever was (C-1). The `gh-pages` branch contains only the static site.
- **Data deletion:** the app has no accounts and no server, so the Play account-deletion requirement doesn't apply. The page gives local-clear instructions and an email route.
- **Website deploy workflow** uses least-privilege `permissions` (`contents: read`, `pages: write`, `id-token: write`) and publishes only `website/`.

---

## 6. Suggested order of work

1. **Today:** C-1 upload-key reset (Play Console) and a new keystore outside the repo.
2. **Before the next release:** H-1 (real interstitial, or opt-in, plus soft-lock fix), H-2 (remove the resume interstitial), H-4 (one-shot rewards), M-4 (timeout and visible failures), M-5 (persisted caps).
3. **Before EEA/US-state traffic grows:** H-3 UMP plus LevelPlay consent and a "Privacy choices" row, then M-2 policy rewrite and a Data safety re-check, M-6 target-audience confirmation.
4. **Revenue hygiene:** M-3 app-ads.txt on a root domain; M-1 refund reconcile and backup exclusion.
5. Low items as you go. After any ad/billing change, re-run the playbook journeys (first launch, levels 1–4 including the level-3 ad, Game Over continue, the Remove Ads purchase and refund on a fresh real test account).
