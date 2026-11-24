package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.ReviewPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 1.3.2: in-app review is asked once, only at a happy moment. */
class ReviewPolicyTest {

    @Test
    fun `asks right after the first new soap unlocks`() {
        assertTrue(ReviewPolicy.shouldAsk(alreadyAsked = false, newSoapJustUnlocked = true, daysPlayed = 1, lifetimePops = 250))
    }

    @Test
    fun `never asks twice`() {
        assertFalse(ReviewPolicy.shouldAsk(alreadyAsked = true, newSoapJustUnlocked = true, daysPlayed = 9, lifetimePops = 9_999))
    }

    @Test
    fun `does not ask in the first session without a soap moment`() {
        assertFalse(ReviewPolicy.shouldAsk(alreadyAsked = false, newSoapJustUnlocked = false, daysPlayed = 1, lifetimePops = 500))
    }

    @Test
    fun `asks returning players who have popped a fair amount`() {
        assertTrue(ReviewPolicy.shouldAsk(false, false, daysPlayed = 2, lifetimePops = 150))
        assertFalse(ReviewPolicy.shouldAsk(false, false, daysPlayed = 2, lifetimePops = 149))
    }
}
