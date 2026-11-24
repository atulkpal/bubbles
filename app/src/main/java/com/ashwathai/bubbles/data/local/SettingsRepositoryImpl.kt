package com.ashwathai.bubbles.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.ashwathai.bubbles.domain.model.AudioConfig
import com.ashwathai.bubbles.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    private val HAPTICS_KEY = booleanPreferencesKey("haptics_enabled")
    private val REDUCED_MOTION_KEY = booleanPreferencesKey("reduced_motion")
    private val ADS_REMOVED_KEY = booleanPreferencesKey("ads_removed")

    private val _soundEnabled = MutableStateFlow(true)
    override val soundEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    override val hapticsEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _reducedMotion = MutableStateFlow(false)
    override val reducedMotion: kotlinx.coroutines.flow.StateFlow<Boolean> = _reducedMotion.asStateFlow()

    private val _adsRemoved = MutableStateFlow(false)
    override val adsRemoved: kotlinx.coroutines.flow.StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private val MUSIC_KEY = booleanPreferencesKey("music_enabled")
    private val SFX_VOLUME_KEY = floatPreferencesKey("sfx_volume")
    private val MUSIC_VOLUME_KEY = floatPreferencesKey("music_volume")

    private val _musicEnabled = MutableStateFlow(true)
    override val musicEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _sfxVolume = MutableStateFlow(AudioConfig.DEFAULT_SFX_VOLUME)
    override val sfxVolume: kotlinx.coroutines.flow.StateFlow<Float> = _sfxVolume.asStateFlow()

    private val _musicVolume = MutableStateFlow(AudioConfig.DEFAULT_MUSIC_VOLUME)
    override val musicVolume: kotlinx.coroutines.flow.StateFlow<Float> = _musicVolume.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.firstOrNull()
            if (prefs != null) {
                _soundEnabled.value = prefs[SOUND_KEY] ?: true
                _hapticsEnabled.value = prefs[HAPTICS_KEY] ?: true
                _reducedMotion.value = prefs[REDUCED_MOTION_KEY] ?: false
                _adsRemoved.value = prefs[ADS_REMOVED_KEY] ?: false
                _musicEnabled.value = prefs[MUSIC_KEY] ?: true
                _sfxVolume.value = AudioConfig.coerceVolume(prefs[SFX_VOLUME_KEY] ?: AudioConfig.DEFAULT_SFX_VOLUME)
                _musicVolume.value = AudioConfig.coerceVolume(prefs[MUSIC_VOLUME_KEY] ?: AudioConfig.DEFAULT_MUSIC_VOLUME)
            }
        }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_KEY] = enabled
        }
        _soundEnabled.value = enabled
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTICS_KEY] = enabled
        }
        _hapticsEnabled.value = enabled
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REDUCED_MOTION_KEY] = enabled
        }
        _reducedMotion.value = enabled
    }

    override suspend fun setAdsRemoved(removed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ADS_REMOVED_KEY] = removed
        }
        _adsRemoved.value = removed
    }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MUSIC_KEY] = enabled
        }
        _musicEnabled.value = enabled
    }

    override suspend fun setSfxVolume(volume: Float) {
        val coerced = AudioConfig.coerceVolume(volume)
        dataStore.edit { preferences ->
            preferences[SFX_VOLUME_KEY] = coerced
        }
        _sfxVolume.value = coerced
    }

    override suspend fun setMusicVolume(volume: Float) {
        val coerced = AudioConfig.coerceVolume(volume)
        dataStore.edit { preferences ->
            preferences[MUSIC_VOLUME_KEY] = coerced
        }
        _musicVolume.value = coerced
    }
}