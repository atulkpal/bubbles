package com.ashwathai.bubbles.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ashwathai.bubbles.R
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val popSounds = IntArray(3) { -1 }
    private var bigPopSound: Int = -1
    private var splitSound: Int = -1
    private var powerUpSound: Int = -1
    private var levelUpSound: Int = -1
    private var gameOverSound: Int = -1
    private var comboSound: Int = -1
    private var bossSound: Int = -1
    private var crackSound: Int = -1
    private var coinSound: Int = -1
    private var soundsLoaded = 0
    private val totalSounds = 11

    var soundEnabled = true
        private set
    var hapticsEnabled = true
        private set

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        loadSounds()
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    private fun loadSounds() {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == 0) {
                soundsLoaded++
            } else {
                Log.w("SoundManager", "Failed to load sound: $soundId")
            }
        }

        popSounds[0] = soundPool.load(context, R.raw.pop, 1)
        popSounds[1] = soundPool.load(context, R.raw.split, 1)
        popSounds[2] = soundPool.load(context, R.raw.pop, 1)
        bigPopSound = soundPool.load(context, R.raw.levelup, 1)
        splitSound = soundPool.load(context, R.raw.split, 1)
        powerUpSound = soundPool.load(context, R.raw.powerup, 1)
        levelUpSound = soundPool.load(context, R.raw.levelup, 1)
        gameOverSound = soundPool.load(context, R.raw.gameover, 1)
        comboSound = soundPool.load(context, R.raw.powerup, 1)
        bossSound = soundPool.load(context, R.raw.gameover, 1)
        crackSound = soundPool.load(context, R.raw.pop, 1)
        coinSound = soundPool.load(context, R.raw.powerup, 1)
    }

    fun playPop() {
        play(popSounds[Random.nextInt(3)], 1f, 0.9f + Random.nextFloat() * 0.25f)
    }

    fun playBigPop() {
        play(bigPopSound, 1f, 0.7f)
    }

    fun playSplit() {
        play(splitSound, 0.7f, 1f)
    }

    fun playPowerUp() {
        play(powerUpSound, 1f, 1.2f)
    }

    fun playCombo() {
        play(comboSound, 1f, 1.4f)
    }

    fun playBoss() {
        play(bossSound, 1f, 0.6f)
    }

    fun playCrack() {
        play(crackSound, 0.8f, 0.8f)
    }

    fun playCoin() {
        play(coinSound, 0.9f, 1.6f)
    }

    fun playLevelUp() {
        play(levelUpSound, 1f, 1f)
    }

    fun playGameOver() {
        play(gameOverSound, 1f, 1f)
    }

    private fun play(soundId: Int, volume: Float, rate: Float) {
        if (!soundEnabled) return
        if (soundId != -1) {
            soundPool.play(soundId, volume, volume, 1, 0, rate)
        }
    }

    fun vibrate(durationMs: Long) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    fun release() {
        soundPool.release()
    }
}