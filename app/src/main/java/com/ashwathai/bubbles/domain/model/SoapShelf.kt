package com.ashwathai.bubbles.domain.model

import androidx.compose.ui.graphics.Color

/**
 * A soap (relaunch spec §7): what the bubbles' film looks like, the room they float in,
 * and a slight pitch character for its pops.
 */
data class Soap(
    val id: String,
    val name: String,
    /** Bubble tints (also the colours that chain in Challenge). */
    val palette: List<Color>,
    /** Thin-film interference bands, looped (first == last). */
    val film: List<Color>,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val lamp: Color,
    /** Multiplies every pop's playback rate: each soap sounds a little different. */
    val pitch: Float
)

object Soaps {
    val CLEAR_NIGHT = Soap(
        "CLEAR_NIGHT", "Clear Night",
        palette = listOf(Color(0xFFFF5FD2), Color(0xFF4FD8FF), Color(0xFFFFD36B), Color(0xFF8A6BFF), Color(0xFF7CF5C4)),
        film = listOf(Color(0xFFFF5FD2), Color(0xFFFFD36B), Color(0xFF7CF5C4), Color(0xFF4FD8FF), Color(0xFF8A6BFF), Color(0xFFFF5FD2)),
        backgroundTop = Color(0xFF0E1022), backgroundBottom = Color(0xFF1C1B3A), lamp = Color(0xFFFFC98A), pitch = 1f
    )
    val LAVENDER_DUSK = Soap(
        "LAVENDER_DUSK", "Lavender Dusk",
        palette = listOf(Color(0xFFC9A7FF), Color(0xFFFF9AD5), Color(0xFFB38CFF), Color(0xFFFFC2E8)),
        film = listOf(Color(0xFFC9A7FF), Color(0xFFFF9AD5), Color(0xFFFFE1F4), Color(0xFF9C7BFF), Color(0xFFC9A7FF)),
        backgroundTop = Color(0xFF160D2B), backgroundBottom = Color(0xFF2C1A45), lamp = Color(0xFFFFB8E0), pitch = 1.06f
    )
    val MONSOON_GLASS = Soap(
        "MONSOON_GLASS", "Monsoon Glass",
        palette = listOf(Color(0xFF6FD6E8), Color(0xFF8FB8D8), Color(0xFFB8E2F2), Color(0xFF5FA8C8)),
        film = listOf(Color(0xFF6FD6E8), Color(0xFFB8E2F2), Color(0xFF8FB8D8), Color(0xFF5F7FC8), Color(0xFF6FD6E8)),
        backgroundTop = Color(0xFF0A161E), backgroundBottom = Color(0xFF14303A), lamp = Color(0xFFBFE6FF), pitch = 0.92f
    )
    val CHAI_STEAM = Soap(
        "CHAI_STEAM", "Chai Steam",
        palette = listOf(Color(0xFFFFB46B), Color(0xFFE89A5F), Color(0xFFFFE0B0), Color(0xFFD9825A)),
        film = listOf(Color(0xFFFFB46B), Color(0xFFFFE0B0), Color(0xFFE89A5F), Color(0xFFC86B5A), Color(0xFFFFB46B)),
        backgroundTop = Color(0xFF1A100A), backgroundBottom = Color(0xFF33210F), lamp = Color(0xFFFFC27A), pitch = 0.88f
    )
    val FIREFLY_NIGHT = Soap(
        "FIREFLY_NIGHT", "Firefly Night",
        palette = listOf(Color(0xFFD9FF6B), Color(0xFF8CFF9E), Color(0xFFFFE66B), Color(0xFF6BFFC8)),
        film = listOf(Color(0xFFD9FF6B), Color(0xFFFFE66B), Color(0xFF8CFF9E), Color(0xFF3FC8A0), Color(0xFFD9FF6B)),
        backgroundTop = Color(0xFF06120C), backgroundBottom = Color(0xFF10261A), lamp = Color(0xFFD9FF8A), pitch = 1.12f
    )
    val SEA_GLASS = Soap(
        "SEA_GLASS", "Sea Glass",
        palette = listOf(Color(0xFF7FFFE0), Color(0xFF9FE8FF), Color(0xFFB8FFD0), Color(0xFF5FD8C8)),
        film = listOf(Color(0xFF7FFFE0), Color(0xFFB8FFD0), Color(0xFF9FE8FF), Color(0xFF5FB8E8), Color(0xFF7FFFE0)),
        backgroundTop = Color(0xFF081719), backgroundBottom = Color(0xFF123236), lamp = Color(0xFFA8FFF0), pitch = 1f
    )
    val MARIGOLD = Soap(
        "MARIGOLD", "Marigold",
        palette = listOf(Color(0xFFFFA530), Color(0xFFFFCC33), Color(0xFFFF7A3D), Color(0xFFFFE08A)),
        film = listOf(Color(0xFFFFA530), Color(0xFFFFE08A), Color(0xFFFF7A3D), Color(0xFFFF5FA0), Color(0xFFFFA530)),
        backgroundTop = Color(0xFF190C06), backgroundBottom = Color(0xFF341808), lamp = Color(0xFFFFB347), pitch = 0.96f
    )

    /** Exclusive to Supporter Pack owners; never in the jar unlock order. */
    val MOONLIT_PEARL = Soap(
        "MOONLIT_PEARL", "Moonlit Pearl",
        palette = listOf(Color(0xFFF2F0FF), Color(0xFFD6E4FF), Color(0xFFFFE9F2), Color(0xFFE2FFF6)),
        film = listOf(Color(0xFFFFFFFF), Color(0xFFD6E4FF), Color(0xFFFFD6EC), Color(0xFFD6FFF1), Color(0xFFFFFFFF)),
        backgroundTop = Color(0xFF0B0D1A), backgroundBottom = Color(0xFF1A1D33), lamp = Color(0xFFE8EEFF), pitch = 1.03f
    )

    /** Starter first, then the unlock order. */
    val ALL = listOf(CLEAR_NIGHT, LAVENDER_DUSK, MONSOON_GLASS, CHAI_STEAM, FIREFLY_NIGHT, SEA_GLASS, MARIGOLD)

    fun byId(id: String?): Soap = (ALL + MOONLIT_PEARL).firstOrNull { it.id == id } ?: CLEAR_NIGHT
}

/** Jar, unlocks, soap of the day and the kind streak. Pure rules (unit-tested). */
object SoapShelf {
    /** Drops (= pops) to fill one jar. ~2 days of normal play per new soap. */
    const val JAR_CAPACITY = 250

    data class FillResult(val jarDrops: Int, val unlocked: Set<String>, val newlyUnlocked: List<String>)

    /** Adds [drops] to the jar; every full jar unlocks the next locked soap, overflow carries on. */
    fun addDrops(jarDrops: Int, unlocked: Set<String>, drops: Int): FillResult {
        var jar = jarDrops + drops.coerceAtLeast(0)
        val owned = unlocked.toMutableSet().apply { add(Soaps.CLEAR_NIGHT.id) }
        val newly = mutableListOf<String>()
        while (jar >= JAR_CAPACITY) {
            val next = Soaps.ALL.firstOrNull { it.id !in owned } ?: break
            jar -= JAR_CAPACITY
            owned += next.id
            newly += next.id
        }
        // All soaps owned: the jar rests full instead of growing forever.
        if (Soaps.ALL.all { it.id in owned }) jar = jar.coerceAtMost(JAR_CAPACITY)
        return FillResult(jar, owned, newly)
    }

    fun nextLocked(unlocked: Set<String>): Soap? = Soaps.ALL.firstOrNull { it.id !in unlocked && it != Soaps.CLEAR_NIGHT }

    /** Same soap for everyone on a given date (yyyy-MM-dd); never the starter. */
    fun soapOfTheDay(date: String): Soap {
        val pool = Soaps.ALL.drop(1)
        val h = date.fold(17) { acc, c -> acc * 31 + c.code }
        return pool[Math.floorMod(h, pool.size)]
    }

    /** The selected soap is usable if owned, or if it's today's free soap; otherwise the starter. */
    fun effectiveSoap(selectedId: String, unlocked: Set<String>, date: String, supporter: Boolean = false): Soap {
        val selected = Soaps.byId(selectedId)
        val usable = selected == Soaps.CLEAR_NIGHT || selected.id in unlocked || selected == soapOfTheDay(date) ||
            (selected == Soaps.MOONLIT_PEARL && supporter)
        return if (usable) selected else Soaps.CLEAR_NIGHT
    }

    /**
     * Kind streak: counts days played. A missed day pauses it (never resets to zero).
     * Returns the new (daysPlayed, lastPlayDate).
     */
    fun recordPlayDay(daysPlayed: Int, lastPlayDate: String, today: String): Pair<Int, String> =
        if (lastPlayDate == today) daysPlayed to lastPlayDate else (daysPlayed + 1) to today

    /** One-time head start for players from before the relaunch: coins → drops (≤ 3 soaps). */
    fun migrationDrops(coins: Int): Int = (coins / 4).coerceIn(0, 3 * JAR_CAPACITY)
}
