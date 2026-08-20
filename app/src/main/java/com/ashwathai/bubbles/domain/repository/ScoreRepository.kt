package com.ashwathai.bubbles.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ScoreRepository {
    val highScore: StateFlow<Int>
    val zenBest: StateFlow<Int>
    val gamesPlayed: StateFlow<Int>
    suspend fun setHighScore(score: Int)
    suspend fun setZenBest(score: Int)
    suspend fun incrementGamesPlayed()
}