package com.ashwathai.bubbles

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ashwathai.bubbles.data.local.EconomyRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * v1.3 economy hardening: overlapping coin writes must never lose an update, and an
 * achievement must be paid exactly once even when two evaluations race.
 */
class EconomyRepositoryImplConcurrencyTest {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataStoreFile = File.createTempFile("eco_concurrency_test", ".preferences_pb")
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    private suspend fun newLoadedRepo(): EconomyRepositoryImpl {
        val repo = EconomyRepositoryImpl(dataStore)
        dataStore.data.first()
        delay(200) // let the async initial load settle before writing
        return repo
    }

    @Test
    fun `parallel addCoins never loses an update`() = runBlocking {
        val repo = newLoadedRepo()
        (1..200).map { scope.launch { repo.addCoins(1) } }.forEach { it.join() }
        assertEquals(200, repo.state.value.coins)
        assertEquals(200, EconomyRepositoryImpl(dataStore).also { delay(200) }.state.value.coins)
    }

    @Test
    fun `parallel spendCoins never overspends`() = runBlocking {
        val repo = newLoadedRepo()
        repo.addCoins(100)
        val results = (1..10).map { scope.async { repo.spendCoins(30) } }.awaitAll()
        assertEquals(3, results.count { it })
        assertEquals(10, repo.state.value.coins)
    }

    @Test
    fun `achievement is unlocked and paid exactly once under a race`() = runBlocking {
        val repo = newLoadedRepo()
        val results = (1..5).map {
            scope.async { repo.unlockAchievements(setOf("pops_100"), rewardPerUnlock = 25) }
        }.awaitAll()
        assertEquals(1, results.count { it.isNotEmpty() })
        assertEquals(25, repo.state.value.coins)
        assertTrue("pops_100" in repo.state.value.unlockedAchievements)
    }

    @Test
    fun `unlockAchievements returns only the newly unlocked ids`() = runBlocking {
        val repo = newLoadedRepo()
        assertEquals(setOf("a"), repo.unlockAchievements(setOf("a"), 25))
        assertEquals(setOf("b"), repo.unlockAchievements(setOf("a", "b"), 25))
        assertEquals(emptySet<String>(), repo.unlockAchievements(setOf("a", "b"), 25))
        assertEquals(50, repo.state.value.coins)
    }
}
