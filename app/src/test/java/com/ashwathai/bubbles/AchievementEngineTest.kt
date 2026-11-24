package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.AchievementEngine
import com.ashwathai.bubbles.domain.model.Achievements
import com.ashwathai.bubbles.domain.model.EconomyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {

    // ── Catalog integrity ──

    @Test
    fun `catalog has exactly 12 achievements with unique ids`() {
        assertEquals(12, Achievements.all.size)
        assertEquals(12, Achievements.all.map { it.id }.toSet().size)
    }

    @Test
    fun `every achievement rewards 25 coins`() {
        Achievements.all.forEach { assertEquals(25, it.rewardCoins) }
    }

    // ── Pops family ──

    @Test
    fun `fresh state unlocks nothing`() {
        val unlocked = AchievementEngine.evaluate(EconomyState(), highestLevel = 1)
        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `100 lifetime pops unlocks pops_100`() {
        val economy = EconomyState(lifetimePops = 100)
        assertEquals(setOf("pops_100"), AchievementEngine.evaluate(economy, 1))
    }

    @Test
    fun `99 lifetime pops unlocks nothing`() {
        val economy = EconomyState(lifetimePops = 99)
        assertTrue(AchievementEngine.evaluate(economy, 1).isEmpty())
    }

    @Test
    fun `10_000 lifetime pops unlocks the whole pops family at once`() {
        val economy = EconomyState(lifetimePops = 10_000)
        assertEquals(
            setOf("pops_100", "pops_1000", "pops_10000"),
            AchievementEngine.evaluate(economy, 1)
        )
    }

    // ── Combo family ──

    @Test
    fun `9 max combo unlocks nothing`() {
        val economy = EconomyState(maxCombo = 9)
        assertTrue(AchievementEngine.evaluate(economy, 1).isEmpty())
    }

    @Test
    fun `25 max combo unlocks combo_10 and combo_25`() {
        val economy = EconomyState(maxCombo = 25)
        assertEquals(
            setOf("combo_10", "combo_25"),
            AchievementEngine.evaluate(economy, 1)
        )
    }

    @Test
    fun `50 max combo unlocks the whole combo family`() {
        val economy = EconomyState(maxCombo = 50)
        assertEquals(
            setOf("combo_10", "combo_25", "combo_50"),
            AchievementEngine.evaluate(economy, 1)
        )
    }

    // ── Level family (highestLevel is next playable; clearing N persists N+1) ──

    @Test
    fun `highestLevel 5 means level 5 not yet cleared and unlocks nothing`() {
        val economy = EconomyState()
        assertTrue(AchievementEngine.evaluate(economy, highestLevel = 5).isEmpty())
    }

    @Test
    fun `clearing level 5 unlocks level_5`() {
        val economy = EconomyState()
        assertEquals(setOf("level_5"), AchievementEngine.evaluate(economy, highestLevel = 6))
    }

    @Test
    fun `clearing all 20 Challenge levels unlocks the whole level family`() {
        val economy = EconomyState()
        assertEquals(
            setOf("level_5", "level_20"),
            AchievementEngine.evaluate(economy, highestLevel = 21)
        )
    }

    // ── Soap family (relaunch) ──

    @Test
    fun `unlocking a second soap unlocks soap_1`() {
        val economy = EconomyState(unlockedSoaps = setOf("CLEAR_NIGHT", "LAVENDER_DUSK"))
        assertEquals(setOf("soap_1"), AchievementEngine.evaluate(economy, 1))
    }

    @Test
    fun `owning every soap unlocks soaps_all`() {
        val economy = EconomyState(unlockedSoaps = com.ashwathai.bubbles.domain.model.Soaps.ALL.map { it.id }.toSet())
        assertEquals(setOf("soap_1", "soaps_all"), AchievementEngine.evaluate(economy, 1))
    }

    // ── Progress family ──

    @Test
    fun `no achievement depends on removed features (prestige, daily score, level 50)`() {
        val economy = EconomyState(prestigeLevel = 9, dailyBest = 99_999)
        assertEquals(emptySet<String>(), AchievementEngine.evaluate(economy, highestLevel = 1))
    }

    @Test
    fun `10 games played unlocks games_10`() {
        val economy = EconomyState(gamesPlayed = 10)
        assertEquals(setOf("games_10"), AchievementEngine.evaluate(economy, 1))
    }

    @Test
    fun `popping on 7 different days unlocks days_7`() {
        val economy = EconomyState(daysPlayed = 7)
        assertEquals(setOf("days_7"), AchievementEngine.evaluate(economy, 1))
    }

    // ── Exactly-once semantics ──

    @Test
    fun `already unlocked achievements are never re-returned`() {
        val economy = EconomyState(
            lifetimePops = 100,
            unlockedAchievements = setOf("pops_100")
        )
        assertTrue(AchievementEngine.evaluate(economy, 1).isEmpty())
    }

    @Test
    fun `mixed state returns only the not-yet-unlocked satisfied ids`() {
        val economy = EconomyState(
            lifetimePops = 1_000,
            maxCombo = 12,
            unlockedAchievements = setOf("pops_100", "pops_1000", "combo_10")
        )
        assertTrue(AchievementEngine.evaluate(economy, 1).isEmpty())
    }

    @Test
    fun `maxed stats unlock everything not already unlocked`() {
        val economy = EconomyState(
            lifetimePops = 10_000,
            maxCombo = 99,
            gamesPlayed = 500,
            daysPlayed = 30,
            unlockedSoaps = com.ashwathai.bubbles.domain.model.Soaps.ALL.map { it.id }.toSet()
        )
        assertEquals(
            setOf(
                "pops_100", "pops_1000", "pops_10000",
                "combo_10", "combo_25", "combo_50",
                "level_5", "level_20", "soap_1",
                "soaps_all", "games_10", "days_7"
            ),
            AchievementEngine.evaluate(economy, highestLevel = 21)
        )
    }
}
