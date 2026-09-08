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
import com.ashwathai.bubbles.domain.model.BubbleType
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
import com.ashwathai.bubbles.domain.usecase.CanSpawnBubbleUseCase
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
    private val checkLevelCompleteUseCase: CheckLevelCompleteUseCase,
    private val canSpawnBubbleUseCase: CanSpawnBubbleUseCase = CanSpawnBubbleUseCase()
) : ViewModel() {

    private val config = GameConfig()
    private var currentLevelConfig: LevelConfig = config.levels[0]
    private var mode = GameMode.ADVENTURE
    private var timeSinceLastSpawn = 0f

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

    /** Tutorial card currently shown (first-ever burst of a special type). Null = none. */
    var tutorialCard by mutableStateOf<BubbleType?>(null)
        private set
    /** Types whose first-appearance label has already been shown this session. */
    private val labelShownThisSession = mutableSetOf<BubbleType>()

    var economy by mutableStateOf(EconomyState())
        private set
    var soundEnabled by mutableStateOf(true)
        private set
    var hapticsEnabled by mutableStateOf(true)
        private set
    var reducedMotion by mutableStateOf(false)
        private set

    /** True on the LevelComplete screen when a NEW level was just unlocked (gold chip). */
    var levelUnlockedThisRun by mutableStateOf(false)
        private set

    /** Fraction of the level timer remaining when the last level was cleared (drives star rating). */
    var lastLevelTimeFraction by mutableStateOf(1f)
        private set

    /** Highest adventure level cleared (persisted). Drives Continue + level select. */
    var highestLevel by mutableStateOf(1)
        private set

    private var slowMoEndTime: Long = 0
    private var freezeEndTime: Long = 0
    private var multiPopEndTime: Long = 0
    private var multiPopActive = false
    private var comboExpiresAt = 0L
    private var bubblesPoppedThisLevel = 0
    private var bubblesSpawnedThisLevel = 0
    private var gameLoopJob: kotlinx.coroutines.Job? = null
    private val tapChannel = Channel<TapEvent>(Channel.UNLIMITED)

    // ── Ad Reward State ──
    private var lastGameOverState: GameState.GameOver? = null
    var previewSkinName: String? = null
        private set
    private var originalSkinBeforePreview: String = ""
    var guaranteeNextPowerUp = false
        private set

    private var width: Float = 0f
    private var height: Float = 0f

    init {
        observeHighScore()
        observeHighestLevel()
        observeEconomy()
        observeSettings()
    }

    private fun observeHighestLevel() {
        viewModelScope.launch {
            scoreRepository.highestLevel.collect { highestLevel = it }
        }
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

    /**
     * Start an Adventure session. [level] defaults to 1; passing any previously
     * cleared level (from the level-select UI) replays it from a clean slate.
     */
    fun startGame(level: Int = 1) {
        mode = GameMode.ADVENTURE
        val requestedLevel = level.coerceIn(1, config.levels.size)
        beginSession(
            levelConfig = config.levels[requestedLevel - 1],
            level = requestedLevel,
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
        bubblesSpawnedThisLevel = 0
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
        timeSinceLastSpawn = 0f

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
        // Tutorial card pauses the whole game (timer + bubbles) until dismissed
        if (tutorialCard != null) return

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
        val forcePowerUp = guaranteeNextPowerUp
        if (guaranteeNextPowerUp) guaranteeNextPowerUp = false

        timeSinceLastSpawn += dt
        val isEndlessMode = mode != GameMode.ADVENTURE
        if (
            timeSinceLastSpawn >= currentLevelConfig.spawnInterval &&
            canSpawnBubbleUseCase(
                bubblesOnBoard = bubbles.size,
                maxBubbles = currentLevelConfig.maxBubbles,
                bubblesSpawnedSoFar = bubblesSpawnedThisLevel,
                isEndlessMode = isEndlessMode
            )
        ) {
            timeSinceLastSpawn = 0f
            val newBubbles = spawnBubblesUseCase(
                config, currentLevelConfig, width, height, bubbles.toList(),
                palette = BubbleSkins.fromName(economy.skin).palette,
                specialChance = specialChance,
                prismBoost = economy.prismBoost,
                forcePowerUp = forcePowerUp,
                maxBubbles = currentLevelConfig.maxBubbles
            )
            bubbles.addAll(newBubbles)
            bubblesSpawnedThisLevel += newBubbles.size
            maybeShowFirstAppearanceLabel(newBubbles)
        }

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

        if (
            mode == GameMode.ADVENTURE &&
            checkLevelCompleteUseCase(
                bubbles.toList(),
                totalBubblesToSpawn = currentLevelConfig.maxBubbles,
                bubblesSpawnedSoFar = bubblesSpawnedThisLevel
            )
        ) {
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
        maybeShowTutorialCard(result.newBubbles, result.newMessages)
    }

    /**
     * First time a special type ever spawns for this player, attach a small
     * "NEW — TYPE" label message near the bubble (non-blocking).
     */
    private fun maybeShowFirstAppearanceLabel(newBubbles: List<Bubble>) {
        if (mode == GameMode.ZEN) return
        for (bubble in newBubbles) {
            val type = bubble.bubbleType
            if (type == BubbleType.NORMAL || type == BubbleType.BOSS) continue
            if (type.name in economy.seenBubbleTypes || type in labelShownThisSession) continue
            labelShownThisSession.add(type)
            messages.add(
                PopMessage(
                    id = System.nanoTime() + 21,
                    text = "NEW — ${type.name.replace('_', ' ')}",
                    x = bubble.x.coerceIn(60f, (width - 60f).coerceAtLeast(60f)),
                    y = (bubble.y - bubble.radius - 20f).coerceAtLeast(80f),
                    rotation = 0f,
                    scale = 0.9f,
                    color = LuxuryColors.Gold300,
                    fontSize = 18
                )
            )
        }
    }

    /**
     * First time a special type is ever burst, show a pausing tutorial card.
     * Triggered from the pop messages produced by HandleTapUseCase.
     */
    private fun maybeShowTutorialCard(remainingBubbles: List<Bubble>, newMessages: List<PopMessage>) {
        if (tutorialCard != null) return
        val burstTypes = newMessages.mapNotNull { msg ->
            when {
                msg.text.startsWith("TICK!") -> BubbleType.TICKING_BOMB
                msg.text.startsWith("MAGNET") -> BubbleType.MAGNET
                msg.text.startsWith("CHAOS") -> BubbleType.CHAOS
                msg.text.startsWith("PHANTOM") -> BubbleType.GHOST
                else -> null
            }
        }
        for (type in burstTypes) {
            if (type.name in economy.seenBubbleTypes) continue
            tutorialCard = type
            hitStopRemaining = 0f // paused via tutorialCard check, not hit-stop
            viewModelScope.launch { economyRepository.markBubbleTypeSeen(type.name) }
            break
        }
    }

    /** Dismiss the tutorial card (tap or auto-timeout). */
    fun dismissTutorialCard() {
        tutorialCard = null
    }

    /** One-line mechanic description for a special bubble type. */
    fun tutorialTextFor(type: BubbleType): String = when (type) {
        BubbleType.BOMB -> "Blasts every bubble nearby — chain it for big points!"
        BubbleType.FROZEN -> "Takes two taps — first crack, then pop."
        BubbleType.RAINBOW -> "Chains with ANY color it touches."
        BubbleType.MAGNET -> "Pulls nearby bubbles toward it. Doesn't chain."
        BubbleType.TICKING_BOMB -> "Explodes on its own in 1.5s — pop it first!"
        BubbleType.CHAOS -> "Flings every bubble around it in random directions."
        BubbleType.GHOST -> "Phases through chains — and splits into 3 when popped."
        else -> "The classic bubble. Pop it!"
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

        // ── Unlock tracking ──
        levelUnlockedThisRun = false
        lastLevelTimeFraction = if (levelTimeLimit > 0f) {
            (levelTimeRemaining / levelTimeLimit).coerceIn(0f, 1f)
        } else 1f

        // ── Milestone reward coins ──
        val milestoneCoins = EconomyConfig.milestoneRewardForLevel(currentLevel)
        viewModelScope.launch {
            economyRepository.addCoins(milestoneCoins)
        }

        // ── Level-clear celebration particles ──
        particles.clear()
        val burstParticles = (1..24).map { i ->
            val angle = (i.toFloat() / 24f) * 2 * 3.14159f
            val speed = 180f + Random.nextFloat() * 220f
            Particle(
                id = System.nanoTime() + i.toLong(),
                x = width / 2f,
                y = height / 2f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = 4f + Random.nextFloat() * 4f,
                color = LuxuryColors.Gold400,
                alpha = 1f,
                life = 1f,
                isRing = false
            )
        }
        particles.addAll(burstParticles)

        val nextLevel = currentLevel + 1
        if (nextLevel <= config.levels.size) {
            currentLevelConfig = config.levels[nextLevel - 1]
            bubblesPoppedThisLevel = 0

            if (nextLevel > scoreRepository.highestLevel.value) {
                levelUnlockedThisRun = true
                viewModelScope.launch { scoreRepository.setHighestLevel(nextLevel) }
            }

            gameState = GameState.LevelComplete(score, highScore, currentLevel, nextLevel)
            gameLoopJob?.cancel()
        } else {
            gameOver(won = true)
        }
    }

    fun startNextLevel() {
        val completedLevel = (gameState as? GameState.LevelComplete)?.completedLevel ?: 0
        val shouldShowAd = EconomyConfig.shouldShowAdBetweenLevels(completedLevel)

        if (shouldShowAd) {
            // Handled by the caller (MainActivity) — this flag is read in onNext
            // When ad is shown and rewarded, startNextLevelConfirmed() is called.
            gameState = GameState.LevelComplete(
                score = (gameState as? GameState.LevelComplete)?.score ?: 0,
                highScore = (gameState as? GameState.LevelComplete)?.highScore ?: 0,
                completedLevel = completedLevel,
                nextLevel = (gameState as? GameState.LevelComplete)?.nextLevel ?: 1,
                waitingForAd = true
            )
            return
        }

        startNextLevelConfirmed()
    }

    /** Called after the interstitial ad is watched (or no ad is needed). */
    fun startNextLevelConfirmed() {
        val completedLevel = (gameState as? GameState.LevelComplete)?.completedLevel ?: 0
        val nextLevel = (gameState as? GameState.LevelComplete)?.nextLevel ?: 1
        val score = (gameState as? GameState.LevelComplete)?.score ?: 0
        val highScore = (gameState as? GameState.LevelComplete)?.highScore ?: 0

        currentLevelConfig = config.levels[nextLevel - 1]
        bubblesPoppedThisLevel = 0
        bubblesSpawnedThisLevel = 0
        combo = 0
        comboExpiresAt = 0
        activePowerUps.clear()
        slowMoEndTime = 0
        freezeEndTime = 0
        multiPopActive = false
        multiPopEndTime = 0
        bubbles.clear()
        messages.clear()
        timeSinceLastSpawn = 0f
        levelUnlockedThisRun = false

        levelTimeLimit = currentLevelConfig.timeLimit
        levelTimeRemaining = currentLevelConfig.timeLimit

        gameState = GameState.Playing(score, highScore, nextLevel, bubblesPoppedThisLevel)
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

        val gameOverState = GameState.GameOver(
            score, highScore, isNewHighScore, isTimeUp, won, zen, daily, dailyReward,
            currentLevel = (gameState as? GameState.Playing)?.currentLevel ?: 1
        )
        lastGameOverState = gameOverState
        gameState = gameOverState
        gameLoopJob?.cancel()
    }

    fun restartGame() {
        revertPreviewSkin()
        guaranteeNextPowerUp = false
        if (mode == GameMode.ZEN) startZen() else startGame()
    }

    fun goHome() {
        revertPreviewSkin()
        guaranteeNextPowerUp = false
        lastGameOverState = null
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

    // ── Ad Reward Methods ──

    /**
     * Continue after Game Over — resume from the last score/level with a full timer.
     * Called when the user watches the "Continue" rewarded ad.
     */
    fun continueGame() {
        val last = lastGameOverState ?: return
        if (last.won || last.zen || last.daily) return // can't continue won/zen/daily games

        val savedScore = last.finalScore
        val savedHighScore = last.highScore
        val savedLevel = last.currentLevel.coerceAtLeast(1)

        // Restore the level config for the level we were on
        val lvlIndex = (savedLevel - 1).coerceIn(0, config.levels.size - 1)
        currentLevelConfig = config.levels[lvlIndex]
        levelTimeLimit = currentLevelConfig.timeLimit
        levelTimeRemaining = currentLevelConfig.timeLimit // full timer
        bubblesPoppedThisLevel = 0
        bubblesSpawnedThisLevel = 0
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
        lastGameOverState = null
        levelUnlockedThisRun = false

        gameState = GameState.Playing(
            score = savedScore,
            highScore = savedHighScore,
            currentLevel = savedLevel,
            bubblesPoppedThisLevel = 0
        )
        startGameLoop()
    }

    /** Add bonus coins from a rewarded ad (coin_bonus, double_daily, etc.). */
    fun addBonusCoins(amount: Int) {
        viewModelScope.launch {
            economyRepository.addCoins(amount)
        }
    }

    /** Grant a time bonus (e.g. +15s after Continue ad). */
    fun addTimeBonus(seconds: Float) {
        if (seconds > 0f) {
            levelTimeRemaining = min(levelTimeLimit, levelTimeRemaining + seconds)
        }
    }

    /** Continue after Game Over by spending coins (no ad needed). */
    fun continueWithCoins() {
        if (economy.coins < EconomyConfig.CONTINUE_COST) return
        viewModelScope.launch {
            economyRepository.spendCoins(EconomyConfig.CONTINUE_COST)
        }
        continueGameWithTimeBonus(EconomyConfig.CONTINUE_TIME_BONUS)
    }

    private fun continueGameWithTimeBonus(timeBonus: Float) {
        val last = lastGameOverState ?: return
        if (last.won || last.zen || last.daily) return

        val savedScore = last.finalScore
        val savedHighScore = last.highScore
        val savedLevel = last.currentLevel.coerceAtLeast(1)

        val lvlIndex = (savedLevel - 1).coerceIn(0, config.levels.size - 1)
        currentLevelConfig = config.levels[lvlIndex]
        levelTimeLimit = currentLevelConfig.timeLimit
        levelTimeRemaining = currentLevelConfig.timeLimit + timeBonus
        bubblesPoppedThisLevel = 0
        bubblesSpawnedThisLevel = 0
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
        lastGameOverState = null
        levelUnlockedThisRun = false

        gameState = GameState.Playing(
            score = savedScore,
            highScore = savedHighScore,
            currentLevel = savedLevel,
            bubblesPoppedThisLevel = 0
        )
        startGameLoop()
    }

    /** Set the flag to guarantee the next spawned bubble is a power-up. */
    fun setGuaranteeNextPowerUp() {
        guaranteeNextPowerUp = true
    }

    /** Preview a locked skin temporarily — reverts after the game ends. */
    fun previewSkin(name: String) {
        if (previewSkinName == null) {
            originalSkinBeforePreview = economy.skin
        }
        previewSkinName = name
        viewModelScope.launch {
            economyRepository.setSkin(name)
        }
    }

    /** Revert to the original skin after a preview game ends. */
    private fun revertPreviewSkin() {
        if (previewSkinName != null) {
            val original = originalSkinBeforePreview
            previewSkinName = null
            viewModelScope.launch {
                economyRepository.setSkin(original)
            }
        }
    }

    /** Check if the daily challenge was already completed today. */
    fun isDailyCompleted(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return today == economy.dailyDate
    }

    override fun onCleared() {
        gameLoopJob?.cancel()
        super.onCleared()
    }
}