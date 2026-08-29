package com.ashwathai.bubbles

/**
 * Single source of truth for Unity LevelPlay monetization.
 * Auto-switches between test and production ad units via BuildConfig.DEBUG.
 *
 * Test key 25b63cf85 with paired test units — works on registered test devices.
 * Production key 27c2f1a6d — swap when ready for open testing.
 */
object AdConfig {
    val USE_TEST_ADS: Boolean get() = BuildConfig.DEBUG

    // ── App Keys ──
    const val PROD_APP_KEY = "27c2f1a6d"
    const val TEST_APP_KEY = "25b63cf85"

    // ── Production Ad Unit IDs (from Unity dashboard) ──
    const val PROD_REWARDED_AD_UNIT_ID = "ydwwpof94u2s7ub8"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "0pwpvuajbj0z8vju"
    const val PROD_BANNER_AD_UNIT_ID = "njj7kjn5k295lgsr"

    // ── Test Ad Unit IDs (paired with test key 25b63cf85) ──
    const val TEST_REWARDED_AD_UNIT_ID = "syz3d8ekts22q0or"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "h3xw38h9214adgxo"
    const val TEST_BANNER_AD_UNIT_ID = "4fpetq4lhe5lsw3e"

    // ── Resolved IDs (auto-switch) ──
    val LEVELPLAY_APP_KEY get() = if (USE_TEST_ADS) TEST_APP_KEY else PROD_APP_KEY
    val REWARDED_AD_UNIT_ID get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    val INTERSTITIAL_AD_UNIT_ID get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID
    val BANNER_AD_UNIT_ID get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    // ── Placement Names (for LevelPlay dashboard analytics) ──
    const val REWARDED_PLACEMENT = "reward"
    const val INTERSTITIAL_PLACEMENT = "foreground"
    const val BANNER_PLACEMENT = "banner"

    // ── Configured Flags ──
    val rewardedConfigured get() = REWARDED_AD_UNIT_ID.isNotBlank()
    val interstitialConfigured get() = INTERSTITIAL_AD_UNIT_ID.isNotBlank()
    val bannerConfigured get() = BANNER_AD_UNIT_ID.isNotBlank()
}
