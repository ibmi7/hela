package com.nendo.argosy.data.emulator

import org.junit.Assert.assertEquals
import org.junit.Test

class Vita3kLinkZipNameTest {

    @Test
    fun `a single file rom keeps its file name`() {
        assertEquals("FIFA 15 [PCSB00604] [EUR].zip", Vita3kLink.downloadZipName("FIFA 15 [PCSB00604] [EUR].zip", "FIFA 15", 1))
    }

    @Test
    fun `a multi file rom becomes its folder name with zip`() {
        assertEquals("Persona 4 Golden.zip", Vita3kLink.downloadZipName("Persona 4 Golden", "Persona 4 Golden", 3))
    }

    @Test
    fun `refused characters are dropped as the download queue drops them`() {
        assertEquals("Steins;Gate 0.zip", Vita3kLink.downloadZipName("Steins;Gate 0", "Steins;Gate 0", 2))
        assertEquals("Catherine Full Body.zip", Vita3kLink.downloadZipName(null, "Catherine: Full Body", 2))
    }
}
