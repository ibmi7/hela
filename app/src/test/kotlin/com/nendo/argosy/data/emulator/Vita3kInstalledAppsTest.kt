package com.nendo.argosy.data.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Vita3kInstalledAppsTest {

    private val apps = listOf("PCSF00243", "PCSB00866", "PCSB00456_dec")
    private fun listing(path: String): List<String>? = apps.takeIf { path == "$ROOT/ux0/app" }

    @Test
    fun `the apps folder sits beside the save data`() {
        assertEquals("$ROOT/ux0/app", Vita3kInstalledApps.appsDirOf("$ROOT/ux0/user/00/savedata/"))
        assertNull(Vita3kInstalledApps.appsDirOf("$ROOT/PSP/SAVEDATA"))
    }

    @Test
    fun `an installed title is found whatever its case`() {
        assertEquals(true, Vita3kInstalledApps.isInstalled(SAVE_DATA, "PCSF00243", ::listing))
        assertEquals(true, Vita3kInstalledApps.isInstalled(SAVE_DATA, "pcsb00866", ::listing))
    }

    @Test
    fun `a missing title and a decrypted copy are not installed`() {
        assertEquals(false, Vita3kInstalledApps.isInstalled(SAVE_DATA, "PCSB00456", ::listing))
        assertEquals(false, Vita3kInstalledApps.isInstalled(SAVE_DATA, "PCSB00456_dec", ::listing))
    }

    @Test
    fun `an unreadable or unknown location gives no answer`() {
        assertNull(Vita3kInstalledApps.isInstalled(null, "PCSF00243", ::listing))
        assertNull(Vita3kInstalledApps.isInstalled("/elsewhere/ux0/user/00/savedata", "PCSF00243", ::listing))
        assertNull(Vita3kInstalledApps.isInstalled("$ROOT/saves", "PCSF00243", ::listing))
    }

    private companion object {
        const val ROOT = "/storage/4A21-0000/Vita3k"
        const val SAVE_DATA = "$ROOT/ux0/user/00/savedata"
    }
}
