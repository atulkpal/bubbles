package com.ashwathai.bubbles.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.Soap
import com.ashwathai.bubbles.domain.model.SoapShelf
import com.ashwathai.bubbles.domain.model.Soaps
import com.ashwathai.bubbles.ui.game.drawSoapBubble
import com.ashwathai.bubbles.ui.theme.soap.SoapColors

/** Small jar drawn in code; [fill] 0..1. */
@Composable
fun JarIcon(fill: Float, liquid: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(fill.coerceIn(0f, 1f), spring(stiffness = 120f), label = "jar")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Size(w * 0.62f, h * 0.66f)
        val topLeft = Offset((w - body.width) / 2f, h * 0.26f)
        val stroke = 1.6.dp.toPx()
        val liquidH = (body.height - stroke * 2) * animated
        drawRoundRect(
            color = liquid.copy(alpha = 0.85f),
            topLeft = Offset(topLeft.x + stroke, topLeft.y + body.height - stroke - liquidH),
            size = Size(body.width - stroke * 2, liquidH),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        drawRoundRect(
            color = SoapColors.TextPrimary.copy(alpha = 0.9f),
            topLeft = topLeft,
            size = body,
            cornerRadius = CornerRadius(w * 0.12f),
            style = Stroke(stroke)
        )
        // lid
        drawRoundRect(
            color = SoapColors.TextPrimary.copy(alpha = 0.9f),
            topLeft = Offset(w * 0.3f, h * 0.14f),
            size = Size(w * 0.4f, h * 0.1f),
            cornerRadius = CornerRadius(w * 0.04f)
        )
    }
}

/** A still soap bubble used as a swatch. */
@Composable
private fun SoapSwatch(soap: Soap, locked: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.9f
        val b = Bubble(
            id = soap.id.hashCode().toLong(), x = size.width / 2f, y = size.height / 2f, radius = r,
            color = soap.palette.first(), vx = 0f, vy = 0f, level = 0
        )
        drawCircle(
            brush = Brush.verticalGradient(listOf(soap.backgroundTop, soap.backgroundBottom)),
            radius = size.minDimension / 2f
        )
        if (locked) {
            drawCircle(Color.White.copy(alpha = 0.18f), radius = r, style = Stroke(1.5.dp.toPx()))
        } else {
            drawSoapBubble(b, timeSec = 1.3f, wobble = false, film = soap.film, lamp = soap.lamp)
        }
    }
}

/**
 * Soap Shelf (relaunch spec §7): the jar, today's free soap, every soap (owned ones
 * selectable), and the kind streak.
 */
@Composable
fun SoapShelfScreen(
    jarDrops: Int,
    unlocked: Set<String>,
    activeSoapId: String,
    daysPlayed: Int,
    today: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val todaysSoap = remember(today) { SoapShelf.soapOfTheDay(today) }
    val next = SoapShelf.nextLocked(unlocked)
    val allOwned = next == null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SoapColors.NightTop)
                .border(1.dp, SoapColors.PillStroke, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Soap Shelf", color = SoapColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoapColors.PillFill)
                        .clickable(onClickLabel = "Close soap shelf") { onClose() },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = SoapColors.TextPrimary, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(16.dp))

            // Jar progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarIcon(
                    fill = jarDrops / SoapShelf.JAR_CAPACITY.toFloat(),
                    liquid = Soaps.byId(activeSoapId).lamp,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (allOwned) "Every soap collected. Thank you for popping!"
                        else "$jarDrops / ${SoapShelf.JAR_CAPACITY} drops",
                        color = SoapColors.TextPrimary, fontSize = 16.sp
                    )
                    if (next != null) {
                        Text("Each pop adds a drop. Fill the jar to unlock ${next.name}.", color = SoapColors.TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SoapColors.PillFill)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((jarDrops / SoapShelf.JAR_CAPACITY.toFloat()).coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(Soaps.byId(activeSoapId).lamp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Today's free soap
            val todayOwned = todaysSoap.id in unlocked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SoapColors.PillFill)
                    .border(1.dp, SoapColors.PillStroke, RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = "Use today's soap") { onSelect(todaysSoap.id) }
                    .padding(12.dp)
            ) {
                SoapSwatch(todaysSoap, locked = false, modifier = Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Soap of the day", color = SoapColors.TextSecondary, fontSize = 12.sp)
                    Text(todaysSoap.name, color = SoapColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (todayOwned) "Yours to keep" else "Free for everyone today",
                        color = SoapColors.TextSecondary, fontSize = 12.sp
                    )
                }
                if (activeSoapId == todaysSoap.id) Text("In use", color = todaysSoap.lamp, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))

            // All soaps
            Soaps.ALL.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { soap ->
                        val usable = soap.id in unlocked || soap == todaysSoap
                        val selected = soap.id == activeSoapId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (selected) Modifier.border(1.5.dp, soap.lamp, RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                                .clickable(enabled = usable, onClickLabel = "Use ${soap.name}") { onSelect(soap.id) }
                                .semantics { contentDescription = if (usable) soap.name else "${soap.name}, locked" }
                                .padding(vertical = 10.dp)
                        ) {
                            SoapSwatch(soap, locked = !usable, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                soap.name,
                                color = if (usable) SoapColors.TextPrimary else SoapColors.TextSecondary.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                            if (!usable) Text("Locked", color = SoapColors.TextSecondary.copy(alpha = 0.45f), fontSize = 11.sp)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (daysPlayed == 1) "1 day with Hushpop" else "$daysPlayed days with Hushpop",
                color = SoapColors.TextSecondary, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Missed a day? No worries, it never resets.",
                color = SoapColors.TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/** "New soap" moment: a quiet chip at the top when a full jar unlocks a soap. */
@Composable
fun SoapUnlockedChip(soap: Soap, onDismiss: () -> Unit) {
    LaunchedEffect(soap.id) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 72.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SoapColors.NightTop.copy(alpha = 0.9f))
                .border(1.dp, soap.lamp, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SoapSwatch(soap, locked = false, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("New soap · ${soap.name}", color = SoapColors.TextPrimary, fontSize = 15.sp)
        }
    }
}

/** Kind end-of-session line: "Nice. 4 breaths." Never a fail message. */
@Composable
fun BreatheSummaryChip(breaths: Int, onDismiss: () -> Unit) {
    LaunchedEffect(breaths) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 72.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            if (breaths == 1) "Nice. 1 calm breath." else "Nice. $breaths calm breaths.",
            color = SoapColors.TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SoapColors.NightTop.copy(alpha = 0.9f))
                .border(1.dp, SoapColors.PillStroke, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}
