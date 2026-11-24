package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.BreathCycle
import com.ashwathai.bubbles.domain.model.BreathCycle.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Relaunch step 5: Breathe mode timing. */
class BreathCycleTest {

    @Test
    fun `phases run inhale 4s, hold 2s, exhale 6s, then pop`() {
        assertEquals(Phase.INHALE, BreathCycle.stateAt(0f).phase)
        assertEquals(Phase.INHALE, BreathCycle.stateAt(3.9f).phase)
        assertEquals(Phase.HOLD, BreathCycle.stateAt(4.1f).phase)
        assertEquals(Phase.EXHALE, BreathCycle.stateAt(6.1f).phase)
        assertEquals(Phase.EXHALE, BreathCycle.stateAt(11.9f).phase)
        assertEquals(Phase.POP, BreathCycle.stateAt(12.1f).phase)
        assertEquals(Phase.DONE, BreathCycle.stateAt(BreathCycle.CYCLE_SEC).phase)
    }

    @Test
    fun `the bubble can only be popped after the full breath`() {
        assertFalse(BreathCycle.canPop(2f))
        assertFalse(BreathCycle.canPop(8f))
        assertTrue(BreathCycle.canPop(13f))
    }

    @Test
    fun `bubble inflates during the inhale and rises during the exhale`() {
        assertTrue(BreathCycle.inflation(1f) < BreathCycle.inflation(3f))
        assertEquals(1f, BreathCycle.inflation(5f), 0.0001f)
        assertTrue(BreathCycle.heightFraction(12f) < BreathCycle.heightFraction(6f))
    }

    @Test
    fun `sessions hold a sensible number of breaths`() {
        assertEquals(4, BreathCycle.breathsIn(1))
        assertEquals(12, BreathCycle.breathsIn(3))
        assertEquals(20, BreathCycle.breathsIn(5))
    }
}
