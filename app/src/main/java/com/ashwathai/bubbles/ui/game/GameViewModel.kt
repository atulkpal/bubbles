package com.ashwathai.bubbles.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashwathai.bubbles.data.sound.SoundManager
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.BubbleSkins
import com.ashwathai.bubbles.domain.model.BubbleThemes
import com.ashwathai.bubbles.domain.model.EconomyConfig
import com.ashwathai.bubbles.domain.model.EconomyState
import com.ashwathai.bubbles.domain.model.GameConfig
import com.ashwathai.bubbles.domain.model.GameState
import com.ashwathai.bubbles.domain.model.LevelConfig
import com.ashwathai.bubbles.domain.model.Particle
import com.ashwathai.bubbles.domain.model.PopMessage
import com.ashwathai.bubbles.domain.model.PowerUpType
import com.ashwathai.bubbles.domain.repository.EconomyRepository
import com.ashwathai.bubbles.domain.repository.ScoreRepository
import com.ashwathai.bubbles.domain.repository.SettingsRepository
import com.ashwathai.bubbles.domain.usecase.CheckLevelCompleteUseCase
import com.ashwathai.bubbles.domain.usecase.HandleTapUseCase
import com.ashwathai.bubbles.domain.usecase.SpawnBubblesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateBubblesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateMessagesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateParticlesUseCase
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryColors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private enum class GameMode { ADVENTURE, ZEN, DAILY }

private data class TapEvent(
    val x: Float,
    val y: Float,
    val timeMs: Long
)

class GameViewModel(
    private val scoreRepository: ScoreRepository,
    private val settingsRepository: SettingsRepository,
    private val economyRepository: EconomyRepository,
    private val soundManager: SoundManager,
    private val spawnBubblesUseCase: SpawnBubblesUseCase,
    private val updateBubblesUseCase: UpdateBubblesUseCase,
    private val handleTapUseCase: HandleTapUseCase,
    private val updateMessagesUseCase: UpdateMessagesUseCase,
    private val updateParticlesUseCase: UpdateParticlesUseCase,
    private val checkLevelCompleteUseCase: CheckLevelCompleteUseCase
) : ViewModel() {

    private val config = GameConfig()
    private var currentLevelConfig: LevelConfig = config.levels[0]
    private var mode = GameMode.ADVENTURE

    private val zenLevelConfig = LevelConfig(
        level = 0,
        maxBubbles = EconomyConfig.ZEN_MAX_BUBBLES,
        spawnInterval = 2.5f,
        baseSpeed = 220f,
        powerUpChance = 0.07f,
        timeLimit = 99999f
    )
    private val dailyLevelConfig = LevelConfig(
        level = 0,
        maxBubbles = 14,
        spawnInterval = 1.0f,
        baseSpeed = 420f,
        powerUpChance = 0.08f,
        timeLimit = EconomyConfig.DAILY_TIME_LIMIT
    )

    var gameState by mutableStateOf<GameState>(GameState.Ready)
        private set

    var levelTimeRemaining by mutableStateOf(0f)
        private set
    var levelTimeLimit by mutableStateOf(60f)
        private set

    val bubbles = mutableStateListOf<Bubble>()
    val messages = mutableStateListOf<PopMessage>()
    val particles = mutableStateListOf<Particle>()
    val activePowerUps = mutableStateListOf<PowerUpType>()

    var combo by mutableStateOf(0)
        private set
    var shakeTrauma by mutableStateOf(0f)
        private set
    var hitStopRemaining by mutableStateOf(0f)
        private set

    var economy by mutableStateOf(EconomyState())
        private set
    var soundEnabled by mutableStateOf(true)
        private set
    var hapticsEnabled by mutableStateOf(true)
        private set
    var reducedMotion by mutableStateOf(false)
        private set

    private var slowMoEndTime: Long = 0
    private var freezeEndTime: Long = 0
    private var multiPopEndTime: Long = 0
    private var multiPopActive = false
    private var comboExpiresAt = 0L
    private var bubblesPoppedThisLevel = 0
    private var gameLoopJob: kotlinx.coroutines.Job? = null
    private val tapChannel = Channel<TapEvent>(Channel.UNLIMITED)

    private var width: Float = 0f
    private var height: Float = 0f

    init {
        observeHighScore()
        observeEconomy()
        observeSettings()
    }

    private fun observeHighScore() {
        viewModelScope.launch {
            scoreRepository.highScore.collect { highScore ->
                when (val state = gameState) {
                    is GameState.Ready -> {}
                    is GameState.Playing -> gameState = state.copy(highScore = highScore)
                    is GameState.Paused -> gameState = state.copy(highScore = highScore)
                    is GameState.LevelComplete -> gameState = state.copy(highScore = highScore)
                    is GameState.GameOver -> {}
                }
            }
        }
    }

    private fun observeEconomy() {
        viewModelScope.launch {
            economyRepository.state.collect { state ->
                economy = state
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.soundEnabled.collect {
                soundEnabled = it
                soundManager.setSoundEnabled(it)
            }
        }
        viewModelScope.launch {
            settingsRepository.hapticsEnabled.collect {
                hapticsEnabled = it
                soundManager.setHapticsEnabled(it)
            }
        }
        viewModelScope.launch {
            settingsRepository.reducedMotion.collect {
                reducedMotion = it
            }
        }
    }

    fun onSurfaceSizeChanged(w: Float, h: Float) {
        width = w
        height = h
    }

    fun onTap(offset: androidx.compose.ui.geometry.Offset) {
        tapChannel.trySend(TapEvent(offset.x, offset.y, System.currentTimeMillis()))
    }

    fun startGame() {
        mode = GameMode.ADVENTURE
        beginSession(
            levelConfig = config.levels[0],
            level = 1,
            highScore = scoreRepository.highScore.value
        )
    }

    fun startZen() {
        mode = GameMode.ZEN
        beginSession(
            levelConfig = zenLevelConfig,
            level = 0,
            highScore = scoreRepository.zenBest.value,
            isZen = true
        )
    }

    fun startDaily() {
        mode = GameMode.DAILY
        beginSession(
            levelConfig = dailyLevelConfig,
            level = 0,
            highScore = economy.dailyBest,
            isDaily = true
        )
    }

    private fun beginSession(
        levelConfig: LevelConfig,
        level: Int,
        highScore: Int,
        isZen: Boolean = false,
        isDaily: Boolean = false
    ) {
        currentLevelConfig = levelConfig
        bubblesPoppedThisLevel = 0
        combo = 0
        comboExpiresAt = 0
        activePowerUps.clear()
        slowMoEndTime = 0
        freezeEndTime = 0
        multiPopActive = false
        multiPopEndTime = 0
        bubbles.clear()
        messages.clear()
        particles.clear()
        shakeTrauma = 0f
        hitStopRemaining = 0f

        levelTimeLimit = levelConfig.timeLimit
        levelTimeRemaining = levelConfig.timeLimit

        gameState = GameState.Playing(
            score = 0,
            highScore = highScore,
            currentLevel = level,
            bubblesPoppedThisLevel = 0,
            isZen = isZen,
            isDaily = isDaily
        )
        startGameLoop()
    }

    fun pauseGame() {
        when (val state = gameState) {
            is GameState.Playing -> {
                gameState = GameState.Paused(state.score, state.highScore, state.currentLevel, state.isZen, state.isDaily)
                gameLoopJob?.cancel()
            }
            else -> {}
        }
    }

    fun resumeGame() {
        when (val state = gameState) {
            is GameState.Paused -> {
                gameState = GameState.Playing(state.score, state.highScore, state.currentLevel, bubblesPoppedThisLevel, state.isZen, state.isDaily)
                startGameLoop()
            }
            else -> {}
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var last = System.nanoTime()
            while (isActive) {
                if (gameState !is GameState.Playing) break
                val now = System.nanoTime()
                val dt = min((now - last) / 1_000_000_000f, 0.05f)
                last = now
                updateGame(dt)
                kotlinx.coroutines.delay(16)
            }
        }
    }

    private fun updateGame(dt: Float) {
        shakeTrauma = max(0f, shakeTrauma - 2.2f * dt)

        if (hitStopRemaining > 0f) {
            hitStopRemaining -= dt
            return
        }

        val isFrozen = freezeEndTime > System.currentTimeMillis()
        val slowMoFactor = if (slowMoEndTime > System.currentTimeMillis()) 0.25f else 1f

        if (mode != GameMode.ZEN) {
            levelTimeRemaining -= dt
            if (levelTimeRemaining <= 0f) {
                levelTimeRemaining = 0f
                if (mode == GameMode.DAILY) {
                    gameOver(isTimeUp = true, daily = true)
                } else {
                    gameOver(isTimeUp = true)
                }
                return
            }
        }

        if (combo > 0 && System.currentTimeMillis() > comboExpiresAt) {
            combo = 0
        }

        val specialChance = 0.12f * economy.prismBoost
        val newBubbles = spawnBubblesUseCase(
            config, currentLevelConfig, width, height, bubbles.toList(),
            palette = BubbleSkins.fromName(economy.skin).palette,
            specialChance = specialChance,
            prismBoost = economy.prismBoost
        )
        bubbles.addAll(newBubbles)

        val updatedBubbles = updateBubblesUseCase(bubbles.toList(), dt, width, height, currentLevelConfig, isFrozen, slowMoFactor)
        bubbles.clear()
        bubbles.addAll(updatedBubbles)

        val updatedMessages = updateMessagesUseCase(messages.toList(), dt)
        messages.clear()
        messages.addAll(updatedMessages)

        val updatedParticles = updateParticlesUseCase(particles.toList(), dt)
        particles.clear()
        particles.addAll(updatedParticles)

        var tapsThisFrame = 0
        while (tapsThisFrame < 3) {
            val tap = tapChannel.tryReceive().getOrNull() ?: break
            handleTap(tap.x, tap.y, tap.timeMs)
            tapsThisFrame++
        }

        checkPowerUpExpiration()

        if (mode == GameMode.ADVENTURE && checkLevelCompleteUseCase(bubbles.toList())) {
            completeLevel()
        }
    }

    private fun handleTap(tapX: Float, tapY: Float, tapTimeMs: Long) {
        val result = handleTapUseCase(
            bubbles.toList(), tapX, tapY, config, currentLevelConfig,
            palette = BubbleSkins.fromName(economy.skin).palette,
            multiPopRadius = if (multiPopActive) economy.multiPopRadius else 0f
        )

        bubbles.clear()
        bubbles.addAll(result.newBubbles)
        messages.addAll(result.newMessages)
        particles.addAll(result.newParticles)

        if (result.bubblesPopped > 0) {
            applyCombo(result, tapTimeMs)
        }

        if (result.powerUpCollected != null) {
            soundManager.playPowerUp()
            activatePowerUp(result.powerUpCollected!!)
        }

        bubblesPoppedThisLevel += result.bubblesPopped
    }

    private fun applyCombo(result: com.ashwathai.bubbles.domain.usecase.TapResult, tapTimeMs: Long) {
        combo = if (tapTimeMs < comboExpiresAt) combo + 1 else 1
        comboExpiresAt = tapTimeMs + EconomyConfig.COMBO_WINDOW_MS

        val comboMultiplier = 1f + (combo - 1) * 0.25f
        val scoreGain = (result.scoreGain * comboMultiplier).toInt()

        if (scoreGain > 0) {
            updateScore(scoreGain)
        }

        if (mode != GameMode.ZEN) {
            levelTimeRemaining = min(levelTimeLimit, levelTimeRemaining + 0.5f)
        }

        val coinsGain = result.bubblesPopped * EconomyConfig.COINS_PER_POP * economy.coinMultiplier + (combo - 1) * EconomyConfig.CHAIN_BONUS
        if (coinsGain > 0) {
            viewModelScope.launch {
                economyRepository.addCoins(coinsGain)
            }
        }

        if (result.isBigPop) {
            soundManager.playBigPop()
            soundManager.vibrateBigPop()
            shakeTrauma = min(1f, shakeTrauma + 0.6f)
            hitStopRemaining = 0.08f
        } else {
            soundManager.playPop()
            soundManager.vibratePop()
            shakeTrauma = min(1f, shakeTrauma + 0.12f)
        }

        if (combo >= 3) {
            soundManager.playCombo()
            soundManager.vibrateCombo()
            messages.add(
                PopMessage(
                    id = System.nanoTime() + 7,
                    text = "COMBO x$combo!",
                    x = if (bubbles.isNotEmpty()) bubbles.first().x else width / 2f,
                    y = if (bubbles.isNotEmpty()) bubbles.first().y else height / 3f,
                    rotation = 0f,
                    scale = 1.2f,
                    color = LuxuryColors.Gold400,
                    fontSize = 34
                )
            )
            if (combo % 5 == 0) soundManager.playCoin()
        }

        viewModelScope.launch {
            economyRepository.recordPops(result.bubblesPopped)
            if (combo > 1) economyRepository.recordMaxCombo(combo)
        }
    }

    private fun updateScore(gain: Int) {
        when (val state = gameState) {
            is GameState.Playing -> gameState = state.copy(score = state.score + gain)
            else -> {}
        }
    }

    private fun activatePowerUp(type: PowerUpType) {
        val now = System.currentTimeMillis()
        when (type) {
            PowerUpType.SLOW_MO -> {
                slowMoEndTime = now + economy.slowMoDurationMs
                if (type !in activePowerUps) activePowerUps.add(type)
            }
            PowerUpType.FREEZE -> {
                freezeEndTime = now + economy.freezeDurationMs
                if (type !in activePowerUps) activePowerUps.add(type)
            }
            PowerUpType.MULTI_POP -> {
                multiPopActive = true
                multiPopEndTime = now + 6000
                if (type !in activePowerUps) activePowerUps.add(type)
            }
            PowerUpType.PRISM -> {
                if (type !in activePowerUps) activePowerUps.add(type)
            }
        }
    }

    private fun checkPowerUpExpiration() {
        val now = System.currentTimeMillis()
        val expired = activePowerUps.filter {
            (it == PowerUpType.SLOW_MO && slowMoEndTime <= now) ||
            (it == PowerUpType.FREEZE && freezeEndTime <= now) ||
            (it == PowerUpType.MULTI_POP && multiPopEndTime <= now)
        }.toList()
        expired.forEach { activePowerUps.remove(it) }
        if (PowerUpType.MULTI_POP !in activePowerUps) {
            multiPopActive = false
        }
    }

    private fun completeLevel() {
        soundManager.playLevelUp()
        soundManager.vibrateLevelUp()

        val currentLevel = (gameState as? GameState.Playing)?.currentLevel ?: 1
        val score = (gameState as? GameState.Playing)?.score ?: 0
        val highScore = (gameState as? GameState.Playing)?.highScore ?: 0

        val nextLevel = currentLevel + 1
        if (nextLevel <= config.levels.size) {
            currentLevelConfig = config.levels[nextLevel - 1]
            bubblesPoppedThisLevel = 0

            gameState = GameState.LevelComplete(score, highScore, currentLevel, nextLevel)
            gameLoopJob?.cancel()
        } else {
            gameOver(won = true)
        }
    }

    fun startNextLevel() {
        gameState = GameState.Playing(
            score = (gameState as? GameState.LevelComplete)?.score ?: 0,
            highScore = (gameState as? GameState.LevelComplete)?.highScore ?: 0,
            currentLevel = (gameState as? GameState.LevelComplete)?.nextLevel ?: 1,
            bubblesPoppedThisLevel = 0
        )
        levelTimeLimit = currentLevelConfig.timeLimit
        levelTimeRemaining = currentLevelConfig.timeLimit
        startGameLoop()
    }

    fun endZenSession() {
        gameOver(zen = true)
    }

    private fun gameOver(isTimeUp: Boolean = false, won: Boolean = false, zen: Boolean = false, daily: Boolean = false) {
        val score = (gameState as? GameState.Playing)?.score ?: 0
        val isNewHighScore: Boolean
        val highScore: Int
        var dailyReward = 0

        when {
            zen -> {
                isNewHighScore = score > scoreRepository.zenBest.value
                highScore = max(score, scoreRepository.zenBest.value)
                if (isNewHighScore) {
                    viewModelScope.launch { scoreRepository.setZenBest(score) }
                }
            }
            daily -> {
                isNewHighScore = score > economy.dailyBest
                highScore = max(score, economy.dailyBest)
                dailyReward = EconomyConfig.DAILY_REWARD + score
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                viewModelScope.launch {
                    economyRepository.completeDailyChallenge(score, date)
                    economyRepository.addCoins(dailyReward)
                }
            }
            else -> {
                isNewHighScore = score > scoreRepository.highScore.value
                highScore = max(score, scoreRepository.highScore.value)
                if (isNewHighScore) {
                    viewModelScope.launch { scoreRepository.setHighScore(score) }
                }
            }
        }

        viewModelScope.launch {
            scoreRepository.incrementGamesPlayed()
            economyRepository.recordGamePlayed()
        }

        if (zen) {
            soundManager.playGameOver()
        } else if (!isTimeUp && won) {
            soundManager.playLevelUp()
            soundManager.vibrateLevelUp()
        } else {
            soundManager.playGameOver()
        }

        gameState = GameState.GameOver(score, highScore, isNewHighScore, isTimeUp, won, zen, daily, dailyReward)
        gameLoopJob?.cancel()
    }

    fun restartGame() {
        if (mode == GameMode.ZEN) startZen() else startGame()
    }

    fun goHome() {
        gameLoopJob?.cancel()
        gameState = GameState.Ready
    }

    fun upgradePowerUp(key: String) {
        val currentLevel = when (key) {
            "slowMo" -> economy.slowMoLevel
            "freeze" -> economy.freezeLevel
            "multiPop" -> economy.multiPopLevel
            else -> economy.prismLevel
        }
        if (currentLevel >= EconomyConfig.UPGRADE_MAX) return
        viewModelScope.launch {
            val ok = economyRepository.spendCoins(EconomyConfig.UPGRADE_COST)
            if (ok) {
                economyRepository.setUpgradeLevel(key, currentLevel + 1)
                soundManager.playCoin()
            }
        }
    }

    fun selectSkin(name: String) {
        viewModelScope.launch {
            economyRepository.setSkin(name)
        }
    }

    fun selectTheme(name: String) {
        viewModelScope.launch {
            economyRepository.setTheme(name)
        }
    }

    fun prestige() {
        viewModelScope.launch {
            economyRepository.prestige()
            soundManager.playLevelUp()
        }
    }

    fun toggleSound() {
        viewModelScope.launch { settingsRepository.setSoundEnabled(!soundEnabled) }
    }

    fun toggleHaptics() {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(!hapticsEnabled) }
    }

    fun toggleReducedMotion() {
        viewModelScope.launch { settingsRepository.setReducedMotion(!reducedMotion) }
    }

    override fun onCleared() {
        gameLoopJob?.cancel()
        super.onCleared()
    }
}