package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.BlowMechanics
import com.ashwathai.bubbles.domain.model.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Relaunch step 3b: hold-to-blow rules. */
class BlowMechanicsTest {

    private val config = GameConfig()

    @Test
    fun `bubble grows while held, never shrinks, and stops at full size`() {
        var last = 0f
        var t = 0f
        while (t <= 4f) {
            val r = BlowMechanics.radiusAt(config, t)
            assertTrue("never shrinks at $t", r >= last)
            last = r
            t += 0.05f
        }
        assertEquals(BlowMechanics.maxRadius(config), BlowMechanics.radiusAt(config, BlowMechanics.FULL_SEC), 0.01f)
        assertEquals(BlowMechanics.maxRadius(config), BlowMechanics.radiusAt(config, 10f), 0.01f)
        assertEquals(BlowMechanics.startRadius(config), BlowMechanics.radiusAt(config, 0f), 0.01f)
    }

    @Test
    fun `growth is quick at first then slows (ease-out)`() {
        val firstHalf = BlowMechanics.radiusAt(config, 1.25f) - BlowMechanics.radiusAt(config, 0f)
        val secondHalf = BlowMechanics.radiusAt(config, 2.5f) - BlowMechanics.radiusAt(config, 1.25f)
        assertTrue(firstHalf > secondHalf)
    }

    @Test
    fun `full bubble detaches by itself shortly after it is full`() {
        assertFalse(BlowMechanics.shouldAutoRelease(BlowMechanics.FULL_SEC))
        assertTrue(BlowMechanics.shouldAutoRelease(BlowMechanics.FULL_SEC + BlowMechanics.AUTO_RELEASE_AFTER_FULL_SEC))
    }

    @Test
    fun `bigger blown bubbles split into more pops`() {
        assertEquals(0, BlowMechanics.levelFor(config, config.minRadius))
        assertEquals(1, BlowMechanics.levelFor(config, config.minRadius + config.radiusStep))
        assertEquals(2, BlowMechanics.levelFor(config, BlowMechanics.maxRadius(config)))
    }

    @Test
    fun `released bubbles are never below the comfortable touch size`() {
        assertEquals(config.minRadius, BlowMechanics.releasedRadius(config, 5f), 0.001f)
        val big = BlowMechanics.maxRadius(config)
        assertEquals(big, BlowMechanics.releasedRadius(config, big), 0.001f)
    }

    @Test
    fun `bubble sits above the fingertip so the finger does not hide it`() {
        assertTrue(BlowMechanics.centerYAboveFinger(1000f, 100f) < 1000f)
    }
}
