package com.ashwathai.bubbles.domain.model

/** Which ambient loop to play. ADVENTURE covers menu, Adventure and Daily. */
enum class MusicTrack { ADVENTURE, ZEN }

/** Audio design tokens — volumes, crossfade, track mapping. Pure and unit-tested. */
object AudioConfig {
    const val DEFAULT_SFX_VOLUME = 1.0f
    const val DEFAULT_MUSIC_VOLUME = 0.35f

    /** Every track switch crossfades over this duration — never a hard cut. */
    const val CROSSFADE_MS = 1500L

    /** Volume ramp granularity for the crossfade. */
    const val CROSSFADE_STEPS = 30

    fun coerceVolume(volume: Float): Float = volume.coerceIn(0f, 1f)

    fun trackForMode(isZen: Boolean): MusicTrack =
        if (isZen) MusicTrack.ZEN else MusicTrack.ADVENTURE
}
