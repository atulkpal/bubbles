package com.ashwathai.bubbles

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unity LevelPlay ad facade — replaces any future AdMob integration.
 *
 * - Singleton object accessed directly (no interface).
 * - Init in Application.onCreate via [init]; UI gates on [sdkReady].
 * - Rewarded ads use load-and-show pattern with preload-after-close.
 * - Fail-closed everywhere: no ad → no reward → user-facing feedback only.
 * - Rewarded ads are ONLY ever shown after the player taps a button that offers them.
 * - No app-open / return-to-app interstitials (removed in v1.3: Play disruptive-ads policy).
 *
 * Ad placements:
 *   - Rewarded (opt-in): continue, retry_daily, double_daily, coin_bonus, powerup_guarantee
 *   - Interstitial: level_complete (paced by EconomyConfig + a minimum time gap)
 */
object LevelPlayAdManager {

    private const val TAG = "LevelPlayAdManager"

    // ── SDK Readiness ──
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private var appContext: Context? = null
    private var currentActivityRef: java.lang.ref.WeakReference<Activity>? = null
    private val currentActivity: Activity? get() = currentActivityRef?.get()
    private var initAttempted = false

    // ── Rewarded Ad State ──
    private var rewardedAd: LevelPlayRewardedAd? = null
    private var rewardedCallbacks: Triple<() -> Unit, (String) -> Unit, () -> Unit>? = null
    private var rewardedRetryCount = 0
    private const val MAX_REWARDED_RETRIES = 3
    private var lastRewardedTapAt = 0L
    private const val REWARDED_COOLDOWN_MS = 60_000L // 60s cooldown between requests

    // ── Interstitial Ad State ──
    private var interstitialAd: LevelPlayInterstitialAd? = null
    /** Continuation for the interstitial currently on screen; run exactly once on close/fail. */
    private var interstitialOnDone: (() -> Unit)? = null
    private var lastLevelInterstitialAt = 0L
    private const val LEVEL_INTERSTITIAL_MIN_GAP_MS = 3 * 60 * 1000L // 3 min

    // ── Daily cap for opt-in rewarded ads (persisted so a restart doesn't reset it) ──
    private const val PREFS = "levelplay_ads"
    private const val KEY_DATE = "rewarded_date"
    private const val KEY_COUNT = "rewarded_count"
    private const val KEY_SESSIONS = "sessions_started"
    /** Sessions with no banner and no interstitial at all (relaunch spec §8). */
    const val AD_FREE_SESSIONS = 3
    private var sessionCount = 0

    /** Banners and interstitials only after the first sessions, and never for supporters. */
    val nonRewardedAllowed: Boolean
        get() = sessionCount > AD_FREE_SESSIONS && !BillingManager.adsRemoved.value

    private var adsShownToday = 0
    private const val MAX_ADS_PER_DAY = 5
    private var lastAdDate = ""

    // ── Init ──

    fun init(context: Context) {
        if (_sdkReady.value) return
        initAttempted = true
        appContext = context.applicationContext
        loadDailyCount()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).let { p ->
            sessionCount = p.getInt(KEY_SESSIONS, 0) + 1
            p.edit().putInt(KEY_SESSIONS, sessionCount).apply()
        }

        // Privacy signal, set before init: a general-audience game, not directed at
        // children (must match the Play Console target-audience setting).
        LevelPlay.setMetaData("is_child_directed", "false")

        LevelPlay.init(
            context,
            LevelPlayInitRequest.Builder(AdConfig.LEVELPLAY_APP_KEY).build(),
            object : LevelPlayInitListener {
                override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                    Log.i(TAG, "LevelPlay initialized")
                    _sdkReady.value = true
                    preloadRewarded()
                    setupInterstitial()
                }

                override fun onInitFailed(error: LevelPlayInitError) {
                    initAttempted = false // allow retry on next ad request
                    Log.e(TAG, "LevelPlay init failed: ${error.errorMessage} — will retry on next ad request")
                }
            }
        )
    }

    fun trackActivity(activity: Activity) {
        currentActivityRef = java.lang.ref.WeakReference(activity)
    }

    // ── Rewarded Ads ──

    /**
     * Load and show a rewarded ad. The [adType] is for dashboard analytics
     * (e.g. "continue", "retry_daily", "coin_bonus").
     *
     * Contract: [onUserEarnedReward] is called ONLY after the ad is fully watched.
     * If the ad fails to load or show, [onAdFailed] is called — NO reward is granted.
     */
    fun loadAndShowRewardedAd(
        adType: String,
        activity: Activity,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit,
        onUserEarnedReward: () -> Unit
    ) {
        // Ad-free: grant reward immediately without showing ad
        if (BillingManager.adsRemoved.value) {
            onAdLoaded()
            onUserEarnedReward()
            return
        }

        // Daily cap check
        checkDailyCap()

        if (adsShownToday >= MAX_ADS_PER_DAY) {
            onAdFailed("Daily ad limit reached ($MAX_ADS_PER_DAY/day)")
            return
        }

        if (!AdConfig.rewardedConfigured) {
            onAdFailed("Rewarded ads not configured yet")
            return
        }

        // Cooldown check
        val now = System.currentTimeMillis()
        if (now - lastRewardedTapAt < REWARDED_COOLDOWN_MS) {
            val wait = (REWARDED_COOLDOWN_MS - (now - lastRewardedTapAt)) / 1000
            onAdFailed("Cooldown active — try again in ${wait}s")
            return
        }
        lastRewardedTapAt = now

        // Retry init if previous attempt failed
        if (!_sdkReady.value && appContext != null && !initAttempted) {
            init(appContext!!)
        }
        if (!_sdkReady.value) {
            onAdFailed("Ads SDK still initializing. Try again in a moment.")
            return
        }

        Log.d(TAG, "Rewarded request: unit=${AdConfig.REWARDED_AD_UNIT_ID} type=$adType")
        rewardedCallbacks = Triple(onAdLoaded, onAdFailed, onUserEarnedReward)

        val ad = rewardedAd ?: LevelPlayRewardedAd(AdConfig.REWARDED_AD_UNIT_ID).also {
            rewardedAd = it
            it.setListener(rewardedListener)
        }

        if (ad.isAdReady) {
            onAdLoaded()
            ad.showAd(activity, AdConfig.REWARDED_PLACEMENT)
        } else {
            ad.loadAd()
        }
    }

    private fun adInfoTag(adInfo: LevelPlayAdInfo): String =
        "unit=${adInfo.adUnitName}(${adInfo.adUnitId}) network=${adInfo.adNetwork}"

    private val rewardedListener = object : LevelPlayRewardedAdListener {
        override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
            Log.d(TAG, "Rewarded loaded | ${adInfoTag(adInfo)}")
            val activity = currentActivity
            val callbacks = rewardedCallbacks
            if (activity == null || callbacks == null) {
                onAdFailedInternal()
                return
            }
            callbacks.first.invoke() // onAdLoaded
            rewardedAd?.showAd(activity, AdConfig.REWARDED_PLACEMENT)
        }

        override fun onAdLoadFailed(error: LevelPlayAdError) {
            // "Load is already called" = our load raced the SDK's parallelLoad — benign
            if (error.errorMessage.contains("already called", ignoreCase = true)) {
                Log.d(TAG, "Rewarded load skipped (load already in flight)")
                return
            }
            Log.e(TAG, "Rewarded load failed: ${error.errorMessage}")
            onAdFailedInternal(error.errorMessage)
        }

        override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) {
            Log.i(TAG, "Reward earned: ${reward.name} x${reward.amount} | ${adInfoTag(adInfo)}")
            rewardedRetryCount = 0 // genuine success — restore full retry budget
            rewardedCallbacks?.third?.invoke() // onUserEarnedReward
        }

        override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
            Log.i(TAG, "Rewarded DISPLAYED | ${adInfoTag(adInfo)} | revenue=${adInfo.revenue}")
            checkDailyCap()
            adsShownToday++
            saveDailyCount()
        }

        override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
            Log.e(TAG, "Rewarded display failed: ${error.errorMessage} | ${adInfoTag(adInfo)}")
            onAdFailedInternal(error.errorMessage)
        }

        override fun onAdClosed(adInfo: LevelPlayAdInfo) {
            Log.d(TAG, "Rewarded closed | ${adInfoTag(adInfo)}")
            // Cycle complete: drop stale callbacks to prevent double reward grants
            rewardedCallbacks = null
            rewardedRetryCount = 0
            preloadRewarded()
        }

        override fun onAdClicked(adInfo: LevelPlayAdInfo) {
            Log.d(TAG, "Rewarded clicked | ${adInfoTag(adInfo)}")
        }

        override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) { /* waterfalls */ }
    }

    private fun onAdFailedInternal(message: String? = null) {
        rewardedCallbacks?.second?.invoke(message ?: "Ad unavailable")
        rewardedCallbacks = null
        if (rewardedRetryCount < MAX_REWARDED_RETRIES) {
            rewardedRetryCount++
            preloadRewarded()
        }
    }

    private fun preloadRewarded() {
        if (AdConfig.rewardedConfigured && _sdkReady.value) {
            val ad = rewardedAd ?: LevelPlayRewardedAd(AdConfig.REWARDED_AD_UNIT_ID).also {
                rewardedAd = it
                it.setListener(rewardedListener)
            }
            if (!ad.isAdReady) ad.loadAd()
        }
    }

    // ── Foreground Interstitial (replaces App Open) ──

    private fun setupInterstitial() {
        if (!AdConfig.interstitialConfigured) return
        interstitialAd = LevelPlayInterstitialAd(AdConfig.INTERSTITIAL_AD_UNIT_ID).apply {
            setListener(object : LevelPlayInterstitialAdListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    Log.d(TAG, "Interstitial loaded | ${adInfoTag(adInfo)}")
                }
                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    Log.e(TAG, "Interstitial load failed: ${error.errorMessage}")
                }
                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
                    Log.i(TAG, "Interstitial DISPLAYED | ${adInfoTag(adInfo)} | revenue=${adInfo.revenue}")
                }
                override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
                    Log.e(TAG, "Interstitial display failed: ${error.errorMessage} | ${adInfoTag(adInfo)}")
                    finishInterstitial()
                }
                override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                    Log.d(TAG, "Interstitial clicked | ${adInfoTag(adInfo)}")
                }
                override fun onAdClosed(adInfo: LevelPlayAdInfo) {
                    Log.d(TAG, "Interstitial closed | ${adInfoTag(adInfo)}")
                    finishInterstitial()
                    loadAd() // preload the next one
                }
                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) { /* waterfalls */ }
            })
            loadAd()
        }
    }

    private fun finishInterstitial() {
        val done = interstitialOnDone
        interstitialOnDone = null
        done?.invoke()
    }

    /**
     * Between-level interstitial. The caller decides *whether* this level earns an ad
     * (EconomyConfig.shouldShowAdBetweenLevels); this enforces ad-free, readiness and the
     * minimum time gap. [onDone] ALWAYS runs exactly once: after the ad closes, if it
     * fails to display, or immediately when no ad is shown, so the game can never stall.
     */
    fun showLevelCompleteInterstitial(activity: Activity, onDone: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        val canShow = nonRewardedAllowed &&
            AdConfig.interstitialConfigured && _sdkReady.value &&
            now - lastLevelInterstitialAt >= LEVEL_INTERSTITIAL_MIN_GAP_MS &&
            ad != null && ad.isAdReady
        if (!canShow) {
            if (ad != null && _sdkReady.value && !ad.isAdReady) ad.loadAd()
            onDone()
            return
        }
        lastLevelInterstitialAt = now
        interstitialOnDone = onDone
        ad!!.showAd(activity, "level_complete")
    }

    // ── Daily Cap ──

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun checkDailyCap() {
        val today = today()
        if (today != lastAdDate) {
            lastAdDate = today
            adsShownToday = 0
            saveDailyCount()
        }
    }

    private fun loadDailyCount() {
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        lastAdDate = prefs.getString(KEY_DATE, "") ?: ""
        adsShownToday = prefs.getInt(KEY_COUNT, 0)
        checkDailyCap()
    }

    private fun saveDailyCount() {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_DATE, lastAdDate)
            ?.putInt(KEY_COUNT, adsShownToday)
            ?.apply()
    }

    /** Check if the user has ads remaining today. */
    fun hasAdsRemaining(): Boolean {
        checkDailyCap()
        return adsShownToday < MAX_ADS_PER_DAY
    }

    /** How many ads are left today. */
    fun adsRemaining(): Int {
        checkDailyCap()
        return (MAX_ADS_PER_DAY - adsShownToday).coerceAtLeast(0)
    }
}
