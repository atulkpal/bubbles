package com.ashwathai.bubbles.ui.theme.soap

import androidx.compose.ui.graphics.Color

/**
 * Hushpop "Soap" design tokens (relaunch spec §3): a quiet night room lit by one warm
 * lamp, with iridescent soap film as the only saturated colour. No gold, no serif.
 */
object SoapColors {
    // Night room
    val NightTop = Color(0xFF0E1022)
    val NightBottom = Color(0xFF1C1B3A)
    val NightHaze = Color(0xFF2A2350)

    // The single warm light source (lamp glow + its reflection in every bubble)
    val Lamp = Color(0xFFFFC98A)

    // Text and UI
    val TextPrimary = Color(0xFFF4F1FF)
    val TextSecondary = Color(0xB3F4F1FF)
    val PillFill = Color(0x1FFFFFFF)
    val PillStroke = Color(0x33FFFFFF)

    /** Thin-film interference bands (magenta → gold → cyan → violet), looped. */
    val Film = listOf(
        Color(0xFFFF5FD2),
        Color(0xFFFFD36B),
        Color(0xFF7CF5C4),
        Color(0xFF4FD8FF),
        Color(0xFF8A6BFF),
        Color(0xFFFF5FD2)
    )
}
