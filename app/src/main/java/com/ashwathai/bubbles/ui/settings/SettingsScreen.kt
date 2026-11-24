package com.ashwathai.bubbles.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import com.ashwathai.bubbles.APP_VERSION
import com.ashwathai.bubbles.openUrl
import com.ashwathai.bubbles.domain.model.Achievement
import com.ashwathai.bubbles.domain.model.Achievements
import com.ashwathai.bubbles.domain.model.BubbleSkin
import com.ashwathai.bubbles.domain.model.BubbleSkins
import com.ashwathai.bubbles.domain.model.BubbleThemes
import com.ashwathai.bubbles.domain.model.EconomyConfig
import com.ashwathai.bubbles.domain.model.EconomyState
import com.ashwathai.bubbles.domain.model.ThemePalette
import com.ashwathai.bubbles.ui.theme.luxury.GlassSurface
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryButton
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryColors
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryIcons
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryMotion
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryRadius
import com.ashwathai.bubbles.ui.theme.luxury.LuxurySectionTitle
import com.ashwathai.bubbles.ui.theme.luxury.LuxurySlider
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryToggle
import com.ashwathai.bubbles.ui.theme.luxury.LuxurySpacing
import com.ashwathai.bubbles.ui.theme.luxury.LuxuryTypography

@Composable
fun SettingsScreen(
    theme: ThemePalette,
    economy: EconomyState,
    soundEnabled: Boolean,
    musicEnabled: Boolean = true,
    sfxVolume: Float = 1f,
    musicVolume: Float = 0.35f,
    hapticsEnabled: Boolean,
    reducedMotion: Boolean,
    adsRemoved: Boolean,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit = {},
    onSfxVolumeChange: (Float) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onToggleHaptics: () -> Unit,
    onToggleReducedMotion: () -> Unit,
    onUpgrade: (String) -> Unit,
    onSelectSkin: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onPrestige: (() -> Unit)? = null,
    onClose: () -> Unit,
    onTrySkin: ((String) -> Unit)? = null,
    onRemoveAds: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Solid night-room backdrop: nothing from the screen underneath bleeds through.
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(com.ashwathai.bubbles.ui.theme.soap.SoapColors.NightTop, com.ashwathai.bubbles.ui.theme.soap.SoapColors.NightBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(shape = RoundedCornerShape(LuxuryRadius.XL),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_scroll")
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SETTINGS",
                        style = LuxuryTypography.DisplaySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.Pill),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.Pill),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LuxuryIcons.Coin,
                            contentDescription = null,
                            tint = LuxuryColors.Gold400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${economy.coins} coins",
                            style = LuxuryTypography.LabelLarge,
                            color = LuxuryColors.Gold400
                        )
                    }
                }
                Spacer(modifier = Modifier.height(LuxurySpacing.MD))

                LuxurySectionTitle("Audio & Haptics", LuxuryIcons.Volume)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LuxuryToggle("Sound", soundEnabled, onToggleSound)
                        LuxuryToggle("Music", musicEnabled, onToggleMusic)
                        LuxurySlider(
                            label = "Effects volume",
                            value = sfxVolume,
                            onValueChange = onSfxVolumeChange,
                            testTag = "sfx_volume_slider"
                        )
                        LuxurySlider(
                            label = "Music volume",
                            value = musicVolume,
                            onValueChange = onMusicVolumeChange,
                            testTag = "music_volume_slider"
                        )
                        LuxuryToggle("Haptics", hapticsEnabled, onToggleHaptics)
                        LuxuryToggle("Reduced Motion", reducedMotion, onToggleReducedMotion)
                    }
                }

                // ── Remove Ads ──
                if (!adsRemoved && onRemoveAds != null) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxurySectionTitle("Premium", LuxuryIcons.Sparkle)
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.MD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Supporter Pack",
                                style = LuxuryTypography.HeadlineSmall,
                                color = LuxuryColors.Gold400
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Removes every ad for good, unlocks the Moonlit Pearl soap, and bonus videos pay out instantly. Thank you for supporting a one-person studio.",
                                style = LuxuryTypography.BodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                            LuxuryButton(
                                text = "BECOME A SUPPORTER",
                                onClick = onRemoveAds,
                                modifier = Modifier.fillMaxWidth(),
                                icon = LuxuryIcons.Sparkle,
                                height = 48.dp
                            )
                        }
                    }
                }
                if (adsRemoved) {
                    Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                    LuxurySectionTitle("Premium", LuxuryIcons.Sparkle)
                    GlassSurface(
                        shape = RoundedCornerShape(LuxuryRadius.MD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LuxuryColors.Gold400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Supporter · no ads · thank you",
                                style = LuxuryTypography.BodyLarge,
                                color = LuxuryColors.Gold400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("Achievements", LuxuryIcons.Star)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "${economy.unlockedAchievements.size} of ${Achievements.all.size} unlocked",
                            style = LuxuryTypography.LabelMedium,
                            color = LuxuryColors.Gold400
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Achievements.all.forEach { achievement ->
                            AchievementRow(
                                achievement = achievement,
                                unlocked = achievement.id in economy.unlockedAchievements
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("Stats", LuxuryIcons.Chart)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        StatRow("Lifetime pops", "${economy.lifetimePops}")
                        StatRow("Best combo", "x${economy.maxCombo}")
                        StatRow("Games played", "${economy.gamesPlayed}")
                        StatRow("Days with Hushpop", "${economy.daysPlayed}")
                        StatRow("Soaps collected", "${economy.unlockedSoaps.size}")
                    }
                }

                Spacer(modifier = Modifier.height(LuxurySpacing.MD))
                LuxurySectionTitle("About", LuxuryIcons.Lotus)
                GlassSurface(
                    shape = RoundedCornerShape(LuxuryRadius.MD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Hushpop",
                            style = LuxuryTypography.HeadlineSmall,
                            color = Color.White
                        )
                        Text(
                            "v$APP_VERSION",
                            style = LuxuryTypography.LabelMedium,
                            color = LuxuryColors.Gold300
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "A premium bubble-popping stress buster — pop, breathe, repeat.",
                            style = LuxuryTypography.BodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                        Text(
                            "© 2026 Ashwath AI. All rights reserved.",
                            style = LuxuryTypography.LabelSmall,
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(LuxurySpacing.SM))
                        Row(horizontalArrangement = Arrangement.spacedBy(LuxurySpacing.SM)) {
                            AboutLinkChip("ABOUT", "https://bubbles.ashwathai.online/about.html")
                            AboutLinkChip("PRIVACY", "https://bubbles.ashwathai.online/privacy.html")
                            AboutLinkChip("DELETE DATA", "https://bubbles.ashwathai.online/data-deletion.html")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    currentLevel: Int,
    coins: Int,
    onUpgrade: () -> Unit,
    tag: String? = null
) {
    val maxed = currentLevel >= EconomyConfig.UPGRADE_MAX
    val affordable = coins >= EconomyConfig.UPGRADE_COST
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LuxuryColors.Gold400,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, style = LuxuryTypography.BodyLarge, color = Color.White)
                Text(detail, style = LuxuryTypography.BodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Glass pill: gold hairline when affordable, dimmed when not, gold outline at MAX.
        val pillBorder = when {
            maxed -> Modifier.border(1.dp, LuxuryColors.Gold400.copy(alpha = 0.55f), CircleShape)
            affordable -> Modifier.border(1.dp, LuxuryColors.Gold400.copy(alpha = 0.45f), CircleShape)
            else -> Modifier
        }
        GlassSurface(
            shape = CircleShape,
            modifier = Modifier
                .height(48.dp)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .then(pillBorder)
                .clickable(enabled = !maxed && affordable) { onUpgrade() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!maxed) {
                    Icon(
                        imageVector = LuxuryIcons.Coin,
                        contentDescription = null,
                        tint = if (affordable) LuxuryColors.Gold400 else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (maxed) "MAX" else "${EconomyConfig.UPGRADE_COST}",
                    style = LuxuryTypography.LabelMedium,
                    color = when {
                        maxed -> LuxuryColors.Gold400
                        affordable -> LuxuryColors.Gold400
                        else -> Color.White.copy(alpha = 0.35f)
                    }
                )
            }
        }
    }
}

@Composable
fun SkinCard(skin: BubbleSkin, locked: Boolean, selected: Boolean, onClick: () -> Unit, onTryAd: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = LuxuryMotion.Press,
        label = "skinCardPress"
    )
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (locked && !selected) 0.55f else 1f
            }
            .width(88.dp)
            .background(
                if (selected) LuxuryColors.Ink900 else LuxuryColors.Ink900.copy(alpha = 0.6f),
                RoundedCornerShape(LuxuryRadius.MD)
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(LuxuryColors.Gold300, LuxuryColors.Gold400)
                    ),
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) else Modifier
            )
            .clickable(
                enabled = !locked,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.radialGradient(
                        listOf(skin.palette.first(), skin.palette.first().copy(alpha = 0.6f))
                    ),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked skin",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            } else if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected skin",
                    tint = LuxuryColors.Gold400,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = skin.name,
                style = LuxuryTypography.LabelSmall,
                color = if (selected) LuxuryColors.Gold300 else Color.White.copy(alpha = 0.7f)
            )
        }
        if (locked && onTryAd != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TRY",
                style = LuxuryTypography.LabelSmall,
                color = LuxuryColors.Gold400,
                modifier = Modifier
                    .clickable { onTryAd() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ThemeCard(themePalette: ThemePalette, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = LuxuryMotion.Press,
        label = "themeCardPress"
    )
    Column(
        modifier = Modifier
            .testTag("theme_chip_${themePalette.name}")
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .width(88.dp)
            .background(
                if (selected) LuxuryColors.Ink900 else LuxuryColors.Ink900.copy(alpha = 0.6f),
                RoundedCornerShape(LuxuryRadius.MD)
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(LuxuryColors.Gold300, LuxuryColors.Gold400)
                    ),
                    shape = RoundedCornerShape(LuxuryRadius.MD)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.verticalGradient(listOf(themePalette.top, themePalette.bottom)),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected theme",
                    tint = LuxuryColors.Gold400,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = themePalette.name,
                style = LuxuryTypography.LabelSmall,
                color = if (selected) LuxuryColors.Gold300 else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = LuxuryTypography.BodyMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = LuxuryTypography.BodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassSurface(
            shape = RoundedCornerShape(LuxuryRadius.Pill),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = LuxuryIcons.Star,
                    contentDescription = if (unlocked) "Unlocked" else "Locked",
                    tint = if (unlocked) LuxuryColors.Gold400 else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (unlocked) achievement.title else "?",
                style = LuxuryTypography.BodyLarge,
                color = if (unlocked) Color.White else Color.White.copy(alpha = 0.45f)
            )
            Text(
                text = if (unlocked) achievement.description else "Keep playing to reveal",
                style = LuxuryTypography.BodySmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        if (unlocked) {
            Text(
                text = "+${achievement.rewardCoins}",
                style = LuxuryTypography.LabelMedium,
                color = LuxuryColors.Gold400
            )
        }
    }
}

@Composable
private fun AboutLinkChip(label: String, url: String) {
    val context = LocalContext.current
    GlassSurface(shape = RoundedCornerShape(LuxuryRadius.Pill)) {
        Box(
            modifier = Modifier
                .clickable { openUrl(context, url) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                label,
                style = LuxuryTypography.LabelMedium,
                color = LuxuryColors.Gold300
            )
        }
    }
}
