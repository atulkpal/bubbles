package com.ashwathai.bubbles.ui.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.ui.theme.soap.SoapColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Hushpop soap bubble (relaunch spec §3, §6). Drawn in world units, inside the
 * world→screen transform. Layers, back to front:
 *  1. near-transparent body with a faint tint of the bubble's colour
 *  2. thin-film interference: a swirling rainbow, masked so it is strongest at the rim
 *  3. Fresnel rim brightening + a thin bright edge
 *  4. a window-shaped specular highlight (top-left) and a warm lamp reflection (bottom-right)
 * The bubble wobbles gently (±3% on two axes) with a per-bubble phase.
 */
fun DrawScope.drawSoapBubble(
    bubble: Bubble,
    timeSec: Float,
    wobble: Boolean = true,
    film: List<Color> = SoapColors.Film,
    lamp: Color = SoapColors.Lamp
) {
    val c = Offset(bubble.x, bubble.y)
    val r = bubble.radius
    val tint = bubble.color
    val phase = (bubble.id % 997L).toFloat() * 0.37f

    val sx = if (wobble) 1f + 0.03f * sin(timeSec * 2.1f + phase) else 1f
    val sy = if (wobble) 1f + 0.03f * sin(timeSec * 2.7f + phase * 1.3f) else 1f

    withTransform({ scale(sx, sy, pivot = c) }) {
        // 1. Body: almost clear, a little denser toward the edge
        drawCircle(
            brush = Brush.radialGradient(
                0f to tint.copy(alpha = 0.03f),
                0.7f to tint.copy(alpha = 0.07f),
                1f to tint.copy(alpha = 0.20f),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )

        // 2. Thin film. Colours lean toward this bubble's tint so chains stay readable.
        val filmBands = film.mapIndexed { i, f ->
            lerp(f, tint, if (i % 2 == 0) 0.45f else 0.2f).copy(alpha = 0.75f)
        }
        val swirl = Offset(
            c.x + sin(timeSec * 0.9f + phase) * r * 0.22f,
            c.y + cos(timeSec * 0.7f + phase) * r * 0.22f
        )
        val bounds = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        drawContext.canvas.saveLayer(bounds, Paint())
        rotate(degrees = (timeSec * 20f + phase * 57f) % 360f, pivot = c) {
            drawCircle(brush = Brush.sweepGradient(filmBands, center = swirl), radius = r, center = c)
        }
        // Mask: clear in the middle, film shows mostly near the rim (like a real bubble)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0.04f),
                0.55f to Color.Black.copy(alpha = 0.16f),
                0.85f to Color.Black.copy(alpha = 0.62f),
                1f to Color.Black.copy(alpha = 0.85f),
                center = c,
                radius = r
            ),
            radius = r,
            center = c,
            blendMode = BlendMode.DstIn
        )
        drawContext.canvas.restore()

        // 3. Fresnel rim + crisp edge
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.84f to Color.Transparent,
                0.97f to Color.White.copy(alpha = 0.26f),
                1f to Color.White.copy(alpha = 0.06f),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = r,
            center = c,
            style = Stroke(width = max(2f, r * 0.018f))
        )

        // 4a. Window highlight (top-left), tilted along the curve
        val hl = Offset(c.x - r * 0.42f, c.y - r * 0.48f)
        rotate(degrees = -38f, pivot = hl) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.78f),
                topLeft = Offset(hl.x - r * 0.17f, hl.y - r * 0.08f),
                size = Size(r * 0.34f, r * 0.16f),
                cornerRadius = CornerRadius(r * 0.08f)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r * 0.045f,
            center = Offset(c.x - r * 0.16f, c.y - r * 0.66f)
        )
        // 4b. Warm lamp reflection (bottom-right arc)
        drawArc(
            color = lamp.copy(alpha = 0.38f),
            startAngle = 18f,
            sweepAngle = 52f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.8f, c.y - r * 0.8f),
            size = Size(r * 1.6f, r * 1.6f),
            style = Stroke(width = r * 0.055f, cap = StrokeCap.Round)
        )
    }
}

/** Night room: violet gradient, one warm lamp glow top-right, a low haze. Screen px space. */
fun DrawScope.drawSoapBackground(
    top: Color = SoapColors.NightTop,
    bottom: Color = SoapColors.NightBottom,
    lamp: Color = SoapColors.Lamp
) {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    drawCircle(
        brush = Brush.radialGradient(
            0f to lamp.copy(alpha = 0.16f),
            0.45f to lamp.copy(alpha = 0.05f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.86f, size.height * 0.08f),
            radius = size.width * 0.9f
        ),
        radius = size.width * 0.9f,
        center = Offset(size.width * 0.86f, size.height * 0.08f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            0.6f to Color.Transparent,
            1f to lerp(bottom, lamp, 0.12f).copy(alpha = 0.55f)
        )
    )
}
