package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.usecase.CanSpawnBubbleUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanSpawnBubbleUseCaseTest {

    private val useCase = CanSpawnBubbleUseCase()

    // ── Adventure mode: spawn budget governs ──

    @Test
    fun `adventure allows spawn while budget remains and board has room`() {
        assertTrue(useCase(bubblesOnBoard = 3, maxBubbles = 8, bubblesSpawnedSoFar = 3, isEndlessMode = false))
    }

    @Test
    fun `adventure blocks spawn once budget is spent even if board is empty`() {
        // Regression: the level-3 never-ending bug — board refilled after every pop
        assertFalse(useCase(bubblesOnBoard = 0, maxBubbles = 8, bubblesSpawnedSoFar = 8, isEndlessMode = false))
    }

    @Test
    fun `adventure blocks spawn when board is at cap even with budget left`() {
        assertFalse(useCase(bubblesOnBoard = 8, maxBubbles = 8, bubblesSpawnedSoFar = 2, isEndlessMode = false))
    }

    @Test
    fun `adventure budget overshoot still blocks spawn`() {
        // Splits/minions can push spawned count past the budget — never spawn more
        assertFalse(useCase(bubblesOnBoard = 1, maxBubbles = 8, bubblesSpawnedSoFar = 10, isEndlessMode = false))
    }

    // ── Endless modes (Zen/Daily): board cap governs ──

    @Test
    fun `endless allows spawn below cap regardless of spawn counter`() {
        assertTrue(useCase(bubblesOnBoard = 5, maxBubbles = 18, bubblesSpawnedSoFar = 999, isEndlessMode = true))
    }

    @Test
    fun `endless blocks spawn at cap`() {
        assertFalse(useCase(bubblesOnBoard = 18, maxBubbles = 18, bubblesSpawnedSoFar = 10, isEndlessMode = true))
    }
}
