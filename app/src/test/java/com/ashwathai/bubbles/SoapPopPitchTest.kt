package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.SoapPopPitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Relaunch step 3c: pop pitch by size + pentatonic phrase. */
class SoapPopPitchTest {

    private val min = 74f
    private val max = 134f

    @Test
    fun `big bubbles pop lower than small ones`() {
        assertTrue(SoapPopPitch.rate(max, min, max, 0) < SoapPopPitch.rate(min, min, max, 0))
    }

    @Test
    fun `rate always stays inside SoundPool's 0_5 to 2_0 range`() {
        for (step in -3..40) for (r in listOf(10f, min, 100f, max, 500f)) {
            val rate = SoapPopPitch.rate(r, min, max, step)
            assertTrue("r=$r step=$step rate=$rate", rate in 0.5f..2f)
        }
    }

    @Test
    fun `quick pops climb the scale, a pause starts the phrase again`() {
        assertEquals(0, SoapPopPitch.nextStep(0, 0L, 10_000L))
        assertEquals(1, SoapPopPitch.nextStep(0, 10_000L, 10_400L))
        assertEquals(2, SoapPopPitch.nextStep(1, 10_400L, 11_000L))
        assertEquals(0, SoapPopPitch.nextStep(2, 11_000L, 11_000L + SoapPopPitch.PHRASE_GAP_MS + 1))
    }

    @Test
    fun `successive notes of one bubble size follow the pentatonic ratios`() {
        val base = SoapPopPitch.rate(100f, min, max, 0)
        assertEquals(base * 9f / 8f, SoapPopPitch.rate(100f, min, max, 1), 0.0001f)
        assertEquals(base * 5f / 4f, SoapPopPitch.rate(100f, min, max, 2), 0.0001f)
    }
}
