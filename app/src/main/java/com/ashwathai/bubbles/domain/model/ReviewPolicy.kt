package com.ashwathai.bubbles.domain.model

/**
 * When to show Google Play's in-app review card (1.3.2). Asked at most once, only at a
 * happy moment: right after the player unlocks their first new soap, or, for players who
 * have not got that far, on a return visit (2nd day) once they have popped a fair amount.
 * Never during play, never in the first session. Play also applies its own quota.
 */
object ReviewPolicy {
    const val MIN_POPS_FOR_RETURN_ASK = 150
    const val MIN_DAYS_FOR_RETURN_ASK = 2

    fun shouldAsk(alreadyAsked: Boolean, newSoapJustUnlocked: Boolean, daysPlayed: Int, lifetimePops: Long): Boolean {
        if (alreadyAsked) return false
        if (newSoapJustUnlocked) return true
        return daysPlayed >= MIN_DAYS_FOR_RETURN_ASK && lifetimePops >= MIN_POPS_FOR_RETURN_ASK
    }
}
