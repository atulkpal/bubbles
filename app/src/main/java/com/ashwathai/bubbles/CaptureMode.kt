package com.ashwathai.bubbles

import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Debug-only "capture mode" for recording promo videos and Shorts (never active in release).
 *
 * Start it with:
 *   adb shell am start -n com.ashwathai.bubbles/.MainActivity --ez capture true --es soap MARIGOLD
 *
 * Effects: HUD, hints, chips, banners and system bars hidden; the chosen soap is used even if
 * locked; a 300 ms white SYNC flash is shown first; every sound the game plays is logged with
 * SystemClock.elapsedRealtime so tools/video/make_short.py can lay the real sound files onto
 * the (silent) screen recording frame-exactly.
 */
object CaptureMode {
    const val TAG = "HushpopCapture"

    var enabled: Boolean = false
        private set
    var soapId: String? = null
        private set

    fun readIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        if (intent.getBooleanExtra("capture", false)) {
            enabled = true
            soapId = intent.getStringExtra("soap")
            log("START", "soap=${soapId ?: "default"}")
        }
    }

    /** One line per audible event: `EVT <name> t=<ms> <extra>` (elapsedRealtime, ms). */
    fun log(name: String, extra: String = "") {
        if (!enabled) return
        Log.i(TAG, "EVT $name t=${SystemClock.elapsedRealtime()} $extra".trim())
    }
}
