package com.ashwathai.bubbles

import android.app.Application
import android.util.Log
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BubblesApplication : Application() {

    companion object {
        // Composables observe this to know when it's safe to load ads.
        private val _sdkReady = MutableStateFlow(false)
        val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        initLevelPlay()
    }

    private fun initLevelPlay() {
        // LevelPlay test app key — safe to use before Play Store listing exists.
        // Replace with your production key (27c2f1a6d) when you go live.
        val appKey = "85460dcd"

        val initRequest = LevelPlayInitRequest.Builder(appKey)
            .build()

        LevelPlay.init(this, initRequest, object : LevelPlayInitListener {
            override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                Log.d("LevelPlay", "SDK initialized successfully")
                _sdkReady.value = true
            }

            override fun onInitFailed(error: LevelPlayInitError) {
                Log.e("LevelPlay", "SDK init failed: ${error.errorMessage}")
            }
        })
    }
}
