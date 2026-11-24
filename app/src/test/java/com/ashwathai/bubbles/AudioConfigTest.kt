package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.AudioConfig
import com.ashwathai.bubbles.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioConfigTest {

    @Test
    fun `default sfx volume is full`() {
        assertEquals(1.0f, AudioConfig.DEFAULT_SFX_VOLUME)
    }

    @Test
    fun `default music volume is subtle`() {
        assertEquals(0.35f, AudioConfig.DEFAULT_MUSIC_VOLUME)
    }

    @Test
    fun `crossfade duration is 1_5 seconds`() {
        assertEquals(1500L, AudioConfig.CROSSFADE_MS)
    }

    @Test
    fun `coerceVolume clamps below zero to zero`() {
        assertEquals(0f, AudioConfig.coerceVolume(-0.5f))
    }

    @Test
    fun `coerceVolume clamps above one to one`() {
        assertEquals(1f, AudioConfig.coerceVolume(1.4f))
    }

    @Test
    fun `coerceVolume passes through in-range values`() {
        assertEquals(0.35f, AudioConfig.coerceVolume(0.35f))
    }

    @Test
    fun `zen mode maps to zen track`() {
        assertEquals(MusicTrack.ZEN, AudioConfig.trackForMode(isZen = true))
    }

    @Test
    fun `non-zen mode maps to adventure track`() {
        assertEquals(MusicTrack.ADVENTURE, AudioConfig.trackForMode(isZen = false))
    }
}
