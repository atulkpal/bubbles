package com.ashwathai.bubbles.domain.repository

import com.ashwathai.bubbles.domain.model.EconomyState
import kotlinx.coroutines.flow.StateFlow

interface EconomyRepository {
    val state: StateFlow<EconomyState>
    suspend fun addCoins(amount: Int)
    suspend fun spendCoins(amount: Int): Boolean
    suspend fun setUpgradeLevel(key: String, level: Int)
    suspend fun setSkin(name: String)
    suspend fun setTheme(name: String)
    suspend fun recordPops(count: Int)
    suspend fun recordMaxCombo(combo: Int)
    suspend fun recordGamePlayed()
    suspend fun prestige()
    suspend fun completeDailyChallenge(score: Int, date: String)
    suspend fun markBubbleTypeSeen(type: String)

    /**
     * Permanently records achievement ids as unlocked (additive, exactly-once) and, in the
     * same atomic write, pays [rewardPerUnlock] coins for each id that was not unlocked before.
     * Returns only the ids that were newly unlocked by this call.
     */
    suspend fun unlockAchievements(ids: Set<String>, rewardPerUnlock: Int = 0): Set<String>
}