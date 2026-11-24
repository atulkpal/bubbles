package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.EconomyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyConfigTest {

    @Test
    fun `no between-level ad before level 6`() {
        (1..5).forEach { assertFalse("level $it", EconomyConfig.shouldShowAdBetweenLevels(it)) }
    }

    @Test
    fun `between-level ad every 3rd level from 6 on, never every level`() {
        listOf(6, 9, 12, 21, 99).forEach { assertTrue("level $it", EconomyConfig.shouldShowAdBetweenLevels(it)) }
        listOf(7, 8, 10, 11, 20, 22, 100).forEach { assertFalse("level $it", EconomyConfig.shouldShowAdBetweenLevels(it)) }
        // At most one level in three ever shows an ad
        assertTrue((1..100).count { EconomyConfig.shouldShowAdBetweenLevels(it) } <= 34)
    }

    @Test
    fun `milestone reward scales with level`() {
        assertEquals(25, EconomyConfig.milestoneRewardForLevel(1))
        assertEquals(35, EconomyConfig.milestoneRewardForLevel(2))
        assertEquals(115, EconomyConfig.milestoneRewardForLevel(10))
    }
}
