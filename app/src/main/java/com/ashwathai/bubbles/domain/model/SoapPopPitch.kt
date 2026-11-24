package com.ashwathai.bubbles.domain.model

/**
 * Pop pitch (relaunch spec §3/§6): big bubbles pop low, small ones high, and quick
 * successive pops climb a pentatonic scale so popping makes a little melody.
 * Returns a SoundPool playback rate (valid range 0.5–2.0).
 */
object SoapPopPitch {
    /** Major pentatonic ratios: do re mi so la (do'). */
    val PENTATONIC = floatArrayOf(1f, 9f / 8f, 5f / 4f, 3f / 2f, 5f / 3f, 2f)

    /** Pops closer together than this continue the phrase; a pause resets it. */
    const val PHRASE_GAP_MS = 1500L

    private const val BIG_RATE = 0.8f
    private const val SMALL_RATE = 1.15f

    fun sizeRate(radius: Float, minRadius: Float, maxRadius: Float): Float {
        if (maxRadius <= minRadius) return 1f
        val f = ((radius - minRadius) / (maxRadius - minRadius)).coerceIn(0f, 1f)
        return SMALL_RATE + (BIG_RATE - SMALL_RATE) * f
    }

    fun rate(radius: Float, minRadius: Float, maxRadius: Float, noteStep: Int): Float {
        var r = sizeRate(radius, minRadius, maxRadius) * PENTATONIC[Math.floorMod(noteStep, PENTATONIC.size)]
        while (r > 2f) r /= 2f
        return r.coerceIn(0.5f, 2f)
    }

    /** Next note index: continues while pops are close together, otherwise starts over. */
    fun nextStep(previousStep: Int, lastPopAtMs: Long, nowMs: Long): Int =
        if (lastPopAtMs > 0 && nowMs - lastPopAtMs <= PHRASE_GAP_MS) previousStep + 1 else 0
}
