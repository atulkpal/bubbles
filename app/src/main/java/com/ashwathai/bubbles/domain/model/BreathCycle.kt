package com.ashwathai.bubbles.domain.model

/**
 * Breathe mode (relaunch spec §4): one guided breath per bubble.
 * Inhale 4 s while it inflates, hold 2 s, exhale 6 s while it drifts up, then pop it
 * (or it pops itself softly after a short wait). Pure timing rules, unit-tested.
 */
object BreathCycle {
    const val INHALE_SEC = 4f
    const val HOLD_SEC = 2f
    const val EXHALE_SEC = 6f
    /** Time the bubble waits to be popped before popping itself. */
    const val POP_WINDOW_SEC = 3f
    const val CYCLE_SEC = INHALE_SEC + HOLD_SEC + EXHALE_SEC + POP_WINDOW_SEC

    val SESSION_MINUTES = listOf(1, 3, 5)

    enum class Phase(val guide: String) {
        INHALE("Breathe in…"),
        HOLD("Hold…"),
        EXHALE("Breathe out…"),
        POP("Pop it gently"),
        DONE("")
    }

    data class State(val phase: Phase, val progress: Float)

    /** Phase and progress (0..1 within the phase) at [t] seconds into a cycle. */
    fun stateAt(t: Float): State = when {
        t < 0f -> State(Phase.INHALE, 0f)
        t < INHALE_SEC -> State(Phase.INHALE, t / INHALE_SEC)
        t < INHALE_SEC + HOLD_SEC -> State(Phase.HOLD, (t - INHALE_SEC) / HOLD_SEC)
        t < INHALE_SEC + HOLD_SEC + EXHALE_SEC -> State(Phase.EXHALE, (t - INHALE_SEC - HOLD_SEC) / EXHALE_SEC)
        t < CYCLE_SEC -> State(Phase.POP, (t - INHALE_SEC - HOLD_SEC - EXHALE_SEC) / POP_WINDOW_SEC)
        else -> State(Phase.DONE, 1f)
    }

    /** The breath bubble can be popped only once the breath is complete. */
    fun canPop(t: Float): Boolean = stateAt(t).phase == Phase.POP

    /** Vertical position as a fraction of screen height: rests low, rises during the exhale. */
    fun heightFraction(t: Float): Float {
        val s = stateAt(t)
        return when (s.phase) {
            Phase.INHALE, Phase.HOLD -> 0.62f
            Phase.EXHALE -> 0.62f - 0.24f * easeInOut(s.progress)
            Phase.POP, Phase.DONE -> 0.38f
        }
    }

    /** 0..1 inflation: grows through the inhale, stays full afterwards. */
    fun inflation(t: Float): Float {
        val s = stateAt(t)
        return if (s.phase == Phase.INHALE) easeInOut(s.progress) else 1f
    }

    /** Whole breaths that fit in a session of [minutes]. */
    fun breathsIn(minutes: Int): Int = ((minutes * 60f) / CYCLE_SEC).toInt().coerceAtLeast(1)

    private fun easeInOut(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }
}
