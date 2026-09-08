package com.ashwathai.bubbles

import androidx.compose.ui.graphics.Color
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.usecase.CheckLevelCompleteUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckLevelCompleteUseCaseTest {

    private val useCase = CheckLevelCompleteUseCase()

    private fun bubble(id: Long) = Bubble(
        id = id,
        x = 100f,
        y = 100f,
        radius = 40f,
        color = Color.Red,
        vx = 0f,
        vy = 0f,
        level = 1
    )

    @Test
    fun `fresh level with empty board is not complete`() {
        // Regression: an empty board at level start used to trigger instant completion
        assertFalse(useCase(emptyList(), totalBubblesToSpawn = 6, bubblesSpawnedSoFar = 0))
    }

    @Test
    fun `partially spawned level with empty board is not complete`() {
        assertFalse(useCase(emptyList(), totalBubblesToSpawn = 6, bubblesSpawnedSoFar = 3))
    }

    @Test
    fun `fully spawned and cleared board is complete`() {
        assertTrue(useCase(emptyList(), totalBubblesToSpawn = 6, bubblesSpawnedSoFar = 6))
    }

    @Test
    fun `fully spawned but board still has bubbles is not complete`() {
        assertFalse(useCase(listOf(bubble(1)), totalBubblesToSpawn = 6, bubblesSpawnedSoFar = 6))
    }

    @Test
    fun `spawn budget overshoot with empty board is complete`() {
        assertTrue(useCase(emptyList(), totalBubblesToSpawn = 6, bubblesSpawnedSoFar = 8))
    }
}
