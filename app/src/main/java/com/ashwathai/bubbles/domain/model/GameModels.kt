package com.ashwathai.bubbles.domain.model

import androidx.compose.ui.graphics.Color

enum class BubbleType {
    NORMAL,
    BOMB,
    RAINBOW,
    FROZEN,
    BOSS
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

    data class LevelComplete(val score: Int, val highScore: Int, val completedLevel: Int, val nextLevel: Int) : GameState()
    object Ready : GameState()
}

data class GameConfig(
    val minRadius: Float = 40f,
    val radiusStep: Float = 30f,
    val maxBubbleLevel: Int = 3,
    val popTexts: List<String> = listOf("Pop!", "Splendid!", "Awesome!", "Boom!", "Nice!", "Bubble!", "Magic!", "Splosh!", "Glorious!", "Luminous!", "Serene!", "Beautiful!"),
    val particleCount: Int = 12,
    val levels: List<LevelConfig> = listOf(
        LevelConfig(1, 6, 2.0f, 300f, 0.03f, timeLimit = 60f),
        LevelConfig(2, 7, 1.8f, 330f, 0.04f, timeLimit = 55f),
        LevelConfig(3, 8, 1.5f, 360f, 0.05f, hasBoss = true, timeLimit = 50f),
        LevelConfig(4, 9, 1.3f, 400f, 0.05f, wind = 20f, timeLimit = 45f),
        LevelConfig(5, 10, 1.2f, 440f, 0.05f, gravity = 30f, hasBoss = true, timeLimit = 40f),
        LevelConfig(6, 11, 1.0f, 480f, 0.06f, wind = 30f, gravity = 20f, timeLimit = 38f),
        LevelConfig(7, 12, 0.9f, 520f, 0.07f, hasBoss = true, timeLimit = 36f),
        LevelConfig(8, 13, 0.8f, 560f, 0.08f, wind = 40f, gravity = 30f, timeLimit = 34f),
        LevelConfig(9, 14, 0.7f, 600f, 0.08f, timeLimit = 32f),
        LevelConfig(10, 15, 0.6f, 650f, 0.09f, hasBoss = true, wind = 50f, gravity = 40f, timeLimit = 30f)
    )
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
    const val DAILY_REWARD = 100
    const val DAILY_TIME_LIMIT = 60f
    const val ZEN_MAX_BUBBLES = 18
    const val ZEN_SPAWN_INTERVAL = 2.5f
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