package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.EconomyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyConfigTest {

    @Test
    fun `ad gating fires on every 3rd level in 1-10`() {
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(3))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(6))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(9))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(1))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(2))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(4))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(10))
    }

    @Test
    fun `ad gating fires on odd levels in 11-20`() {
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(11))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(13))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(15))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(17))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(19))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(12))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(14))
        assertFalse(EconomyConfig.shouldShowAdBetweenLevels(20))
    }

    @Test
    fun `ad gating fires on every level 21 and above`() {
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(21))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(22))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(30))
        assertTrue(EconomyConfig.shouldShowAdBetweenLevels(100))
    }

    @Test
    fun `milestone reward scales with level`() {
        assertEquals(25, EconomyConfig.milestoneRewardForLevel(1))
        assertEquals(35, EconomyConfig.milestoneRewardForLevel(2))
        assertEquals(115, EconomyConfig.milestoneRewardForLevel(10))
    }
}
