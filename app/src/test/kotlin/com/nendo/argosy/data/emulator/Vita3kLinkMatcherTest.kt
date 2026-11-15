package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.InstalledApp
import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.LinkGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Vita3kLinkMatcherTest {

    private fun game(id: Long, title: String, rommFileName: String? = null, files: List<String> = emptyList(), titleId: String? = null) =
        LinkGame(id, title, titleId, null, rommFileName, files)

    private val thorGames = listOf(
        game(1, "FIFA 15", "FIFA 15 [PCSB00604] [EUR].zip"),
        game(2, "MLB 14: The Show", "MLB® 14 The Show™ [PCSA00115] [USA].zip"),
        game(3, "Ys VIII: Lacrimosa of Dana", "Ys VIII - Lacrimosa of Dana", listOf("EP1063-PCSB01128_00-YSVIIIGAME000000_bg_1_df27f9b07dc34a2b78c6d091db9ba63e01c1fc68.pkg", "EP1063-PCSB01128_00-YSVIIIDLC00L0000_bg_1_2e7933e7d22839edb966fb8cc2dd115d7501e658.pkg")),
        game(4, "Catherine: Full Body", "Catherine - Full Body", listOf("JP0005-PCSG01179_00-ZZZZZZZZZZZZZZZZ_bg_1_5bab26208abd25132ea925bb4dd87def61265134.pkg")),
        game(5, "Persona 4 Golden", "Persona 4 Golden", listOf("LTncdecBVsTHeSLEZFxxfhjtwnXrTlNdtKcGdjlCsGnkqFcmlxNpAIQlJerDXONr.pkg", "work.bin")),
        game(6, "Danganronpa: Trigger Happy Havoc", "Danganronpa - Trigger Happy Havoc", listOf("IHqWvWZPbvHKzxJHCFyJEuHqIYPhuGzDvGtTMNKXWRhuXxAYGdaxLzBvmjJzzwrQ.pkg")),
        game(7, "Danganronpa 2: Goodbye Despair", "Danganronpa 2 - Goodbye Despair"),
        game(8, "Virtua Tennis 4: World Tour Edition", "Virtua Tennis 4 World Tour Edition"),
        game(9, "Fez", "FEZ [PCSB00456] [EUR].zip", titleId = "PCSB00456"),
        game(10, "Freedom Wars", "Freedom Wars")
    )

    private val thorApps = listOf(
        InstalledApp("PCSA00115", "MLB® 14 The Show™"),
        InstalledApp("PCSB00031", "Virtua Tennis 4"),
        InstalledApp("PCSB00245", "Persona 4 GOLDEN"),
        InstalledApp("PCSB00604", "FIFA 15"),
        InstalledApp("PCSB00866", "THE LEGEND OF HEROES: TRAILS OF COLD STEEL"),
        InstalledApp("PCSB01128", "Ys VIII -Lacrimosa of DANA-"),
        InstalledApp("PCSE00261", "Danganronpa: Trigger Happy Havoc"),
        InstalledApp("PCSF00243", "Killzone™ Mercenary"),
        InstalledApp("PCSG01179", "キャサリン・フルボディ")
    )

    @Test
    fun `the thor library splits into four sure pairs and three title pairs`() {
        val result = Vita3kLinkMatcher.match(thorGames, thorApps, held = setOf("PCSF00243", "PCSB00866"))

        assertEquals(
            setOf(1L to "PCSB00604", 2L to "PCSA00115", 3L to "PCSB01128", 4L to "PCSG01179"),
            result.sure.map { it.gameId to it.titleId }.toSet()
        )
        assertEquals(
            setOf(5L to "PCSB00245", 6L to "PCSE00261", 8L to "PCSB00031"),
            result.byTitle.map { it.gameId to it.titleId }.toSet()
        )
    }

    @Test
    fun `a game whose own title id is not installed is never matched by name`() {
        val result = Vita3kLinkMatcher.match(
            listOf(game(9, "Fez", titleId = "PCSB00456")),
            listOf(InstalledApp("PCSB99999", "FEZ")),
            held = emptySet()
        )

        assertTrue(result.sure.isEmpty())
        assertTrue(result.byTitle.isEmpty())
    }

    @Test
    fun `a title shared by two games links neither`() {
        val result = Vita3kLinkMatcher.match(
            listOf(game(1, "Danganronpa: Trigger Happy Havoc"), game(2, "Danganronpa 2: Goodbye Despair")),
            listOf(InstalledApp("PCSE00261", "Danganronpa")),
            held = emptySet()
        )

        assertTrue(result.byTitle.isEmpty())
    }

    @Test
    fun `an id already held by a local game is not offered again`() {
        val result = Vita3kLinkMatcher.match(
            listOf(game(1, "FIFA 15", "FIFA 15 [PCSB00604] [EUR].zip")),
            listOf(InstalledApp("PCSB00604", "FIFA 15")),
            held = setOf("PCSB00604")
        )

        assertTrue(result.sure.isEmpty())
        assertTrue(result.byTitle.isEmpty())
    }

    @Test
    fun `titles match on whole words only`() {
        assertTrue(Vita3kLinkMatcher.titlesMatch("Virtua Tennis 4: World Tour Edition", "Virtua Tennis 4"))
        assertTrue(Vita3kLinkMatcher.titlesMatch("MLB 14: The Show", "MLB® 14 The Show™"))
        assertFalse(Vita3kLinkMatcher.titlesMatch("Ys: Memories of Celceta", "Ys VIII"))
        assertFalse(Vita3kLinkMatcher.titlesMatch("Danganronpa 2: Goodbye Despair", "Danganronpa: Trigger Happy Havoc"))
    }

    @Test
    fun `a title that prefixes two games pairs with neither`() {
        val result = Vita3kLinkMatcher.match(
            listOf(game(1, "Steins;Gate"), game(2, "Steins;Gate 0")),
            listOf(InstalledApp("PCSG00213", "STEINS;GATE")),
            held = emptySet()
        )

        assertTrue(result.byTitle.isEmpty())
    }
}
