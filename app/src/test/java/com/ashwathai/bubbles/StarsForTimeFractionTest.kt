package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.starsForTimeFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class StarsForTimeFractionTest {

    @Test
    fun `half the timer remaining earns three stars`() {
        assertEquals(3, starsForTimeFraction(0.5f))
        assertEquals(3, starsForTimeFraction(0.73f))
        assertEquals(3, starsForTimeFraction(1f))
    }

    @Test
    fun `quarter of the timer remaining earns two stars`() {
        assertEquals(2, starsForTimeFraction(0.25f))
        assertEquals(2, starsForTimeFraction(0.49f))
    }

    @Test
    fun `a clear with almost no time left still earns one star`() {
        assertEquals(1, starsForTimeFraction(0f))
        assertEquals(1, starsForTimeFraction(0.1f))
        assertEquals(1, starsForTimeFraction(0.24f))
    }

    @Test
    fun `negative fractions clamp to one star`() {
        assertEquals(1, starsForTimeFraction(-0.2f))
    }
}
