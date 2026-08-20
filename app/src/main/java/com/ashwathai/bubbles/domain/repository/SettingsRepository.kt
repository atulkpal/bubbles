package com.ashwathai.bubbles.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val soundEnabled: StateFlow<Boolean>
    val hapticsEnabled: StateFlow<Boolean>
    val reducedMotion: StateFlow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
}