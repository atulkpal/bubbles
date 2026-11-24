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
    val LEVEL_50 = Achievement("level_50", "Half Century", "Clear level 50")

    // ── Progress family ──
    val PRESTIGE_1 = Achievement("prestige_1", "Reborn in Gold", "Reach Prestige 1")
    val GAMES_10 = Achievement("games_10", "Regular", "Play 10 games")
    val DAILY_500 = Achievement("daily_500", "Daily Devotee", "Score 500 in a Daily Challenge")

    val all = listOf(
        POPS_100, POPS_1000, POPS_10000,
        COMBO_10, COMBO_25, COMBO_50,
        LEVEL_5, LEVEL_20, LEVEL_50,
        PRESTIGE_1, GAMES_10, DAILY_500
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
            if (highestLevel > 50) add(Achievements.LEVEL_50.id)
            // Progress
            if (economy.prestigeLevel >= 1) add(Achievements.PRESTIGE_1.id)
            if (economy.gamesPlayed >= 10) add(Achievements.GAMES_10.id)
            if (economy.dailyBest >= 500) add(Achievements.DAILY_500.id)
        }
        return satisfied - economy.unlockedAchievements
    }
}
