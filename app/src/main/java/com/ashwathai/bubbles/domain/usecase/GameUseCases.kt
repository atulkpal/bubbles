package com.ashwathai.bubbles.domain.usecase

import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.BubbleType
import com.ashwathai.bubbles.domain.model.GameConfig
import com.ashwathai.bubbles.domain.model.LevelConfig
import com.ashwathai.bubbles.domain.model.Particle
import com.ashwathai.bubbles.domain.model.PopMessage
import com.ashwathai.bubbles.domain.model.PowerUpType
import androidx.compose.ui.graphics.Color
import com.ashwathai.bubbles.ui.theme.Gold
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val BOMB_BLAST_RADIUS = 180f
private val BOMB_COLOR = Color(0xFF333333)
private val RAINBOW_COLOR = Color(0xFFFFFFFF)
private val FROZEN_COLOR = Color(0xFF81D4FA)
private val BOSS_COLOR = Color(0xFF4A148C)

class SpawnBubblesUseCase {

    operator fun invoke(
        config: GameConfig,
        levelConfig: LevelConfig,
        width: Float,
        height: Float,
        existingBubbles: List<Bubble>,
        palette: List<Color>,
        specialChance: Float = 0.12f,
        prismBoost: Float = 1f,
        random: Random = Random.Default
    ): List<Bubble> {
        val needed = levelConfig.maxBubbles - existingBubbles.size
        if (needed <= 0 || width <= 0 || height <= 0) return emptyList()

        val newBubbles = mutableListOf<Bubble>()

        if (levelConfig.hasBoss && existingBubbles.none { it.bubbleType == BubbleType.BOSS }) {
            newBubbles.add(spawnBoss(config, levelConfig, width, height, random))
        }

        repeat(min(needed, 2)) {
            newBubbles.add(spawnLargeBubble(config, levelConfig, width, height, palette, specialChance, prismBoost, random))
        }
        return newBubbles
    }

    private fun spawnBoss(config: GameConfig, levelConfig: LevelConfig, width: Float, height: Float, random: Random): Bubble {
        val radius = config.minRadius * 2.6f
        val side = random.nextInt(4)
        var x = 0f
        var y = 0f
        when (side) {
            0 -> { x = -radius; y = -radius }
            1 -> { x = width + radius; y = -radius }
            2 -> { x = -radius; y = height + radius }
            3 -> { x = width + radius; y = height + radius }
        }
        val targetX = width * 0.4f + random.nextFloat() * width * 0.2f
        val targetY = height * 0.4f + random.nextFloat() * height * 0.2f
        val dx = targetX - x
        val dy = targetY - y
        val dist = sqrt(dx * dx + dy * dy)
        val speed = levelConfig.baseSpeed * 0.4f
        return Bubble(
            id = System.nanoTime() + random.nextLong(),
            x = x,
            y = y,
            radius = radius,
            color = BOSS_COLOR,
            vx = if (dist > 0) (dx / dist) * speed else 0f,
            vy = if (dist > 0) (dy / dist) * speed else 0f,
            level = 3,
            bubbleType = BubbleType.BOSS,
            health = 5
        )
    }

    private fun spawnLargeBubble(
        config: GameConfig,
        levelConfig: LevelConfig,
        width: Float,
        height: Float,
        palette: List<Color>,
        specialChance: Float,
        prismBoost: Float,
        random: Random
    ): Bubble {
        val level = random.nextInt(config.maxBubbleLevel - 1) + 2
        val radius = config.minRadius + (level * config.radiusStep)
        val side = random.nextInt(4)
        var x = 0f
        var y = 0f
        when (side) {
            0 -> { x = -radius; y = -radius }
            1 -> { x = width + radius; y = -radius }
            2 -> { x = -radius; y = height + radius }
            3 -> { x = width + radius; y = height + radius }
        }

        val targetX = width * 0.2f + random.nextFloat() * width * 0.6f
        val targetY = height * 0.2f + random.nextFloat() * height * 0.6f
        val dx = targetX - x
        val dy = targetY - y
        val dist = sqrt(dx * dx + dy * dy)

        val isPowerUp = random.nextFloat() < levelConfig.powerUpChance
        val powerUpType = if (isPowerUp) PowerUpType.values().random(random) else null

        val hue = random.nextFloat()

        val specialRoll = random.nextFloat()
        val rainbowLimit = specialChance * (0.6f + 0.4f * min(1f, prismBoost))
        val bubbleType = when {
            specialRoll < specialChance * 0.35f -> BubbleType.BOMB
            specialRoll < specialChance * 0.6f -> BubbleType.FROZEN
            specialRoll < rainbowLimit -> BubbleType.RAINBOW
            else -> BubbleType.NORMAL
        }

        val color = when {
            isPowerUp -> Gold
            bubbleType == BubbleType.BOMB -> BOMB_COLOR
            bubbleType == BubbleType.RAINBOW -> RAINBOW_COLOR
            bubbleType == BubbleType.FROZEN -> FROZEN_COLOR
            else -> palette[(hue * palette.size).toInt().mod(palette.size)]
        }

        val speed = levelConfig.baseSpeed * (0.7f + random.nextFloat() * 0.6f)

        return Bubble(
            id = System.nanoTime() + random.nextLong(),
            x = x,
            y = y,
            radius = radius,
            color = color,
            vx = if (dist > 0) (dx / dist) * speed else 0f,
            vy = if (dist > 0) (dy / dist) * speed else 0f,
            level = level,
            isPowerUp = isPowerUp,
            powerUpType = powerUpType,
            bubbleType = bubbleType,
            health = if (bubbleType == BubbleType.FROZEN) 2 else 1
        )
    }
}

class UpdateBubblesUseCase {

    operator fun invoke(
        bubbles: List<Bubble>,
        dt: Float,
        width: Float,
        height: Float,
        levelConfig: LevelConfig,
        isFrozen: Boolean,
        slowMoFactor: Float
    ): List<Bubble> {
        if (isFrozen) return bubbles

        val effectiveDt = dt * slowMoFactor

        return bubbles.map { bubble ->
            var newX = bubble.x + bubble.vx * effectiveDt
            var newY = bubble.y + bubble.vy * effectiveDt
            var newVx = bubble.vx
            var newVy = bubble.vy

            if (levelConfig.wind != 0f) {
                newVx += levelConfig.wind * effectiveDt
            }

            if (levelConfig.gravity != 0f) {
                newVy += levelConfig.gravity * effectiveDt
            }

            val radius = bubble.radius
            if (newX - radius < 0) {
                newX = radius
                newVx = abs(newVx) * 0.8f
            } else if (newX + radius > width) {
                newX = width - radius
                newVx = -abs(newVx) * 0.8f
            }

            if (newY - radius < 0) {
                newY = radius
                newVy = abs(newVy) * 0.8f
            } else if (newY + radius > height) {
                newY = height - radius
                newVy = -abs(newVy) * 0.8f
            }

            bubble.copy(x = newX, y = newY, vx = newVx, vy = newVy)
        }.filter { it.x > -200 && it.x < width + 200 && it.y > -200 && it.y < height + 200 }
    }
}

data class TapResult(
    val newBubbles: List<Bubble>,
    val newMessages: List<PopMessage>,
    val newParticles: List<Particle>,
    val scoreGain: Int,
    val powerUpCollected: PowerUpType?,
    val bubblesPopped: Int,
    val isBigPop: Boolean
)

class HandleTapUseCase {

    operator fun invoke(
        bubbles: List<Bubble>,
        tapX: Float,
        tapY: Float,
        config: GameConfig,
        levelConfig: LevelConfig,
        palette: List<Color>,
        multiPopRadius: Float = 0f,
        random: Random = Random.Default
    ): TapResult {
        val hitIndex = bubbles.indexOfFirst { b ->
            sqrt((tapX - b.x) * (tapX - b.x) + (tapY - b.y) * (tapY - b.y)) < b.radius
        }

        if (hitIndex == -1) {
            return TapResult(bubbles, emptyList(), emptyList(), 0, null, 0, false)
        }

        val hitBubble = bubbles[hitIndex]

        // Multi-pop power-up: destroy every bubble within radius of the tap
        if (multiPopRadius > 0f) {
            val toDestroy = bubbles.filter {
                sqrt((tapX - it.x) * (tapX - it.x) + (tapY - it.y) * (tapY - it.y)) < multiPopRadius
            }.toMutableSet()
            return destroySet(toDestroy, bubbles, config, levelConfig, palette, random, big = true)
        }

        // Frozen bubble cracks instead of popping
        if (hitBubble.bubbleType == BubbleType.FROZEN && hitBubble.health > 1) {
            val updated = bubbles.mapIndexed { i, b ->
                if (i == hitIndex) b.copy(health = b.health - 1, cracked = true) else b
            }
            val msg = PopMessage(
                id = System.nanoTime() + 1,
                text = "❄ CRACK!",
                x = hitBubble.x,
                y = hitBubble.y,
                rotation = 0f,
                scale = 0.8f,
                color = FROZEN_COLOR
            )
            val particles = crackParticles(hitBubble, random)
            return TapResult(updated, listOf(msg), particles, 1, null, 0, false)
        }

        // Boss takes damage per tap
        if (hitBubble.bubbleType == BubbleType.BOSS) {
            return handleBossHit(hitBubble, hitIndex, bubbles, config, levelConfig, random)
        }

        // BOMB, RAINBOW, NORMAL: expand a destructive chain
        val toDestroy = LinkedHashSet<Bubble>()
        val queue = ArrayDeque<Bubble>()
        queue.add(hitBubble)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!toDestroy.add(current)) continue
            for (other in bubbles) {
                if (other in toDestroy) continue
                if (other.bubbleType == BubbleType.BOSS) continue
                val overlap = sqrt(
                    (current.x - other.x) * (current.x - other.x) +
                    (current.y - other.y) * (current.y - other.y)
                ) < (current.radius + other.radius) * 1.1f
                val chainable = when {
                    current.bubbleType == BubbleType.BOMB ->
                        sqrt((current.x - other.x) * (current.x - other.x) + (current.y - other.y) * (current.y - other.y)) < BOMB_BLAST_RADIUS
                    current.bubbleType == BubbleType.RAINBOW || other.bubbleType == BubbleType.RAINBOW -> overlap
                    else -> overlap && current.color == other.color
                }
                if (chainable) queue.add(other)
            }
        }

        val big = hitBubble.bubbleType == BubbleType.BOMB || hitBubble.level >= 3 || toDestroy.size >= 4
        return destroySet(toDestroy, bubbles, config, levelConfig, palette, random, big = big)
    }

    private fun handleBossHit(
        hitBubble: Bubble,
        hitIndex: Int,
        bubbles: List<Bubble>,
        config: GameConfig,
        levelConfig: LevelConfig,
        random: Random
    ): TapResult {
        val newHealth = hitBubble.health - 1
        if (newHealth > 0) {
            val updated = bubbles.mapIndexed { i, b ->
                if (i == hitIndex) b.copy(health = newHealth) else b
            }
            val minions = (0 until 2).map { spawnMinion(hitBubble, config, random) }
            val msg = PopMessage(
                id = System.nanoTime() + 1,
                text = "BOSS $newHealth",
                x = hitBubble.x,
                y = hitBubble.y - hitBubble.radius,
                rotation = 0f,
                scale = 0.9f,
                color = Color(0xFFE1BEE7),
                fontSize = 20
            )
            return TapResult(updated + minions, listOf(msg), smallHitParticles(hitBubble, random), 25, null, 1, false)
        }

        // Boss destroyed: big explosion, destroys everything in blast radius
        val toDestroy = bubbles.filter {
            sqrt((hitBubble.x - it.x) * (hitBubble.x - it.x) + (hitBubble.y - it.y) * (hitBubble.y - it.y)) < hitBubble.radius * 2.5f
        }.toMutableSet()
        toDestroy.add(hitBubble)
        return destroySet(toDestroy, bubbles, config, levelConfig, palette = null, random = random, big = true, bossDown = true, boss = hitBubble)
    }

    private fun spawnMinion(boss: Bubble, config: GameConfig, random: Random): Bubble {
        val radius = config.minRadius
        val angle = random.nextFloat() * 2 * 3.14159f
        val speed = 120f + random.nextFloat() * 80f
        return Bubble(
            id = System.nanoTime() + random.nextLong(),
            x = boss.x,
            y = boss.y,
            radius = radius,
            color = Color(0xFF9575CD),
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            level = 1,
            bubbleType = BubbleType.NORMAL
        )
    }

    private fun destroySet(
        toDestroy: MutableSet<Bubble>,
        bubbles: List<Bubble>,
        config: GameConfig,
        levelConfig: LevelConfig,
        palette: List<Color>?,
        random: Random,
        big: Boolean,
        bossDown: Boolean = false,
        boss: Bubble? = null
    ): TapResult {
        val remaining = bubbles.filterNot { it in toDestroy }
        val newBubbles = mutableListOf<Bubble>()
        val particles = mutableListOf<Particle>()
        var scoreGain = 0
        var powerUpCollected: PowerUpType? = null
        var chainCount = 0

        toDestroy.forEach { b ->
            when (b.bubbleType) {
                BubbleType.BOMB -> scoreGain += 20
                BubbleType.RAINBOW -> scoreGain += 30
                else -> {}
            }
            if (b.isPowerUp) {
                powerUpCollected = b.powerUpType
                scoreGain += 50
            } else if (b.bubbleType != BubbleType.BOMB && b.bubbleType != BubbleType.RAINBOW) {
                scoreGain += (b.level + 1) * 10
            }

            if (b.bubbleType == BubbleType.RAINBOW) {
                particles.addAll(sparkleParticles(b, config, random))
            } else {
                particles.addAll(createParticles(b, config, random))
            }

            if (b.level > 0 && b.bubbleType == BubbleType.NORMAL && !b.isPowerUp) {
                val newLevel = b.level - 1
                val newRadius = config.minRadius + (newLevel * config.radiusStep)
                val angle = random.nextFloat() * 2 * 3.14159f
                val speed = 150f * (1f + random.nextFloat() * 0.5f)

                newBubbles.add(Bubble(
                    id = System.nanoTime() + 2,
                    x = b.x,
                    y = b.y,
                    radius = newRadius,
                    color = b.color,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    level = newLevel
                ))
                newBubbles.add(Bubble(
                    id = System.nanoTime() + 3,
                    x = b.x,
                    y = b.y,
                    radius = newRadius,
                    color = b.color,
                    vx = cos(angle + 3.14159f) * speed,
                    vy = sin(angle + 3.14159f) * speed,
                    level = newLevel
                ))
            }

            if (b.bubbleType == BubbleType.BOMB) {
                particles.addAll(explosionParticles(b, random))
            }
            chainCount++
        }

        val messages = mutableListOf<PopMessage>()
        val poppedSize = toDestroy.size

        when {
            bossDown -> {
                messages.add(PopMessage(
                    id = System.nanoTime() + 5,
                    text = "👑 BOSS DOWN! +$scoreGain",
                    x = boss!!.x,
                    y = boss.y,
                    rotation = 0f,
                    scale = 1.2f,
                    color = Gold
                ))
                scoreGain += 100
            }
            poppedSize >= 4 -> {
                messages.add(PopMessage(
                    id = System.nanoTime() + 5,
                    text = "🔥 CHAIN x$poppedSize! +$scoreGain",
                    x = bubbles.first().x,
                    y = bubbles.first().y,
                    rotation = 0f,
                    scale = 1.1f,
                    color = Gold,
                    fontSize = 30
                ))
            }
            else -> {
                messages.add(PopMessage(
                    id = System.nanoTime() + 5,
                    text = config.popTexts.random(random),
                    x = bubbles.first().x,
                    y = bubbles.first().y,
                    rotation = (random.nextFloat() - 0.5f) * 0.5f,
                    scale = 0.8f
                ))
            }
        }

        return TapResult(
            newBubbles = remaining + newBubbles,
            newMessages = messages,
            newParticles = particles,
            scoreGain = scoreGain,
            powerUpCollected = powerUpCollected,
            bubblesPopped = chainCount,
            isBigPop = big
        )
    }

    private fun createParticles(bubble: Bubble, config: GameConfig, random: Random): List<Particle> {
        val particles = mutableListOf<Particle>()
        val count = config.particleCount
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * 3.14159f
            val speed = 100f + random.nextFloat() * 200f
            val radius = (2f + random.nextFloat() * 4f) * (bubble.radius / config.minRadius)
            particles.add(Particle(
                id = System.nanoTime() + i.toLong(),
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = radius,
                color = bubble.color,
                alpha = 1f,
                life = 1f
            ))
        }
        particles.add(Particle(
            id = System.nanoTime() + 999,
            x = bubble.x,
            y = bubble.y,
            vx = 0f,
            vy = 0f,
            radius = bubble.radius * 0.8f,
            color = bubble.color,
            alpha = 0.8f,
            life = 0.5f,
            isRing = true
        ))
        return particles
    }

    private fun crackParticles(bubble: Bubble, random: Random): List<Particle> {
        val particles = mutableListOf<Particle>()
        for (i in 0 until 6) {
            val angle = random.nextFloat() * 2 * 3.14159f
            val speed = 60f + random.nextFloat() * 100f
            particles.add(Particle(
                id = System.nanoTime() + i.toLong(),
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = 2f + random.nextFloat() * 3f,
                color = Color(0xFFE1F5FE),
                alpha = 0.9f,
                life = 0.6f
            ))
        }
        return particles
    }

    private fun smallHitParticles(bubble: Bubble, random: Random): List<Particle> {
        val particles = mutableListOf<Particle>()
        for (i in 0 until 8) {
            val angle = random.nextFloat() * 2 * 3.14159f
            val speed = 80f + random.nextFloat() * 120f
            particles.add(Particle(
                id = System.nanoTime() + i.toLong(),
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = 2f + random.nextFloat() * 3f,
                color = Color(0xFFE1BEE7),
                alpha = 0.9f,
                life = 0.6f
            ))
        }
        return particles
    }

    private fun explosionParticles(bubble: Bubble, random: Random): List<Particle> {
        val particles = mutableListOf<Particle>()
        for (i in 0 until 24) {
            val angle = random.nextFloat() * 2 * 3.14159f
            val speed = 150f + random.nextFloat() * 350f
            particles.add(Particle(
                id = System.nanoTime() + 50 + i.toLong(),
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = 3f + random.nextFloat() * 4f,
                color = if (i % 2 == 0) Color(0xFFFFD54F) else Color(0xFFFF7043),
                alpha = 1f,
                life = 0.9f
            ))
        }
        particles.add(Particle(
            id = System.nanoTime() + 200,
            x = bubble.x,
            y = bubble.y,
            vx = 0f,
            vy = 0f,
            radius = 100f,
            color = Color(0xFFFFF3E0),
            alpha = 0.9f,
            life = 0.5f,
            isRing = true
        ))
        return particles
    }

    private fun sparkleParticles(bubble: Bubble, config: GameConfig, random: Random): List<Particle> {
        val particles = mutableListOf<Particle>()
        for (i in 0 until 20) {
            val angle = random.nextFloat() * 2 * 3.14159f
            val speed = 120f + random.nextFloat() * 260f
            particles.add(Particle(
                id = System.nanoTime() + i.toLong(),
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = 2f + random.nextFloat() * 3f,
                color = RainbowPalette[i % RainbowPalette.size],
                alpha = 1f,
                life = 0.8f
            ))
        }
        particles.add(Particle(
            id = System.nanoTime() + 999,
            x = bubble.x,
            y = bubble.y,
            vx = 0f,
            vy = 0f,
            radius = bubble.radius,
            color = Color.White,
            alpha = 0.9f,
            life = 0.6f,
            isRing = true
        ))
        return particles
    }

    private companion object {
        val RainbowPalette = listOf(
            Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
            Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)
        )
    }
}

class UpdateMessagesUseCase {
    operator fun invoke(messages: List<PopMessage>, dt: Float): List<PopMessage> {
        return messages.map { msg ->
            msg.copy(
                y = msg.y - 80f * dt,
                alpha = msg.alpha - 1.2f * dt,
                scale = msg.scale + 0.3f * dt
            )
        }.filter { it.alpha > 0 }
    }
}

class UpdateParticlesUseCase {
    operator fun invoke(particles: List<Particle>, dt: Float): List<Particle> {
        return particles.map { p ->
            if (p.isRing) {
                p.copy(
                    radius = p.radius + 400f * dt,
                    alpha = p.alpha - 1.8f * dt,
                    life = p.life - dt
                )
            } else {
                val gravity = 200f
                p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + p.vy * dt,
                    vy = p.vy + gravity * dt,
                    alpha = p.alpha - 1.5f * dt,
                    life = p.life - dt
                )
            }
        }.filter { it.alpha > 0 && it.life > 0 }
    }
}

class CheckLevelCompleteUseCase {
    operator fun invoke(bubbles: List<Bubble>): Boolean {
        return bubbles.isEmpty()
    }
}

class ActivatePowerUpUseCase {
    operator fun invoke(
        powerUpType: PowerUpType,
        activePowerUps: MutableList<PowerUpType>,
        slowMoEndTime: Long,
        freezeEndTime: Long
    ): Pair<Long, Long> {
        var newSlowMoEndTime = slowMoEndTime
        var newFreezeEndTime = freezeEndTime

        when (powerUpType) {
            PowerUpType.SLOW_MO -> {
                if (powerUpType !in activePowerUps) activePowerUps.add(PowerUpType.SLOW_MO)
            }
            PowerUpType.FREEZE -> {
                if (powerUpType !in activePowerUps) activePowerUps.add(PowerUpType.FREEZE)
            }
            PowerUpType.MULTI_POP -> {
                if (powerUpType !in activePowerUps) activePowerUps.add(PowerUpType.MULTI_POP)
            }
            PowerUpType.PRISM -> {
                if (powerUpType !in activePowerUps) activePowerUps.add(PowerUpType.PRISM)
            }
        }

        return Pair(System.currentTimeMillis() + 5000, System.currentTimeMillis() + 3000)
    }
}

class CheckPowerUpExpirationUseCase {
    operator fun invoke(
        activePowerUps: MutableList<PowerUpType>,
        slowMoEndTime: Long,
        freezeEndTime: Long
    ): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val expired = activePowerUps.filter {
            (it == PowerUpType.SLOW_MO && slowMoEndTime <= now) ||
            (it == PowerUpType.FREEZE && freezeEndTime <= now)
        }.toList()

        expired.forEach { activePowerUps.remove(it) }

        return Pair(slowMoEndTime, freezeEndTime)
    }
}