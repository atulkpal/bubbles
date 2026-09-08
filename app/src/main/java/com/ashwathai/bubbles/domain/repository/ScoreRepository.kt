package com.ashwathai.bubbles.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ScoreRepository {
    val highScore: StateFlow<Int>
    val zenBest: StateFlow<Int>
    val gamesPlayed: StateFlow<Int>

    /** Highest adventure level cleared (1-based). Level 1 is always playable. */
    val highestLevel: StateFlow<Int>
    suspend fun setHighScore(score: Int)
    suspend fun setZenBest(score: Int)
    suspend fun incrementGamesPlayed()
    suspend fun setHighestLevel(level: Int)
}