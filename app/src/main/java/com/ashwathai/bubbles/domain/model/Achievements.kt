package com.ashwathai.bubbles.domain.model

/**
 * Local milestone achievements. Data-only — no network, no leaderboards.
 * Evaluated by [AchievementEngine] against [EconomyState] and persisted via
 * `EconomyState.unlockedAchievements`.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int = Achievements.REWARD_COINS
)

object Achievements {
    const val REWARD_COINS = 25

    // ── Pops family ──
    val POPS_100 = Achievement("pops_100", "First Hundred", "Pop 100 bubbles")
    val POPS_1000 = Achievement("pops_1000", "Pop Enthusiast", "Pop 1,000 bubbles")
    val POPS_10000 = Achievement("pops_10000", "Bubble Legend", "Pop 10,000 bubbles")

    // ── Combo family ──
    val COMBO_10 = Achievement("combo_10", "Chain Starter", "Reach a 10x combo")
    val COMBO_25 = Achievement("combo_25", "Combo Artist", "Reach a 25x combo")
    val COMBO_50 = Achievement("combo_50", "Combo Master", "Reach a 50x combo")

    // ── Level family (clearing N persists highestLevel = N + 1) ──
    val LEVEL_5 = Achievement("level_5", "Getting Somewhere", "Clear level 5")
    val LEVEL_20 = Achievement("level_20", "Deep Diver", "Clear level 20")
    // Relaunch: Challenge ends at level 20, so "level 50" became the first new soap.
    val SOAP_1 = Achievement("soap_1", "First Soap", "Fill the jar and unlock a new soap")

    // ── Progress family ──
    val SOAPS_ALL = Achievement("soaps_all", "Soap Collector", "Collect every soap on the shelf")
    val GAMES_10 = Achievement("games_10", "Regular", "Play 10 games")
    val DAYS_7 = Achievement("days_7", "A Calm Week", "Pop on 7 different days")

    val all = listOf(
        POPS_100, POPS_1000, POPS_10000,
        COMBO_10, COMBO_25, COMBO_50,
        LEVEL_5, LEVEL_20, SOAP_1,
        SOAPS_ALL, GAMES_10, DAYS_7
    )

    fun fromId(id: String): Achievement? = all.firstOrNull { it.id == id }
}

/**
 * Pure evaluator: returns the ids of achievements whose thresholds are satisfied
 * by [economy]/[highestLevel] but not already present in
 * [EconomyState.unlockedAchievements]. Never re-returns unlocked ids (exactly-once).
 */
object AchievementEngine {

    fun evaluate(economy: EconomyState, highestLevel: Int): Set<String> {
        val satisfied = buildSet {
            // Pops
            if (economy.lifetimePops >= 100) add(Achievements.POPS_100.id)
            if (economy.lifetimePops >= 1_000) add(Achievements.POPS_1000.id)
            if (economy.lifetimePops >= 10_000) add(Achievements.POPS_10000.id)
            // Combo
            if (economy.maxCombo >= 10) add(Achievements.COMBO_10.id)
            if (economy.maxCombo >= 25) add(Achievements.COMBO_25.id)
            if (economy.maxCombo >= 50) add(Achievements.COMBO_50.id)
            // Levels — highestLevel is the *next playable* level, so cleared N means highestLevel > N
            if (highestLevel > 5) add(Achievements.LEVEL_5.id)
            if (highestLevel > 20) add(Achievements.LEVEL_20.id)
            // Soap Shelf
            if (economy.unlockedSoaps.size >= 2) add(Achievements.SOAP_1.id)
            if (Soaps.ALL.all { it.id in economy.unlockedSoaps }) add(Achievements.SOAPS_ALL.id)
            // Progress
            if (economy.gamesPlayed >= 10) add(Achievements.GAMES_10.id)
            if (economy.daysPlayed >= 7) add(Achievements.DAYS_7.id)
        }
        return satisfied - economy.unlockedAchievements
    }
}
