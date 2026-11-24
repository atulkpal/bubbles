package com.ashwathai.bubbles

import androidx.compose.ui.graphics.Color
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.GameConfig
import com.ashwathai.bubbles.domain.model.WorldScale
import com.ashwathai.bubbles.domain.usecase.HandleTapUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Relaunch step 1: the same game on every screen, and taps that pop what you see. */
class TapHitTestAndWorldScaleTest {

    private val config = GameConfig()
    private val level = config.levels.first()
    private val tap = HandleTapUseCase()

    private fun bubble(id: Long, x: Float, y: Float, radius: Float = 80f, color: Color = Color.Cyan) =
        Bubble(id = id, x = x, y = y, radius = radius, color = color, vx = 0f, vy = 0f, level = 0)

    @Test
    fun `overlapping bubbles - the one drawn on top is popped`() {
        // Different colours so the same-colour chain rule doesn't pop both.
        val under = bubble(1, 100f, 100f, color = Color.Red)
        val over = bubble(2, 120f, 100f, color = Color.Blue) // drawn later = on top
        val result = tap(listOf(under, over), 110f, 100f, config, level, listOf(Color.Cyan), random = Random(1))
        val ids = result.newBubbles.map { it.id }
        assertTrue("bottom bubble must survive", 1L in ids)
        assertTrue("top bubble must be popped", 2L !in ids)
    }

    @Test
    fun `a tap just outside the rim still pops (touch slop)`() {
        val b = bubble(1, 200f, 200f, radius = 80f)
        val justOutside = 200f + 80f + WorldScale.TOUCH_SLOP * 0.5f
        val result = tap(listOf(b), justOutside, 200f, config, level, listOf(Color.Cyan), random = Random(1))
        assertTrue(result.newBubbles.none { it.id == 1L })
    }

    @Test
    fun `a clear miss pops nothing`() {
        val b = bubble(1, 200f, 200f, radius = 80f)
        val farAway = 200f + 80f + WorldScale.TOUCH_SLOP * 2f
        val result = tap(listOf(b), farAway, 200f, config, level, listOf(Color.Cyan), random = Random(1))
        assertEquals(0, result.bubblesPopped)
    }

    @Test
    fun `smallest bubble is at least 56 dp across on any phone`() {
        val minDiameterDp = config.minRadius * 2f / WorldScale.REF_DENSITY
        assertTrue("was $minDiameterDp dp", minDiameterDp >= 56f)
    }

    @Test
    fun `world size is the same in dp on low and high density screens`() {
        // A 411 dp wide phone at 2.0x (822 px) and at 3.5x (1439 px) gets the same world width.
        val lowDensityWorld = 822f / WorldScale.pxPerWorld(2.0f)
        val highDensityWorld = 1438.5f / WorldScale.pxPerWorld(3.5f)
        assertEquals(lowDensityWorld, highDensityWorld, 0.5f)
        assertEquals(WorldScale.dpToWorld(411f), lowDensityWorld, 0.5f)
    }
}
