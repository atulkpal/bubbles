package com.ashwathai.bubbles.domain.model

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

enum class BubbleType {
    NORMAL,
    BOMB,
    RAINBOW,
    FROZEN,
    BOSS,
    MAGNET,
    TICKING_BOMB,
    CHAOS,
    GHOST
}

data class Bubble(
    val id: Long,
    var x: Float,
    var y: Float,
    var radius: Float,
    val color: Color,
    var vx: Float,
    var vy: Float,
    val level: Int,
    val isPowerUp: Boolean = false,
    val powerUpType: PowerUpType? = null,
    val bubbleType: BubbleType = BubbleType.NORMAL,
    var health: Int = 1,
    var cracked: Boolean = false
)

data class PopMessage(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    var alpha: Float = 1f,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    val color: Color = Color.White,
    val fontSize: Int = 26
)

data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color,
    var alpha: Float = 1f,
    var life: Float = 1f,
    val isRing: Boolean = false
)

enum class PowerUpType {
    SLOW_MO,
    FREEZE,
    MULTI_POP,
    PRISM
}

data class LevelConfig(
    val level: Int,
    val maxBubbles: Int,
    val spawnInterval: Float,
    val baseSpeed: Float,
    val powerUpChance: Float,
    val hasBoss: Boolean = false,
    val wind: Float = 0f,
    val gravity: Float = 0f,
    val timeLimit: Float = 60f
)

sealed class GameState {
    data class Playing(
        val score: Int,
        val highScore: Int,
        val currentLevel: Int,
        val bubblesPoppedThisLevel: Int,
        val isZen: Boolean = false,
        val isDaily: Boolean = false
    ) : GameState()

    data class Paused(
        val score: Int,
        val highScore: Int,
        val currentLevel: Int,
        val isZen: Boolean = false,
        val isDaily: Boolean = false
    ) : GameState()

    data class GameOver(
        val finalScore: Int,
        val highScore: Int,
        val isNewHighScore: Boolean,
        val isTimeUp: Boolean = false,
        val won: Boolean = false,
        val zen: Boolean = false,
        val daily: Boolean = false,
        val dailyReward: Int = 0
    ) : GameState()

    data class LevelComplete(
        val score: Int,
        val highScore: Int,
        val completedLevel: Int,
        val nextLevel: Int,
        val waitingForAd: Boolean = false
    ) : GameState()
    object Ready : GameState()
}

fun generateProceduralLevel(level: Int): LevelConfig {
    // Difficulty scales logarithmically — each stage is harder but the curve flattens
    val logScale = kotlin.math.ln(level.toFloat() + 1f) / kotlin.math.ln(101f)
    val linearScale = (level - 20).toFloat() / 80f // 0..1 from level 21 to 100
    val difficulty = (logScale + linearScale) / 2f // Blend both

    // Stage bands — introduce new pressures every 10 levels
    val stage = (level - 1) / 10
    val stageInBand = (level - 1) % 10

    // Max bubbles: grows from 26 to ~55 over levels 21-100
    val maxBubbles = (26 + (level - 20) * 0.35f).toInt().coerceIn(26, 55)

    // Spawn interval: decreases from 0.25s to 0.08s
    val spawnInterval = kotlin.math.max(0.08f, 0.25f - (level - 20) * 0.0018f)

    // Base speed: grows from 980 to ~1400
    val baseSpeed = (980f + (level - 20) * 5.2f).coerceIn(980f, 1400f)

    // Power-up chance grows with difficulty — at high levels, chaos is the norm
    val powerUpChance = (0.15f + difficulty * 0.20f).coerceIn(0.15f, 0.35f)

    // Boss every 3 levels starting at 21 (21, 24, 27, ...)
    val hasBoss = (level - 21) % 3 == 0 && level >= 21

    // Wind: appears at stage 2+ (level 21+), oscillates by level parity
    val windBase = if (stage >= 2) (stage - 2) * 15f + stageInBand * 3f else 0f
    val wind = if (windBase > 0) (if (level % 2 == 0) -1f else 1f) * windBase else 0f

    // Gravity: appears at stage 3+ (level 31+), grows steadily
    val gravity = if (stage >= 3) (stage - 3) * 20f + stageInBand * 2f else 0f

    // Time limit: shrinks from 19s to 8s over the range
    val timeLimit = kotlin.math.max(8f, 19f - (level - 20) * 0.11f)

    return LevelConfig(
        level = level,
        maxBubbles = maxBubbles,
        spawnInterval = spawnInterval,
        baseSpeed = baseSpeed,
        powerUpChance = powerUpChance,
        hasBoss = hasBoss,
        wind = wind,
        gravity = gravity,
        timeLimit = timeLimit
    )
}

data class GameConfig(
    val minRadius: Float = 40f,
    val radiusStep: Float = 30f,
    val maxBubbleLevel: Int = 3,
    val popTexts: List<String> = listOf("Pop!", "Splendid!", "Awesome!", "Boom!", "Nice!", "Bubble!", "Magic!", "Splosh!", "Glorious!", "Luminous!", "Serene!", "Beautiful!"),
    val particleCount: Int = 12,
    val levels: List<LevelConfig> = buildList {
        // ── Curated levels 1-20 ──
        add(LevelConfig(1, 6, 2.0f, 300f, 0.03f, timeLimit = 60f))
        add(LevelConfig(2, 7, 1.8f, 330f, 0.04f, timeLimit = 55f))
        add(LevelConfig(3, 8, 1.5f, 360f, 0.05f, hasBoss = true, timeLimit = 50f))
        add(LevelConfig(4, 9, 1.3f, 400f, 0.05f, wind = 20f, timeLimit = 45f))
        add(LevelConfig(5, 10, 1.2f, 440f, 0.05f, gravity = 30f, hasBoss = true, timeLimit = 40f))
        add(LevelConfig(6, 11, 1.0f, 480f, 0.06f, wind = 30f, gravity = 20f, timeLimit = 38f))
        add(LevelConfig(7, 12, 0.9f, 520f, 0.07f, hasBoss = true, timeLimit = 36f))
        add(LevelConfig(8, 13, 0.8f, 560f, 0.08f, wind = 40f, gravity = 30f, timeLimit = 34f))
        add(LevelConfig(9, 14, 0.7f, 600f, 0.08f, timeLimit = 32f))
        add(LevelConfig(10, 15, 0.6f, 650f, 0.09f, hasBoss = true, wind = 50f, gravity = 40f, timeLimit = 30f))
        add(LevelConfig(11, 16, 0.55f, 680f, 0.10f, gravity = 50f, hasBoss = true, wind = 60f, timeLimit = 28f))
        add(LevelConfig(12, 17, 0.50f, 710f, 0.10f, gravity = 55f, wind = 65f, timeLimit = 27f))
        add(LevelConfig(13, 18, 0.45f, 740f, 0.11f, gravity = 60f, hasBoss = true, wind = 70f, timeLimit = 26f))
        add(LevelConfig(14, 19, 0.42f, 770f, 0.11f, gravity = 65f, wind = 75f, timeLimit = 25f))
        add(LevelConfig(15, 20, 0.38f, 800f, 0.12f, gravity = 70f, hasBoss = true, wind = 80f, timeLimit = 24f))
        add(LevelConfig(16, 21, 0.35f, 830f, 0.12f, gravity = 75f, wind = 85f, timeLimit = 23f))
        add(LevelConfig(17, 22, 0.32f, 860f, 0.13f, gravity = 80f, hasBoss = true, wind = 90f, timeLimit = 22f))
        add(LevelConfig(18, 23, 0.30f, 890f, 0.13f, gravity = 85f, wind = 95f, timeLimit = 21f))
        add(LevelConfig(19, 24, 0.28f, 920f, 0.14f, gravity = 90f, hasBoss = true, wind = 100f, timeLimit = 20f))
        add(LevelConfig(20, 25, 0.26f, 950f, 0.14f, gravity = 95f, wind = 105f, timeLimit = 19f))
        // ── Procedural levels 21-100 ──
        for (level in 21..100) {
            add(generateProceduralLevel(level))
        }
    }
)

data class ThemePalette(
    val name: String,
    val top: Color,
    val bottom: Color,
    val accent: Color
)

object BubbleThemes {
    val OCEAN = ThemePalette("Ocean", Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF00E5FF))
    val SUNSET = ThemePalette("Sunset", Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFA07A))
    val FOREST = ThemePalette("Forest", Color(0xFF134E5E), Color(0xFF71B280), Color(0xFF7CFC00))
    val NEON = ThemePalette("Neon", Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFFFF00FF))
    val PASTEL = ThemePalette("Pastel", Color(0xFF2A4B7C), Color(0xFFA8E6CF), Color(0xFFFF8BA0))
    val MONO = ThemePalette("Mono", Color(0xFF2C3E50), Color(0xFF4CA1AF), Color(0xFFBDC3C7))

    val all = listOf(OCEAN, SUNSET, FOREST, NEON, PASTEL, MONO)

    fun fromName(name: String): ThemePalette {
        return all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OCEAN
    }
}

data class BubbleSkin(
    val name: String,
    val palette: List<Color>,
    val emoji: String? = null,
    val needsPrestige: Boolean = false
)

object BubbleSkins {
    val CLASSIC = BubbleSkin("Classic", listOf(
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF3949AB),
        Color(0xFF1E88E5), Color(0xFF00897B), Color(0xFF43A047),
        Color(0xFFFDD835), Color(0xFFFB8C00), Color(0xFFD81B60)
    ))

    val NEON = BubbleSkin("Neon", listOf(
        Color(0xFFFF005C), Color(0xFFFF9E00), Color(0xFF00E676),
        Color(0xFF00B0FF), Color(0xFF651FFF), Color(0xFFF50057),
        Color(0xFFFFD600), Color(0xFF00E5FF), Color(0xFF76FF03)
    ))

    val PASTEL = BubbleSkin("Pastel", listOf(
        Color(0xFFFFB3BA), Color(0xFFFFDFBA), Color(0xFFFFFAAE),
        Color(0xFFBAFFC9), Color(0xFFBAE1FF), Color(0xFFE8BBE0),
        Color(0xFFE0BBE4), Color(0xFFFEC8D8), Color(0xFFA2E1DB)
    ))

    val GALAXY = BubbleSkin("Galaxy", listOf(
        Color(0xFF6A00FF), Color(0xFF8E2DE2), Color(0xFF4A00E0),
        Color(0xFF355C7D), Color(0xFF6C5B7B), Color(0xFFC06CDA),
        Color(0xFF9B59B6), Color(0xFF16A085), Color(0xFF3A1C71)
    ))

    val EMOJI = BubbleSkin("Emoji", listOf(
        Color(0xFFFFD700), Color(0xFFFF6F61), Color(0xFF6C5CE7),
        Color(0xFF00CEC9), Color(0xFFFDCB6E), Color(0xFFFD79A8),
        Color(0xFF55EFC4), Color(0xFF74B9FF), Color(0xFFFF7675)
    ), emoji = "X")

    val GOLD = BubbleSkin("Gold", listOf(
        Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFB300),
        Color(0xFFFF8F00), Color(0xFFFFA500), Color(0xFFFFB900),
        Color(0xFFFFCC00), Color(0xFFFFDF00), Color(0xFFFFECB3)
    ), needsPrestige = true)

    val all = listOf(CLASSIC, NEON, PASTEL, GALAXY, EMOJI, GOLD)

    fun fromName(name: String): BubbleSkin {
        return all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CLASSIC
    }
}

object EconomyConfig {
    const val COINS_PER_POP = 1
    const val CHAIN_BONUS = 2
    const val COMBO_WINDOW_MS = 700L
    const val UPGRADE_COST = 100
    const val UPGRADE_MAX = 5
    const val CONTINUE_COST = 50        // coins to continue after game over (+15s)
    const val CONTINUE_TIME_BONUS = 15f  // seconds added on continue
    const val DAILY_REWARD = 100
    const val DAILY_TIME_LIMIT = 60f
    const val ZEN_MAX_BUBBLES = 18
    const val ZEN_SPAWN_INTERVAL = 2.5f

    // ── Milestone Rewards (coins awarded on level-up) ──
    const val MILESTONE_INTERVAL_1_10 = 3   // ad every 3rd level (3, 6, 9)
    const val MILESTONE_INTERVAL_11_20 = 2  // ad every 2nd level (11, 13, 15, 17, 19)
    const val MILESTONE_INTERVAL_21_PLUS = 1 // ad on every level (21+)
    const val MILESTONE_COINS_BASE = 25     // base coins at each milestone
    const val MILESTONE_COINS_PER_LEVEL = 10 // extra coins per level number beyond 1

    fun milestoneRewardForLevel(level: Int): Int {
        return MILESTONE_COINS_BASE + (level - 1) * MILESTONE_COINS_PER_LEVEL
    }

    fun shouldShowAdBetweenLevels(completedLevel: Int): Boolean {
        return when {
            completedLevel in 1..10 -> completedLevel % MILESTONE_INTERVAL_1_10 == 0
            completedLevel in 11..20 -> (completedLevel - 10) % MILESTONE_INTERVAL_11_20 == 1 || completedLevel == 11
            else -> (completedLevel - 20) % MILESTONE_INTERVAL_21_PLUS == 1 || completedLevel == 21
        }
    }
}

data class EconomyState(
    val coins: Int = 0,
    val slowMoLevel: Int = 1,
    val freezeLevel: Int = 1,
    val multiPopLevel: Int = 1,
    val prismLevel: Int = 1,
    val skin: String = BubbleSkins.CLASSIC.name,
    val theme: String = BubbleThemes.OCEAN.name,
    val lifetimePops: Long = 0,
    val maxCombo: Int = 0,
    val gamesPlayed: Int = 0,
    val prestigeLevel: Int = 0,
    val dailyDate: String = "",
    val dailyBest: Int = 0
) {
    val slowMoDurationMs: Long get() = 5000L + (slowMoLevel - 1) * 1500L
    val freezeDurationMs: Long get() = 3000L + (freezeLevel - 1) * 800L
    val multiPopRadius: Float get() = 150f + (multiPopLevel - 1) * 30f
    val prismBoost: Float get() = 1f + (prismLevel - 1) * 0.25f
    val coinMultiplier: Int get() = if (prestigeLevel >= 1) 2 else 1
}