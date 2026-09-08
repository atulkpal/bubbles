package com.ashwathai.bubbles.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ashwathai.bubbles.domain.repository.ScoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ScoreRepositoryImpl(private val dataStore: DataStore<Preferences>) : ScoreRepository {

    private val HIGH_SCORE_KEY = intPreferencesKey("high_score")
    private val ZEN_BEST_KEY = intPreferencesKey("zen_best")
    private val GAMES_PLAYED_KEY = intPreferencesKey("games_played")
    private val HIGHEST_LEVEL_KEY = intPreferencesKey("highest_level")

    private val _highScore = MutableStateFlow(0)
    override val highScore: kotlinx.coroutines.flow.StateFlow<Int> = _highScore.asStateFlow()

    private val _zenBest = MutableStateFlow(0)
    override val zenBest: kotlinx.coroutines.flow.StateFlow<Int> = _zenBest.asStateFlow()

    private val _gamesPlayed = MutableStateFlow(0)
    override val gamesPlayed: kotlinx.coroutines.flow.StateFlow<Int> = _gamesPlayed.asStateFlow()

    private val _highestLevel = MutableStateFlow(1)
    override val highestLevel: kotlinx.coroutines.flow.StateFlow<Int> = _highestLevel.asStateFlow()

    init {
        loadHighScore()
        loadZenBest()
        loadGamesPlayed()
        loadHighestLevel()
    }

    private fun loadHighestLevel() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull()
            if (prefs != null) {
                _highestLevel.value = (prefs[HIGHEST_LEVEL_KEY] ?: 1).coerceAtLeast(1)
            }
        }
    }

    private fun loadHighScore() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull()
            if (prefs != null) {
                _highScore.value = prefs[HIGH_SCORE_KEY] ?: 0
            }
        }
    }

    private fun loadZenBest() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull()
            if (prefs != null) {
                _zenBest.value = prefs[ZEN_BEST_KEY] ?: 0
            }
        }
    }

    private fun loadGamesPlayed() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull()
            if (prefs != null) {
                _gamesPlayed.value = prefs[GAMES_PLAYED_KEY] ?: 0
            }
        }
    }

    override suspend fun setHighScore(score: Int) {
        dataStore.edit { preferences ->
            preferences[HIGH_SCORE_KEY] = score
        }
        _highScore.value = score
    }

    override suspend fun setZenBest(score: Int) {
        dataStore.edit { preferences ->
            preferences[ZEN_BEST_KEY] = score
        }
        _zenBest.value = score
    }

    override suspend fun setHighestLevel(level: Int) {
        val clamped = level.coerceAtLeast(1)
        dataStore.edit { preferences ->
            preferences[HIGHEST_LEVEL_KEY] = clamped
        }
        _highestLevel.value = clamped
    }

    override suspend fun incrementGamesPlayed() {
        val newCount = _gamesPlayed.value + 1
        dataStore.edit { preferences ->
            preferences[GAMES_PLAYED_KEY] = newCount
        }
        _gamesPlayed.value = newCount
    }
}