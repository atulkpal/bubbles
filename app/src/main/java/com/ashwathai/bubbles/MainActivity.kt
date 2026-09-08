package com.ashwathai.bubbles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.ashwathai.bubbles.data.local.EconomyRepositoryImpl
import com.ashwathai.bubbles.data.local.ScoreRepositoryImpl
import com.ashwathai.bubbles.data.local.SettingsRepositoryImpl
import com.ashwathai.bubbles.data.sound.SoundManager
import com.ashwathai.bubbles.di.AppModule
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.BubbleSkin
import com.ashwathai.bubbles.domain.model.BubbleSkins
import com.ashwathai.bubbles.domain.model.BubbleThemes
import com.ashwathai.bubbles.domain.model.BubbleType
import com.ashwathai.bubbles.domain.model.EconomyConfig
import com.ashwathai.bubbles.domain.model.EconomyState
import com.ashwathai.bubbles.domain.model.GameState
import com.ashwathai.bubbles.domain.model.Particle
import com.ashwathai.bubbles.domain.model.PopMessage
import com.ashwathai.bubbles.domain.model.PowerUpType
import com.ashwathai.bubbles.domain.model.ThemePalette
import com.ashwathai.bubbles.domain.model.starsForTimeFraction
import com.ashwathai.bubbles.domain.repository.EconomyRepository
import com.ashwathai.bubbles.domain.repository.ScoreRepository
import com.ashwathai.bubbles.domain.repository.SettingsRepository
import com.ashwathai.bubbles.ui.game.GameViewModel
import com.ashwathai.bubbles.ui.theme.BubblesTheme
import com.ashwathai.bubbles.ui.theme.luxury.GlassSurface
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryButton
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryColors
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryIcons
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryMotion
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryRadius
import com.ashwathai.bubbles.ui.theme.luxury.LuxurySectionTitle
import com.ashwathai.bubbles.ui.theme.luxury.LuxurySpacing
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryStat
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryTimerRing
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryToggle
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryTypography

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val dataStore by lazy {
        AppModule.provideDataStore(this)
    }
    private val scoreRepository: ScoreRepository by lazy {
        AppModule.provideScoreRepository(dataStore)
    }
    private val settingsRepository: SettingsRepository by lazy {
        AppModule.provideSettingsRepository(dataStore)
    }
    private val economyRepository: EconomyRepository by lazy {
        AppModule.provideEconomyRepository(dataStore)
    }
    private val soundManager: SoundManager by lazy {
        AppModule.provideSoundManager(this)
    }
    private val gameViewModel: GameViewModel by lazy {
        GameViewModel(
            scoreRepository = scoreRepository,
            settingsRepository = settingsRepository,
            economyRepository = economyRepository,
            soundManager = soundManager,
            spawnBubblesUseCase = AppModule.provideSpawnBubblesUseCase(),
            updateBubblesUseCase = AppModule.provideUpdateBubblesUseCase(),
            handleTapUseCase = AppModule.provideHandleTapUseCase(),
            updateMessagesUseCase = AppModule.provideUpdateMessagesUseCase(),
            updateParticlesUseCase = AppModule.provideUpdateParticlesUseCase(),
            checkLevelCompleteUseCase = AppModule.provideCheckLevelCompleteUseCase(),
            canSpawnBubbleUseCase = AppModule.provideCanSpawnBubbleUseCase()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LevelPlayAdManager.trackActivity(this)
        enableEdgeToEdge()
        setContent {
            BubblesTheme {
                BubbleScreen(gameViewModel)
            }
        }
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}

fun Modifier.themeBackground(theme: ThemePalette): Modifier {
    return background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
}

/** App version shown in Settings → About. Keep in sync with app/build.gradle.kts. */
const val APP_VERSION = "1.2"

fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
fun BubbleScreen(gameViewModel: GameViewModel) {
    val gameState = gameViewModel.gameState
    val bubbles = gameViewModel.bubbles
    val messages = gameViewModel.messages
    val particles = gameViewModel.particles
    val activePowerUps = gameViewModel.activePowerUps
    val economy = gameViewModel.economy
    val skin = BubbleSkins.fromName(economy.skin)
    val theme = BubbleThemes.fromName(economy.theme)
    val combo = gameViewModel.combo
    val shakeTrauma = gameViewModel.shakeTrauma

    var showSettings by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    gameViewModel.onTap(offset)
                }
            }
            .themeBackground(theme)
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        if (width > 0 && height > 0) {
            LaunchedEffect(Unit) {
                gameViewModel.onSurfaceSizeChanged(width, height)
            }
        }

        when (gameState) {
            is GameState.Ready -> {
                val playedToday = gameViewModel.isDailyCompleted()
                val activity = LocalContext.current as? android.app.Activity
                StartScreen(
                    coins = economy.coins,
                    dailyBest = economy.dailyBest,
                    playedToday = playedToday,
                    highestLevel = gameViewModel.highestLevel,
                    reducedMotion = gameViewModel.reducedMotion,
                    onStartLevel = { level -> gameViewModel.startGame(level) },
                    onZen = { gameViewModel.startZen() },
                    onDaily = { gameViewModel.startDaily() },
                    onSettings = { showSettings = true },
                    onPowerUpGuarantee = if (activity != null) {
                        {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "powerup_guarantee",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {},
                                onUserEarnedReward = {
                                    gameViewModel.setGuaranteeNextPowerUp()
                                }
                            )
                        }
                    } else null,
                    onDailyRetry = if (playedToday && activity != null) {
                        {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "retry_daily",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {},
                                onUserEarnedReward = {
                                    gameViewModel.startDaily()
                                }
                            )
                        }
                    } else null,
                    adsRemaining = LevelPlayAdManager.adsRemaining()
                )
            }
            is GameState.Playing -> {
                GameScreen(
                    width = width,
                    height = height,
                    bubbles = bubbles,
                    messages = messages,
                    particles = particles,
                    score = gameState.score,
                    highScore = gameState.highScore,
                    level = gameState.currentLevel,
                    activePowerUps = activePowerUps,
                    timeRemaining = gameViewModel.levelTimeRemaining,
                    timeLimit = gameViewModel.levelTimeLimit,
                    bubblesLeft = bubbles.size,
                    combo = combo,
                    coins = economy.coins,
                    isZen = gameState.isZen,
                    isDaily = gameState.isDaily,
                    skin = skin,
                    theme = theme,
                    shakeTrauma = shakeTrauma,
                    reducedMotion = gameViewModel.reducedMotion,
                    tutorialCard = gameViewModel.tutorialCard,
                    tutorialText = gameViewModel.tutorialCard?.let { gameViewModel.tutorialTextFor(it) },
                    onDismissTutorial = { gameViewModel.dismissTutorialCard() },
                    onPause = { gameViewModel.pauseGame() },
                    onEndZen = { gameViewModel.endZenSession() }
                )
            }
            is GameState.Paused -> {
                GameScreen(
                    width = width,
                    height = height,
                    bubbles = bubbles,
                    messages = messages,
                    particles = particles,
                    score = gameState.score,
                    highScore = gameState.highScore,
                    level = gameState.currentLevel,
                    activePowerUps = activePowerUps,
                    timeRemaining = gameViewModel.levelTimeRemaining,
                    timeLimit = gameViewModel.levelTimeLimit,
                    bubblesLeft = bubbles.size,
                    combo = combo,
                    coins = economy.coins,
                    isZen = gameState.isZen,
                    isDaily = gameState.isDaily,
                    skin = skin,
                    theme = theme,
                    shakeTrauma = 0f,
                    reducedMotion = gameViewModel.reducedMotion,
                    onPause = { gameViewModel.resumeGame() },
                    onEndZen = {},
                    isPaused = true
                )
                PauseOverlay(
                    onResume = { gameViewModel.resumeGame() },
                    onRestart = { gameViewModel.restartGame() },
                    onHome = { gameViewModel.goHome() }
                )
            }
            is GameState.LevelComplete -> {
                val activity = LocalContext.current as? android.app.Activity
                val waitingForAd = gameState.waitingForAd

                if (waitingForAd && activity != null) {
                    // Side effect must run once per level — not on every recomposition
                    LaunchedEffect(gameState.completedLevel) {
                        if (LevelPlayAdManager.adsRemaining() > 0) {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "level_interstitial",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {
                                    // If ad fails, skip the ad and go to next level directly
                                    gameViewModel.startNextLevelConfirmed()
                                },
                                onUserEarnedReward = {
                                    gameViewModel.startNextLevelConfirmed()
                                }
                            )
                        } else {
                            // Daily ad cap reached — skip straight to the next level
                            gameViewModel.startNextLevelConfirmed()
                        }
                    }
                } else {
                    // No ad needed — show the level complete screen
                    val milestoneCoins = EconomyConfig.milestoneRewardForLevel(gameState.completedLevel)
                    LevelCompleteScreen(
                        theme = theme,
                        score = gameState.score,
                        highScore = gameState.highScore,
                        completedLevel = gameState.completedLevel,
                        nextLevel = gameState.nextLevel,
                        milestoneCoins = milestoneCoins,
                        stars = starsForTimeFraction(gameViewModel.lastLevelTimeFraction),
                        unlockedNextLevel = gameViewModel.levelUnlockedThisRun,
                        reducedMotion = gameViewModel.reducedMotion,
                        onNext = { gameViewModel.startNextLevel() },
                        onHome = { gameViewModel.goHome() },
                        onCoinBonus = if (activity != null && gameState.completedLevel >= 3) {
                            {
                                LevelPlayAdManager.loadAndShowRewardedAd(
                                    adType = "coin_bonus",
                                    activity = activity,
                                    onAdLoaded = {},
                                    onAdFailed = {},
                                    onUserEarnedReward = {
                                        gameViewModel.addBonusCoins(50)
                                    }
                                )
                            }
                        } else null,
                        adsRemaining = LevelPlayAdManager.adsRemaining()
                    )
                }
            }
            is GameState.GameOver -> {
                val activity = LocalContext.current as? android.app.Activity
                GameOverScreen(
                    theme = theme,
                    finalScore = gameState.finalScore,
                    highScore = gameState.highScore,
                    isNewHighScore = gameState.isNewHighScore,
                    isTimeUp = gameState.isTimeUp,
                    won = gameState.won,
                    zen = gameState.zen,
                    daily = gameState.daily,
                    dailyReward = gameState.dailyReward,
                    coins = economy.coins,
                    prestigeLevel = economy.prestigeLevel,
                    canContinueWithCoins = economy.coins >= EconomyConfig.CONTINUE_COST && !gameState.won && !gameState.zen && !gameState.daily,
                    onRestart = { gameViewModel.restartGame() },
                    onHome = { gameViewModel.goHome() },
                    onPrestige = { gameViewModel.prestige() },
                    onContinue = if (!gameState.won && !gameState.zen && !gameState.daily && activity != null) {
                        {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "continue",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {},
                                onUserEarnedReward = {
                                    gameViewModel.continueGame()
                                    gameViewModel.addTimeBonus(15f)
                                }
                            )
                        }
                    } else null,
                    onContinueWithCoins = if (!gameState.won && !gameState.zen && !gameState.daily && economy.coins >= EconomyConfig.CONTINUE_COST) {
                        {
                            gameViewModel.continueWithCoins()
                        }
                    } else null,
                    onDoubleDaily = if (gameState.daily && gameState.dailyReward > 0 && activity != null) {
                        {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "double_daily",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {},
                                onUserEarnedReward = {
                                    gameViewModel.addBonusCoins(gameState.dailyReward)
                                }
                            )
                        }
                    } else null,
                    onCoinBonus = if (!gameState.zen && activity != null) {
                        {
                            LevelPlayAdManager.loadAndShowRewardedAd(
                                adType = "coin_bonus",
                                activity = activity,
                                onAdLoaded = {},
                                onAdFailed = {},
                                onUserEarnedReward = {
                                    gameViewModel.addBonusCoins(50)
                                }
                            )
                        }
                    } else null,
                    adsRemaining = LevelPlayAdManager.adsRemaining()
                )
            }
        }

        if (showSettings) {
            val activity = LocalContext.current as? android.app.Activity
            val adsRemoved by BillingManager.adsRemoved.collectAsStateWithLifecycle()
            SettingsScreen(
                theme = theme,
                economy = economy,
                soundEnabled = gameViewModel.soundEnabled,
                hapticsEnabled = gameViewModel.hapticsEnabled,
                reducedMotion = gameViewModel.reducedMotion,
                adsRemoved = adsRemoved,
                onToggleSound = { gameViewModel.toggleSound() },
                onToggleHaptics = { gameViewModel.toggleHaptics() },
                onToggleReducedMotion = { gameViewModel.toggleReducedMotion() },
                onUpgrade = { gameViewModel.upgradePowerUp(it) },
                onSelectSkin = { gameViewModel.selectSkin(it) },
                onSelectTheme = { gameViewModel.selectTheme(it) },
                onPrestige = { gameViewModel.prestige() },
                onClose = { showSettings = false },
                onTrySkin = if (activity != null) {
                    { skinName ->
                        LevelPlayAdManager.loadAndShowRewardedAd(
                            adType = "skin_preview",
                            activity = activity,
                            onAdLoaded = {},
                            onAdFailed = {},
                            onUserEarnedReward = {
                                gameViewModel.previewSkin(skinName)
                            }
                        )
                    }
                } else null,
                onRemoveAds = if (activity != null && !adsRemoved) {
                    { BillingManager.launchRemoveAdsFlow(activity) }
                } else null
            )
        }

        LevelPlayBanner(Modifier.align(Alignment.BottomCenter))
    }
}

private fun todayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

@Composable
fun StartScreen(
    coins: Int,
    dailyBest: Int,
    playedToday: Boolean,
    highestLevel: Int = 1,
    reducedMotion: Boolean = false,
    onStartLevel: (Int) -> Unit,
    onZen: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    onPowerUpGuarantee: (() -> Unit)? = null,
    onDailyRetry: (() -> Unit)? = null,
    adsRemaining: Int = 5
) {
    var step by remember { mutableStateOf(0) }
    var showLevelSelect by remember { mutableStateOf(false) }
    val staggerDelay = if (reducedMotion) 0L else 120L

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            for (i in 1..8) {
                kotlinx.coroutines.delay(staggerDelay)
                step = i
            }
        } else {
            step = 8
        }
    }

    fun staggerIndex(i: Int): Boolean = step >= i

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            AnimatedVisibility(
                visible = staggerIndex(1),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 })
            ) {
                LogoOrb(reducedMotion = reducedMotion)
            }
            Spacer(modifier = Modifier.height(LuxurySpacing.SM))
            AnimatedVisibility(
                visible = staggerIndex(2),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 })
            ) {
                Text(
                    "BUBBLES",
                    style = LuxuryTypography.DisplayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedVisibility(
                visible = staggerIndex(3),
                enter = fadeIn()
            ) {
                Text(
                    "POP · BREATHE · REPEAT",
                    style = LuxuryTypography.LabelMedium,
                    color = LuxuryColors.Gold300
                )
            }
            Spacer(modifier = Modifier.height(LuxurySpacing.XL))

            AnimatedVisibility(
                visible = staggerIndex(4),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.Pill),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LuxuryIcons.Coin,
                            contentDescription = null,
                            tint = LuxuryColors.Gold400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$coins",
                            style = LuxuryTypography.HeadlineMedium,
                            color = LuxuryColors.Gold400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(LuxurySpacing.XL))

            AnimatedVisibility(
                visible = staggerIndex(5),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                Column {
                    LuxuryButton(
                        text = "CONTINUE · LEVEL $highestLevel",
                        onClick = { onStartLevel(highestLevel) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.PlayArrow
                    )
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = if (showLevelSelect) "HIDE LEVELS" else "SELECT LEVEL",
                        onClick = { showLevelSelect = !showLevelSelect },
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Layers
                    )
                    AnimatedVisibility(visible = showLevelSelect) {
                        Column {
                            Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(LuxurySpacing.SM)
                            ) {
                                val playableMax = (highestLevel + 1).coerceAtMost(100)
                                for (n in 1..playableMax) {
                                    LevelChip(
                                        level = n,
                                        isNext = n == highestLevel + 1,
                                        onClick = { onStartLevel(n) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxuryButton(
                        text = "ZEN MODE",
                        onClick = onZen,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Lotus
                    )
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxuryButton(
                        text = if (playedToday) "DAILY · COMPLETED" else "DAILY CHALLENGE",
                        onClick = onDaily,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !playedToday,
                        primary = false,
                        icon = Icons.Default.DateRange
                    )
                }
            }
            Spacer(modifier = Modifier.height(LuxurySpacing.MD))
            AnimatedVisibility(
                visible = staggerIndex(6),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                LuxuryButton(
                    text = "SETTINGS & UPGRADES",
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Settings
                )
            }

            // ── Ad Buttons ──
            AnimatedVisibility(
                visible = staggerIndex(7) && onPowerUpGuarantee != null && adsRemaining > 0,
                enter = fadeIn()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "POWER-UP GUARANTEE · WATCH AD",
                        onClick = onPowerUpGuarantee!!,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Burst
                    )
                }
            }
            if (playedToday && onDailyRetry != null && adsRemaining > 0) {
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxuryButton(
                    text = "RETRY DAILY · WATCH AD",
                    onClick = onDailyRetry,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Refresh
                )
            }

            Spacer(modifier = Modifier.height(LuxurySpacing.XL))
            AnimatedVisibility(
                visible = staggerIndex(7),
                enter = fadeIn()
            ) {
                Text(
                    "Clear each level before time runs out.\nDifficulty grows with every level — speed, count, wind, gravity.\nCoins from pops & milestones buy power-up upgrades.",
                    style = LuxuryTypography.BodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun LevelChip(
    level: Int,
    isNext: Boolean,
    onClick: () -> Unit
) {
    GlassSurface(
        shape = RoundedCornerShape(LuxuryRadius.SM)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNext) {
                Icon(
                    imageVector = LuxuryIcons.Star,
                    contentDescription = "Next level",
                    tint = LuxuryColors.Gold400,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                "$level",
                style = LuxuryTypography.LabelLarge,
                color = if (isNext) LuxuryColors.Gold400 else Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun AboutLinkChip(label: String, url: String) {
    val context = LocalContext.current
    GlassSurface(shape = RoundedCornerShape(LuxuryRadius.Pill)) {
        Box(
            modifier = Modifier
                .clickable { openUrl(context, url) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                label,
                style = LuxuryTypography.LabelMedium,
                color = LuxuryColors.Gold300
            )
        }
    }
}

@Composable
fun LogoOrb(reducedMotion: Boolean = false) {
    val scale: Float
    val glow: Float
    if (reducedMotion) {
        scale = 1f
        glow = 0.72f
    } else {
        val transition = rememberInfiniteTransition(label = "logo")
        val animScale by transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        val animGlow by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        scale = animScale
        glow = animGlow
    }
    Box(modifier = Modifier.size(104.dp)) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LuxuryColors.Gold300.copy(alpha = glow),
                            LuxuryColors.Gold500,
                            LuxuryColors.Gold600
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = 18.dp)
                .size(22.dp)
                .background(Color.White.copy(alpha = 0.85f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-16).dp)
                .size(10.dp)
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
        )
    }
}

@Composable
fun GameScreen(
    width: Float,
    height: Float,
    bubbles: List<Bubble>,
    messages: List<PopMessage>,
    particles: List<Particle>,
    score: Int,
    highScore: Int,
    level: Int,
    activePowerUps: List<PowerUpType>,
    timeRemaining: Float,
    timeLimit: Float,
    bubblesLeft: Int,
    combo: Int,
    coins: Int,
    isZen: Boolean,
    isDaily: Boolean,
    skin: BubbleSkin,
    theme: ThemePalette,
    shakeTrauma: Float,
    reducedMotion: Boolean,
    tutorialCard: BubbleType? = null,
    tutorialText: String? = null,
    onDismissTutorial: () -> Unit = {},
    onPause: () -> Unit,
    onEndZen: () -> Unit,
    isPaused: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val shakePx = if (reducedMotion || shakeTrauma <= 0f) {
            0f
        } else {
            (Random.nextFloat() - 0.5f) * 2f * shakeTrauma * 14f
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakePx
                    translationY = shakePx
                }
        ) {
            drawRect(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
            particles.forEach { drawParticle(it, reducedMotion) }
            bubbles.forEach { drawBubble(it, skin) }
        }

        messages.forEach { PopMessageView(it) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(top = 70.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LuxuryStat("Score", "$score", LuxuryColors.Gold400)
                        LuxuryStat("Best", "$highScore", Color.White)
                        if (isZen) {
                            LuxuryStat("Zen", "INF", theme.accent)
                        } else {
                            LuxuryStat("Level", "$level", theme.accent)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = LuxuryIcons.Coin,
                                contentDescription = null,
                                tint = LuxuryColors.Gold400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$coins",
                                style = LuxuryTypography.HeadlineMedium,
                                color = LuxuryColors.Gold400
                            )
                        }
                    }
                }

                if (!isZen) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        LuxuryTimerRing(
                            progress = timeRemaining / timeLimit,
                            accent = if (timeRemaining < 10f) LuxuryColors.Destructive else theme.accent,
                            size = 64.dp
                        )
                        Text(
                            text = "${timeRemaining.toInt()}s",
                            style = LuxuryTypography.HeadlineSmall,
                            color = if (timeRemaining < 10f) LuxuryColors.Destructive else Color.White
                        )
                    }
                }

                if (combo >= 2) {
                    ComboDisplay(combo = combo)
                }

                if (activePowerUps.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        activePowerUps.forEach { powerUp ->
                            PowerUpIndicator(powerUp = powerUp)
                        }
                    }
                }

                if (isZen) {
                    Text(
                        text = "Breathe... pop whenever you like",
                        style = LuxuryTypography.BodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Text(
                        text = "Bubbles left: $bubblesLeft",
                        style = LuxuryTypography.BodySmall,
                        color = if (bubblesLeft <= 0) theme.accent else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            GlassSurface(
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 140.dp, end = 16.dp)
                    .size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { if (isZen) onEndZen() else onPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isZen) Icons.Default.Check else LuxuryIcons.Pause,
                        contentDescription = if (isZen) "End zen session" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        if (tutorialCard != null && tutorialText != null) {
            BubbleTutorialCard(
                type = tutorialCard,
                text = tutorialText,
                reducedMotion = reducedMotion,
                onDismiss = onDismissTutorial
            )
        }
    }
}

/**
 * First-burst tutorial card — full-pause modal teaching a newly unlocked
 * bubble type. GlassSurface + luxury typography; tap to dismiss, auto-dismiss
 * after 3s. Respects reduced motion (no spring bounce).
 */
@Composable
fun BubbleTutorialCard(
    type: BubbleType,
    text: String,
    reducedMotion: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(type) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(LuxuryRadius.LG),
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .then(if (reducedMotion) Modifier else Modifier.graphicsLayer {
                    scaleX = 0.92f
                    scaleY = 0.92f
                })
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "NEW BUBBLE",
                    style = LuxuryTypography.LabelMedium,
                    color = LuxuryColors.Gold300
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    type.name.replace('_', ' '),
                    style = LuxuryTypography.DisplaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text,
                    style = LuxuryTypography.BodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                Text(
                    "TAP TO CONTINUE",
                    style = LuxuryTypography.LabelSmall,
                    color = LuxuryColors.Gold400
                )
            }
        }
    }
}

@Composable
fun ComboDisplay(combo: Int) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo > 1) {
            scale.snapTo(1.3f)
            scale.animateTo(1f, tween(220, easing = LuxuryMotion.EaseOut))
        }
    }
    Text(
        text = "COMBO x$combo",
        style = LuxuryTypography.HeadlineLarge,
        color = LuxuryColors.Gold400,
        modifier = Modifier
            .padding(top = 6.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}

@Composable
fun PowerUpIndicator(powerUp: PowerUpType) {
    val (icon, label, color) = when (powerUp) {
        PowerUpType.SLOW_MO -> Triple(LuxuryIcons.Timer, "SLOW", LuxuryColors.IceBlue)
        PowerUpType.FREEZE -> Triple(LuxuryIcons.Snowflake, "FREEZE", LuxuryColors.IceBlue)
        PowerUpType.MULTI_POP -> Triple(LuxuryIcons.Burst, "MULTI", LuxuryColors.Gold400)
        PowerUpType.PRISM -> Triple(LuxuryIcons.Prism, "PRISM", LuxuryColors.PrismMagenta)
    }

    GlassSurface(
        shape = RoundedCornerShape(LuxuryRadius.Pill),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = LuxuryTypography.LabelSmall,
                color = color
            )
        }
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassSurface(shape = RoundedCornerShape(LuxuryRadius.XL),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "PAUSED",
                    style = LuxuryTypography.DisplaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Take a breath.",
                    style = LuxuryTypography.BodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(LuxurySpacing.LG))
                LuxuryButton(
                    text = "RESUME",
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow
                )
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxuryButton(
                    text = "RESTART",
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Refresh
                )
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxuryButton(
                    text = "HOME",
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Home
                )
            }
        }
    }
}
}

@Composable
fun LevelCompleteScreen(
    theme: ThemePalette,
    score: Int,
    highScore: Int,
    completedLevel: Int,
    nextLevel: Int,
    milestoneCoins: Int = 0,
    stars: Int = 1,
    unlockedNextLevel: Boolean = false,
    reducedMotion: Boolean = false,
    onNext: () -> Unit,
    onHome: () -> Unit,
    onCoinBonus: (() -> Unit)? = null,
    adsRemaining: Int = 5
) {
    // ── Achievement choreography ──
    // Card springs in with a gentle overshoot, stars pop in sequentially,
    // the score counts up, and the unlock chip lands last.
    val cardEntrance = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            cardEntrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    var starsRevealed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            kotlinx.coroutines.delay(250)
            starsRevealed = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(LuxuryRadius.XL),
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .graphicsLayer {
                    scaleX = cardEntrance.value
                    scaleY = cardEntrance.value
                    alpha = cardEntrance.value.coerceIn(0f, 1f)
                }
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LEVEL $completedLevel",
                    style = LuxuryTypography.LabelMedium,
                    color = LuxuryColors.Gold300,
                    textAlign = TextAlign.Center
                )
                Text(
                    "CLEAR!",
                    style = LuxuryTypography.DisplayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // ── Star rating: earned stars pop in one by one ──
                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                Row(horizontalArrangement = Arrangement.spacedBy(LuxurySpacing.SM)) {
                    repeat(3) { index ->
                        val earned = index < stars
                        val starScale by animateFloatAsState(
                            targetValue = if (starsRevealed && earned) 1f else 0.3f,
                            animationSpec = if (reducedMotion) snap() else keyframes {
                                durationMillis = 450
                                1.45f at 220
                                1f at 450
                            },
                            label = "star$index"
                        )
                        Icon(
                            imageVector = LuxuryIcons.Star,
                            contentDescription = if (earned) "Star earned" else "Star not earned",
                            tint = if (earned) LuxuryColors.Gold400 else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = starScale
                                    scaleY = starScale
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))

                // ── Score counts up from 0 ──
                val displayedScore by animateIntAsState(
                    targetValue = score,
                    animationSpec = if (reducedMotion) snap() else tween(900, easing = LuxuryMotion.EaseOut),
                    label = "scoreCountUp"
                )
                Text(
                    "+$displayedScore",
                    style = LuxuryTypography.HeadlineLarge,
                    color = LuxuryColors.Gold400
                )
                Text(
                    "Best  $highScore",
                    style = LuxuryTypography.BodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (milestoneCoins > 0) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = LuxuryIcons.Coin,
                            contentDescription = null,
                            tint = LuxuryColors.Gold400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "+$milestoneCoins COINS",
                            style = LuxuryTypography.HeadlineSmall,
                            color = LuxuryColors.Gold400
                        )
                    }
                    Text(
                        "Milestone reward — spend on upgrades",
                        style = LuxuryTypography.BodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                }

                // ── New level unlocked chip ──
                if (unlockedNextLevel) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    GlassSurface(shape = RoundedCornerShape(LuxuryRadius.Pill)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LuxuryIcons.Star,
                                contentDescription = null,
                                tint = LuxuryColors.Gold400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LEVEL $nextLevel UNLOCKED",
                                style = LuxuryTypography.LabelMedium,
                                color = LuxuryColors.Gold300
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.LG))
                LuxuryButton(
                    text = "NEXT LEVEL $nextLevel",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.AutoMirrored.Filled.ArrowForward
                )
                if (onCoinBonus != null && adsRemaining > 0) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "+50 COINS · WATCH AD",
                        onClick = onCoinBonus,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Coin
                    )
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxuryButton(
                    text = "HOME",
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Home
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(
    theme: ThemePalette,
    finalScore: Int,
    highScore: Int,
    isNewHighScore: Boolean,
    isTimeUp: Boolean,
    won: Boolean,
    zen: Boolean,
    daily: Boolean,
    dailyReward: Int,
    coins: Int,
    prestigeLevel: Int,
    canContinueWithCoins: Boolean = false,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onPrestige: () -> Unit,
    onContinue: (() -> Unit)? = null,
    onContinueWithCoins: (() -> Unit)? = null,
    onDoubleDaily: (() -> Unit)? = null,
    onCoinBonus: (() -> Unit)? = null,
    adsRemaining: Int = 5
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(shape = RoundedCornerShape(LuxuryRadius.XL),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    won -> {
                        Text(
                            "VICTORY!",
                            style = LuxuryTypography.DisplaySmall,
                            color = LuxuryColors.Gold400,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$finalScore levels cleared!",
                            style = LuxuryTypography.BodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    zen -> {
                        Text(
                            "ZEN COMPLETE",
                            style = LuxuryTypography.DisplaySmall,
                            color = theme.accent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    daily -> {
                        Text(
                            "DAILY COMPLETE",
                            style = LuxuryTypography.DisplaySmall,
                            color = theme.accent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Reward +$dailyReward",
                            style = LuxuryTypography.HeadlineMedium,
                            color = LuxuryColors.Gold400
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    isNewHighScore -> {
                        Text(
                            "NEW HIGH SCORE",
                            style = LuxuryTypography.DisplaySmall,
                            color = LuxuryColors.Gold400,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    isTimeUp -> {
                        Text(
                            "TIME'S UP",
                            style = LuxuryTypography.DisplaySmall,
                            color = LuxuryColors.Destructive,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pop bubbles to add time",
                            style = LuxuryTypography.BodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    else -> {
                        Text(
                            "GAME OVER",
                            style = LuxuryTypography.DisplaySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Text(
                    "$finalScore",
                    style = LuxuryTypography.DisplayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Best  $highScore",
                    style = LuxuryTypography.BodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (!zen && !daily) {
                    Text(
                        "Coins  $coins",
                        style = LuxuryTypography.BodyMedium,
                        color = LuxuryColors.Gold400
                    )
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.LG))
                LuxuryButton(
                    text = if (zen) "POP AGAIN" else "PLAY AGAIN",
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow
                )
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                // ── Rewarded Ad Buttons ──
                if (!zen && !won && !daily && onContinue != null && adsRemaining > 0) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "CONTINUE · WATCH AD",
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = Icons.Default.PlayArrow
                    )
                }
                if (canContinueWithCoins && onContinueWithCoins != null) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "CONTINUE · ${EconomyConfig.CONTINUE_COST} COINS",
                        onClick = onContinueWithCoins,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Coin
                    )
                }
                if (daily && dailyReward > 0 && onDoubleDaily != null && adsRemaining > 0) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "DOUBLE REWARD · WATCH AD",
                        onClick = onDoubleDaily,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Sparkle
                    )
                }
                if (!zen && onCoinBonus != null && adsRemaining > 0) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "+50 COINS · WATCH AD",
                        onClick = onCoinBonus,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Coin
                    )
                }
                if (adsRemaining <= 5) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$adsRemaining ads remaining today",
                        style = LuxuryTypography.LabelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxuryButton(
                    text = "HOME",
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    icon = Icons.Default.Home
                )
                if (won && prestigeLevel < 1) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                    LuxuryButton(
                        text = "PRESTIGE · GOLD SKIN + 2X COINS",
                        onClick = onPrestige,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Crown
                    )
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    theme: ThemePalette,
    economy: EconomyState,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    reducedMotion: Boolean,
    adsRemoved: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleReducedMotion: () -> Unit,
    onUpgrade: (String) -> Unit,
    onSelectSkin: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onPrestige: () -> Unit,
    onClose: () -> Unit,
    onTrySkin: ((String) -> Unit)? = null,
    onRemoveAds: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(shape = RoundedCornerShape(LuxuryRadius.XL),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SETTINGS",
                        style = LuxuryTypography.DisplaySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.Pill),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close settings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.Pill),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LuxuryIcons.Coin,
                            contentDescription = null,
                            tint = LuxuryColors.Gold400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${economy.coins} coins",
                            style = LuxuryTypography.LabelLarge,
                            color = LuxuryColors.Gold400
                        )
                    }
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.MD))

                LuxurySectionTitle("Audio & Haptics", LuxuryIcons.Volume)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LuxuryToggle("Sound", soundEnabled, onToggleSound)
                        LuxuryToggle("Haptics", hapticsEnabled, onToggleHaptics)
                        LuxuryToggle("Reduced Motion", reducedMotion, onToggleReducedMotion)
                    }
                }

                // ── Remove Ads ──
                if (!adsRemoved && onRemoveAds != null) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxurySectionTitle("Premium", LuxuryIcons.Sparkle)
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.MD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Remove All Ads",
                                style = LuxuryTypography.HeadlineSmall,
                                color = LuxuryColors.Gold400
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No banners, no interstitials, no rewarded ads. Rewards grant instantly.",
                                style = LuxuryTypography.BodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                            LuxuryButton(
                                text = "REMOVE ADS",
                                onClick = onRemoveAds,
                                modifier = Modifier.fillMaxWidth(),
                                icon = LuxuryIcons.Sparkle,
                                height = 48.dp
                            )
                        }
                    }
                }
                if (adsRemoved) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxurySectionTitle("Premium", LuxuryIcons.Sparkle)
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.MD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LuxuryColors.Gold400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Ads removed · Premium",
                                style = LuxuryTypography.BodyLarge,
                                color = LuxuryColors.Gold400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("Upgrades", LuxuryIcons.Bolt)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        UpgradeRow(
                            icon = LuxuryIcons.Timer,
                            label = "Slow-Mo",
                            detail = "Lv ${economy.slowMoLevel} · ${economy.slowMoDurationMs / 1000}s",
                            currentLevel = economy.slowMoLevel,
                            coins = economy.coins,
                            onUpgrade = { onUpgrade("slowMo") }
                        )
                        UpgradeRow(
                            icon = LuxuryIcons.Snowflake,
                            label = "Freeze",
                            detail = "Lv ${economy.freezeLevel} · ${economy.freezeDurationMs / 1000}s",
                            currentLevel = economy.freezeLevel,
                            coins = economy.coins,
                            onUpgrade = { onUpgrade("freeze") }
                        )
                        UpgradeRow(
                            icon = LuxuryIcons.Burst,
                            label = "Multi-Pop",
                            detail = "Lv ${economy.multiPopLevel} · ${economy.multiPopRadius.toInt()}px",
                            currentLevel = economy.multiPopLevel,
                            coins = economy.coins,
                            onUpgrade = { onUpgrade("multiPop") }
                        )
                        UpgradeRow(
                            icon = LuxuryIcons.Prism,
                            label = "Prism",
                            detail = "Lv ${economy.prismLevel} · x${(economy.prismBoost * 100).toInt()}%",
                            currentLevel = economy.prismLevel,
                            coins = economy.coins,
                            onUpgrade = { onUpgrade("prism") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("Skins", LuxuryIcons.Layers)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BubbleSkins.all.forEach { s ->
                        val locked = s.needsPrestige && economy.prestigeLevel < 1
                        val selected = s.name == economy.skin
                        SkinCard(
                            skin = s,
                            locked = locked,
                            selected = selected,
                            onClick = { if (!locked) onSelectSkin(s.name) },
                            onTryAd = if (locked && onTrySkin != null) {{ onTrySkin(s.name) }} else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                LuxurySectionTitle("Themes", LuxuryIcons.Tint)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BubbleThemes.all.forEach { t ->
                        ThemeCard(
                            themePalette = t,
                            selected = t.name == economy.theme,
                            onClick = { onSelectTheme(t.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("Stats", LuxuryIcons.Chart)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        StatRow("Lifetime pops", "${economy.lifetimePops}")
                        StatRow("Best combo", "x${economy.maxCombo}")
                        StatRow("Games played", "${economy.gamesPlayed}")
                        StatRow("Prestige", "Lv ${economy.prestigeLevel}")
                        StatRow("Daily best", "${economy.dailyBest}")
                    }
                }

                if (economy.prestigeLevel < 1) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxuryButton(
                        text = "PRESTIGE · GOLD SKIN + 2X COINS",
                        onClick = onPrestige,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        icon = LuxuryIcons.Sparkle,
                        height = 48.dp
                    )
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("About", LuxuryIcons.Lotus)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Bubbles",
                            style = LuxuryTypography.HeadlineSmall,
                            color = Color.White
                        )
                        Text(
                            "v$APP_VERSION",
                            style = LuxuryTypography.LabelMedium,
                            color = LuxuryColors.Gold300
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "A premium bubble-popping stress buster — pop, breathe, repeat.",
                            style = LuxuryTypography.BodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                        Text(
                            "© 2026 Ashwath AI. All rights reserved.",
                            style = LuxuryTypography.LabelSmall,
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                        Row(horizontalArrangement = Arrangement.spacedBy(LuxurySpacing.SM)) {
                            AboutLinkChip("ABOUT", "https://atulkpal.github.io/bubbles/about.html")
                            AboutLinkChip("PRIVACY", "https://atulkpal.github.io/bubbles/privacy.html")
                            AboutLinkChip("DELETE DATA", "https://atulkpal.github.io/bubbles/data-deletion.html")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxuryButton(
                    text = "CLOSE",
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Close,
                    height = 48.dp
                )
            }
        }
    }
}

@Composable
fun UpgradeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    currentLevel: Int,
    coins: Int,
    onUpgrade: () -> Unit
) {
    val maxed = currentLevel >= EconomyConfig.UPGRADE_MAX
    val affordable = coins >= EconomyConfig.UPGRADE_COST
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LuxuryColors.Gold400,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, style = LuxuryTypography.BodyLarge, color = Color.White)
                Text(detail, style = LuxuryTypography.BodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        GlassSurface(
            shape = RoundedCornerShape(LuxuryRadius.SM),
            modifier = Modifier
                .height(36.dp)
                .clickable(enabled = !maxed && affordable) { onUpgrade() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!maxed) {
                    Icon(
                        imageVector = LuxuryIcons.Coin,
                        contentDescription = null,
                        tint = LuxuryColors.Gold400,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (maxed) "MAX" else "${EconomyConfig.UPGRADE_COST}",
                    style = LuxuryTypography.LabelMedium,
                    color = if (maxed) Color.White.copy(alpha = 0.4f) else LuxuryColors.Gold400
                )
            }
        }
    }
}

@Composable
fun SkinCard(skin: BubbleSkin, locked: Boolean, selected: Boolean, onClick: () -> Unit, onTryAd: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .background(
                if (selected) LuxuryColors.Ink900 else LuxuryColors.Ink900.copy(alpha = 0.6f),
                RoundedCornerShape(LuxuryRadius.MD)
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) else Modifier
            )
            .clickable(enabled = !locked) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.radialGradient(
                        listOf(skin.palette.first(), skin.palette.first().copy(alpha = 0.6f))
                    ),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = skin.name,
                style = LuxuryTypography.LabelSmall,
                color = if (selected) LuxuryColors.Gold300 else Color.White.copy(alpha = 0.7f)
            )
        }
        if (locked && onTryAd != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TRY",
                style = LuxuryTypography.LabelSmall,
                color = LuxuryColors.Gold400,
                modifier = Modifier
                    .clickable { onTryAd() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ThemeCard(themePalette: ThemePalette, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .background(
                if (selected) LuxuryColors.Ink900 else LuxuryColors.Ink900.copy(alpha = 0.6f),
                RoundedCornerShape(LuxuryRadius.MD)
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.verticalGradient(listOf(themePalette.top, themePalette.bottom)),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = themePalette.name,
            style = LuxuryTypography.LabelSmall,
            color = if (selected) LuxuryColors.Gold300 else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = LuxuryTypography.BodyMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = LuxuryTypography.BodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun DrawScope.drawBubble(bubble: Bubble, skin: BubbleSkin) {
    val center = Offset(bubble.x, bubble.y)
    val r = bubble.radius
    val base = bubble.color

    // Fixed light source — slightly off-center top-left for balanced highlights
    val lightX = size.width * 0.28f
    val lightY = size.height * 0.12f
    val dx = lightX - center.x
    val dy = lightY - center.y
    val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = dx / dist
    val ny = dy / dist

    // Specular sits closer to the surface edge facing the light
    val hlOffX = nx * r * 0.48f
    val hlOffY = ny * r * 0.48f
    // Secondary specular slightly further in
    val secX = nx * r * 0.28f
    val secY = ny * r * 0.28f
    // Ambient bloom on the shadow side
    val ambX = -nx * r * 0.38f
    val ambY = -ny * r * 0.38f

    // 1. Soft outer glow — thinner, tighter to the bubble
    drawCircle(
        color = base.copy(alpha = 0.10f),
        radius = r * 1.08f,
        center = center,
        style = Stroke(width = r * 0.05f)
    )

    // 2. Main glass body — radial gradient biased toward the light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(base, base.copy(alpha = 0.93f), base.copy(alpha = 0.68f)),
            center = Offset(center.x + hlOffX * 0.65f, center.y + hlOffY * 0.65f),
            radius = r * 1.35f
        ),
        radius = r,
        center = center
    )

    // 3. Lens layer — broad warm bloom toward the light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(center.x + hlOffX, center.y + hlOffY),
            radius = r * 0.95f
        ),
        radius = r,
        center = center
    )

    // 4. Primary specular — crisp bright dot near the surface
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = r * 0.14f,
        center = Offset(center.x + hlOffX, center.y + hlOffY)
    )
    // 5. Secondary specular — smaller, closer to center
    drawCircle(
        color = Color.White.copy(alpha = 0.42f),
        radius = r * 0.06f,
        center = Offset(center.x + secX, center.y + secY)
    )
    // 6. Ambient glow — soft fill on the shadow side
    drawCircle(
        color = Color.White.copy(alpha = 0.11f),
        radius = r * 0.50f,
        center = Offset(center.x + ambX, center.y + ambY)
    )

    val iconColor = if (base.luminance() > 0.55f) LuxuryColors.Ink950 else Color.White
    val iconSize = r * 1.4f
    when (bubble.bubbleType) {
        BubbleType.NORMAL -> {
            if (skin.emoji != null) {
                drawSparkleIcon(center.x, center.y, r * 0.8f, iconColor)
            }
        }
        BubbleType.BOMB -> {
            drawBombIcon(center.x, center.y, iconSize, iconColor)
        }
        BubbleType.RAINBOW -> {
            drawPrismIcon(center.x, center.y, iconSize, iconColor)
        }
        BubbleType.FROZEN -> {
            drawIceIcon(center.x, center.y, iconSize, iconColor)
            if (bubble.cracked) {
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(center.x - r * 0.5f, center.y - r * 0.4f),
                    end = Offset(center.x + r * 0.2f, center.y + r * 0.5f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(center.x + r * 0.4f, center.y - r * 0.3f),
                    end = Offset(center.x - r * 0.3f, center.y + r * 0.3f),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        BubbleType.BOSS -> {
            drawCrownIcon(center.x, center.y, iconSize, LuxuryColors.Gold400)
            val pips = bubble.health
            val pipSize = 8.dp.toPx()
            val gap = 4.dp.toPx()
            val total = pips * pipSize + (pips - 1) * gap
            val startX = center.x - total / 2f
            val pipY = center.y + r + 10.dp.toPx()
            for (i in 0 until pips) {
                drawCircle(
                    color = LuxuryColors.Gold300,
                    radius = pipSize / 2f,
                    center = Offset(startX + i * (pipSize + gap) + pipSize / 2f, pipY)
                )
            }
        }
        BubbleType.MAGNET -> {
            // Draw magnet symbol: N/S split with a line
            drawPath(
                path = Path().apply {
                    val mr = iconSize * 0.7f
                    moveTo(center.x - mr, center.y)
                    lineTo(center.x + mr, center.y)
                    moveTo(center.x, center.y - mr)
                    lineTo(center.x, center.y + mr)
                },
                color = LuxuryColors.Ink950,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = LuxuryColors.Gold400.copy(alpha = 0.5f),
                radius = iconSize * 0.4f,
                center = center
            )
        }
        BubbleType.TICKING_BOMB -> {
            drawBombIcon(center.x, center.y, iconSize, iconColor)
            // Countdown tick mark
            drawCircle(
                color = LuxuryColors.Gold400,
                radius = iconSize * 0.3f,
                center = Offset(center.x, center.y - iconSize * 0.1f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        BubbleType.CHAOS -> {
            // Chaos symbol: swirling arcs
            repeat(3) { i ->
                val rot = i * 2.0f * 3.14159f / 3f
                val path = Path()
                val sr = iconSize * 0.5f
                val cx = center.x + cos(rot) * sr * 0.3f
                val cy = center.y + sin(rot) * sr * 0.3f
                path.moveTo(cx - sr * 0.5f, cy)
                path.arcTo(
                    Rect(cx - sr * 0.5f, cy - sr * 0.5f, cx + sr * 0.5f, cy + sr * 0.5f),
                    0f,
                    270f,
                    false
                )
                drawPath(path, iconColor)
            }
        }
        BubbleType.GHOST -> {
            // Ghost: wavy outline icon
            drawPath(
                path = Path().apply {
                    val gr = iconSize * 0.4f
                    moveTo(center.x - gr, center.y + gr * 0.3f)
                    quadraticTo(
                        center.x, center.y - gr * 1.2f,
                        center.x + gr, center.y + gr * 0.3f
                    )
                    lineTo(center.x + gr * 0.8f, center.y + gr * 0.8f)
                    quadraticTo(
                        center.x, center.y + gr * 1.2f,
                        center.x - gr * 0.8f, center.y + gr * 0.8f
                    )
                    close()
                },
                color = iconColor.copy(alpha = 0.8f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }

    if (bubble.isPowerUp) {
        drawCircle(
            color = LuxuryColors.Gold400,
            radius = r * 1.1f,
            center = center,
            style = Stroke(width = 2.5.dp.toPx())
        )
        when (bubble.powerUpType) {
            PowerUpType.SLOW_MO -> drawClockIcon(center.x, center.y, iconSize, LuxuryColors.Ink950)
            PowerUpType.FREEZE -> drawIceIcon(center.x, center.y, iconSize, LuxuryColors.Ink950)
            PowerUpType.MULTI_POP -> drawSparkleIcon(center.x, center.y, iconSize, LuxuryColors.Ink950)
            PowerUpType.PRISM -> drawPrismIcon(center.x, center.y, iconSize, LuxuryColors.Ink950)
            null -> {}
        }
    }
}

private fun DrawScope.drawSparkleIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.moveTo(cx, cy - r)
    path.lineTo(cx + r * 0.25f, cy - r * 0.25f)
    path.lineTo(cx + r, cy)
    path.lineTo(cx + r * 0.25f, cy + r * 0.25f)
    path.lineTo(cx, cy + r)
    path.lineTo(cx - r * 0.25f, cy + r * 0.25f)
    path.lineTo(cx - r, cy)
    path.lineTo(cx - r * 0.25f, cy - r * 0.25f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawBombIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.addOval(Rect(cx - r * 0.9f, cy - r * 0.7f, cx + r * 0.9f, cy + r * 0.95f))
    path.moveTo(cx - r * 0.35f, cy - r * 0.65f)
    path.lineTo(cx - r * 0.1f, cy - r * 1.05f)
    path.lineTo(cx + r * 0.25f, cy - r * 0.9f)
    path.lineTo(cx + r * 0.35f, cy - r * 0.6f)
    path.close()
    path.moveTo(cx + r * 0.05f, cy - r * 1.05f)
    path.lineTo(cx + r * 0.05f, cy - r * 1.4f)
    path.lineTo(cx + r * 0.45f, cy - r * 1.1f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawPrismIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.moveTo(cx, cy - r * 0.75f)
    path.lineTo(cx + r * 0.95f, cy + r * 0.65f)
    path.lineTo(cx - r * 0.95f, cy + r * 0.65f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawIceIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.moveTo(cx, cy - r * 0.8f)
    path.lineTo(cx + r * 0.7f, cy + r * 0.7f)
    path.lineTo(cx - r * 0.7f, cy + r * 0.7f)
    path.close()
    path.moveTo(cx, cy + r * 0.8f)
    path.lineTo(cx + r * 0.7f, cy - r * 0.7f)
    path.lineTo(cx - r * 0.7f, cy - r * 0.7f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawCrownIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.moveTo(cx - r * 0.8f, cy + r * 0.55f)
    path.lineTo(cx - r * 0.9f, cy - r * 0.45f)
    path.lineTo(cx - r * 0.3f, cy + r * 0.0f)
    path.lineTo(cx, cy - r * 0.7f)
    path.lineTo(cx + r * 0.3f, cy + r * 0.0f)
    path.lineTo(cx + r * 0.9f, cy - r * 0.45f)
    path.lineTo(cx + r * 0.8f, cy + r * 0.55f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawClockIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val r = s / 2f
    val path = Path()
    path.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
    path.moveTo(cx - r * 0.07f, cy - r * 0.6f)
    path.lineTo(cx + r * 0.07f, cy - r * 0.6f)
    path.lineTo(cx + r * 0.07f, cy + r * 0.12f)
    path.lineTo(cx - r * 0.07f, cy + r * 0.12f)
    path.close()
    path.moveTo(cx - r * 0.07f, cy)
    path.lineTo(cx + r * 0.5f, cy + r * 0.42f)
    path.lineTo(cx + r * 0.3f, cy + r * 0.58f)
    path.lineTo(cx - r * 0.07f, cy + r * 0.14f)
    path.close()
    drawPath(path, color)
}

@Composable
fun PopMessageView(msg: PopMessage) {
    Text(
        text = msg.text,
        color = msg.color.copy(alpha = msg.alpha),
        fontSize = (msg.fontSize * msg.scale).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.4f * msg.alpha),
                blurRadius = 8f
            )
        ),
        modifier = Modifier
            .offset(x = (msg.x - 50).dp, y = msg.y.dp)
            .rotate(msg.rotation)
    )
}

fun DrawScope.drawParticle(particle: Particle, reducedMotion: Boolean) {
    if (reducedMotion && particle.alpha > 0.4f) return
    val alpha = if (reducedMotion) particle.alpha * 0.5f else particle.alpha
    val c = Offset(particle.x, particle.y)
    if (particle.isRing) {
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = particle.radius,
            center = c,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = particle.color.copy(alpha = alpha * 0.3f),
            radius = particle.radius * 0.7f,
            center = c,
            style = Stroke(width = 6.dp.toPx())
        )
    } else {
        drawCircle(
            color = particle.color.copy(alpha = alpha * 0.22f),
            radius = particle.radius * 2.1f,
            center = c
        )
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = particle.radius,
            center = c
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.5f),
            radius = particle.radius * 0.4f,
            center = c
        )
    }
}
