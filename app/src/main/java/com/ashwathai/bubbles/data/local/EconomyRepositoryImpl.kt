package com.ashwathai.bubbles.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ashwathai.bubbles.domain.model.BubbleSkins
import com.ashwathai.bubbles.domain.model.BubbleThemes
import com.ashwathai.bubbles.domain.model.EconomyConfig
import com.ashwathai.bubbles.domain.model.EconomyState
import com.ashwathai.bubbles.domain.repository.EconomyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EconomyRepositoryImpl(private val dataStore: DataStore<Preferences>) : EconomyRepository {

    private val COINS_KEY = intPreferencesKey("eco_coins")
    private val SLOWMO_KEY = intPreferencesKey("eco_slowmo")
    private val FREEZE_KEY = intPreferencesKey("eco_freeze")
    private val MULTIPOP_KEY = intPreferencesKey("eco_multipop")
    private val PRISM_KEY = intPreferencesKey("eco_prism")
    private val SKIN_KEY = stringPreferencesKey("eco_skin")
    private val THEME_KEY = stringPreferencesKey("eco_theme")
    private val POPS_KEY = longPreferencesKey("eco_lifetime_pops")
    private val MAX_COMBO_KEY = intPreferencesKey("eco_max_combo")
    private val GAMES_KEY = intPreferencesKey("eco_games_played")
    private val PRESTIGE_KEY = intPreferencesKey("eco_prestige")
    private val DAILY_DATE_KEY = stringPreferencesKey("eco_daily_date")
    private val DAILY_BEST_KEY = intPreferencesKey("eco_daily_best")
    private val SEEN_TYPES_KEY = stringSetPreferencesKey("eco_seen_bubble_types")
    private val ACHIEVEMENTS_KEY = stringSetPreferencesKey("eco_unlocked_achievements")

    /** Serialises every read-modify-write so overlapping coin/stat updates can't lose each other. */
    private val writeLock = Mutex()

    private val _state = MutableStateFlow(EconomyState())
    override val state: kotlinx.coroutines.flow.StateFlow<EconomyState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull() ?: return@launch
            _state.value = EconomyState(
                coins = prefs[COINS_KEY] ?: 0,
                slowMoLevel = (prefs[SLOWMO_KEY] ?: 1).coerceIn(1, EconomyConfig.UPGRADE_MAX),
                freezeLevel = (prefs[FREEZE_KEY] ?: 1).coerceIn(1, EconomyConfig.UPGRADE_MAX),
                multiPopLevel = (prefs[MULTIPOP_KEY] ?: 1).coerceIn(1, EconomyConfig.UPGRADE_MAX),
                prismLevel = (prefs[PRISM_KEY] ?: 1).coerceIn(1, EconomyConfig.UPGRADE_MAX),
                skin = prefs[SKIN_KEY] ?: BubbleSkins.CLASSIC.name,
                theme = prefs[THEME_KEY] ?: BubbleThemes.OCEAN.name,
                lifetimePops = prefs[POPS_KEY] ?: 0L,
                maxCombo = prefs[MAX_COMBO_KEY] ?: 0,
                gamesPlayed = prefs[GAMES_KEY] ?: 0,
                prestigeLevel = prefs[PRESTIGE_KEY] ?: 0,
                dailyDate = prefs[DAILY_DATE_KEY] ?: "",
                dailyBest = prefs[DAILY_BEST_KEY] ?: 0,
                seenBubbleTypes = prefs[SEEN_TYPES_KEY] ?: emptySet(),
                unlockedAchievements = prefs[ACHIEVEMENTS_KEY] ?: emptySet()
            )
        }
    }

    override suspend fun addCoins(amount: Int) = writeLock.withLock {
        if (amount <= 0) return@withLock
        val newCoins = _state.value.coins + amount
        dataStore.edit { it[COINS_KEY] = newCoins }
        _state.value = _state.value.copy(coins = newCoins)
    }

    override suspend fun spendCoins(amount: Int): Boolean = writeLock.withLock {
        if (amount <= 0) return@withLock true
        val current = _state.value
        if (current.coins < amount) return@withLock false
        val newCoins = current.coins - amount
        dataStore.edit { it[COINS_KEY] = newCoins }
        _state.value = current.copy(coins = newCoins)
        true
    }

    override suspend fun setUpgradeLevel(key: String, level: Int) = writeLock.withLock {
        val clamped = level.coerceIn(1, EconomyConfig.UPGRADE_MAX)
        val current = _state.value
        val updated = when (key) {
            "slowMo" -> current.copy(slowMoLevel = clamped)
            "freeze" -> current.copy(freezeLevel = clamped)
            "multiPop" -> current.copy(multiPopLevel = clamped)
            "prism" -> current.copy(prismLevel = clamped)
            else -> current
        }
        if (updated != current) {
            when (key) {
                "slowMo" -> dataStore.edit { it[SLOWMO_KEY] = clamped }
                "freeze" -> dataStore.edit { it[FREEZE_KEY] = clamped }
                "multiPop" -> dataStore.edit { it[MULTIPOP_KEY] = clamped }
                "prism" -> dataStore.edit { it[PRISM_KEY] = clamped }
            }
            _state.value = updated
        }
    }

    override suspend fun setSkin(name: String) = writeLock.withLock {
        dataStore.edit { it[SKIN_KEY] = name }
        _state.value = _state.value.copy(skin = name)
    }

    override suspend fun setTheme(name: String) = writeLock.withLock {
        dataStore.edit { it[THEME_KEY] = name }
        _state.value = _state.value.copy(theme = name)
    }

    override suspend fun recordPops(count: Int) = writeLock.withLock {
        if (count <= 0) return@withLock
        val newPops = _state.value.lifetimePops + count
        dataStore.edit { it[POPS_KEY] = newPops }
        _state.value = _state.value.copy(lifetimePops = newPops)
    }

    override suspend fun recordMaxCombo(combo: Int) = writeLock.withLock {
        if (combo <= _state.value.maxCombo) return@withLock
        dataStore.edit { it[MAX_COMBO_KEY] = combo }
        _state.value = _state.value.copy(maxCombo = combo)
    }

    override suspend fun recordGamePlayed() = writeLock.withLock {
        val newGames = _state.value.gamesPlayed + 1
        dataStore.edit { it[GAMES_KEY] = newGames }
        _state.value = _state.value.copy(gamesPlayed = newGames)
    }

    override suspend fun prestige() = writeLock.withLock {
        val level = _state.value.prestigeLevel + 1
        dataStore.edit {
            it[PRESTIGE_KEY] = level
            it[COINS_KEY] = 0
            it[SKIN_KEY] = BubbleSkins.GOLD.name
        }
        _state.value = _state.value.copy(prestigeLevel = level, coins = 0, skin = BubbleSkins.GOLD.name)
    }

    override suspend fun completeDailyChallenge(score: Int, date: String) = writeLock.withLock {
        val best = maxOf(_state.value.dailyBest, score)
        dataStore.edit {
            it[DAILY_DATE_KEY] = date
            it[DAILY_BEST_KEY] = best
        }
        _state.value = _state.value.copy(dailyDate = date, dailyBest = best)
    }

    override suspend fun markBubbleTypeSeen(type: String) = writeLock.withLock {
        if (type in _state.value.seenBubbleTypes) return@withLock
        val updated = _state.value.seenBubbleTypes + type
        dataStore.edit { it[SEEN_TYPES_KEY] = updated }
        _state.value = _state.value.copy(seenBubbleTypes = updated)
    }

    override suspend fun unlockAchievements(ids: Set<String>, rewardPerUnlock: Int): Set<String> =
        writeLock.withLock {
            val current = _state.value
            val newlyUnlocked = ids - current.unlockedAchievements
            if (newlyUnlocked.isEmpty()) return@withLock emptySet()
            val updated = current.unlockedAchievements + newlyUnlocked
            val coins = current.coins + newlyUnlocked.size * rewardPerUnlock.coerceAtLeast(0)
            dataStore.edit {
                it[ACHIEVEMENTS_KEY] = updated
                it[COINS_KEY] = coins
            }
            _state.value = current.copy(unlockedAchievements = updated, coins = coins)
            newlyUnlocked
        }
}