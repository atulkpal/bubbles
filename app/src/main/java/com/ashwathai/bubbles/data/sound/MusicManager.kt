package com.ashwathai.bubbles.data.sound

import android.content.Context
import android.media.MediaPlayer
import com.ashwathai.bubbles.R
import com.ashwathai.bubbles.domain.model.AudioConfig
import com.ashwathai.bubbles.domain.model.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ambient background music with crossfading track switches.
 *
 * Two [MediaPlayer] slots (A/B) loop gaplessly via `setLooping(true)`;
 * every track change ramps the old slot down and the new slot up over
 * [AudioConfig.CROSSFADE_MS] — never a hard cut. All playback honors
 * [musicEnabled] and [volume].
 */
class MusicManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val players = arrayOfNulls<MediaPlayer>(2)
    private var activeIndex = 0
    private var currentTrack: MusicTrack? = null
    private var pausedForBackground = false
    private var crossfadeJob: Job? = null

    var musicEnabled = true
        private set
    private var volume: Float = AudioConfig.DEFAULT_MUSIC_VOLUME

    private fun resFor(track: MusicTrack): Int = when (track) {
        MusicTrack.ADVENTURE -> R.raw.music_adventure
        MusicTrack.ZEN -> R.raw.music_zen
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            pauseActive()
        } else if (!pausedForBackground) {
            currentTrack?.let { resumeActive() }
        }
    }

    fun setVolume(v: Float) {
        volume = AudioConfig.coerceVolume(v)
        activePlayer()?.setVolume(volume, volume)
    }

    /** Play [track], crossfading from whatever is currently playing. No-op if same. */
    fun play(track: MusicTrack) {
        if (!musicEnabled) {
            currentTrack = track
            return
        }
        val active = activePlayer()
        if (track == currentTrack && active?.isPlaying == true) {
            active.setVolume(volume, volume)
            return
        }
        crossfadeTo(track)
    }

    /** Pause for game pause / app background. Remembers the track for [resume]. */
    fun pause() {
        crossfadeJob?.cancel()
        pauseActive()
        pausedForBackground = true
    }

    /** Resume after [pause] if music is enabled. */
    fun resume() {
        pausedForBackground = false
        if (musicEnabled) resumeActive()
    }

    /** Fade out and release everything. */
    fun stop() {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            fadeOut(activePlayer())
            releaseAll()
            currentTrack = null
        }
    }

    fun release() {
        crossfadeJob?.cancel()
        releaseAll()
        currentTrack = null
        scope.cancel()
    }

    private fun activePlayer(): MediaPlayer? = players[activeIndex]

    private fun pauseActive() {
        activePlayer()?.let { if (it.isPlaying) it.pause() }
    }

    private fun resumeActive() {
        activePlayer()?.let {
            it.setVolume(volume, volume)
            if (!it.isPlaying) it.start()
        }
    }

    private fun crossfadeTo(track: MusicTrack) {
        crossfadeJob?.cancel()
        currentTrack = track
        pausedForBackground = false
        crossfadeJob = scope.launch {
            val oldPlayer = activePlayer()
            val nextIndex = 1 - activeIndex
            players[nextIndex]?.release()
            val next = MediaPlayer.create(context, resFor(track)) ?: return@launch
            next.isLooping = true
            next.setVolume(0f, 0f)
            next.start()
            players[nextIndex] = next
            activeIndex = nextIndex

            val steps = AudioConfig.CROSSFADE_STEPS
            val stepMs = AudioConfig.CROSSFADE_MS / steps
            for (i in 1..steps) {
                val f = i / steps.toFloat()
                next.setVolume(volume * f, volume * f)
                oldPlayer?.let {
                    val v = volume * (1f - f)
                    try {
                        it.setVolume(v, v)
                    } catch (_: IllegalStateException) {
                        // released mid-fade; nothing to duck
                    }
                }
                delay(stepMs)
            }
            try {
                oldPlayer?.stop()
            } catch (_: IllegalStateException) {
                // already stopped/released
            }
            oldPlayer?.release()
        }
    }

    private suspend fun fadeOut(player: MediaPlayer?) {
        if (player == null) return
        val steps = AudioConfig.CROSSFADE_STEPS
        val stepMs = AudioConfig.CROSSFADE_MS / steps
        for (i in steps - 1 downTo 0) {
            val v = volume * (i / steps.toFloat())
            try {
                player.setVolume(v, v)
            } catch (_: IllegalStateException) {
                return
            }
            delay(stepMs)
        }
    }

    private fun releaseAll() {
        players.forEachIndexed { i, p ->
            try {
                p?.stop()
            } catch (_: IllegalStateException) {
                // already stopped; still release below
            }
            p?.release()
            players[i] = null
        }
    }
}
