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
}