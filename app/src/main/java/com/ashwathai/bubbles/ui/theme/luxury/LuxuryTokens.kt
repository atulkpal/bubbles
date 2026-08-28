package com.ashwathai.bubbles.ui.theme.luxury

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LuxuryColors {
    val Gold600 = Color(0xFFA16207)
    val Gold500 = Color(0xFFCA8A04)
    val Gold400 = Color(0xFFF0B429)
    val Gold300 = Color(0xFFFCD34D)
    val GoldGradient = listOf(Gold300, Gold500, Gold600)

    val Ink950 = Color(0xFF0C0A09)
    val Ink900 = Color(0xFF1C1917)
    val Ink800 = Color(0xFF292524)
    val Ink700 = Color(0xFF44403C)
    val Ink600 = Color(0xFF57534E)

    val Paper50 = Color(0xFFFAFAF9)
    val Paper100 = Color(0xFFF5F5F4)
    val Paper200 = Color(0xFFE7E5E4)

    val MutedForeground = Color(0xFF78716C)
    val Destructive = Color(0xFFDC2626)

    val IceBlue = Color(0xFFB3E5FC)
    val PrismMagenta = Color(0xFFE91E63)
    val BossCrimson = Color(0xFFB91C1C)

    val GlassBorderDark = Color(0x1AFFFFFF)
    val GlassBorderLight = Color(0x0D000000)
    val GlassFillDark = Color(0x10000000)
    val GlassFillLight = Color(0x08FFFFFF)
    val GlassHighlightDark = Color(0x18FFFFFF)
    val GlassHighlightLight = Color(0x18FFFFFF)
}

object LuxurySpacing {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 16.dp
    val LG = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
    val XXXL = 64.dp
}

object LuxuryRadius {
    val XS = 6.dp
    val SM = 10.dp
    val MD = 16.dp
    val LG = 24.dp
    val XL = 32.dp
    val Pill = 999.dp
}

object LuxuryElevation {
    val Flat = 0.dp
    val Card = 8.dp
    val Overlay = 24.dp
    val Modal = 40.dp
}

object LuxuryText {
    const val DisplayLarge = 52f
    const val DisplayMedium = 40f
    const val DisplaySmall = 32f
    const val HeadlineLarge = 24f
    const val HeadlineMedium = 20f
    const val HeadlineSmall = 17f
    const val BodyLarge = 16f
    const val BodyMedium = 14f
    const val BodySmall = 12f
    const val LabelLarge = 15f
    const val LabelMedium = 12f
    const val LabelSmall = 10f

    const val GoldLetterSpacing = 2f
    const val UppercaseLabelSpacing = 1.5f
}