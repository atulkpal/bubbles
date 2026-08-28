package com.ashwathai.bubbles.ui.theme.luxury

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.DampingRatioHighBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring

object LuxuryMotion {
    const val MicroMs = 120
    const val QuickMs = 200
    const val StandardMs = 300
    const val SlowMs = 450

    val EaseOut = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
    val EaseInOut = FastOutSlowInEasing
    val EaseSmooth = LinearOutSlowInEasing

    fun spring(
        dampingRatio: Float = DampingRatioHighBouncy,
        stiffness: Float = StiffnessLow
    ) = spring<Float>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
}

object LuxuryHaptics {
    // Waveform patterns: alternating on/off durations in ms.
    // Single short tap for a normal pop.
    val PopPattern = longArrayOf(15)

    // Longer, heavier thud for big pops / boss hits.
    val BigPopPattern = longArrayOf(40, 30, 25)

    // Double-pulse for combo feedback.
    val ComboPattern = longArrayOf(20, 25, 20)

    // Triple rising pulse for boss hits.
    val BossHitPattern = longArrayOf(15, 20, 25, 20, 35)

    // Ascending success fanfare for level-up.
    val LevelUpPattern = longArrayOf(15, 15, 20, 15, 25, 15, 40)
}