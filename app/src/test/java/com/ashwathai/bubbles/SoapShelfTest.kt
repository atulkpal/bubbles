package com.ashwathai.bubbles

import com.ashwathai.bubbles.domain.model.SoapShelf
import com.ashwathai.bubbles.domain.model.Soaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Relaunch step 4: jar, unlock order, soap of the day, kind streak. */
class SoapShelfTest {

    private val cap = SoapShelf.JAR_CAPACITY

    @Test
    fun `filling the jar unlocks the next soap in order and keeps the overflow`() {
        val r = SoapShelf.addDrops(cap - 10, emptySet(), 25)
        assertEquals(listOf(Soaps.LAVENDER_DUSK.id), r.newlyUnlocked)
        assertEquals(15, r.jarDrops)
        assertTrue(Soaps.CLEAR_NIGHT.id in r.unlocked)
    }

    @Test
    fun `a big head start can unlock several soaps at once`() {
        val r = SoapShelf.addDrops(0, emptySet(), cap * 2 + 5)
        assertEquals(listOf(Soaps.LAVENDER_DUSK.id, Soaps.MONSOON_GLASS.id), r.newlyUnlocked)
        assertEquals(5, r.jarDrops)
    }

    @Test
    fun `once every soap is owned the jar rests full`() {
        val all = Soaps.ALL.map { it.id }.toSet()
        val r = SoapShelf.addDrops(cap, all, 999)
        assertTrue(r.newlyUnlocked.isEmpty())
        assertEquals(cap, r.jarDrops)
    }

    @Test
    fun `soap of the day is the same for a date, varies across dates, never the starter`() {
        assertEquals(SoapShelf.soapOfTheDay("2026-11-24"), SoapShelf.soapOfTheDay("2026-11-24"))
        val week = (1..14).map { SoapShelf.soapOfTheDay("2026-12-%02d".format(it)) }.toSet()
        assertTrue("rotates", week.size >= 3)
        assertTrue(week.none { it == Soaps.CLEAR_NIGHT })
    }

    @Test
    fun `a locked soap can be used only on its free day`() {
        val date = "2026-11-24"
        val free = SoapShelf.soapOfTheDay(date)
        assertEquals(free, SoapShelf.effectiveSoap(free.id, emptySet(), date))
        val other = Soaps.ALL.drop(1).first { it != free }
        assertEquals(Soaps.CLEAR_NIGHT, SoapShelf.effectiveSoap(other.id, emptySet(), date))
        assertEquals(other, SoapShelf.effectiveSoap(other.id, setOf(other.id), date))
    }

    @Test
    fun `streak counts play days and a missed day never resets it`() {
        var s = SoapShelf.recordPlayDay(0, "", "2026-11-24")
        assertEquals(1, s.first)
        s = SoapShelf.recordPlayDay(s.first, s.second, "2026-11-24")
        assertEquals("same day counts once", 1, s.first)
        s = SoapShelf.recordPlayDay(s.first, s.second, "2026-11-30") // skipped days
        assertEquals(2, s.first)
    }

    @Test
    fun `migration gives returning players a capped head start`() {
        assertEquals(0, SoapShelf.migrationDrops(0))
        assertEquals(100, SoapShelf.migrationDrops(400))
        assertEquals(3 * cap, SoapShelf.migrationDrops(1_000_000))
        assertNotEquals(0, SoapShelf.migrationDrops(41_045))
    }
}
