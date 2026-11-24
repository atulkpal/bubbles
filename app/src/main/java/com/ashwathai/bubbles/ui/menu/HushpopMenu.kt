package com.ashwathai.bubbles.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashwathai.bubbles.domain.model.Bubble
import com.ashwathai.bubbles.domain.model.Soap
import com.ashwathai.bubbles.ui.game.drawSoapBubble
import com.ashwathai.bubbles.ui.theme.soap.SoapColors

/**
 * The Hushpop menu (relaunch step 6), reached from Float's menu button. Float is home;
 * this is where Challenge, the Soap Shelf and Settings live.
 */
@Composable
fun HushpopMenu(
    soap: Soap,
    highestLevel: Int,
    maxLevel: Int,
    reducedMotion: Boolean,
    onFloat: () -> Unit,
    onChallenge: (Int) -> Unit,
    onShelf: () -> Unit,
    onSettings: () -> Unit
) {
    var showLevels by remember { mutableStateOf(false) }
    val nextLevel = highestLevel.coerceIn(1, maxLevel)
    val allCleared = highestLevel > maxLevel
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - start) / 1_000_000_000f }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
        ) {
            Canvas(Modifier.size(150.dp)) {
                val b = Bubble(
                    id = 7L, x = size.width / 2f, y = size.height / 2f, radius = size.minDimension * 0.42f,
                    color = soap.palette.first(), vx = 0f, vy = 0f, level = 0
                )
                drawSoapBubble(b, t, wobble = !reducedMotion, film = soap.film, lamp = soap.lamp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Hushpop", color = SoapColors.TextPrimary, fontSize = 46.sp, fontWeight = FontWeight.Light)
            Text("pop · breathe · repeat", color = SoapColors.TextSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(40.dp))

            MenuButton("Float", primary = true, accent = soap.lamp, onClick = onFloat)
            Spacer(Modifier.height(12.dp))
            MenuButton(
                if (allCleared) "Challenge · all $maxLevel cleared" else "Challenge · level $nextLevel",
                accent = soap.lamp,
                onClick = { onChallenge(nextLevel) }
            )
            if (highestLevel > 1) {
                Text(
                    if (showLevels) "Hide levels" else "Choose a level",
                    color = SoapColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showLevels = !showLevels }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                AnimatedVisibility(showLevels) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp)
                    ) {
                        for (n in 1..nextLevel) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SoapColors.PillFill)
                                    .border(1.dp, if (n == nextLevel) soap.lamp else SoapColors.PillStroke, CircleShape)
                                    .clickable(onClickLabel = "Play level $n") { onChallenge(n) }
                            ) { Text("$n", color = SoapColors.TextPrimary, fontSize = 15.sp) }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
            MenuButton("Soap Shelf", accent = soap.lamp, onClick = onShelf)
            Spacer(Modifier.height(12.dp))
            MenuButton("Settings", accent = soap.lamp, onClick = onSettings)
            Spacer(Modifier.height(36.dp))
            Text(
                "Float is endless and calm. Challenge has 20 timed levels when you want a goal.",
                color = SoapColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuButton(label: String, accent: androidx.compose.ui.graphics.Color, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) accent.copy(alpha = 0.22f) else SoapColors.PillFill)
            .border(1.dp, if (primary) accent.copy(alpha = 0.8f) else SoapColors.PillStroke, RoundedCornerShape(50))
            .clickable(onClickLabel = label) { onClick() }
    ) {
        Text(label, color = SoapColors.TextPrimary, fontSize = 17.sp)
    }
}
