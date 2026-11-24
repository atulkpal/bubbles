package com.ashwathai.bubbles.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val soundEnabled: StateFlow<Boolean>
    val hapticsEnabled: StateFlow<Boolean>
    val reducedMotion: StateFlow<Boolean>
    val adsRemoved: StateFlow<Boolean>
    val musicEnabled: StateFlow<Boolean>
    val sfxVolume: StateFlow<Float>
    val musicVolume: StateFlow<Float>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setAdsRemoved(removed: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setSfxVolume(volume: Float)
    suspend fun setMusicVolume(volume: Float)
}