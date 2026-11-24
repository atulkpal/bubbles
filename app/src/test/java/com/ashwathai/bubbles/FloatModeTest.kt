package com.ashwathai.bubbles

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ashwathai.bubbles.data.local.EconomyRepositoryImpl
import com.ashwathai.bubbles.domain.model.BubbleType
import com.ashwathai.bubbles.domain.model.GameConfig
import com.ashwathai.bubbles.domain.usecase.FloatMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Relaunch step 3a: calm Float mode and the "pops today" pill. */
class FloatModeTest {

    private val config = GameConfig()
    private val palette = listOf(Color.Cyan, Color.Magenta)
    private val width = 1080f
    private val height = 2400f

    @Test
    fun `float bubbles spawn below the screen, fully on-screen horizontally, normal only`() {
        val rnd = Random(7)
        repeat(200) {
            val b = FloatMotion.spawn(config, width, height, palette, rnd)
            assertTrue(b.y >= height)
            assertTrue(b.x - b.radius >= 0f && b.x + b.radius <= width)
            assertEquals(BubbleType.NORMAL, b.bubbleType)
            assertTrue("rises", b.vy < 0f)
            assertTrue("level 1..2", b.level in 1..2)
        }
    }

    @Test
    fun `float bubbles rise calmly and drift away off the top`() {
        var bubbles = listOf(FloatMotion.spawn(config, width, height, palette, Random(1)))
        var t = 0f
        var seconds = 0f
        while (bubbles.isNotEmpty() && seconds < 60f) {
            bubbles = FloatMotion.update(bubbles, 1f / 60f, width, t)
            t += 1f / 60f
            seconds += 1f / 60f
        }
        assertTrue("left the screen", bubbles.isEmpty())
        // Calm: crossing a 2400-unit screen takes several seconds, not a dash.
        assertTrue("took $seconds s", seconds in 8f..30f)
    }

    @Test
    fun `a split kick eases back to an upward drift and never leaves the sides`() {
        val start = FloatMotion.spawn(config, width, height / 2f, palette, Random(3))
        var bubbles = listOf(start.copy(y = height / 2f, vx = 900f, vy = 600f))
        repeat(240) { i ->
            bubbles = FloatMotion.update(bubbles, 1f / 60f, width, i / 60f)
            bubbles.forEach { assertTrue(it.x - it.radius >= -0.01f && it.x + it.radius <= width + 0.01f) }
        }
        assertTrue("moving up again", bubbles.single().vy < 0f)
    }

    // ── pops today ──
    private val scope = CoroutineScope(Dispatchers.IO)
    private val file = File.createTempFile("float_pops_test", ".preferences_pb")
    private val store = PreferenceDataStoreFactory.create(scope = scope) { file }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun `pops today counts per calendar day and resets on a new day`() = runBlocking {
        var date = "2026-11-24"
        val repo = EconomyRepositoryImpl(store) { date }
        delay(200)
        repo.recordPops(3)
        repo.recordPops(4)
        assertEquals(7, repo.state.value.popsOn("2026-11-24"))
        assertEquals(0, repo.state.value.popsOn("2026-11-25"))
        date = "2026-11-25"
        repo.recordPops(2)
        assertEquals(2, repo.state.value.popsOn("2026-11-25"))
        assertEquals(9L, repo.state.value.lifetimePops)
    }
}
