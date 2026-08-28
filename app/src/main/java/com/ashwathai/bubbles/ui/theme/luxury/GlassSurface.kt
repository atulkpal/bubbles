package com.ashwathai.bubbles.ui.theme.luxury

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LuxuryRadius.LG),
    content: @Composable () -> Unit
) {
    val fill = LuxuryColors.GlassFillDark
    val highlight = LuxuryColors.GlassHighlightDark

    val surfaced = Modifier
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(highlight.copy(alpha = 0.35f), fill),
                startY = 0f,
                endY = 1f
            ),
            shape = shape
        )
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = Float.POSITIVE_INFINITY
            ),
            shape = shape
        )
        .clip(shape)

    Box(modifier = modifier.then(surfaced)) { content() }
}