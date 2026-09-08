package com.ashwathai.bubbles

import androidx.compose.ui.graphics.Color
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.BubbleSkins
import com.ashwathai.bubbles.domain.model.BubbleType
import com.ashwathai.bubbles.domain.model.GameConfig
import com.ashwathai.bubbles.domain.model.LevelConfig
import com.ashwathai.bubbles.domain.usecase.HandleTapUseCase
import com.ashwathai.bubbles.domain.usecase.SpawnBubblesUseCase
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpawnBalanceTest {

    private val spawn = SpawnBubblesUseCase()
    private val config = GameConfig()
    private val palette = BubbleSkins.CLASSIC.palette

    private fun levelConfig(level: Int) = LevelConfig(
        level = level,
        maxBubbles = 20,
        spawnInterval = 1f,
        baseSpeed = 400f,
        powerUpChance = 0.05f,
        timeLimit = 60f
    )

    /** Spawn [count] bubbles one at a time (1 per call, as the game loop does). */
    private fun spawnMany(level: Int, count: Int, seed: Long = 42): List<Bubble> {
        val random = Random(seed)
        return (0 until count).map {
            spawn(
                config, levelConfig(level), 800f, 1200f, emptyList(),
                palette = palette,
                random = random
            ).first()
        }
    }

    private fun typeCounts(bubbles: List<Bubble>): Map<BubbleType, Int> {
        val counts = mutableMapOf<BubbleType, Int>()
        bubbles.forEach { b ->
            counts[b.bubbleType] = (counts[b.bubbleType] ?: 0) + 1
        }
        return counts
    }

    @Test
    fun `level 1 has no special bubbles at all`() {
        val counts = typeCounts(spawnMany(1, 200))
        assertEquals(200, counts[BubbleType.NORMAL] ?: 0)
    }

    @Test
    fun `level 2 still has no special bubbles`() {
        val counts = typeCounts(spawnMany(2, 200))
        assertEquals(200, counts[BubbleType.NORMAL] ?: 0)
    }

    @Test
    fun `level 5 introduces bomb and frozen but nothing beyond`() {
        val counts = typeCounts(spawnMany(5, 400))
        assertTrue("expected some FROZEN at level 5", (counts[BubbleType.FROZEN] ?: 0) > 0)
        assertTrue("expected some BOMB at level 5", (counts[BubbleType.BOMB] ?: 0) > 0)
        assertEquals(0, counts[BubbleType.MAGNET] ?: 0)
        assertEquals(0, counts[BubbleType.TICKING_BOMB] ?: 0)
        assertEquals(0, counts[BubbleType.CHAOS] ?: 0)
        assertEquals(0, counts[BubbleType.GHOST] ?: 0)
        assertTrue("NORMAL must stay dominant", (counts[BubbleType.NORMAL] ?: 0) > 200)
    }

    @Test
    fun `level 20 has a healthy mix with normals dominant`() {
        val counts = typeCounts(spawnMany(20, 500))
        val normal = counts[BubbleType.NORMAL] ?: 0
        assertTrue("NORMAL should be 40-65% at level 20, was $normal/500", normal > 200 && normal < 325)
        assertTrue("GHOST should be possible by level 20", (counts[BubbleType.GHOST] ?: 0) > 0)
    }

    @Test
    fun `zen mix only contains bomb frozen rainbow and normal`() {
        val zenConfig = LevelConfig(
            level = 0,
            maxBubbles = 18,
            spawnInterval = 2.5f,
            baseSpeed = 220f,
            powerUpChance = 0.07f,
            timeLimit = 99999f
        )
        val random = Random(7)
        val bubbles = (0 until 300).map {
            spawn(config, zenConfig, 800f, 1200f, emptyList(), palette = palette, random = random).first()
        }
        val counts = typeCounts(bubbles)
        assertEquals(0, counts[BubbleType.MAGNET] ?: 0)
        assertEquals(0, counts[BubbleType.TICKING_BOMB] ?: 0)
        assertEquals(0, counts[BubbleType.CHAOS] ?: 0)
        assertEquals(0, counts[BubbleType.GHOST] ?: 0)
        assertTrue("NORMAL dominant in zen", (counts[BubbleType.NORMAL] ?: 0) > 150)
    }

    // ── Ghost split containment ──

    private val tap = HandleTapUseCase()

    private fun ghost(id: Long, radius: Float, generation: Int = 0) = Bubble(
        id = id,
        x = 100f,
        y = 100f,
        radius = radius,
        color = Color(0xFFB3E5FC),
        vx = 0f,
        vy = 0f,
        level = 1,
        bubbleType = BubbleType.GHOST,
        health = 1,
        splitGeneration = generation
    )

    @Test
    fun `ghost splits into 3 children at floored minimum radius`() {
        val result = tap(
            listOf(ghost(1, 70f)),
            100f, 100f,
            config, levelConfig(20), palette,
            random = Random(3)
        )
        val children = result.newBubbles.filter { it.bubbleType == BubbleType.GHOST }
        assertEquals(3, children.size)
        children.forEach {
            assertEquals("children must never be smaller than minRadius", 1, it.splitGeneration)
            assertTrue("child radius $it.radius must be >= minRadius", it.radius >= config.minRadius)
        }
    }

    @Test
    fun `split ghost children never re-split`() {
        val child = ghost(10, 40f, generation = 1)
        val result = tap(
            listOf(child),
            100f, 100f,
            config, levelConfig(20), palette,
            random = Random(3)
        )
        assertEquals(
            "gen-1 ghost must not spawn more ghosts",
            0,
            result.newBubbles.count { it.bubbleType == BubbleType.GHOST }
        )
    }

    @Test
    fun `small parent ghost children are floored not halved`() {
        val result = tap(
            listOf(ghost(1, 50f)),
            100f, 100f,
            config, levelConfig(20), palette,
            random = Random(3)
        )
        result.newBubbles.filter { it.bubbleType == BubbleType.GHOST }.forEach {
            assertTrue("child radius $it.radius must be >= minRadius", it.radius >= config.minRadius)
        }
    }
}