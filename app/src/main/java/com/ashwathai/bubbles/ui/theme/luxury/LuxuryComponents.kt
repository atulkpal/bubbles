package com.ashwathai.bubbles.ui.theme.luxury

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LuxuryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(if (pressed) 80 else 200),
        label = "press"
    )

    val bg = if (primary) {
        Brush.horizontalGradient(LuxuryColors.GoldGradient)
    } else {
        Brush.horizontalGradient(listOf(LuxuryColors.GlassFillDark, LuxuryColors.Ink900))
    }

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .background(bg, RoundedCornerShape(LuxuryRadius.MD))
            .clip(RoundedCornerShape(LuxuryRadius.MD))
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
                scope.launch {
                    delay(140)
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (primary) LuxuryColors.Ink950 else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (primary) LuxuryColors.Ink950 else Color.White,
                style = LuxuryTypography.LabelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LuxuryToggle(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LuxurySpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            style = LuxuryTypography.BodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        LuxurySwitch(checked = checked, onToggle = onToggle)
    }
}

@Composable
fun LuxurySwitch(checked: Boolean, onToggle: () -> Unit) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = LuxuryMotion.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "thumb"
    )
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .size(width = trackWidth, height = trackHeight)
            .background(
                color = if (checked) LuxuryColors.Gold500 else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(LuxuryRadius.Pill)
            )
            .clip(RoundedCornerShape(LuxuryRadius.Pill))
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(LuxurySpacing.XS)
                .size(22.dp)
                .graphicsLayer {
                    translationX = with(density) { (thumbOffset * (trackWidth - 30.dp).toPx()).toFloat() }
                }
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(LuxuryRadius.Pill)
                )
        )
    }
}

@Composable
fun LuxuryStat(
    label: String,
    value: String,
    accent: Color = LuxuryColors.Gold400,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            style = LuxuryTypography.LabelSmall
        )
        Text(
            text = value,
            color = accent,
            style = LuxuryTypography.HeadlineMedium
        )
    }
}

@Composable
fun LuxuryTimerRing(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(200),
        label = "timer"
    )
    val stroke = 4.dp

    Canvas(modifier = modifier.size(size)) {
        val sweep = animatedProgress * 360f
        drawArc(
            color = Color.White.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LuxurySectionTitle(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = LuxurySpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LuxuryColors.Gold400,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            color = LuxuryColors.Gold300,
            style = LuxuryTypography.LabelMedium
        )
    }
}