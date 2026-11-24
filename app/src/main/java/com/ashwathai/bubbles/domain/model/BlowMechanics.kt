package com.ashwathai.bubbles.domain.model

import kotlin.math.max
import kotlin.math.min

/**
 * Hold-to-blow (relaunch spec §5): hold on empty space in Float and a bubble inflates at
 * the fingertip; release (or wait until it is full) and it floats away. Pure rules only,
 * in world units, so they can be unit-tested.
 */
object BlowMechanics {
    /** Holds shorter than this are treated as a plain tap on empty space (nothing blown). */
    const val MIN_HOLD_SEC = 0.18f
    /** Time to inflate from a speck to full size. */
    const val FULL_SEC = 2.5f
    /** Once full, the bubble detaches by itself after this extra time, like a real one. */
    const val AUTO_RELEASE_AFTER_FULL_SEC = 0.6f
    /** Marker stored in EconomyState.seenBubbleTypes once the player has blown a bubble. */
    const val HINT_SEEN_MARKER = "HINT_BLOW"

    fun startRadius(config: GameConfig): Float = config.minRadius * 0.35f
    fun maxRadius(config: GameConfig): Float = config.minRadius + 2 * config.radiusStep

    /** Radius after holding for [heldSec]: quick at first, slowing as it fills (ease-out). */
    fun radiusAt(config: GameConfig, heldSec: Float): Float {
        val f = min(1f, max(0f, heldSec) / FULL_SEC)
        val ease = 1f - (1f - f) * (1f - f)
        return startRadius(config) + (maxRadius(config) - startRadius(config)) * ease
    }

    fun shouldAutoRelease(heldSec: Float): Boolean = heldSec >= FULL_SEC + AUTO_RELEASE_AFTER_FULL_SEC

    /** Split level for a released bubble: bigger blown bubbles cascade into more pops. */
    fun levelFor(config: GameConfig, radius: Float): Int = when {
        radius >= config.minRadius + 2 * config.radiusStep * 0.95f -> 2
        radius >= config.minRadius + config.radiusStep * 0.9f -> 1
        else -> 0
    }

    /** Released bubbles are never smaller than the minimum comfortable touch size. */
    fun releasedRadius(config: GameConfig, radius: Float): Float = max(radius, config.minRadius)

    /** The bubble sits just above the fingertip so the finger doesn't hide it. */
    fun centerYAboveFinger(fingerY: Float, radius: Float): Float = fingerY - radius * 0.9f
}
