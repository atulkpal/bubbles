package com.ashwathai.bubbles

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ashwathai.bubbles.data.local.EconomyRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EconomyRepositoryImplAchievementsTest {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataStoreFile = File.createTempFile("eco_achievements_test", ".preferences_pb")
    private val sharedDataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    // DataStore prohibits two instances on the same file in one process, so every
    // repo wraps the same DataStore instance — a fresh EconomyRepositoryImpl still
    // exercises the real disk-load path (init → load() → dataStore.data).

    private fun newRepo(): EconomyRepositoryImpl = EconomyRepositoryImpl(sharedDataStore)

    @Test
    fun `unlockAchievements persists ids to state`() = runBlocking {
        val repo = newRepo()

        repo.unlockAchievements(setOf("pops_100", "combo_10"))

        val unlocked = repo.state.value.unlockedAchievements
        assertEquals(setOf("pops_100", "combo_10"), unlocked)
    }

    @Test
    fun `unlocked achievements survive a fresh load from DataStore`() = runBlocking {
        // Arrange: write achievements with one repository instance
        val writer = newRepo()
        writer.unlockAchievements(setOf("level_5", "prestige_1"))

        // Act: a brand-new repository instance must load them from DataStore
        val reader = newRepo()

        // Give the async load() a moment to complete
        var loaded = reader.state.value
        var attempts = 0
        while (loaded.unlockedAchievements.isEmpty() && attempts < 50) {
            Thread.sleep(20)
            loaded = reader.state.value
            attempts++
        }

        assertTrue("expected unlockedAchievements to load, got ${loaded.unlockedAchievements}",
            loaded.unlockedAchievements.containsAll(setOf("level_5", "prestige_1")))
    }

    @Test
    fun `unlockAchievements is additive and does not drop prior unlocks`() = runBlocking {
        val repo = newRepo()

        repo.unlockAchievements(setOf("pops_100"))
        repo.unlockAchievements(setOf("pops_1000"))

        assertEquals(setOf("pops_100", "pops_1000"), repo.state.value.unlockedAchievements)
    }

    @Test
    fun `empty unlock set is a no-op`() = runBlocking {
        val repo = newRepo()

        repo.unlockAchievements(emptySet())

        assertTrue(repo.state.value.unlockedAchievements.isEmpty())
    }
}
