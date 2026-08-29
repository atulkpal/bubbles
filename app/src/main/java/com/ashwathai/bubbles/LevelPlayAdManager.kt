package com.ashwathai.bubbles

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
 * - Foreground interstitial replaces App Open format (LevelPlay has none).
 * - Fail-closed everywhere: no ad → no reward → user-facing feedback only.
 *
 * Ad placements:
 *   - Rewarded: continue, retry_daily, double_daily, coin_bonus, powerup_guarantee, skin_preview
 *   - Interstitial: foreground (onStart after 4h), level_complete (every 3rd)
 */
object LevelPlayAdManager {

    private const val TAG = "LevelPlayAdManager"

    // ── SDK Readiness ──
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private var appContext: Context? = null
    private var currentActivity: Activity? = null
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
    private var lastForegroundInterstitialAt = 0L
    private const val FOREGROUND_FRESHNESS_MS = 4 * 60 * 60 * 1000L // 4h

    // ── Level-Complete Interstitial Pacing ──
    private var levelCompletionCount = 0
    private var lastLevelInterstitialAt = 0L
    private const val LEVEL_INTERSTITIAL_EVERY = 3
    private const val LEVEL_INTERSTITIAL_MIN_GAP_MS = 3 * 60 * 1000L // 3 min

    // ── Daily Ad Cap ──
    private var adsShownToday = 0
    private const val MAX_ADS_PER_DAY = 5
    private var lastAdDate = ""

    // ── Lifecycle Observer ──
    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            maybeShowForegroundInterstitial()
        }
    }
    private var foregroundWired = false

    // ── Init ──

    fun init(context: Context) {
        if (_sdkReady.value) return
        initAttempted = true
        appContext = context.applicationContext

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
        currentActivity = activity
        if (!foregroundWired) {
            foregroundWired = true
            ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundObserver)
        }
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
            adsShownToday++
            updateDailyCount()
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
                }
                override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                    Log.d(TAG, "Interstitial clicked | ${adInfoTag(adInfo)}")
                }
                override fun onAdClosed(adInfo: LevelPlayAdInfo) {
                    Log.d(TAG, "Interstitial closed | ${adInfoTag(adInfo)}")
                }
                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) { /* waterfalls */ }
            })
            loadAd()
        }
    }

    private fun maybeShowForegroundInterstitial() {
        val activity = currentActivity ?: return
        val now = System.currentTimeMillis()
        if (now - lastForegroundInterstitialAt < FOREGROUND_FRESHNESS_MS) return
        if (!AdConfig.interstitialConfigured || !_sdkReady.value) return

        val ad = interstitialAd ?: LevelPlayInterstitialAd(AdConfig.INTERSTITIAL_AD_UNIT_ID).also {
            interstitialAd = it
            setupInterstitial()
        }
        if (ad.isAdReady) {
            lastForegroundInterstitialAt = now
            ad.showAd(activity, AdConfig.INTERSTITIAL_PLACEMENT)
            ad.loadAd() // reload for next foreground
        } else {
            ad.loadAd()
        }
    }

    /**
     * Show an interstitial after every Nth level completion.
     * Called from GameViewModel after GameState.LevelComplete is emitted.
     * Never blocks the reward flow — only fires if the ad is ready.
     */
    fun maybeShowLevelCompleteInterstitial(activity: Activity) {
        if (!AdConfig.interstitialConfigured || !_sdkReady.value) return
        levelCompletionCount++
        if (levelCompletionCount % LEVEL_INTERSTITIAL_EVERY != 0) return
        val now = System.currentTimeMillis()
        if (now - lastLevelInterstitialAt < LEVEL_INTERSTITIAL_MIN_GAP_MS) return

        val ad = interstitialAd ?: LevelPlayInterstitialAd(AdConfig.INTERSTITIAL_AD_UNIT_ID).also {
            interstitialAd = it
            setupInterstitial()
        }
        if (ad.isAdReady) {
            lastLevelInterstitialAt = now
            Log.i(TAG, "Level-complete interstitial DISPLAYED (completion #$levelCompletionCount)")
            ad.showAd(activity, "level_complete")
        } else {
            ad.loadAd()
        }
    }

    // ── Daily Cap ──

    private fun checkDailyCap() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (today != lastAdDate) {
            lastAdDate = today
            adsShownToday = 0
        }
    }

    private fun updateDailyCount() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (today != lastAdDate) {
            lastAdDate = today
            adsShownToday = 1
        }
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
