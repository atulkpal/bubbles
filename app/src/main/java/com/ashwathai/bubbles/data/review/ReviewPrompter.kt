package com.ashwathai.bubbles.data.review

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ashwathai.bubbles.domain.model.ReviewPolicy
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Shows Google Play's official in-app review card at most once (see [ReviewPolicy]).
 * The "asked" flag is saved before launching, so a dismissed or quota-suppressed card is
 * never retried by us. Play decides whether the card actually appears.
 */
class ReviewPrompter(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("review_prompt", Context.MODE_PRIVATE)

    fun maybeAsk(activity: Activity, newSoapJustUnlocked: Boolean, daysPlayed: Int, lifetimePops: Long) {
        val asked = prefs.getBoolean(KEY_ASKED, false)
        if (!ReviewPolicy.shouldAsk(asked, newSoapJustUnlocked, daysPlayed, lifetimePops)) return
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                Log.w("ReviewPrompter", "Review flow unavailable: ${request.exception?.message}")
            }
        }
    }

    private companion object {
        const val KEY_ASKED = "asked_once"
    }
}
