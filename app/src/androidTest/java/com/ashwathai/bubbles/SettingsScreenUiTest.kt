package com.ashwathai.bubbles

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ashwathai.bubbles.domain.model.Achievements
import com.ashwathai.bubbles.domain.model.BubbleThemes
import com.ashwathai.bubbles.domain.model.EconomyState
import com.ashwathai.bubbles.ui.settings.SettingsScreen
import com.ashwathai.bubbles.ui.theme.BubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        economy: EconomyState = EconomyState(),
        soundEnabled: Boolean = true,
        musicVolume: Float = 0.35f,
        adsRemoved: Boolean = false,
        onToggleSound: () -> Unit = {},
        onToggleMusic: () -> Unit = {},
        onMusicVolumeChange: (Float) -> Unit = {},
        onSelectTheme: (String) -> Unit = {},
        onUpgrade: (String) -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        composeRule.setContent {
            BubblesTheme {
                SettingsScreen(
                    theme = BubbleThemes.fromName(economy.theme),
                    economy = economy,
                    soundEnabled = soundEnabled,
                    musicVolume = musicVolume,
                    hapticsEnabled = true,
                    reducedMotion = true,
                    adsRemoved = adsRemoved,
                    onToggleSound = onToggleSound,
                    onToggleMusic = onToggleMusic,
                    onMusicVolumeChange = onMusicVolumeChange,
                    onToggleHaptics = {},
                    onToggleReducedMotion = {},
                    onUpgrade = onUpgrade,
                    onSelectSkin = {},
                    onSelectTheme = onSelectTheme,
                    onPrestige = {},
                    onClose = onClose
                )
            }
        }
    }

    /** Section titles render uppercase, so scroll targets use case-insensitive substring. */
    private fun scrollTo(text: String) {
        composeRule
            .onNodeWithTag("settings_scroll")
            .performScrollToNode(hasText(text, ignoreCase = true, substring = true))
    }

    @Test
    fun settingsShowsCoreSections() {
        setContent()
        composeRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        scrollTo("Achievements")
        composeRule.onNodeWithText("Achievements", ignoreCase = true).assertIsDisplayed()
        scrollTo("Stats")
        composeRule.onNodeWithText("Stats", ignoreCase = true).assertIsDisplayed()
        // Scroll to the copyright line (bottom of the About card) so the whole
        // card is on screen. "About" alone matches both the section title and chip.
        scrollTo("Ashwath AI")
        composeRule.onNodeWithText("Bubbles").assertIsDisplayed()
    }

    @Test
    fun achievementsSectionShowsUnlockedTitleAndLockedPlaceholder() {
        val economy = EconomyState(unlockedAchievements = setOf("pops_100"))
        setContent(economy = economy)
        scrollTo("unlocked")
        composeRule.onNodeWithText("1 of ${Achievements.all.size} unlocked").assertIsDisplayed()
        scrollTo("First Hundred")
        composeRule.onNodeWithText("First Hundred").assertIsDisplayed()
    }

    @Test
    fun upgradePillCallsUpgradeForSlowMo() {
        var upgradedKey: String? = null
        setContent(
            economy = EconomyState(coins = 500),
            onUpgrade = { upgradedKey = it }
        )
        composeRule.onNodeWithTag("upgrade_slowMo").performClick()
        assert(upgradedKey == "slowMo")
    }

    @Test
    fun themeSelectionInvokesCallback() {
        var selected: String? = null
        setContent(onSelectTheme = { selected = it })
        // The Audio card grew (volume sliders), pushing the Themes row below the
        // fold. Scroll to the next section so the whole row is vertically on
        // screen, then bring the chip into view inside its horizontal row.
        scrollTo("Achievements")
        composeRule.onNodeWithTag("theme_chip_${BubbleThemes.SUNSET.name}").performScrollTo()
        composeRule.onNodeWithTag("theme_chip_${BubbleThemes.SUNSET.name}").performClick()
        assert(selected == BubbleThemes.SUNSET.name)
    }

    @Test
    fun soundToggleRowInvokesCallback() {
        var toggled = false
        setContent(onToggleSound = { toggled = true })
        composeRule.onNodeWithText("Sound").performClick()
        assert(toggled)
    }

    @Test
    fun closeChipClosesSettings() {
        var closed = false
        setContent(onClose = { closed = true })
        composeRule.onNodeWithContentDescription("Close settings").performClick()
        assert(closed)
    }

    @Test
    fun premiumSectionHiddenWhenAdsRemoved() {
        setContent(adsRemoved = true)
        composeRule.onAllNodesWithText("Remove All Ads").assertCountEquals(0)
    }

    @Test
    fun musicToggleRowInvokesCallback() {
        var toggled = false
        setContent(onToggleMusic = { toggled = true })
        composeRule.onNodeWithText("Music").performClick()
        assert(toggled)
    }

    @Test
    fun volumeSlidersRenderWithPercentages() {
        setContent()
        composeRule.onNodeWithText("Effects volume").assertIsDisplayed()
        composeRule.onNodeWithText("Music volume").assertIsDisplayed()
        composeRule.onNodeWithTag("sfx_volume_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("music_volume_slider").assertIsDisplayed()
        composeRule.onNodeWithText("35%").assertIsDisplayed()
    }

    @Test
    fun musicVolumeSliderInvokesCallback() {
        var received: Float? = null
        setContent(onMusicVolumeChange = { received = it })
        composeRule.onNodeWithTag("music_volume_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assert(received == 0.5f)
    }
}
