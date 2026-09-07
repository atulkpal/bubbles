package com.ashwathai.bubbles

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryColors
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryTypography
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener

/**
 * LevelPlay banner composable — uses [AdConfig.BANNER_AD_UNIT_ID] for test/prod switching.
 * Zero-height collapse on failure (no blank space).
 * Destroy on dispose to prevent memory leaks.
 */
@Composable
fun LevelPlayBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sdkReady by LevelPlayAdManager.sdkReady.collectAsStateWithLifecycle()
    val adsRemoved by BillingManager.adsRemoved.collectAsStateWithLifecycle()

    if (adsRemoved) return
    if (!AdConfig.bannerConfigured) return
    if (!sdkReady) return

    var adFailed by remember { mutableStateOf(false) }
    var adStatus by remember { mutableStateOf("") }
    val showDebug = remember(context) {
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val bannerAdView = remember(context) {
        val config = LevelPlayBannerAdView.Config.Builder()
            .setAdSize(LevelPlayAdSize.BANNER)
            .build()
        LevelPlayBannerAdView(context, AdConfig.BANNER_AD_UNIT_ID, config).apply {
            setBannerListener(object : LevelPlayBannerAdViewListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    Log.d("LevelPlayBanner", "Banner loaded: ${adInfo.adUnitId}")
                    adFailed = false
                    adStatus = ""
                }
                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    Log.e("LevelPlayBanner", "Banner load failed: ${error.errorMessage}")
                    adFailed = true
                    adStatus = if (showDebug) "Banner failed: ${error.errorMessage}" else ""
                }
                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
                    adFailed = false
                    adStatus = ""
                }
                override fun onAdDisplayFailed(adInfo: LevelPlayAdInfo, error: LevelPlayAdError) {
                    adFailed = true
                    adStatus = if (showDebug) "Banner display failed: ${error.errorMessage}" else ""
                }
                override fun onAdClicked(adInfo: LevelPlayAdInfo) {}
                override fun onAdExpanded(adInfo: LevelPlayAdInfo) {}
                override fun onAdCollapsed(adInfo: LevelPlayAdInfo) {}
                override fun onAdLeftApplication(adInfo: LevelPlayAdInfo) {}
            })
        }
    }

    LaunchedEffect(bannerAdView) {
        adStatus = "Loading ad"
        bannerAdView.loadAd()
    }

    DisposableEffect(bannerAdView) {
        onDispose { bannerAdView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (adFailed) 0.dp else 50.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!adFailed) {
            AndroidView(
                factory = { bannerAdView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
        if (showDebug && adStatus.isNotEmpty()) {
            Text(
                text = adStatus,
                style = LuxuryTypography.LabelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuxuryColors.Ink950.copy(alpha = 0.78f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
