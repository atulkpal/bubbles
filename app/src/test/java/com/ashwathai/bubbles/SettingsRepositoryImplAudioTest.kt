package com.ashwathai.bubbles

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ashwathai.bubbles.data.local.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SettingsRepositoryImplAudioTest {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataStoreFile = File.createTempFile("settings_audio_test", ".preferences_pb")
    private val sharedDataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    private fun newRepo(): SettingsRepositoryImpl = SettingsRepositoryImpl(sharedDataStore)

    @Test
    fun `music is enabled by default`() = runBlocking {
        assertEquals(true, newRepo().musicEnabled.value)
    }

    @Test
    fun `sfx volume defaults to full`() = runBlocking {
        assertEquals(1.0f, newRepo().sfxVolume.value)
    }

    @Test
    fun `music volume defaults to subtle`() = runBlocking {
        assertEquals(0.35f, newRepo().musicVolume.value)
    }

    @Test
    fun `music toggle round-trips`() = runBlocking {
        val repo = newRepo()

        repo.setMusicEnabled(false)
        assertEquals(false, repo.musicEnabled.value)

        repo.setMusicEnabled(true)
        assertEquals(true, repo.musicEnabled.value)
    }

    @Test
    fun `sfx volume round-trips`() = runBlocking {
        val repo = newRepo()

        repo.setSfxVolume(0.6f)
        assertEquals(0.6f, repo.sfxVolume.value)
    }

    @Test
    fun `music volume round-trips`() = runBlocking {
        val repo = newRepo()

        repo.setMusicVolume(0.2f)
        assertEquals(0.2f, repo.musicVolume.value)
    }

    @Test
    fun `volumes are coerced into range`() = runBlocking {
        val repo = newRepo()

        repo.setSfxVolume(2.5f)
        assertEquals(1.0f, repo.sfxVolume.value)

        repo.setMusicVolume(-1f)
        assertEquals(0f, repo.musicVolume.value)
    }
}
