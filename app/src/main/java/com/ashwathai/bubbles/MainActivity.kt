package com.ashwathai.bubbles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.ashwathai.bubbles.domain.repository.EconomyRepository
import com.ashwathai.bubbles.domain.repository.ScoreRepository
import com.ashwathai.bubbles.domain.repository.SettingsRepository
import com.ashwathai.bubbles.ui.game.GameViewModel
import com.ashwathai.bubbles.ui.theme.Gold
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
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
            checkLevelCompleteUseCase = AppModule.provideCheckLevelCompleteUseCase()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BubbleScreen(gameViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}

@Composable
fun AdBannerView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = androidx.compose.runtime.remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth().height(50.dp)
    )
}

fun Modifier.themeBackground(theme: ThemePalette): Modifier {
    return background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
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
                val playedToday = todayString() == economy.dailyDate
                StartScreen(
                    coins = economy.coins,
                    dailyBest = economy.dailyBest,
                    playedToday = playedToday,
                    onStart = { gameViewModel.startGame() },
                    onZen = { gameViewModel.startZen() },
                    onDaily = { gameViewModel.startDaily() },
                    onSettings = { showSettings = true }
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
                LevelCompleteScreen(
                    theme = theme,
                    score = gameState.score,
                    highScore = gameState.highScore,
                    completedLevel = gameState.completedLevel,
                    nextLevel = gameState.nextLevel,
                    onNext = { gameViewModel.startNextLevel() },
                    onHome = { gameViewModel.goHome() }
                )
            }
            is GameState.GameOver -> {
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
                    onRestart = { gameViewModel.restartGame() },
                    onHome = { gameViewModel.goHome() },
                    onPrestige = { gameViewModel.prestige() }
                )
            }
        }

        if (showSettings) {
            SettingsScreen(
                theme = theme,
                economy = economy,
                soundEnabled = gameViewModel.soundEnabled,
                hapticsEnabled = gameViewModel.hapticsEnabled,
                reducedMotion = gameViewModel.reducedMotion,
                onToggleSound = { gameViewModel.toggleSound() },
                onToggleHaptics = { gameViewModel.toggleHaptics() },
                onToggleReducedMotion = { gameViewModel.toggleReducedMotion() },
                onUpgrade = { gameViewModel.upgradePowerUp(it) },
                onSelectSkin = { gameViewModel.selectSkin(it) },
                onSelectTheme = { gameViewModel.selectTheme(it) },
                onPrestige = { gameViewModel.prestige() },
                onClose = { showSettings = false }
            )
        }

        AdBannerView(Modifier.align(Alignment.BottomCenter))
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
    onStart: () -> Unit,
    onZen: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text("🫧 BUBBLES", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pop bubbles. Beat the clock. Unlock skins.", fontSize = 18.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
            ) {
                Text(
                    "🪙 $coins",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("▶ ADVENTURE", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onZen,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("🧘 ZEN MODE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDaily,
                enabled = !playedToday,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (playedToday) "✅ DAILY DONE — see you tomorrow!"
                    else "📅 DAILY CHALLENGE · Best $dailyBest",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("⚙ SETTINGS & UPGRADES", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "ADVENTURE: clear all bubbles before time runs out · 10 levels\nBosses, bombs, rainbows & frozen bubbles ahead!",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
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
                modifier = Modifier.padding(top = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudCard("SCORE", "$score", MaterialTheme.colorScheme.primary)
                    HudCard("BEST", "$highScore", MaterialTheme.colorScheme.secondary)
                    if (isZen) {
                        HudCard("🧘", "ZEN", MaterialTheme.colorScheme.tertiary)
                    } else {
                        HudCard("LEVEL", "$level", MaterialTheme.colorScheme.tertiary)
                    }
                    HudCard("🪙", "$coins", Gold)
                }

                if (!isZen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${timeRemaining.toInt()}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (timeRemaining < 10f) Color(0xFFE53935) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { (timeRemaining / timeLimit).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp),
                            color = if (timeRemaining < 10f) Color(0xFFE53935) else theme.accent,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }

                if (combo >= 2) {
                    Text(
                        text = "🔥 COMBO x$combo",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                        text = "Breathe... pop whenever you like 🫧",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (bubblesLeft > 0) {
                    Text(
                        text = "Bubbles left: $bubblesLeft — clear them all!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Text(
                        text = "LEVEL CLEAR!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.accent,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (isZen) {
                OutlinedButton(
                    onClick = onEndZen,
                    modifier = Modifier.padding(top = 70.dp, end = 12.dp).height(48.dp)
                ) {
                    Text("DONE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.padding(top = 70.dp, end = 12.dp).height(48.dp)
                ) {
                    Text("⏸", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isPaused) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text("PAUSED", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun HudCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.padding(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Column {
                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            }
        }
    }
}

@Composable
fun PowerUpIndicator(powerUp: PowerUpType) {
    val (label, color) = when (powerUp) {
        PowerUpType.SLOW_MO -> "⏱ SLOW" to MaterialTheme.colorScheme.primary
        PowerUpType.FREEZE -> "❄ FREEZE" to MaterialTheme.colorScheme.secondary
        PowerUpType.MULTI_POP -> "💥 MULTI" to MaterialTheme.colorScheme.tertiary
        PowerUpType.PRISM -> "🌈 PRISM" to Color.Magenta
    }

    Card(
        modifier = Modifier.padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f))
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PAUSED", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("RESUME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("RESTART", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("HOME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
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
    onNext: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 LEVEL $completedLevel CLEAR!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Best: $highScore", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(22.dp))
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("NEXT LEVEL ($nextLevel) ▶", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("HOME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
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
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onPrestige: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    won -> {
                        Text("🏆 YOU WIN! 🎉", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("All 10 levels cleared!", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    zen -> {
                        Text("🧘 ZEN SESSION COMPLETE", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    daily -> {
                        Text("📅 DAILY CHALLENGE DONE!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reward: +${dailyReward} 🪙", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    isNewHighScore -> {
                        Text("🎉 NEW HIGH SCORE! 🎉", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    isTimeUp -> {
                        Text("⏰ TIME'S UP!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pop bubbles to add time", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    else -> {
                        Text("GAME OVER", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Text("Score: $finalScore", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Best: $highScore", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!zen && !daily) {
                    Text("Coins: $coins 🪙", fontSize = 18.sp, color = Gold)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(if (zen) "POP AGAIN" else "PLAY AGAIN", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("HOME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (won && prestigeLevel < 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onPrestige, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("✨ PRESTIGE: unlock GOLD skin + 2x coins", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleReducedMotion: () -> Unit,
    onUpgrade: (String) -> Unit,
    onSelectSkin: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onPrestige: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚙ SETTINGS", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("🪙 ${economy.coins} coins", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                Spacer(modifier = Modifier.height(16.dp))

                SettingRow("🔊 Sound", soundEnabled, onToggleSound)
                SettingRow("📳 Haptics", hapticsEnabled, onToggleHaptics)
                SettingRow("♿ Reduced motion", reducedMotion, onToggleReducedMotion)

                Spacer(modifier = Modifier.height(16.dp))
                Text("⬆ UPGRADES", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                UpgradeRow("⏱ Slow-Mo", "Lv ${economy.slowMoLevel}", "${economy.slowMoDurationMs / 1000}s", economy.slowMoLevel, economy.coins) { onUpgrade("slowMo") }
                UpgradeRow("❄ Freeze", "Lv ${economy.freezeLevel}", "${economy.freezeDurationMs / 1000}s", economy.freezeLevel, economy.coins) { onUpgrade("freeze") }
                UpgradeRow("💥 Multi-Pop", "Lv ${economy.multiPopLevel}", "${economy.multiPopRadius.toInt()}px", economy.multiPopLevel, economy.coins) { onUpgrade("multiPop") }
                UpgradeRow("🌈 Prism", "Lv ${economy.prismLevel}", "x${(economy.prismBoost * 100).toInt()}%", economy.prismLevel, economy.coins) { onUpgrade("prism") }

                Spacer(modifier = Modifier.height(16.dp))
                Text("🎨 SKINS", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
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
                            onClick = { if (!locked) onSelectSkin(s.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("🖼 THEMES", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
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

                Spacer(modifier = Modifier.height(16.dp))
                Text("📊 STATS", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
                StatRow("Lifetime pops", "${economy.lifetimePops}")
                StatRow("Best combo", "x${economy.maxCombo}")
                StatRow("Games played", "${economy.gamesPlayed}")
                StatRow("Prestige", "Lv ${economy.prestigeLevel}")
                StatRow("Daily best", "${economy.dailyBest}")

                if (economy.prestigeLevel < 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onPrestige, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("✨ PRESTIGE (resets coins → GOLD skin + 2x coins)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("CLOSE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingRow(label: String, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
fun UpgradeRow(label: String, level: String, value: String, currentLevel: Int, coins: Int, onUpgrade: () -> Unit) {
    val maxed = currentLevel >= EconomyConfig.UPGRADE_MAX
    val affordable = coins >= EconomyConfig.UPGRADE_COST
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("$level · $value", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = onUpgrade,
            enabled = !maxed && affordable,
            modifier = Modifier.height(40.dp)
        ) {
            Text(
                if (maxed) "MAX" else "${EconomyConfig.UPGRADE_COST} 🪙",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SkinCard(skin: BubbleSkin, locked: Boolean, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(72.dp)
            .clickable(enabled = !locked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) skin.palette.first().copy(alpha = 0.7f) else Color(0xFF222222)
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                        .background(skin.palette.first(), shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (locked) "🔒" else skin.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeCard(themePalette: ThemePalette, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) themePalette.accent.copy(alpha = 0.5f) else Color(0xFF222222)
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                        .background(Brush.verticalGradient(listOf(themePalette.top, themePalette.bottom)), shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = themePalette.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private val BubbleEmojis = listOf("🫧", "✨", "⭐", "🌙", "🍭", "🌈", "⚡", "🍀", "🔥", "💎", "🌸", "🐸")

fun DrawScope.drawBubble(bubble: Bubble, skin: BubbleSkin) {
    val center = Offset(bubble.x, bubble.y)

    drawCircle(color = bubble.color, radius = bubble.radius, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = bubble.radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = bubble.radius * 0.2f,
        center = Offset(bubble.x - bubble.radius * 0.3f, bubble.y - bubble.radius * 0.3f)
    )

    when (bubble.bubbleType) {
        BubbleType.NORMAL -> {
            if (skin.emoji != null) {
                drawEmoji(BubbleEmojis[(bubble.id % BubbleEmojis.size).toInt().mod(BubbleEmojis.size)], center, bubble.radius * 0.7f)
            }
        }
        BubbleType.BOMB -> {
            drawEmoji("💣", center, bubble.radius * 0.9f)
        }
        BubbleType.RAINBOW -> {
            val rainbowColors = listOf(
                Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
                Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)
            )
            val sweep = 60f
            rainbowColors.forEachIndexed { i, c ->
                drawArc(
                    color = c,
                    startAngle = i * sweep,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(bubble.x - bubble.radius, bubble.y - bubble.radius),
                    size = androidx.compose.ui.geometry.Size(bubble.radius * 2, bubble.radius * 2),
                    style = Stroke(width = bubble.radius * 0.12f)
                )
            }
        }
        BubbleType.FROZEN -> {
            drawEmoji(if (bubble.cracked) "🧊" else "❄️", center, bubble.radius * 0.8f)
            if (bubble.cracked) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(bubble.x - bubble.radius * 0.5f, bubble.y - bubble.radius * 0.4f),
                    end = Offset(bubble.x + bubble.radius * 0.2f, bubble.y + bubble.radius * 0.5f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(bubble.x + bubble.radius * 0.4f, bubble.y - bubble.radius * 0.3f),
                    end = Offset(bubble.x - bubble.radius * 0.3f, bubble.y + bubble.radius * 0.3f),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        BubbleType.BOSS -> {
            drawEmoji("👑", center, bubble.radius * 0.8f)
            val pips = bubble.health
            val pipSize = 8.dp.toPx()
            val gap = 4.dp.toPx()
            val total = pips * pipSize + (pips - 1) * gap
            val startX = bubble.x - total / 2f
            val pipY = bubble.y + bubble.radius + 10.dp.toPx()
            for (i in 0 until pips) {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = pipSize / 2f,
                    center = Offset(startX + i * (pipSize + gap) + pipSize / 2f, pipY)
                )
            }
        }
    }

    if (bubble.isPowerUp) {
        val iconSize = bubble.radius * 0.6f
        val symbol = when (bubble.powerUpType) {
            PowerUpType.SLOW_MO -> "⏱"
            PowerUpType.FREEZE -> "❄️"
            PowerUpType.MULTI_POP -> "💥"
            PowerUpType.PRISM -> "🌈"
            null -> ""
        }
        if (symbol.isNotEmpty()) {
            drawEmoji(symbol, center, iconSize)
        }
    }
}

fun DrawScope.drawEmoji(emoji: String, center: Offset, size: Float) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = size
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        emoji,
        center.x,
        center.y + size * 0.35f,
        paint
    )
}

@Composable
fun PopMessageView(msg: PopMessage) {
    Text(
        text = msg.text,
        color = msg.color.copy(alpha = msg.alpha),
        fontSize = (msg.fontSize * msg.scale).sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = (msg.x - 50).dp, y = msg.y.dp)
            .rotate(msg.rotation)
    )
}

fun DrawScope.drawParticle(particle: Particle, reducedMotion: Boolean) {
    if (reducedMotion && particle.alpha > 0.4f) return
    val alpha = if (reducedMotion) particle.alpha * 0.5f else particle.alpha
    if (particle.isRing) {
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = particle.radius,
            center = Offset(particle.x, particle.y),
            style = Stroke(width = 4.dp.toPx())
        )
    } else {
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = particle.radius,
            center = Offset(particle.x, particle.y)
        )
    }
}